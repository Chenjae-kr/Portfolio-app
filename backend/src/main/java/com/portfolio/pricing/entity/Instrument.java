package com.portfolio.pricing.entity;

import com.portfolio.common.util.AssetClass;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "instruments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "instrument_type", nullable = false)
    private InstrumentType instrumentType;

    @Column(nullable = false)
    private String name;

    private String ticker;

    @Column(name = "exchange_id")
    private String exchangeId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 2)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false)
    private AssetClass assetClass;

    private String sector;

    private String industry;

    private String provider;

    @Column(name = "expense_ratio", precision = 6, scale = 4)
    private BigDecimal expenseRatio;

    @Column(name = "benchmark_index")
    private String benchmarkIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InstrumentStatus status = InstrumentStatus.ACTIVE;

    @Column(name = "first_listed_at")
    private LocalDate firstListedAt;

    @Column(name = "delisted_at")
    private LocalDate delistedAt;

    public enum InstrumentType {
        STOCK,
        ETF,
        ETN,
        BOND,
        COMMODITY_INDEX,
        CASH_PROXY
    }

    public enum InstrumentStatus {
        ACTIVE,
        DELISTED
    }
}
