package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.entity.ShiftStatus;
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
public class ShiftDto {
    private Long id;
    private String shiftNumber;
    private Long terminalId;
    private String terminalCode;
    private String terminalName;
    private Long cashierId;
    private String cashierName;
    private ShiftStatus status;
    private Instant openedAt;
    private Instant closedAt;
    private BigDecimal openingCash;
    private BigDecimal closingCash;
    private BigDecimal expectedCash;
    private BigDecimal cashDifference;
    private BigDecimal totalSales;
    private BigDecimal totalReturns;
    private BigDecimal totalDiscounts;
    private BigDecimal totalTaxes;
    private BigDecimal cashPayments;
    private BigDecimal cardPayments;
    private BigDecimal otherPayments;
    private Integer transactionCount;
    private Integer voidedCount;
    private Integer returnCount;
    private BigDecimal cashIn;
    private BigDecimal cashOut;
    private String notes;
    private String closingNotes;
    private Instant createdAt;
}
