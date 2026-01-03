package com.hisobnoma.platform.finance.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCreditNoteRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    private Long originalInvoiceId;

    @NotNull(message = "Credit note date is required")
    private LocalDate creditNoteDate;

    @Size(max = 50, message = "Reason code cannot exceed 50 characters")
    private String reasonCode;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    @NotNull(message = "Credit amount is required")
    @DecimalMin(value = "0.01", message = "Credit amount must be positive")
    private BigDecimal creditAmount;

    private BigDecimal subtotal;

    @DecimalMin(value = "0.0", message = "Tax amount must be non-negative")
    private BigDecimal taxAmount;

    @Size(max = 3, message = "Currency code cannot exceed 3 characters")
    @Builder.Default
    private String currency = "UZS";

    @DecimalMin(value = "0.0001", message = "Exchange rate must be positive")
    private BigDecimal exchangeRate;

    private Long arAccountId;

    private Long revenueAccountId;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}
