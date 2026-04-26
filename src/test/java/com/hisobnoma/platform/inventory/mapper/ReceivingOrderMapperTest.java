package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.ReceivingLineDto;
import com.hisobnoma.platform.inventory.dto.ReceivingOrderDto;
import com.hisobnoma.platform.inventory.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReceivingOrderMapperTest {

    @Autowired
    private ReceivingOrderMapper receivingOrderMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        PurchaseOrder po = PurchaseOrder.builder()
                .id(10L)
                .poNumber("PO-2025-001")
                .lines(new ArrayList<>())
                .build();

        Vendor vendor = Vendor.builder()
                .id(5L)
                .code("VND-001")
                .name("Tech Supplier")
                .active(true)
                .build();

        Location location = Location.builder()
                .id(3L)
                .code("WH-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .childLocations(new ArrayList<>())
                .build();

        ReceivingOrder ro = ReceivingOrder.builder()
                .id(1L)
                .tenantId(1L)
                .receivingNumber("RCV-2025-001")
                .purchaseOrder(po)
                .vendor(vendor)
                .location(location)
                .status(ReceivingStatus.COMPLETED)
                .receivingDate(LocalDate.of(2025, 1, 20))
                .vendorDeliveryNote("DN-001")
                .vendorInvoiceNumber("INV-001")
                .vendorInvoiceDate(LocalDate.of(2025, 1, 19))
                .subtotal(new BigDecimal("10000.0000"))
                .discountAmount(new BigDecimal("200.0000"))
                .taxAmount(new BigDecimal("1176.0000"))
                .shippingAmount(new BigDecimal("150.0000"))
                .totalAmount(new BigDecimal("11126.0000"))
                .currency("UZS")
                .notes("All items received")
                .internalNotes("Quality check passed")
                .stockUpdated(true)
                .apInvoiceCreated(false)
                .lines(new ArrayList<>())
                .build();

        // When
        ReceivingOrderDto dto = receivingOrderMapper.toDto(ro);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("RCV-2025-001", dto.getReceivingNumber());
        assertEquals(10L, dto.getPurchaseOrderId());
        assertEquals("PO-2025-001", dto.getPoNumber());
        assertEquals(5L, dto.getVendorId());
        assertEquals("VND-001", dto.getVendorCode());
        assertEquals("Tech Supplier", dto.getVendorName());
        assertEquals(3L, dto.getLocationId());
        assertEquals("WH-001", dto.getLocationCode());
        assertEquals("Main Warehouse", dto.getLocationName());
        assertEquals(ReceivingStatus.COMPLETED, dto.getStatus());
        assertEquals(LocalDate.of(2025, 1, 20), dto.getReceivingDate());
        assertEquals("DN-001", dto.getVendorDeliveryNote());
        assertEquals("INV-001", dto.getVendorInvoiceNumber());
        assertEquals(LocalDate.of(2025, 1, 19), dto.getVendorInvoiceDate());
        assertEquals(new BigDecimal("10000.0000"), dto.getSubtotal());
        assertEquals(new BigDecimal("200.0000"), dto.getDiscountAmount());
        assertEquals(new BigDecimal("1176.0000"), dto.getTaxAmount());
        assertEquals(new BigDecimal("150.0000"), dto.getShippingAmount());
        assertEquals(new BigDecimal("11126.0000"), dto.getTotalAmount());
        assertEquals("UZS", dto.getCurrency());
        assertEquals("All items received", dto.getNotes());
        assertEquals("Quality check passed", dto.getInternalNotes());
        assertTrue(dto.isStockUpdated());
        assertFalse(dto.isApInvoiceCreated());
        // Expression-based: empty lines -> no variances
        assertFalse(dto.isHasVariances());
    }

    @Test
    void toDto_withNullPurchaseOrder() {
        // Given
        Vendor vendor = Vendor.builder()
                .id(5L).code("VND-001").name("Vendor").active(true).build();
        Location location = Location.builder()
                .id(3L).code("WH-001").name("Warehouse")
                .locationType(LocationType.WAREHOUSE).childLocations(new ArrayList<>())
                .build();

        ReceivingOrder ro = ReceivingOrder.builder()
                .id(1L)
                .receivingNumber("RCV-002")
                .purchaseOrder(null)
                .vendor(vendor)
                .location(location)
                .status(ReceivingStatus.DRAFT)
                .receivingDate(LocalDate.now())
                .lines(new ArrayList<>())
                .build();

        // When
        ReceivingOrderDto dto = receivingOrderMapper.toDto(ro);

        // Then
        assertNotNull(dto);
        assertNull(dto.getPurchaseOrderId());
        assertNull(dto.getPoNumber());
    }

    @Test
    void toLineDto_mapsAllFields() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(10L).sku("SKU-001").name("Galaxy S24")
                .baseUom(uom).sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>()).images(new ArrayList<>()).attributes(new ArrayList<>())
                .build();

        ProductVariant variant = ProductVariant.builder()
                .id(20L).name("128GB Black").product(product).sku("SKU-001-BLK").build();

        Location targetLocation = Location.builder()
                .id(4L).code("BIN-A1").name("Bin A1")
                .locationType(LocationType.BIN).childLocations(new ArrayList<>())
                .build();

        ReceivingOrder ro = ReceivingOrder.builder()
                .id(1L).receivingNumber("RCV-001").lines(new ArrayList<>()).build();

        PurchaseOrderLine poLine = PurchaseOrderLine.builder()
                .id(50L).build();

        ReceivingLine line = ReceivingLine.builder()
                .id(100L)
                .receivingOrder(ro)
                .poLine(poLine)
                .product(product)
                .productVariant(variant)
                .lineNumber(1)
                .description("Galaxy S24 128GB Black")
                .expectedQuantity(new BigDecimal("50.0000"))
                .receivedQuantity(new BigDecimal("48.0000"))
                .acceptedQuantity(new BigDecimal("47.0000"))
                .rejectedQuantity(new BigDecimal("1.0000"))
                .rejectionReason("Damaged packaging")
                .uom(uom)
                .unitCost(new BigDecimal("500.0000"))
                .taxPercent(new BigDecimal("12.00"))
                .taxAmount(new BigDecimal("2820.0000"))
                .lineTotal(new BigDecimal("26320.0000"))
                .batchNumber("BATCH-001")
                .serialNumbers("SN001,SN002,SN003")
                .manufactureDate(LocalDate.of(2024, 12, 1))
                .expiryDate(LocalDate.of(2027, 12, 1))
                .targetLocation(targetLocation)
                .notes("Minor damage on one unit")
                .stockMovementId(200L)
                .build();

        // When
        ReceivingLineDto dto = receivingOrderMapper.toLineDto(line);

        // Then
        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals(1L, dto.getReceivingOrderId());
        assertEquals(50L, dto.getPoLineId());
        assertEquals(10L, dto.getProductId());
        assertEquals("SKU-001", dto.getProductSku());
        assertEquals("Galaxy S24", dto.getProductName());
        assertEquals(20L, dto.getProductVariantId());
        assertEquals("128GB Black", dto.getVariantName());
        assertEquals(1, dto.getLineNumber());
        assertEquals("Galaxy S24 128GB Black", dto.getDescription());
        assertEquals(new BigDecimal("50.0000"), dto.getExpectedQuantity());
        assertEquals(new BigDecimal("48.0000"), dto.getReceivedQuantity());
        assertEquals(new BigDecimal("47.0000"), dto.getAcceptedQuantity());
        assertEquals(new BigDecimal("1.0000"), dto.getRejectedQuantity());
        assertEquals("Damaged packaging", dto.getRejectionReason());
        assertEquals(1L, dto.getUomId());
        assertEquals("PCS", dto.getUomCode());
        assertEquals("Pieces", dto.getUomName());
        assertEquals(new BigDecimal("500.0000"), dto.getUnitCost());
        assertEquals(new BigDecimal("12.00"), dto.getTaxPercent());
        assertEquals(new BigDecimal("2820.0000"), dto.getTaxAmount());
        assertEquals(new BigDecimal("26320.0000"), dto.getLineTotal());
        assertEquals("BATCH-001", dto.getBatchNumber());
        assertEquals("SN001,SN002,SN003", dto.getSerialNumbers());
        assertEquals(LocalDate.of(2024, 12, 1), dto.getManufactureDate());
        assertEquals(LocalDate.of(2027, 12, 1), dto.getExpiryDate());
        assertEquals(4L, dto.getTargetLocationId());
        assertEquals("Bin A1", dto.getTargetLocationName());
        assertEquals("Minor damage on one unit", dto.getNotes());
        assertEquals(200L, dto.getStockMovementId());
        // Expression-based
        assertEquals(new BigDecimal("-2.0000"), dto.getVarianceQuantity());
        assertTrue(dto.isHasVariance());
        assertFalse(dto.isOverReceived());
        assertTrue(dto.isUnderReceived());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        Vendor vendor = Vendor.builder().id(1L).code("VND-001").name("Vendor").active(true).build();
        Location location = Location.builder()
                .id(1L).code("WH-001").name("Warehouse")
                .locationType(LocationType.WAREHOUSE).childLocations(new ArrayList<>())
                .build();

        ReceivingOrder ro1 = ReceivingOrder.builder()
                .id(1L).receivingNumber("RCV-001").vendor(vendor).location(location)
                .status(ReceivingStatus.DRAFT).receivingDate(LocalDate.now()).lines(new ArrayList<>())
                .build();

        ReceivingOrder ro2 = ReceivingOrder.builder()
                .id(2L).receivingNumber("RCV-002").vendor(vendor).location(location)
                .status(ReceivingStatus.COMPLETED).receivingDate(LocalDate.now()).lines(new ArrayList<>())
                .build();

        // When
        List<ReceivingOrderDto> dtos = receivingOrderMapper.toDtoList(List.of(ro1, ro2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("RCV-001", dtos.get(0).getReceivingNumber());
        assertEquals("RCV-002", dtos.get(1).getReceivingNumber());
    }
}
