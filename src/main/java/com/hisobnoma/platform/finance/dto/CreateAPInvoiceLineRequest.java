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
public class CreateAPInvoiceLineRequest {

    private Long productId;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Long expenseAccountId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be positive")
    private BigDecimal quantity;

    @Size(max = 20, message = "Unit of measure cannot exceed 20 characters")
    private String unitOfMeasure;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.0", message = "Discount percent must be non-negative")
    @DecimalMax(value = "100.0", message = "Discount percent cannot exceed 100")
    private BigDecimal discountPercent;

    @DecimalMin(value = "0.0", message = "Discount amount must be non-negative")
    private BigDecimal discountAmount;

    @Size(max = 50, message = "Tax code cannot exceed 50 characters")
    private String taxCode;

    @DecimalMin(value = "0.0", message = "Tax rate must be non-negative")
    private BigDecimal taxRate;

    private Long purchaseOrderLineId;

    private Long receivingLineId;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
