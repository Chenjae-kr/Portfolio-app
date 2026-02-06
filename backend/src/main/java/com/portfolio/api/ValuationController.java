package com.portfolio.api;

import com.portfolio.portfolio.entity.Portfolio;
import com.portfolio.portfolio.service.PortfolioService;
import com.portfolio.infra.init.DataInitializer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/v1/portfolios")
@RequiredArgsConstructor
public class ValuationController {

    private final PortfolioService portfolioService;

    private static final String DEFAULT_WORKSPACE_ID = DataInitializer.DEFAULT_WORKSPACE_ID;

    @GetMapping("/{id}/valuation")
    public ResponseEntity<?> getValuation(
            @PathVariable String id,
            @RequestParam(defaultValue = "REALTIME") String mode,
            @RequestParam(name = "as_of", required = false) String asOf
    ) {
        try {
            // 포트폴리오 존재 여부 확인
            Portfolio portfolio = portfolioService.findById(id, DEFAULT_WORKSPACE_ID);

            // 기본 빈 Valuation 응답 반환 (아직 거래/포지션이 없으므로)
            ValuationResponse valuation = new ValuationResponse();
            valuation.setPortfolioId(id);
            valuation.setAsOf(asOf != null ? asOf : LocalDate.now().toString());
            valuation.setMode(mode);
            valuation.setCurrency(portfolio.getBaseCurrency());
            valuation.setTotalValueBase(BigDecimal.ZERO);
            valuation.setCashValueBase(BigDecimal.ZERO);
            valuation.setDayPnlBase(BigDecimal.ZERO);
            valuation.setTotalPnlBase(BigDecimal.ZERO);
            valuation.setPositions(Collections.emptyList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", valuation);
            response.put("meta", Map.of("timestamp", Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), "NOT_FOUND", 404);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), "INTERNAL_ERROR", 500);
        }
    }

    @GetMapping("/{id}/performance")
    public ResponseEntity<?> getPerformance(
            @PathVariable String id,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "TWR") String metric,
            @RequestParam(defaultValue = "DAILY") String frequency
    ) {
        try {
            // 포트폴리오 존재 여부 확인
            portfolioService.findById(id, DEFAULT_WORKSPACE_ID);

            // 빈 성과 데이터 반환
            PerformanceResponse performance = new PerformanceResponse();
            performance.setPortfolioId(id);
            performance.setFrom(from);
            performance.setTo(to);
            performance.setMetric(metric);
            performance.setFrequency(frequency);
            performance.setDataPoints(Collections.emptyList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", performance);
            response.put("meta", Map.of("timestamp", Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), "NOT_FOUND", 404);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), "INTERNAL_ERROR", 500);
        }
    }

    private ResponseEntity<?> createErrorResponse(String message, String code, int status) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);

        Map<String, Object> response = new HashMap<>();
        response.put("data", null);
        response.put("meta", Map.of("timestamp", Instant.now().toString()));
        response.put("error", error);

        return ResponseEntity.status(status).body(response);
    }

    @Data
    public static class ValuationResponse {
        private String portfolioId;
        private String asOf;
        private String mode;
        private String currency;
        private BigDecimal totalValueBase;
        private BigDecimal cashValueBase;
        private BigDecimal dayPnlBase;
        private BigDecimal totalPnlBase;
        private List<PositionValuation> positions;
    }

    @Data
    public static class PositionValuation {
        private String instrumentId;
        private String ticker;
        private String name;
        private BigDecimal quantity;
        private BigDecimal avgCost;
        private BigDecimal currentPrice;
        private BigDecimal marketValue;
        private BigDecimal marketValueBase;
        private BigDecimal dayPnl;
        private BigDecimal totalPnl;
        private BigDecimal weight;
    }

    @Data
    public static class PerformanceResponse {
        private String portfolioId;
        private String from;
        private String to;
        private String metric;
        private String frequency;
        private List<DataPoint> dataPoints;
    }

    @Data
    public static class DataPoint {
        private String date;
        private BigDecimal value;
    }
}
