package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CreatePriceListItemRequest;
import com.hisobnoma.platform.pos.dto.PriceListItemDto;
import com.hisobnoma.platform.pos.entity.PriceListItem;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PriceListItemMapper {

    @Mapping(source = "priceList.id", target = "priceListId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.sku", target = "productCode")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "variant.id", target = "variantId")
    @Mapping(source = "variant.name", target = "variantName")
    @Mapping(target = "basePrice", ignore = true)
    @Mapping(target = "effectivePrice", ignore = true)
    PriceListItemDto toDto(PriceListItem item);

    List<PriceListItemDto> toDtoList(List<PriceListItem> items);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "priceList", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "variant", ignore = true)
    @Mapping(target = "active", constant = "true")
    PriceListItem toEntity(CreatePriceListItemRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "priceList", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "variant", ignore = true)
    void updateEntity(CreatePriceListItemRequest request, @MappingTarget PriceListItem item);
}
