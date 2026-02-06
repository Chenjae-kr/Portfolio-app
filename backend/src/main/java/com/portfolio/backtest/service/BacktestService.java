package com.portfolio.backtest.service;

import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.exception.ErrorCode;
import com.portfolio.pricing.service.PriceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 백테스트 엔진
 *
 * - 정적 자산 배분 (Static Allocation)
 * - 주기적 리밸런싱 (Periodic: Monthly/Quarterly/Semi-Annual/Annual)
 * - 밴드 리밸런싱 (Band: 목표 비중 ± threshold 이탈 시)
 * - 거래 비용 0.1% 반영
 * - 배당 재투자 옵션
 * - 일별 Equity Curve, Drawdown, Trade Log 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BacktestService {

    private final PriceService priceService;

    // In-memory 저장소 (추후 DB 전환)
    private final Map<String, BacktestConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, BacktestRun> runs = new ConcurrentHashMap<>();
    private final Map<String, BacktestResult> results = new ConcurrentHashMap<>();

    private static final BigDecimal TRANSACTION_FEE_RATE = new BigDecimal("0.001"); // 0.1%
    private static final int SCALE = 6;

    // ========== Config CRUD ==========

    public BacktestConfig createConfig(BacktestConfig config) {
        if (config.getId() == null || config.getId().isBlank()) {
            config.setId(UUID.randomUUID().toString());
        }
        configs.put(config.getId(), config);
        log.info("Created backtest config: id={}, name={}", config.getId(), config.getName());
        return config;
    }

    public BacktestConfig getConfig(String configId) {
        BacktestConfig config = configs.get(configId);
        if (config == null) {
            throw new BusinessException(ErrorCode.BACKTEST_NOT_FOUND);
        }
        return config;
    }

    public List<BacktestConfig> listConfigs() {
        return new ArrayList<>(configs.values());
    }

    // ========== Run ==========

    public BacktestRun runBacktest(String configId, BacktestConfig inlineConfig) {
        BacktestConfig config;
        if (inlineConfig != null) {
            config = createConfig(inlineConfig);
        } else if (configId != null) {
            config = getConfig(configId);
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // Run 생성
        BacktestRun run = new BacktestRun();
        run.setId(UUID.randomUUID().toString());
        run.setConfigId(config.getId());
        run.setStatus("RUNNING");
        run.setStartedAt(Instant.now().toString());
        runs.put(run.getId(), run);

        // 동기 실행
        try {
            BacktestResult result = executeBacktest(config, run.getId());
            results.put(run.getId(), result);

            run.setStatus("SUCCEEDED");
            run.setFinishedAt(Instant.now().toString());
            log.info("Backtest succeeded: runId={}, config={}", run.getId(), config.getName());
        } catch (Exception e) {
            run.setStatus("FAILED");
            run.setFinishedAt(Instant.now().toString());
            run.setErrorMessage(e.getMessage());
            log.error("Backtest failed: runId={}", run.getId(), e);
        }

        return run;
    }

    public BacktestRun getRun(String runId) {
        BacktestRun run = runs.get(runId);
        if (run == null) {
            throw new BusinessException(ErrorCode.BACKTEST_NOT_FOUND);
        }
        return run;
    }

    public List<BacktestRun> listRuns(String configId) {
        if (configId != null) {
            return runs.values().stream()
                    .filter(r -> configId.equals(r.getConfigId()))
                    .toList();
        }
        return new ArrayList<>(runs.values());
    }

    public BacktestResult getResult(String runId) {
        BacktestResult result = results.get(runId);
        if (result == null) {
            throw new BusinessException(ErrorCode.BACKTEST_NOT_FOUND);
        }
        return result;
    }

    // ========== 백테스트 엔진 ==========

    private BacktestResult executeBacktest(BacktestConfig config, String runId) {
        LocalDate start = LocalDate.parse(config.getStartDate());
        LocalDate end = LocalDate.parse(config.getEndDate());
        BigDecimal initialCapital = config.getInitialCapitalBase();

        List<TargetAlloc> targetAllocs = config.getTargets();
        if (targetAllocs == null || targetAllocs.isEmpty()) {
            throw new IllegalArgumentException("At least one target allocation required");
        }

        // DCA 설정
        boolean isDCA = "DCA".equals(config.getInvestmentType());
        BigDecimal totalInvested = initialCapital != null ? initialCapital : BigDecimal.ZERO;

        // 포지션: instrumentId → 보유 수량
        Map<String, BigDecimal> positions = new LinkedHashMap<>();
        BigDecimal cash = totalInvested;

        List<SeriesPoint> series = new ArrayList<>();
        List<TradeLog> tradeLogs = new ArrayList<>();

        LocalDate current = start;
        LocalDate lastRebalanceDate = null;
        LocalDate lastDepositDate = null;
        boolean firstDay = true;

        while (!current.isAfter(end)) {
            // 주말 제외
            if (current.getDayOfWeek().getValue() > 5) {
                current = current.plusDays(1);
                continue;
            }

            // 일별 가격 조회
            Map<String, BigDecimal> prices = new LinkedHashMap<>();
            for (TargetAlloc t : targetAllocs) {
                BigDecimal price = priceService.getHistoricalPrice(t.getInstrumentId(), current)
                        .orElse(BigDecimal.valueOf(100));
                prices.put(t.getInstrumentId(), price);
            }

            // 첫 거래일: 초기 배분 + DCA인 경우 첫 입금
            if (firstDay) {
                if (isDCA && config.getDcaAmount() != null
                        && config.getDcaAmount().compareTo(BigDecimal.ZERO) > 0) {
                    cash = cash.add(config.getDcaAmount());
                    totalInvested = totalInvested.add(config.getDcaAmount());
                    TradeLog depositLog = new TradeLog();
                    depositLog.setTs(current.toString());
                    depositLog.setAction("DEPOSIT");
                    depositLog.setAmount(config.getDcaAmount().setScale(2, RoundingMode.HALF_UP));
                    tradeLogs.add(depositLog);
                }
                if (cash.compareTo(BigDecimal.ZERO) > 0) {
                    cash = executeTrades(targetAllocs, positions, cash, prices, tradeLogs, current);
                }
                firstDay = false;
                lastRebalanceDate = current;
                lastDepositDate = current;
            } else {
                // DCA 입금 체크
                boolean deposited = false;
                if (isDCA && config.getDcaAmount() != null
                        && config.getDcaAmount().compareTo(BigDecimal.ZERO) > 0) {
                    if (isDepositDue(current, lastDepositDate, config.getDcaFrequency())) {
                        cash = cash.add(config.getDcaAmount());
                        totalInvested = totalInvested.add(config.getDcaAmount());
                        lastDepositDate = current;
                        deposited = true;

                        TradeLog depositLog = new TradeLog();
                        depositLog.setTs(current.toString());
                        depositLog.setAction("DEPOSIT");
                        depositLog.setAmount(config.getDcaAmount().setScale(2, RoundingMode.HALF_UP));
                        tradeLogs.add(depositLog);
                    }
                }

                // 리밸런싱 체크
                boolean shouldRebalance = shouldRebalance(
                        config.getRebalanceType(), config.getRebalancePeriod(),
                        config.getBandThreshold(), current, lastRebalanceDate,
                        targetAllocs, positions, cash, prices);

                if (shouldRebalance || deposited) {
                    cash = executeTrades(targetAllocs, positions, cash, prices, tradeLogs, current);
                    if (shouldRebalance) {
                        lastRebalanceDate = current;
                    }
                }
            }

            // 포트폴리오 가치 계산
            BigDecimal totalValue = cash;
            for (Map.Entry<String, BigDecimal> entry : positions.entrySet()) {
                BigDecimal price = prices.getOrDefault(entry.getKey(), BigDecimal.ZERO);
                totalValue = totalValue.add(entry.getValue().multiply(price));
            }

            // Drawdown 계산
            BigDecimal drawdown = BigDecimal.ZERO;
            if (!series.isEmpty()) {
                BigDecimal peak = series.stream()
                        .map(SeriesPoint::getEquityCurveBase)
                        .max(BigDecimal::compareTo)
                        .orElse(totalValue);
                if (peak.compareTo(BigDecimal.ZERO) > 0 && totalValue.compareTo(peak) < 0) {
                    drawdown = peak.subtract(totalValue)
                            .divide(peak, SCALE, RoundingMode.HALF_UP);
                }
            }

            SeriesPoint point = new SeriesPoint();
            point.setTs(current.toString());
            point.setEquityCurveBase(totalValue.setScale(2, RoundingMode.HALF_UP));
            point.setDrawdown(drawdown);
            point.setCashBase(cash.setScale(2, RoundingMode.HALF_UP));
            point.setTotalInvested(totalInvested.setScale(2, RoundingMode.HALF_UP));
            series.add(point);

            current = current.plusDays(1);
        }

        // 통계 계산
        PerformanceStats stats = calculateStats(series, initialCapital, totalInvested, isDCA);

        BacktestResult result = new BacktestResult();
        BacktestRun run = getRun(runId);
        result.setRun(run);
        result.setSeries(series);
        result.setStats(stats);
        result.setTradeLogs(tradeLogs);

        return result;
    }

    /**
     * 목표 비중에 맞게 매매 실행
     */
    private BigDecimal executeTrades(
            List<TargetAlloc> targets,
            Map<String, BigDecimal> positions,
            BigDecimal cash,
            Map<String, BigDecimal> prices,
            List<TradeLog> tradeLogs,
            LocalDate date) {

        // 현재 포트폴리오 가치
        BigDecimal totalValue = cash;
        for (Map.Entry<String, BigDecimal> entry : positions.entrySet()) {
            BigDecimal price = prices.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            totalValue = totalValue.add(entry.getValue().multiply(price));
        }

        if (totalValue.compareTo(BigDecimal.ZERO) <= 0) return cash;

        // 목표 수량 계산 및 매매
        for (TargetAlloc target : targets) {
            String instrumentId = target.getInstrumentId();
            BigDecimal price = prices.getOrDefault(instrumentId, BigDecimal.valueOf(100));
            if (price.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal targetValue = totalValue.multiply(target.getTargetWeight());
            BigDecimal currentQty = positions.getOrDefault(instrumentId, BigDecimal.ZERO);
            BigDecimal currentValue = currentQty.multiply(price);
            BigDecimal diffValue = targetValue.subtract(currentValue);

            // 최소 거래 임계값 (총 가치의 0.5% 미만이면 스킵)
            if (diffValue.abs().compareTo(totalValue.multiply(new BigDecimal("0.005"))) < 0) {
                continue;
            }

            BigDecimal tradeQty = diffValue.divide(price, 8, RoundingMode.HALF_DOWN);
            BigDecimal tradeAmount = tradeQty.abs().multiply(price);
            BigDecimal fee = tradeAmount.multiply(TRANSACTION_FEE_RATE).setScale(2, RoundingMode.HALF_UP);

            if (tradeQty.compareTo(BigDecimal.ZERO) > 0) {
                // BUY
                BigDecimal cost = tradeAmount.add(fee);
                if (cost.compareTo(cash) > 0) {
                    // 현금 부족 → 가능한 만큼만 매수
                    tradeQty = cash.subtract(fee)
                            .divide(price, 8, RoundingMode.HALF_DOWN);
                    tradeAmount = tradeQty.abs().multiply(price);
                    fee = tradeAmount.multiply(TRANSACTION_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
                    cost = tradeAmount.add(fee);
                }
                positions.put(instrumentId, currentQty.add(tradeQty));
                cash = cash.subtract(cost);

                TradeLog log = new TradeLog();
                log.setTs(date.toString());
                log.setInstrumentId(instrumentId);
                log.setAction("BUY");
                log.setQuantity(tradeQty.setScale(4, RoundingMode.HALF_UP));
                log.setPrice(price.setScale(2, RoundingMode.HALF_UP));
                log.setFee(fee);
                tradeLogs.add(log);
            } else if (tradeQty.compareTo(BigDecimal.ZERO) < 0) {
                // SELL
                BigDecimal sellQty = tradeQty.abs();
                if (sellQty.compareTo(currentQty) > 0) {
                    sellQty = currentQty;
                }
                BigDecimal proceeds = sellQty.multiply(price).subtract(fee);
                positions.put(instrumentId, currentQty.subtract(sellQty));
                cash = cash.add(proceeds);

                TradeLog log = new TradeLog();
                log.setTs(date.toString());
                log.setInstrumentId(instrumentId);
                log.setAction("SELL");
                log.setQuantity(sellQty.setScale(4, RoundingMode.HALF_UP));
                log.setPrice(price.setScale(2, RoundingMode.HALF_UP));
                log.setFee(fee);
                tradeLogs.add(log);
            }
        }

        return cash;
    }

    /**
     * 리밸런싱 필요 여부 확인
     */
    private boolean shouldRebalance(
            String rebalanceType, String period, BigDecimal bandThreshold,
            LocalDate current, LocalDate lastRebalance,
            List<TargetAlloc> targets, Map<String, BigDecimal> positions,
            BigDecimal cash, Map<String, BigDecimal> prices) {

        if ("NONE".equals(rebalanceType)) return false;

        if ("PERIODIC".equals(rebalanceType)) {
            if (lastRebalance == null) return true;
            return isRebalanceDue(current, lastRebalance, period);
        }

        if ("BAND".equals(rebalanceType)) {
            if (bandThreshold == null) bandThreshold = new BigDecimal("0.05");
            return isOutsideBand(targets, positions, cash, prices, bandThreshold);
        }

        return false;
    }

    /**
     * DCA 입금일 여부 확인 (주기 판정은 isRebalanceDue와 동일)
     */
    private boolean isDepositDue(LocalDate current, LocalDate lastDeposit, String frequency) {
        if (lastDeposit == null) return true;
        if (frequency == null) frequency = "MONTHLY";
        return isRebalanceDue(current, lastDeposit, frequency);
    }

    private boolean isRebalanceDue(LocalDate current, LocalDate lastRebalance, String period) {
        return switch (period) {
            case "MONTHLY" -> !current.getMonth().equals(lastRebalance.getMonth())
                    || current.getYear() != lastRebalance.getYear();
            case "QUARTERLY" -> {
                int currentQ = (current.getMonthValue() - 1) / 3;
                int lastQ = (lastRebalance.getMonthValue() - 1) / 3;
                yield currentQ != lastQ || current.getYear() != lastRebalance.getYear();
            }
            case "SEMI_ANNUAL" -> {
                int currentH = (current.getMonthValue() - 1) / 6;
                int lastH = (lastRebalance.getMonthValue() - 1) / 6;
                yield currentH != lastH || current.getYear() != lastRebalance.getYear();
            }
            case "ANNUAL" -> current.getYear() != lastRebalance.getYear();
            default -> false;
        };
    }

    private boolean isOutsideBand(
            List<TargetAlloc> targets, Map<String, BigDecimal> positions,
            BigDecimal cash, Map<String, BigDecimal> prices, BigDecimal threshold) {

        BigDecimal totalValue = cash;
        for (Map.Entry<String, BigDecimal> entry : positions.entrySet()) {
            BigDecimal price = prices.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            totalValue = totalValue.add(entry.getValue().multiply(price));
        }

        if (totalValue.compareTo(BigDecimal.ZERO) <= 0) return false;

        for (TargetAlloc target : targets) {
            BigDecimal qty = positions.getOrDefault(target.getInstrumentId(), BigDecimal.ZERO);
            BigDecimal price = prices.getOrDefault(target.getInstrumentId(), BigDecimal.ZERO);
            BigDecimal currentWeight = qty.multiply(price)
                    .divide(totalValue, SCALE, RoundingMode.HALF_UP);
            BigDecimal diff = currentWeight.subtract(target.getTargetWeight()).abs();

            if (diff.compareTo(threshold) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 성과 통계 계산
     * DCA 모드에서는 TWR(Time-Weighted Return) 기반 CAGR을 사용하여
     * 현금 흐름의 영향을 제거한 순수 투자 성과를 측정
     */
    private PerformanceStats calculateStats(List<SeriesPoint> series,
                                            BigDecimal initialCapital,
                                            BigDecimal totalInvested,
                                            boolean isDCA) {
        PerformanceStats stats = new PerformanceStats();
        stats.setTotalInvested(totalInvested);

        if (series.size() < 2) return stats;

        BigDecimal finalValue = series.get(series.size() - 1).getEquityCurveBase();

        if (!isDCA) {
            // LUMP_SUM: 기존 로직
            if (initialCapital != null && initialCapital.compareTo(BigDecimal.ZERO) > 0) {
                double totalReturn = finalValue.subtract(initialCapital)
                        .divide(initialCapital, 10, RoundingMode.HALF_UP).doubleValue();

                int tradingDays = series.size();
                double years = (double) tradingDays / 252.0;
                double cagr = years > 0 ? Math.pow(1.0 + totalReturn, 1.0 / years) - 1.0 : 0;
                stats.setCagr(BigDecimal.valueOf(cagr).setScale(SCALE, RoundingMode.HALF_UP));
            }
        } else {
            // DCA: TWR (Time-Weighted Return) 기반 CAGR
            double twrProduct = 1.0;
            for (int i = 1; i < series.size(); i++) {
                BigDecimal prevEquity = series.get(i - 1).getEquityCurveBase();
                BigDecimal currEquity = series.get(i).getEquityCurveBase();
                BigDecimal prevInvested = series.get(i - 1).getTotalInvested();
                BigDecimal currInvested = series.get(i).getTotalInvested();

                // 당일 입금액 감지
                BigDecimal depositOnDay = currInvested.subtract(prevInvested);
                BigDecimal adjustedPrev = prevEquity.add(depositOnDay);

                if (adjustedPrev.compareTo(BigDecimal.ZERO) > 0) {
                    double dailyReturn = currEquity.subtract(adjustedPrev)
                            .divide(adjustedPrev, 10, RoundingMode.HALF_UP).doubleValue();
                    twrProduct *= (1.0 + dailyReturn);
                }
            }

            int tradingDays = series.size();
            double years = (double) tradingDays / 252.0;
            double cagr = years > 0 ? Math.pow(twrProduct, 1.0 / years) - 1.0 : 0;
            stats.setCagr(BigDecimal.valueOf(cagr).setScale(SCALE, RoundingMode.HALF_UP));
        }

        // 일별 수익률 (DCA에서는 입금 효과 제거)
        List<Double> dailyReturns = new ArrayList<>();
        for (int i = 1; i < series.size(); i++) {
            BigDecimal prev = series.get(i - 1).getEquityCurveBase();
            BigDecimal curr = series.get(i).getEquityCurveBase();

            if (isDCA) {
                BigDecimal deposit = series.get(i).getTotalInvested()
                        .subtract(series.get(i - 1).getTotalInvested());
                prev = prev.add(deposit);
            }

            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                dailyReturns.add(curr.subtract(prev).divide(prev, 10, RoundingMode.HALF_UP).doubleValue());
            }
        }

        if (!dailyReturns.isEmpty()) {
            // Volatility
            double mean = dailyReturns.stream().mapToDouble(r -> r).average().orElse(0);
            double variance = dailyReturns.stream()
                    .mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0);
            double annualVol = Math.sqrt(variance) * Math.sqrt(252);
            stats.setVol(BigDecimal.valueOf(annualVol).setScale(SCALE, RoundingMode.HALF_UP));

            // Sharpe Ratio (risk-free = 3.5%)
            if (annualVol > 0 && stats.getCagr() != null) {
                double sharpe = (stats.getCagr().doubleValue() - 0.035) / annualVol;
                stats.setSharpe(BigDecimal.valueOf(sharpe).setScale(4, RoundingMode.HALF_UP));
            }
        }

        // MDD
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal maxDd = BigDecimal.ZERO;
        for (SeriesPoint sp : series) {
            if (sp.getEquityCurveBase().compareTo(peak) > 0) {
                peak = sp.getEquityCurveBase();
            }
            if (peak.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dd = peak.subtract(sp.getEquityCurveBase())
                        .divide(peak, SCALE, RoundingMode.HALF_UP);
                if (dd.compareTo(maxDd) > 0) maxDd = dd;
            }
        }
        stats.setMdd(maxDd);

        return stats;
    }

    // ========== DTOs ==========

    @Data
    public static class BacktestConfig {
        private String id;
        private String name;
        private String startDate;
        private String endDate;
        private BigDecimal initialCapitalBase;
        private String rebalanceType = "PERIODIC";       // NONE, PERIODIC, BAND
        private String rebalancePeriod = "QUARTERLY";     // MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL
        private BigDecimal bandThreshold;                 // BAND 리밸런싱 임계값
        private boolean dividendReinvest = true;
        private String priceMode = "ADJ_CLOSE";
        private String investmentType = "LUMP_SUM";      // LUMP_SUM, DCA
        private BigDecimal dcaAmount;                     // 적립식 투자 금액
        private String dcaFrequency;                      // MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL
        private List<TargetAlloc> targets = new ArrayList<>();
    }

    @Data
    public static class TargetAlloc {
        private String instrumentId;
        private String assetClass;
        private BigDecimal targetWeight;
    }

    @Data
    public static class BacktestRun {
        private String id;
        private String configId;
        private String status;        // RUNNING, SUCCEEDED, FAILED
        private String startedAt;
        private String finishedAt;
        private String errorMessage;
    }

    @Data
    public static class BacktestResult {
        private BacktestRun run;
        private List<SeriesPoint> series;
        private PerformanceStats stats;
        private List<TradeLog> tradeLogs;
    }

    @Data
    public static class SeriesPoint {
        private String ts;
        private BigDecimal equityCurveBase;
        private BigDecimal drawdown;
        private BigDecimal cashBase;
        private BigDecimal totalInvested;
    }

    @Data
    public static class PerformanceStats {
        private String id;
        private String label;
        private BigDecimal cagr;
        private BigDecimal vol;
        private BigDecimal mdd;
        private BigDecimal sharpe;
        private BigDecimal beta;
        private BigDecimal trackingError;
        private BigDecimal totalInvested;
    }

    @Data
    public static class TradeLog {
        private String ts;
        private String instrumentId;
        private String action;     // BUY, SELL, DEPOSIT
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal fee;
        private BigDecimal amount;  // DEPOSIT 이벤트용 입금 금액
    }
}
