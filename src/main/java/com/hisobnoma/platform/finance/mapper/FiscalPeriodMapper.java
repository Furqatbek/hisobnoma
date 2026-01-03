package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.FiscalPeriodDto;
import com.hisobnoma.platform.finance.entity.FiscalPeriod;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FiscalPeriodMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "fiscalYear.id", target = "fiscalYearId")
    @Mapping(source = "fiscalYear.year", target = "fiscalYear")
    @Mapping(expression = "java(fiscalPeriod.getDisplayName())", target = "displayName")
    FiscalPeriodDto toDto(FiscalPeriod fiscalPeriod);

    List<FiscalPeriodDto> toDtoList(List<FiscalPeriod> fiscalPeriods);
}
