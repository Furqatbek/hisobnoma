package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateTaxCodeRequest;
import com.hisobnoma.platform.finance.dto.TaxCodeDto;
import com.hisobnoma.platform.finance.entity.TaxCode;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {TaxRateMapper.class})
public interface TaxCodeMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "salesAccount.id", target = "salesAccountId")
    @Mapping(source = "salesAccount.code", target = "salesAccountCode")
    @Mapping(source = "salesAccount.name", target = "salesAccountName")
    @Mapping(source = "purchaseAccount.id", target = "purchaseAccountId")
    @Mapping(source = "purchaseAccount.code", target = "purchaseAccountCode")
    @Mapping(source = "purchaseAccount.name", target = "purchaseAccountName")
    TaxCodeDto toDto(TaxCode taxCode);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "salesAccount.id", target = "salesAccountId")
    @Mapping(source = "salesAccount.code", target = "salesAccountCode")
    @Mapping(source = "salesAccount.name", target = "salesAccountName")
    @Mapping(source = "purchaseAccount.id", target = "purchaseAccountId")
    @Mapping(source = "purchaseAccount.code", target = "purchaseAccountCode")
    @Mapping(source = "purchaseAccount.name", target = "purchaseAccountName")
    @Mapping(target = "rates", ignore = true)
    TaxCodeDto toDtoWithoutRates(TaxCode taxCode);

    List<TaxCodeDto> toDtoList(List<TaxCode> taxCodes);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "systemCode", ignore = true)
    @Mapping(target = "salesAccount", ignore = true)
    @Mapping(target = "purchaseAccount", ignore = true)
    @Mapping(target = "rates", ignore = true)
    TaxCode toEntity(CreateTaxCodeRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true), nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "systemCode", ignore = true)
    @Mapping(target = "salesAccount", ignore = true)
    @Mapping(target = "purchaseAccount", ignore = true)
    @Mapping(target = "rates", ignore = true)
    void updateEntity(CreateTaxCodeRequest request, @MappingTarget TaxCode taxCode);
}
