package com.hisobnoma.platform.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
public class CreateAPInvoiceRequest {

    @Size(max = 100, message = "Vendor invoice number cannot exceed 100 characters")
    private String vendorInvoiceNumber;

    private Long vendorId;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private LocalDate receivedDate;

    private Long purchaseOrderId;

    private Long receivingOrderId;

    @DecimalMin(value = "0.0", message = "Discount amount must be positive")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.0", message = "Tax amount must be positive")
    private BigDecimal taxAmount;

    @DecimalMin(value = "0.0", message = "Shipping amount must be positive")
    private BigDecimal shippingAmount;

    @Size(max = 3, message = "Currency code cannot exceed 3 characters")
    @Builder.Default
    private String currency = "UZS";

    @DecimalMin(value = "0.0001", message = "Exchange rate must be positive")
    private BigDecimal exchangeRate;

    @Size(max = 100, message = "Payment terms cannot exceed 100 characters")
    private String paymentTerms;

    private Integer paymentTermsDays;

    private Long expenseAccountId;

    private Long apAccountId;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @NotEmpty(message = "At least one invoice line is required")
    @Valid
    private List<CreateAPInvoiceLineRequest> lines;
}
