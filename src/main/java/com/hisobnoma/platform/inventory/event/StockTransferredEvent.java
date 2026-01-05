package com.hisobnoma.platform.inventory.event;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Event published when stock is transferred between locations.
 */
@Getter
public class StockTransferredEvent extends InventoryEvent {

    private final Long fromLocationId;
    private final String fromLocationCode;
    private final String fromLocationName;
    private final Long toLocationId;
    private final String toLocationCode;
    private final String toLocationName;
    private final BigDecimal quantity;
    private final BigDecimal unitCost;
    private final BigDecimal sourceQuantityBefore;
    private final BigDecimal sourceQuantityAfter;
    private final BigDecimal destQuantityBefore;
    private final BigDecimal destQuantityAfter;
    private final String referenceNumber;
    private final String reason;
    private final String batchNumber;

    public StockTransferredEvent(Object source, Long tenantId, Long productId, String productSku, String productName,
                                  Long fromLocationId, String fromLocationCode, String fromLocationName,
                                  Long toLocationId, String toLocationCode, String toLocationName,
                                  BigDecimal quantity, BigDecimal unitCost,
                                  BigDecimal sourceQuantityBefore, BigDecimal sourceQuantityAfter,
                                  BigDecimal destQuantityBefore, BigDecimal destQuantityAfter,
                                  String referenceNumber, String reason, String batchNumber) {
        super(source, tenantId, productId, productSku, productName);
        this.fromLocationId = fromLocationId;
        this.fromLocationCode = fromLocationCode;
        this.fromLocationName = fromLocationName;
        this.toLocationId = toLocationId;
        this.toLocationCode = toLocationCode;
        this.toLocationName = toLocationName;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.sourceQuantityBefore = sourceQuantityBefore;
        this.sourceQuantityAfter = sourceQuantityAfter;
        this.destQuantityBefore = destQuantityBefore;
        this.destQuantityAfter = destQuantityAfter;
        this.referenceNumber = referenceNumber;
        this.reason = reason;
        this.batchNumber = batchNumber;
    }

    @Override
    public String getEventType() {
        return "STOCK_TRANSFERRED";
    }

    @Override
    public String getDescription() {
        return String.format("Transferred %s units of %s (%s) from %s to %s",
                quantity, productName, productSku, fromLocationCode, toLocationCode);
    }

    /**
     * Builder for creating StockTransferredEvent instances.
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
        private Long fromLocationId;
        private String fromLocationCode;
        private String fromLocationName;
        private Long toLocationId;
        private String toLocationCode;
        private String toLocationName;
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private BigDecimal sourceQuantityBefore;
        private BigDecimal sourceQuantityAfter;
        private BigDecimal destQuantityBefore;
        private BigDecimal destQuantityAfter;
        private String referenceNumber;
        private String reason;
        private String batchNumber;

        public Builder(Object source) {
            this.source = source;
        }

        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder productId(Long productId) { this.productId = productId; return this; }
        public Builder productSku(String productSku) { this.productSku = productSku; return this; }
        public Builder productName(String productName) { this.productName = productName; return this; }
        public Builder fromLocationId(Long fromLocationId) { this.fromLocationId = fromLocationId; return this; }
        public Builder fromLocationCode(String fromLocationCode) { this.fromLocationCode = fromLocationCode; return this; }
        public Builder fromLocationName(String fromLocationName) { this.fromLocationName = fromLocationName; return this; }
        public Builder toLocationId(Long toLocationId) { this.toLocationId = toLocationId; return this; }
        public Builder toLocationCode(String toLocationCode) { this.toLocationCode = toLocationCode; return this; }
        public Builder toLocationName(String toLocationName) { this.toLocationName = toLocationName; return this; }
        public Builder quantity(BigDecimal quantity) { this.quantity = quantity; return this; }
        public Builder unitCost(BigDecimal unitCost) { this.unitCost = unitCost; return this; }
        public Builder sourceQuantityBefore(BigDecimal sourceQuantityBefore) { this.sourceQuantityBefore = sourceQuantityBefore; return this; }
        public Builder sourceQuantityAfter(BigDecimal sourceQuantityAfter) { this.sourceQuantityAfter = sourceQuantityAfter; return this; }
        public Builder destQuantityBefore(BigDecimal destQuantityBefore) { this.destQuantityBefore = destQuantityBefore; return this; }
        public Builder destQuantityAfter(BigDecimal destQuantityAfter) { this.destQuantityAfter = destQuantityAfter; return this; }
        public Builder referenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder batchNumber(String batchNumber) { this.batchNumber = batchNumber; return this; }

        public StockTransferredEvent build() {
            return new StockTransferredEvent(source, tenantId, productId, productSku, productName,
                    fromLocationId, fromLocationCode, fromLocationName,
                    toLocationId, toLocationCode, toLocationName,
                    quantity, unitCost, sourceQuantityBefore, sourceQuantityAfter,
                    destQuantityBefore, destQuantityAfter, referenceNumber, reason, batchNumber);
        }
    }
}
