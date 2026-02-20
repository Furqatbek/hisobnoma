package com.hisobnoma.platform.hr.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "positions", indexes = {
    @Index(name = "idx_positions_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Position extends TenantAwareEntity {

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(precision = 15, scale = 2)
    private BigDecimal baseSalary;

    @Column(nullable = false)
    private boolean active = true;
}
