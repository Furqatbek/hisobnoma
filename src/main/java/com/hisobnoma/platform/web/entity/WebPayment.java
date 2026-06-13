package com.hisobnoma.platform.web.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An online card-payment attempt against a {@link WebOrder}.
 *
 * The customer is redirected to {@link #paymentUrl} (Payme/Click/Uzum); the
 * provider later confirms the outcome via webhook (or staff confirm it). The
 * mobile app polls {@code GET /web/payments/{publicId}} using the opaque
 * {@link #publicId} — never the order number or phone, to keep PII out of
 * URLs and logs.
 */
@Entity
@Table(name = "web_payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_web_payments_public_id", columnNames = "public_id"),
        indexes = {
                @Index(name = "idx_web_payments_order", columnList = "tenant_id, web_order_id"),
                @Index(name = "idx_web_payments_provider_ref", columnList = "provider, provider_reference")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WebPayment extends TenantAwareEntity {

    @Column(name = "web_order_id", nullable = false)
    private Long webOrderId;

    /** Denormalized for provider callbacks and logging. */
    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    /** Opaque, unguessable id handed to the app for status polling. */
    @Column(name = "public_id", nullable = false, length = 40)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WebPaymentProviderType provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WebPaymentStatus status = WebPaymentStatus.PENDING;

    @Column(precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "UZS";

    /** The provider's own transaction id, learned from the callback. */
    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "payment_url", length = 1000)
    private String paymentUrl;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "paid_at")
    private Instant paidAt;
}
