package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * ProductAttribute entity for custom product fields/attributes.
 * Supports flexible key-value pairs for product specifications.
 */
@Entity
@Table(name = "product_attributes", indexes = {
    @Index(name = "idx_product_attrs_product", columnList = "product_id"),
    @Index(name = "idx_product_attrs_name", columnList = "attribute_name"),
    @Index(name = "idx_product_attrs_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ProductAttribute extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    @Column(name = "attribute_value", length = 500)
    private String attributeValue;

    @Column(name = "attribute_type", length = 50)
    @Builder.Default
    private String attributeType = "TEXT";

    @Column(name = "attribute_group", length = 100)
    private String attributeGroup;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private boolean visible = true;

    @Column(name = "is_searchable", nullable = false)
    @Builder.Default
    private boolean searchable = false;

    @Column(name = "is_filterable", nullable = false)
    @Builder.Default
    private boolean filterable = false;
}
