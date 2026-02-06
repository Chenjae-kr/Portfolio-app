package com.portfolio.analytics.service;

import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.exception.ErrorCode;
import com.portfolio.portfolio.repository.PortfolioRepository;
import com.portfolio.valuation.service.ValuationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 포트폴리오 성과 분석 서비스
 * 
 * - TWR (Time-Weighted Return): 현금흐름 영향 배제한 순수 투자 수익률
 * - 리스크 지표: Volatility, MDD (Maximum Drawdown), Sharpe Ratio
 * - 기간별 수익률 시계열 (일/주/월)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceService {

    private final ValuationService valuationService;
    private final PortfolioRepository portfolioRepository;

    private static final BigDecimal RISK_FREE_RATE_ANNUAL = new BigDecimal("0.035"); // 연 3.5%
    private static final int TRADING_DAYS_PER_YEAR = 252;

    /**
     * 포트폴리오 성과 데이터 계산
     */
    @Transactional(readOnly = true)
    public PerformanceResult calculatePerformance(String portfolioId, String workspaceId,
                                                   LocalDate from, LocalDate to,
                                                   String metric, String frequency) {
        portfolioRepository.findByIdAndWorkspaceId(portfolioId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 1. 기간 내 일별 포트폴리오 가치 시계열 생성
        List<DailyValue> dailyValues = calculateDailyValues(portfolioId, from, to);

        if (dailyValues.size() < 2) {
            return emptyResult(portfolioId, from, to, metric, frequency);
        }

        // 2. TWR 수익률 시계열 계산
        List<DataPoint> cumulativeReturns = calculateTWR(dailyValues);

        // 3. 빈도별 리샘플링
        List<DataPoint> resampled = resample(cumulativeReturns, frequency);

        // 4. 리스크 지표 계산
        RiskMetrics riskMetrics = calculateRiskMetrics(dailyValues);

        PerformanceResult result = new PerformanceResult();
        result.portfolioId = portfolioId;
        result.from = from.toString();
        result.to = to.toString();
        result.metric = metric;
        result.frequency = frequency;
        result.dataPoints = resampled;
        result.stats = riskMetrics;
        result.benchmarks = generateBenchmarks(from, to);

        return result;
    }

    /**
     * 일별 포트폴리오 가치 계산
     */
    private List<DailyValue> calculateDailyValues(String portfolioId, LocalDate from, LocalDate to) {
        List<DailyValue> values = new ArrayList<>();
        LocalDate current = from;

        while (!current.isAfter(to)) {
            // 주말 제외
            if (current.getDayOfWeek().getValue() <= 5) {
                BigDecimal totalValue = valuationService.calculateValueAtDate(portfolioId, current);
                BigDecimal cashFlow = valuationService.getCashFlowAtDate(portfolioId, current);

                DailyValue dv = new DailyValue();
                dv.date = current;
                dv.totalValue = totalValue;
                dv.cashFlow = cashFlow;
                values.add(dv);
            }
            current = current.plusDays(1);
        }

        return values;
    }

    /**
     * TWR (Time-Weighted Return) 계산
     * 
     * TWR = ∏(1 + r_i) - 1
     * r_i = (V_end - V_begin - CF) / V_begin
     * 
     * 현금흐름이 있는 날은 구간을 분리하여 계산
     */
    private List<DataPoint> calculateTWR(List<DailyValue> dailyValues) {
        List<DataPoint> points = new ArrayList<>();
        BigDecimal cumulativeReturn = BigDecimal.ONE; // 1.0 = 0% return

        // 첫 날은 기준점
        DataPoint first = new DataPoint();
        first.date = dailyValues.get(0).date.toString();
        first.value = BigDecimal.ZERO;
        points.add(first);

        for (int i = 1; i < dailyValues.size(); i++) {
            DailyValue prev = dailyValues.get(i - 1);
            DailyValue curr = dailyValues.get(i);

            // 전일 가치가 0이면 스킵
            if (prev.totalValue.compareTo(BigDecimal.ZERO) <= 0) {
                DataPoint dp = new DataPoint();
                dp.date = curr.date.toString();
                dp.value = cumulativeReturn.subtract(BigDecimal.ONE)
                        .setScale(6, RoundingMode.HALF_UP);
                points.add(dp);
                continue;
            }

            // 일별 수익률: (오늘 가치 - 어제 가치 - 오늘 현금흐름) / 어제 가치
            BigDecimal periodReturn = curr.totalValue
                    .subtract(prev.totalValue)
                    .subtract(curr.cashFlow)
                    .divide(prev.totalValue, 10, RoundingMode.HALF_UP);

            cumulativeReturn = cumulativeReturn.multiply(
                    BigDecimal.ONE.add(periodReturn));

            DataPoint dp = new DataPoint();
            dp.date = curr.date.toString();
            dp.value = cumulativeReturn.subtract(BigDecimal.ONE)
                    .setScale(6, RoundingMode.HALF_UP);
            points.add(dp);
        }

        return points;
    }

    /**
     * 리스크 지표 계산
     */
    private RiskMetrics calculateRiskMetrics(List<DailyValue> dailyValues) {
        RiskMetrics metrics = new RiskMetrics();

        if (dailyValues.size() < 2) return metrics;

        // 일별 수익률 계산
        List<Double> dailyReturns = new ArrayList<>();
        for (int i = 1; i < dailyValues.size(); i++) {
            DailyValue prev = dailyValues.get(i - 1);
            DailyValue curr = dailyValues.get(i);

            if (prev.totalValue.compareTo(BigDecimal.ZERO) > 0) {
                double ret = curr.totalValue
                        .subtract(prev.totalValue)
                        .subtract(curr.cashFlow)
                        .divide(prev.totalValue, 10, RoundingMode.HALF_UP)
                        .doubleValue();
                dailyReturns.add(ret);
            }
        }

        if (dailyReturns.isEmpty()) return metrics;

        // 총 수익률
        double totalReturn = 1.0;
        for (double r : dailyReturns) {
            totalReturn *= (1.0 + r);
        }
        totalReturn -= 1.0;
        metrics.totalReturn = BigDecimal.valueOf(totalReturn).setScale(6, RoundingMode.HALF_UP);

        // 연환산 수익률 (CAGR)
        int days = dailyReturns.size();
        if (days > 0 && totalReturn > -1.0) {
            double cagr = Math.pow(1.0 + totalReturn, (double) TRADING_DAYS_PER_YEAR / days) - 1.0;
            metrics.cagr = BigDecimal.valueOf(cagr).setScale(6, RoundingMode.HALF_UP);
        }

        // 변동성 (Volatility) = 일별 수익률 표준편차 × √252
        double mean = dailyReturns.stream().mapToDouble(r -> r).average().orElse(0);
        double variance = dailyReturns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .average().orElse(0);
        double dailyVol = Math.sqrt(variance);
        double annualVol = dailyVol * Math.sqrt(TRADING_DAYS_PER_YEAR);
        metrics.volatility = BigDecimal.valueOf(annualVol).setScale(6, RoundingMode.HALF_UP);

        // MDD (Maximum Drawdown)
        double peak = 0;
        double maxDrawdown = 0;
        double cumulative = 1.0;
        for (double r : dailyReturns) {
            cumulative *= (1.0 + r);
            if (cumulative > peak) peak = cumulative;
            double drawdown = (peak - cumulative) / peak;
            if (drawdown > maxDrawdown) maxDrawdown = drawdown;
        }
        metrics.mdd = BigDecimal.valueOf(maxDrawdown).setScale(6, RoundingMode.HALF_UP);

        // Sharpe Ratio = (연환산 수익률 - 무위험이자율) / 변동성
        if (annualVol > 0 && metrics.cagr != null) {
            double sharpe = (metrics.cagr.doubleValue() - RISK_FREE_RATE_ANNUAL.doubleValue()) / annualVol;
            metrics.sharpe = BigDecimal.valueOf(sharpe).setScale(4, RoundingMode.HALF_UP);
        }

        return metrics;
    }

    /**
     * 빈도별 리샘플링 (DAILY, WEEKLY, MONTHLY)
     */
    private List<DataPoint> resample(List<DataPoint> points, String frequency) {
        if ("DAILY".equals(frequency) || points.isEmpty()) {
            return points;
        }

        List<DataPoint> resampled = new ArrayList<>();
        String prevKey = null;

        for (DataPoint dp : points) {
            LocalDate date = LocalDate.parse(dp.date);
            String key;

            if ("WEEKLY".equals(frequency)) {
                // 주의 마지막 거래일
                int weekOfYear = date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
                key = date.getYear() + "-W" + weekOfYear;
            } else { // MONTHLY
                key = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            }

            if (!key.equals(prevKey)) {
                // 이전 구간의 마지막 데이터 포인트를 사용
                if (!resampled.isEmpty()) {
                    // 이미 추가됨
                }
                prevKey = key;
            }

            // 마지막 포인트를 해당 기간의 대표값으로 사용
            if (resampled.isEmpty() || !key.equals(getKey(resampled.get(resampled.size() - 1), frequency))) {
                resampled.add(dp);
            } else {
                resampled.set(resampled.size() - 1, dp);
            }
        }

        return resampled;
    }

    private String getKey(DataPoint dp, String frequency) {
        LocalDate date = LocalDate.parse(dp.date);
        if ("WEEKLY".equals(frequency)) {
            int weekOfYear = date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
            return date.getYear() + "-W" + weekOfYear;
        }
        return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
    }

    /**
     * Mock 벤치마크 수익률 생성 (결정론적 시뮬레이션)
     */
    private List<BenchmarkSeries> generateBenchmarks(LocalDate from, LocalDate to) {
        List<BenchmarkSeries> benchmarks = new ArrayList<>();
        benchmarks.add(generateSingleBenchmark("KOSPI", "KOSPI 200", from, to, 0.08, 0.18));
        benchmarks.add(generateSingleBenchmark("SP500", "S&P 500", from, to, 0.10, 0.15));
        return benchmarks;
    }

    private BenchmarkSeries generateSingleBenchmark(String id, String label,
            LocalDate from, LocalDate to, double annualReturn, double annualVol) {
        BenchmarkSeries series = new BenchmarkSeries();
        series.id = id;
        series.label = label;

        double dailyReturn = Math.pow(1 + annualReturn, 1.0 / TRADING_DAYS_PER_YEAR) - 1;
        double dailyVol = annualVol / Math.sqrt(TRADING_DAYS_PER_YEAR);

        List<DataPoint> points = new ArrayList<>();
        double cumulative = 1.0;

        DataPoint first = new DataPoint();
        first.date = from.toString();
        first.value = BigDecimal.ZERO;
        points.add(first);

        LocalDate current = from.plusDays(1);
        List<Double> dailyReturns = new ArrayList<>();

        while (!current.isAfter(to)) {
            if (current.getDayOfWeek().getValue() <= 5) {
                int seed = Objects.hash(id, current.toString());
                Random rng = new Random(seed);
                double noise = rng.nextGaussian() * dailyVol;
                double dayRet = dailyReturn + noise;
                dailyReturns.add(dayRet);
                cumulative *= (1 + dayRet);

                DataPoint dp = new DataPoint();
                dp.date = current.toString();
                dp.value = BigDecimal.valueOf(cumulative - 1.0).setScale(6, RoundingMode.HALF_UP);
                points.add(dp);
            }
            current = current.plusDays(1);
        }

        series.dataPoints = points;

        // Calculate stats
        RiskMetrics metrics = new RiskMetrics();
        double totalRet = cumulative - 1.0;
        metrics.totalReturn = BigDecimal.valueOf(totalRet).setScale(6, RoundingMode.HALF_UP);

        int days = dailyReturns.size();
        if (days > 0 && totalRet > -1.0) {
            metrics.cagr = BigDecimal.valueOf(
                    Math.pow(1 + totalRet, (double) TRADING_DAYS_PER_YEAR / days) - 1)
                    .setScale(6, RoundingMode.HALF_UP);
        }
        metrics.volatility = BigDecimal.valueOf(annualVol).setScale(6, RoundingMode.HALF_UP);

        double bmPeak = 0, maxDD = 0, cum = 1.0;
        for (double r : dailyReturns) {
            cum *= (1 + r);
            if (cum > bmPeak) bmPeak = cum;
            double dd = bmPeak > 0 ? (bmPeak - cum) / bmPeak : 0;
            if (dd > maxDD) maxDD = dd;
        }
        metrics.mdd = BigDecimal.valueOf(maxDD).setScale(6, RoundingMode.HALF_UP);

        if (annualVol > 0 && metrics.cagr != null) {
            metrics.sharpe = BigDecimal.valueOf(
                    (metrics.cagr.doubleValue() - RISK_FREE_RATE_ANNUAL.doubleValue()) / annualVol)
                    .setScale(4, RoundingMode.HALF_UP);
        }

        series.stats = metrics;
        return series;
    }

    private PerformanceResult emptyResult(String portfolioId, LocalDate from, LocalDate to,
                                          String metric, String frequency) {
        PerformanceResult result = new PerformanceResult();
        result.portfolioId = portfolioId;
        result.from = from.toString();
        result.to = to.toString();
        result.metric = metric;
        result.frequency = frequency;
        result.dataPoints = Collections.emptyList();
        result.stats = new RiskMetrics();
        return result;
    }

    // ===== DTOs =====

    private static class DailyValue {
        LocalDate date;
        BigDecimal totalValue;
        BigDecimal cashFlow;
    }

    public static class PerformanceResult {
        public String portfolioId;
        public String from;
        public String to;
        public String metric;
        public String frequency;
        public List<DataPoint> dataPoints;
        public RiskMetrics stats;
        public List<BenchmarkSeries> benchmarks;
    }

    public static class BenchmarkSeries {
        public String id;
        public String label;
        public List<DataPoint> dataPoints;
        public RiskMetrics stats;
    }

    public static class DataPoint {
        public String date;
        public BigDecimal value;
    }

    public static class RiskMetrics {
        public BigDecimal totalReturn = BigDecimal.ZERO;
        public BigDecimal cagr = BigDecimal.ZERO;
        public BigDecimal volatility = BigDecimal.ZERO;
        public BigDecimal mdd = BigDecimal.ZERO;
        public BigDecimal sharpe = BigDecimal.ZERO;
    }
}
