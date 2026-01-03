package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Currency entity representing supported currencies.
 * Includes both system-wide currencies (tenantId = null) and tenant-specific ones.
 */
@Entity
@Table(name = "currencies", indexes = {
    @Index(name = "idx_currency_tenant", columnList = "tenant_id"),
    @Index(name = "idx_currency_code", columnList = "code"),
    @Index(name = "idx_currency_active", columnList = "active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Currency extends TenantAwareEntity {

    @Column(nullable = false, length = 3)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 10)
    private String symbol;

    @Column(name = "decimal_places", nullable = false)
    @Builder.Default
    private Integer decimalPlaces = 2;

    @Column(name = "is_base_currency", nullable = false)
    @Builder.Default
    private boolean baseCurrency = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "format_pattern", length = 50)
    @Builder.Default
    private String formatPattern = "#,##0.00";

    @Column(name = "symbol_position", length = 10)
    @Builder.Default
    private String symbolPosition = "BEFORE";

    @Column(name = "thousands_separator", length = 1)
    @Builder.Default
    private String thousandsSeparator = ",";

    @Column(name = "decimal_separator", length = 1)
    @Builder.Default
    private String decimalSeparator = ".";

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(length = 500)
    private String notes;
}
