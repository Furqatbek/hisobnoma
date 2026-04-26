package com.hisobnoma.platform.mobile.mapper;

import com.hisobnoma.platform.mobile.dto.AlertPreferenceDTO;
import com.hisobnoma.platform.mobile.entity.AlertPreference;
import com.hisobnoma.platform.mobile.entity.MobileAlert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AlertPreferenceMapperTest {

    @Autowired
    private AlertPreferenceMapper alertPreferenceMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        AlertPreference entity = AlertPreference.builder()
                .id(1L)
                .tenantId(1L)
                .userId(10L)
                .alertType(MobileAlert.AlertType.LOW_STOCK)
                .pushEnabled(true)
                .inAppEnabled(true)
                .emailEnabled(false)
                .smsEnabled(false)
                .telegramEnabled(true)
                .thresholdValue(10)
                .quietHoursStart(22)
                .quietHoursEnd(7)
                .build();

        // When
        AlertPreferenceDTO dto = alertPreferenceMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(MobileAlert.AlertType.LOW_STOCK, dto.getAlertType());
        assertTrue(dto.isPushEnabled());
        assertTrue(dto.isInAppEnabled());
        assertFalse(dto.isEmailEnabled());
        assertFalse(dto.isSmsEnabled());
        assertTrue(dto.isTelegramEnabled());
        assertEquals(10, dto.getThresholdValue());
        assertEquals(22, dto.getQuietHoursStart());
        assertEquals(7, dto.getQuietHoursEnd());
    }

    @Test
    void toEntity_fromDto_mapsBasicFields() {
        // Given
        AlertPreferenceDTO dto = AlertPreferenceDTO.builder()
                .alertType(MobileAlert.AlertType.PAYMENT_OVERDUE)
                .pushEnabled(true)
                .inAppEnabled(true)
                .emailEnabled(true)
                .smsEnabled(false)
                .telegramEnabled(false)
                .thresholdValue(5)
                .quietHoursStart(23)
                .quietHoursEnd(6)
                .build();

        // When
        AlertPreference entity = alertPreferenceMapper.toEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals(MobileAlert.AlertType.PAYMENT_OVERDUE, entity.getAlertType());
        assertTrue(entity.isPushEnabled());
        assertTrue(entity.isInAppEnabled());
        assertTrue(entity.isEmailEnabled());
        assertFalse(entity.isSmsEnabled());
        assertFalse(entity.isTelegramEnabled());
        assertEquals(5, entity.getThresholdValue());
        assertEquals(23, entity.getQuietHoursStart());
        assertEquals(6, entity.getQuietHoursEnd());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getUserId());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        AlertPreference existing = AlertPreference.builder()
                .id(1L)
                .tenantId(1L)
                .userId(10L)
                .alertType(MobileAlert.AlertType.LOW_STOCK)
                .pushEnabled(true)
                .inAppEnabled(true)
                .emailEnabled(false)
                .smsEnabled(false)
                .telegramEnabled(false)
                .thresholdValue(10)
                .build();

        AlertPreferenceDTO dto = AlertPreferenceDTO.builder()
                .alertType(MobileAlert.AlertType.LOW_STOCK)
                .pushEnabled(false)
                .inAppEnabled(true)
                .emailEnabled(true)
                .smsEnabled(true)
                .telegramEnabled(true)
                .thresholdValue(20)
                .quietHoursStart(21)
                .quietHoursEnd(8)
                .build();

        // When
        alertPreferenceMapper.updateEntity(dto, existing);

        // Then
        assertFalse(existing.isPushEnabled());
        assertTrue(existing.isInAppEnabled());
        assertTrue(existing.isEmailEnabled());
        assertTrue(existing.isSmsEnabled());
        assertTrue(existing.isTelegramEnabled());
        assertEquals(20, existing.getThresholdValue());
        assertEquals(21, existing.getQuietHoursStart());
        assertEquals(8, existing.getQuietHoursEnd());
        // ID, tenantId, userId, alertType should remain unchanged
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
        assertEquals(10L, existing.getUserId());
        assertEquals(MobileAlert.AlertType.LOW_STOCK, existing.getAlertType());
    }

    @Test
    void toDtoList_mapsAllPreferences() {
        // Given
        AlertPreference p1 = AlertPreference.builder()
                .id(1L).alertType(MobileAlert.AlertType.LOW_STOCK)
                .pushEnabled(true).inAppEnabled(true).build();
        AlertPreference p2 = AlertPreference.builder()
                .id(2L).alertType(MobileAlert.AlertType.SYSTEM_ALERT)
                .pushEnabled(false).inAppEnabled(true).build();

        // When
        List<AlertPreferenceDTO> dtos = alertPreferenceMapper.toDtoList(List.of(p1, p2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
