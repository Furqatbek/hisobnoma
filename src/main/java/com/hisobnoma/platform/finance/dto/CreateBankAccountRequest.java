package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.BankAccountType;
import jakarta.validation.constraints.NotBlank;
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
public class CreateBankAccountRequest {

    @NotBlank(message = "Account code is required")
    @Size(max = 50, message = "Account code must not exceed 50 characters")
    private String accountCode;

    @NotBlank(message = "Account name is required")
    @Size(max = 200, message = "Account name must not exceed 200 characters")
    private String accountName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Account type is required")
    private BankAccountType accountType;

    @Size(max = 200, message = "Bank name must not exceed 200 characters")
    private String bankName;

    @Size(max = 200, message = "Bank branch must not exceed 200 characters")
    private String bankBranch;

    @Size(max = 50, message = "Bank code must not exceed 50 characters")
    private String bankCode;

    @Size(max = 50, message = "Account number must not exceed 50 characters")
    private String accountNumber;

    @Size(max = 50, message = "IBAN must not exceed 50 characters")
    private String iban;

    @Size(max = 20, message = "SWIFT code must not exceed 20 characters")
    private String swiftCode;

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currency = "UZS";

    private BigDecimal openingBalance;

    private LocalDate openingBalanceDate;

    private Long glAccountId;

    @Size(max = 100, message = "Contact name must not exceed 100 characters")
    private String contactName;

    @Size(max = 50, message = "Contact phone must not exceed 50 characters")
    private String contactPhone;

    @Size(max = 100, message = "Contact email must not exceed 100 characters")
    private String contactEmail;

    private boolean defaultAccount;

    private boolean allowPayments = true;

    private boolean allowReceipts = true;

    private Integer sortOrder;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
