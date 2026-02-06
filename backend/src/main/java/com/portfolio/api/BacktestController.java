package com.portfolio.api;

import com.portfolio.backtest.service.BacktestService;
import com.portfolio.backtest.service.BacktestService.*;
import com.portfolio.common.exception.BusinessException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 백테스트 API
 *
 * POST   /v1/backtests/configs        - 설정 생성
 * GET    /v1/backtests/configs         - 설정 목록
 * GET    /v1/backtests/configs/{id}    - 설정 조회
 * POST   /v1/backtests/runs            - 백테스트 실행
 * GET    /v1/backtests/runs            - 실행 목록
 * GET    /v1/backtests/runs/{id}       - 실행 상태 조회
 * GET    /v1/backtests/runs/{id}/results - 결과 조회
 */
@RestController
@RequestMapping("/v1/backtests")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestService backtestService;

    // ===== Config =====

    @PostMapping("/configs")
    public ResponseEntity<?> createConfig(@RequestBody BacktestConfig config) {
        try {
            BacktestConfig created = backtestService.createConfig(config);
            return ResponseEntity.ok(wrapResponse(created));
        } catch (BusinessException e) {
            return createErrorResponse(e.getMessage(), e.getErrorCode().getHttpStatus());
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/configs")
    public ResponseEntity<?> listConfigs() {
        try {
            List<BacktestConfig> configs = backtestService.listConfigs();
            return ResponseEntity.ok(wrapResponse(configs));
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/configs/{id}")
    public ResponseEntity<?> getConfig(@PathVariable String id) {
        try {
            BacktestConfig config = backtestService.getConfig(id);
            return ResponseEntity.ok(wrapResponse(config));
        } catch (BusinessException e) {
            return createErrorResponse(e.getMessage(), e.getErrorCode().getHttpStatus());
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===== Run =====

    @PostMapping("/runs")
    public ResponseEntity<?> runBacktest(@RequestBody RunRequest request) {
        try {
            BacktestRun run = backtestService.runBacktest(
                    request.getConfigId(), request.getInlineConfig());
            return ResponseEntity.ok(wrapResponse(run));
        } catch (BusinessException e) {
            return createErrorResponse(e.getMessage(), e.getErrorCode().getHttpStatus());
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/runs")
    public ResponseEntity<?> listRuns(@RequestParam(required = false) String configId) {
        try {
            List<BacktestRun> runs = backtestService.listRuns(configId);
            return ResponseEntity.ok(wrapResponse(runs));
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<?> getRun(@PathVariable String id) {
        try {
            BacktestRun run = backtestService.getRun(id);
            return ResponseEntity.ok(wrapResponse(run));
        } catch (BusinessException e) {
            return createErrorResponse(e.getMessage(), e.getErrorCode().getHttpStatus());
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/runs/{id}/results")
    public ResponseEntity<?> getResults(@PathVariable String id) {
        try {
            BacktestResult result = backtestService.getResult(id);
            return ResponseEntity.ok(wrapResponse(result));
        } catch (BusinessException e) {
            return createErrorResponse(e.getMessage(), e.getErrorCode().getHttpStatus());
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===== Helpers =====

    private Map<String, Object> wrapResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        response.put("meta", Map.of("timestamp", Instant.now().toString()));
        response.put("error", null);
        return response;
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
    public static class RunRequest {
        private String configId;
        private BacktestConfig inlineConfig;
    }
}
