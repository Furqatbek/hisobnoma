package com.hisobnoma.platform.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Tenant-aware entity class that extends AuditableEntity.
 * Adds tenantId field for multi-tenancy support.
 * All tenant-specific entities should extend this class.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantAwareEntity extends AuditableEntity {

    @Column(name = "tenant_id")
    private Long tenantId;
}
