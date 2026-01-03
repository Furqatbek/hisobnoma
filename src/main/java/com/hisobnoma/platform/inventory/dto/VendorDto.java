package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorDto {

    private Long id;
    private String code;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String altPhone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String taxId;
    private String paymentTerms;
    private Integer paymentTermsDays;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private String defaultCurrency;
    private String bankName;
    private String bankAccount;
    private String bankRouting;
    private String website;
    private String notes;
    private boolean active;
    private boolean preferred;
    private Integer leadTimeDays;
    private BigDecimal minOrderAmount;
}
