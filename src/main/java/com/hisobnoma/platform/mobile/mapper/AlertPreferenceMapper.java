package com.hisobnoma.platform.mobile.mapper;

import com.hisobnoma.platform.mobile.dto.AlertPreferenceDTO;
import com.hisobnoma.platform.mobile.entity.AlertPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AlertPreferenceMapper {

    AlertPreferenceDTO toDto(AlertPreference entity);

    List<AlertPreferenceDTO> toDtoList(List<AlertPreference> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    AlertPreference toEntity(AlertPreferenceDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "alertType", ignore = true)
    void updateEntity(AlertPreferenceDTO dto, @MappingTarget AlertPreference entity);
}
