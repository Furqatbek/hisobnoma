package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateUomRequest;
import com.hisobnoma.platform.inventory.dto.UnitOfMeasureDto;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UnitOfMeasureMapper {

    @Mapping(target = "baseUomId", source = "baseUom.id")
    @Mapping(target = "baseUomCode", source = "baseUom.code")
    @Mapping(target = "baseUomName", source = "baseUom.name")
    UnitOfMeasureDto toDto(UnitOfMeasure uom);

    List<UnitOfMeasureDto> toDtoList(List<UnitOfMeasure> uoms);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "baseUom", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    UnitOfMeasure toEntity(CreateUomRequest request);
}
