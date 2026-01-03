package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Vendor contact entity for storing multiple contacts per vendor.
 */
@Entity
@Table(name = "vendor_contacts", indexes = {
    @Index(name = "idx_vendor_contacts_vendor", columnList = "vendor_id"),
    @Index(name = "idx_vendor_contacts_primary", columnList = "is_primary")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class VendorContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

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

    @Column(name = "is_ordering_contact")
    @Builder.Default
    private boolean orderingContact = false;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
