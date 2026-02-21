package com.hisobnoma.platform.delivery.mapper;

import com.hisobnoma.platform.delivery.dto.DeliveryRegionDTO;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DeliveryRegionMapper {

    @Mapping(target = "villageCount", expression = "java(entity.getVillages() != null ? entity.getVillages().size() : 0)")
    DeliveryRegionDTO toDto(DeliveryRegion entity);

    List<DeliveryRegionDTO> toDtoList(List<DeliveryRegion> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "villages", ignore = true)
    DeliveryRegion toEntity(DeliveryRegionDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "villages", ignore = true)
    void updateEntity(DeliveryRegionDTO dto, @MappingTarget DeliveryRegion entity);
}
