package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.pos.dto.POSTransactionLineDto;
import com.hisobnoma.platform.pos.entity.POSTransaction;
import com.hisobnoma.platform.pos.entity.POSTransactionLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class POSTransactionLineMapperTest {

    @Autowired
    private POSTransactionLineMapper posTransactionLineMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        POSTransaction transaction = POSTransaction.builder()
                .id(1L)
                .build();

        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(5L)
                .code("PCS")
                .name("Pieces")
                .build();

        Product product = Product.builder()
                .id(10L)
                .sku("SKU-001")
                .name("Test Product")
                .baseUom(uom)
                .build();

        ProductVariant variant = ProductVariant.builder()
                .id(20L)
                .name("Red / Large")
                .build();

        POSTransactionLine line = POSTransactionLine.builder()
                .id(100L)
                .transaction(transaction)
                .lineNumber(1)
                .product(product)
                .variant(variant)
                .productCode("SKU-001")
                .productName("Test Product")
                .variantName("Red / Large")
                .barcode("1234567890")
                .quantity(new BigDecimal("2.0000"))
                .unitPrice(new BigDecimal("50000.0000"))
                .originalPrice(new BigDecimal("55000.0000"))
                .costPrice(new BigDecimal("30000.0000"))
                .discountAmount(new BigDecimal("5000.0000"))
                .discountPercent(new BigDecimal("5.00"))
                .discountReason("Promo")
                .taxCode("VAT-12")
                .taxRate(new BigDecimal("12.00"))
                .taxAmount(new BigDecimal("11400.0000"))
                .lineTotal(new BigDecimal("106400.0000"))
                .isReturn(false)
                .serialNumber("SN-001")
                .batchNumber("BATCH-001")
                .locationId(50L)
                .saleUomId(6L)
                .saleQuantity(new BigDecimal("2.0000"))
                .saleUomCode("BOX")
                .saleUomName("Box")
                .notes("Line notes")
                .build();

        // When
        POSTransactionLineDto dto = posTransactionLineMapper.toDto(line);

        // Then
        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals(1L, dto.getTransactionId());
        assertEquals(1, dto.getLineNumber());
        assertEquals(10L, dto.getProductId());
        assertEquals(20L, dto.getVariantId());
        assertEquals("SKU-001", dto.getProductCode());
        assertEquals("Test Product", dto.getProductName());
        assertEquals("Red / Large", dto.getVariantName());
        assertEquals("1234567890", dto.getBarcode());
        assertEquals("PCS", dto.getUomCode());
        assertEquals("Pieces", dto.getUomName());
        assertEquals(new BigDecimal("2.0000"), dto.getQuantity());
        assertEquals(new BigDecimal("50000.0000"), dto.getUnitPrice());
        assertEquals(new BigDecimal("55000.0000"), dto.getOriginalPrice());
        assertEquals(new BigDecimal("30000.0000"), dto.getCostPrice());
        assertEquals(new BigDecimal("5000.0000"), dto.getDiscountAmount());
        assertEquals(new BigDecimal("5.00"), dto.getDiscountPercent());
        assertEquals("Promo", dto.getDiscountReason());
        assertEquals("VAT-12", dto.getTaxCode());
        assertEquals(new BigDecimal("12.00"), dto.getTaxRate());
        assertEquals(new BigDecimal("11400.0000"), dto.getTaxAmount());
        assertEquals(new BigDecimal("106400.0000"), dto.getLineTotal());
        assertFalse(dto.isReturn());
        assertEquals("SN-001", dto.getSerialNumber());
        assertEquals("BATCH-001", dto.getBatchNumber());
        assertEquals(50L, dto.getLocationId());
        assertEquals(6L, dto.getSaleUomId());
        assertEquals(new BigDecimal("2.0000"), dto.getSaleQuantity());
        assertEquals("BOX", dto.getSaleUomCode());
        assertEquals("Box", dto.getSaleUomName());
        assertEquals("Line notes", dto.getNotes());
    }

    @Test
    void toDto_withNullVariant_mapsVariantIdToNull() {
        // Given
        POSTransaction transaction = POSTransaction.builder()
                .id(1L)
                .build();

        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(5L)
                .code("PCS")
                .name("Pieces")
                .build();

        Product product = Product.builder()
                .id(10L)
                .baseUom(uom)
                .build();

        POSTransactionLine line = POSTransactionLine.builder()
                .id(100L)
                .transaction(transaction)
                .lineNumber(1)
                .product(product)
                .variant(null)
                .productName("Test Product")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("10000.0000"))
                .lineTotal(new BigDecimal("10000.0000"))
                .build();

        // When
        POSTransactionLineDto dto = posTransactionLineMapper.toDto(line);

        // Then
        assertNotNull(dto);
        assertNull(dto.getVariantId());
    }

    @Test
    void toDtoList_mapsAllLines() {
        // Given
        POSTransaction transaction = POSTransaction.builder().id(1L).build();
        UnitOfMeasure uom = UnitOfMeasure.builder().id(5L).code("PCS").name("Pieces").build();
        Product product = Product.builder().id(10L).baseUom(uom).build();

        POSTransactionLine line1 = POSTransactionLine.builder()
                .id(1L).transaction(transaction).lineNumber(1).product(product)
                .productName("Product 1").quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.TEN).lineTotal(BigDecimal.TEN).build();

        POSTransactionLine line2 = POSTransactionLine.builder()
                .id(2L).transaction(transaction).lineNumber(2).product(product)
                .productName("Product 2").quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.TEN).lineTotal(BigDecimal.TEN).build();

        // When
        List<POSTransactionLineDto> dtos = posTransactionLineMapper.toDtoList(List.of(line1, line2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
