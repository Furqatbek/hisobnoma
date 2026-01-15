package com.hisobnoma.platform.reports.mapper;

import com.hisobnoma.platform.reports.dto.ReportDefinitionDTO;
import com.hisobnoma.platform.reports.entity.ReportDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReportDefinitionMapper {

    ReportDefinitionDTO toDto(ReportDefinition entity);

    List<ReportDefinitionDTO> toDtoList(List<ReportDefinition> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "queryClass", ignore = true)
    @Mapping(target = "templatePath", ignore = true)
    ReportDefinition toEntity(ReportDefinitionDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "queryClass", ignore = true)
    @Mapping(target = "templatePath", ignore = true)
    @Mapping(target = "code", ignore = true)
    void updateEntity(ReportDefinitionDTO dto, @MappingTarget ReportDefinition entity);
}
