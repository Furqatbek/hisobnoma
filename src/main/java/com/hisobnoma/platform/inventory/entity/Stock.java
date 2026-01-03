package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Stock entity representing the inventory quantity of a product at a specific location.
 * Tracks on-hand, reserved, and available quantities.
 */
@Entity
@Table(name = "stock", indexes = {
    @Index(name = "idx_stock_tenant", columnList = "tenant_id"),
    @Index(name = "idx_stock_product", columnList = "product_id"),
    @Index(name = "idx_stock_location", columnList = "location_id"),
    @Index(name = "idx_stock_product_location", columnList = "product_id, location_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_stock_product_location", columnNames = {"product_id", "location_id", "tenant_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Stock extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /**
     * Total quantity physically on hand at the location
     */
    @Column(name = "quantity_on_hand", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    /**
     * Quantity reserved for pending sales/orders
     */
    @Column(name = "quantity_reserved", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    /**
     * Quantity in transit (coming from another location)
     */
    @Column(name = "quantity_in_transit", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal quantityInTransit = BigDecimal.ZERO;

    /**
     * Quantity on order (from purchase orders not yet received)
     */
    @Column(name = "quantity_on_order", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal quantityOnOrder = BigDecimal.ZERO;

    /**
     * Average cost of the stock at this location
     */
    @Column(name = "average_cost", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal averageCost = BigDecimal.ZERO;

    /**
     * Last known cost (most recent purchase price)
     */
    @Column(name = "last_cost", precision = 18, scale = 4)
    private BigDecimal lastCost;

    /**
     * Reorder point specific to this location (overrides product default)
     */
    @Column(name = "reorder_point", precision = 18, scale = 4)
    private BigDecimal reorderPoint;

    /**
     * Reorder quantity specific to this location
     */
    @Column(name = "reorder_quantity", precision = 18, scale = 4)
    private BigDecimal reorderQuantity;

    /**
     * Minimum stock level for this location
     */
    @Column(name = "min_stock_level", precision = 18, scale = 4)
    private BigDecimal minStockLevel;

    /**
     * Maximum stock level for this location
     */
    @Column(name = "max_stock_level", precision = 18, scale = 4)
    private BigDecimal maxStockLevel;

    /**
     * Calculates the available quantity (on-hand minus reserved)
     */
    public BigDecimal getQuantityAvailable() {
        return quantityOnHand.subtract(quantityReserved);
    }

    /**
     * Calculates the total stock value at average cost
     */
    public BigDecimal getStockValue() {
        if (averageCost == null) {
            return BigDecimal.ZERO;
        }
        return quantityOnHand.multiply(averageCost);
    }

    /**
     * Checks if stock is below reorder point
     */
    public boolean isBelowReorderPoint() {
        BigDecimal effectiveReorderPoint = reorderPoint != null ? reorderPoint :
            (product != null ? product.getReorderPoint() : BigDecimal.ZERO);
        return getQuantityAvailable().compareTo(effectiveReorderPoint) < 0;
    }

    /**
     * Checks if stock is below minimum level
     */
    public boolean isBelowMinimum() {
        BigDecimal effectiveMinLevel = minStockLevel != null ? minStockLevel :
            (product != null ? product.getMinStockLevel() : BigDecimal.ZERO);
        return quantityOnHand.compareTo(effectiveMinLevel) < 0;
    }

    /**
     * Updates the average cost using weighted average method
     */
    public void updateAverageCost(BigDecimal newQuantity, BigDecimal newCost) {
        if (newQuantity.compareTo(BigDecimal.ZERO) <= 0 || newCost == null) {
            return;
        }

        BigDecimal totalExistingValue = quantityOnHand.multiply(averageCost != null ? averageCost : BigDecimal.ZERO);
        BigDecimal totalNewValue = newQuantity.multiply(newCost);
        BigDecimal totalQuantity = quantityOnHand.add(newQuantity);

        if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
            this.averageCost = totalExistingValue.add(totalNewValue)
                    .divide(totalQuantity, 4, java.math.RoundingMode.HALF_UP);
        }

        this.lastCost = newCost;
    }
}
