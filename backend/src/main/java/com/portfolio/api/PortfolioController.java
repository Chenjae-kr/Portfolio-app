package com.portfolio.api;

import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.util.AssetClass;
import com.portfolio.common.util.SecurityUtils;
import com.portfolio.portfolio.entity.Portfolio;
import com.portfolio.portfolio.entity.PortfolioTarget;
import com.portfolio.portfolio.service.PortfolioService;
import com.portfolio.pricing.entity.Instrument;
import com.portfolio.pricing.repository.InstrumentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final SecurityUtils securityUtils;
    private final InstrumentRepository instrumentRepository;

    @GetMapping
    public ResponseEntity<?> listPortfolios() {
        try {
            String workspaceId = securityUtils.getCurrentWorkspaceId();
            List<Portfolio> portfolios = portfolioService.findAll(workspaceId);

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
            String workspaceId = securityUtils.getCurrentWorkspaceId();
            Portfolio portfolio = portfolioService.findById(id, workspaceId);

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
            String workspaceId = securityUtils.getCurrentWorkspaceId();
            Portfolio portfolio = portfolioService.create(
                    workspaceId,
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
            String workspaceId = securityUtils.getCurrentWorkspaceId();
            Portfolio portfolio = portfolioService.update(
                    id,
                    workspaceId,
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
            String workspaceId = securityUtils.getCurrentWorkspaceId();
            portfolioService.delete(id, workspaceId);

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

    // ===== Portfolio Targets =====

    @GetMapping("/{id}/targets")
    public ResponseEntity<?> getTargets(@PathVariable String id) {
        try {
            String workspaceId = securityUtils.getCurrentWorkspaceId();
            List<PortfolioTarget> targets = portfolioService.getTargets(id, workspaceId);

            // Batch lookup instruments
            List<String> instrumentIds = targets.stream()
                    .map(PortfolioTarget::getInstrumentId)
                    .filter(iid -> iid != null && !iid.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
            Map<String, Instrument> instrumentMap = new HashMap<>();
            if (!instrumentIds.isEmpty()) {
                instrumentRepository.findByIdIn(instrumentIds)
                        .forEach(inst -> instrumentMap.put(inst.getId(), inst));
            }

            List<Map<String, Object>> targetList = targets.stream()
                    .map(t -> toTargetDto(t, instrumentMap))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", targetList);
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}/targets")
    public ResponseEntity<?> updateTargets(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean normalize,
            @RequestBody UpdateTargetsRequest request
    ) {
        try {
            String workspaceId = securityUtils.getCurrentWorkspaceId();
            List<PortfolioTarget> targets = request.getTargets().stream()
                    .map(dto -> {
                        PortfolioTarget target = new PortfolioTarget();
                        target.setInstrumentId(dto.getInstrumentId());
                        target.setAssetClass(dto.getAssetClass() != null ?
                                AssetClass.valueOf(dto.getAssetClass()) : AssetClass.EQUITY);
                        target.setCurrency(dto.getCurrency());
                        target.setTargetWeight(dto.getTargetWeight());
                        target.setMinWeight(dto.getMinWeight());
                        target.setMaxWeight(dto.getMaxWeight());
                        return target;
                    })
                    .collect(Collectors.toList());

            List<PortfolioTarget> savedTargets = portfolioService.updateTargets(
                    id, workspaceId, targets, normalize);

            // Batch lookup instruments for saved targets
            List<String> savedInstrumentIds = savedTargets.stream()
                    .map(PortfolioTarget::getInstrumentId)
                    .filter(iid -> iid != null && !iid.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
            Map<String, Instrument> savedInstrumentMap = new HashMap<>();
            if (!savedInstrumentIds.isEmpty()) {
                instrumentRepository.findByIdIn(savedInstrumentIds)
                        .forEach(inst -> savedInstrumentMap.put(inst.getId(), inst));
            }

            List<Map<String, Object>> targetList = savedTargets.stream()
                    .map(t -> toTargetDto(t, savedInstrumentMap))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", targetList);
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            return createErrorResponse(e.getMessage(), e.getErrorCode().getHttpStatus());
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> toTargetDto(PortfolioTarget target, Map<String, Instrument> instrumentMap) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", target.getId());
        dto.put("instrumentId", target.getInstrumentId());
        dto.put("assetClass", target.getAssetClass().name());
        dto.put("currency", target.getCurrency());
        dto.put("targetWeight", target.getTargetWeight());
        dto.put("minWeight", target.getMinWeight());
        dto.put("maxWeight", target.getMaxWeight());
        dto.put("updatedAt", target.getUpdatedAt().toString());

        // Add instrument ticker and name
        if (target.getInstrumentId() != null) {
            Instrument inst = instrumentMap.get(target.getInstrumentId());
            if (inst != null) {
                dto.put("ticker", inst.getTicker());
                dto.put("instrumentName", inst.getName());
            }
        }

        return dto;
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

    @Data
    public static class UpdateTargetsRequest {
        private List<TargetDto> targets;
    }

    @Data
    public static class TargetDto {
        private String instrumentId;
        private String assetClass;
        private String currency;
        private BigDecimal targetWeight;
        private BigDecimal minWeight;
        private BigDecimal maxWeight;
    }
}
