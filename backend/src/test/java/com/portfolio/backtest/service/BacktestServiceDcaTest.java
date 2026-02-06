package com.portfolio.backtest.service;

import com.portfolio.backtest.service.BacktestService.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("BacktestService DCA 테스트")
class BacktestServiceDcaTest {

    @Autowired
    private BacktestService backtestService;

    private BacktestConfig createBaseConfig() {
        BacktestConfig config = new BacktestConfig();
        config.setName("DCA Test");
        config.setStartDate("2023-01-01");
        config.setEndDate("2023-12-31");
        config.setRebalanceType("NONE");
        config.setDividendReinvest(true);
        config.setPriceMode("ADJ_CLOSE");

        TargetAlloc spy = new TargetAlloc();
        spy.setInstrumentId("SPY");
        spy.setAssetClass("EQUITY");
        spy.setTargetWeight(new BigDecimal("0.6"));

        TargetAlloc bnd = new TargetAlloc();
        bnd.setInstrumentId("BND");
        bnd.setAssetClass("BOND");
        bnd.setTargetWeight(new BigDecimal("0.4"));

        config.setTargets(List.of(spy, bnd));
        return config;
    }

    @Test
    @DisplayName("LUMP_SUM 백테스트 - 기존 동작 유지 (하위 호환성)")
    void lumpSum_backwardCompatibility() {
        BacktestConfig config = createBaseConfig();
        config.setInitialCapitalBase(new BigDecimal("100000000"));
        // investmentType 미설정 → 기본값 LUMP_SUM

        BacktestRun run = backtestService.runBacktest(null, config);

        assertThat(run.getStatus()).isEqualTo("SUCCEEDED");

        BacktestResult result = backtestService.getResult(run.getId());
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).isNotEmpty();
        assertThat(result.getStats()).isNotNull();
        assertThat(result.getStats().getCagr()).isNotNull();

        // totalInvested = initialCapital (일시불)
        assertThat(result.getStats().getTotalInvested())
                .isEqualByComparingTo(new BigDecimal("100000000"));

