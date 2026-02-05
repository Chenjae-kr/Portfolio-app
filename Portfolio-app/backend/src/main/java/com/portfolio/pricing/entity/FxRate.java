package com.portfolio.pricing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fx_rates")
@IdClass(FxRateId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FxRate {

    @Id
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Id
    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quoteCurrency;

    @Id
    @Column(nullable = false)
    private LocalDateTime ts;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal rate;

    private String source;
}
