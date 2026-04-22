package com.hisobnoma.platform.admin.mapper;

import com.hisobnoma.platform.admin.dto.TenantSettingDTO;
import com.hisobnoma.platform.admin.entity.SystemSetting;
import com.hisobnoma.platform.admin.entity.TenantSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class TenantSettingMapperTest {

    private TenantSettingMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(TenantSettingMapper.class);
    }

    @Test
    void toDto_allFields_mappedCorrectly() {
        // Given
        TenantSetting entity = TenantSetting.builder()
                .settingKey("pos.receipt.header")
                .settingValue("Welcome to our store")
                .description("Receipt header text")
                .category("POS")
                .valueType(SystemSetting.SettingValueType.STRING)
                .sensitive(false)
                .active(true)
                .build();
        entity.setId(5L);
        entity.setTenantId(1L);

        // When
        TenantSettingDTO dto = mapper.toDto(entity);

        // Then
        assertEquals(5L, dto.getId());
        assertEquals(1L, dto.getTenantId());
        assertEquals("pos.receipt.header", dto.getSettingKey());
        assertEquals("Welcome to our store", dto.getSettingValue());
        assertEquals("Receipt header text", dto.getDescription());
        assertEquals("POS", dto.getCategory());
        assertEquals(SystemSetting.SettingValueType.STRING, dto.getValueType());
        assertFalse(dto.isSensitive());
        assertTrue(dto.isActive());
    }

    @Test
    void toDto_nullValue_handledGracefully() {
        // Given
        TenantSetting entity = TenantSetting.builder()
                .settingKey("empty.key")
                .settingValue(null)
                .description(null)
                .category("GENERAL")
                .sensitive(false)
                .build();
        entity.setTenantId(1L);

        // When
        TenantSettingDTO dto = mapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertNull(dto.getSettingValue());
        assertNull(dto.getDescription());
    }

    @Test
    void fromDto_allFields_entityCreatedCorrectly() {
        // Given
        TenantSettingDTO dto = TenantSettingDTO.builder()
                .tenantId(1L)
                .settingKey("pos.receipt.footer")
                .settingValue("Thank you for shopping!")
                .description("Receipt footer text")
                .category("POS")
                .valueType(SystemSetting.SettingValueType.STRING)
                .sensitive(false)
                .active(true)
                .build();

        // When
        TenantSetting entity = mapper.toEntity(dto);

        // Then
        assertEquals(1L, entity.getTenantId());
        assertEquals("pos.receipt.footer", entity.getSettingKey());
        assertEquals("Thank you for shopping!", entity.getSettingValue());
        assertEquals("Receipt footer text", entity.getDescription());
        assertEquals("POS", entity.getCategory());
        assertEquals(SystemSetting.SettingValueType.STRING, entity.getValueType());
        assertFalse(entity.isSensitive());
        assertTrue(entity.isActive());
    }

    @Test
    void fromDto_tenantIdPreserved() {
        // Given
        TenantSettingDTO dto = TenantSettingDTO.builder()
                .tenantId(42L)
                .settingKey("test.key")
                .settingValue("value")
                .category("GENERAL")
                .build();

        // When
        TenantSetting entity = mapper.toEntity(dto);

        // Then
        assertEquals(42L, entity.getTenantId());
    }
}
