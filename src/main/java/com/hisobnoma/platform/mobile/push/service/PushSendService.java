package com.hisobnoma.platform.mobile.push.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.mobile.push.apns.ApnsClient;
import com.hisobnoma.platform.mobile.push.apns.ApnsPayload;
import com.hisobnoma.platform.mobile.push.apns.ApnsResult;
import com.hisobnoma.platform.mobile.push.dto.PushSendResult;
import com.hisobnoma.platform.mobile.push.dto.SendNotificationRequest;
import com.hisobnoma.platform.mobile.push.entity.DevicePushToken;
import com.hisobnoma.platform.mobile.push.repository.DevicePushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Fans a notification out to a tenant's (or a single user's) APNs tokens, routing each to the
 * correct Apple host by its environment, and prunes tokens APNs reports as permanently dead.
 * Deliberately NOT wrapped in a DB transaction — the APNs network calls run outside any open
 * transaction; only the dead-token cleanup touches the DB (in its own short transaction).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushSendService {

    private final DevicePushTokenRepository repository;
    private final ApnsClient apnsClient;
    private final SecurityContextHelper securityContextHelper;

    public PushSendResult send(SendNotificationRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        List<DevicePushToken> tokens;
        if ("user".equalsIgnoreCase(request.getAudience())) {
            if (request.getUserId() == null) {
                throw new BusinessException("userId is required when audience is 'user'", "USER_ID_REQUIRED");
            }
            tokens = repository.findByTenantIdAndUserId(tenantId, request.getUserId());
        } else {
            tokens = repository.findByTenantId(tenantId);
        }

        ApnsPayload payload = new ApnsPayload(
                request.getTitle(), request.getBody(), request.getBadge(),
                request.getType(), request.getId(), request.getRoute());

        int sent = 0, failed = 0;
        List<Long> deadIds = new ArrayList<>();
        for (DevicePushToken token : tokens) {
            ApnsResult result = apnsClient.send(token.getToken(), token.getEnvironment(), payload);
            if (result.isSuccess()) {
                sent++;
            } else {
                failed++;
                if (result.tokenDead()) {
                    deadIds.add(token.getId());
                }
            }
        }
        if (!deadIds.isEmpty()) {
            repository.deleteAllById(deadIds); // own transaction; prunes tokens APNs rejected
            log.info("Pruned {} dead push tokens for tenant {}", deadIds.size(), tenantId);
        }

        boolean configured = apnsClient.isConfigured();
        if (!configured) {
            log.info("Push send requested but APNs is not configured — {} recipient(s) skipped", tokens.size());
        }
        return new PushSendResult(tokens.size(), sent, failed, deadIds.size(), configured);
    }
}
