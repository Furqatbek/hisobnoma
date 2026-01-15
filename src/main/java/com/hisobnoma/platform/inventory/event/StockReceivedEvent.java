package com.hisobnoma.platform.inventory.event;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Event published when stock is received into inventory.
 * This includes receiving from purchase orders, returns, and manual stock-in.
 */
@Getter
public class StockReceivedEvent extends InventoryEvent {

    private final Long locationId;
    private final String locationCode;
    private final String locationName;
    private final BigDecimal quantity;
    private final BigDecimal unitCost;
    private final BigDecimal quantityBefore;
    private final BigDecimal quantityAfter;
    private final String referenceType;
    private final Long referenceId;
    private final String referenceNumber;
    private final String batchNumber;

    public StockReceivedEvent(Object source, Long tenantId, Long productId, String productSku, String productName,
                              Long locationId, String locationCode, String locationName,
                              BigDecimal quantity, BigDecimal unitCost,
                              BigDecimal quantityBefore, BigDecimal quantityAfter,
                              String referenceType, Long referenceId, String referenceNumber,
                              String batchNumber) {
        super(source, tenantId, productId, productSku, productName);
        this.locationId = locationId;
        this.locationCode = locationCode;
        this.locationName = locationName;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.referenceNumber = referenceNumber;
        this.batchNumber = batchNumber;
    }

    @Override
    public String getEventType() {
        return "STOCK_RECEIVED";
    }

    @Override
    public String getDescription() {
        return String.format("Received %s units of %s (%s) at %s. Stock: %s -> %s",
                quantity, getProductName(), getProductSku(), locationCode, quantityBefore, quantityAfter);
    }

    /**
     * Builder for creating StockReceivedEvent instances.
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
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private BigDecimal quantityBefore;
        private BigDecimal quantityAfter;
        private String referenceType;
        private Long referenceId;
        private String referenceNumber;
        private String batchNumber;

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
        public Builder quantity(BigDecimal quantity) { this.quantity = quantity; return this; }
        public Builder unitCost(BigDecimal unitCost) { this.unitCost = unitCost; return this; }
        public Builder quantityBefore(BigDecimal quantityBefore) { this.quantityBefore = quantityBefore; return this; }
        public Builder quantityAfter(BigDecimal quantityAfter) { this.quantityAfter = quantityAfter; return this; }
        public Builder referenceType(String referenceType) { this.referenceType = referenceType; return this; }
        public Builder referenceId(Long referenceId) { this.referenceId = referenceId; return this; }
        public Builder referenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; return this; }
        public Builder batchNumber(String batchNumber) { this.batchNumber = batchNumber; return this; }

        public StockReceivedEvent build() {
            return new StockReceivedEvent(source, tenantId, productId, productSku, productName,
                    locationId, locationCode, locationName, quantity, unitCost,
                    quantityBefore, quantityAfter, referenceType, referenceId, referenceNumber, batchNumber);
        }
    }
}
