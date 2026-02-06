package com.portfolio.valuation.service;

import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.exception.ErrorCode;
import com.portfolio.ledger.entity.Transaction;
import com.portfolio.ledger.entity.TransactionLeg;
import com.portfolio.ledger.repository.TransactionRepository;
import com.portfolio.portfolio.entity.Portfolio;
import com.portfolio.portfolio.repository.PortfolioRepository;
import com.portfolio.pricing.entity.Instrument;
import com.portfolio.pricing.repository.InstrumentRepository;
import com.portfolio.pricing.service.PriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValuationService {

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final InstrumentRepository instrumentRepository;
    private final PriceService priceService;

    /**
     * 포트폴리오 평가 계산
     * - 거래 내역에서 포지션 계산 (보유 수량, 평균 단가)
     * - 현금 잔액 계산
     * - 시가 평가액 계산 (Mock 가격 사용)
     */
    @Transactional(readOnly = true)
    public PortfolioValuation calculateValuation(String portfolioId, String workspaceId) {
        Portfolio portfolio = portfolioRepository.findByIdAndWorkspaceId(portfolioId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // POSTED 거래만 조회
        List<Transaction> transactions = transactionRepository.findByPortfolioIdWithLegs(
                portfolioId, Transaction.TransactionStatus.VOID);

        // 포지션 계산
        Map<String, PositionAccumulator> positionMap = new LinkedHashMap<>();
        BigDecimal cashBalance = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            for (TransactionLeg leg : tx.getLegs()) {
                if (leg.getLegType() == TransactionLeg.LegType.ASSET && leg.getInstrumentId() != null) {
                    positionMap.computeIfAbsent(leg.getInstrumentId(), k -> new PositionAccumulator())
                            .addTrade(leg.getQuantity(), leg.getPrice(), leg.getAmount());
                } else if (leg.getLegType() == TransactionLeg.LegType.CASH) {
                    // EXTERNAL 계정은 포트폴리오 현금에 포함 안 함
                    if (!"EXTERNAL".equals(leg.getAccount())) {
                        cashBalance = cashBalance.add(leg.getAmount());
                    }
                }
                // FEE, TAX는 현금에서 차감 (CASH leg에 이미 반영)
            }
        }

        // 각 포지션 평가
        List<PositionValuation> positions = new ArrayList<>();
        BigDecimal totalAssetValue = BigDecimal.ZERO;

        for (Map.Entry<String, PositionAccumulator> entry : positionMap.entrySet()) {
            String instrumentId = entry.getKey();
            PositionAccumulator acc = entry.getValue();

            // 수량이 0이면 스킵 (전량 매도)
            if (acc.quantity.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal currentPrice = priceService.getCurrentPrice(instrumentId);
            BigDecimal marketValue = acc.quantity.multiply(currentPrice);
            BigDecimal costBasis = acc.quantity.multiply(acc.getAvgCost());
            BigDecimal unrealizedPnl = marketValue.subtract(costBasis);

            // Instrument 정보 조회 (있으면)
            String ticker = instrumentId;
            String name = null;
            Optional<Instrument> instrument = instrumentRepository.findById(instrumentId);
            String assetClass = "EQUITY"; // default
            if (instrument.isPresent()) {
                ticker = instrument.get().getTicker() != null ? instrument.get().getTicker() : instrumentId;
                name = instrument.get().getName();
                if (instrument.get().getAssetClass() != null) {
                    assetClass = instrument.get().getAssetClass().name();
                }
            }

            PositionValuation pv = new PositionValuation();
            pv.instrumentId = instrumentId;
            pv.ticker = ticker;
            pv.instrumentName = name;
            pv.assetClass = assetClass;
            pv.quantity = acc.quantity;
            pv.avgCost = acc.getAvgCost();
            pv.marketPrice = currentPrice;
            pv.marketValue = marketValue;
            pv.marketValueBase = marketValue; // 같은 통화면 1:1
            pv.unrealizedPnlBase = unrealizedPnl;
            pv.realizedPnlBase = acc.realizedPnl;

            totalAssetValue = totalAssetValue.add(marketValue);
            positions.add(pv);
        }

        BigDecimal totalValue = totalAssetValue.add(cashBalance);

        // 비중 계산 (자산 시가 총합 기반 - 음수 총가치 방지)
        BigDecimal weightBase = totalAssetValue.abs().compareTo(BigDecimal.ZERO) > 0
                ? totalAssetValue.abs()
                : BigDecimal.ONE;
        for (PositionValuation pv : positions) {
            pv.weight = pv.marketValueBase.abs().divide(weightBase, 6, RoundingMode.HALF_UP);
        }

        // 전체 손익
        BigDecimal totalCostBasis = positions.stream()
                .map(p -> p.quantity.multiply(p.avgCost))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPnl = totalAssetValue.subtract(totalCostBasis);

        PortfolioValuation valuation = new PortfolioValuation();
        valuation.portfolioId = portfolioId;
        valuation.currency = portfolio.getBaseCurrency();
        valuation.totalValueBase = totalValue;
        valuation.cashValueBase = cashBalance;
        valuation.dayPnlBase = BigDecimal.ZERO; // 일별 변동은 가격 히스토리 필요
        valuation.totalPnlBase = totalPnl;
        valuation.positions = positions;

        log.debug("Valuation for portfolio {}: total={}, cash={}, positions={}",
                portfolioId, totalValue, cashBalance, positions.size());

        return valuation;
    }

    /**
     * 특정 날짜 기준 포트폴리오 가치 계산 (성과 분석용)
     * 해당 날짜까지의 거래를 기반으로 포지션을 계산하고, 해당 날짜의 종가로 평가
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateValueAtDate(String portfolioId, LocalDate date) {
        List<Transaction> transactions = transactionRepository.findByPortfolioIdWithLegs(
                portfolioId, Transaction.TransactionStatus.VOID);

        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        Map<String, PositionAccumulator> positionMap = new LinkedHashMap<>();
        BigDecimal cashBalance = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            // 해당 날짜까지의 거래만 포함
            if (tx.getOccurredAt() != null && tx.getOccurredAt().isAfter(endOfDay)) continue;

            for (TransactionLeg leg : tx.getLegs()) {
                if (leg.getLegType() == TransactionLeg.LegType.ASSET && leg.getInstrumentId() != null) {
                    positionMap.computeIfAbsent(leg.getInstrumentId(), k -> new PositionAccumulator())
                            .addTrade(leg.getQuantity(), leg.getPrice(), leg.getAmount());
                } else if (leg.getLegType() == TransactionLeg.LegType.CASH) {
                    if (!"EXTERNAL".equals(leg.getAccount())) {
                        cashBalance = cashBalance.add(leg.getAmount());
                    }
                }
            }
        }

        BigDecimal totalAssetValue = BigDecimal.ZERO;
        for (Map.Entry<String, PositionAccumulator> entry : positionMap.entrySet()) {
            PositionAccumulator acc = entry.getValue();
            if (acc.quantity.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal price = priceService.getHistoricalPrice(entry.getKey(), date)
                    .orElse(priceService.getCurrentPrice(entry.getKey()));
            totalAssetValue = totalAssetValue.add(acc.quantity.multiply(price));
        }

        return totalAssetValue.add(cashBalance);
    }

    /**
     * 특정 날짜의 현금흐름 계산 (입금/출금 합계, TWR 계산용)
     */
    @Transactional(readOnly = true)
    public BigDecimal getCashFlowAtDate(String portfolioId, LocalDate date) {
        List<Transaction> transactions = transactionRepository.findByPortfolioIdWithLegs(
                portfolioId, Transaction.TransactionStatus.VOID);

        BigDecimal cashFlow = BigDecimal.ZERO;
        for (Transaction tx : transactions) {
            if (tx.getOccurredAt() == null) continue;
            LocalDate txDate = tx.getOccurredAt().toLocalDate();
            if (!txDate.equals(date)) continue;

            // DEPOSIT, WITHDRAW 거래의 EXTERNAL 계정 금액이 외부 현금흐름
            if (tx.getType() == Transaction.TransactionType.DEPOSIT
                    || tx.getType() == Transaction.TransactionType.WITHDRAW) {
                for (TransactionLeg leg : tx.getLegs()) {
                    if (leg.getLegType() == TransactionLeg.LegType.CASH
                            && !"EXTERNAL".equals(leg.getAccount())) {
                        cashFlow = cashFlow.add(leg.getAmount());
                    }
                }
            }
        }
        return cashFlow;
    }

    // ===== Inner classes =====

    /**
     * 포지션 누적기 (거래 기반 포지션 계산)
     */
    private static class PositionAccumulator {
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO; // 총 매입 금액
        BigDecimal realizedPnl = BigDecimal.ZERO;

        void addTrade(BigDecimal qty, BigDecimal price, BigDecimal amount) {
            if (qty == null) return;

            if (qty.compareTo(BigDecimal.ZERO) > 0) {
                // 매수: 평균 단가에 반영
                totalCost = totalCost.add(qty.multiply(price != null ? price : BigDecimal.ZERO));
                quantity = quantity.add(qty);
            } else {
                // 매도: 실현 손익 계산
                BigDecimal sellQty = qty.abs();
                BigDecimal avgCost = getAvgCost();
                BigDecimal sellPrice = price != null ? price : BigDecimal.ZERO;
                realizedPnl = realizedPnl.add(
                        sellQty.multiply(sellPrice.subtract(avgCost)));

                // 비용 비례 차감
                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal costReduction = avgCost.multiply(sellQty);
                    totalCost = totalCost.subtract(costReduction);
                }
                quantity = quantity.add(qty); // qty가 음수이므로 차감됨
            }
        }

        BigDecimal getAvgCost() {
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
            return totalCost.divide(quantity, 6, RoundingMode.HALF_UP);
        }
    }

    // ===== Response DTOs =====

    public static class PortfolioValuation {
        public String portfolioId;
        public String currency;
        public BigDecimal totalValueBase;
        public BigDecimal cashValueBase;
        public BigDecimal dayPnlBase;
        public BigDecimal totalPnlBase;
        public List<PositionValuation> positions;
    }

    public static class PositionValuation {
        public String instrumentId;
        public String ticker;
        public String instrumentName;
        public String assetClass;
        public BigDecimal quantity;
        public BigDecimal avgCost;
        public BigDecimal marketPrice;
        public BigDecimal marketValue;
        public BigDecimal marketValueBase;
        public BigDecimal unrealizedPnlBase;
        public BigDecimal realizedPnlBase;
        public BigDecimal weight;
        public BigDecimal dayPnlBase;
    }
}
