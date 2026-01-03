package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateExchangeRateRequest;
import com.hisobnoma.platform.finance.dto.ExchangeRateDto;
import com.hisobnoma.platform.finance.entity.ExchangeRate;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExchangeRateMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    ExchangeRateDto toDto(ExchangeRate exchangeRate);

    List<ExchangeRateDto> toDtoList(List<ExchangeRate> exchangeRates);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "inverseRate", ignore = true)
    @Mapping(target = "active", ignore = true)
    ExchangeRate toEntity(CreateExchangeRateRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true), nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "inverseRate", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(CreateExchangeRateRequest request, @MappingTarget ExchangeRate exchangeRate);
}
