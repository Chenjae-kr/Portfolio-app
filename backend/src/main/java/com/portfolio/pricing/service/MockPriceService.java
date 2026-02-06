package com.portfolio.pricing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Mock 가격 서비스 (개발/테스트용)
 * 
 * - 주요 종목 기준가 제공
 * - 히스토리컬 가격: 기준가에서 일별 ±0.5~2% 변동 시뮬레이션
 * - 추후 ExternalPriceService로 교체 예정
 */
@Service
@Slf4j
public class MockPriceService implements PriceService {

    /** 종목별 기준가 (현재가) */
    private static final Map<String, BigDecimal> BASE_PRICES = Map.ofEntries(
            // 한국 주식
            Map.entry("005930", new BigDecimal("78000")),   // 삼성전자
            Map.entry("000660", new BigDecimal("195000")),  // SK하이닉스
            Map.entry("035420", new BigDecimal("210000")),  // NAVER
            Map.entry("035720", new BigDecimal("62000")),   // 카카오
            Map.entry("051910", new BigDecimal("650000")),  // LG화학
            Map.entry("006400", new BigDecimal("53000")),   // 삼성SDI
            Map.entry("068270", new BigDecimal("350000")),  // 셀트리온
            // 미국 주식
            Map.entry("AAPL", new BigDecimal("245.00")),
            Map.entry("MSFT", new BigDecimal("415.00")),
            Map.entry("GOOGL", new BigDecimal("180.00")),
            Map.entry("AMZN", new BigDecimal("225.00")),
            Map.entry("TSLA", new BigDecimal("390.00")),
            Map.entry("NVDA", new BigDecimal("135.00")),
            // ETF
            Map.entry("VOO", new BigDecimal("520.00")),
            Map.entry("QQQ", new BigDecimal("530.00")),
            Map.entry("SPY", new BigDecimal("565.00")),
            Map.entry("VTI", new BigDecimal("290.00")),
            // 채권 ETF
            Map.entry("TLT", new BigDecimal("92.00")),
            Map.entry("BND", new BigDecimal("72.00"))
    );

    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("100.00");

    @Override
    public BigDecimal getCurrentPrice(String instrumentId) {
        return BASE_PRICES.getOrDefault(instrumentId, DEFAULT_PRICE);
    }

    @Override
    public Map<String, BigDecimal> getCurrentPrices(Iterable<String> instrumentIds) {
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        for (String id : instrumentIds) {
            prices.put(id, getCurrentPrice(id));
        }
        return prices;
    }

    @Override
    public Optional<BigDecimal> getHistoricalPrice(String instrumentId, LocalDate date) {
        BigDecimal price = simulateHistoricalPrice(instrumentId, date);
        return Optional.of(price);
    }

    @Override
    public Map<LocalDate, BigDecimal> getHistoricalPrices(String instrumentId, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> prices = new LinkedHashMap<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            // 주말 제외 (토/일)
            if (current.getDayOfWeek().getValue() <= 5) {
                prices.put(current, simulateHistoricalPrice(instrumentId, current));
            }
            current = current.plusDays(1);
        }
        return prices;
    }

    @Override
    public BigDecimal getFxRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) return BigDecimal.ONE;

        // Mock 환율
        if ("USD".equals(fromCurrency) && "KRW".equals(toCurrency)) {
            return new BigDecimal("1350.00");
        }
        if ("KRW".equals(fromCurrency) && "USD".equals(toCurrency)) {
            return new BigDecimal("0.000741");
        }
        if ("EUR".equals(fromCurrency) && "KRW".equals(toCurrency)) {
            return new BigDecimal("1480.00");
        }
        if ("JPY".equals(fromCurrency) && "KRW".equals(toCurrency)) {
            return new BigDecimal("9.10");
        }

        return BigDecimal.ONE;
    }

    /**
     * 히스토리컬 가격 시뮬레이션
     * 
     * 결정론적(deterministic) 시뮬레이션 - 같은 종목+날짜 = 항상 같은 가격
     * 현재가에서 과거로 갈수록 약간 낮은 가격 (장기 상승 추세 시뮬레이션)
     */
    private BigDecimal simulateHistoricalPrice(String instrumentId, LocalDate date) {
        BigDecimal basePrice = BASE_PRICES.getOrDefault(instrumentId, DEFAULT_PRICE);
        LocalDate today = LocalDate.now();

        if (!date.isBefore(today)) {
            return basePrice;
        }

        long daysBefore = ChronoUnit.DAYS.between(date, today);

        // 결정론적 일별 변동 생성 (hash 기반)
        int seed = Objects.hash(instrumentId, date.toString());
        Random rng = new Random(seed);

        // 장기 추세: 연 8% 상승 가정 → 일별 약 0.03%
        double trendFactor = Math.exp(-0.0003 * daysBefore);

        // 일별 노이즈: ±1.5% 범위
        double noiseFactor = 1.0 + (rng.nextGaussian() * 0.015);

        double historicalMultiplier = trendFactor * noiseFactor;
        // 극단적 변동 방지 (0.5배 ~ 1.5배)
        historicalMultiplier = Math.max(0.5, Math.min(1.5, historicalMultiplier));

        return basePrice.multiply(BigDecimal.valueOf(historicalMultiplier))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
