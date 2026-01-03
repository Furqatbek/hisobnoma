package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Vendor/Supplier entity for purchase orders and AP.
 */
@Entity
@Table(name = "vendors", indexes = {
    @Index(name = "idx_vendors_tenant", columnList = "tenant_id"),
    @Index(name = "idx_vendors_code", columnList = "code"),
    @Index(name = "idx_vendors_active", columnList = "active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Vendor extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "alt_phone", length = 50)
    private String altPhone;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    @Column(name = "credit_limit", precision = 18, scale = 4)
    private BigDecimal creditLimit;

    @Column(name = "current_balance", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "default_currency", length = 3)
    @Builder.Default
    private String defaultCurrency = "UZS";

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Column(name = "bank_routing", length = 50)
    private String bankRouting;

    @Column(length = 255)
    private String website;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_preferred")
    @Builder.Default
    private boolean preferred = false;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "min_order_amount", precision = 18, scale = 4)
    private BigDecimal minOrderAmount;
}
