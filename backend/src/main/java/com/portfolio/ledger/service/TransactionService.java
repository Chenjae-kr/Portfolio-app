package com.portfolio.ledger.service;

import com.portfolio.common.exception.BusinessException;
import com.portfolio.common.exception.ErrorCode;
import com.portfolio.common.util.AssetClass;
import com.portfolio.ledger.entity.Transaction;
import com.portfolio.ledger.entity.TransactionLeg;
import com.portfolio.ledger.repository.TransactionRepository;
import com.portfolio.portfolio.entity.Portfolio;
import com.portfolio.portfolio.repository.PortfolioRepository;
import com.portfolio.pricing.entity.Instrument;
import com.portfolio.pricing.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final InstrumentRepository instrumentRepository;

    /**
     * 거래 생성 (복식부기 검증 포함)
     */
    @Transactional
    public Transaction createTransaction(String portfolioId, String workspaceId,
                                          Transaction.TransactionType type,
                                          LocalDateTime occurredAt,
                                          String note,
                                          List<TransactionLeg> legs) {
        // 1. 포트폴리오 존재 확인
        Portfolio portfolio = portfolioRepository.findByIdAndWorkspaceId(portfolioId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 2. Legs 검증
        validateLegs(type, legs);

        // 2.5. Instrument 자동 등록 (존재하지 않는 instrumentId가 있으면 생성)
        ensureInstrumentsExist(legs, portfolio.getBaseCurrency());

        // 3. Transaction 생성
        Transaction transaction = Transaction.builder()
                .portfolioId(portfolioId)
                .type(type)
                .occurredAt(occurredAt != null ? occurredAt : LocalDateTime.now())
                .status(Transaction.TransactionStatus.POSTED)
                .note(note)
                .build();

        // 4. Legs 연결
        for (TransactionLeg leg : legs) {
            transaction.addLeg(leg);
        }

        Transaction saved = transactionRepository.save(transaction);
        log.info("Created transaction: id={}, portfolio={}, type={}, legs={}",
                saved.getId(), portfolioId, type, legs.size());

        return saved;
    }

    /**
     * 거래 목록 조회 (legs 포함, 필터 옵션)
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(String portfolioId, String workspaceId) {
        return getTransactions(portfolioId, workspaceId, null, null, null);
    }

    /**
     * 거래 목록 조회 (필터)
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(String portfolioId,
                                             String workspaceId,
                                             Transaction.TransactionType type,
                                             LocalDateTime from,
                                             LocalDateTime to) {
        // 포트폴리오 접근 권한 확인
        portfolioRepository.findByIdAndWorkspaceId(portfolioId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        Transaction.TransactionStatus excludeStatus = Transaction.TransactionStatus.VOID;

        if (type != null && from != null && to != null) {
            return transactionRepository.findByPortfolioIdWithLegsAndAllFilters(
                    portfolioId, excludeStatus, type, from, to);
        } else if (type != null) {
            return transactionRepository.findByPortfolioIdWithLegsAndTypeFilter(
                    portfolioId, excludeStatus, type);
        } else if (from != null && to != null) {
            return transactionRepository.findByPortfolioIdWithLegsAndDateFilter(
                    portfolioId, excludeStatus, from, to);
        } else {
            return transactionRepository.findByPortfolioIdWithLegs(
                    portfolioId, excludeStatus);
        }
    }

    /**
     * 거래 상세 조회
     */
    @Transactional(readOnly = true)
    public Transaction getTransaction(String transactionId) {
        return transactionRepository.findByIdWithLegs(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    /**
     * 거래 취소 (VOID)
     */
    @Transactional
    public Transaction voidTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByIdWithLegs(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.getStatus() == Transaction.TransactionStatus.VOID) {
            throw new BusinessException(ErrorCode.TRANSACTION_ALREADY_VOID);
        }

        transaction.setStatus(Transaction.TransactionStatus.VOID);
        Transaction saved = transactionRepository.save(transaction);

        log.info("Voided transaction: id={}, portfolio={}", transactionId, transaction.getPortfolioId());
        return saved;
    }

    /**
     * 기간별 거래 조회 (POSTED만)
     */
    @Transactional(readOnly = true)
    public List<Transaction> getPostedTransactionsInPeriod(String portfolioId,
                                                           LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findPostedTransactionsInPeriod(portfolioId, from, to);
    }

    // ===== Instrument 자동 등록 =====

    /**
     * Legs에 포함된 instrumentId가 DB에 존재하지 않으면 자동 등록
     * (사용자가 ticker/ID를 직접 입력하는 경우 대비)
     */
    private void ensureInstrumentsExist(List<TransactionLeg> legs, String baseCurrency) {
        for (TransactionLeg leg : legs) {
            String instId = leg.getInstrumentId();
            if (instId == null || instId.isBlank()) continue;

            // ID로 먼저 조회
            Optional<Instrument> byId = instrumentRepository.findById(instId);
            if (byId.isPresent()) continue;

            // Ticker로 조회
            Optional<Instrument> byTicker = instrumentRepository.findByTicker(instId);
            if (byTicker.isPresent()) {
                // ticker로 찾았으면 leg의 instrumentId를 실제 ID로 교체
                leg.setInstrumentId(byTicker.get().getId());
                continue;
            }

            // 존재하지 않으면 자동 등록 (ticker = 입력값, 기본 STOCK/EQUITY)
            Instrument newInstrument = Instrument.builder()
                    .instrumentType(Instrument.InstrumentType.STOCK)
                    .name(instId)
                    .ticker(instId)
                    .currency(leg.getCurrency() != null ? leg.getCurrency() : baseCurrency)
                    .assetClass(AssetClass.EQUITY)
                    .status(Instrument.InstrumentStatus.ACTIVE)
                    .build();
            Instrument saved = instrumentRepository.save(newInstrument);
            leg.setInstrumentId(saved.getId());
            log.info("Auto-registered instrument: ticker={}, id={}", instId, saved.getId());
        }
    }

    // ===== 검증 로직 =====

    /**
     * 복식부기 Legs 검증
     * - Legs가 비어있으면 안 됨
     * - BUY/SELL은 정확히 1개의 ASSET leg 필요
     * - 각 leg의 amount 필수
     * - 통화별 legs 합계가 0이어야 함 (복식부기 원칙)
     */
    private void validateLegs(Transaction.TransactionType type, List<TransactionLeg> legs) {
        if (legs == null || legs.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_LEGS);
        }

        // BUY/SELL은 정확히 1개의 ASSET leg 필요
        if (type == Transaction.TransactionType.BUY || type == Transaction.TransactionType.SELL) {
            long assetLegCount = legs.stream()
                    .filter(leg -> leg.getLegType() == TransactionLeg.LegType.ASSET)
                    .count();

            if (assetLegCount != 1) {
                throw new BusinessException(ErrorCode.INVALID_TRANSACTION_LEGS);
            }

            // ASSET leg는 instrumentId 필수
            legs.stream()
                    .filter(leg -> leg.getLegType() == TransactionLeg.LegType.ASSET)
                    .findFirst()
                    .ifPresent(assetLeg -> {
                        if (assetLeg.getInstrumentId() == null || assetLeg.getInstrumentId().isBlank()) {
                            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_LEGS);
                        }
                        if (assetLeg.getQuantity() == null || assetLeg.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_LEGS);
                        }
                    });
        }

        // 각 leg의 amount 필수
        for (TransactionLeg leg : legs) {
            if (leg.getAmount() == null) {
                throw new BusinessException(ErrorCode.INVALID_TRANSACTION_LEGS);
            }
            if (leg.getCurrency() == null || leg.getCurrency().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_TRANSACTION_LEGS);
            }
        }

        // 통화별 합계가 0인지 검증 (복식부기)
        // 같은 통화 내에서 legs 합계 = 0
        legs.stream()
                .collect(java.util.stream.Collectors.groupingBy(TransactionLeg::getCurrency))
                .forEach((currency, currencyLegs) -> {
                    BigDecimal sum = currencyLegs.stream()
                            .map(TransactionLeg::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 허용 오차 (반올림 오차 대비)
                    if (sum.abs().compareTo(new BigDecimal("0.01")) > 0) {
                        log.warn("Legs balance check: currency={}, sum={}", currency, sum);
                        throw new BusinessException(ErrorCode.INVALID_TRANSACTION_LEGS);
                    }
                });
    }
}
