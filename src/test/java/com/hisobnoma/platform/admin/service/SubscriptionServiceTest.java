package com.hisobnoma.platform.admin.service;

import com.hisobnoma.platform.admin.dto.SubscriptionDto;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.entity.TenantPlan;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final Long TENANT_ID = 7L;

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks private SubscriptionService service;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .name("Shop").code("SHOP").active(true)
                .plan(TenantPlan.STARTER)
                .maxUsers(TenantPlan.STARTER.getMaxUsers())
                .maxLocations(TenantPlan.STARTER.getMaxLocations())
                .build();
        tenant.setId(TENANT_ID);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
    }

    @Test
    void getSubscription_reportsUsageAndSwitchablePlans() {
        when(userRepository.countByTenantIdAndEnabledTrue(TENANT_ID)).thenReturn(3L);
        when(locationRepository.countByTenantIdAndParentLocationIdIsNull(TENANT_ID)).thenReturn(2L);

        SubscriptionDto dto = service.getSubscription();

        assertEquals("STARTER", dto.getCurrentPlan());
        assertEquals(3L, dto.getUsedUsers());
        assertEquals(2L, dto.getUsedLocations());
        assertEquals(TenantPlan.values().length, dto.getPlans().size());

        SubscriptionDto.PlanDto free = planOf(dto, "FREE");
        assertFalse(free.isSwitchable(), "3 users exceed FREE's limit of 2");
        assertFalse(free.getBlockedReason().isEmpty());

        SubscriptionDto.PlanDto business = planOf(dto, "BUSINESS");
        assertTrue(business.isSwitchable());
        assertTrue(business.getBlockedReason().isEmpty());

        SubscriptionDto.PlanDto starter = planOf(dto, "STARTER");
        assertTrue(starter.isCurrent());
        assertFalse(starter.isSwitchable(), "Current plan is not a switch target");
    }

    @Test
    void changePlan_upgrade_appliesNewLimits() {
        when(userRepository.countByTenantIdAndEnabledTrue(TENANT_ID)).thenReturn(3L);
        when(locationRepository.countByTenantIdAndParentLocationIdIsNull(TENANT_ID)).thenReturn(2L);

        SubscriptionDto dto = service.changePlan("BUSINESS");

        assertEquals("BUSINESS", dto.getCurrentPlan());
        assertEquals(TenantPlan.BUSINESS, tenant.getPlan());
        assertEquals(TenantPlan.BUSINESS.getMaxUsers(), tenant.getMaxUsers());
        assertEquals(TenantPlan.BUSINESS.getMaxLocations(), tenant.getMaxLocations());
        verify(tenantRepository).save(tenant);
    }

    @Test
    void changePlan_downgradeBelowUsage_rejected() {
        when(userRepository.countByTenantIdAndEnabledTrue(TENANT_ID)).thenReturn(3L);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.changePlan("FREE"));
        assertTrue(ex.getMessage().contains("3"));
        assertEquals(TenantPlan.STARTER, tenant.getPlan(), "Plan must not change on rejection");
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void changePlan_samePlanOrUnknown_rejected() {
        assertThrows(ValidationException.class, () -> service.changePlan("STARTER"));
        assertThrows(ValidationException.class, () -> service.changePlan("GOLD"));
        verify(tenantRepository, never()).save(any());
    }

    private SubscriptionDto.PlanDto planOf(SubscriptionDto dto, String code) {
        return dto.getPlans().stream()
                .filter(p -> p.getCode().equals(code))
                .findFirst().orElseThrow();
    }
}
