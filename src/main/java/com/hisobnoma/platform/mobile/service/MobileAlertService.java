package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.mobile.dto.AlertPreferenceDTO;
import com.hisobnoma.platform.mobile.dto.MobileAlertDTO;
import com.hisobnoma.platform.mobile.entity.AlertPreference;
import com.hisobnoma.platform.mobile.entity.MobileAlert;
import com.hisobnoma.platform.mobile.mapper.AlertPreferenceMapper;
import com.hisobnoma.platform.mobile.mapper.MobileAlertMapper;
import com.hisobnoma.platform.mobile.repository.AlertPreferenceRepository;
import com.hisobnoma.platform.mobile.repository.MobileAlertRepository;
import com.hisobnoma.platform.telegram.service.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service for managing mobile alerts and notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileAlertService {

    private final MobileAlertRepository alertRepository;
    private final AlertPreferenceRepository preferenceRepository;
    private final MobileAlertMapper alertMapper;
    private final AlertPreferenceMapper preferenceMapper;
    private final SecurityContextHelper securityContextHelper;
    private final PushNotificationService pushNotificationService;

    @Autowired(required = false)
    private TelegramNotificationService telegramNotificationService;

    /**
     * Get paginated alerts for the current user.
     */
    @Transactional(readOnly = true)
    public Page<MobileAlertDTO> getAlerts(Pageable pageable) {
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return alertRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId, pageable)
                .map(alertMapper::toDto);
    }

    /**
     * Get unread alerts for the current user.
     */
    @Transactional(readOnly = true)
    public Page<MobileAlertDTO> getUnreadAlerts(Pageable pageable) {
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return alertRepository.findByUserIdAndTenantIdAndReadFalseOrderByCreatedAtDesc(userId, tenantId, pageable)
                .map(alertMapper::toDto);
    }

    /**
     * Get alerts by type for the current user.
     */
    @Transactional(readOnly = true)
    public Page<MobileAlertDTO> getAlertsByType(MobileAlert.AlertType alertType, Pageable pageable) {
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return alertRepository.findByUserIdAndTenantIdAndAlertTypeOrderByCreatedAtDesc(userId, tenantId, alertType, pageable)
                .map(alertMapper::toDto);
    }

    /**
     * Get unread alert count for the current user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return alertRepository.countUnreadByUser(userId, tenantId, Instant.now());
    }

    /**
     * Mark alert as read.
     */
    @Transactional
    public void markAsRead(Long alertId) {
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        int updated = alertRepository.markAsRead(alertId, userId, Instant.now());
        if (updated == 0) {
            throw new NotFoundException("Alert not found or not owned by user");
        }
    }

    /**
     * Mark all alerts as read for the current user.
     */
    @Transactional
    public void markAllAsRead() {
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        Long tenantId = securityContextHelper.getRequiredTenantId();
        alertRepository.markAllAsRead(userId, tenantId, Instant.now());
    }

    /**
     * Create and send an alert to a user.
     */
    @Transactional
    public MobileAlertDTO createAlert(Long userId, Long tenantId, MobileAlert.AlertType alertType,
                                       String title, String message, MobileAlert.AlertPriority priority,
                                       String entityType, Long entityId, String data) {
        MobileAlert alert = MobileAlert.builder()
                .userId(userId)
                .alertType(alertType)
                .title(title)
                .message(message)
                .priority(priority)
                .entityType(entityType)
                .entityId(entityId)
                .data(data)
                .read(false)
                .sentViaPush(false)
                .build();
        alert.setTenantId(tenantId);

        alert = alertRepository.save(alert);

        // Send push notification if user has it enabled
        if (isPushEnabledForUser(userId, tenantId, alertType)) {
            pushNotificationService.sendPushNotification(userId, title, message, alertType.name(), entityId);
            alert.setSentViaPush(true);
            alert.setPushSentAt(Instant.now());
            alert = alertRepository.save(alert);
        }

        // Send Telegram notification if user has it enabled
        if (telegramNotificationService != null) {
            telegramNotificationService.sendAlert(userId, tenantId, alertType, title, message);
        }

        return alertMapper.toDto(alert);
    }

    /**
     * Create a broadcast alert to all users in a tenant.
     */
    @Transactional
    public void createBroadcastAlert(Long tenantId, MobileAlert.AlertType alertType,
                                      String title, String message, MobileAlert.AlertPriority priority) {
        List<Long> userIds = preferenceRepository.findUserIdsWithPushEnabledForType(tenantId, alertType);
        for (Long userId : userIds) {
            createAlert(userId, tenantId, alertType, title, message, priority, null, null, null);
        }
    }

    /**
     * Get alert preferences for the current user.
     */
    @Transactional(readOnly = true)
    public List<AlertPreferenceDTO> getAlertPreferences() {
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return preferenceMapper.toDtoList(preferenceRepository.findByUserIdAndTenantId(userId, tenantId));
    }

    /**
     * Update alert preference.
     */
    @Transactional
    public AlertPreferenceDTO updateAlertPreference(MobileAlert.AlertType alertType, AlertPreferenceDTO dto) {
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        Long tenantId = securityContextHelper.getRequiredTenantId();

        AlertPreference preference = preferenceRepository
                .findByUserIdAndTenantIdAndAlertType(userId, tenantId, alertType)
                .orElseGet(() -> {
                    AlertPreference newPref = AlertPreference.builder()
                            .userId(userId)
                            .alertType(alertType)
                            .build();
                    newPref.setTenantId(tenantId);
                    return newPref;
                });

        preferenceMapper.updateEntity(dto, preference);
        preference = preferenceRepository.save(preference);
        return preferenceMapper.toDto(preference);
    }

    /**
     * Initialize default preferences for a user.
     */
    @Transactional
    public void initializeDefaultPreferences(Long userId, Long tenantId) {
        for (MobileAlert.AlertType type : MobileAlert.AlertType.values()) {
            if (!preferenceRepository.findByUserIdAndTenantIdAndAlertType(userId, tenantId, type).isPresent()) {
                AlertPreference preference = AlertPreference.builder()
                        .userId(userId)
                        .alertType(type)
                        .pushEnabled(getDefaultPushEnabled(type))
                        .inAppEnabled(true)
                        .emailEnabled(false)
                        .smsEnabled(false)
                        .build();
                preference.setTenantId(tenantId);
                preferenceRepository.save(preference);
            }
        }
    }

    private boolean isPushEnabledForUser(Long userId, Long tenantId, MobileAlert.AlertType alertType) {
        return preferenceRepository.existsByUserIdAndTenantIdAndAlertTypeAndPushEnabledTrue(userId, tenantId, alertType);
    }

    private boolean getDefaultPushEnabled(MobileAlert.AlertType type) {
        return switch (type) {
            case LOW_STOCK, OUT_OF_STOCK, LARGE_TRANSACTION, PAYMENT_RECEIVED, APPROVAL_REQUIRED, SYSTEM_ALERT -> true;
            default -> false;
        };
    }

    /**
     * Cleanup expired alerts (scheduled job).
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void cleanupExpiredAlerts() {
        int deleted = alertRepository.deleteExpiredAlerts(Instant.now());
        log.info("Cleaned up {} expired alerts", deleted);
    }
}
