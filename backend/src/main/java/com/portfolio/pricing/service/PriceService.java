package com.portfolio.pricing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * 가격 데이터 조회 인터페이스
 * 
 * 구현체:
 * - MockPriceService: 개발용 Mock 가격
 * - (향후) ExternalPriceService: Alpha Vantage / Yahoo Finance / KRX API 연동
 */
public interface PriceService {

    /**
     * 종목의 현재 가격 조회
     */
    BigDecimal getCurrentPrice(String instrumentId);

    /**
     * 여러 종목의 현재 가격 일괄 조회
     */
    Map<String, BigDecimal> getCurrentPrices(Iterable<String> instrumentIds);

    /**
     * 특정 날짜의 종가 조회
     */
    Optional<BigDecimal> getHistoricalPrice(String instrumentId, LocalDate date);

    /**
     * 기간 내 일별 종가 조회 (성과 분석용)
     */
    Map<LocalDate, BigDecimal> getHistoricalPrices(String instrumentId, LocalDate from, LocalDate to);

    /**
     * 환율 조회 (예: USD/KRW)
     */
    BigDecimal getFxRate(String fromCurrency, String toCurrency);
}
