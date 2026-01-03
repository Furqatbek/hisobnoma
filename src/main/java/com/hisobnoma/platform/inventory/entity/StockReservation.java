package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * StockReservation entity for tracking pending stock reservations.
 * Used to hold stock for pending orders before they are completed.
 */
@Entity
@Table(name = "stock_reservations", indexes = {
    @Index(name = "idx_stock_reservations_tenant", columnList = "tenant_id"),
    @Index(name = "idx_stock_reservations_product", columnList = "product_id"),
    @Index(name = "idx_stock_reservations_location", columnList = "location_id"),
    @Index(name = "idx_stock_reservations_reference", columnList = "reference_type, reference_id"),
    @Index(name = "idx_stock_reservations_status", columnList = "status"),
    @Index(name = "idx_stock_reservations_expires", columnList = "expires_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class StockReservation extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "reserved_at", nullable = false)
    @Builder.Default
    private LocalDateTime reservedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private MovementReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(length = 500)
    private String notes;

    public enum ReservationStatus {
        ACTIVE,
        COMPLETED,
        CANCELLED,
        EXPIRED
    }

    /**
     * Checks if the reservation has expired
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Checks if the reservation is still active
     */
    public boolean isActive() {
        return status == ReservationStatus.ACTIVE && !isExpired();
    }

    /**
     * Marks the reservation as completed
     */
    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    /**
     * Cancels the reservation
     */
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    /**
     * Marks the reservation as expired
     */
    public void markExpired() {
        this.status = ReservationStatus.EXPIRED;
    }
}
