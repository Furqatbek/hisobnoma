package com.hisobnoma.platform.common.tenant;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Aspect for handling tenant-aware operations.
 * Sets tenant ID on entities before save operations.
 */
@Slf4j
@Aspect
@Component
public class TenantAspect {

    @Before("execution(* com.hisobnoma.platform..repository.*Repository.save*(..)) && args(entity, ..)")
    public void setTenantBeforeSave(JoinPoint joinPoint, Object entity) {
        if (entity instanceof TenantAwareEntity tenantEntity) {
            if (tenantEntity.getTenantId() == null && TenantContext.hasTenant()) {
                tenantEntity.setTenantId(TenantContext.getCurrentTenant());
                log.debug("Set tenant ID {} on entity {}", TenantContext.getCurrentTenant(),
                        entity.getClass().getSimpleName());
            }
        }
    }
}
