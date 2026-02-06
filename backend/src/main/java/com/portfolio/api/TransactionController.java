package com.portfolio.api;

import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.util.SecurityUtils;
import com.portfolio.ledger.entity.Transaction;
import com.portfolio.ledger.entity.TransactionLeg;
import com.portfolio.ledger.service.TransactionService;
import com.portfolio.pricing.entity.Instrument;
import com.portfolio.pricing.repository.InstrumentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final InstrumentRepository instrumentRepository;
    private final SecurityUtils securityUtils;

    /**
     * 거래 목록 조회 (필터: fromDate, toDate, type)
     * GET /v1/portfolios/{portfolioId}/transactions
     */
    @GetMapping("/v1/portfolios/{portfolioId}/transactions")
    public ResponseEntity<?> listTransactions(
            @PathVariable String portfolioId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        try {
            String workspaceId = securityUtils.getCurrentWorkspaceId();
            Transaction.TransactionType transactionType =
                    type != null && !type.isBlank() ? Transaction.TransactionType.valueOf(type) : null;
            LocalDateTime fromDateTime = from != null && !from.isBlank() ? LocalDateTime.parse(from) : null;
            LocalDateTime toDateTime = to != null && !to.isBlank() ? LocalDateTime.parse(to) : null;

            List<Transaction> transactions = transactionService.getTransactions(
                    portfolioId, workspaceId, transactionType, fromDateTime, toDateTime);

            List<Map<String, Object>> items = transactions.stream()
                    .map(this::toTransactionDto)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", items);
            response.put("meta", Map.of(
                    "timestamp", Instant.now().toString(),
                    "total", transactions.size()
            ));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            return createErrorResponse(e.getMessage(), e.getErrorCode().getHttpStatus());
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 거래 생성
     * POST /v1/portfolios/{portfolioId}/transactions
     */
    @PostMapping("/v1/portfolios/{portfolioId}/transactions")
    public ResponseEntity<?> createTransaction(
            @PathVariable String portfolioId,
            @RequestBody CreateTransactionRequest request) {
        try {
            String workspaceId = securityUtils.getCurrentWorkspaceId();
            List<TransactionLeg> legs = request.getLegs().stream()
                    .map(dto -> TransactionLeg.builder()
                            .legType(TransactionLeg.LegType.valueOf(dto.getLegType()))
                            .instrumentId(dto.getInstrumentId())
                            .currency(dto.getCurrency())
                            .quantity(dto.getQuantity())
                            .price(dto.getPrice())
                            .amount(dto.getAmount())
                            .fxRateToBase(dto.getFxRateToBase())
                            .account(dto.getAccount())
                            .build())
                    .collect(Collectors.toList());

            LocalDateTime occurredAt = request.getOccurredAt() != null
                    ? LocalDateTime.parse(request.getOccurredAt())
                    : LocalDateTime.now();

            Transaction transaction = transactionService.createTransaction(
                    portfolioId,
                    workspaceId,
                    Transaction.TransactionType.valueOf(request.getType()),
                    occurredAt,
                    request.getNote(),
                    legs
            );

            Map<String, Object> response = new HashMap<>();
            response.put("data", toTransactionDto(transaction));
            response.put("meta", Map.of("timestamp", Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BusinessException e) {
            return createErrorResponse(e.getMessage(), e.getErrorCode().getHttpStatus());
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 거래 상세 조회
     * GET /v1/transactions/{id}
     */
    @GetMapping("/v1/transactions/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable String id) {
        try {
            Transaction transaction = transactionService.getTransaction(id);

            Map<String, Object> response = new HashMap<>();
            response.put("data", toTransactionDto(transaction));
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
     * 거래 취소 (VOID)
     * POST /v1/transactions/{id}/void
     */
    @PostMapping("/v1/transactions/{id}/void")
    public ResponseEntity<?> voidTransaction(@PathVariable String id) {
        try {
            Transaction transaction = transactionService.voidTransaction(id);

            Map<String, Object> response = new HashMap<>();
            response.put("data", toTransactionDto(transaction));
            response.put("meta", Map.of("timestamp", Instant.now().toString()));
            response.put("error", null);

            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            return createErrorResponse(e.getMessage(), e.getErrorCode().getHttpStatus());
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===== DTO 변환 =====

    private Map<String, Object> toTransactionDto(Transaction transaction) {
        List<String> instrumentIds = transaction.getLegs().stream()
                .map(TransactionLeg::getInstrumentId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());

        Map<String, Instrument> instrumentMap = new HashMap<>();
        if (!instrumentIds.isEmpty()) {
            instrumentRepository.findByIdIn(instrumentIds)
                    .forEach(inst -> instrumentMap.put(inst.getId(), inst));
        }

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", transaction.getId());
        dto.put("portfolioId", transaction.getPortfolioId());
        dto.put("occurredAt", transaction.getOccurredAt().toString());
        dto.put("settleDate", transaction.getSettleDate() != null ? transaction.getSettleDate().toString() : null);
        dto.put("type", transaction.getType().name());
        dto.put("status", transaction.getStatus().name());
        dto.put("note", transaction.getNote());
        dto.put("tags", transaction.getTags());
        dto.put("createdAt", transaction.getCreatedAt().toString());

        List<Map<String, Object>> legDtos = transaction.getLegs().stream()
                .map(leg -> toLegDto(leg, instrumentMap))
                .collect(Collectors.toList());
        dto.put("legs", legDtos);

        return dto;
    }

    private Map<String, Object> toLegDto(TransactionLeg leg, Map<String, Instrument> instrumentMap) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", leg.getId());
        dto.put("legType", leg.getLegType().name());
        dto.put("instrumentId", leg.getInstrumentId());
        dto.put("currency", leg.getCurrency());
        dto.put("quantity", leg.getQuantity());
        dto.put("price", leg.getPrice());
        dto.put("amount", leg.getAmount());
        dto.put("fxRateToBase", leg.getFxRateToBase());
        dto.put("account", leg.getAccount());

        if (leg.getInstrumentId() != null) {
            Instrument inst = instrumentMap.get(leg.getInstrumentId());
            if (inst != null) {
                dto.put("ticker", inst.getTicker());
                dto.put("instrumentName", inst.getName());
            }
        }

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

    // ===== Request DTOs =====

    @Data
    public static class CreateTransactionRequest {
        private String type;
        private String occurredAt;
        private String note;
        private List<LegDto> legs;
    }

    @Data
    public static class LegDto {
        private String legType;
        private String instrumentId;
        private String currency;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal amount;
        private BigDecimal fxRateToBase;
        private String account;
    }
}
