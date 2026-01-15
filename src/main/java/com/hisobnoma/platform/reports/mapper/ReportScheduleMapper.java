package com.hisobnoma.platform.reports.mapper;

import com.hisobnoma.platform.reports.dto.ReportScheduleDTO;
import com.hisobnoma.platform.reports.entity.ReportSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReportScheduleMapper {

    @Mapping(target = "reportDefinitionId", source = "reportDefinition.id")
    @Mapping(target = "reportCode", source = "reportDefinition.code")
    @Mapping(target = "reportName", source = "reportDefinition.name")
    ReportScheduleDTO toDto(ReportSchedule entity);

    List<ReportScheduleDTO> toDtoList(List<ReportSchedule> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "reportDefinition", ignore = true)
    @Mapping(target = "lastRunAt", ignore = true)
    @Mapping(target = "nextRunAt", ignore = true)
    @Mapping(target = "lastRunStatus", ignore = true)
    @Mapping(target = "createdByUserId", ignore = true)
    @Mapping(target = "webhookUrl", ignore = true)
    ReportSchedule toEntity(ReportScheduleDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "reportDefinition", ignore = true)
    @Mapping(target = "lastRunAt", ignore = true)
    @Mapping(target = "nextRunAt", ignore = true)
    @Mapping(target = "lastRunStatus", ignore = true)
    @Mapping(target = "createdByUserId", ignore = true)
    @Mapping(target = "webhookUrl", ignore = true)
    void updateEntity(ReportScheduleDTO dto, @MappingTarget ReportSchedule entity);
}
