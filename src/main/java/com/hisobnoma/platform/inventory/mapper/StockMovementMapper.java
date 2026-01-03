package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.StockMovementDto;
import com.hisobnoma.platform.inventory.entity.StockMovement;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productVariantId", source = "productVariant.id")
    @Mapping(target = "variantName", source = "productVariant.name")
    @Mapping(target = "fromLocationId", source = "fromLocation.id")
    @Mapping(target = "fromLocationName", source = "fromLocation.name")
    @Mapping(target = "toLocationId", source = "toLocation.id")
    @Mapping(target = "toLocationName", source = "toLocation.name")
    StockMovementDto toDto(StockMovement movement);

    List<StockMovementDto> toDtoList(List<StockMovement> movements);
}
