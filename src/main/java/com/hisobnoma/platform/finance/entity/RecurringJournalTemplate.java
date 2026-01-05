package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Recurring journal entry template.
 * Used to schedule and auto-generate journal entries on a recurring basis.
 */
@Entity
@Table(name = "recurring_journal_templates", indexes = {
    @Index(name = "idx_recurring_templates_tenant", columnList = "tenant_id"),
    @Index(name = "idx_recurring_templates_next_run", columnList = "next_run_date"),
    @Index(name = "idx_recurring_templates_active", columnList = "is_active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringJournalTemplate extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    /**
     * Day of month for MONTHLY (1-31), or day of week for WEEKLY (1-7)
     */
    @Column(name = "frequency_day")
    private Integer frequencyDay;

    /**
     * Date when the template becomes effective
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Date when the template expires (null = no expiry)
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Next scheduled run date
     */
    @Column(name = "next_run_date")
    private LocalDate nextRunDate;

    /**
     * Last time a journal was generated from this template
     */
    @Column(name = "last_run_date")
    private LocalDate lastRunDate;

    /**
     * Number of times this template has been executed
     */
    @Column(name = "run_count")
    @Builder.Default
    private Integer runCount = 0;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    /**
     * Auto-post the generated journal entries
     */
    @Column(name = "auto_post")
    @Builder.Default
    private boolean autoPost = false;

    /**
     * Template lines containing debit/credit entries
     */
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecurringJournalLine> lines = new ArrayList<>();

    public void addLine(RecurringJournalLine line) {
        lines.add(line);
        line.setTemplate(this);
    }

    public void removeLine(RecurringJournalLine line) {
        lines.remove(line);
        line.setTemplate(null);
    }

    /**
     * Calculate the next run date based on frequency
     */
    public LocalDate calculateNextRunDate() {
        LocalDate baseDate = lastRunDate != null ? lastRunDate : startDate;

        return switch (frequency) {
            case DAILY -> baseDate.plusDays(1);
            case WEEKLY -> baseDate.plusWeeks(1);
            case BIWEEKLY -> baseDate.plusWeeks(2);
            case MONTHLY -> baseDate.plusMonths(1);
            case QUARTERLY -> baseDate.plusMonths(3);
            case SEMIANNUALLY -> baseDate.plusMonths(6);
            case ANNUALLY -> baseDate.plusYears(1);
        };
    }

    public void incrementRunCount() {
        this.runCount = (this.runCount == null ? 0 : this.runCount) + 1;
    }

    public enum RecurrenceFrequency {
        DAILY,
        WEEKLY,
        BIWEEKLY,
        MONTHLY,
        QUARTERLY,
        SEMIANNUALLY,
        ANNUALLY
    }
}
