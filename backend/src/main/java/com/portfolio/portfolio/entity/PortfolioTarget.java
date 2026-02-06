package com.portfolio.portfolio.entity;

import com.portfolio.common.util.AssetClass;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_targets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "instrument_id", "currency"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "instrument_id")
    private String instrumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false)
    private AssetClass assetClass;

    @Column(length = 3)
    private String currency;

    @Column(name = "target_weight", nullable = false, precision = 7, scale = 4)
    private BigDecimal targetWeight;

    @Column(name = "min_weight", precision = 7, scale = 4)
    private BigDecimal minWeight;

    @Column(name = "max_weight", precision = 7, scale = 4)
    private BigDecimal maxWeight;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
