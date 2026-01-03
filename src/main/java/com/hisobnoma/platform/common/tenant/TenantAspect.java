package com.hisobnoma.platform.common.tenant;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class TenantAspect {

    @PersistenceContext
    private EntityManager entityManager;

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

    @Before("execution(* com.hisobnoma.platform..repository.*Repository.find*(..))")
    public void enableTenantFilter(JoinPoint joinPoint) {
        if (TenantContext.hasTenant()) {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("tenantFilter") == null) {
                session.enableFilter("tenantFilter")
                        .setParameter("tenantId", TenantContext.getCurrentTenant());
                log.debug("Enabled tenant filter for tenant: {}", TenantContext.getCurrentTenant());
            }
        }
    }
}
