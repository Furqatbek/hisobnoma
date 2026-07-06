package com.hisobnoma.platform.distribution.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A B2B wholesale order taken by a distribution agent (or the back office) for a
 * finance {@link com.hisobnoma.platform.finance.entity.Customer}. Fulfilled from a
 * warehouse location, then converted to an AR invoice.
 *
 * <p>Deliberately separate from POS transactions and web orders: it has its own
 * agent attribution, credit/cash split, and multi-step fulfilment lifecycle.
 */
@Entity
@Table(name = "distribution_orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_dist_orders_number", columnNames = {"tenant_id", "order_number"}),
        indexes = {
                @Index(name = "idx_dist_orders_tenant", columnList = "tenant_id"),
                @Index(name = "idx_dist_orders_status", columnList = "tenant_id, status"),
                @Index(name = "idx_dist_orders_agent", columnList = "agent_id"),
                @Index(name = "idx_dist_orders_customer", columnList = "customer_id"),
                @Index(name = "idx_dist_orders_created", columnList = "tenant_id, created_at")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DistributionOrder extends TenantAwareEntity {

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DistributionOrderStatus status = DistributionOrderStatus.DRAFT;

    /** Agent who took the order; null for back-office orders. */
    @Column(name = "agent_id")
    private Long agentId;

    /** Field visit this order was taken during, if any. */
    @Column(name = "visit_id")
    private Long visitId;

    /** Route this order belongs to, if taken on a planned route. */
    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    /** Warehouse the order is fulfilled from. Resolved at confirmation if null. */
    @Column(name = "source_location_id")
    private Long sourceLocationId;

    /** Pricing basis: the customer's price list at order time (snapshot). */
    @Column(name = "price_list_id")
    private Long priceListId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 10)
    @Builder.Default
    private DistributionPaymentMethod paymentMethod = DistributionPaymentMethod.CREDIT;

    @Column(precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "delivery_fee", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** Cash taken on delivery (for CASH / MIXED). */
    @Column(name = "cash_collected", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal cashCollected = BigDecimal.ZERO;

    /** Portion billed to AR (for CREDIT / MIXED). */
    @Column(name = "credit_amount", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "ar_invoice_id")
    private Long arInvoiceId;

    @Column(name = "ar_invoice_number", length = 50)
    private String arInvoiceNumber;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_lat", precision = 10, scale = 7)
    private BigDecimal deliveryLat;

    @Column(name = "delivery_lng", precision = 10, scale = 7)
    private BigDecimal deliveryLng;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "UZS";

    @Column(length = 1000)
    private String notes;

    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    /**
     * False when a best-effort stock movement (reserve/release/deduct) for this order failed and
     * the order needs manual inventory reconciliation. True in the normal case.
     */
    @Column(name = "stock_settled", nullable = false)
    @Builder.Default
    private boolean stockSettled = true;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DistributionOrderLine> lines = new ArrayList<>();

    public void addLine(DistributionOrderLine line) {
        line.setOrder(this);
        this.lines.add(line);
    }

    /** Recomputes subtotal/total from the current lines and header adjustments. */
    public void recalculateTotals() {
        BigDecimal linesTotal = lines.stream()
                .map(DistributionOrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.subtotal = linesTotal;
        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        BigDecimal fee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        // Clamp once, AFTER adding tax + fee, so this matches exactly how
        // ARInvoiceService.createInvoice derives the invoice total (sum of lines
        // — including the fee/tax lines — minus the header discount). Clamping the
        // discounted subtotal before adding tax/fee would diverge when discount > subtotal.
        this.totalAmount = linesTotal.add(tax).add(fee).subtract(discount).max(BigDecimal.ZERO);
    }
}
