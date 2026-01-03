package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Customer contact entity for storing multiple contacts per customer.
 */
@Entity
@Table(name = "customer_contacts", indexes = {
    @Index(name = "idx_customer_contacts_customer", columnList = "customer_id"),
    @Index(name = "idx_customer_contacts_primary", columnList = "is_primary")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CustomerContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String title;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "mobile_phone", length = 50)
    private String mobilePhone;

    @Column(length = 50)
    private String fax;

    @Column(name = "is_primary")
    @Builder.Default
    private boolean primary = false;

    @Column(name = "is_billing_contact")
    @Builder.Default
    private boolean billingContact = false;

    @Column(name = "is_shipping_contact")
    @Builder.Default
    private boolean shippingContact = false;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
