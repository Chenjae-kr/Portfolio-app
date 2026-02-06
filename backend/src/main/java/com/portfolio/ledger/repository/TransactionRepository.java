package com.portfolio.ledger.repository;

import com.portfolio.ledger.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.legs WHERE t.id = :id")
    Optional<Transaction> findByIdWithLegs(@Param("id") String id);

    Page<Transaction> findByPortfolioIdAndStatusNotOrderByOccurredAtDesc(
            String portfolioId,
            Transaction.TransactionStatus status,
            Pageable pageable
    );

    @Query("SELECT DISTINCT t FROM Transaction t LEFT JOIN FETCH t.legs " +
            "WHERE t.portfolioId = :portfolioId AND t.status <> :status " +
            "ORDER BY t.occurredAt DESC")
    List<Transaction> findByPortfolioIdWithLegs(
            @Param("portfolioId") String portfolioId,
            @Param("status") Transaction.TransactionStatus status
    );

    /**
     * 거래 목록 조회 (날짜 필터만, from/to는 sentinel로 null 대신 사용)
     */
    @Query("SELECT DISTINCT t FROM Transaction t LEFT JOIN FETCH t.legs " +
            "WHERE t.portfolioId = :portfolioId AND t.status <> :excludeStatus " +
            "AND t.occurredAt >= :fromDate AND t.occurredAt <= :toDate " +
            "ORDER BY t.occurredAt DESC")
    List<Transaction> findByPortfolioIdWithDateFilters(
            @Param("portfolioId") String portfolioId,
            @Param("excludeStatus") Transaction.TransactionStatus excludeStatus,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    /**
     * 거래 목록 조회 (날짜 + 유형 필터, from/to는 sentinel로 null 대신 사용)
     */
    @Query("SELECT DISTINCT t FROM Transaction t LEFT JOIN FETCH t.legs " +
            "WHERE t.portfolioId = :portfolioId AND t.status <> :excludeStatus " +
            "AND t.occurredAt >= :fromDate AND t.occurredAt <= :toDate " +
            "AND t.type = :type " +
            "ORDER BY t.occurredAt DESC")
    List<Transaction> findByPortfolioIdWithDateFiltersAndType(
            @Param("portfolioId") String portfolioId,
            @Param("excludeStatus") Transaction.TransactionStatus excludeStatus,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("type") Transaction.TransactionType type
    );

    @Query("SELECT t FROM Transaction t WHERE t.portfolioId = :portfolioId " +
            "AND t.status = 'POSTED' " +
            "AND t.occurredAt BETWEEN :from AND :to " +
            "ORDER BY t.occurredAt ASC")
    List<Transaction> findPostedTransactionsInPeriod(
            @Param("portfolioId") String portfolioId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT t FROM Transaction t WHERE t.portfolioId = :portfolioId " +
            "AND t.status = 'POSTED' " +
            "AND t.type = :type " +
            "ORDER BY t.occurredAt DESC")
    List<Transaction> findByPortfolioIdAndType(
            @Param("portfolioId") String portfolioId,
            @Param("type") Transaction.TransactionType type
    );
}
