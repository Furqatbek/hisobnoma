package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.APInvoiceLineDto;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceLineRequest;
import com.hisobnoma.platform.finance.entity.APInvoice;
import com.hisobnoma.platform.finance.entity.APInvoiceLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class APInvoiceLineMapperTest {

    @Autowired
    private APInvoiceLineMapper apInvoiceLineMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        APInvoice apInvoice = APInvoice.builder().id(10L).build();

        APInvoiceLine line = APInvoiceLine.builder()
                .id(1L)
                .apInvoice(apInvoice)
                .lineNumber(1)
                .productId(50L)
                .productSku("SKU-001")
                .productName("Test Product")
                .description("Invoice line description")
                .expenseAccountId(100L)
                .expenseAccountCode("5000")
                .quantity(new BigDecimal("10.0000"))
                .unitOfMeasure("PCS")
                .unitPrice(new BigDecimal("5000.0000"))
                .discountPercent(new BigDecimal("5.00"))
                .discountAmount(new BigDecimal("2500.0000"))
                .lineTotal(new BigDecimal("47500.0000"))
                .taxCode("VAT-12")
                .taxRate(new BigDecimal("12.00"))
                .taxAmount(new BigDecimal("5700.0000"))
                .purchaseOrderLineId(200L)
                .receivingLineId(300L)
                .orderedQuantity(new BigDecimal("10.0000"))
                .receivedQuantity(new BigDecimal("10.0000"))
                .varianceQuantity(BigDecimal.ZERO)
                .varianceAmount(BigDecimal.ZERO)
                .notes("Line notes")
                .build();

        // When
        APInvoiceLineDto dto = apInvoiceLineMapper.toDto(line);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getApInvoiceId());
        assertEquals(1, dto.getLineNumber());
        assertEquals(50L, dto.getProductId());
        assertEquals("SKU-001", dto.getProductSku());
        assertEquals("Test Product", dto.getProductName());
        assertEquals("Invoice line description", dto.getDescription());
        assertEquals(100L, dto.getExpenseAccountId());
        assertEquals("5000", dto.getExpenseAccountCode());
        assertEquals(new BigDecimal("10.0000"), dto.getQuantity());
        assertEquals("PCS", dto.getUnitOfMeasure());
        assertEquals(new BigDecimal("5000.0000"), dto.getUnitPrice());
        assertEquals(new BigDecimal("5.00"), dto.getDiscountPercent());
        assertEquals(new BigDecimal("2500.0000"), dto.getDiscountAmount());
        assertEquals(new BigDecimal("47500.0000"), dto.getLineTotal());
        assertEquals("VAT-12", dto.getTaxCode());
        assertEquals(new BigDecimal("12.00"), dto.getTaxRate());
        assertEquals(new BigDecimal("5700.0000"), dto.getTaxAmount());
        assertEquals(200L, dto.getPurchaseOrderLineId());
        assertEquals(300L, dto.getReceivingLineId());
        assertEquals(new BigDecimal("10.0000"), dto.getOrderedQuantity());
        assertEquals(new BigDecimal("10.0000"), dto.getReceivedQuantity());
        assertEquals(BigDecimal.ZERO, dto.getVarianceQuantity());
        assertEquals(BigDecimal.ZERO, dto.getVarianceAmount());
        assertEquals("Line notes", dto.getNotes());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreateAPInvoiceLineRequest request = CreateAPInvoiceLineRequest.builder()
                .productId(50L)
                .description("New line")
                .expenseAccountId(100L)
                .quantity(new BigDecimal("5.0000"))
                .unitOfMeasure("KG")
                .unitPrice(new BigDecimal("10000.0000"))
                .discountPercent(new BigDecimal("10.00"))
                .discountAmount(new BigDecimal("5000.0000"))
                .taxCode("VAT-12")
                .taxRate(new BigDecimal("12.00"))
                .purchaseOrderLineId(200L)
                .receivingLineId(300L)
                .notes("New line notes")
                .build();

        // When
        APInvoiceLine entity = apInvoiceLineMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("New line", entity.getDescription());
        assertEquals(100L, entity.getExpenseAccountId());
        assertEquals(new BigDecimal("5.0000"), entity.getQuantity());
        assertEquals("KG", entity.getUnitOfMeasure());
        assertEquals(new BigDecimal("10000.0000"), entity.getUnitPrice());
        assertEquals(new BigDecimal("10.00"), entity.getDiscountPercent());
        assertEquals(new BigDecimal("5000.0000"), entity.getDiscountAmount());
        assertEquals("VAT-12", entity.getTaxCode());
        assertEquals(new BigDecimal("12.00"), entity.getTaxRate());
        assertEquals(200L, entity.getPurchaseOrderLineId());
        assertEquals(300L, entity.getReceivingLineId());
        assertEquals("New line notes", entity.getNotes());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getApInvoice());
        assertNull(entity.getLineNumber());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        APInvoice apInvoice = APInvoice.builder().id(10L).build();

        APInvoiceLine existing = APInvoiceLine.builder()
                .id(1L)
                .apInvoice(apInvoice)
                .lineNumber(1)
                .description("Old description")
                .quantity(new BigDecimal("5.0000"))
                .unitPrice(new BigDecimal("10000.0000"))
                .build();

        CreateAPInvoiceLineRequest request = CreateAPInvoiceLineRequest.builder()
                .description("Updated description")
                .quantity(new BigDecimal("10.0000"))
                .unitPrice(new BigDecimal("12000.0000"))
                .notes("Updated notes")
                .build();

        // When
        apInvoiceLineMapper.updateEntity(request, existing);

        // Then
        assertEquals("Updated description", existing.getDescription());
        assertEquals(new BigDecimal("10.0000"), existing.getQuantity());
        assertEquals(new BigDecimal("12000.0000"), existing.getUnitPrice());
        assertEquals("Updated notes", existing.getNotes());
        assertEquals(1L, existing.getId());
        assertNotNull(existing.getApInvoice());
        assertEquals(1, existing.getLineNumber());
    }

    @Test
    void toDtoList_mapsAllLines() {
        // Given
        APInvoice inv = APInvoice.builder().id(10L).build();

        APInvoiceLine l1 = APInvoiceLine.builder()
                .id(1L).apInvoice(inv).lineNumber(1).description("Line 1")
                .quantity(BigDecimal.ONE).unitPrice(BigDecimal.TEN).build();
        APInvoiceLine l2 = APInvoiceLine.builder()
                .id(2L).apInvoice(inv).lineNumber(2).description("Line 2")
                .quantity(BigDecimal.ONE).unitPrice(BigDecimal.TEN).build();

        // When
        List<APInvoiceLineDto> dtos = apInvoiceLineMapper.toDtoList(List.of(l1, l2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
