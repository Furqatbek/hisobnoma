package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateProductRequest;
import com.hisobnoma.platform.inventory.dto.ProductVariantDto;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "fullName", expression = "java(variant.getFullName())")
    @Mapping(target = "effectiveSellingPrice", expression = "java(variant.getEffectiveSellingPrice())")
    @Mapping(target = "effectiveCostPrice", expression = "java(variant.getEffectiveCostPrice())")
    ProductVariantDto toDto(ProductVariant variant);

    List<ProductVariantDto> toDtoList(List<ProductVariant> variants);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ProductVariant toEntity(CreateProductRequest.CreateVariantRequest request);
}
