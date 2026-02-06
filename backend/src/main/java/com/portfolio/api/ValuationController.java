package com.portfolio.api;

import com.portfolio.common.exception.BusinessException;
import com.portfolio.infra.init.DataInitializer;
import com.portfolio.portfolio.service.PortfolioService;
import com.portfolio.valuation.service.ValuationService;
import com.portfolio.valuation.service.ValuationService.PortfolioValuation;
import com.portfolio.valuation.service.ValuationService.PositionValuation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/portfolios")
@RequiredArgsConstructor
public class ValuationController {

    private final PortfolioService portfolioService;
    private final ValuationService valuationService;

    private static final String DEFAULT_WORKSPACE_ID = DataInitializer.DEFAULT_WORKSPACE_ID;

    /**
     * 포트폴리오 평가액 조회
     * GET /v1/portfolios/{id}/valuation
     */
    @GetMapping("/{id}/valuation")
    public ResponseEntity<?> getValuation(
            @PathVariable String id,
            @RequestParam(defaultValue = "REALTIME") String mode) {
        try {
            PortfolioValuation valuation = valuationService.calculateValuation(id, DEFAULT_WORKSPACE_ID);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("portfolioId", valuation.portfolioId);
            data.put("asOf", LocalDate.now().toString());
            data.put("mode", mode);
            data.put("currency", valuation.currency);
            data.put("totalValueBase", valuation.totalValueBase);
            data.put("cashValueBase", valuation.cashValueBase);
            data.put("dayPnlBase", valuation.dayPnlBase);
            data.put("totalPnlBase", valuation.totalPnlBase);
            data.put("positions", valuation.positions.stream()
                    .map(this::toPositionDto)
                    .collect(Collectors.toList()));
            data.put("fxUsed", Collections.emptyMap());
            data.put("priceTimestamp", Collections.emptyMap());

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("meta", Map.of("timestamp", Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            return createErrorResponse(e.getMessage(), e.getErrorCode().getHttpStatus());
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 포트폴리오 성과 조회 (아직 미구현 - 빈 데이터 반환)
     * GET /v1/portfolios/{id}/performance
     */
    @GetMapping("/{id}/performance")
    public ResponseEntity<?> getPerformance(
            @PathVariable String id,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "TWR") String metric,
            @RequestParam(defaultValue = "DAILY") String frequency) {
        try {
            portfolioService.findById(id, DEFAULT_WORKSPACE_ID);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("portfolioId", id);
            data.put("from", from);
            data.put("to", to);
            data.put("metric", metric);
            data.put("frequency", frequency);
            data.put("dataPoints", Collections.emptyList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("meta", Map.of("timestamp", Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            return createErrorResponse(e.getMessage(), e.getErrorCode().getHttpStatus());
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> toPositionDto(PositionValuation pv) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("instrumentId", pv.instrumentId);
        dto.put("ticker", pv.ticker);
        dto.put("instrumentName", pv.instrumentName);
        dto.put("quantity", pv.quantity);
        dto.put("avgCost", pv.avgCost);
        dto.put("marketPrice", pv.marketPrice);
        dto.put("marketValue", pv.marketValue);
        dto.put("marketValueBase", pv.marketValueBase);
        dto.put("unrealizedPnlBase", pv.unrealizedPnlBase);
        dto.put("realizedPnlBase", pv.realizedPnlBase);
        dto.put("weight", pv.weight);
        dto.put("dayPnlBase", pv.dayPnlBase);
        return dto;
    }

    private ResponseEntity<?> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", status.name());
        error.put("message", message);

        Map<String, Object> response = new HashMap<>();
        response.put("data", null);
        response.put("meta", Map.of("timestamp", Instant.now().toString()));
        response.put("error", error);

        return ResponseEntity.status(status).body(response);
    }
}
