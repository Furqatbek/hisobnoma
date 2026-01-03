package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateVendorRequest {

    @NotBlank(message = "Vendor code is required")
    @Size(max = 50, message = "Vendor code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Vendor name is required")
    @Size(max = 200, message = "Vendor name must not exceed 200 characters")
    private String name;

    @Size(max = 100, message = "Contact person must not exceed 100 characters")
    private String contactPerson;

    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    @Size(max = 50, message = "Alt phone must not exceed 50 characters")
    private String altPhone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Size(max = 50, message = "Tax ID must not exceed 50 characters")
    private String taxId;

    @Size(max = 100, message = "Payment terms must not exceed 100 characters")
    private String paymentTerms;

    private Integer paymentTermsDays;
    private BigDecimal creditLimit;
    private String defaultCurrency;
    private String bankName;
    private String bankAccount;
    private String bankRouting;
    private String website;
    private String notes;
    private boolean active = true;
    private boolean preferred;
    private Integer leadTimeDays;
    private BigDecimal minOrderAmount;
}
