package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private String sku;
    private String barcode;
    private String name;
    private String description;
    private String shortDescription;

    private Long categoryId;
    private String categoryName;

    private Long brandId;
    private String brandName;

    private Long baseUomId;
    private String baseUomCode;
    private String baseUomName;

    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BigDecimal minSellingPrice;
    private BigDecimal wholesalePrice;

    private boolean trackInventory;
    private boolean allowNegativeStock;
    private BigDecimal minStockLevel;
    private BigDecimal reorderPoint;
    private BigDecimal reorderQuantity;
    private BigDecimal maxStockLevel;

    private boolean active;
    private boolean service;
    private boolean sellable;
    private boolean purchasable;
    private boolean hasVariants;
    private boolean trackBatch;
    private boolean trackSerial;

    private BigDecimal weight;
    private String weightUnit;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private String dimensionUnit;

    private String primaryImageUrl;
    private String manufacturer;
    private String manufacturerPartNumber;
    private String taxCode;
    private String notes;
    private String tags;

    private BigDecimal margin;
    private BigDecimal markup;

    private BigDecimal stockQuantity;

    private List<ProductVariantDto> variants;
    private List<ProductImageDto> images;
    private List<ProductAttributeDto> attributes;
}
