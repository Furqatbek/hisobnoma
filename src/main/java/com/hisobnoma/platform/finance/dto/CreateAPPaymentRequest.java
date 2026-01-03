package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.APPaymentMethod;
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
public class CreateAPPaymentRequest {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private APPaymentMethod paymentMethod;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be positive")
    private BigDecimal paymentAmount;

    @Size(max = 3, message = "Currency code cannot exceed 3 characters")
    @Builder.Default
    private String currency = "UZS";

    @DecimalMin(value = "0.0001", message = "Exchange rate must be positive")
    private BigDecimal exchangeRate;

    private Long bankAccountId;

    private Long cashAccountId;

    private Long apAccountId;

    @Size(max = 50, message = "Reference number cannot exceed 50 characters")
    private String referenceNumber;

    @Size(max = 50, message = "Check number cannot exceed 50 characters")
    private String checkNumber;

    private LocalDate checkDate;

    @Size(max = 200, message = "Memo cannot exceed 200 characters")
    private String memo;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @NotEmpty(message = "At least one payment allocation is required")
    @Valid
    private List<CreateAPPaymentAllocationRequest> allocations;
}
