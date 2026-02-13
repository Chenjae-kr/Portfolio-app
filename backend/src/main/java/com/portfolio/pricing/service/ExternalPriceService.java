package com.portfolio.pricing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.pricing.entity.Instrument;
import com.portfolio.pricing.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 외부 시세 API 서비스.
 * - 해외(미국 포함): Alpha Vantage
 * - 국내(한국): KRX 정보데이터시스템
 *
 * 실패 시 MockPriceService로 자동 폴백한다.
 */
@Service
@Primary
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.pricing.external", name = "enabled", havingValue = "true")
public class ExternalPriceService implements PriceService {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private final InstrumentRepository instrumentRepository;
    private final MockPriceService fallbackPriceService;
    private final ObjectMapper objectMapper;
    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${app.pricing.external.alpha-vantage.base-url:https://www.alphavantage.co}")
    private String alphaBaseUrl;

    @Value("${app.pricing.external.alpha-vantage.api-key:}")
    private String alphaApiKey;

    @Value("${app.pricing.external.krx.base-url:http://data.krx.co.kr}")
    private String krxBaseUrl;

    @Value("${app.pricing.external.timeout-ms:3000}")
    private long timeoutMs;

    private RestTemplate restTemplate() {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public BigDecimal getCurrentPrice(String instrumentId) {
        return resolveInstrument(instrumentId)
                .map(instrument -> fetchExternalCurrentPrice(instrument, instrumentId)
                        .orElseGet(() -> fallbackPriceService.getCurrentPrice(instrumentId)))
                .orElseGet(() -> fallbackPriceService.getCurrentPrice(instrumentId));
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
        return resolveInstrument(instrumentId)
                .flatMap(inst -> fetchAlphaHistoricalPrice(inst.getTicker(), date))
                .or(() -> fallbackPriceService.getHistoricalPrice(instrumentId, date));
    }

    @Override
    public Map<LocalDate, BigDecimal> getHistoricalPrices(String instrumentId, LocalDate from, LocalDate to) {
        Optional<Instrument> instrument = resolveInstrument(instrumentId);
        if (instrument.isEmpty()) {
            return fallbackPriceService.getHistoricalPrices(instrumentId, from, to);
        }

        Optional<Map<LocalDate, BigDecimal>> external = fetchAlphaHistoricalPrices(instrument.get().getTicker(), from, to);
        return external.orElseGet(() -> fallbackPriceService.getHistoricalPrices(instrumentId, from, to));
    }

    @Override
    public BigDecimal getFxRate(String fromCurrency, String toCurrency) {
        if (Objects.equals(fromCurrency, toCurrency)) {
            return BigDecimal.ONE;
        }

        try {
            String pair = fromCurrency + toCurrency;
            String url = alphaBaseUrl + "/query?function=CURRENCY_EXCHANGE_RATE&from_currency=" + fromCurrency
                    + "&to_currency=" + toCurrency + "&apikey=" + alphaApiKey;

            String body = restTemplate().getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(body);
            String rate = root.path("Realtime Currency Exchange Rate")
                    .path("5. Exchange Rate")
                    .asText(null);
            if (rate != null && !rate.isBlank()) {
                return new BigDecimal(rate).setScale(6, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            log.warn("Alpha Vantage FX fetch failed: {}->{}, fallback mock. cause={}", fromCurrency, toCurrency, e.getMessage());
        }
        return fallbackPriceService.getFxRate(fromCurrency, toCurrency);
    }

    private Optional<BigDecimal> fetchExternalCurrentPrice(Instrument instrument, String instrumentId) {
        try {
            boolean isKoreanStock = "KR".equalsIgnoreCase(instrument.getCountry())
                    || (instrument.getTicker() != null && instrument.getTicker().matches("\\d{6}"));
            if (isKoreanStock) {
                return fetchKrxCurrentPrice(instrument.getTicker());
            }
            return fetchAlphaCurrentPrice(instrument.getTicker());
        } catch (Exception e) {
            log.warn("External price fetch failed for instrument={}, ticker={}, cause={}",
                    instrumentId, instrument.getTicker(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> fetchAlphaCurrentPrice(String ticker) {
        if (ticker == null || ticker.isBlank() || alphaApiKey == null || alphaApiKey.isBlank()) {
            return Optional.empty();
        }
        String url = alphaBaseUrl + "/query?function=GLOBAL_QUOTE&symbol=" + ticker + "&apikey=" + alphaApiKey;
        String body = restTemplate().getForObject(url, String.class);
        JsonNode root = objectMapper.readTree(body);
        String rawPrice = root.path("Global Quote").path("05. price").asText(null);
        if (rawPrice == null || rawPrice.isBlank()) return Optional.empty();

        return Optional.of(new BigDecimal(rawPrice).setScale(2, RoundingMode.HALF_UP));
    }

    private Optional<BigDecimal> fetchKrxCurrentPrice(String ticker) {
        if (ticker == null || ticker.isBlank()) return Optional.empty();

        String url = krxBaseUrl + "/comm/bldAttendant/getJsonData.cmd";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("bld", "dbms/MDC/STAT/standard/MDCSTAT01501");
        form.add("locale", "ko_KR");
        form.add("isuCd", ticker);
        form.add("isuCd2", ticker);
        form.add("strtDd", LocalDate.now().minusDays(5).format(YYYYMMDD));
        form.add("endDd", LocalDate.now().format(YYYYMMDD));
        form.add("share", "1");
        form.add("money", "1");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Optional.empty();
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode outBlock = root.path("OutBlock_1");
        if (!outBlock.isArray() || outBlock.isEmpty()) {
            return Optional.empty();
        }

        JsonNode last = outBlock.get(outBlock.size() - 1);
        String closePrice = Optional.ofNullable(last.path("TDD_CLSPRC").asText(null))
                .orElse(last.path("CLSPRC").asText(null));

        if (closePrice == null || closePrice.isBlank()) return Optional.empty();
        String normalized = closePrice.replace(",", "").trim();
        return Optional.of(new BigDecimal(normalized));
    }

    private Optional<BigDecimal> fetchAlphaHistoricalPrice(String ticker, LocalDate date) {
        return fetchAlphaHistoricalPrices(ticker, date, date).map(map -> map.get(date));
    }

    private Optional<Map<LocalDate, BigDecimal>> fetchAlphaHistoricalPrices(String ticker, LocalDate from, LocalDate to) {
        if (ticker == null || ticker.isBlank() || alphaApiKey == null || alphaApiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            String url = alphaBaseUrl + "/query?function=TIME_SERIES_DAILY_ADJUSTED&outputsize=full&symbol="
                    + ticker + "&apikey=" + alphaApiKey;
            String body = restTemplate().getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode series = root.path("Time Series (Daily)");
            if (!series.isObject()) return Optional.empty();

            Map<LocalDate, BigDecimal> result = new LinkedHashMap<>();
            LocalDate cursor = from;
            while (!cursor.isAfter(to)) {
                JsonNode daily = series.path(cursor.toString());
                if (!daily.isMissingNode()) {
                    String close = daily.path("5. adjusted close").asText(null);
                    if (close == null || close.isBlank()) {
                        close = daily.path("4. close").asText(null);
                    }
                    if (close != null && !close.isBlank()) {
                        result.put(cursor, new BigDecimal(close).setScale(2, RoundingMode.HALF_UP));
                    }
                }
                cursor = cursor.plusDays(1);
            }

            return result.isEmpty() ? Optional.empty() : Optional.of(result);
        } catch (Exception e) {
            log.warn("Alpha historical fetch failed: ticker={}, cause={}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Instrument> resolveInstrument(String instrumentId) {
        Optional<Instrument> byId = instrumentRepository.findById(instrumentId);
        if (byId.isPresent()) return byId;
        return instrumentRepository.findByTicker(instrumentId);
    }
}
