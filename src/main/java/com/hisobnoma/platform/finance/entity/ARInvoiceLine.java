package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * AR Invoice line item entity.
 */
@Entity
@Table(name = "ar_invoice_lines", indexes = {
    @Index(name = "idx_ar_invoice_lines_invoice", columnList = "ar_invoice_id"),
    @Index(name = "idx_ar_invoice_lines_product", columnList = "product_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ARInvoiceLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ar_invoice_id", nullable = false)
    private ARInvoice arInvoice;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    /**
     * Item ID (inventory item reference).
     */
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_sku", length = 50)
    private String productSku;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(nullable = false, length = 500)
    private String description;

    /**
     * Revenue account for this line (overrides invoice default).
     */
    @Column(name = "revenue_account_id")
    private Long revenueAccountId;

    @Column(name = "revenue_account_code", length = 20)
    private String revenueAccountCode;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    /**
     * Unit cost for profit margin calculation.
     */
    @Column(name = "unit_cost", precision = 18, scale = 4)
    private BigDecimal unitCost;

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
     * Stored profit margin percentage.
     */
    @Column(name = "profit_margin", precision = 18, scale = 2)
    private BigDecimal profitMargin;

    /**
     * Link to POS transaction line if from POS.
     */
    @Column(name = "pos_line_id")
    private Long posLineId;

    /**
     * Link to Sales Order line if from order.
     */
    @Column(name = "sales_order_line_id")
    private Long salesOrderLineId;

    @Column(length = 500)
    private String notes;

    /**
     * Calculate line total based on quantity, price, and discounts.
     */
    public void calculateLineTotal() {
        BigDecimal grossAmount = quantity.multiply(unitPrice);

        // Apply discount
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = grossAmount.multiply(discountPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }

        lineTotal = grossAmount.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);

        // Calculate tax
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = lineTotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
    }

    /**
     * Calculate profit margin for this line.
     */
    public BigDecimal getProfitMargin() {
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal revenue = lineTotal;
        BigDecimal cost = unitCost.multiply(quantity);
        return revenue.subtract(cost);
    }

    /**
     * Calculate profit margin percentage.
     */
    public BigDecimal getProfitMarginPercent() {
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) == 0 || lineTotal.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal cost = unitCost.multiply(quantity);
        BigDecimal margin = lineTotal.subtract(cost);
        return margin.multiply(BigDecimal.valueOf(100)).divide(lineTotal, 2, RoundingMode.HALF_UP);
    }
}
