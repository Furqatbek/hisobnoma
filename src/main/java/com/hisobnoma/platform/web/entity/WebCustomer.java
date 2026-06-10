package com.hisobnoma.platform.web.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Online shop customer account, identified by a verified phone number.
 * Optionally linked by staff to an AR {@code Customer} so order-to-invoice
 * conversion reuses the existing debtor record.
 */
@Entity
@Table(name = "web_customers",
        uniqueConstraints = @UniqueConstraint(name = "uk_web_customers_phone", columnNames = {"tenant_id", "phone"}),
        indexes = @Index(name = "idx_web_customers_tenant", columnList = "tenant_id"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WebCustomer extends TenantAwareEntity {

    /**
     * Digits-only normalized phone (e.g. 998901234567).
     */
    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 200)
    private String name;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Optional link to the AR customer, set by staff.
     */
    @Column(name = "customer_id")
    private Long customerId;

    /**
     * Customer withdrew SMS marketing consent — excluded from all campaign segments.
     */
    @Column(name = "sms_opt_out", nullable = false)
    @Builder.Default
    private boolean smsOptOut = false;
}
