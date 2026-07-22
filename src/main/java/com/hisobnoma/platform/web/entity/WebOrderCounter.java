package com.hisobnoma.platform.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-tenant allocator row for online-shop order numbers ("WO-%06d"). Read under a pessimistic
 * write lock so concurrent checkouts on the same tenant serialize on number allocation instead of
 * colliding on the unique order-number index. Deliberately a bare entity (tenant id IS the key;
 * no audit columns) — it is infrastructure, not business data.
 */
@Entity
@Table(name = "web_order_counters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebOrderCounter {

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "next_number", nullable = false)
    private long nextNumber;
}
