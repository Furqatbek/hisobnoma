package com.hisobnoma.platform.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Updates the shop-specific overrides of a catalog item.
 * Null values clear the override so the product's own name/price is used.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCatalogItemRequest {

    @Size(max = 200, message = "Display name cannot exceed 200 characters")
    private String displayName;

    @DecimalMin(value = "0.0", message = "Price override must be non-negative")
    private BigDecimal priceOverride;
}
