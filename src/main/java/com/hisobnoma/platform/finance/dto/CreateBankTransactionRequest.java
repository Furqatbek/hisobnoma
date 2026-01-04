package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.BankTransactionType;
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
public class CreateBankTransactionRequest {

    @NotNull(message = "Bank account ID is required")
    private Long bankAccountId;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    private LocalDate valueDate;

    @NotNull(message = "Transaction type is required")
    private BankTransactionType transactionType;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private boolean isDebit;

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currency = "UZS";

    private BigDecimal exchangeRate;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;

    @Size(max = 50, message = "Check number must not exceed 50 characters")
    private String checkNumber;

    @Size(max = 200, message = "Payee/Payer must not exceed 200 characters")
    private String payeePayer;

    private String referenceType;

    private Long referenceId;

    private Long counterAccountId;

    private Long transferToAccountId;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
