package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.APInvoiceStatus;
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
public class APInvoiceDto {
    private Long id;
    private Long tenantId;
    private String invoiceNumber;
    private String vendorInvoiceNumber;
    private Long vendorId;
    private String vendorName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private LocalDate receivedDate;
    private APInvoiceStatus status;
    private Long purchaseOrderId;
    private String purchaseOrderNumber;
    private Long receivingOrderId;
    private String receivingOrderNumber;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private String currency;
    private BigDecimal exchangeRate;
    private String paymentTerms;
    private Integer paymentTermsDays;
    private Long expenseAccountId;
    private Long apAccountId;
    private String description;
    private String notes;
    private String matchingStatus;
    private String matchingNotes;
    private boolean glPosted;
    private Long glJournalEntryId;
    private Instant glPostedAt;
    private Long submittedBy;
    private Instant submittedAt;
    private Long approvedBy;
    private Instant approvedAt;
    private Long rejectedBy;
    private Instant rejectedAt;
    private String rejectionReason;
    private Long cancelledBy;
    private Instant cancelledAt;
    private String cancellationReason;
    private List<APInvoiceLineDto> lines;
    private Instant createdAt;
    private Instant updatedAt;

    // Calculated fields
    private boolean overdue;
    private int daysOverdue;
    private int daysUntilDue;
}
