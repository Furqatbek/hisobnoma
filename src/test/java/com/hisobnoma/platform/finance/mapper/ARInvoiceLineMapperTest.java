package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.ARInvoiceLineDto;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceLineRequest;
import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.entity.ARInvoiceLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ARInvoiceLineMapperTest {

    @Autowired
    private ARInvoiceLineMapper arInvoiceLineMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        ARInvoice arInvoice = ARInvoice.builder().id(10L).build();

        ARInvoiceLine line = ARInvoiceLine.builder()
                .id(1L)
                .arInvoice(arInvoice)
                .lineNumber(1)
                .itemId(40L)
                .productId(50L)
                .productSku("SKU-001")
                .productName("Test Product")
                .description("Invoice line description")
                .revenueAccountId(100L)
                .revenueAccountCode("4000")
                .quantity(new BigDecimal("10.0000"))
                .unitOfMeasure("PCS")
                .unitPrice(new BigDecimal("8000.0000"))
                .unitCost(new BigDecimal("5000.0000"))
                .discountPercent(new BigDecimal("10.00"))
                .discountAmount(new BigDecimal("8000.0000"))
                .lineTotal(new BigDecimal("72000.0000"))
                .taxCode("VAT-12")
                .taxRate(new BigDecimal("12.00"))
                .taxAmount(new BigDecimal("8640.0000"))
                .posLineId(200L)
                .salesOrderLineId(300L)
                .notes("Line notes")
                .build();

        // When
        ARInvoiceLineDto dto = arInvoiceLineMapper.toDto(line);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getArInvoiceId());
        assertEquals(1, dto.getLineNumber());
        assertEquals(50L, dto.getProductId());
        assertEquals("SKU-001", dto.getProductSku());
        assertEquals("Test Product", dto.getProductName());
        assertEquals("Invoice line description", dto.getDescription());
        assertEquals(100L, dto.getRevenueAccountId());
        assertEquals("4000", dto.getRevenueAccountCode());
        assertEquals(new BigDecimal("10.0000"), dto.getQuantity());
        assertEquals("PCS", dto.getUnitOfMeasure());
        assertEquals(new BigDecimal("8000.0000"), dto.getUnitPrice());
        assertEquals(new BigDecimal("5000.0000"), dto.getUnitCost());
        assertEquals(new BigDecimal("10.00"), dto.getDiscountPercent());
        assertEquals(new BigDecimal("8000.0000"), dto.getDiscountAmount());
        assertEquals(new BigDecimal("72000.0000"), dto.getLineTotal());
        assertEquals("VAT-12", dto.getTaxCode());
        assertEquals(new BigDecimal("12.00"), dto.getTaxRate());
        assertEquals(new BigDecimal("8640.0000"), dto.getTaxAmount());
        assertEquals(200L, dto.getPosLineId());
        assertEquals(300L, dto.getSalesOrderLineId());
        assertEquals("Line notes", dto.getNotes());
        // profitMargin and profitMarginPercent are computed via expression
        assertNotNull(dto.getProfitMargin());
        assertNotNull(dto.getProfitMarginPercent());
    }

    @Test
    void toDto_withNullUnitCost_mapsMarginToNull() {
        // Given
        ARInvoice arInvoice = ARInvoice.builder().id(10L).build();

        ARInvoiceLine line = ARInvoiceLine.builder()
                .id(1L)
                .arInvoice(arInvoice)
                .lineNumber(1)
                .description("No cost line")
                .quantity(new BigDecimal("1.0000"))
                .unitPrice(new BigDecimal("10000.0000"))
                .unitCost(null)
                .lineTotal(new BigDecimal("10000.0000"))
                .build();

        // When
        ARInvoiceLineDto dto = arInvoiceLineMapper.toDto(line);

        // Then
        assertNotNull(dto);
        assertNull(dto.getProfitMargin());
        assertNull(dto.getProfitMarginPercent());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreateARInvoiceLineRequest request = CreateARInvoiceLineRequest.builder()
                .itemId(40L)
                .productId(50L)
                .productSku("SKU-002")
                .productName("New Product")
                .description("New line")
                .revenueAccountId(100L)
                .revenueAccountCode("4000")
                .quantity(new BigDecimal("5.0000"))
                .unitOfMeasure("KG")
                .unitPrice(new BigDecimal("15000.0000"))
                .unitCost(new BigDecimal("8000.0000"))
                .discountPercent(new BigDecimal("5.00"))
                .taxCode("VAT-12")
                .taxRate(new BigDecimal("12.00"))
                .posLineId(200L)
                .salesOrderLineId(300L)
                .notes("New line notes")
                .build();

        // When
        ARInvoiceLine entity = arInvoiceLineMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(40L, entity.getItemId());
        assertEquals(50L, entity.getProductId());
        assertEquals("SKU-002", entity.getProductSku());
        assertEquals("New Product", entity.getProductName());
        assertEquals("New line", entity.getDescription());
        assertEquals(100L, entity.getRevenueAccountId());
        assertEquals("4000", entity.getRevenueAccountCode());
        assertEquals(new BigDecimal("5.0000"), entity.getQuantity());
        assertEquals("KG", entity.getUnitOfMeasure());
        assertEquals(new BigDecimal("15000.0000"), entity.getUnitPrice());
        assertEquals(new BigDecimal("8000.0000"), entity.getUnitCost());
        assertEquals(new BigDecimal("5.00"), entity.getDiscountPercent());
        assertEquals("VAT-12", entity.getTaxCode());
        assertEquals(new BigDecimal("12.00"), entity.getTaxRate());
        assertEquals(200L, entity.getPosLineId());
        assertEquals(300L, entity.getSalesOrderLineId());
        assertEquals("New line notes", entity.getNotes());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getArInvoice());
        assertNull(entity.getLineNumber());
    }

    @Test
    void toDtoList_mapsAllLines() {
        // Given
        ARInvoice inv = ARInvoice.builder().id(10L).build();

        ARInvoiceLine l1 = ARInvoiceLine.builder()
                .id(1L).arInvoice(inv).lineNumber(1).description("Line 1")
                .quantity(BigDecimal.ONE).unitPrice(BigDecimal.TEN)
                .lineTotal(BigDecimal.TEN).build();
        ARInvoiceLine l2 = ARInvoiceLine.builder()
                .id(2L).arInvoice(inv).lineNumber(2).description("Line 2")
                .quantity(BigDecimal.ONE).unitPrice(BigDecimal.TEN)
                .lineTotal(BigDecimal.TEN).build();

        // When
        List<ARInvoiceLineDto> dtos = arInvoiceLineMapper.toDtoList(List.of(l1, l2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
