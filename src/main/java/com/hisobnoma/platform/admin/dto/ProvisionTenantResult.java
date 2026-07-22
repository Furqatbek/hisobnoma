package com.hisobnoma.platform.admin.dto;

/** Summary of a completed tenant provisioning. */
public record ProvisionTenantResult(
        Long tenantId,
        String code,
        int accountsCreated,
        Long adminUserId,
        String adminUsername,
        int fiscalYear) {
}
