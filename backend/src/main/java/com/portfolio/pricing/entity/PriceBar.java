package com.portfolio.pricing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_bars")
@IdClass(PriceBarId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceBar {

    @Id
    @Column(name = "instrument_id", nullable = false)
    private String instrumentId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Timeframe timeframe;

    @Id
    @Column(nullable = false)
    private LocalDate ts;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal open;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal high;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal low;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal close;

    @Column(name = "adj_close", precision = 18, scale = 6)
    private BigDecimal adjClose;

    private Long volume;

    @Column(name = "data_vendor")
    private String dataVendor;

    public enum Timeframe {
        D1,   // Daily
        W1,   // Weekly
        M1    // Monthly
    }
}
