package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.entity.TransactionStatus;
import com.hisobnoma.platform.pos.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POSTransactionDto {
    private Long id;
    private String transactionNumber;
    private Long terminalId;
    private String terminalCode;
    private String terminalName;
    private Long locationId;
    private String locationName;
    private Long shiftId;
    private String shiftNumber;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long cashierId;
    private String cashierName;
    private TransactionType transactionType;
    private TransactionStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
    private String discountReason;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal changeAmount;
    private BigDecimal balanceDue;
    private String currency;
    private Integer itemCount;
    private Integer lineCount;
    private Instant heldAt;
    private Long heldBy;
    private String heldReason;
    private Instant completedAt;
    private Long completedBy;
    private Instant voidedAt;
    private Long voidedBy;
    private String voidReason;
    private Long originalTransactionId;
    private String originalTransactionNumber;
    private String returnReason;
    private Long arInvoiceId;
    private Long glJournalEntryId;
    private boolean glPosted;
    private boolean stockDeducted;
    private Long deliveryRegionId;
    private String deliveryRegionName;
    private Long deliveryVillageId;
    private String deliveryVillageName;
    private String notes;
    private List<POSTransactionLineDto> lines;
    private List<POSPaymentDto> payments;
    private Instant createdAt;
}
