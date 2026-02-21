package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.ProductUomDto;
import com.hisobnoma.platform.inventory.entity.ProductUom;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductUomMapper {

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "uom.id", target = "uomId")
    @Mapping(source = "uom.code", target = "uomCode")
    @Mapping(source = "uom.name", target = "uomName")
    @Mapping(source = "uom.symbol", target = "uomSymbol")
    @Mapping(target = "effectiveSellingPrice", expression = "java(productUom.getEffectiveSellingPrice())")
    ProductUomDto toDto(ProductUom productUom);

    List<ProductUomDto> toDtoList(List<ProductUom> productUoms);
}
