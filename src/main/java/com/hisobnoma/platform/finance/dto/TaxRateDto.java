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
public class TaxRateDto {
    private Long id;
    private Long taxCodeId;
    private String taxCodeCode;
    private String taxCodeName;
    private String name;
    private BigDecimal rate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private boolean active;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
