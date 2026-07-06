package com.hisobnoma.platform.distribution.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

/**
 * A planned selling route: an ordered set of customer stops an agent visits,
 * optionally on a fixed weekday within a territory.
 */
@Entity
@Table(name = "distribution_routes",
        uniqueConstraints = @UniqueConstraint(name = "uk_dist_routes_code", columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_dist_routes_tenant", columnList = "tenant_id"),
                @Index(name = "idx_dist_routes_agent", columnList = "agent_id"),
                @Index(name = "idx_dist_routes_status", columnList = "tenant_id, status")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DistributionRoute extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "territory_region_id")
    private Long territoryRegionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private RouteStatus status = RouteStatus.DRAFT;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DistributionRouteStop> stops = new ArrayList<>();

    public void addStop(DistributionRouteStop stop) {
        stop.setRoute(this);
        this.stops.add(stop);
    }
}
