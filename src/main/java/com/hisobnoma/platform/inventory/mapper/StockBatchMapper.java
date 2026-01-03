package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.StockBatchDto;
import com.hisobnoma.platform.inventory.entity.StockBatch;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockBatchMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "locationId", source = "location.id")
    @Mapping(target = "locationCode", source = "location.code")
    @Mapping(target = "locationName", source = "location.name")
    @Mapping(target = "quantityAvailable", expression = "java(batch.getQuantityAvailable())")
    @Mapping(target = "value", expression = "java(batch.getValue())")
    @Mapping(target = "daysUntilExpiry", expression = "java(batch.getDaysUntilExpiry())")
    @Mapping(target = "expired", expression = "java(batch.isExpired())")
    @Mapping(target = "expiringWithin30Days", expression = "java(batch.isExpiringWithin(30))")
    StockBatchDto toDto(StockBatch batch);

    List<StockBatchDto> toDtoList(List<StockBatch> batches);
}
