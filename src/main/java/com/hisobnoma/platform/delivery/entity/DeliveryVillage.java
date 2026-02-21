package com.hisobnoma.platform.delivery.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "delivery_villages", indexes = {
        @Index(name = "idx_delivery_village_tenant", columnList = "tenant_id"),
        @Index(name = "idx_delivery_village_region", columnList = "region_id"),
        @Index(name = "idx_delivery_village_active", columnList = "tenant_id, active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DeliveryVillage extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private DeliveryRegion region;

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
}
