package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.entity.POSPaymentStatus;
import com.hisobnoma.platform.pos.entity.POSPaymentType;
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
public class POSPaymentDto {
    private Long id;
    private Long transactionId;
    private Integer paymentNumber;
    private POSPaymentType paymentType;
    private POSPaymentStatus status;
    private BigDecimal amount;
    private BigDecimal tenderedAmount;
    private BigDecimal changeAmount;
    private String currency;
    private String cardType;
    private String cardLastFour;
    private String authCode;
    private String gatewayReference;
    private String giftCardNumber;
    private String checkNumber;
    private String mobileReference;
    private Instant processedAt;
    private Long processedBy;
    private Instant voidedAt;
    private Long voidedBy;
    private String voidReason;
    private Instant refundedAt;
    private Long refundedBy;
    private BigDecimal refundAmount;
    private String refundReason;
    private String notes;
    private Instant createdAt;
}
