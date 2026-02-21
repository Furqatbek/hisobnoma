package com.hisobnoma.platform.delivery.mapper;

import com.hisobnoma.platform.delivery.dto.DeliveryVillageDTO;
import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DeliveryVillageMapper {

    @Mapping(source = "region.id", target = "regionId")
    @Mapping(source = "region.name", target = "regionName")
    DeliveryVillageDTO toDto(DeliveryVillage entity);

    List<DeliveryVillageDTO> toDtoList(List<DeliveryVillage> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "region", ignore = true)
    DeliveryVillage toEntity(DeliveryVillageDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "region", ignore = true)
    void updateEntity(DeliveryVillageDTO dto, @MappingTarget DeliveryVillage entity);
}
