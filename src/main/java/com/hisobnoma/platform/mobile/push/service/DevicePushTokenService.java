package com.hisobnoma.platform.mobile.push.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.mobile.push.dto.RegisterPushTokenRequest;
import com.hisobnoma.platform.mobile.push.entity.DevicePushToken;
import com.hisobnoma.platform.mobile.push.entity.PushEnvironment;
import com.hisobnoma.platform.mobile.push.repository.DevicePushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Registration / removal of APNs device tokens for the authenticated user. Upserts on the
 * device token itself (a re-register just refreshes ownership + environment + last-seen).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DevicePushTokenService {

    private final DevicePushTokenRepository repository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Intentionally NOT {@code @Transactional}: the repository's own per-method transactions
     * ({@code saveAndFlush} is transactional) mean a lost insert race (unique-constraint violation)
     * rolls back cleanly and independently, letting the catch retry as an update. A single outer
     * transaction would instead be poisoned (rollback-only) by the first violation and the retry
     * could not commit.
     */
    public void registerToken(RegisterPushTokenRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        Long userId = securityContextHelper.getCurrentUserId();
        String token = request.getToken().trim();
        PushEnvironment environment = PushEnvironment.from(request.getEnvironment());
        String platform = (request.getPlatform() != null && !request.getPlatform().isBlank())
                ? request.getPlatform().trim() : "ios";

        try {
            upsert(tenantId, userId, token, platform, environment, request.getAppVersion());
        } catch (DataIntegrityViolationException race) {
            // Concurrent re-register of the same (tenant, token) lost the insert race on the unique
            // constraint. The row now exists — reload and update it so registration stays idempotent
            // instead of surfacing a 500.
            upsert(tenantId, userId, token, platform, environment, request.getAppVersion());
        }
        log.debug("Registered push token for user {} ({}/{})", userId, platform, environment);
    }

    private void upsert(Long tenantId, Long userId, String token, String platform,
                        PushEnvironment environment, String appVersion) {
        DevicePushToken entity = repository.findByTenantIdAndToken(tenantId, token)
                .orElseGet(() -> DevicePushToken.builder().token(token).tenantId(tenantId).build());
        entity.setUserId(userId);
        entity.setPlatform(platform);
        entity.setEnvironment(environment);
        entity.setAppVersion(appVersion);
        entity.setLastSeenAt(Instant.now());
        repository.saveAndFlush(entity);
    }

    @Transactional
    public void removeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        Long tenantId = securityContextHelper.getRequiredTenantId();
        repository.deleteByTenantIdAndToken(tenantId, token.trim());
    }
}
