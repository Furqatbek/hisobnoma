package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Fiscal Period entity representing a monthly accounting period within a fiscal year.
 */
@Entity
@Table(name = "fiscal_periods", indexes = {
    @Index(name = "idx_fiscal_period_tenant", columnList = "tenant_id"),
    @Index(name = "idx_fiscal_period_year", columnList = "fiscal_year_id"),
    @Index(name = "idx_fiscal_period_number", columnList = "period_number"),
    @Index(name = "idx_fiscal_period_status", columnList = "status"),
    @Index(name = "idx_fiscal_period_dates", columnList = "start_date, end_date")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FiscalPeriod extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

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

    @Column(name = "is_adjustment_period", nullable = false)
    @Builder.Default
    private boolean adjustmentPeriod = false;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "reopened_at")
    private Instant reopenedAt;

    @Column(name = "reopened_by")
    private Long reopenedBy;

    @Column(name = "reopen_reason", length = 500)
    private String reopenReason;

    @Column(length = 500)
    private String notes;

    /**
     * Checks if a date falls within this period.
     */
    public boolean containsDate(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Checks if posting is allowed in this period.
     */
    public boolean allowsPosting() {
        return status == PeriodStatus.OPEN;
    }

    /**
     * Gets a display name for this period (e.g., "January 2024").
     */
    public String getDisplayName() {
        return name + " " + fiscalYear.getYear();
    }
}
