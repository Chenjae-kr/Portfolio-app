package com.portfolio.pricing.repository;

import com.portfolio.pricing.entity.PriceBar;
import com.portfolio.pricing.entity.PriceBarId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceBarRepository extends JpaRepository<PriceBar, PriceBarId> {

    @Query("SELECT p FROM PriceBar p WHERE p.instrumentId = :instrumentId " +
            "AND p.timeframe = :timeframe " +
            "AND p.ts BETWEEN :from AND :to " +
            "ORDER BY p.ts ASC")
    List<PriceBar> findByInstrumentIdAndTimeframeAndTsBetween(
            @Param("instrumentId") String instrumentId,
            @Param("timeframe") PriceBar.Timeframe timeframe,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT p FROM PriceBar p WHERE p.instrumentId = :instrumentId " +
            "AND p.timeframe = :timeframe " +
            "ORDER BY p.ts DESC LIMIT 1")
    Optional<PriceBar> findLatestByInstrumentIdAndTimeframe(
            @Param("instrumentId") String instrumentId,
            @Param("timeframe") PriceBar.Timeframe timeframe
    );

    @Query("SELECT p FROM PriceBar p WHERE p.instrumentId IN :instrumentIds " +
            "AND p.timeframe = :timeframe " +
            "AND p.ts = (SELECT MAX(p2.ts) FROM PriceBar p2 " +
            "            WHERE p2.instrumentId = p.instrumentId AND p2.timeframe = :timeframe)")
    List<PriceBar> findLatestForInstruments(
            @Param("instrumentIds") List<String> instrumentIds,
            @Param("timeframe") PriceBar.Timeframe timeframe
    );
}
