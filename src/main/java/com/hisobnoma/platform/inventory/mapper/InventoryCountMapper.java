package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.InventoryCountDto;
import com.hisobnoma.platform.inventory.dto.InventoryCountLineDto;
import com.hisobnoma.platform.inventory.entity.InventoryCount;
import com.hisobnoma.platform.inventory.entity.InventoryCountLine;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryCountMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "locationId", source = "location.id")
    @Mapping(target = "locationCode", source = "location.code")
    @Mapping(target = "locationName", source = "location.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "completionPercentage", expression = "java(ic.getCompletionPercentage())")
    @Mapping(target = "complete", expression = "java(ic.isComplete())")
    InventoryCountDto toDto(InventoryCount ic);

    List<InventoryCountDto> toDtoList(List<InventoryCount> inventoryCounts);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "inventoryCountId", source = "inventoryCount.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productVariantId", source = "productVariant.id")
    @Mapping(target = "variantName", source = "productVariant.name")
    @Mapping(target = "uomId", source = "uom.id")
    @Mapping(target = "uomCode", source = "uom.code")
    @Mapping(target = "uomName", source = "uom.name")
    @Mapping(target = "hasVariance", expression = "java(line.hasVariance())")
    @Mapping(target = "positiveVariance", expression = "java(line.isPositiveVariance())")
    @Mapping(target = "negativeVariance", expression = "java(line.isNegativeVariance())")
    @Mapping(target = "variancePercentage", expression = "java(line.getVariancePercentage())")
    InventoryCountLineDto toLineDto(InventoryCountLine line);

    List<InventoryCountLineDto> toLineDtoList(List<InventoryCountLine> lines);
}
