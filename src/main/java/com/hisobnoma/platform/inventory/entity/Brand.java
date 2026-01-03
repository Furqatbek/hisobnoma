package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Brand entity for product brand management.
 */
@Entity
@Table(name = "brands", indexes = {
    @Index(name = "idx_brands_tenant", columnList = "tenant_id"),
    @Index(name = "idx_brands_code", columnList = "code")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Brand extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 255)
    private String website;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
