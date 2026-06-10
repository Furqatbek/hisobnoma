package com.hisobnoma.platform.web.entity;

import java.util.Map;
import java.util.Set;

/**
 * Lifecycle of an online shop order.
 */
public enum WebOrderStatus {
    NEW,
    CONFIRMED,
    DELIVERING,
    COMPLETED,
    CANCELLED;

    private static final Map<WebOrderStatus, Set<WebOrderStatus>> ALLOWED = Map.of(
            NEW, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(DELIVERING, COMPLETED, CANCELLED),
            DELIVERING, Set.of(COMPLETED, CANCELLED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canTransitionTo(WebOrderStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
