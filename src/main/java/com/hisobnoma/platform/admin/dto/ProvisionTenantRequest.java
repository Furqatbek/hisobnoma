package com.hisobnoma.platform.admin.dto;

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
public class ProvisionTenantRequest {

    @NotBlank @Size(max = 200)
    private String name;

    /** Unique short code, stored uppercase (e.g. "ACME"). */
    @NotBlank @Size(max = 50)
    private String code;

    @Size(max = 500)
    private String description;

    private String timezone;   // default Asia/Tashkent
    private String currency;   // default UZS

    @NotBlank @Size(max = 100)
    private String adminUsername;
    @NotBlank @Size(min = 8, max = 100)
    private String adminPassword;
    private String adminPhone;
    private String adminFirstName;
    private String adminLastName;

    /** Defaults to the current year. */
    private Integer fiscalYear;
}
