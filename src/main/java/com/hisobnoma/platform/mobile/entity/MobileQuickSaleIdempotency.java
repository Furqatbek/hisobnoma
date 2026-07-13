package com.hisobnoma.platform.mobile.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Maps a client-generated {@code clientRequestId} (UUID) to the POS transaction created for it,
 * so a retried {@code /mobile/pos/quick-sale} (e.g. after a dropped response) returns the original
 * sale instead of creating a duplicate. Unique per tenant.
 */
@Entity
@Table(name = "mobile_quick_sale_idempotency",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mobile_quicksale_client_request",
                columnNames = {"tenant_id", "client_request_id"}),
        indexes = @Index(name = "idx_mobile_quicksale_tenant", columnList = "tenant_id"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class MobileQuickSaleIdempotency extends TenantAwareEntity {

    @Column(name = "client_request_id", nullable = false, length = 100)
    private String clientRequestId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;
}
