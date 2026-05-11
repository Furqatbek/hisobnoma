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
public class CreateARInvoiceRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private Long posTransactionId;

    private Long salesOrderId;

    @DecimalMin(value = "0.0", message = "Discount amount must be non-negative")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.0", message = "Discount percent must be non-negative")
    @DecimalMax(value = "100.0", message = "Discount percent cannot exceed 100")
    private BigDecimal discountPercent;

    @DecimalMin(value = "0.0", message = "Shipping amount must be non-negative")
    private BigDecimal shippingAmount;

    @DecimalMin(value = "0.0", message = "Tax amount must be non-negative")
    private BigDecimal taxAmount;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", message = "Total amount must be non-negative")
    private BigDecimal totalAmount;

    @Size(max = 3, message = "Currency code cannot exceed 3 characters")
    @Builder.Default
    private String currency = "UZS";

    @DecimalMin(value = "0.0001", message = "Exchange rate must be positive")
    private BigDecimal exchangeRate;

    private Integer paymentTerms;

    private Long arAccountId;

    private Long revenueAccountId;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @Size(max = 1000, message = "Internal notes cannot exceed 1000 characters")
    private String internalNotes;

    @Size(max = 500, message = "Billing address cannot exceed 500 characters")
    private String billingAddress;

    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    private String shippingAddress;

    @NotEmpty(message = "At least one invoice line is required")
    @Valid
    private List<CreateARInvoiceLineRequest> lines;
}
