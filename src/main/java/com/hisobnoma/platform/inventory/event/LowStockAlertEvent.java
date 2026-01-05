package com.hisobnoma.platform.inventory.event;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Event published when stock falls below the reorder point or minimum level.
 * Can be used to trigger notifications, automatic purchase orders, or alerts.
 */
@Getter
public class LowStockAlertEvent extends InventoryEvent {

    private final Long locationId;
    private final String locationCode;
    private final String locationName;
    private final BigDecimal currentQuantity;
    private final BigDecimal reorderPoint;
    private final BigDecimal minimumStockLevel;
    private final BigDecimal reorderQuantity;
    private final AlertSeverity severity;
    private final String categoryName;
    private final String brandName;

    public enum AlertSeverity {
        /**
         * Stock is out - zero or negative quantity
         */
        CRITICAL,
        /**
         * Stock is below minimum level
         */
        LOW,
        /**
         * Stock is below reorder point but above minimum
         */
        WARNING
    }

    public LowStockAlertEvent(Object source, Long tenantId, Long productId, String productSku, String productName,
                               Long locationId, String locationCode, String locationName,
                               BigDecimal currentQuantity, BigDecimal reorderPoint,
                               BigDecimal minimumStockLevel, BigDecimal reorderQuantity,
                               AlertSeverity severity, String categoryName, String brandName) {
        super(source, tenantId, productId, productSku, productName);
        this.locationId = locationId;
        this.locationCode = locationCode;
        this.locationName = locationName;
        this.currentQuantity = currentQuantity;
        this.reorderPoint = reorderPoint;
        this.minimumStockLevel = minimumStockLevel;
        this.reorderQuantity = reorderQuantity;
        this.severity = severity;
        this.categoryName = categoryName;
        this.brandName = brandName;
    }

    @Override
    public String getEventType() {
        return "LOW_STOCK_ALERT";
    }

    @Override
    public String getDescription() {
        return String.format("[%s] Low stock alert for %s (%s) at %s. Current: %s, Reorder Point: %s",
                severity, productName, productSku, locationCode, currentQuantity, reorderPoint);
    }

    /**
     * Calculates the shortfall quantity (how much below reorder point).
     */
    public BigDecimal getShortfallQuantity() {
        return reorderPoint.subtract(currentQuantity).max(BigDecimal.ZERO);
    }

    /**
     * Returns the suggested order quantity (reorder quantity or shortfall, whichever is greater).
     */
    public BigDecimal getSuggestedOrderQuantity() {
        if (reorderQuantity != null && reorderQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return reorderQuantity.max(getShortfallQuantity());
        }
        return getShortfallQuantity();
    }

    /**
     * Returns true if stock is completely out.
     */
    public boolean isOutOfStock() {
        return currentQuantity.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Builder for creating LowStockAlertEvent instances.
     */
    public static Builder builder(Object source) {
        return new Builder(source);
    }

    public static class Builder {
        private final Object source;
        private Long tenantId;
        private Long productId;
        private String productSku;
        private String productName;
        private Long locationId;
        private String locationCode;
        private String locationName;
        private BigDecimal currentQuantity;
        private BigDecimal reorderPoint;
        private BigDecimal minimumStockLevel;
        private BigDecimal reorderQuantity;
        private AlertSeverity severity;
        private String categoryName;
        private String brandName;

        public Builder(Object source) {
            this.source = source;
        }

        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder productId(Long productId) { this.productId = productId; return this; }
        public Builder productSku(String productSku) { this.productSku = productSku; return this; }
        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder locationId(Long locationId) { this.locationId = locationId; return this; }
        public Builder locationCode(String locationCode) { this.locationCode = locationCode; return this; }
        public Builder locationName(String locationName) { this.locationName = locationName; return this; }
        public Builder currentQuantity(BigDecimal currentQuantity) { this.currentQuantity = currentQuantity; return this; }
        public Builder reorderPoint(BigDecimal reorderPoint) { this.reorderPoint = reorderPoint; return this; }
        public Builder minimumStockLevel(BigDecimal minimumStockLevel) { this.minimumStockLevel = minimumStockLevel; return this; }
        public Builder reorderQuantity(BigDecimal reorderQuantity) { this.reorderQuantity = reorderQuantity; return this; }
        public Builder severity(AlertSeverity severity) { this.severity = severity; return this; }
        public Builder categoryName(String categoryName) { this.categoryName = categoryName; return this; }
        public Builder brandName(String brandName) { this.brandName = brandName; return this; }

        public LowStockAlertEvent build() {
            return new LowStockAlertEvent(source, tenantId, productId, productSku, productName,
                    locationId, locationCode, locationName, currentQuantity, reorderPoint,
                    minimumStockLevel, reorderQuantity, severity, categoryName, brandName);
        }
    }
}
