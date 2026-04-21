package com.hisobnoma.platform.admin.service;

import com.hisobnoma.platform.admin.dto.TenantSettingDTO;
import com.hisobnoma.platform.admin.entity.SystemSetting;
import com.hisobnoma.platform.admin.entity.TenantSetting;
import com.hisobnoma.platform.admin.mapper.TenantSettingMapper;
import com.hisobnoma.platform.admin.repository.SystemSettingRepository;
import com.hisobnoma.platform.admin.repository.TenantSettingRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
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
class TenantSettingServiceTest {

    @Mock
    private TenantSettingRepository tenantSettingRepository;
    @Mock
    private SystemSettingRepository systemSettingRepository;
    @Mock
    private TenantSettingMapper tenantSettingMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;
    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private TenantSettingService tenantSettingService;

    private static final Long TENANT_ID = 1L;
    private TenantSetting sampleSetting;
    private TenantSettingDTO sampleDto;

    @BeforeEach
    void setUp() {
        sampleSetting = TenantSetting.builder()
                .settingKey("pos.receipt.header")
                .settingValue("Welcome to our store")
                .description("Receipt header text")
                .category("POS")
                .active(true)
                .build();
        sampleSetting.setTenantId(TENANT_ID);

        sampleDto = TenantSettingDTO.builder()
                .settingKey("pos.receipt.header")
                .settingValue("Welcome to our store")
                .description("Receipt header text")
                .category("POS")
                .tenantId(TENANT_ID)
                .build();
    }

    // ---- getAllSettings ----

