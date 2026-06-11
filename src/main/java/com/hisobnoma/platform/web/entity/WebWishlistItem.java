package com.hisobnoma.platform.web.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "web_wishlist_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_web_wishlist_item",
                columnNames = {"web_customer_id", "catalog_item_id"}),
        indexes = {
                @Index(name = "idx_web_wishlist_tenant_item", columnList = "tenant_id, catalog_item_id"),
                @Index(name = "idx_web_wishlist_tenant_customer", columnList = "tenant_id, web_customer_id")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WebWishlistItem extends TenantAwareEntity {

    @Column(name = "web_customer_id", nullable = false)
    private Long webCustomerId;

    @Column(name = "catalog_item_id", nullable = false)
    private Long catalogItemId;

    @Column(name = "last_known_available", nullable = false)
    @Builder.Default
    private boolean lastKnownAvailable = true;

    @Column(name = "last_notified_sale_price", precision = 18, scale = 4)
    private BigDecimal lastNotifiedSalePrice;

    @Column(name = "notified_at")
    private Instant notifiedAt;
}
