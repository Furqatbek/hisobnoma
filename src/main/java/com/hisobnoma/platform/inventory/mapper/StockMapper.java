package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.StockDto;
import com.hisobnoma.platform.inventory.entity.Stock;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productBarcode", source = "product.barcode")
    @Mapping(target = "locationId", source = "location.id")
    @Mapping(target = "locationCode", source = "location.code")
    @Mapping(target = "locationName", source = "location.name")
    @Mapping(target = "quantityAvailable", expression = "java(stock.getQuantityAvailable())")
    @Mapping(target = "stockValue", expression = "java(stock.getStockValue())")
    @Mapping(target = "belowReorderPoint", expression = "java(stock.isBelowReorderPoint())")
    @Mapping(target = "belowMinimum", expression = "java(stock.isBelowMinimum())")
    @Mapping(target = "baseUomCode", source = "product.baseUom.code")
    @Mapping(target = "baseUomName", source = "product.baseUom.name")
    StockDto toDto(Stock stock);

    List<StockDto> toDtoList(List<Stock> stocks);
}
