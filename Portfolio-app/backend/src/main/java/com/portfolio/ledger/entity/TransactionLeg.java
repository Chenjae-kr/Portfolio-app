package com.portfolio.ledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transaction_legs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "leg_type", nullable = false)
    private LegType legType;

    @Column(name = "instrument_id")
    private String instrumentId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(precision = 18, scale = 6)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "fx_rate_to_base", precision = 18, scale = 8)
    private BigDecimal fxRateToBase;

    private String account;

    public enum LegType {
        ASSET,
        CASH,
        FEE,
        TAX,
        INCOME,
        FX
    }
}
