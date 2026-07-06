package com.hisobnoma.platform.distribution.entity;

import java.util.Set;

/**
 * Fulfilment lifecycle of a distribution (B2B wholesale) order.
 *
 * <pre>
 * DRAFT ─▶ CONFIRMED ─▶ PICKING ─▶ LOADED ─▶ IN_TRANSIT ─▶ DELIVERED ─▶ INVOICED
 *   │          │           │          │           │
 *   └──────────┴───────────┴──────────┴───────────┴──▶ CANCELLED
 * </pre>
 *
 * Stock is reserved on {@code CONFIRMED}, deducted on {@code DELIVERED}, and the
 * AR invoice is raised on {@code INVOICED}. {@code INVOICED} and {@code CANCELLED}
 * are terminal.
 */
public enum DistributionOrderStatus {
    DRAFT,
    CONFIRMED,
    PICKING,
    LOADED,
    IN_TRANSIT,
    DELIVERED,
    INVOICED,
    CANCELLED;

    /** Statuses this one may legally transition to. */
    public Set<DistributionOrderStatus> allowedNext() {
        return switch (this) {
            case DRAFT -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> Set.of(PICKING, CANCELLED);
            case PICKING -> Set.of(LOADED, CANCELLED);
            case LOADED -> Set.of(IN_TRANSIT, CANCELLED);
            case IN_TRANSIT -> Set.of(DELIVERED, CANCELLED);
            case DELIVERED -> Set.of(INVOICED);
            case INVOICED, CANCELLED -> Set.of();
        };
    }

    public boolean canTransitionTo(DistributionOrderStatus target) {
        return allowedNext().contains(target);
    }

    public boolean isTerminal() {
        return this == INVOICED || this == CANCELLED;
    }

    /** True once stock has been reserved and not yet released/deducted. */
    public boolean holdsReservation() {
        return this == CONFIRMED || this == PICKING || this == LOADED || this == IN_TRANSIT;
    }
}
