package com.portfolio.pricing.entity;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PriceBarId implements Serializable {

    private String instrumentId;
    private PriceBar.Timeframe timeframe;
    private LocalDate ts;
}
