package com.hisobnoma.platform.admin.service;

import com.hisobnoma.platform.admin.dto.SystemSettingDTO;
import com.hisobnoma.platform.admin.entity.SystemSetting;
import com.hisobnoma.platform.admin.mapper.SystemSettingMapper;
import com.hisobnoma.platform.admin.repository.SystemSettingRepository;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;
    @Mock
    private SystemSettingMapper systemSettingMapper;

    @InjectMocks
    private SystemSettingService systemSettingService;

    private SystemSetting sampleSetting;
    private SystemSettingDTO sampleDto;

    @BeforeEach
    void setUp() {
        sampleSetting = SystemSetting.builder()
                .settingKey("app.name")
                .settingValue("Hisobnoma")
                .defaultValue("Platform")
                .description("Application name")
                .category("GENERAL")
                .valueType(SystemSetting.SettingValueType.STRING)
                .readonly(false)
                .active(true)
                .build();

        sampleDto = SystemSettingDTO.builder()
                .settingKey("app.name")
                .settingValue("Hisobnoma")
                .defaultValue("Platform")
                .description("Application name")
                .category("GENERAL")
                .build();
    }

    // ---- getAllSettings ----

    @Test
    void getAllSettings_returnsList() {
        // Given
        when(systemSettingRepository.findAllActiveOrderedByCategoryAndSortOrder())
                .thenReturn(List.of(sampleSetting));
        when(systemSettingMapper.toDtoList(List.of(sampleSetting)))
                .thenReturn(List.of(sampleDto));

        // When
        List<SystemSettingDTO> result = systemSettingService.getAllSettings();

        // Then
        assertEquals(1, result.size());
        assertEquals("app.name", result.get(0).getSettingKey());
    }

    @Test
    void getAllSettings_noSettings_returnsEmptyList() {
        // Given
        when(systemSettingRepository.findAllActiveOrderedByCategoryAndSortOrder())
                .thenReturn(Collections.emptyList());
        when(systemSettingMapper.toDtoList(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When
        List<SystemSettingDTO> result = systemSettingService.getAllSettings();

        // Then
        assertTrue(result.isEmpty());
    }

    // ---- getSettingsByCategory ----

    @Test
    void getSettingsByCategory_returnsFilteredList() {
        // Given
        when(systemSettingRepository.findByCategoryAndActiveTrue("GENERAL"))
                .thenReturn(List.of(sampleSetting));
        when(systemSettingMapper.toDtoList(List.of(sampleSetting)))
                .thenReturn(List.of(sampleDto));

        // When
        List<SystemSettingDTO> result = systemSettingService.getSettingsByCategory("GENERAL");

        // Then
        assertEquals(1, result.size());
    }

    // ---- getSetting ----

    @Test
    void getSetting_found_returnsDto() {
        // Given
        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));
        when(systemSettingMapper.toDto(sampleSetting)).thenReturn(sampleDto);

        // When
        SystemSettingDTO result = systemSettingService.getSetting("app.name");

        // Then
        assertNotNull(result);
        assertEquals("app.name", result.getSettingKey());
    }

    @Test
    void getSetting_notFound_throwsNotFoundException() {
        // Given
        when(systemSettingRepository.findBySettingKey("nonexistent"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
                systemSettingService.getSetting("nonexistent"));
    }

    // ---- getSettingValue ----

    @Test
    void getSettingValue_found_returnsValue() {
        // Given
        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));

        // When
        String result = systemSettingService.getSettingValue("app.name");

        // Then
        assertEquals("Hisobnoma", result);
    }

    @Test
    void getSettingValue_notFound_returnsNull() {
        // Given
        when(systemSettingRepository.findBySettingKey("missing"))
                .thenReturn(Optional.empty());

        // When
        String result = systemSettingService.getSettingValue("missing");

        // Then
        assertNull(result);
    }

    @Test
    void getSettingValue_withDefault_returnsDefault_whenNotFound() {
        // Given
        when(systemSettingRepository.findBySettingKey("missing"))
                .thenReturn(Optional.empty());

        // When
        String result = systemSettingService.getSettingValue("missing", "fallback");

        // Then
        assertEquals("fallback", result);
    }

    @Test
    void getSettingValue_withDefault_returnsActual_whenFound() {
        // Given
        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));

        // When
        String result = systemSettingService.getSettingValue("app.name", "fallback");

        // Then
        assertEquals("Hisobnoma", result);
    }

    // ---- getBooleanSetting ----

    @Test
    void getBooleanSetting_trueValue_returnsTrue() {
        // Given
        SystemSetting boolSetting = SystemSetting.builder()
                .settingKey("feature.enabled")
                .settingValue("true")
                .build();
        when(systemSettingRepository.findBySettingKey("feature.enabled"))
                .thenReturn(Optional.of(boolSetting));

        // When
        boolean result = systemSettingService.getBooleanSetting("feature.enabled", false);

        // Then
        assertTrue(result);
    }

    @Test
    void getBooleanSetting_notFound_returnsDefault() {
        // Given
        when(systemSettingRepository.findBySettingKey("missing"))
                .thenReturn(Optional.empty());

        // When
        boolean result = systemSettingService.getBooleanSetting("missing", true);

        // Then
        assertTrue(result);
    }

    // ---- getIntegerSetting ----

    @Test
    void getIntegerSetting_found_returnsInteger() {
        // Given
        SystemSetting intSetting = SystemSetting.builder()
                .settingKey("max.retries")
                .settingValue("5")
                .build();
        when(systemSettingRepository.findBySettingKey("max.retries"))
                .thenReturn(Optional.of(intSetting));

        // When
        Integer result = systemSettingService.getIntegerSetting("max.retries", 3);

        // Then
        assertEquals(5, result);
    }

    @Test
    void getIntegerSetting_notFound_returnsDefault() {
        // Given
        when(systemSettingRepository.findBySettingKey("missing"))
                .thenReturn(Optional.empty());

        // When
        Integer result = systemSettingService.getIntegerSetting("missing", 10);

        // Then
        assertEquals(10, result);
    }

    // ---- getCategories ----

    @Test
    void getCategories_returnsList() {
        // Given
        when(systemSettingRepository.findDistinctCategories())
                .thenReturn(List.of("GENERAL", "SECURITY"));

        // When
        List<String> result = systemSettingService.getCategories();

        // Then
        assertEquals(2, result.size());
    }

    // ---- createSetting ----

    @Test
    void createSetting_success_returnsDto() {
        // Given
        when(systemSettingRepository.existsBySettingKey("app.name")).thenReturn(false);
        when(systemSettingMapper.toEntity(sampleDto)).thenReturn(sampleSetting);
        when(systemSettingRepository.save(sampleSetting)).thenReturn(sampleSetting);
        when(systemSettingMapper.toDto(sampleSetting)).thenReturn(sampleDto);

        // When
        SystemSettingDTO result = systemSettingService.createSetting(sampleDto);

        // Then
        assertNotNull(result);
        assertEquals("app.name", result.getSettingKey());
        verify(systemSettingRepository).save(any());
    }

    @Test
    void createSetting_duplicateKey_throwsDuplicateResourceException() {
        // Given
        when(systemSettingRepository.existsBySettingKey("app.name")).thenReturn(true);

        // When / Then
        assertThrows(DuplicateResourceException.class, () ->
                systemSettingService.createSetting(sampleDto));
        verify(systemSettingRepository, never()).save(any());
    }

    // ---- updateSetting ----

    @Test
    void updateSetting_success_returnsUpdatedDto() {
        // Given
        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));
        when(systemSettingRepository.save(sampleSetting)).thenReturn(sampleSetting);
        when(systemSettingMapper.toDto(sampleSetting)).thenReturn(sampleDto);

        // When
        SystemSettingDTO result = systemSettingService.updateSetting("app.name", sampleDto);

        // Then
        assertNotNull(result);
        verify(systemSettingMapper).updateEntity(eq(sampleDto), eq(sampleSetting));
    }

    @Test
    void updateSetting_notFound_throwsNotFoundException() {
        // Given
        when(systemSettingRepository.findBySettingKey("missing"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
                systemSettingService.updateSetting("missing", sampleDto));
    }

    @Test
    void updateSetting_readonlySetting_throwsBusinessException() {
        // Given
        sampleSetting.setReadonly(true);
        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));

        // When / Then
        assertThrows(BusinessException.class, () ->
                systemSettingService.updateSetting("app.name", sampleDto));
        verify(systemSettingRepository, never()).save(any());
    }

    // ---- updateSettingValue ----

    @Test
    void updateSettingValue_success_updatesValue() {
        // Given
        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));
        when(systemSettingRepository.save(sampleSetting)).thenReturn(sampleSetting);
        when(systemSettingMapper.toDto(sampleSetting)).thenReturn(sampleDto);

        // When
        SystemSettingDTO result = systemSettingService.updateSettingValue("app.name", "NewName");

        // Then
        assertNotNull(result);
        assertEquals("NewName", sampleSetting.getSettingValue());
    }

    @Test
    void updateSettingValue_readonlySetting_throwsBusinessException() {
        // Given
        sampleSetting.setReadonly(true);
        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));

        // When / Then
        assertThrows(BusinessException.class, () ->
                systemSettingService.updateSettingValue("app.name", "NewName"));
    }

    // ---- updateSettings (batch) ----

    @Test
    void updateSettings_updatesOnlyWritableSettings() {
        // Given
        SystemSetting readonlySetting = SystemSetting.builder()
                .settingKey("readonly.key")
                .settingValue("old")
                .readonly(true)
                .build();

        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));
        when(systemSettingRepository.findBySettingKey("readonly.key"))
                .thenReturn(Optional.of(readonlySetting));

        Map<String, String> updates = Map.of(
                "app.name", "Updated",
                "readonly.key", "ShouldNotChange"
        );

        // When
        systemSettingService.updateSettings(updates);

        // Then
        assertEquals("Updated", sampleSetting.getSettingValue());
        assertEquals("old", readonlySetting.getSettingValue());
    }

    // ---- deleteSetting ----

    @Test
    void deleteSetting_success_deactivatesSetting() {
        // Given
        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));

        // When
        systemSettingService.deleteSetting("app.name");

        // Then
        assertFalse(sampleSetting.isActive());
        verify(systemSettingRepository).save(sampleSetting);
    }

    @Test
    void deleteSetting_notFound_throwsNotFoundException() {
        // Given
        when(systemSettingRepository.findBySettingKey("missing"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
                systemSettingService.deleteSetting("missing"));
    }

    @Test
    void deleteSetting_readonlySetting_throwsBusinessException() {
        // Given
        sampleSetting.setReadonly(true);
        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));

        // When / Then
        assertThrows(BusinessException.class, () ->
                systemSettingService.deleteSetting("app.name"));
    }

    // ---- getSettingsAsMap ----

    @Test
    void getSettingsAsMap_returnsKeyValueMap() {
        // Given
        when(systemSettingRepository.findByActiveTrue())
                .thenReturn(List.of(sampleSetting));

        // When
        Map<String, String> result = systemSettingService.getSettingsAsMap();

        // Then
        assertEquals(1, result.size());
        assertEquals("Hisobnoma", result.get("app.name"));
    }

    @Test
    void getSettingsAsMap_nullValue_returnsEmptyString() {
        // Given
        SystemSetting nullValueSetting = SystemSetting.builder()
                .settingKey("empty.key")
                .settingValue(null)
                .defaultValue(null)
                .build();
        when(systemSettingRepository.findByActiveTrue())
                .thenReturn(List.of(nullValueSetting));

        // When
        Map<String, String> result = systemSettingService.getSettingsAsMap();

        // Then
        assertEquals("", result.get("empty.key"));
    }
}
