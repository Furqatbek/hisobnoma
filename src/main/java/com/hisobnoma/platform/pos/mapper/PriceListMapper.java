package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CreatePriceListRequest;
import com.hisobnoma.platform.pos.dto.PriceListDto;
import com.hisobnoma.platform.pos.entity.PriceList;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PriceListItemMapper.class}, builder = @Builder(disableBuilder = true))
public interface PriceListMapper {

    @Named("toDto")
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "itemCount", expression = "java(priceList.getItems() != null ? priceList.getItems().size() : 0)")
    @Mapping(target = "customerCount", expression = "java(priceList.getCustomerAssignments() != null ? priceList.getCustomerAssignments().size() : 0)")
    @Mapping(target = "locationName", ignore = true)
    PriceListDto toDto(PriceList priceList);

    @Named("toDtoWithItems")
    @Mapping(target = "itemCount", expression = "java(priceList.getItems() != null ? priceList.getItems().size() : 0)")
    @Mapping(target = "customerCount", expression = "java(priceList.getCustomerAssignments() != null ? priceList.getCustomerAssignments().size() : 0)")
    @Mapping(target = "locationName", ignore = true)
    PriceListDto toDtoWithItems(PriceList priceList);

    @IterableMapping(qualifiedByName = "toDto")
    List<PriceListDto> toDtoList(List<PriceList> priceLists);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "customerAssignments", ignore = true)
    @Mapping(target = "active", constant = "true")
    PriceList toEntity(CreatePriceListRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "customerAssignments", ignore = true)
    void updateEntity(CreatePriceListRequest request, @MappingTarget PriceList priceList);
}
