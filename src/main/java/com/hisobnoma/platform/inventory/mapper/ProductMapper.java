package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ProductVariantMapper.class, ProductImageMapper.class, ProductAttributeMapper.class})
public interface ProductMapper {

    @Named("toDto")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "baseUomId", source = "baseUom.id")
    @Mapping(target = "baseUomCode", source = "baseUom.code")
    @Mapping(target = "baseUomName", source = "baseUom.name")
    @Mapping(target = "margin", expression = "java(product.getMargin())")
    @Mapping(target = "markup", expression = "java(product.getMarkup())")
    ProductDto toDto(Product product);

    @Named("toDtoSimple")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "baseUomId", source = "baseUom.id")
    @Mapping(target = "baseUomCode", source = "baseUom.code")
    @Mapping(target = "baseUomName", source = "baseUom.name")
    @Mapping(target = "margin", expression = "java(product.getMargin())")
    @Mapping(target = "markup", expression = "java(product.getMarkup())")
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    ProductDto toDtoSimple(Product product);

    @IterableMapping(qualifiedByName = "toDtoSimple")
    List<ProductDto> toDtoList(List<Product> products);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "baseUom", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    @Mapping(target = "hasVariants", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Product toEntity(CreateProductRequest request);
}
