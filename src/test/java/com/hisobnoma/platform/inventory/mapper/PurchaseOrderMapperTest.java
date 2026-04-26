package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.PurchaseOrderDto;
import com.hisobnoma.platform.inventory.dto.PurchaseOrderLineDto;
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
class PurchaseOrderMapperTest {

    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
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

        PurchaseOrder po = PurchaseOrder.builder()
                .id(1L)
                .tenantId(1L)
                .poNumber("PO-2025-001")
                .vendor(vendor)
                .location(location)
                .status(POStatus.APPROVED)
                .orderDate(LocalDate.of(2025, 1, 15))
                .expectedDate(LocalDate.of(2025, 1, 25))
                .receivedDate(null)
                .subtotal(new BigDecimal("10000.0000"))
                .discountAmount(new BigDecimal("500.0000"))
                .discountPercent(new BigDecimal("5.00"))
                .taxAmount(new BigDecimal("1140.0000"))
                .shippingAmount(new BigDecimal("200.0000"))
                .totalAmount(new BigDecimal("10840.0000"))
                .currency("UZS")
                .paymentTerms("Net 30")
                .shippingMethod("Truck delivery")
                .shippingAddress("123 Warehouse St, Tashkent")
                .notes("Urgent order")
                .internalNotes("Check quality on arrival")
                .vendorReference("VS-REF-001")
                .lines(new ArrayList<>())
                .build();

        // When
        PurchaseOrderDto dto = purchaseOrderMapper.toDto(po);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("PO-2025-001", dto.getPoNumber());
        assertEquals(5L, dto.getVendorId());
        assertEquals("VND-001", dto.getVendorCode());
        assertEquals("Tech Supplier", dto.getVendorName());
        assertEquals(3L, dto.getLocationId());
        assertEquals("WH-001", dto.getLocationCode());
        assertEquals("Main Warehouse", dto.getLocationName());
        assertEquals(POStatus.APPROVED, dto.getStatus());
        assertEquals(LocalDate.of(2025, 1, 15), dto.getOrderDate());
        assertEquals(LocalDate.of(2025, 1, 25), dto.getExpectedDate());
        assertNull(dto.getReceivedDate());
        assertEquals(new BigDecimal("10000.0000"), dto.getSubtotal());
        assertEquals(new BigDecimal("500.0000"), dto.getDiscountAmount());
        assertEquals(new BigDecimal("5.00"), dto.getDiscountPercent());
        assertEquals(new BigDecimal("1140.0000"), dto.getTaxAmount());
        assertEquals(new BigDecimal("200.0000"), dto.getShippingAmount());
        assertEquals(new BigDecimal("10840.0000"), dto.getTotalAmount());
        assertEquals("UZS", dto.getCurrency());
        assertEquals("Net 30", dto.getPaymentTerms());
        assertEquals("Truck delivery", dto.getShippingMethod());
        assertEquals("123 Warehouse St, Tashkent", dto.getShippingAddress());
        assertEquals("Urgent order", dto.getNotes());
        assertEquals("Check quality on arrival", dto.getInternalNotes());
        assertEquals("VS-REF-001", dto.getVendorReference());
        // Expression-based: empty lines -> fullyReceived = true (all match vacuously)
        assertFalse(dto.isPartiallyReceived());
    }

    @Test
    void toLineDto_mapsAllFields() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(10L)
                .sku("SKU-001")
                .name("Galaxy S24")
                .baseUom(uom)
                .sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        ProductVariant variant = ProductVariant.builder()
                .id(20L)
                .name("128GB Black")
                .product(product)
                .sku("SKU-001-BLK")
                .build();

        PurchaseOrder po = PurchaseOrder.builder()
                .id(1L)
                .poNumber("PO-001")
                .lines(new ArrayList<>())
                .build();

        PurchaseOrderLine line = PurchaseOrderLine.builder()
                .id(100L)
                .purchaseOrder(po)
                .product(product)
                .productVariant(variant)
                .lineNumber(1)
                .description("Galaxy S24 128GB Black")
                .quantity(new BigDecimal("50.0000"))
                .receivedQuantity(new BigDecimal("30.0000"))
                .cancelledQuantity(new BigDecimal("5.0000"))
                .uom(uom)
                .unitPrice(new BigDecimal("500.0000"))
                .discountPercent(new BigDecimal("2.00"))
                .discountAmount(new BigDecimal("500.0000"))
                .taxPercent(new BigDecimal("12.00"))
                .taxAmount(new BigDecimal("2940.0000"))
                .lineTotal(new BigDecimal("27440.0000"))
                .notes("Priority item")
                .expectedDate(LocalDate.of(2025, 1, 25))
                .build();

        // When
        PurchaseOrderLineDto dto = purchaseOrderMapper.toLineDto(line);

        // Then
        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals(1L, dto.getPurchaseOrderId());
        assertEquals(10L, dto.getProductId());
        assertEquals("SKU-001", dto.getProductSku());
        assertEquals("Galaxy S24", dto.getProductName());
        assertEquals(20L, dto.getProductVariantId());
        assertEquals("128GB Black", dto.getVariantName());
        assertEquals(1, dto.getLineNumber());
        assertEquals("Galaxy S24 128GB Black", dto.getDescription());
        assertEquals(new BigDecimal("50.0000"), dto.getQuantity());
        assertEquals(new BigDecimal("30.0000"), dto.getReceivedQuantity());
        assertEquals(new BigDecimal("5.0000"), dto.getCancelledQuantity());
        assertEquals(1L, dto.getUomId());
        assertEquals("PCS", dto.getUomCode());
        assertEquals("Pieces", dto.getUomName());
        assertEquals(new BigDecimal("500.0000"), dto.getUnitPrice());
        assertEquals(new BigDecimal("2.00"), dto.getDiscountPercent());
        assertEquals(new BigDecimal("500.0000"), dto.getDiscountAmount());
        assertEquals(new BigDecimal("12.00"), dto.getTaxPercent());
        assertEquals(new BigDecimal("2940.0000"), dto.getTaxAmount());
        assertEquals(new BigDecimal("27440.0000"), dto.getLineTotal());
        assertEquals("Priority item", dto.getNotes());
        assertEquals(LocalDate.of(2025, 1, 25), dto.getExpectedDate());
        // Expression-based
        assertEquals(new BigDecimal("15.0000"), dto.getPendingQuantity());
        assertFalse(dto.isFullyReceived());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        Vendor vendor = Vendor.builder().id(1L).code("VND-001").name("Vendor").active(true).build();
        Location location = Location.builder()
                .id(1L).code("WH-001").name("Warehouse")
                .locationType(LocationType.WAREHOUSE).childLocations(new ArrayList<>())
                .build();

        PurchaseOrder po1 = PurchaseOrder.builder()
                .id(1L).poNumber("PO-001").vendor(vendor).location(location)
                .status(POStatus.DRAFT).orderDate(LocalDate.now()).lines(new ArrayList<>())
                .build();

        PurchaseOrder po2 = PurchaseOrder.builder()
                .id(2L).poNumber("PO-002").vendor(vendor).location(location)
                .status(POStatus.APPROVED).orderDate(LocalDate.now()).lines(new ArrayList<>())
                .build();

        // When
        List<PurchaseOrderDto> dtos = purchaseOrderMapper.toDtoList(List.of(po1, po2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("PO-001", dtos.get(0).getPoNumber());
        assertEquals("PO-002", dtos.get(1).getPoNumber());
    }
}
