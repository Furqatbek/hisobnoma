package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.mobile.dto.DeviceTokenDTO;
import com.hisobnoma.platform.mobile.dto.RegisterDeviceRequest;
import com.hisobnoma.platform.mobile.entity.DeviceToken;
import com.hisobnoma.platform.mobile.mapper.DeviceTokenMapper;
import com.hisobnoma.platform.mobile.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for managing device tokens for push notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenMapper deviceTokenMapper;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Register or update a device for push notifications.
     */
    @Transactional
    public DeviceTokenDTO registerDevice(RegisterDeviceRequest request) {
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        Long tenantId = securityContextHelper.getRequiredTenantId();

        DeviceToken deviceToken = deviceTokenRepository
                .findByUserIdAndDeviceIdAndTenantId(userId, request.getDeviceId(), tenantId)
                .orElseGet(() -> {
                    DeviceToken newToken = deviceTokenMapper.fromRequest(request);
                    newToken.setUserId(userId);
                    newToken.setTenantId(tenantId);
                    return newToken;
                });

        // Update token details
        deviceToken.setFcmToken(request.getFcmToken());
        deviceToken.setPlatform(request.getPlatform());
        deviceToken.setDeviceName(request.getDeviceName());
        deviceToken.setDeviceModel(request.getDeviceModel());
        deviceToken.setOsVersion(request.getOsVersion());
        deviceToken.setAppVersion(request.getAppVersion());
        deviceToken.setLastActiveAt(Instant.now());
        deviceToken.setActive(true);

        deviceToken = deviceTokenRepository.save(deviceToken);
        log.info("Device registered for user {} with device ID {}", userId, request.getDeviceId());

        return deviceTokenMapper.toDto(deviceToken);
    }

    /**
     * Get all registered devices for the current user.
     */
    @Transactional(readOnly = true)
    public List<DeviceTokenDTO> getDevices() {
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return deviceTokenMapper.toDtoList(
                deviceTokenRepository.findByUserIdAndTenantIdAndActiveTrue(userId, tenantId));
    }

    /**
     * Deactivate a device (logout from push notifications).
     */
    @Transactional
    public void deactivateDevice(String deviceId) {
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        deviceTokenRepository.deactivateDevice(userId, deviceId);
        log.info("Device {} deactivated for user {}", deviceId, userId);
    }

    /**
     * Update last active timestamp for a device.
     */
    @Transactional
    public void updateLastActive(Long deviceTokenId) {
        deviceTokenRepository.updateLastActiveAt(deviceTokenId, Instant.now());
    }

    /**
     * Get active device tokens for a user (used by push notification service).
     */
    @Transactional(readOnly = true)
    public List<DeviceToken> getActiveDeviceTokens(Long userId) {
        return deviceTokenRepository.findByUserIdAndActiveTrue(userId);
    }

    /**
     * Get active device tokens for multiple users (for broadcast).
     */
    @Transactional(readOnly = true)
    public List<DeviceToken> getActiveDeviceTokensForUsers(List<Long> userIds) {
        return deviceTokenRepository.findActiveByUserIds(userIds);
    }

    /**
     * Cleanup inactive devices (scheduled job).
     * Devices not active for 90 days are deactivated.
     */
    @Scheduled(cron = "0 0 3 * * ?") // Run at 3 AM daily
    @Transactional
    public void cleanupInactiveDevices() {
        Instant cutoffDate = Instant.now().minus(90, ChronoUnit.DAYS);
        int deactivated = deviceTokenRepository.deactivateInactiveDevices(cutoffDate);
        log.info("Deactivated {} inactive devices", deactivated);
    }
}
