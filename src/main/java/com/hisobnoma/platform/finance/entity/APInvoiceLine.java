package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * AP Invoice Line entity.
 * Represents individual line items on an AP invoice.
 */
@Entity
@Table(name = "ap_invoice_lines", indexes = {
    @Index(name = "idx_ap_invoice_lines_invoice", columnList = "ap_invoice_id"),
    @Index(name = "idx_ap_invoice_lines_product", columnList = "product_id"),
    @Index(name = "idx_ap_invoice_lines_account", columnList = "expense_account_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class APInvoiceLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ap_invoice_id", nullable = false)
    private APInvoice apInvoice;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    /**
     * Optional link to product (for inventory items).
     */
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_sku", length = 50)
    private String productSku;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(nullable = false, length = 500)
    private String description;

    /**
     * GL expense account for this line.
     */
    @Column(name = "expense_account_id")
    private Long expenseAccountId;

    @Column(name = "expense_account_code", length = 20)
    private String expenseAccountCode;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "discount_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /**
     * Link to PO line for 3-way matching.
     */
    @Column(name = "purchase_order_line_id")
    private Long purchaseOrderLineId;

    /**
     * Link to receiving line for 3-way matching.
     */
    @Column(name = "receiving_line_id")
    private Long receivingLineId;

    /**
     * Quantity ordered (from PO) for matching.
     */
    @Column(name = "ordered_quantity", precision = 18, scale = 4)
    private BigDecimal orderedQuantity;

    /**
     * Quantity received for matching.
     */
    @Column(name = "received_quantity", precision = 18, scale = 4)
    private BigDecimal receivedQuantity;

    /**
     * Variance between ordered/received and invoiced.
     */
    @Column(name = "variance_quantity", precision = 18, scale = 4)
    private BigDecimal varianceQuantity;

    @Column(name = "variance_amount", precision = 18, scale = 4)
    private BigDecimal varianceAmount;

    @Column(length = 500)
    private String notes;

    /**
     * Calculates and sets the line total.
     */
    public void calculateLineTotal() {
        BigDecimal gross = quantity.multiply(unitPrice);

        // Apply discount
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            this.discountAmount = gross.multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        }

        this.lineTotal = gross.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);

        // Calculate tax
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = lineTotal.multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * Calculates variance between ordered/received and invoiced quantities.
     */
    public void calculateVariance() {
        BigDecimal baseQty = receivedQuantity != null ? receivedQuantity :
                            (orderedQuantity != null ? orderedQuantity : BigDecimal.ZERO);

        if (baseQty.compareTo(BigDecimal.ZERO) > 0) {
            this.varianceQuantity = quantity.subtract(baseQty);
            this.varianceAmount = varianceQuantity.multiply(unitPrice);
        }
    }
}
