package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Exchange Rate entity for currency conversion.
 * Stores historical exchange rates for multi-currency transactions.
 */
@Entity
@Table(name = "exchange_rates", indexes = {
    @Index(name = "idx_exchange_rate_tenant", columnList = "tenant_id"),
    @Index(name = "idx_exchange_rate_from", columnList = "from_currency"),
    @Index(name = "idx_exchange_rate_to", columnList = "to_currency"),
    @Index(name = "idx_exchange_rate_date", columnList = "effective_date"),
    @Index(name = "idx_exchange_rate_lookup", columnList = "from_currency, to_currency, effective_date")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ExchangeRate extends TenantAwareEntity {

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal rate;

    @Column(name = "inverse_rate", precision = 18, scale = 8)
    private BigDecimal inverseRate;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(length = 50)
    private String source;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 500)
    private String notes;

    /**
     * Converts an amount from the source currency to the target currency.
     */
    public BigDecimal convert(BigDecimal amount) {
        if (amount == null || rate == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(rate);
    }

    /**
     * Converts an amount from the target currency back to the source currency.
     */
    public BigDecimal convertBack(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (inverseRate != null) {
            return amount.multiply(inverseRate);
        }
        if (rate != null && rate.compareTo(BigDecimal.ZERO) != 0) {
            return amount.divide(rate, 8, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Checks if this rate is effective for a given date.
     */
    public boolean isEffectiveFor(LocalDate date) {
        if (!active) {
            return false;
        }
        if (date.isBefore(effectiveDate)) {
            return false;
        }
        if (expiryDate != null && date.isAfter(expiryDate)) {
            return false;
        }
        return true;
    }

    /**
     * Calculates and sets the inverse rate.
     */
    public void calculateInverseRate() {
        if (rate != null && rate.compareTo(BigDecimal.ZERO) != 0) {
            this.inverseRate = BigDecimal.ONE.divide(rate, 8, java.math.RoundingMode.HALF_UP);
        }
    }
}
