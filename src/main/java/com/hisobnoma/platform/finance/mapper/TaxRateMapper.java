package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateTaxRateRequest;
import com.hisobnoma.platform.finance.dto.TaxRateDto;
import com.hisobnoma.platform.finance.entity.TaxRate;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaxRateMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "taxCode.id", target = "taxCodeId")
    @Mapping(source = "taxCode.code", target = "taxCodeCode")
    @Mapping(source = "taxCode.name", target = "taxCodeName")
    TaxRateDto toDto(TaxRate taxRate);

    List<TaxRateDto> toDtoList(List<TaxRate> taxRates);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "taxCode", ignore = true)
    TaxRate toEntity(CreateTaxRateRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true), nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "taxCode", ignore = true)
    void updateEntity(CreateTaxRateRequest request, @MappingTarget TaxRate taxRate);
}
