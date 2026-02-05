package com.portfolio.pricing.entity;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FxRateId implements Serializable {

    private String baseCurrency;
    private String quoteCurrency;
    private LocalDateTime ts;
}
