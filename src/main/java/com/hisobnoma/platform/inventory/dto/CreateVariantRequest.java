package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a product variant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVariantRequest {

    @Size(max = 50)
    private String sku;

    @Size(max = 50)
    private String barcode;

    @Size(max = 200)
    private String name;

    @Size(max = 50)
    private String option1Name;

    @NotBlank(message = "Option 1 value is required")
    @Size(max = 100)
    private String option1Value;

    @Size(max = 50)
    private String option2Name;

    @Size(max = 100)
    private String option2Value;

    @Size(max = 50)
    private String option3Name;

    @Size(max = 100)
    private String option3Value;

    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BigDecimal priceDifference;

    private BigDecimal weight;

    @Size(max = 500)
    private String imageUrl;

    private Integer sortOrder;

    @Builder.Default
    private boolean active = true;
}
