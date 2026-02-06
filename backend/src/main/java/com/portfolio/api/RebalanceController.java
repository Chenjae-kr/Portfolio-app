package com.portfolio.api;

import com.portfolio.common.exception.BusinessException;
import com.portfolio.infra.init.DataInitializer;
import com.portfolio.rebalance.service.RebalanceService;
import com.portfolio.rebalance.service.RebalanceService.RebalanceAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 리밸런싱 분석 API
 * GET /v1/portfolios/{id}/rebalance
 */
@RestController
@RequestMapping("/v1/portfolios")
@RequiredArgsConstructor
public class RebalanceController {

    private final RebalanceService rebalanceService;

    private static final String DEFAULT_WORKSPACE_ID = DataInitializer.DEFAULT_WORKSPACE_ID;

    @GetMapping("/{id}/rebalance")
    public ResponseEntity<?> getRebalanceAnalysis(@PathVariable String id) {
        try {
            RebalanceAnalysis analysis = rebalanceService.analyze(id, DEFAULT_WORKSPACE_ID);

            Map<String, Object> response = new HashMap<>();
            response.put("data", analysis);
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
}
