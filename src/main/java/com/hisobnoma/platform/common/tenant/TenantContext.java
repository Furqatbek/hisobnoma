package com.hisobnoma.platform.common.tenant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    public static Long getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenant(Long tenantId) {
        log.debug("Setting current tenant to: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static void clear() {
        log.debug("Clearing tenant context");
        CURRENT_TENANT.remove();
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }
}
