package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.ProductImageDto;
import com.hisobnoma.platform.inventory.entity.ProductImage;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {

    @Mapping(target = "productId", source = "product.id")
    ProductImageDto toDto(ProductImage image);

    List<ProductImageDto> toDtoList(List<ProductImage> images);
}
