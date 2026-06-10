package com.hisobnoma.platform.web.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Line of an online shop order. Product name and price are snapshotted
 * at checkout time so later catalog changes don't alter the order.
 */
@Entity
@Table(name = "web_order_lines", indexes = {
        @Index(name = "idx_web_order_lines_order", columnList = "web_order_id"),
        @Index(name = "idx_web_order_lines_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WebOrderLine extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "web_order_id", nullable = false)
    private WebOrder webOrder;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "catalog_item_id")
    private Long catalogItemId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "unit_name", length = 50)
    private String unitName;

    @Column(precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 18, scale = 4, nullable = false)
    private BigDecimal lineTotal;
}
