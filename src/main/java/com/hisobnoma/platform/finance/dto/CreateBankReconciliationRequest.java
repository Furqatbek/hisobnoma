package com.hisobnoma.platform.finance.dto;

import jakarta.validation.constraints.NotNull;
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
public class CreateBankReconciliationRequest {

    @NotNull(message = "Bank account ID is required")
    private Long bankAccountId;

    @NotNull(message = "Statement date is required")
    private LocalDate statementDate;

    @NotNull(message = "Statement start date is required")
    private LocalDate statementStartDate;

    @NotNull(message = "Statement end date is required")
    private LocalDate statementEndDate;

    @NotNull(message = "Opening balance is required")
    private BigDecimal openingBalance;

    @NotNull(message = "Statement ending balance is required")
    private BigDecimal statementEndingBalance;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
