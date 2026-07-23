package com.hisobnoma.platform.distribution.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A field visit by an agent to a customer: a GPS-stamped check-in, an outcome, and
 * an optional check-out. May be tied to a route/stop and to an order placed during it.
 */
@Entity
@Table(name = "distribution_visits", indexes = {
    @Index(name = "idx_dist_visits_tenant", columnList = "tenant_id"),
    @Index(name = "idx_dist_visits_agent", columnList = "tenant_id, agent_id"),
    @Index(name = "idx_dist_visits_customer", columnList = "customer_id"),
    @Index(name = "idx_dist_visits_checkin", columnList = "tenant_id, check_in_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DistributionVisit extends TenantAwareEntity {

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "route_stop_id")
    private Long routeStopId;

    @Column(name = "check_in_at", nullable = false)
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @Column(name = "check_in_lat", precision = 10, scale = 7)
    private BigDecimal checkInLat;

    @Column(name = "check_in_lng", precision = 10, scale = 7)
    private BigDecimal checkInLng;

    @Column(name = "check_out_lat", precision = 10, scale = 7)
    private BigDecimal checkOutLat;

    @Column(name = "check_out_lng", precision = 10, scale = 7)
    private BigDecimal checkOutLng;

    @Enumerated(EnumType.STRING)
    @Column(name = "visit_type", nullable = false, length = 20)
    @Builder.Default
    private VisitType visitType = VisitType.PLANNED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VisitOutcome outcome = VisitOutcome.PENDING;

    @Column(name = "distribution_order_id")
    private Long distributionOrderId;

    /** Cash collected against the customer's outstanding AR invoices during this visit. */
    @Column(name = "collected_amount", precision = 19, scale = 4)
    private BigDecimal collectedAmount;

    /** The AR payment created for {@link #collectedAmount} (completed + GL-posted). */
    @Column(name = "ar_payment_id")
    private Long arPaymentId;

    @Column(length = 1000)
    private String notes;
}
