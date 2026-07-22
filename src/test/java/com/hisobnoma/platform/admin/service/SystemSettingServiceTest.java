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
import org.mockito.ArgumentCaptor;
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

    @Test
    void getAllSettings_returnsCorrectCount() {
        // Given — exactly 5 settings
        SystemSetting s1 = SystemSetting.builder().settingKey("k1").category("A").active(true).build();
        SystemSetting s2 = SystemSetting.builder().settingKey("k2").category("A").active(true).build();
        SystemSetting s3 = SystemSetting.builder().settingKey("k3").category("B").active(true).build();
        SystemSetting s4 = SystemSetting.builder().settingKey("k4").category("B").active(true).build();
        SystemSetting s5 = SystemSetting.builder().settingKey("k5").category("C").active(true).build();
        List<SystemSetting> settings = List.of(s1, s2, s3, s4, s5);
        List<SystemSettingDTO> dtos = List.of(
                SystemSettingDTO.builder().settingKey("k1").build(),
                SystemSettingDTO.builder().settingKey("k2").build(),
                SystemSettingDTO.builder().settingKey("k3").build(),
                SystemSettingDTO.builder().settingKey("k4").build(),
                SystemSettingDTO.builder().settingKey("k5").build()
        );
        when(systemSettingRepository.findAllActiveOrderedByCategoryAndSortOrder()).thenReturn(settings);
        when(systemSettingMapper.toDtoList(settings)).thenReturn(dtos);

        // When
        List<SystemSettingDTO> result = systemSettingService.getAllSettings();

        // Then
        assertEquals(5, result.size());
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

    @Test
    void getSettingsByCategory_nonExistent_returnsEmptyList() {
        // Given
        when(systemSettingRepository.findByCategoryAndActiveTrue("NONEXISTENT"))
                .thenReturn(Collections.emptyList());
        when(systemSettingMapper.toDtoList(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When
        List<SystemSettingDTO> result = systemSettingService.getSettingsByCategory("NONEXISTENT");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getSettingsByCategory_caseSensitive_noMatch() {
        // Given — only "SECURITY" uppercase exists, querying lowercase
        when(systemSettingRepository.findByCategoryAndActiveTrue("security"))
                .thenReturn(Collections.emptyList());
        when(systemSettingMapper.toDtoList(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When
        List<SystemSettingDTO> result = systemSettingService.getSettingsByCategory("security");

        // Then
        assertTrue(result.isEmpty());
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

    @Test
    void getSetting_nullKey_throwsException() {
        // Given
        when(systemSettingRepository.findBySettingKey(null))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
                systemSettingService.getSetting(null));
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

    @Test
    void getCategories_noSettings_returnsEmptyList() {
        // Given
        when(systemSettingRepository.findDistinctCategories())
                .thenReturn(Collections.emptyList());

        // When
        List<String> result = systemSettingService.getCategories();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getCategories_singleCategory_returnsSingleEntry() {
        // Given
        when(systemSettingRepository.findDistinctCategories())
                .thenReturn(List.of("GENERAL"));

        // When
        List<String> result = systemSettingService.getCategories();

        // Then
        assertEquals(1, result.size());
        assertEquals("GENERAL", result.get(0));
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

    @Test
    void createSetting_nullKey_throwsException() {
        // Given — DTO with null key
        SystemSettingDTO nullKeyDto = SystemSettingDTO.builder()
                .settingKey(null)
                .settingValue("value")
                .build();
        when(systemSettingRepository.existsBySettingKey(null)).thenReturn(false);
        when(systemSettingMapper.toEntity(nullKeyDto)).thenReturn(
                SystemSetting.builder().settingKey(null).settingValue("value").build());
        when(systemSettingRepository.save(any())).thenThrow(new RuntimeException("Constraint violation"));

        // When / Then
        assertThrows(RuntimeException.class, () ->
                systemSettingService.createSetting(nullKeyDto));
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

    @Test
    void updateSetting_sameValue_stillSucceeds() {
        // Given — DTO value is the same as existing
        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));
        when(systemSettingRepository.save(sampleSetting)).thenReturn(sampleSetting);
        when(systemSettingMapper.toDto(sampleSetting)).thenReturn(sampleDto);

        // When
        SystemSettingDTO result = systemSettingService.updateSetting("app.name", sampleDto);

        // Then
        assertNotNull(result);
        verify(systemSettingMapper).updateEntity(eq(sampleDto), eq(sampleSetting));
        verify(systemSettingRepository).save(sampleSetting);
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

    @Test
    void updateSettingValue_nonExistentKey_throwsNotFoundException() {
        // Given
        when(systemSettingRepository.findBySettingKey("nonexistent.key"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
                systemSettingService.updateSettingValue("nonexistent.key", "60"));
    }

    @Test
    void updateSettingValue_emptyValue_successfullySetToEmpty() {
        // Given
        when(systemSettingRepository.findBySettingKey("app.name"))
                .thenReturn(Optional.of(sampleSetting));
        when(systemSettingRepository.save(sampleSetting)).thenReturn(sampleSetting);
        when(systemSettingMapper.toDto(sampleSetting)).thenReturn(sampleDto);

        // When
        SystemSettingDTO result = systemSettingService.updateSettingValue("app.name", "");

        // Then
        assertNotNull(result);
        assertEquals("", sampleSetting.getSettingValue());
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

    @Test
    void updateSettings_mapOfThreeKeys_allThreeUpdated() {
        // Given
        SystemSetting s1 = SystemSetting.builder().settingKey("k1").settingValue("old1").readonly(false).build();
        SystemSetting s2 = SystemSetting.builder().settingKey("k2").settingValue("old2").readonly(false).build();
        SystemSetting s3 = SystemSetting.builder().settingKey("k3").settingValue("old3").readonly(false).build();
        when(systemSettingRepository.findBySettingKey("k1")).thenReturn(Optional.of(s1));
        when(systemSettingRepository.findBySettingKey("k2")).thenReturn(Optional.of(s2));
        when(systemSettingRepository.findBySettingKey("k3")).thenReturn(Optional.of(s3));

        Map<String, String> updates = Map.of("k1", "new1", "k2", "new2", "k3", "new3");

        // When
        systemSettingService.updateSettings(updates);

        // Then
        assertEquals("new1", s1.getSettingValue());
        assertEquals("new2", s2.getSettingValue());
        assertEquals("new3", s3.getSettingValue());
        verify(systemSettingRepository, times(3)).save(any());
    }

    @Test
    void updateSettings_emptyMap_noUpdatesPerformed() {
        // Given
        Map<String, String> updates = Collections.emptyMap();

        // When
        systemSettingService.updateSettings(updates);

        // Then
        verify(systemSettingRepository, never()).findBySettingKey(any());
        verify(systemSettingRepository, never()).save(any());
    }

    @Test
    void updateSettings_missingKey_isCreatedNotSkipped() {
        // Upsert contract: a missing key must not make the save a silent no-op
        SystemSetting existing = SystemSetting.builder().settingKey("k1").settingValue("old").readonly(false).build();
        when(systemSettingRepository.findBySettingKey("k1")).thenReturn(Optional.of(existing));
        when(systemSettingRepository.findBySettingKey("missing.key")).thenReturn(Optional.empty());

        Map<String, String> updates = Map.of("k1", "new1", "missing.key", "value");

        // When
        systemSettingService.updateSettings(updates);

        // Then
        assertEquals("new1", existing.getSettingValue());
        ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
        verify(systemSettingRepository, times(2)).save(captor.capture());
        SystemSetting created = captor.getAllValues().stream()
                .filter(s -> "missing.key".equals(s.getSettingKey()))
                .findFirst().orElseThrow();
        assertEquals("value", created.getSettingValue());
        assertEquals("MISSING", created.getCategory());
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

    @Test
    void deleteSetting_alreadyDeleted_throwsNotFoundException() {
        // Given — key was previously deleted (not found in repository)
        when(systemSettingRepository.findBySettingKey("session.timeout"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
                systemSettingService.deleteSetting("session.timeout"));
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
