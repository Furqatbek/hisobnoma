package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.PurchaseOrderDto;
import com.hisobnoma.platform.inventory.dto.PurchaseOrderLineDto;
import com.hisobnoma.platform.inventory.entity.PurchaseOrder;
import com.hisobnoma.platform.inventory.entity.PurchaseOrderLine;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PurchaseOrderMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "vendorId", source = "vendor.id")
    @Mapping(target = "vendorCode", source = "vendor.code")
    @Mapping(target = "vendorName", source = "vendor.name")
    @Mapping(target = "locationId", source = "location.id")
    @Mapping(target = "locationCode", source = "location.code")
    @Mapping(target = "locationName", source = "location.name")
    @Mapping(target = "fullyReceived", expression = "java(po.isFullyReceived())")
    @Mapping(target = "partiallyReceived", expression = "java(po.isPartiallyReceived())")
    PurchaseOrderDto toDto(PurchaseOrder po);

    List<PurchaseOrderDto> toDtoList(List<PurchaseOrder> purchaseOrders);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "purchaseOrderId", source = "purchaseOrder.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productVariantId", source = "productVariant.id")
    @Mapping(target = "variantName", source = "productVariant.name")
    @Mapping(target = "uomId", source = "uom.id")
    @Mapping(target = "uomCode", source = "uom.code")
    @Mapping(target = "uomName", source = "uom.name")
    @Mapping(target = "pendingQuantity", expression = "java(line.getPendingQuantity())")
    @Mapping(target = "fullyReceived", expression = "java(line.isFullyReceived())")
    PurchaseOrderLineDto toLineDto(PurchaseOrderLine line);

    List<PurchaseOrderLineDto> toLineDtoList(List<PurchaseOrderLine> lines);
}
