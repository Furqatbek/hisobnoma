package com.hisobnoma.platform.hr.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary_advances", indexes = {
    @Index(name = "idx_salary_advances_tenant", columnList = "tenant_id"),
    @Index(name = "idx_salary_advances_employee", columnList = "employee_id"),
    @Index(name = "idx_salary_advances_period", columnList = "tenant_id, period_year, period_month")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class SalaryAdvance extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "advance_date", nullable = false)
    private LocalDate advanceDate;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AdvanceStatus status = AdvanceStatus.GIVEN;

    @Column(name = "salary_record_id")
    private Long salaryRecordId;

    @Column(name = "gl_journal_entry_id")
    private Long glJournalEntryId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum AdvanceStatus {
        GIVEN, DEDUCTED, CANCELLED
    }
}