    @Test
    void getAllSettings_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findAllActiveByTenantIdOrdered(TENANT_ID))
                .thenReturn(List.of(sampleSetting));
        when(tenantSettingMapper.toDtoList(List.of(sampleSetting)))
                .thenReturn(List.of(sampleDto));

        // When
        List<TenantSettingDTO> result = tenantSettingService.getAllSettings();

        // Then
        assertEquals(1, result.size());
        assertEquals("pos.receipt.header", result.get(0).getSettingKey());
    }

    @Test
    void getAllSettings_noSettings_returnsEmptyList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findAllActiveByTenantIdOrdered(TENANT_ID))
                .thenReturn(Collections.emptyList());
        when(tenantSettingMapper.toDtoList(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When
        List<TenantSettingDTO> result = tenantSettingService.getAllSettings();

        // Then
        assertTrue(result.isEmpty());
    }

    // ---- getSettingsByCategory ----

    @Test
    void getSettingsByCategory_returnsFilteredList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndCategoryAndActiveTrue(TENANT_ID, "POS"))
                .thenReturn(List.of(sampleSetting));
        when(tenantSettingMapper.toDtoList(List.of(sampleSetting)))
                .thenReturn(List.of(sampleDto));

        // When
        List<TenantSettingDTO> result = tenantSettingService.getSettingsByCategory("POS");

        // Then
        assertEquals(1, result.size());
    }

    // ---- getSetting ----

    @Test
    void getSetting_found_returnsDto() {
        // Given
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "pos.receipt.header"))
                .thenReturn(Optional.of(sampleSetting));
        when(tenantSettingMapper.toDto(sampleSetting)).thenReturn(sampleDto);

        // When
        TenantSettingDTO result = tenantSettingService.getSetting(TENANT_ID, "pos.receipt.header");

        // Then
        assertNotNull(result);
        assertEquals("pos.receipt.header", result.getSettingKey());
    }

    @Test
    void getSetting_notFound_throwsNotFoundException() {
        // Given
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "missing"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
                tenantSettingService.getSetting(TENANT_ID, "missing"));
    }

    // ---- getSettingValue (tenant-specific with system fallback) ----

    @Test
    void getSettingValue_tenantOverrideExists_returnsTenantValue() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "pos.receipt.header"))
                .thenReturn(Optional.of(sampleSetting));

        // When
        String result = tenantSettingService.getSettingValue("pos.receipt.header");

        // Then
        assertEquals("Welcome to our store", result);
    }

    @Test
    void getSettingValue_noTenantOverride_fallsBackToSystemSetting() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "app.name"))
                .thenReturn(Optional.empty());
        when(systemSettingService.getSettingValue("app.name")).thenReturn("Hisobnoma");

        // When
        String result = tenantSettingService.getSettingValue("app.name");

        // Then
        assertEquals("Hisobnoma", result);
    }

    @Test
    void getSettingValue_noTenantNoSystem_returnsNull() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "missing"))
                .thenReturn(Optional.empty());
        when(systemSettingService.getSettingValue("missing")).thenReturn(null);

        // When
        String result = tenantSettingService.getSettingValue("missing");

        // Then
        assertNull(result);
    }

    @Test
    void getSettingValue_withDefault_returnsDefault_whenBothMissing() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "missing"))
                .thenReturn(Optional.empty());
        when(systemSettingService.getSettingValue("missing")).thenReturn(null);

        // When
        String result = tenantSettingService.getSettingValue("missing", "default");

        // Then
        assertEquals("default", result);
    }

    // ---- getBooleanSetting ----

    @Test
    void getBooleanSetting_tenantOverrideTrue_returnsTrue() {
        // Given
        TenantSetting boolSetting = TenantSetting.builder()
                .settingKey("feature.x")
                .settingValue("true")
                .build();
        boolSetting.setTenantId(TENANT_ID);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "feature.x"))
                .thenReturn(Optional.of(boolSetting));

        // When
        boolean result = tenantSettingService.getBooleanSetting("feature.x", false);

        // Then
        assertTrue(result);
    }

    @Test
    void getBooleanSetting_noTenantOverride_fallsBackToSystem() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "feature.x"))
                .thenReturn(Optional.empty());
        when(systemSettingService.getBooleanSetting("feature.x", false)).thenReturn(true);

        // When
        boolean result = tenantSettingService.getBooleanSetting("feature.x", false);

        // Then
        assertTrue(result);
    }

    // ---- getIntegerSetting ----

    @Test
    void getIntegerSetting_tenantOverride_returnsTenantValue() {
        // Given
        TenantSetting intSetting = TenantSetting.builder()
                .settingKey("max.items")
                .settingValue("50")
                .build();
        intSetting.setTenantId(TENANT_ID);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "max.items"))
                .thenReturn(Optional.of(intSetting));

        // When
        Integer result = tenantSettingService.getIntegerSetting("max.items", 10);

        // Then
        assertEquals(50, result);
    }

    // ---- getCategories ----

    @Test
    void getCategories_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findDistinctCategoriesByTenantId(TENANT_ID))
                .thenReturn(List.of("POS", "GENERAL"));

        // When
        List<String> result = tenantSettingService.getCategories();

        // Then
        assertEquals(2, result.size());
    }

    // ---- createSetting ----

    @Test
    void createSetting_success_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.existsByTenantIdAndSettingKey(TENANT_ID, "pos.receipt.header"))
                .thenReturn(false);
        when(tenantSettingMapper.toEntity(sampleDto)).thenReturn(sampleSetting);
        when(systemSettingRepository.findBySettingKey("pos.receipt.header"))
                .thenReturn(Optional.empty());
        when(tenantSettingRepository.save(sampleSetting)).thenReturn(sampleSetting);
        when(tenantSettingMapper.toDto(sampleSetting)).thenReturn(sampleDto);

        // When
        TenantSettingDTO result = tenantSettingService.createSetting(sampleDto);

        // Then
        assertNotNull(result);
        verify(tenantSettingRepository).save(any());
    }

    @Test
    void createSetting_linksToSystemSetting_whenExists() {
        // Given
        SystemSetting sysSetting = SystemSetting.builder()
                .settingKey("pos.receipt.header")
                .build();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.existsByTenantIdAndSettingKey(TENANT_ID, "pos.receipt.header"))
                .thenReturn(false);
        when(tenantSettingMapper.toEntity(sampleDto)).thenReturn(sampleSetting);
        when(systemSettingRepository.findBySettingKey("pos.receipt.header"))
                .thenReturn(Optional.of(sysSetting));
        when(tenantSettingRepository.save(sampleSetting)).thenReturn(sampleSetting);
        when(tenantSettingMapper.toDto(sampleSetting)).thenReturn(sampleDto);

        // When
        tenantSettingService.createSetting(sampleDto);

        // Then
        assertEquals(sysSetting, sampleSetting.getSystemSetting());
    }

    @Test
    void createSetting_duplicateKey_throwsDuplicateResourceException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.existsByTenantIdAndSettingKey(TENANT_ID, "pos.receipt.header"))
                .thenReturn(true);

        // When / Then
        assertThrows(DuplicateResourceException.class, () ->
                tenantSettingService.createSetting(sampleDto));
        verify(tenantSettingRepository, never()).save(any());
    }

    // ---- updateSetting ----

    @Test
    void updateSetting_success_returnsUpdatedDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "pos.receipt.header"))
                .thenReturn(Optional.of(sampleSetting));
        when(tenantSettingRepository.save(sampleSetting)).thenReturn(sampleSetting);
        when(tenantSettingMapper.toDto(sampleSetting)).thenReturn(sampleDto);

        // When
        TenantSettingDTO result = tenantSettingService.updateSetting("pos.receipt.header", sampleDto);

        // Then
        assertNotNull(result);
        verify(tenantSettingMapper).updateEntity(eq(sampleDto), eq(sampleSetting));
    }

    @Test
    void updateSetting_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "missing"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
                tenantSettingService.updateSetting("missing", sampleDto));
    }

    // ---- updateSettingValue ----

    @Test
    void updateSettingValue_existingSetting_updatesValue() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "pos.receipt.header"))
                .thenReturn(Optional.of(sampleSetting));
        when(tenantSettingRepository.save(sampleSetting)).thenReturn(sampleSetting);
        when(tenantSettingMapper.toDto(sampleSetting)).thenReturn(sampleDto);

        // When
        TenantSettingDTO result = tenantSettingService.updateSettingValue("pos.receipt.header", "New Header");

        // Then
        assertEquals("New Header", sampleSetting.getSettingValue());
    }

    @Test
    void updateSettingValue_newSetting_createsFromSystemDefaults() {
        // Given
        SystemSetting sysSetting = SystemSetting.builder()
                .settingKey("new.key")
                .description("System desc")
                .category("GENERAL")
                .valueType(SystemSetting.SettingValueType.STRING)
                .sensitive(false)
                .build();

        TenantSetting newSetting = new TenantSetting();
        newSetting.setSettingKey("new.key");
        newSetting.setTenantId(TENANT_ID);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "new.key"))
                .thenReturn(Optional.empty());
        when(systemSettingRepository.findBySettingKey("new.key"))
                .thenReturn(Optional.of(sysSetting));
        when(tenantSettingRepository.save(any(TenantSetting.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tenantSettingMapper.toDto(any(TenantSetting.class))).thenReturn(
                TenantSettingDTO.builder().settingKey("new.key").settingValue("val").build());

        // When
        TenantSettingDTO result = tenantSettingService.updateSettingValue("new.key", "val");

        // Then
        assertNotNull(result);
        verify(tenantSettingRepository).save(any(TenantSetting.class));
    }

    // ---- updateSettings (batch) ----

    @Test
    void updateSettings_updatesMultipleSettings() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(eq(TENANT_ID), anyString()))
                .thenReturn(Optional.of(sampleSetting));
        when(tenantSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantSettingMapper.toDto(any(TenantSetting.class))).thenReturn(sampleDto);

        Map<String, String> updates = Map.of(
                "pos.receipt.header", "Updated",
                "pos.receipt.footer", "Thank you"
        );

        // When
        tenantSettingService.updateSettings(updates);

        // Then
        verify(tenantSettingRepository, times(2)).save(any());
    }

    // ---- deleteSetting ----

    @Test
    void deleteSetting_success_deactivatesSetting() {
        // Given
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "pos.receipt.header"))
                .thenReturn(Optional.of(sampleSetting));

        // When
        tenantSettingService.deleteSetting(TENANT_ID, "pos.receipt.header");

        // Then
        assertFalse(sampleSetting.isActive());
        verify(tenantSettingRepository).save(sampleSetting);
    }

    @Test
    void deleteSetting_notFound_throwsNotFoundException() {
        // Given
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, "missing"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
                tenantSettingService.deleteSetting(TENANT_ID, "missing"));
    }

    // ---- getSettingsAsMap ----

    @Test
    void getSettingsAsMap_returnsKeyValueMap() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndActiveTrue(TENANT_ID))
                .thenReturn(List.of(sampleSetting));

        // When
        Map<String, String> result = tenantSettingService.getSettingsAsMap();

        // Then
        assertEquals(1, result.size());
        assertEquals("Welcome to our store", result.get("pos.receipt.header"));
    }

    @Test
    void getSettingsAsMap_nullValue_returnsEmptyString() {
        // Given
        TenantSetting nullSetting = TenantSetting.builder()
                .settingKey("empty.key")
                .settingValue(null)
                .build();
        nullSetting.setTenantId(TENANT_ID);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndActiveTrue(TENANT_ID))
                .thenReturn(List.of(nullSetting));

        // When
        Map<String, String> result = tenantSettingService.getSettingsAsMap();

        // Then
        assertEquals("", result.get("empty.key"));
    }
}
