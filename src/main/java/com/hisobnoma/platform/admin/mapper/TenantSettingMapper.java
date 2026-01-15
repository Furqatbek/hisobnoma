package com.hisobnoma.platform.admin.mapper;

import com.hisobnoma.platform.admin.dto.TenantSettingDTO;
import com.hisobnoma.platform.admin.entity.TenantSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TenantSettingMapper {

    @Mapping(target = "settingValue", expression = "java(entity.isSensitive() ? \"********\" : entity.getSettingValue())")
    @Mapping(target = "systemSettingId", source = "systemSetting.id")
    TenantSettingDTO toDto(TenantSetting entity);

    List<TenantSettingDTO> toDtoList(List<TenantSetting> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "systemSetting", ignore = true)
    TenantSetting toEntity(TenantSettingDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "settingKey", ignore = true)
    @Mapping(target = "systemSetting", ignore = true)
    void updateEntity(TenantSettingDTO dto, @MappingTarget TenantSetting entity);
}
