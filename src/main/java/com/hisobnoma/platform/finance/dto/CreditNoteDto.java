package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.CreditNoteStatus;
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
public class CreditNoteDto {
    private Long id;
    private Long tenantId;
    private String creditNoteNumber;
    private Long customerId;
    private String customerName;
    private Long originalInvoiceId;
    private String originalInvoiceNumber;
    private LocalDate creditNoteDate;
    private CreditNoteStatus status;
    private String reasonCode;
    private String reason;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal appliedAmount;
    private BigDecimal balance;
    private BigDecimal refundedAmount;
    private String currency;
    private BigDecimal exchangeRate;
    private Long arAccountId;
    private Long revenueAccountId;
    private String description;
    private String notes;
    private boolean glPosted;
    private Long glJournalEntryId;
    private Instant glPostedAt;
    private Long approvedBy;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
