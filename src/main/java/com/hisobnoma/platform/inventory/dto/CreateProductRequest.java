package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.constraints.*;
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
public class CreateProductRequest {

    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    @Size(max = 50, message = "Barcode must not exceed 50 characters")
    private String barcode;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;

    private Long categoryId;

    private Long brandId;

    @NotNull(message = "Base UOM is required")
    private Long baseUomId;

    @DecimalMin(value = "0", message = "Cost price must be non-negative")
    private BigDecimal costPrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0", inclusive = false, message = "Selling price must be positive")
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0", message = "Minimum selling price must be non-negative")
    private BigDecimal minSellingPrice;

    @DecimalMin(value = "0", message = "Wholesale price must be non-negative")
    private BigDecimal wholesalePrice;

    @Builder.Default
    private boolean trackInventory = true;

    @Builder.Default
    private boolean allowNegativeStock = false;

    @DecimalMin(value = "0", message = "Minimum stock level must be non-negative")
    private BigDecimal minStockLevel;

    @DecimalMin(value = "0", message = "Reorder point must be non-negative")
    private BigDecimal reorderPoint;

    @DecimalMin(value = "0", message = "Reorder quantity must be non-negative")
    private BigDecimal reorderQuantity;

    @DecimalMin(value = "0", message = "Maximum stock level must be non-negative")
    private BigDecimal maxStockLevel;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean service = false;

    @Builder.Default
    private boolean sellable = true;

    @Builder.Default
    private boolean purchasable = true;

    @Builder.Default
    private boolean trackBatch = false;

    @Builder.Default
    private boolean trackSerial = false;

    @DecimalMin(value = "0", message = "Weight must be non-negative")
    private BigDecimal weight;

    @Size(max = 20, message = "Weight unit must not exceed 20 characters")
    private String weightUnit;

    @DecimalMin(value = "0", message = "Length must be non-negative")
    private BigDecimal length;

    @DecimalMin(value = "0", message = "Width must be non-negative")
    private BigDecimal width;

    @DecimalMin(value = "0", message = "Height must be non-negative")
    private BigDecimal height;

    @Size(max = 20, message = "Dimension unit must not exceed 20 characters")
    private String dimensionUnit;

    @Size(max = 500, message = "Primary image URL must not exceed 500 characters")
    private String primaryImageUrl;

    @Size(max = 200, message = "Manufacturer must not exceed 200 characters")
    private String manufacturer;

    @Size(max = 100, message = "Manufacturer part number must not exceed 100 characters")
    private String manufacturerPartNumber;

    @Size(max = 100, message = "Tax code must not exceed 100 characters")
    private String taxCode;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    @Size(max = 500, message = "Tags must not exceed 500 characters")
    private String tags;

    @Size(max = 1000, message = "Link must not exceed 1000 characters")
    private String link;

    private List<CreateVariantRequest> variants;
    private List<CreateAttributeRequest> attributes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateVariantRequest {
        private String sku;
        private String barcode;
        @NotBlank(message = "Variant name is required")
        private String name;
        private String option1Name;
        private String option1Value;
        private String option2Name;
        private String option2Value;
        private String option3Name;
        private String option3Value;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private BigDecimal priceDifference;
        private BigDecimal weight;
        private String imageUrl;
        private Integer sortOrder;
        @Builder.Default
        private boolean active = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAttributeRequest {
        @NotBlank(message = "Attribute name is required")
        private String attributeName;
        private String attributeValue;
        private String attributeType;
        private String attributeGroup;
        private Integer sortOrder;
        @Builder.Default
        private boolean visible = true;
        @Builder.Default
        private boolean searchable = false;
        @Builder.Default
        private boolean filterable = false;
    }
}
