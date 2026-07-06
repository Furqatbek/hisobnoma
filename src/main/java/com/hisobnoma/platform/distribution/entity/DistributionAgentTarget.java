package com.hisobnoma.platform.distribution.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A performance target for a distribution agent over a period. Actuals are computed
 * live from orders/visits — only the goals are stored here.
 */
@Entity
@Table(name = "distribution_agent_targets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agent_targets_period",
                columnNames = {"tenant_id", "agent_id", "period_start", "period_end"}),
        indexes = {
                @Index(name = "idx_agent_targets_tenant", columnList = "tenant_id"),
                @Index(name = "idx_agent_targets_agent", columnList = "agent_id"),
                @Index(name = "idx_agent_targets_period", columnList = "tenant_id, period_start, period_end")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DistributionAgentTarget extends TenantAwareEntity {

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 10)
    @Builder.Default
    private TargetPeriodType periodType = TargetPeriodType.MONTHLY;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "target_revenue", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal targetRevenue = BigDecimal.ZERO;

    @Column(name = "target_orders")
    @Builder.Default
    private Integer targetOrders = 0;

    @Column(name = "target_visits")
    @Builder.Default
    private Integer targetVisits = 0;

    @Column(name = "target_new_customers")
    @Builder.Default
    private Integer targetNewCustomers = 0;

    @Column(name = "target_collection", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal targetCollection = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;
}
