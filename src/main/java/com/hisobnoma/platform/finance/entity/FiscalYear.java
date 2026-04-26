package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fiscal Year entity representing an accounting year.
 */
@Entity
@Table(name = "fiscal_years", indexes = {
    @Index(name = "idx_fiscal_year_tenant", columnList = "tenant_id"),
    @Index(name = "idx_fiscal_year_year", columnList = "year"),
    @Index(name = "idx_fiscal_year_current", columnList = "is_current")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FiscalYear extends TenantAwareEntity {

    @Column(name = "`year`", nullable = false)
    private Integer year;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PeriodStatus status = PeriodStatus.OPEN;

    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private boolean current = false;

    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private boolean closed = false;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "retained_earnings_account_id")
    private Long retainedEarningsAccountId;

    @Column(name = "year_end_closing_entry_id")
    private Long yearEndClosingEntryId;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "fiscalYear", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("periodNumber ASC")
    @Builder.Default
    private List<FiscalPeriod> periods = new ArrayList<>();

    /**
     * Adds a fiscal period to this year.
     */
    public void addPeriod(FiscalPeriod period) {
        periods.add(period);
        period.setFiscalYear(this);
    }

    /**
     * Removes a fiscal period from this year.
     */
    public void removePeriod(FiscalPeriod period) {
        periods.remove(period);
        period.setFiscalYear(null);
    }

    /**
     * Checks if a date falls within this fiscal year.
     */
    public boolean containsDate(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Gets the current open period for this fiscal year.
     */
    public FiscalPeriod getCurrentOpenPeriod() {
        return periods.stream()
                .filter(p -> p.getStatus() == PeriodStatus.OPEN)
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if all periods are closed.
     */
    public boolean areAllPeriodsClosed() {
        return periods.stream()
                .allMatch(p -> p.getStatus() == PeriodStatus.CLOSED || p.getStatus() == PeriodStatus.LOCKED);
    }
}
