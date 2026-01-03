package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.ReceivingLineDto;
import com.hisobnoma.platform.inventory.dto.ReceivingOrderDto;
import com.hisobnoma.platform.inventory.entity.ReceivingLine;
import com.hisobnoma.platform.inventory.entity.ReceivingOrder;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReceivingOrderMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "purchaseOrderId", source = "purchaseOrder.id")
    @Mapping(target = "poNumber", source = "purchaseOrder.poNumber")
    @Mapping(target = "vendorId", source = "vendor.id")
    @Mapping(target = "vendorCode", source = "vendor.code")
    @Mapping(target = "vendorName", source = "vendor.name")
    @Mapping(target = "locationId", source = "location.id")
    @Mapping(target = "locationCode", source = "location.code")
    @Mapping(target = "locationName", source = "location.name")
    @Mapping(target = "hasVariances", expression = "java(ro.hasVariances())")
    ReceivingOrderDto toDto(ReceivingOrder ro);

    List<ReceivingOrderDto> toDtoList(List<ReceivingOrder> receivingOrders);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "receivingOrderId", source = "receivingOrder.id")
    @Mapping(target = "poLineId", source = "poLine.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productVariantId", source = "productVariant.id")
    @Mapping(target = "variantName", source = "productVariant.name")
    @Mapping(target = "uomId", source = "uom.id")
    @Mapping(target = "uomCode", source = "uom.code")
    @Mapping(target = "uomName", source = "uom.name")
    @Mapping(target = "targetLocationId", source = "targetLocation.id")
    @Mapping(target = "targetLocationName", source = "targetLocation.name")
    @Mapping(target = "varianceQuantity", expression = "java(line.getVarianceQuantity())")
    @Mapping(target = "hasVariance", expression = "java(line.hasVariance())")
    @Mapping(target = "overReceived", expression = "java(line.isOverReceived())")
    @Mapping(target = "underReceived", expression = "java(line.isUnderReceived())")
    ReceivingLineDto toLineDto(ReceivingLine line);

    List<ReceivingLineDto> toLineDtoList(List<ReceivingLine> lines);
}
