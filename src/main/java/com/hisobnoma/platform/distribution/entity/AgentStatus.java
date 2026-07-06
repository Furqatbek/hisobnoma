package com.hisobnoma.platform.distribution.entity;

/**
 * Lifecycle status of a distribution (sales/route) agent.
 */
public enum AgentStatus {
    /**
     * Active agent who can take orders and be assigned routes.
     */
    ACTIVE,

    /**
     * Temporarily suspended (e.g. disciplinary, leave). Retained but cannot operate.
     */
    SUSPENDED,

    /**
     * Permanently deactivated. Kept for historical order/KPI attribution.
     */
    TERMINATED
}
