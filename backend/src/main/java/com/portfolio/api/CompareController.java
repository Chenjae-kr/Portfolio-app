package com.portfolio.api;

import com.portfolio.analytics.service.PerformanceService;
import com.portfolio.analytics.service.PerformanceService.PerformanceResult;
import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.util.SecurityUtils;
import com.portfolio.portfolio.entity.Portfolio;
import com.portfolio.portfolio.service.PortfolioService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * 포트폴리오 비교 분석 API
 * POST /v1/compare/performance
 */
@RestController
@RequestMapping("/v1/compare")
@RequiredArgsConstructor
public class CompareController {

    private final PerformanceService performanceService;
    private final PortfolioService portfolioService;
    private final SecurityUtils securityUtils;

    /**
     * 다중 포트폴리오 성과 비교
     */
    @PostMapping("/performance")
    public ResponseEntity<?> comparePerformance(@RequestBody CompareRequest request) {
        try {
            String workspaceId = securityUtils.getCurrentWorkspaceId();

            if (request.getPortfolioIds() == null || request.getPortfolioIds().size() < 2) {
                return createErrorResponse("At least 2 portfolios required", HttpStatus.BAD_REQUEST);
            }
            if (request.getPortfolioIds().size() > 5) {
                return createErrorResponse("Maximum 5 portfolios allowed", HttpStatus.BAD_REQUEST);
            }

            LocalDate from = LocalDate.parse(request.getFrom());
            LocalDate to = LocalDate.parse(request.getTo());
            String metric = request.getMetric() != null ? request.getMetric() : "TWR";
            String frequency = request.getFrequency() != null ? request.getFrequency() : "DAILY";

            List<Map<String, Object>> curves = new ArrayList<>();
            List<Map<String, Object>> statsTable = new ArrayList<>();

            for (String portfolioId : request.getPortfolioIds()) {
                Portfolio portfolio = portfolioService.findById(portfolioId, workspaceId);

                PerformanceResult result = performanceService.calculatePerformance(
                        portfolioId, workspaceId, from, to, metric, frequency);

                Map<String, Object> curve = new LinkedHashMap<>();
                curve.put("id", portfolioId);
                curve.put("label", portfolio.getName());
                curve.put("metric", metric);
                curve.put("points", result.dataPoints);
                curves.add(curve);

                Map<String, Object> stat = new LinkedHashMap<>();
                stat.put("id", portfolioId);
                stat.put("label", portfolio.getName());
                if (result.stats != null) {
                    stat.put("totalReturn", result.stats.totalReturn);
                    stat.put("cagr", result.stats.cagr);
                    stat.put("vol", result.stats.volatility);
                    stat.put("mdd", result.stats.mdd);
                    stat.put("sharpe", result.stats.sharpe);
                }
                statsTable.add(stat);
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("curves", curves);
            data.put("statsTable", statsTable);

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

    @Data
    public static class CompareRequest {
        private List<String> portfolioIds;
        private String from;
        private String to;
        private String metric;
        private String frequency;
        private String currencyMode;
    }
}
