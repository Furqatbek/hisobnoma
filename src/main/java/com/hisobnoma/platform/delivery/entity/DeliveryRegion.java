package com.hisobnoma.platform.delivery.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delivery_regions", indexes = {
        @Index(name = "idx_delivery_region_tenant", columnList = "tenant_id"),
        @Index(name = "idx_delivery_region_active", columnList = "tenant_id, active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DeliveryRegion extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * Delivery fee applied to online shop orders in this region.
     */
    @Column(name = "delivery_fee", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private java.math.BigDecimal deliveryFee = java.math.BigDecimal.ZERO;

    @PrePersist
    @PreUpdate
    void normalizeDeliveryFee() {
        // The DTO mapper may set null when the field is omitted by clients
        if (deliveryFee == null) {
            deliveryFee = java.math.BigDecimal.ZERO;
        }
    }

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeliveryVillage> villages = new ArrayList<>();
}
