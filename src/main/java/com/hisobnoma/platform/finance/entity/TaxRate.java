package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Tax Rate entity representing the rate for a specific tax code.
 * Supports effective date ranges for historical rate tracking.
 */
@Entity
@Table(name = "tax_rates", indexes = {
    @Index(name = "idx_tax_rate_tenant", columnList = "tenant_id"),
    @Index(name = "idx_tax_rate_code", columnList = "tax_code_id"),
    @Index(name = "idx_tax_rate_effective", columnList = "effective_from"),
    @Index(name = "idx_tax_rate_active", columnList = "active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TaxRate extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_code_id", nullable = false)
    private TaxCode taxCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal rate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "min_amount", precision = 18, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 18, scale = 4)
    private BigDecimal maxAmount;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 500)
    private String notes;

    /**
     * Checks if this rate is effective for the given date.
     */
    public boolean isEffectiveFor(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        boolean afterStart = !date.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || !date.isAfter(effectiveTo);
        return afterStart && beforeEnd && active;
    }

    /**
     * Calculates tax for the given amount.
     * For inclusive tax, extracts the tax from the amount.
     * For exclusive tax, calculates tax on top of the amount.
     */
    public BigDecimal calculateTax(BigDecimal amount, boolean inclusive) {
        if (amount == null || rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Check amount bounds if defined
        if (minAmount != null && amount.compareTo(minAmount) < 0) {
            return BigDecimal.ZERO;
        }
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            amount = maxAmount;
        }

        BigDecimal rateDecimal = rate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        if (inclusive) {
            // Tax is included in the amount: tax = amount * rate / (1 + rate)
            BigDecimal divisor = BigDecimal.ONE.add(rateDecimal);
            return amount.multiply(rateDecimal).divide(divisor, 4, RoundingMode.HALF_UP);
        } else {
            // Tax is on top of the amount: tax = amount * rate
            return amount.multiply(rateDecimal).setScale(4, RoundingMode.HALF_UP);
        }
    }

    /**
     * Calculates the base amount (excluding tax) from a tax-inclusive amount.
     */
    public BigDecimal calculateBaseAmount(BigDecimal inclusiveAmount) {
        if (inclusiveAmount == null || rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
            return inclusiveAmount;
        }

        BigDecimal rateDecimal = rate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.add(rateDecimal);
        return inclusiveAmount.divide(divisor, 4, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the total amount (including tax) from a base amount.
     */
    public BigDecimal calculateTotalAmount(BigDecimal baseAmount) {
        if (baseAmount == null || rate == null) {
            return baseAmount;
        }

        BigDecimal taxAmount = calculateTax(baseAmount, false);
        return baseAmount.add(taxAmount);
    }
}
