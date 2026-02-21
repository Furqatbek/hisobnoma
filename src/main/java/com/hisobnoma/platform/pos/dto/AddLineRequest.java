package com.hisobnoma.platform.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class AddLineRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long variantId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    /**
     * Optional override of unit price
     */
    private BigDecimal unitPrice;

    private BigDecimal discountAmount;

    private BigDecimal discountPercent;

    @Size(max = 200, message = "Discount reason must not exceed 200 characters")
    private String discountReason;

    @Size(max = 100, message = "Serial number must not exceed 100 characters")
    private String serialNumber;

    @Size(max = 100, message = "Batch number must not exceed 100 characters")
    private String batchNumber;

    private Long locationId;

    /**
     * Optional: alternate UOM for selling. If set, quantity is in this UOM
     * and will be converted to base UOM for stock deduction.
     */
    private Long productUomId;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
