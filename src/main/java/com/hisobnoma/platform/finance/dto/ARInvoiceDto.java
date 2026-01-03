package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ARInvoiceDto {
    private Long id;
    private Long tenantId;
    private String invoiceNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private ARInvoiceStatus status;
    private Long posTransactionId;
    private String posTransactionNumber;
    private Long salesOrderId;
    private String salesOrderNumber;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal creditApplied;
    private BigDecimal balanceDue;
    private String currency;
    private BigDecimal exchangeRate;
    private String paymentTerms;
    private Integer paymentTermsDays;
    private Long arAccountId;
    private Long revenueAccountId;
    private String description;
    private String notes;
    private String billingAddress;
    private String shippingAddress;
    private boolean glPosted;
    private Long glJournalEntryId;
    private Instant glPostedAt;
    private Instant sentAt;
    private Long sentBy;
    private boolean overdue;
    private int daysOverdue;
    private int daysUntilDue;
    private List<ARInvoiceLineDto> lines;
    private Instant createdAt;
    private Instant updatedAt;
}
