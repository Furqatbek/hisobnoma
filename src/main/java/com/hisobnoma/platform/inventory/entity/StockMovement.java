package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * StockMovement entity representing a record of stock quantity changes.
 * Tracks all movements of inventory including receipts, issues, transfers, and adjustments.
 */
@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_stock_movements_tenant", columnList = "tenant_id"),
    @Index(name = "idx_stock_movements_product", columnList = "product_id"),
    @Index(name = "idx_stock_movements_from_location", columnList = "from_location_id"),
    @Index(name = "idx_stock_movements_to_location", columnList = "to_location_id"),
    @Index(name = "idx_stock_movements_type", columnList = "movement_type"),
    @Index(name = "idx_stock_movements_reference", columnList = "reference_type, reference_id"),
    @Index(name = "idx_stock_movements_date", columnList = "movement_date")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class StockMovement extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_location_id")
    private Location fromLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id")
    private Location toLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    /**
     * Unit cost at the time of movement
     */
    @Column(name = "unit_cost", precision = 18, scale = 4)
    private BigDecimal unitCost;

    /**
     * Total cost of the movement (quantity * unit cost)
     */
    @Column(name = "total_cost", precision = 18, scale = 4)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 30)
    private MovementReferenceType referenceType;

    /**
     * ID of the referenced document (PO, SO, etc.)
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * Human-readable reference number
     */
    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate;

    /**
     * Reason for the movement (especially for adjustments)
     */
    @Column(length = 200)
    private String reason;

    @Column(length = 1000)
    private String notes;

    /**
     * Batch number if batch tracking is enabled
     */
    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    /**
     * Serial number if serial tracking is enabled
     */
    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    /**
     * Stock quantity before the movement at source location
     */
    @Column(name = "quantity_before", precision = 18, scale = 4)
    private BigDecimal quantityBefore;

    /**
     * Stock quantity after the movement at destination location
     */
    @Column(name = "quantity_after", precision = 18, scale = 4)
    private BigDecimal quantityAfter;

    /**
     * Indicates if this movement has been reversed
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean reversed = false;

    /**
     * Reference to the reversal movement if this was reversed
     */
    @Column(name = "reversal_movement_id")
    private Long reversalMovementId;

    /**
     * Reference to the original movement if this is a reversal
     */
    @Column(name = "original_movement_id")
    private Long originalMovementId;

    @PrePersist
    protected void prePersist() {
        if (movementDate == null) {
            movementDate = LocalDateTime.now();
        }
        calculateTotalCost();
    }

    private void calculateTotalCost() {
        if (quantity != null && unitCost != null) {
            totalCost = quantity.multiply(unitCost);
        }
    }

    /**
     * Determines if this is an incoming movement (adds stock)
     */
    public boolean isIncoming() {
        return movementType == MovementType.STOCK_IN ||
               movementType == MovementType.RETURN ||
               movementType == MovementType.PRODUCTION ||
               movementType == MovementType.INITIAL ||
               (movementType == MovementType.ADJUSTMENT && quantity.compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Determines if this is an outgoing movement (removes stock)
     */
    public boolean isOutgoing() {
        return movementType == MovementType.STOCK_OUT ||
               movementType == MovementType.CONSUMPTION ||
               movementType == MovementType.WRITE_OFF ||
               (movementType == MovementType.ADJUSTMENT && quantity.compareTo(BigDecimal.ZERO) < 0);
    }
}
