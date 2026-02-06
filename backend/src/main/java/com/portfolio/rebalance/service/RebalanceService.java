package com.portfolio.rebalance.service;

import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.exception.ErrorCode;
import com.portfolio.portfolio.entity.PortfolioTarget;
import com.portfolio.portfolio.repository.PortfolioRepository;
import com.portfolio.portfolio.repository.PortfolioTargetRepository;
import com.portfolio.valuation.service.ValuationService;
import com.portfolio.valuation.service.ValuationService.PortfolioValuation;
import com.portfolio.valuation.service.ValuationService.PositionValuation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 리밸런싱 분석 서비스
 *
 * - 현재 비중 vs 목표 비중 비교
 * - 최소 매매로 리밸런싱 추천
 * - 현금 제약 반영
 * - 거래 비용 추정
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RebalanceService {

    private final ValuationService valuationService;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioTargetRepository targetRepository;

    private static final BigDecimal FEE_RATE = new BigDecimal("0.001"); // 0.1%
    private static final int SCALE = 6;

    /**
     * 리밸런싱 분석 수행
     */
    @Transactional(readOnly = true)
    public RebalanceAnalysis analyze(String portfolioId, String workspaceId) {
        portfolioRepository.findByIdAndWorkspaceId(portfolioId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 1. 현재 평가 조회
        PortfolioValuation valuation = valuationService.calculateValuation(portfolioId, workspaceId);

        // 2. 목표 비중 조회
        List<PortfolioTarget> targets = targetRepository.findByPortfolioId(portfolioId);

        if (targets.isEmpty()) {
            return emptyAnalysis(portfolioId, valuation);
        }

        BigDecimal totalValue = valuation.totalValueBase;

        // 3. 현재 비중 맵
        Map<String, BigDecimal> currentWeights = new LinkedHashMap<>();
        Map<String, BigDecimal> currentValues = new LinkedHashMap<>();
        Map<String, String> instrumentNames = new LinkedHashMap<>();

        for (PositionValuation pos : valuation.positions) {
            BigDecimal weight = totalValue.compareTo(BigDecimal.ZERO) > 0
                    ? pos.marketValueBase
                    .divide(totalValue, SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            currentWeights.put(pos.instrumentId, weight);
            currentValues.put(pos.instrumentId, pos.marketValueBase);
            instrumentNames.put(pos.instrumentId, pos.instrumentName != null ? pos.instrumentName : pos.instrumentId);
        }

        // 현금 비중
        BigDecimal cashWeight = totalValue.compareTo(BigDecimal.ZERO) > 0
                ? valuation.cashValueBase
                .divide(totalValue, SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 4. 목표 비중 맵
        Map<String, BigDecimal> targetWeights = new LinkedHashMap<>();
        for (PortfolioTarget target : targets) {
            if (target.getInstrumentId() != null) {
                targetWeights.put(target.getInstrumentId(), target.getTargetWeight());
            }
        }

        // 5. 비중 비교 테이블 구성
        Set<String> allInstruments = new LinkedHashSet<>();
        allInstruments.addAll(currentWeights.keySet());
        allInstruments.addAll(targetWeights.keySet());

        List<WeightComparison> comparisons = new ArrayList<>();
        List<TradeRecommendation> trades = new ArrayList<>();
        BigDecimal totalFee = BigDecimal.ZERO;

        for (String instrumentId : allInstruments) {
            BigDecimal curWeight = currentWeights.getOrDefault(instrumentId, BigDecimal.ZERO);
            BigDecimal tgtWeight = targetWeights.getOrDefault(instrumentId, BigDecimal.ZERO);
            BigDecimal diff = tgtWeight.subtract(curWeight);

            BigDecimal curValue = currentValues.getOrDefault(instrumentId, BigDecimal.ZERO);
            BigDecimal tgtValue = totalValue.multiply(tgtWeight).setScale(0, RoundingMode.HALF_UP);
            BigDecimal diffValue = tgtValue.subtract(curValue);

            String name = instrumentNames.getOrDefault(instrumentId, instrumentId);

            // 비중 비교
            WeightComparison wc = new WeightComparison();
            wc.setInstrumentId(instrumentId);
            wc.setInstrumentName(name);
            wc.setCurrentWeight(curWeight);
            wc.setTargetWeight(tgtWeight);
            wc.setDifference(diff);
            wc.setCurrentValue(curValue.setScale(0, RoundingMode.HALF_UP));
            wc.setTargetValue(tgtValue);
            wc.setDiffValue(diffValue);
            comparisons.add(wc);

            // 매매 추천 (차이가 0.5% 이상이면)
            if (diff.abs().compareTo(new BigDecimal("0.005")) > 0) {
                TradeRecommendation trade = new TradeRecommendation();
                trade.setInstrumentId(instrumentId);
                trade.setInstrumentName(name);
                trade.setAction(diffValue.compareTo(BigDecimal.ZERO) > 0 ? "BUY" : "SELL");
                trade.setAmount(diffValue.abs().setScale(0, RoundingMode.HALF_UP));
                trade.setEstimatedFee(diffValue.abs().multiply(FEE_RATE).setScale(0, RoundingMode.HALF_UP));
                trades.add(trade);

                totalFee = totalFee.add(trade.getEstimatedFee());
            }
        }

        // 정렬: 비중 차이 절대값 큰 순서
        comparisons.sort((a, b) -> b.getDifference().abs().compareTo(a.getDifference().abs()));
        trades.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        // 결과
        RebalanceAnalysis analysis = new RebalanceAnalysis();
        analysis.setPortfolioId(portfolioId);
        analysis.setTotalValue(totalValue.setScale(0, RoundingMode.HALF_UP));
        analysis.setCashBalance(valuation.cashValueBase.setScale(0, RoundingMode.HALF_UP));
        analysis.setCashWeight(cashWeight);
        analysis.setComparisons(comparisons);
        analysis.setTrades(trades);
        analysis.setTotalEstimatedFee(totalFee);
        analysis.setNeedsRebalancing(!trades.isEmpty());

        // 최대 이탈도
        BigDecimal maxDrift = comparisons.stream()
                .map(c -> c.getDifference().abs())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        analysis.setMaxDrift(maxDrift);

        return analysis;
    }

    private RebalanceAnalysis emptyAnalysis(String portfolioId, PortfolioValuation valuation) {
        RebalanceAnalysis analysis = new RebalanceAnalysis();
        analysis.setPortfolioId(portfolioId);
        analysis.setTotalValue(valuation.totalValueBase.setScale(0, RoundingMode.HALF_UP));
        analysis.setCashBalance(valuation.cashValueBase.setScale(0, RoundingMode.HALF_UP));
        analysis.setCashWeight(BigDecimal.ONE);
        analysis.setComparisons(Collections.emptyList());
        analysis.setTrades(Collections.emptyList());
        analysis.setTotalEstimatedFee(BigDecimal.ZERO);
        analysis.setNeedsRebalancing(false);
        analysis.setMaxDrift(BigDecimal.ZERO);
        return analysis;
    }

    // ========== DTOs ==========

    @Data
    public static class RebalanceAnalysis {
        private String portfolioId;
        private BigDecimal totalValue;
        private BigDecimal cashBalance;
        private BigDecimal cashWeight;
        private List<WeightComparison> comparisons;
        private List<TradeRecommendation> trades;
        private BigDecimal totalEstimatedFee;
        private boolean needsRebalancing;
        private BigDecimal maxDrift;
    }

    @Data
    public static class WeightComparison {
        private String instrumentId;
        private String instrumentName;
        private BigDecimal currentWeight;
        private BigDecimal targetWeight;
        private BigDecimal difference;
        private BigDecimal currentValue;
        private BigDecimal targetValue;
        private BigDecimal diffValue;
    }

    @Data
    public static class TradeRecommendation {
        private String instrumentId;
        private String instrumentName;
        private String action;        // BUY or SELL
        private BigDecimal amount;
        private BigDecimal estimatedFee;
    }
}
