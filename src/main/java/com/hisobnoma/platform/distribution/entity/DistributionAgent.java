package com.hisobnoma.platform.distribution.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A distribution agent — a field sales / route / van-sales representative who takes
 * wholesale orders, visits customers and (optionally) sells from a vehicle.
 *
 * <p>Optionally linked to a platform {@code user} account (for the agent's own login/app)
 * and/or an HR {@code employee} record. Territory coverage is expressed through the
 * {@link DistributionTerritory} child collection.
 */
@Entity
@Table(name = "distribution_agents", indexes = {
    @Index(name = "idx_dist_agents_tenant", columnList = "tenant_id"),
    @Index(name = "idx_dist_agents_code", columnList = "code"),
    @Index(name = "idx_dist_agents_user", columnList = "user_id"),
    @Index(name = "idx_dist_agents_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DistributionAgent extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    /** Platform user account for the agent's own login, if provisioned. */
    @Column(name = "user_id")
    private Long userId;

    /** HR employee record, if the agent is on payroll. */
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "vehicle_plate", length = 50)
    private String vehiclePlate;

    @Column(name = "vehicle_name", length = 200)
    private String vehicleName;

    /** Commission rate applied to the agent's sales, as a percentage (0-100). */
    @Column(name = "commission_percent", precision = 5, scale = 2)
    private BigDecimal commissionPercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AgentStatus status = AgentStatus.ACTIVE;

    @Column(name = "hired_at")
    private LocalDate hiredAt;

    @Column(name = "terminated_at")
    private LocalDate terminatedAt;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DistributionTerritory> territories = new ArrayList<>();

    /** Attaches a territory, keeping the bidirectional relationship consistent. */
    public void addTerritory(DistributionTerritory territory) {
        territory.setAgent(this);
        this.territories.add(territory);
    }
}
