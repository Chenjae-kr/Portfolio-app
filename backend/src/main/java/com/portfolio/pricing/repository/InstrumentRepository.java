package com.portfolio.pricing.repository;

import com.portfolio.common.util.AssetClass;
import com.portfolio.pricing.entity.Instrument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, String> {

    Optional<Instrument> findByTicker(String ticker);

    @Query("SELECT i FROM Instrument i WHERE i.status = 'ACTIVE' " +
            "AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(i.ticker) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Instrument> searchByNameOrTicker(@Param("query") String query, Pageable pageable);

    @Query("SELECT i FROM Instrument i WHERE i.status = 'ACTIVE' " +
            "AND i.assetClass = :assetClass " +
            "AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(i.ticker) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Instrument> searchByNameOrTickerAndAssetClass(
            @Param("query") String query,
            @Param("assetClass") AssetClass assetClass,
            Pageable pageable
    );

    List<Instrument> findByIdIn(List<String> ids);

    List<Instrument> findByAssetClassAndStatus(AssetClass assetClass, Instrument.InstrumentStatus status);
}
