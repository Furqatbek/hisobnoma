package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateFiscalYearRequest;
import com.hisobnoma.platform.finance.dto.FiscalYearDto;
import com.hisobnoma.platform.finance.entity.FiscalYear;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {FiscalPeriodMapper.class})
public interface FiscalYearMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    FiscalYearDto toDto(FiscalYear fiscalYear);

    List<FiscalYearDto> toDtoList(List<FiscalYear> fiscalYears);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "periods", ignore = true)
    FiscalYearDto toDtoWithoutPeriods(FiscalYear fiscalYear);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "closed", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "closedBy", ignore = true)
    @Mapping(target = "yearEndClosingEntryId", ignore = true)
    @Mapping(target = "periods", ignore = true)
    FiscalYear toEntity(CreateFiscalYearRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true), nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "closed", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "closedBy", ignore = true)
    @Mapping(target = "yearEndClosingEntryId", ignore = true)
    @Mapping(target = "periods", ignore = true)
    void updateEntity(CreateFiscalYearRequest request, @MappingTarget FiscalYear fiscalYear);
}
