package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * POS Payment entity.
 * Represents a payment for a POS transaction.
 * Supports multiple payments per transaction (split payments).
 */
@Entity
@Table(name = "pos_payments", indexes = {
    @Index(name = "idx_pos_payments_transaction", columnList = "transaction_id"),
    @Index(name = "idx_pos_payments_type", columnList = "payment_type"),
    @Index(name = "idx_pos_payments_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class POSPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private POSTransaction transaction;

    @Column(name = "payment_number", nullable = false)
    private Integer paymentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private POSPaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private POSPaymentStatus status = POSPaymentStatus.PENDING;

    @Column(precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "tendered_amount", precision = 18, scale = 4)
    private BigDecimal tenderedAmount;

    @Column(name = "change_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    /**
     * For card payments - card type (VISA, MASTERCARD, etc.)
     */
    @Column(name = "card_type", length = 50)
    private String cardType;

    /**
     * Last 4 digits of card
     */
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    /**
     * Card authorization code
     */
    @Column(name = "auth_code", length = 50)
    private String authCode;

    /**
     * Payment gateway reference
     */
    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    /**
     * Payment gateway response
     */
    @Column(name = "gateway_response", length = 1000)
    private String gatewayResponse;

    /**
     * For gift card payments
     */
    @Column(name = "gift_card_number", length = 50)
    private String giftCardNumber;

    /**
     * For check payments
     */
    @Column(name = "check_number", length = 50)
    private String checkNumber;

    /**
     * For mobile payments (reference)
     */
    @Column(name = "mobile_reference", length = 100)
    private String mobileReference;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by")
    private Long voidedBy;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "refunded_by")
    private Long refundedBy;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refund_amount", precision = 18, scale = 4)
    private BigDecimal refundAmount;

    @Column(length = 500)
    private String notes;

    public void calculateChange() {
        if (paymentType == POSPaymentType.CASH && tenderedAmount != null) {
            if (tenderedAmount.compareTo(amount) > 0) {
                this.changeAmount = tenderedAmount.subtract(amount);
            } else {
                this.changeAmount = BigDecimal.ZERO;
            }
        }
    }

    public void approve() {
        this.status = POSPaymentStatus.APPROVED;
        this.processedAt = Instant.now();
    }

    public void decline() {
        this.status = POSPaymentStatus.DECLINED;
        this.processedAt = Instant.now();
    }

    public void voidPayment(Long userId, String reason) {
        this.status = POSPaymentStatus.VOIDED;
        this.voidedAt = Instant.now();
        this.voidedBy = userId;
        this.voidReason = reason;
    }

    public void refund(Long userId, BigDecimal refundAmount, String reason) {
        this.status = POSPaymentStatus.REFUNDED;
        this.refundedAt = Instant.now();
        this.refundedBy = userId;
        this.refundAmount = refundAmount;
        this.refundReason = reason;
    }
}
