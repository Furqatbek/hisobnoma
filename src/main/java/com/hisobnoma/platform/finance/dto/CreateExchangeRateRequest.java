package com.hisobnoma.platform.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class CreateExchangeRateRequest {

    @NotBlank(message = "From currency is required")
    @Size(min = 3, max = 3, message = "From currency code must be exactly 3 characters")
    private String fromCurrency;

    @NotBlank(message = "To currency is required")
    @Size(min = 3, max = 3, message = "To currency code must be exactly 3 characters")
    private String toCurrency;

    @NotNull(message = "Rate is required")
    @Positive(message = "Rate must be positive")
    private BigDecimal rate;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    @Size(max = 50, message = "Source must not exceed 50 characters")
    private String source;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
