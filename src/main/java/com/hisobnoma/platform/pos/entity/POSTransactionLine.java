package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * POS Transaction Line entity.
 * Represents a line item in a POS transaction.
 */
@Entity
@Table(name = "pos_transaction_lines", indexes = {
    @Index(name = "idx_pos_tx_lines_transaction", columnList = "transaction_id"),
    @Index(name = "idx_pos_tx_lines_product", columnList = "product_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class POSTransactionLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private POSTransaction transaction;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "variant_name", length = 200)
    private String variantName;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "original_price", precision = 18, scale = 4)
    private BigDecimal originalPrice;

    @Column(name = "cost_price", precision = 18, scale = 4)
    private BigDecimal costPrice;

    @Column(name = "discount_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "discount_reason", length = 200)
    private String discountReason;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "line_total", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "is_return")
    @Builder.Default
    private boolean isReturn = false;

    @Column(name = "original_line_id")
    private Long originalLineId;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "location_id")
    private Long locationId;

    @Column(length = 500)
    private String notes;

    @PrePersist
    @PreUpdate
    public void calculateLineTotal() {
        BigDecimal grossAmount = unitPrice.multiply(quantity);
        BigDecimal discountedAmount = grossAmount.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        this.lineTotal = discountedAmount.add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
    }

    public void applyPercentDiscount(BigDecimal percent, String reason) {
        this.discountPercent = percent;
        this.discountReason = reason;
        BigDecimal grossAmount = unitPrice.multiply(quantity);
        this.discountAmount = grossAmount.multiply(percent).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        calculateLineTotal();
    }

    public void applyFixedDiscount(BigDecimal amount, String reason) {
        this.discountAmount = amount;
        this.discountReason = reason;
        this.discountPercent = null;
        calculateLineTotal();
    }

    public BigDecimal getGrossAmount() {
        return unitPrice.multiply(quantity);
    }

    public BigDecimal getNetAmount() {
        return getGrossAmount().subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
    }

    public BigDecimal getProfit() {
        if (costPrice == null) {
            return BigDecimal.ZERO;
        }
        return getNetAmount().subtract(costPrice.multiply(quantity));
    }
}
