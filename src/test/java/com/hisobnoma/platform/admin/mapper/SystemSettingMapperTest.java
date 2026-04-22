package com.hisobnoma.platform.admin.mapper;

import com.hisobnoma.platform.admin.dto.SystemSettingDTO;
import com.hisobnoma.platform.admin.entity.SystemSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class SystemSettingMapperTest {

    private SystemSettingMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(SystemSettingMapper.class);
    }

    @Test
    void toDto_allFields_mappedCorrectly() {
        // Given
        SystemSetting entity = SystemSetting.builder()
                .settingKey("session.timeout")
                .settingValue("30")
                .defaultValue("60")
                .description("Session timeout in minutes")
                .category("SECURITY")
                .valueType(SystemSetting.SettingValueType.INTEGER)
                .sensitive(false)
                .readonly(true)
                .active(true)
                .validationRegex("\\d+")
                .minValue(1L)
                .maxValue(120L)
                .allowedValues(null)
                .sortOrder(1)
                .build();
        entity.setId(5L);

        // When
        SystemSettingDTO dto = mapper.toDto(entity);

        // Then
        assertEquals(5L, dto.getId());
        assertEquals("session.timeout", dto.getSettingKey());
        assertEquals("30", dto.getSettingValue());
        assertEquals("60", dto.getDefaultValue());
        assertEquals("Session timeout in minutes", dto.getDescription());
        assertEquals("SECURITY", dto.getCategory());
        assertEquals(SystemSetting.SettingValueType.INTEGER, dto.getValueType());
        assertFalse(dto.isSensitive());
        assertTrue(dto.isReadonly());
        assertTrue(dto.isActive());
        assertEquals("\\d+", dto.getValidationRegex());
        assertEquals(1L, dto.getMinValue());
        assertEquals(120L, dto.getMaxValue());
        assertNull(dto.getAllowedValues());
        assertEquals(1, dto.getSortOrder());
    }

    @Test
    void toDto_nullDescription_handledGracefully() {
        // Given
        SystemSetting entity = SystemSetting.builder()
                .settingKey("app.name")
                .settingValue("Hisobnoma")
                .description(null)
                .category("GENERAL")
                .sensitive(false)
                .build();

        // When
        SystemSettingDTO dto = mapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertNull(dto.getDescription());
        assertEquals("app.name", dto.getSettingKey());
    }

    @Test
    void fromDto_allFields_entityCreatedCorrectly() {
        // Given
        SystemSettingDTO dto = SystemSettingDTO.builder()
                .settingKey("max.retries")
                .settingValue("5")
                .defaultValue("3")
                .description("Maximum retry attempts")
                .category("GENERAL")
                .valueType(SystemSetting.SettingValueType.INTEGER)
                .sensitive(false)
                .readonly(false)
                .active(true)
                .validationRegex("\\d+")
                .minValue(1L)
                .maxValue(10L)
                .sortOrder(2)
                .build();

        // When
        SystemSetting entity = mapper.toEntity(dto);

        // Then
        assertEquals("max.retries", entity.getSettingKey());
        assertEquals("5", entity.getSettingValue());
        assertEquals("3", entity.getDefaultValue());
        assertEquals("Maximum retry attempts", entity.getDescription());
        assertEquals("GENERAL", entity.getCategory());
        assertEquals(SystemSetting.SettingValueType.INTEGER, entity.getValueType());
        assertFalse(entity.isSensitive());
        assertFalse(entity.isReadonly());
        assertTrue(entity.isActive());
    }

    @Test
    void fromDto_idNotCopied_entityHasNoId() {
        // Given — DTO has an id set, but toEntity ignores it (creation scenario)
        SystemSettingDTO dto = SystemSettingDTO.builder()
                .id(99L)
                .settingKey("new.setting")
                .settingValue("value")
                .category("GENERAL")
                .build();

        // When
        SystemSetting entity = mapper.toEntity(dto);

        // Then
        assertNull(entity.getId());
        assertEquals("new.setting", entity.getSettingKey());
    }
}
