package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * ProductImage entity for storing product images.
 * Supports multiple images per product with sort ordering.
 */
@Entity
@Table(name = "product_images", indexes = {
    @Index(name = "idx_product_images_product", columnList = "product_id"),
    @Index(name = "idx_product_images_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ProductImage extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "alt_text", length = 200)
    private String altText;

    @Column(length = 200)
    private String title;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "\"primary\"", nullable = false)
    @Builder.Default
    private boolean primary = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
