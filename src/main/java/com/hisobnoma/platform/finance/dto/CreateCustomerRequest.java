package com.hisobnoma.platform.finance.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {

    @Size(max = 50, message = "Customer code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Customer name is required")
    @Size(max = 200, message = "Customer name cannot exceed 200 characters")
    private String name;

    @Size(max = 200, message = "Legal name cannot exceed 200 characters")
    private String legalName;

    @Size(max = 50, message = "Tax ID cannot exceed 50 characters")
    private String taxId;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(max = 50, message = "Phone cannot exceed 50 characters")
    private String phone;

    @Size(max = 50, message = "Alt phone cannot exceed 50 characters")
    private String altPhone;

    @Size(max = 50, message = "Mobile phone cannot exceed 50 characters")
    private String mobilePhone;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    private String shippingAddress;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;

    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;

    @Size(max = 100, message = "Payment terms cannot exceed 100 characters")
    private String paymentTerms;

    private Integer paymentTermsDays;

    @DecimalMin(value = "0.0", message = "Credit limit must be non-negative")
    private BigDecimal creditLimit;

    @Size(max = 3, message = "Currency code cannot exceed 3 characters")
    @Builder.Default
    private String defaultCurrency = "UZS";

    private Long priceListId;

    private Long arAccountId;

    private Long revenueAccountId;

    @DecimalMin(value = "0.0", message = "Discount percent must be non-negative")
    @DecimalMax(value = "100.0", message = "Discount percent cannot exceed 100")
    private BigDecimal discountPercent;

    @Size(max = 255, message = "Website cannot exceed 255 characters")
    private String website;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @Size(max = 1000, message = "Internal notes cannot exceed 1000 characters")
    private String internalNotes;

    @Size(max = 50, message = "Customer type cannot exceed 50 characters")
    private String customerType;

    private Long salesRepId;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean creditHold = false;
}
