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

    @Query("SELECT DISTINCT t FROM Transaction t LEFT JOIN FETCH t.legs " +
            "WHERE t.portfolioId = :portfolioId " +
            "AND t.status <> :status " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:from IS NULL OR t.occurredAt >= :from) " +
            "AND (:to IS NULL OR t.occurredAt <= :to) " +
            "ORDER BY t.occurredAt DESC")
    List<Transaction> findByPortfolioIdWithLegsAndFilters(
            @Param("portfolioId") String portfolioId,
            @Param("status") Transaction.TransactionStatus status,
            @Param("type") Transaction.TransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
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
