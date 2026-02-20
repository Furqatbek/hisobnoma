package com.hisobnoma.platform.hr.entity;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees", indexes = {
    @Index(name = "idx_employees_tenant", columnList = "tenant_id"),
    @Index(name = "idx_employees_status", columnList = "tenant_id, status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Employee extends TenantAwareEntity {

    @Column(name = "employee_code", nullable = false, length = 20)
    private String employeeCode;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(length = 50)
    private String middleName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private LocalDate hireDate;

    private LocalDate terminationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(precision = 15, scale = 2)
    private BigDecimal currentSalary;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(lastName).append(" ").append(firstName);
        if (middleName != null && !middleName.isEmpty()) {
            sb.append(" ").append(middleName);
        }
        return sb.toString();
    }

    public enum EmployeeStatus {
        ACTIVE, ON_LEAVE, TERMINATED
    }
}
