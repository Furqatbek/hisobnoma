package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Unit of Measure entity for product quantity management.
 * Supports base units and conversion between units.
 */
@Entity
@Table(name = "units_of_measure", indexes = {
    @Index(name = "idx_uom_tenant", columnList = "tenant_id"),
    @Index(name = "idx_uom_code", columnList = "code")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class UnitOfMeasure extends TenantAwareEntity {

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 10)
    private String symbol;

    @Column(length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_uom_id")
    private UnitOfMeasure baseUom;

    @Column(name = "conversion_factor", precision = 18, scale = 6)
    @Builder.Default
    private BigDecimal conversionFactor = BigDecimal.ONE;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_base_unit", nullable = false)
    @Builder.Default
    private boolean isBaseUnit = true;

    public BigDecimal convertToBase(BigDecimal quantity) {
        if (isBaseUnit || baseUom == null) {
            return quantity;
        }
        return quantity.multiply(conversionFactor);
    }

    public BigDecimal convertFromBase(BigDecimal baseQuantity) {
        if (isBaseUnit || baseUom == null) {
            return baseQuantity;
        }
        return baseQuantity.divide(conversionFactor, 6, java.math.RoundingMode.HALF_UP);
    }
}
