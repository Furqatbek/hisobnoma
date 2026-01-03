package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.ARPaymentMethod;
import com.hisobnoma.platform.finance.entity.ARPaymentStatus;
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
public class ARPaymentDto {
    private Long id;
    private Long tenantId;
    private String paymentNumber;
    private Long customerId;
    private String customerName;
    private LocalDate paymentDate;
    private ARPaymentStatus status;
    private ARPaymentMethod paymentMethod;
    private BigDecimal paymentAmount;
    private BigDecimal allocatedAmount;
    private BigDecimal unallocatedAmount;
    private String currency;
    private BigDecimal exchangeRate;
    private Long bankAccountId;
    private String bankAccountName;
    private Long cashAccountId;
    private Long arAccountId;
    private String referenceNumber;
    private String checkNumber;
    private LocalDate checkDate;
    private String cardLastFour;
    private String cardType;
    private String transactionId;
    private String memo;
    private String notes;
    private boolean glPosted;
    private Long glJournalEntryId;
    private Instant glPostedAt;
    private boolean deposited;
    private Instant depositedAt;
    private String depositReference;
    private boolean fullyAllocated;
    private BigDecimal availableAmount;
    private List<ARPaymentAllocationDto> allocations;
    private Instant createdAt;
    private Instant updatedAt;
}