        // 시리즈의 totalInvested는 항상 동일 (추가 입금 없음)
        BigDecimal firstInvested = result.getSeries().get(0).getTotalInvested();
        BigDecimal lastInvested = result.getSeries().get(result.getSeries().size() - 1).getTotalInvested();
        assertThat(firstInvested).isEqualByComparingTo(lastInvested);
    }

    @Test
    @DisplayName("DCA 백테스트 - 월별 적립 기본 플로우")
    void dca_monthlyDeposit() {
        BacktestConfig config = createBaseConfig();
        config.setInitialCapitalBase(BigDecimal.ZERO);
        config.setInvestmentType("DCA");
        config.setDcaAmount(new BigDecimal("1000000"));
        config.setDcaFrequency("MONTHLY");

        BacktestRun run = backtestService.runBacktest(null, config);

        assertThat(run.getStatus()).isEqualTo("SUCCEEDED");

        BacktestResult result = backtestService.getResult(run.getId());
        assertThat(result.getSeries()).isNotEmpty();

        // DEPOSIT 거래 로그 확인
        long depositCount = result.getTradeLogs().stream()
                .filter(log -> "DEPOSIT".equals(log.getAction()))
                .count();
        assertThat(depositCount).isGreaterThanOrEqualTo(11); // 약 11~12회 (1년간 월별)

        // totalInvested가 증가하는지 확인
        BigDecimal firstInvested = result.getSeries().get(0).getTotalInvested();
        BigDecimal lastInvested = result.getSeries().get(result.getSeries().size() - 1).getTotalInvested();
        assertThat(lastInvested).isGreaterThan(firstInvested);

        // DEPOSIT 로그에 amount가 있는지 확인
        result.getTradeLogs().stream()
                .filter(log -> "DEPOSIT".equals(log.getAction()))
                .forEach(log -> {
                    assertThat(log.getAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
                    assertThat(log.getInstrumentId()).isNull();
                });
    }

    @Test
    @DisplayName("DCA 백테스트 - 초기 투자금 + 적립")
    void dca_withInitialCapital() {
        BacktestConfig config = createBaseConfig();
        config.setInitialCapitalBase(new BigDecimal("10000000"));
        config.setInvestmentType("DCA");
        config.setDcaAmount(new BigDecimal("1000000"));
        config.setDcaFrequency("MONTHLY");

        BacktestRun run = backtestService.runBacktest(null, config);

        assertThat(run.getStatus()).isEqualTo("SUCCEEDED");

        BacktestResult result = backtestService.getResult(run.getId());

        // totalInvested = 초기 10,000,000 + 월별 적립
        BigDecimal totalInvested = result.getStats().getTotalInvested();
        assertThat(totalInvested).isGreaterThan(new BigDecimal("10000000"));

        // 첫날 totalInvested는 초기 투자금
        BigDecimal firstInvested = result.getSeries().get(0).getTotalInvested();
        assertThat(firstInvested).isEqualByComparingTo(new BigDecimal("10000000.00"));

        // BUY 거래가 첫날부터 발생 (초기 투자금 배분)
        long buyCount = result.getTradeLogs().stream()
                .filter(log -> "BUY".equals(log.getAction()))
                .count();
        assertThat(buyCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("DCA + 분기 리밸런싱 조합")
    void dca_withQuarterlyRebalance() {
        BacktestConfig config = createBaseConfig();
        config.setInitialCapitalBase(BigDecimal.ZERO);
        config.setInvestmentType("DCA");
        config.setDcaAmount(new BigDecimal("1000000"));
        config.setDcaFrequency("MONTHLY");
        config.setRebalanceType("PERIODIC");
        config.setRebalancePeriod("QUARTERLY");

        BacktestRun run = backtestService.runBacktest(null, config);

        assertThat(run.getStatus()).isEqualTo("SUCCEEDED");

        BacktestResult result = backtestService.getResult(run.getId());

        // DEPOSIT과 BUY/SELL 모두 존재
        long depositCount = result.getTradeLogs().stream()
                .filter(log -> "DEPOSIT".equals(log.getAction()))
                .count();
        long tradeCount = result.getTradeLogs().stream()
                .filter(log -> "BUY".equals(log.getAction()) || "SELL".equals(log.getAction()))
                .count();

        assertThat(depositCount).isGreaterThanOrEqualTo(11);
        assertThat(tradeCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("DCA 초기 투자금 0원 - 첫날 거래 없음")
    void dca_zeroInitialCapital() {
        BacktestConfig config = createBaseConfig();
        config.setInitialCapitalBase(BigDecimal.ZERO);
        config.setInvestmentType("DCA");
        config.setDcaAmount(new BigDecimal("500000"));
        config.setDcaFrequency("MONTHLY");

        BacktestRun run = backtestService.runBacktest(null, config);

        assertThat(run.getStatus()).isEqualTo("SUCCEEDED");

        BacktestResult result = backtestService.getResult(run.getId());

        // 첫날 equity는 0
        assertThat(result.getSeries().get(0).getEquityCurveBase())
                .isEqualByComparingTo(BigDecimal.ZERO);

        // 첫 거래는 DEPOSIT이어야 함 (첫날이 아닌 다음 월)
        assertThat(result.getTradeLogs()).isNotEmpty();
        TradeLog firstLog = result.getTradeLogs().get(0);
        assertThat(firstLog.getAction()).isEqualTo("DEPOSIT");
    }

    @Test
    @DisplayName("시리즈 totalInvested 필드 - 단조 증가 확인")
    void dca_seriesTotalInvestedMonotonicallyIncreasing() {
        BacktestConfig config = createBaseConfig();
        config.setInitialCapitalBase(BigDecimal.ZERO);
        config.setInvestmentType("DCA");
        config.setDcaAmount(new BigDecimal("1000000"));
        config.setDcaFrequency("MONTHLY");

        BacktestRun run = backtestService.runBacktest(null, config);
        BacktestResult result = backtestService.getResult(run.getId());

        // totalInvested는 단조 증가 (감소하지 않음)
        for (int i = 1; i < result.getSeries().size(); i++) {
            BigDecimal prev = result.getSeries().get(i - 1).getTotalInvested();
            BigDecimal curr = result.getSeries().get(i).getTotalInvested();
            assertThat(curr).isGreaterThanOrEqualTo(prev);
        }

        // 모든 SeriesPoint에 totalInvested가 존재
        result.getSeries().forEach(sp ->
                assertThat(sp.getTotalInvested()).isNotNull());
    }

    @Test
    @DisplayName("DCA TWR CAGR 계산 - null이 아닌 값 반환")
    void dca_twrCalculation() {
        BacktestConfig config = createBaseConfig();
        config.setInitialCapitalBase(BigDecimal.ZERO);
        config.setInvestmentType("DCA");
        config.setDcaAmount(new BigDecimal("1000000"));
        config.setDcaFrequency("MONTHLY");

        BacktestRun run = backtestService.runBacktest(null, config);
        BacktestResult result = backtestService.getResult(run.getId());

        assertThat(result.getStats().getCagr()).isNotNull();
        assertThat(result.getStats().getVol()).isNotNull();
        assertThat(result.getStats().getMdd()).isNotNull();
    }
}
