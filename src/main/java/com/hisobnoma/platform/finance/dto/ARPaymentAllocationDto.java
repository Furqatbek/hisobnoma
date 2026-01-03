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
public class ARPaymentAllocationDto {
    private Long id;
    private Long arPaymentId;
    private Long arInvoiceId;
    private Long creditNoteId;
    private String invoiceNumber;
    private BigDecimal invoiceAmount;
    private BigDecimal invoiceBalanceBefore;
    private BigDecimal allocatedAmount;
    private BigDecimal discountTaken;
    private BigDecimal writeOffAmount;
    private BigDecimal invoiceBalanceAfter;
    private BigDecimal totalApplied;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
