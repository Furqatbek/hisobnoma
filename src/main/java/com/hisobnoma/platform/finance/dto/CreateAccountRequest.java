package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Account code is required")
    @Size(max = 20, message = "Account code must not exceed 20 characters")
    private String code;

    @NotBlank(message = "Account name is required")
    @Size(max = 200, message = "Account name must not exceed 200 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    private Long parentAccountId;

    private boolean controlAccount;
    private boolean allowsDirectPosting = true;

    private BigDecimal openingBalance;

    @Size(max = 3, message = "Currency code must not exceed 3 characters")
    private String currency;

    @Size(max = 50, message = "Tax code must not exceed 50 characters")
    private String taxCode;

    @Size(max = 50, message = "Bank account number must not exceed 50 characters")
    private String bankAccountNumber;

    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    private Integer sortOrder;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
