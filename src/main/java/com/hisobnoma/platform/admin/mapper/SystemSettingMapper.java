package com.hisobnoma.platform.admin.mapper;

import com.hisobnoma.platform.admin.dto.SystemSettingDTO;
import com.hisobnoma.platform.admin.entity.SystemSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SystemSettingMapper {

    @Mapping(target = "settingValue", expression = "java(entity.isSensitive() ? \"********\" : entity.getSettingValue())")
    SystemSettingDTO toDto(SystemSetting entity);

    List<SystemSettingDTO> toDtoList(List<SystemSetting> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    SystemSetting toEntity(SystemSettingDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "settingKey", ignore = true)
    void updateEntity(SystemSettingDTO dto, @MappingTarget SystemSetting entity);
}
