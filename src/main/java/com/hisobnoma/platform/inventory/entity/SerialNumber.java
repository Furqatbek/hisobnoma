package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SerialNumber entity for tracking individual serialized items.
 * Used for products that require unique identification (e.g., electronics, vehicles).
 */
@Entity
@Table(name = "serial_numbers", indexes = {
    @Index(name = "idx_serial_numbers_tenant", columnList = "tenant_id"),
    @Index(name = "idx_serial_numbers_product", columnList = "product_id"),
    @Index(name = "idx_serial_numbers_location", columnList = "location_id"),
    @Index(name = "idx_serial_numbers_serial", columnList = "serial_number"),
    @Index(name = "idx_serial_numbers_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_serial_number_product",
            columnNames = {"product_id", "serial_number", "tenant_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class SerialNumber extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SerialStatus status = SerialStatus.AVAILABLE;

    /**
     * Batch this serial belongs to (if applicable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private StockBatch batch;

    /**
     * Cost of this specific unit
     */
    @Column(name = "unit_cost", precision = 18, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    @Column(name = "sold_date")
    private LocalDateTime soldDate;

    /**
     * Supplier/Vendor ID for traceability
     */
    @Column(name = "supplier_id")
    private Long supplierId;

    /**
     * Customer ID if sold
     */
    @Column(name = "customer_id")
    private Long customerId;

    /**
     * Reference to the receiving order that created this serial
     */
    @Column(name = "receiving_order_id")
    private Long receivingOrderId;

    /**
     * Reference to the sales transaction that sold this serial
     */
    @Column(name = "sales_transaction_id")
    private Long salesTransactionId;

    /**
     * Manufacturer's serial number (may differ from internal)
     */
    @Column(name = "manufacturer_serial", length = 100)
    private String manufacturerSerial;

    /**
     * IMEI number (for phones)
     */
    @Column(length = 50)
    private String imei;

    /**
     * MAC address (for network devices)
     */
    @Column(name = "mac_address", length = 50)
    private String macAddress;

    @Column(length = 500)
    private String notes;

    /**
     * Indicates if this serial is reserved for an order
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean reserved = false;

    /**
     * Order ID if reserved
     */
    @Column(name = "reserved_for_order_id")
    private Long reservedForOrderId;

    public enum SerialStatus {
        AVAILABLE,
        RESERVED,
        SOLD,
        RETURNED,
        IN_TRANSIT,
        DAMAGED,
        LOST,
        EXPIRED,
        RECALLED
    }

    /**
     * Checks if the serial is available for sale
     */
    public boolean isAvailable() {
        return status == SerialStatus.AVAILABLE && !reserved;
    }

    /**
     * Checks if warranty is still valid
     */
    public boolean isUnderWarranty() {
        if (warrantyEndDate == null) {
            return false;
        }
        return warrantyEndDate.isAfter(LocalDate.now()) || warrantyEndDate.isEqual(LocalDate.now());
    }

    /**
     * Gets days remaining on warranty
     */
    public Long getWarrantyDaysRemaining() {
        if (warrantyEndDate == null || warrantyEndDate.isBefore(LocalDate.now())) {
            return 0L;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), warrantyEndDate);
    }

    /**
     * Marks the serial as sold
     */
    public void markAsSold(Long customerId, Long transactionId) {
        this.status = SerialStatus.SOLD;
        this.soldDate = LocalDateTime.now();
        this.customerId = customerId;
        this.salesTransactionId = transactionId;
        this.reserved = false;
        this.reservedForOrderId = null;

        // Set warranty start if sold and warranty period exists
        if (warrantyEndDate != null && warrantyStartDate == null) {
            warrantyStartDate = LocalDate.now();
        }
    }

    /**
     * Reserves the serial for an order
     */
    public void reserve(Long orderId) {
        this.reserved = true;
        this.reservedForOrderId = orderId;
        this.status = SerialStatus.RESERVED;
    }

    /**
     * Unreserves the serial
     */
    public void unreserve() {
        this.reserved = false;
        this.reservedForOrderId = null;
        this.status = SerialStatus.AVAILABLE;
    }
}
