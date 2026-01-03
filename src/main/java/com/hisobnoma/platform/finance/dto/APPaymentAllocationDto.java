package com.hisobnoma.platform.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class APPaymentAllocationDto {
    private Long id;
    private Long apPaymentId;
    private Long apInvoiceId;
    private String invoiceNumber;
    private BigDecimal invoiceAmount;
    private BigDecimal invoiceBalanceBefore;
    private BigDecimal allocatedAmount;
    private BigDecimal discountTaken;
    private BigDecimal writeOffAmount;
    private BigDecimal invoiceBalanceAfter;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    // Calculated field
    private BigDecimal totalApplied;
}
