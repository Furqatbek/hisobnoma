package com.hisobnoma.platform.reports.mapper;

import com.hisobnoma.platform.reports.dto.ReportExecutionDTO;
import com.hisobnoma.platform.reports.entity.ReportExecution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReportExecutionMapper {

    @Mapping(target = "reportDefinitionId", source = "reportDefinition.id")
    @Mapping(target = "reportCode", source = "reportDefinition.code")
    @Mapping(target = "reportName", source = "reportDefinition.name")
    @Mapping(target = "reportScheduleId", source = "reportSchedule.id")
    ReportExecutionDTO toDto(ReportExecution entity);

    List<ReportExecutionDTO> toDtoList(List<ReportExecution> entities);
}
