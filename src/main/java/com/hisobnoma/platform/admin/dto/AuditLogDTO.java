package com.hisobnoma.platform.admin.dto;

import com.hisobnoma.platform.admin.entity.AuditLog;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String username;
    private String userFullName;
    private AuditLog.AuditAction action;
    private String entityType;
    private Long entityId;
    private String entityName;
    private String module;
    private String description;
    private String oldValues;
    private String newValues;
    private String changedFields;
    private String ipAddress;
    private String userAgent;
    private String requestUrl;
    private String requestMethod;
    private Instant actionTimestamp;
    private Long executionTimeMs;
    private boolean success;
    private String errorMessage;
    private String correlationId;
}
