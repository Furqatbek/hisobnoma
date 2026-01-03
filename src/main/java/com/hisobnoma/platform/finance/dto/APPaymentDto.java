package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.APPaymentMethod;
import com.hisobnoma.platform.finance.entity.APPaymentStatus;
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
public class APPaymentDto {
    private Long id;
    private Long tenantId;
    private String paymentNumber;
    private Long vendorId;
    private String vendorName;
    private LocalDate paymentDate;
    private APPaymentStatus status;
    private APPaymentMethod paymentMethod;
    private BigDecimal paymentAmount;
    private BigDecimal allocatedAmount;
    private BigDecimal unallocatedAmount;
    private String currency;
    private BigDecimal exchangeRate;
    private Long bankAccountId;
    private String bankAccountName;
    private Long cashAccountId;
    private Long apAccountId;
    private String referenceNumber;
    private String checkNumber;
    private LocalDate checkDate;
    private String memo;
    private String notes;
    private boolean glPosted;
    private Long glJournalEntryId;
    private Instant glPostedAt;
    private boolean reconciled;
    private Instant reconciledAt;
    private Long reconciledBy;
    private Long submittedBy;
    private Instant submittedAt;
    private Long approvedBy;
    private Instant approvedAt;
    private Long completedBy;
    private Instant completedAt;
    private Long voidedBy;
    private Instant voidedAt;
    private String voidReason;
    private List<APPaymentAllocationDto> allocations;
    private Instant createdAt;
    private Instant updatedAt;

    // Calculated fields
    private boolean fullyAllocated;
    private BigDecimal availableAmount;
}
