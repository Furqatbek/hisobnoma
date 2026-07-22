package com.hisobnoma.platform.admin.service;

import com.hisobnoma.platform.admin.dto.SubscriptionDto;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.entity.TenantPlan;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Self-service plan management for the tenant's own subscription. Changing
 * plan applies the tier's limits immediately; a downgrade is refused while the
 * tenant's current usage (active users, root locations) exceeds the target
 * limits. Recurring billing is a later phase (docs/MULTI_TENANCY.md §9) —
 * until then price is informational and no invoice is created.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public SubscriptionDto getSubscription() {
        Tenant tenant = currentTenant();
        long usedUsers = userRepository.countByTenantIdAndEnabledTrue(tenant.getId());
        long usedLocations = locationRepository.countByTenantIdAndParentLocationIdIsNull(tenant.getId());

        List<SubscriptionDto.PlanDto> plans = Arrays.stream(TenantPlan.values())
                .map(plan -> toPlanDto(plan, tenant, usedUsers, usedLocations))
                .toList();

        return SubscriptionDto.builder()
                .currentPlan(tenant.getPlan().name())
                .subscriptionExpiresAt(tenant.getSubscriptionExpiresAt())
                .maxUsers(tenant.getMaxUsers())
                .maxLocations(tenant.getMaxLocations())
                .usedUsers(usedUsers)
                .usedLocations(usedLocations)
                .plans(plans)
                .build();
    }

    @Transactional
    public SubscriptionDto changePlan(String planCode) {
        TenantPlan target = parsePlan(planCode);
        Tenant tenant = currentTenant();

        if (tenant.getPlan() == target) {
            throw new ValidationException("Tenant is already on plan " + target.name());
        }

        long usedUsers = userRepository.countByTenantIdAndEnabledTrue(tenant.getId());
        long usedLocations = locationRepository.countByTenantIdAndParentLocationIdIsNull(tenant.getId());
        String blocked = blockedReason(target, usedUsers, usedLocations);
        if (!blocked.isEmpty()) {
            throw new ValidationException(blocked);
        }

        TenantPlan previous = tenant.getPlan();
        tenant.setPlan(target);
        tenant.setMaxUsers(target.getMaxUsers());
        tenant.setMaxLocations(target.getMaxLocations());
        tenantRepository.save(tenant);
        log.info("Tenant {} changed plan {} -> {} (by user {})",
                tenant.getId(), previous, target, securityContextHelper.getCurrentUserId());

        return getSubscription();
    }

    private SubscriptionDto.PlanDto toPlanDto(TenantPlan plan, Tenant tenant,
                                              long usedUsers, long usedLocations) {
        String blocked = blockedReason(plan, usedUsers, usedLocations);
        return SubscriptionDto.PlanDto.builder()
                .code(plan.name())
                .name(plan.getDisplayName())
                .monthlyPrice(plan.getMonthlyPrice())
                .maxUsers(plan.getMaxUsers())
                .maxLocations(plan.getMaxLocations())
                .current(tenant.getPlan() == plan)
                .switchable(tenant.getPlan() != plan && blocked.isEmpty())
                .blockedReason(blocked)
                .build();
    }

    /** Empty string when the plan fits the tenant's current usage. */
    private String blockedReason(TenantPlan plan, long usedUsers, long usedLocations) {
        if (usedUsers > plan.getMaxUsers()) {
            return "Фаол фойдаланувчилар сони (" + usedUsers + ") режа лимитидан ("
                    + plan.getMaxUsers() + ") кўп — аввал фойдаланувчиларни камайтиринг";
        }
        if (usedLocations > plan.getMaxLocations()) {
            return "Омборлар сони (" + usedLocations + ") режа лимитидан ("
                    + plan.getMaxLocations() + ") кўп — аввал омборларни камайтиринг";
        }
        return "";
    }

    private TenantPlan parsePlan(String planCode) {
        try {
            return TenantPlan.valueOf(planCode.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Unknown plan: " + planCode);
        }
    }

    private Tenant currentTenant() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId));
    }
}
