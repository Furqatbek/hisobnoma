package com.hisobnoma.platform.web.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Order placed by a customer through the online shop (mobile app).
 * Deliberately separate from POS transactions (which are bound to shifts)
 * and converted to an AR invoice by an explicit staff action.
 */
@Entity
@Table(name = "web_orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_web_orders_number", columnNames = {"tenant_id", "order_number"}),
        indexes = {
                @Index(name = "idx_web_orders_tenant", columnList = "tenant_id"),
                @Index(name = "idx_web_orders_status", columnList = "tenant_id, status"),
                @Index(name = "idx_web_orders_created", columnList = "tenant_id, created_at")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WebOrder extends TenantAwareEntity {

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WebOrderStatus status = WebOrderStatus.NEW;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(name = "delivery_region_id")
    private Long deliveryRegionId;

    @Column(name = "delivery_region_name", length = 100)
    private String deliveryRegionName;

    @Column(name = "delivery_village_id")
    private Long deliveryVillageId;

    @Column(name = "delivery_village_name", length = 100)
    private String deliveryVillageName;

    @Column(name = "customer_note", length = 500)
    private String customerNote;

    @Column(name = "total_amount", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Linked AR customer, set when the order is converted to an invoice.
     */
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "ar_invoice_id")
    private Long arInvoiceId;

    @Column(name = "ar_invoice_number", length = 50)
    private String arInvoiceNumber;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "webOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WebOrderLine> lines = new ArrayList<>();

    public void addLine(WebOrderLine line) {
        lines.add(line);
        line.setWebOrder(this);
    }

    public void recalculateTotal() {
        this.totalAmount = lines.stream()
                .map(WebOrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
