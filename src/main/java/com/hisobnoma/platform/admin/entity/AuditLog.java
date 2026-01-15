package com.hisobnoma.platform.admin.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Audit log entity for tracking all system activities and changes.
 * Provides a comprehensive audit trail for compliance and debugging.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_tenant", columnList = "tenant_id"),
    @Index(name = "idx_audit_logs_user", columnList = "user_id"),
    @Index(name = "idx_audit_logs_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_logs_action", columnList = "action"),
    @Index(name = "idx_audit_logs_timestamp", columnList = "action_timestamp"),
    @Index(name = "idx_audit_logs_module", columnList = "module")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseEntity {

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "user_full_name", length = 200)
    private String userFullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name", length = 200)
    private String entityName;

    @Column(length = 50)
    private String module;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(name = "changed_fields", columnDefinition = "TEXT")
    private String changedFields;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_url", length = 500)
    private String requestUrl;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "action_timestamp", nullable = false)
    @Builder.Default
    private Instant actionTimestamp = Instant.now();

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "is_success", nullable = false)
    @Builder.Default
    private boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "correlation_id", length = 50)
    private String correlationId;

    public enum AuditAction {
        CREATE,
        READ,
        UPDATE,
        DELETE,
        LOGIN,
        LOGOUT,
        LOGIN_FAILED,
        PASSWORD_CHANGE,
        PERMISSION_CHANGE,
        EXPORT,
        IMPORT,
        APPROVE,
        REJECT,
        SUBMIT,
        CANCEL,
        ACTIVATE,
        DEACTIVATE,
        TRANSFER,
        ADJUSTMENT,
        CUSTOM
    }
}
