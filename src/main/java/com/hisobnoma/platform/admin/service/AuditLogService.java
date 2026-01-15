package com.hisobnoma.platform.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.admin.dto.AuditLogDTO;
import com.hisobnoma.platform.admin.entity.AuditLog;
import com.hisobnoma.platform.admin.mapper.AuditLogMapper;
import com.hisobnoma.platform.admin.repository.AuditLogRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final SecurityContextHelper securityContextHelper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> getAuditLogs(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<AuditLog> logs = auditLogRepository.findByTenantId(tenantId, pageable);
        return PageResponse.of(logs.map(auditLogMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> getAuditLogsByUser(Long userId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<AuditLog> logs = auditLogRepository.findByTenantIdAndUserId(tenantId, userId, pageable);
        return PageResponse.of(logs.map(auditLogMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<AuditLog> logs = auditLogRepository.findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId, pageable);
        return PageResponse.of(logs.map(auditLogMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> getAuditLogsByAction(AuditLog.AuditAction action, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<AuditLog> logs = auditLogRepository.findByTenantIdAndAction(tenantId, action, pageable);
        return PageResponse.of(logs.map(auditLogMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> getAuditLogsByModule(String module, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<AuditLog> logs = auditLogRepository.findByTenantIdAndModule(tenantId, module, pageable);
        return PageResponse.of(logs.map(auditLogMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> getAuditLogsByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<AuditLog> logs = auditLogRepository.findByTenantIdAndDateRange(tenantId, startDate, endDate, pageable);
        return PageResponse.of(logs.map(auditLogMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> getFailedActions(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<AuditLog> logs = auditLogRepository.findFailedActionsByTenantId(tenantId, pageable);
        return PageResponse.of(logs.map(auditLogMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<Object[]> getActionStats(int days) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return auditLogRepository.countActionsByTenantIdSince(tenantId, since);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getModuleActivityStats(int days) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return auditLogRepository.countModuleActivityByTenantIdSince(tenantId, since);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getMostActiveUsers(int days) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return auditLogRepository.findMostActiveUsersByTenantIdSince(tenantId, since);
    }

    @Transactional(readOnly = true)
    public long getFailedLoginCount(int hours) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return auditLogRepository.countFailedLoginsSince(tenantId, since);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(AuditLog.AuditAction action, String entityType, Long entityId,
                         String entityName, String module, String description) {
        logInternal(action, entityType, entityId, entityName, module, description, null, null, null, true, null);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(AuditLog.AuditAction action, String entityType, Long entityId,
                         String entityName, String module, String description,
                         Object oldValue, Object newValue) {
        logInternal(action, entityType, entityId, entityName, module, description,
                serializeToJson(oldValue), serializeToJson(newValue), null, true, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(AuditLog.AuditAction action, String entityType, Long entityId,
                        String entityName, String module, String description) {
        return logInternal(action, entityType, entityId, entityName, module, description, null, null, null, true, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(AuditLog.AuditAction action, String entityType, Long entityId,
                        String entityName, String module, String description,
                        String oldValues, String newValues, String changedFields) {
        return logInternal(action, entityType, entityId, entityName, module, description, oldValues, newValues, changedFields, true, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logFailure(AuditLog.AuditAction action, String entityType, Long entityId,
                               String entityName, String module, String description, String errorMessage) {
        return logInternal(action, entityType, entityId, entityName, module, description, null, null, null, false, errorMessage);
    }

    private AuditLog logInternal(AuditLog.AuditAction action, String entityType, Long entityId,
                                  String entityName, String module, String description,
                                  String oldValues, String newValues, String changedFields,
                                  boolean success, String errorMessage) {
        try {
            Long tenantId = null;
            Long userId = null;
            String username = null;
            String userFullName = null;

            try {
                UserPrincipal user = securityContextHelper.getRequiredCurrentUser();
                tenantId = user.getTenantId();
                userId = user.getId();
                username = user.getUsername();
                userFullName = user.getUsername(); // UserPrincipal doesn't store full name
            } catch (Exception e) {
                log.debug("Could not get user info for audit log: {}", e.getMessage());
            }

            String ipAddress = null;
            String userAgent = null;
            String requestUrl = null;
            String requestMethod = null;

            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    ipAddress = getClientIpAddress(request);
                    userAgent = request.getHeader("User-Agent");
                    requestUrl = request.getRequestURI();
                    requestMethod = request.getMethod();
                }
            } catch (Exception e) {
                log.debug("Could not get request info for audit log: {}", e.getMessage());
            }

            AuditLog auditLog = AuditLog.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .username(username)
                    .userFullName(userFullName)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityName(entityName)
                    .module(module)
                    .description(description)
                    .oldValues(oldValues)
                    .newValues(newValues)
                    .changedFields(changedFields)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
                    .requestUrl(requestUrl)
                    .requestMethod(requestMethod)
                    .actionTimestamp(Instant.now())
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();

            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_CLIENT_IP",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle multiple IPs in X-Forwarded-For
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private String serializeToJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return obj.toString();
        }
    }
}
