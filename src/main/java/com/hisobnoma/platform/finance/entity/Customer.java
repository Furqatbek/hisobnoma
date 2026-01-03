package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer entity for sales and accounts receivable.
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customers_tenant", columnList = "tenant_id"),
    @Index(name = "idx_customers_code", columnList = "code"),
    @Index(name = "idx_customers_email", columnList = "email"),
    @Index(name = "idx_customers_active", columnList = "active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Customer extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "alt_phone", length = 50)
    private String altPhone;

    @Column(name = "mobile_phone", length = 50)
    private String mobilePhone;

    @Column(length = 500)
    private String address;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * Payment terms for this customer (e.g., "Net 30", "Due on Receipt").
     */
    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    /**
     * Maximum credit limit for this customer.
     */
    @Column(name = "credit_limit", precision = 18, scale = 4)
    private BigDecimal creditLimit;

    /**
     * Current outstanding balance (invoices - payments).
     */
    @Column(name = "current_balance", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    /**
     * Total amount invoiced to this customer (lifetime).
     */
    @Column(name = "total_invoiced", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalInvoiced = BigDecimal.ZERO;

    /**
     * Total amount received from this customer (lifetime).
     */
    @Column(name = "total_received", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalReceived = BigDecimal.ZERO;

    @Column(name = "default_currency", length = 3)
    @Builder.Default
    private String defaultCurrency = "UZS";

    /**
     * Price list assigned to this customer for special pricing.
     */
    @Column(name = "price_list_id")
    private Long priceListId;

    /**
     * Default AR account for this customer.
     */
    @Column(name = "ar_account_id")
    private Long arAccountId;

    /**
     * Default sales revenue account for this customer.
     */
    @Column(name = "revenue_account_id")
    private Long revenueAccountId;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(length = 255)
    private String website;

    @Column(length = 1000)
    private String notes;

    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Whether this customer is on credit hold.
     */
    @Column(name = "credit_hold")
    @Builder.Default
    private boolean creditHold = false;

    /**
     * Customer type/category for classification.
     */
    @Column(name = "customer_type", length = 50)
    private String customerType;

    /**
     * Sales representative assigned to this customer.
     */
    @Column(name = "sales_rep_id")
    private Long salesRepId;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CustomerContact> contacts = new ArrayList<>();

    public void addContact(CustomerContact contact) {
        contacts.add(contact);
        contact.setCustomer(this);
    }

    public void removeContact(CustomerContact contact) {
        contacts.remove(contact);
        contact.setCustomer(null);
    }

    /**
     * Calculate available credit for this customer.
     */
    public BigDecimal getAvailableCredit() {
        if (creditLimit == null) {
            return null; // No limit set
        }
        BigDecimal balance = currentBalance != null ? currentBalance : BigDecimal.ZERO;
        BigDecimal available = creditLimit.subtract(balance);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }

    /**
     * Check if customer is over their credit limit.
     */
    public boolean isOverCreditLimit() {
        if (creditLimit == null) {
            return false; // No limit set
        }
        BigDecimal balance = currentBalance != null ? currentBalance : BigDecimal.ZERO;
        return balance.compareTo(creditLimit) > 0;
    }
}
