package com.hisobnoma.platform.distribution.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * One customer stop on a {@link DistributionRoute}, in visit order.
 */
@Entity
@Table(name = "distribution_route_stops", indexes = {
    @Index(name = "idx_route_stops_tenant", columnList = "tenant_id"),
    @Index(name = "idx_route_stops_route", columnList = "route_id"),
    @Index(name = "idx_route_stops_customer", columnList = "customer_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DistributionRouteStop extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private DistributionRoute route;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "visit_window_start")
    private LocalTime visitWindowStart;

    @Column(name = "visit_window_end")
    private LocalTime visitWindowEnd;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 500)
    private String address;

    @Column(length = 500)
    private String notes;
}
