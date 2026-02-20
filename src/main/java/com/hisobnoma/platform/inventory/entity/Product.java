package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Product entity representing items in the inventory.
 * Contains pricing, stock settings, and relationships to categories, brands, and units.
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_tenant", columnList = "tenant_id"),
    @Index(name = "idx_products_sku", columnList = "sku"),
    @Index(name = "idx_products_barcode", columnList = "barcode"),
    @Index(name = "idx_products_category", columnList = "category_id"),
    @Index(name = "idx_products_brand", columnList = "brand_id"),
    @Index(name = "idx_products_active", columnList = "active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Product extends TenantAwareEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(length = 50)
    private String barcode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_uom_id", nullable = false)
    private UnitOfMeasure baseUom;

    @Column(name = "cost_price", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "selling_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal sellingPrice;

    @Column(name = "min_selling_price", precision = 18, scale = 4)
    private BigDecimal minSellingPrice;

    @Column(name = "wholesale_price", precision = 18, scale = 4)
    private BigDecimal wholesalePrice;

    @Column(name = "track_inventory", nullable = false)
    @Builder.Default
    private boolean trackInventory = true;

    @Column(name = "allow_negative_stock", nullable = false)
    @Builder.Default
    private boolean allowNegativeStock = false;

    @Column(name = "min_stock_level", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal minStockLevel = BigDecimal.ZERO;

    @Column(name = "reorder_point", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal reorderPoint = BigDecimal.ZERO;

    @Column(name = "reorder_quantity", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal reorderQuantity = BigDecimal.ZERO;

    @Column(name = "max_stock_level", precision = 18, scale = 4)
    private BigDecimal maxStockLevel;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_service", nullable = false)
    @Builder.Default
    private boolean service = false;

    @Column(name = "is_sellable", nullable = false)
    @Builder.Default
    private boolean sellable = true;

    @Column(name = "is_purchasable", nullable = false)
    @Builder.Default
    private boolean purchasable = true;

    @Column(name = "has_variants", nullable = false)
    @Builder.Default
    private boolean hasVariants = false;

    @Column(name = "track_batch", nullable = false)
    @Builder.Default
    private boolean trackBatch = false;

    @Column(name = "track_serial", nullable = false)
    @Builder.Default
    private boolean trackSerial = false;

    @Column(precision = 10, scale = 3)
    private BigDecimal weight;

    @Column(name = "weight_unit", length = 20)
    private String weightUnit;

    @Column(precision = 10, scale = 2)
    private BigDecimal length;

    @Column(precision = 10, scale = 2)
    private BigDecimal width;

    @Column(precision = 10, scale = 2)
    private BigDecimal height;

    @Column(name = "dimension_unit", length = 20)
    private String dimensionUnit;

    @Column(name = "primary_image_url", length = 500)
    private String primaryImageUrl;

    @Column(length = 200)
    private String manufacturer;

    @Column(name = "manufacturer_part_number", length = 100)
    private String manufacturerPartNumber;

    @Column(name = "tax_code", length = 100)
    private String taxCode;

    @Column(length = 2000)
    private String notes;

    @Column(length = 500)
    private String tags;

    @Column(length = 1000)
    private String link;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductAttribute> attributes = new ArrayList<>();

    public void addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
    }

    public void removeVariant(ProductVariant variant) {
        variants.remove(variant);
        variant.setProduct(null);
    }

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }

    public void addAttribute(ProductAttribute attribute) {
        attributes.add(attribute);
        attribute.setProduct(this);
    }

    public void removeAttribute(ProductAttribute attribute) {
        attributes.remove(attribute);
        attribute.setProduct(null);
    }

    public BigDecimal getMargin() {
        if (costPrice == null || costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return sellingPrice.subtract(costPrice)
                .divide(costPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal getMarkup() {
        if (costPrice == null || costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return sellingPrice.subtract(costPrice)
                .divide(sellingPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
