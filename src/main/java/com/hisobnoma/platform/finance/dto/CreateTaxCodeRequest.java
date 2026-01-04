package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.TaxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaxCodeRequest {

    @NotBlank(message = "Tax code is required")
    @Size(max = 50, message = "Tax code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Tax name is required")
    @Size(max = 200, message = "Tax name must not exceed 200 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Tax type is required")
    private TaxType taxType;

    private boolean inclusive;

    private boolean compound;

    private boolean appliesToSales = true;

    private boolean appliesToPurchases = true;

    private Long salesAccountId;

    private Long purchaseAccountId;

    @Size(max = 200, message = "Tax authority must not exceed 200 characters")
    private String taxAuthority;

    @Size(max = 100, message = "Tax registration number must not exceed 100 characters")
    private String taxRegistrationNumber;

    @Size(max = 50, message = "Report code must not exceed 50 characters")
    private String reportCode;

    private Integer sortOrder;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
