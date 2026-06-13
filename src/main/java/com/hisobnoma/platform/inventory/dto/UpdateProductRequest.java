package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @Size(max = 50, message = "Barcode must not exceed 50 characters")
    private String barcode;

    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;

    private Long categoryId;

    private Long brandId;

    private Long baseUomId;

    @DecimalMin(value = "0", message = "Cost price must be non-negative")
    private BigDecimal costPrice;

    @DecimalMin(value = "0", inclusive = false, message = "Selling price must be positive")
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0", message = "Minimum selling price must be non-negative")
    private BigDecimal minSellingPrice;

    @DecimalMin(value = "0", message = "Wholesale price must be non-negative")
    private BigDecimal wholesalePrice;

    private Boolean trackInventory;
    private Boolean allowNegativeStock;

    @DecimalMin(value = "0", message = "Minimum stock level must be non-negative")
    private BigDecimal minStockLevel;

    @DecimalMin(value = "0", message = "Reorder point must be non-negative")
    private BigDecimal reorderPoint;

    @DecimalMin(value = "0", message = "Reorder quantity must be non-negative")
    private BigDecimal reorderQuantity;

    @DecimalMin(value = "0", message = "Maximum stock level must be non-negative")
    private BigDecimal maxStockLevel;

    private Boolean active;
    private Boolean service;
    private Boolean sellable;
    private Boolean purchasable;
    private Boolean trackBatch;
    private Boolean trackSerial;
    private Boolean fractional;
    private BigDecimal orderStep;

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
}
