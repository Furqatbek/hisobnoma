package com.hisobnoma.platform.distribution.entity;

import java.util.Set;

/**
 * Lifecycle of a van-sales loadout.
 *
 * <pre>
 * DRAFT ─▶ LOADED ─▶ RECONCILED
 *   │        │
 *   └────────┴──▶ CANCELLED
 * </pre>
 *
 * Stock is transferred warehouse→vehicle on {@code LOADED}; at {@code RECONCILED}
 * the agent's returns go back to the warehouse, damaged goods are written off, and
 * the sold quantity (loaded − returned − damaged) is issued from the vehicle.
 * {@code RECONCILED} and {@code CANCELLED} are terminal.
 */
public enum VanLoadoutStatus {
    DRAFT,
    LOADED,
    RECONCILED,
    CANCELLED;

    public Set<VanLoadoutStatus> allowedNext() {
        return switch (this) {
            case DRAFT -> Set.of(LOADED, CANCELLED);
            case LOADED -> Set.of(RECONCILED, CANCELLED);
            case RECONCILED, CANCELLED -> Set.of();
        };
    }

    public boolean canTransitionTo(VanLoadoutStatus target) {
        return allowedNext().contains(target);
    }
}
