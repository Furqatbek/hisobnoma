package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for importing products from CSV/Excel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportDto {

    private String sku;
    private String barcode;
    private String name;
    private String description;
    private String shortDescription;
    private String categoryName;
    private String brandName;
    private String baseUomCode;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BigDecimal minSellingPrice;
    private BigDecimal wholesalePrice;
    private Boolean trackInventory;
    private Boolean allowNegativeStock;
    private BigDecimal minStockLevel;
    private BigDecimal reorderPoint;
    private BigDecimal reorderQuantity;
    private BigDecimal maxStockLevel;
    private Boolean active;
    private Boolean service;
    private Boolean sellable;
    private Boolean purchasable;
    private Boolean trackBatch;
    private Boolean trackSerial;
    private BigDecimal weight;
    private String weightUnit;
    private String manufacturer;
    private String manufacturerPartNumber;
    private String taxCode;
    private String tags;

    // Row number for error reporting
    private int rowNumber;

    // Validation errors
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }
}
