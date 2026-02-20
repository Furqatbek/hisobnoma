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
@Table(name = "salary_records", indexes = {
    @Index(name = "idx_salary_records_tenant", columnList = "tenant_id"),
    @Index(name = "idx_salary_records_employee", columnList = "employee_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class SalaryRecord extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Integer periodYear;

    @Column(nullable = false)
    private Integer periodMonth;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal baseAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal bonusAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal deductionAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SalaryStatus status = SalaryStatus.PENDING;

    private LocalDate paidDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum SalaryStatus {
        PENDING, PAID, CANCELLED
    }
}
