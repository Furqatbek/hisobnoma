package com.hisobnoma.platform.finance.dto;

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
public class CreateJournalLineRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private BigDecimal debitAmount;
    private BigDecimal creditAmount;

    @Size(max = 50, message = "Cost center must not exceed 50 characters")
    private String costCenter;

    @Size(max = 50, message = "Project code must not exceed 50 characters")
    private String projectCode;

    @Size(max = 50, message = "Department must not exceed 50 characters")
    private String department;

    @Size(max = 50, message = "Reference type must not exceed 50 characters")
    private String referenceType;

    private Long referenceId;

    @Size(max = 50, message = "Tax code must not exceed 50 characters")
    private String taxCode;

    private BigDecimal taxAmount;
}
