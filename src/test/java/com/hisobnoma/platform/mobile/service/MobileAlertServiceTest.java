package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileAlertServiceTest {

    @Mock
    private MobileAlertRepository alertRepository;

    @Mock
    private AlertPreferenceRepository preferenceRepository;

    @Mock
    private MobileAlertMapper alertMapper;

    @Mock
    private AlertPreferenceMapper preferenceMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private TelegramNotificationService telegramNotificationService;

    @Mock
    private com.hisobnoma.platform.auth.repository.UserRepository userRepository;

    @InjectMocks
    private MobileAlertService mobileAlertService;

    private void injectTelegramService() {
        ReflectionTestUtils.setField(mobileAlertService, "telegramNotificationService", telegramNotificationService);
    }

    private MobileAlert mobileAlert;
    private MobileAlertDTO mobileAlertDTO;
    private AlertPreference alertPreference;
    private AlertPreferenceDTO alertPreferenceDTO;
    private UserPrincipal userPrincipal;
    private static final Long USER_ID = 10L;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        injectTelegramService();
        userPrincipal = mock(UserPrincipal.class);

        mobileAlert = MobileAlert.builder()
                .id(1L)
                .userId(USER_ID)
                .alertType(MobileAlert.AlertType.LOW_STOCK)
                .title("Low Stock Alert")
                .message("Product XYZ is low on stock")
                .priority(MobileAlert.AlertPriority.HIGH)
                .read(false)
                .sentViaPush(false)
                .build();
        mobileAlert.setTenantId(TENANT_ID);

        mobileAlertDTO = MobileAlertDTO.builder()
                .id(1L)
                .alertType(MobileAlert.AlertType.LOW_STOCK)
                .title("Low Stock Alert")
                .message("Product XYZ is low on stock")
                .priority(MobileAlert.AlertPriority.HIGH)
                .read(false)
                .build();

        alertPreference = AlertPreference.builder()
                .id(1L)
                .userId(USER_ID)
                .alertType(MobileAlert.AlertType.LOW_STOCK)
                .pushEnabled(true)
                .inAppEnabled(true)
                .emailEnabled(false)
                .smsEnabled(false)
                .build();
        alertPreference.setTenantId(TENANT_ID);

        alertPreferenceDTO = AlertPreferenceDTO.builder()
                .id(1L)
                .alertType(MobileAlert.AlertType.LOW_STOCK)
                .pushEnabled(true)
                .inAppEnabled(true)
                .emailEnabled(false)
                .smsEnabled(false)
                .build();
    }

    // ====== getAlerts ======

    @Test
    void getAlerts_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<MobileAlert> page = new PageImpl<>(List.of(mobileAlert), pageable, 1);
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(alertRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(USER_ID, TENANT_ID, pageable))
                .thenReturn(page);
        when(alertMapper.toDto(mobileAlert)).thenReturn(mobileAlertDTO);

        // When
        Page<MobileAlertDTO> result = mobileAlertService.getAlerts(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Low Stock Alert", result.getContent().get(0).getTitle());
    }

    // ====== getUnreadAlerts ======

    @Test
    void getUnreadAlerts_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<MobileAlert> page = new PageImpl<>(List.of(mobileAlert), pageable, 1);
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(alertRepository.findByUserIdAndTenantIdAndReadFalseOrderByCreatedAtDesc(USER_ID, TENANT_ID, pageable))
                .thenReturn(page);
        when(alertMapper.toDto(mobileAlert)).thenReturn(mobileAlertDTO);

        // When
        Page<MobileAlertDTO> result = mobileAlertService.getUnreadAlerts(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertFalse(result.getContent().get(0).isRead());
    }

    // ====== getAlertsByType ======

    @Test
    void getAlertsByType_returnsFilteredAlerts() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<MobileAlert> page = new PageImpl<>(List.of(mobileAlert), pageable, 1);
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(alertRepository.findByUserIdAndTenantIdAndAlertTypeOrderByCreatedAtDesc(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK, pageable))
                .thenReturn(page);
        when(alertMapper.toDto(mobileAlert)).thenReturn(mobileAlertDTO);

        // When
        Page<MobileAlertDTO> result = mobileAlertService.getAlertsByType(MobileAlert.AlertType.LOW_STOCK, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(MobileAlert.AlertType.LOW_STOCK, result.getContent().get(0).getAlertType());
    }

    // ====== getUnreadCount ======

    @Test
    void getUnreadCount_returnsCount() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(alertRepository.countUnreadByUser(eq(USER_ID), eq(TENANT_ID), any(Instant.class))).thenReturn(5L);

        // When
        long result = mobileAlertService.getUnreadCount();

        // Then
        assertEquals(5L, result);
    }

    // ====== markAsRead ======

    @Test
    void markAsRead_success() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(alertRepository.markAsRead(eq(1L), eq(USER_ID), any(Instant.class))).thenReturn(1);

        // When
        mobileAlertService.markAsRead(1L);

        // Then
        verify(alertRepository).markAsRead(eq(1L), eq(USER_ID), any(Instant.class));
    }

    @Test
    void markAsRead_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(alertRepository.markAsRead(eq(999L), eq(USER_ID), any(Instant.class))).thenReturn(0);

        // When/Then
        assertThrows(NotFoundException.class, () -> mobileAlertService.markAsRead(999L));
    }

    // ====== markAllAsRead ======

    @Test
    void markAllAsRead_callsRepository() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);

        // When
        mobileAlertService.markAllAsRead();

        // Then
        verify(alertRepository).markAllAsRead(eq(USER_ID), eq(TENANT_ID), any(Instant.class));
    }

    // ====== createAlert ======

    @Test
    void createAlert_withPushEnabled_sendsPushAndReturnsDto() {
        // Given
        when(alertRepository.save(any(MobileAlert.class))).thenReturn(mobileAlert);
        when(preferenceRepository.existsByUserIdAndTenantIdAndAlertTypeAndPushEnabledTrue(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK)).thenReturn(true);
        when(alertMapper.toDto(any(MobileAlert.class))).thenReturn(mobileAlertDTO);

        // When
        MobileAlertDTO result = mobileAlertService.createAlert(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK,
                "Low Stock Alert", "Product XYZ is low on stock",
                MobileAlert.AlertPriority.HIGH, "Product", 100L, null);

        // Then
        assertNotNull(result);
        assertEquals("Low Stock Alert", result.getTitle());
        verify(pushNotificationService).sendPushNotification(
                USER_ID, "Low Stock Alert", "Product XYZ is low on stock", "LOW_STOCK", 100L);
        verify(alertRepository, times(2)).save(any(MobileAlert.class));
    }

    @Test
    void createAlert_withPushDisabled_skippsPush() {
        // Given
        when(alertRepository.save(any(MobileAlert.class))).thenReturn(mobileAlert);
        when(preferenceRepository.existsByUserIdAndTenantIdAndAlertTypeAndPushEnabledTrue(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK)).thenReturn(false);
        when(alertMapper.toDto(any(MobileAlert.class))).thenReturn(mobileAlertDTO);

        // When
        MobileAlertDTO result = mobileAlertService.createAlert(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK,
                "Low Stock Alert", "Product XYZ is low on stock",
                MobileAlert.AlertPriority.HIGH, null, null, null);

        // Then
        assertNotNull(result);
        verify(pushNotificationService, never()).sendPushNotification(any(), any(), any(), any(), any());
        verify(alertRepository, times(1)).save(any(MobileAlert.class));
    }

    @Test
    void createAlert_withTelegramService_sendsTelegram() {
        // Given
        when(alertRepository.save(any(MobileAlert.class))).thenReturn(mobileAlert);
        when(preferenceRepository.existsByUserIdAndTenantIdAndAlertTypeAndPushEnabledTrue(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK)).thenReturn(false);
        when(alertMapper.toDto(any(MobileAlert.class))).thenReturn(mobileAlertDTO);

        // When
        MobileAlertDTO result = mobileAlertService.createAlert(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK,
                "Low Stock Alert", "Message", MobileAlert.AlertPriority.HIGH, null, null, null);

        // Then
        assertNotNull(result);
        verify(telegramNotificationService).sendAlert(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK, "Low Stock Alert", "Message");
    }

    // ====== createBroadcastAlert ======

    @Test
    void createBroadcastAlert_sendsToAllEnabledUsers() {
        // Given
        List<Long> userIds = List.of(10L, 20L);
        when(preferenceRepository.findUserIdsWithPushEnabledForType(TENANT_ID, MobileAlert.AlertType.SYSTEM_ALERT))
                .thenReturn(userIds);
        when(alertRepository.save(any(MobileAlert.class))).thenReturn(mobileAlert);
        when(preferenceRepository.existsByUserIdAndTenantIdAndAlertTypeAndPushEnabledTrue(
                any(), eq(TENANT_ID), eq(MobileAlert.AlertType.SYSTEM_ALERT))).thenReturn(false);
        when(alertMapper.toDto(any(MobileAlert.class))).thenReturn(mobileAlertDTO);

        // When
        mobileAlertService.createBroadcastAlert(
                TENANT_ID, MobileAlert.AlertType.SYSTEM_ALERT,
                "System Update", "Maintenance window", MobileAlert.AlertPriority.NORMAL);

        // Then
        verify(alertRepository, times(2)).save(any(MobileAlert.class));
    }

    // ====== getAlertPreferences ======

    @Test
    void getAlertPreferences_returnsPreferences() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(preferenceRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                .thenReturn(List.of(alertPreference));
        when(preferenceMapper.toDtoList(List.of(alertPreference))).thenReturn(List.of(alertPreferenceDTO));

        // When
        List<AlertPreferenceDTO> result = mobileAlertService.getAlertPreferences();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isPushEnabled());
    }

    // ====== updateAlertPreference ======

    @Test
    void updateAlertPreference_existingPreference_updatesAndReturns() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(preferenceRepository.findByUserIdAndTenantIdAndAlertType(USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK))
                .thenReturn(Optional.of(alertPreference));
        when(preferenceRepository.save(any(AlertPreference.class))).thenReturn(alertPreference);
        when(preferenceMapper.toDto(alertPreference)).thenReturn(alertPreferenceDTO);

        // When
        AlertPreferenceDTO result = mobileAlertService.updateAlertPreference(
                MobileAlert.AlertType.LOW_STOCK, alertPreferenceDTO);

        // Then
        assertNotNull(result);
        verify(preferenceMapper).updateEntity(alertPreferenceDTO, alertPreference);
        verify(preferenceRepository).save(alertPreference);
    }

    @Test
    void updateAlertPreference_newPreference_createsAndReturns() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(preferenceRepository.findByUserIdAndTenantIdAndAlertType(USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(any(AlertPreference.class))).thenReturn(alertPreference);
        when(preferenceMapper.toDto(any(AlertPreference.class))).thenReturn(alertPreferenceDTO);

        // When
        AlertPreferenceDTO result = mobileAlertService.updateAlertPreference(
                MobileAlert.AlertType.LOW_STOCK, alertPreferenceDTO);

        // Then
        assertNotNull(result);
        verify(preferenceRepository).save(any(AlertPreference.class));
    }

    // ====== initializeDefaultPreferences ======

    @Test
    void initializeDefaultPreferences_createsPreferencesForAllTypes() {
        // Given
        when(preferenceRepository.findByUserIdAndTenantIdAndAlertType(eq(USER_ID), eq(TENANT_ID), any()))
                .thenReturn(Optional.empty());

        // When
        mobileAlertService.initializeDefaultPreferences(USER_ID, TENANT_ID);

        // Then
        verify(preferenceRepository, times(MobileAlert.AlertType.values().length))
                .save(any(AlertPreference.class));
    }

    @Test
    void initializeDefaultPreferences_skipsExistingPreferences() {
        // Given - LOW_STOCK already exists, all others do not
        lenient().when(preferenceRepository.findByUserIdAndTenantIdAndAlertType(
                eq(USER_ID), eq(TENANT_ID), any(MobileAlert.AlertType.class)))
                .thenAnswer(invocation -> {
                    MobileAlert.AlertType type = invocation.getArgument(2);
                    if (type == MobileAlert.AlertType.LOW_STOCK) {
                        return Optional.of(alertPreference);
                    }
                    return Optional.empty();
                });

        // When
        mobileAlertService.initializeDefaultPreferences(USER_ID, TENANT_ID);

        // Then
        verify(preferenceRepository, times(MobileAlert.AlertType.values().length - 1))
                .save(any(AlertPreference.class));
    }

    // ====== createStaffBroadcast ======

    @Test
    void createStaffBroadcast_persistsOneAlertPerActiveStaffMember() {
        when(userRepository.findActiveUserIdsByTenantId(1L)).thenReturn(List.of(10L, 20L, 30L));

        int count = mobileAlertService.createStaffBroadcast(1L, MobileAlert.AlertType.ORDER_PLACED,
                "Янги буюртма", "WO-1", MobileAlert.AlertPriority.HIGH, "WEB_ORDER", 99L);

        assertEquals(3, count);
        org.mockito.ArgumentCaptor<List<MobileAlert>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(alertRepository).saveAll(captor.capture());
        List<MobileAlert> saved = captor.getValue();
        assertEquals(3, saved.size());
        assertEquals(List.of(10L, 20L, 30L), saved.stream().map(MobileAlert::getUserId).toList());
        MobileAlert first = saved.get(0);
        assertEquals(1L, first.getTenantId());
        assertEquals(MobileAlert.AlertType.ORDER_PLACED, first.getAlertType());
        assertEquals(MobileAlert.AlertPriority.HIGH, first.getPriority());
        assertEquals("WEB_ORDER", first.getEntityType());
        assertEquals(99L, first.getEntityId());
        assertFalse(first.isRead());
        // This channel does not push or Telegram — those are delivered separately by the caller.
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void createStaffBroadcast_noActiveStaff_savesEmptyAndReturnsZero() {
        when(userRepository.findActiveUserIdsByTenantId(1L)).thenReturn(List.of());

        int count = mobileAlertService.createStaffBroadcast(1L, MobileAlert.AlertType.ORDER_PLACED,
                "t", "m", MobileAlert.AlertPriority.NORMAL, "WEB_ORDER", 1L);

        assertEquals(0, count);
    }

    // ====== cleanupExpiredAlerts ======

    @Test
    void cleanupExpiredAlerts_deletesExpiredAlerts() {
        // Given
        when(alertRepository.deleteExpiredAlerts(any(Instant.class))).thenReturn(10);

        // When
        mobileAlertService.cleanupExpiredAlerts();

        // Then
        verify(alertRepository).deleteExpiredAlerts(any(Instant.class));
    }
}
