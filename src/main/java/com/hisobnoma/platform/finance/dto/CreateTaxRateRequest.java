package com.hisobnoma.platform.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class CreateTaxRateRequest {

    @NotNull(message = "Tax code ID is required")
    private Long taxCodeId;

    @NotBlank(message = "Rate name is required")
    @Size(max = 100, message = "Rate name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Rate is required")
    @PositiveOrZero(message = "Rate must be zero or positive")
    private BigDecimal rate;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @PositiveOrZero(message = "Minimum amount must be zero or positive")
    private BigDecimal minAmount;

    @PositiveOrZero(message = "Maximum amount must be zero or positive")
    private BigDecimal maxAmount;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
