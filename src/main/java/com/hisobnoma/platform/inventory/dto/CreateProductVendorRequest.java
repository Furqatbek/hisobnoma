package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductVendorRequest {
    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    private String vendorSku;
    private String vendorProductName;
    private BigDecimal unitCost;
    private BigDecimal minOrderQuantity;
    private Integer leadTimeDays;
    private boolean preferred;
    private boolean active = true;
    private String notes;
}
