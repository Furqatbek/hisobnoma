package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.SerialNumberDto;
import com.hisobnoma.platform.inventory.entity.SerialNumber;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SerialNumberMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productVariantId", source = "productVariant.id")
    @Mapping(target = "variantName", source = "productVariant.name")
    @Mapping(target = "locationId", source = "location.id")
    @Mapping(target = "locationCode", source = "location.code")
    @Mapping(target = "locationName", source = "location.name")
    @Mapping(target = "batchId", source = "batch.id")
    @Mapping(target = "batchNumber", source = "batch.batchNumber")
    @Mapping(target = "underWarranty", expression = "java(serial.isUnderWarranty())")
    @Mapping(target = "warrantyDaysRemaining", expression = "java(serial.getWarrantyDaysRemaining())")
    SerialNumberDto toDto(SerialNumber serial);

    List<SerialNumberDto> toDtoList(List<SerialNumber> serials);
}
