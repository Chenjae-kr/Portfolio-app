package com.portfolio.api;

import com.portfolio.portfolio.entity.Portfolio;
import com.portfolio.portfolio.service.PortfolioService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    // 개발 모드: 임시 workspace ID (실제로는 인증된 사용자의 workspace에서 가져와야 함)
    private static final String DEFAULT_WORKSPACE_ID = "default-workspace";

    @GetMapping
    public ResponseEntity<?> listPortfolios() {
        try {
            List<Portfolio> portfolios = portfolioService.findAll(DEFAULT_WORKSPACE_ID);
            
            List<Map<String, Object>> portfolioList = portfolios.stream()
                    .map(this::toPortfolioDto)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", portfolioList);
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPortfolio(@PathVariable String id) {
        try {
            Portfolio portfolio = portfolioService.findById(id, DEFAULT_WORKSPACE_ID);

            Map<String, Object> response = new HashMap<>();
            response.put("data", toPortfolioDto(portfolio));
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<?> createPortfolio(@RequestBody CreatePortfolioRequest request) {
        try {
            Portfolio portfolio = portfolioService.create(
                    DEFAULT_WORKSPACE_ID,
                    request.getName(),
                    request.getDescription(),
                    request.getBaseCurrency(),
                    Portfolio.PortfolioType.valueOf(request.getType())
            );

            Map<String, Object> response = new HashMap<>();
            response.put("data", toPortfolioDto(portfolio));
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePortfolio(@PathVariable String id, 
                                            @RequestBody UpdatePortfolioRequest request) {
        try {
            Portfolio portfolio = portfolioService.update(
                    id,
                    DEFAULT_WORKSPACE_ID,
                    request.getName(),
                    request.getDescription()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("data", toPortfolioDto(portfolio));
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePortfolio(@PathVariable String id) {
        try {
            portfolioService.delete(id, DEFAULT_WORKSPACE_ID);

            Map<String, Object> response = new HashMap<>();
            response.put("data", Map.of("message", "Portfolio archived successfully"));
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> toPortfolioDto(Portfolio portfolio) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", portfolio.getId());
        dto.put("name", portfolio.getName());
        dto.put("description", portfolio.getDescription());
        dto.put("baseCurrency", portfolio.getBaseCurrency());
        dto.put("type", portfolio.getType().name());
        dto.put("createdAt", portfolio.getCreatedAt().toString());
        dto.put("updatedAt", portfolio.getUpdatedAt().toString());
        return dto;
    }

    private ResponseEntity<?> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", status.name());
        error.put("message", message);

        Map<String, Object> response = new HashMap<>();
        response.put("data", null);
        response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
        response.put("error", error);

        return ResponseEntity.status(status).body(response);
    }

    @Data
    public static class CreatePortfolioRequest {
        private String name;
        private String description;
        private String baseCurrency;
        private String type;
    }

    @Data
    public static class UpdatePortfolioRequest {
        private String name;
        private String description;
    }
}
