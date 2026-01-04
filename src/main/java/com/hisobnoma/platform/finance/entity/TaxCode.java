package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Tax Code entity representing different tax categories.
 * Each tax code can have multiple rates for different date ranges.
 */
@Entity
@Table(name = "tax_codes", indexes = {
    @Index(name = "idx_tax_code_tenant", columnList = "tenant_id"),
    @Index(name = "idx_tax_code_code", columnList = "code"),
    @Index(name = "idx_tax_code_type", columnList = "tax_type"),
    @Index(name = "idx_tax_code_active", columnList = "active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TaxCode extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 20)
    private TaxType taxType;

    @Column(name = "is_inclusive", nullable = false)
    @Builder.Default
    private boolean inclusive = false;

    @Column(name = "is_compound", nullable = false)
    @Builder.Default
    private boolean compound = false;

    @Column(name = "applies_to_sales", nullable = false)
    @Builder.Default
    private boolean appliesToSales = true;

    @Column(name = "applies_to_purchases", nullable = false)
    @Builder.Default
    private boolean appliesToPurchases = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_account_id")
    private Account salesAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_account_id")
    private Account purchaseAccount;

    @Column(name = "tax_authority", length = 200)
    private String taxAuthority;

    @Column(name = "tax_registration_number", length = 100)
    private String taxRegistrationNumber;

    @Column(name = "report_code", length = 50)
    private String reportCode;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system_code", nullable = false)
    @Builder.Default
    private boolean systemCode = false;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "taxCode", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("effectiveFrom DESC")
    @Builder.Default
    private List<TaxRate> rates = new ArrayList<>();

    /**
     * Adds a tax rate to this tax code.
     */
    public void addRate(TaxRate rate) {
        rates.add(rate);
        rate.setTaxCode(this);
    }

    /**
     * Removes a tax rate from this tax code.
     */
    public void removeRate(TaxRate rate) {
        rates.remove(rate);
        rate.setTaxCode(null);
    }
}
