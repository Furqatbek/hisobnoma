package com.hisobnoma.platform.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCurrencyRequest {

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    private String code;

    @NotBlank(message = "Currency name is required")
    @Size(max = 100, message = "Currency name must not exceed 100 characters")
    private String name;

    @Size(max = 10, message = "Symbol must not exceed 10 characters")
    private String symbol;

    private Integer decimalPlaces = 2;

    private boolean baseCurrency;

    @Size(max = 50, message = "Format pattern must not exceed 50 characters")
    private String formatPattern;

    @Size(max = 10, message = "Symbol position must not exceed 10 characters")
    private String symbolPosition;

    @Size(max = 1, message = "Thousands separator must be 1 character")
    private String thousandsSeparator;

    @Size(max = 1, message = "Decimal separator must be 1 character")
    private String decimalSeparator;

    private Integer sortOrder;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
