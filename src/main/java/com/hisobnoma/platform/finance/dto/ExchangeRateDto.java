package com.hisobnoma.platform.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDto {
    private Long id;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private BigDecimal inverseRate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String source;
    private boolean active;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
