package com.hisobnoma.platform.finance.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateARInvoiceLineRequest {

    private Long itemId;

    private Long productId;

    @Size(max = 50, message = "Product SKU cannot exceed 50 characters")
    private String productSku;

    @Size(max = 200, message = "Product name cannot exceed 200 characters")
    private String productName;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Long revenueAccountId;

    @Size(max = 20, message = "Revenue account code cannot exceed 20 characters")
    private String revenueAccountCode;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be positive")
    private BigDecimal quantity;

    @Size(max = 20, message = "Unit of measure cannot exceed 20 characters")
    private String unitOfMeasure;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.0", message = "Unit cost must be non-negative")
    private BigDecimal unitCost;

    @DecimalMin(value = "0.0", message = "Discount percent must be non-negative")
    @DecimalMax(value = "100.0", message = "Discount percent cannot exceed 100")
    private BigDecimal discountPercent;

    @Size(max = 50, message = "Tax code cannot exceed 50 characters")
    private String taxCode;

    @DecimalMin(value = "0.0", message = "Tax rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Tax rate cannot exceed 100")
    private BigDecimal taxRate;

    @DecimalMin(value = "0.0", message = "Tax amount must be non-negative")
    private BigDecimal taxAmount;

    private Long posLineId;

    private Long salesOrderLineId;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
