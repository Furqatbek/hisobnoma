package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePurchaseOrderRequest {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotNull(message = "Location ID is required")
    private Long locationId;

    @NotNull(message = "Order date is required")
    private LocalDate orderDate;

    private LocalDate expectedDate;

    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal shippingAmount;

    @Size(max = 3, message = "Currency code must not exceed 3 characters")
    private String currency;

    @Size(max = 100, message = "Payment terms must not exceed 100 characters")
    private String paymentTerms;

    @Size(max = 100, message = "Shipping method must not exceed 100 characters")
    private String shippingMethod;

    @Size(max = 500, message = "Shipping address must not exceed 500 characters")
    private String shippingAddress;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @Size(max = 1000, message = "Internal notes must not exceed 1000 characters")
    private String internalNotes;

    @Size(max = 100, message = "Vendor reference must not exceed 100 characters")
    private String vendorReference;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<CreatePurchaseOrderLineRequest> lines;
}
