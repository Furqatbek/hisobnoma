package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateProductRequest;
import com.hisobnoma.platform.inventory.dto.ProductAttributeDto;
import com.hisobnoma.platform.inventory.entity.ProductAttribute;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductAttributeMapper {

    @Mapping(target = "productId", source = "product.id")
    ProductAttributeDto toDto(ProductAttribute attribute);

    List<ProductAttributeDto> toDtoList(List<ProductAttribute> attributes);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ProductAttribute toEntity(CreateProductRequest.CreateAttributeRequest request);
}
