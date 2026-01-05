package com.hisobnoma.platform.inventory.event;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Event published when stock is adjusted (inventory count, correction, write-off).
 */
@Getter
public class StockAdjustedEvent extends InventoryEvent {

    private final Long locationId;
    private final String locationCode;
    private final String locationName;
    private final BigDecimal adjustmentQuantity;
    private final BigDecimal quantityBefore;
    private final BigDecimal quantityAfter;
    private final String referenceType;
    private final Long referenceId;
    private final String referenceNumber;
    private final String reason;
    private final AdjustmentType adjustmentType;

    public enum AdjustmentType {
        INVENTORY_COUNT,
        CORRECTION,
        WRITE_OFF,
        DAMAGE,
        EXPIRY,
        MANUAL
    }

    public StockAdjustedEvent(Object source, Long tenantId, Long productId, String productSku, String productName,
                               Long locationId, String locationCode, String locationName,
                               BigDecimal adjustmentQuantity, BigDecimal quantityBefore, BigDecimal quantityAfter,
                               String referenceType, Long referenceId, String referenceNumber,
                               String reason, AdjustmentType adjustmentType) {
        super(source, tenantId, productId, productSku, productName);
        this.locationId = locationId;
        this.locationCode = locationCode;
        this.locationName = locationName;
        this.adjustmentQuantity = adjustmentQuantity;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.referenceNumber = referenceNumber;
        this.reason = reason;
        this.adjustmentType = adjustmentType;
    }

    @Override
    public String getEventType() {
        return "STOCK_ADJUSTED";
    }

    @Override
    public String getDescription() {
        String direction = adjustmentQuantity.compareTo(BigDecimal.ZERO) >= 0 ? "increased" : "decreased";
        return String.format("Stock %s by %s units for %s (%s) at %s. Stock: %s -> %s. Reason: %s",
                direction, adjustmentQuantity.abs(), productName, productSku, locationCode,
                quantityBefore, quantityAfter, reason != null ? reason : "N/A");
    }

    /**
     * Returns true if this was a positive adjustment (stock increase).
     */
    public boolean isPositiveAdjustment() {
        return adjustmentQuantity.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns true if this was a negative adjustment (stock decrease).
     */
    public boolean isNegativeAdjustment() {
        return adjustmentQuantity.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Builder for creating StockAdjustedEvent instances.
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
        private BigDecimal adjustmentQuantity;
        private BigDecimal quantityBefore;
        private BigDecimal quantityAfter;
        private String referenceType;
        private Long referenceId;
        private String referenceNumber;
        private String reason;
        private AdjustmentType adjustmentType = AdjustmentType.MANUAL;

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
        public Builder adjustmentQuantity(BigDecimal adjustmentQuantity) { this.adjustmentQuantity = adjustmentQuantity; return this; }
        public Builder quantityBefore(BigDecimal quantityBefore) { this.quantityBefore = quantityBefore; return this; }
        public Builder quantityAfter(BigDecimal quantityAfter) { this.quantityAfter = quantityAfter; return this; }
        public Builder referenceType(String referenceType) { this.referenceType = referenceType; return this; }
        public Builder referenceId(Long referenceId) { this.referenceId = referenceId; return this; }
        public Builder referenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder adjustmentType(AdjustmentType adjustmentType) { this.adjustmentType = adjustmentType; return this; }

        public StockAdjustedEvent build() {
            return new StockAdjustedEvent(source, tenantId, productId, productSku, productName,
                    locationId, locationCode, locationName, adjustmentQuantity,
                    quantityBefore, quantityAfter, referenceType, referenceId, referenceNumber,
                    reason, adjustmentType);
        }
    }
}
