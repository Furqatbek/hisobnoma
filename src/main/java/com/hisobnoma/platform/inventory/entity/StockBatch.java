package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * StockBatch entity for tracking inventory by batch/lot.
 * Used for products that require batch tracking (e.g., food, pharmaceuticals).
 */
@Entity
@Table(name = "stock_batches", indexes = {
    @Index(name = "idx_stock_batches_tenant", columnList = "tenant_id"),
    @Index(name = "idx_stock_batches_product", columnList = "product_id"),
    @Index(name = "idx_stock_batches_location", columnList = "location_id"),
    @Index(name = "idx_stock_batches_number", columnList = "batch_number"),
    @Index(name = "idx_stock_batches_expiry", columnList = "expiry_date"),
    @Index(name = "idx_stock_batches_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_stock_batch_product_location_number",
            columnNames = {"product_id", "location_id", "batch_number", "tenant_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class StockBatch extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    /**
     * Current quantity of this batch at the location
     */
    @Column(nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    /**
     * Initial quantity when batch was received
     */
    @Column(name = "initial_quantity", precision = 18, scale = 4)
    private BigDecimal initialQuantity;

    /**
     * Reserved quantity from this batch
     */
    @Column(name = "quantity_reserved", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    /**
     * Supplier/vendor batch number (may differ from internal batch number)
     */
    @Column(name = "supplier_batch_number", length = 100)
    private String supplierBatchNumber;

    /**
     * Cost per unit for this batch
     */
    @Column(name = "unit_cost", precision = 18, scale = 4)
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BatchStatus status = BatchStatus.ACTIVE;

    @Column(length = 500)
    private String notes;

    /**
     * Supplier/Vendor ID for traceability
     */
    @Column(name = "supplier_id")
    private Long supplierId;

    /**
     * Reference to the receiving order that created this batch
     */
    @Column(name = "receiving_order_id")
    private Long receivingOrderId;

    public enum BatchStatus {
        ACTIVE,
        QUARANTINE,
        EXPIRED,
        DEPLETED,
        RECALLED
    }

    /**
     * Gets available quantity (total - reserved)
     */
    public BigDecimal getQuantityAvailable() {
        return quantity.subtract(quantityReserved);
    }

    /**
     * Checks if the batch is expired
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Checks if the batch is expiring within given days
     */
    public boolean isExpiringWithin(int days) {
        if (expiryDate == null) {
            return false;
        }
        return expiryDate.isBefore(LocalDate.now().plusDays(days));
    }

    /**
     * Gets the number of days until expiry
     */
    public Long getDaysUntilExpiry() {
        if (expiryDate == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    /**
     * Gets the total value of this batch
     */
    public BigDecimal getValue() {
        if (unitCost == null) {
            return BigDecimal.ZERO;
        }
        return quantity.multiply(unitCost);
    }

    /**
     * Checks if batch has available quantity
     */
    public boolean hasAvailableQuantity() {
        return getQuantityAvailable().compareTo(BigDecimal.ZERO) > 0;
    }
}
