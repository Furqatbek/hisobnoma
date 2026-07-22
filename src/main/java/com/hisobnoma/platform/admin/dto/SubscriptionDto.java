package com.hisobnoma.platform.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Current subscription state + the catalogue of switchable plans. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {

    private String currentPlan;
    private Instant subscriptionExpiresAt;

    private int maxUsers;
    private int maxLocations;
    private long usedUsers;
    private long usedLocations;

    private List<PlanDto> plans;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanDto {
        private String code;
        private String name;
        private BigDecimal monthlyPrice;
        private int maxUsers;
        private int maxLocations;
        private boolean current;
        /** False when downgrading would put current usage over the plan's limits. */
        private boolean switchable;
        /** Why the plan is not switchable (empty when switchable). */
        private String blockedReason;
    }
}
