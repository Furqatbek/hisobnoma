package com.hisobnoma.platform.mobile.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Mobile admin: record an expense. */
@Data
public class MobileExpenseRequest {

    /** Defaults to today when omitted. */
    private LocalDate createDate;

    /** Defaults to "Boshqa" when omitted. */
    private String category;

    @NotNull(message = "totalAmount is required")
    @Positive(message = "totalAmount must be positive")
    private BigDecimal totalAmount;

    private String currency;
    private String notes;
}
