package com.hisobnoma.platform.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxCalculationResult {
    private String taxCode;
    private String taxName;
    private BigDecimal rate;
    private BigDecimal baseAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private boolean inclusive;
}
