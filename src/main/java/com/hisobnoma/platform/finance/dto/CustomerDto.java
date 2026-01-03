package com.hisobnoma.platform.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private Long id;
    private Long tenantId;
    private String code;
    private String name;
    private String legalName;
    private String taxId;
    private String email;
    private String phone;
    private String altPhone;
    private String mobilePhone;
    private String address;
    private String shippingAddress;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String paymentTerms;
    private Integer paymentTermsDays;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private BigDecimal totalInvoiced;
    private BigDecimal totalReceived;
    private String defaultCurrency;
    private Long priceListId;
    private Long arAccountId;
    private Long revenueAccountId;
    private BigDecimal discountPercent;
    private String website;
    private String notes;
    private boolean active;
    private boolean creditHold;
    private String customerType;
    private Long salesRepId;
    private BigDecimal availableCredit;
    private boolean overCreditLimit;
    private Instant createdAt;
    private Instant updatedAt;
}
