package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.APInvoiceDto;
import com.hisobnoma.platform.finance.dto.APInvoiceLineDto;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceRequest;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceLineRequest;
import com.hisobnoma.platform.finance.entity.APInvoice;
import com.hisobnoma.platform.finance.entity.APInvoiceLine;
import com.hisobnoma.platform.finance.entity.APInvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class APInvoiceMapperTest {

    private APInvoiceMapper mapper;
    private APInvoiceLineMapper lineMapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(APInvoiceMapper.class);
        lineMapper = Mappers.getMapper(APInvoiceLineMapper.class);
    }

    // ========== toDto ==========

    @Test
    void toDto_mapsAllFields() {
        // Given
        APInvoiceLine line = APInvoiceLine.builder()
                .lineNumber(1)
                .description("Office Supplies")
                .quantity(new BigDecimal("10.0000"))
                .unitPrice(new BigDecimal("25.0000"))
                .lineTotal(new BigDecimal("250.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .build();
        line.setId(1L);

        APInvoice invoice = APInvoice.builder()
                .invoiceNumber("AP-000001")
                .vendorInvoiceNumber("VINV-001")
                .vendorId(1L)
                .vendorName("Test Vendor")
                .invoiceDate(LocalDate.of(2025, 3, 15))
                .dueDate(LocalDate.of(2025, 4, 14))
                .status(APInvoiceStatus.DRAFT)
                .purchaseOrderId(10L)
                .purchaseOrderNumber("PO-000001")
                .receivingOrderId(20L)
                .receivingOrderNumber("RCV-000001")
                .subtotal(new BigDecimal("250.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("250.0000"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("250.0000"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .paymentTerms("Net 30")
                .paymentTermsDays(30)
                .expenseAccountId(100L)
                .apAccountId(200L)
                .description("Test invoice")
                .notes("Some notes")
                .matchingStatus("MATCHED")
                .matchingNotes("All lines matched successfully")
                .glPosted(false)
                .lines(new ArrayList<>(List.of(line)))
                .build();
        invoice.setId(1L);
        invoice.setTenantId(1L);

        // When
        APInvoiceDto dto = mapper.toDto(invoice);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(1L, dto.getTenantId());
        assertEquals("AP-000001", dto.getInvoiceNumber());
        assertEquals("VINV-001", dto.getVendorInvoiceNumber());
        assertEquals(1L, dto.getVendorId());
        assertEquals("Test Vendor", dto.getVendorName());
        assertEquals(LocalDate.of(2025, 3, 15), dto.getInvoiceDate());
        assertEquals(LocalDate.of(2025, 4, 14), dto.getDueDate());
        assertEquals(APInvoiceStatus.DRAFT, dto.getStatus());
        assertEquals(10L, dto.getPurchaseOrderId());
        assertEquals("PO-000001", dto.getPurchaseOrderNumber());
        assertEquals(20L, dto.getReceivingOrderId());
        assertEquals("RCV-000001", dto.getReceivingOrderNumber());
        assertEquals(new BigDecimal("250.0000"), dto.getSubtotal());
        assertEquals(BigDecimal.ZERO, dto.getDiscountAmount());
        assertEquals(BigDecimal.ZERO, dto.getTaxAmount());
        assertEquals(BigDecimal.ZERO, dto.getShippingAmount());
        assertEquals(new BigDecimal("250.0000"), dto.getTotalAmount());
        assertEquals(BigDecimal.ZERO, dto.getPaidAmount());
        assertEquals(new BigDecimal("250.0000"), dto.getBalanceDue());
        assertEquals("UZS", dto.getCurrency());
        assertEquals(BigDecimal.ONE, dto.getExchangeRate());
        assertEquals("Net 30", dto.getPaymentTerms());
        assertEquals(30, dto.getPaymentTermsDays());
        assertEquals(100L, dto.getExpenseAccountId());
        assertEquals(200L, dto.getApAccountId());
        assertEquals("Test invoice", dto.getDescription());
        assertEquals("Some notes", dto.getNotes());
        assertEquals("MATCHED", dto.getMatchingStatus());
        assertFalse(dto.isGlPosted());
        // lines should be mapped
        assertNotNull(dto.getLines());
    }

    @Test
    void toDto_mapsOverdueFieldsCorrectly() {
        // Given - invoice that is overdue
        APInvoice overdueInvoice = APInvoice.builder()
                .invoiceNumber("AP-000002")
                .invoiceDate(LocalDate.now().minusDays(60))
                .dueDate(LocalDate.now().minusDays(30))
                .status(APInvoiceStatus.APPROVED)
                .subtotal(new BigDecimal("500.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("500.0000"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("500.0000"))
                .lines(new ArrayList<>())
                .build();
        overdueInvoice.setId(2L);

        // When
        APInvoiceDto dto = mapper.toDto(overdueInvoice);

        // Then
        assertNotNull(dto);
        assertTrue(dto.isOverdue());
        assertTrue(dto.getDaysOverdue() > 0);
    }

    @Test
    void toDto_handlesNonOverdueInvoice() {
        // Given - invoice that is not overdue
        APInvoice invoice = APInvoice.builder()
                .invoiceNumber("AP-000003")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(APInvoiceStatus.APPROVED)
                .subtotal(new BigDecimal("300.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("300.0000"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("300.0000"))
                .lines(new ArrayList<>())
                .build();
        invoice.setId(3L);

        // When
        APInvoiceDto dto = mapper.toDto(invoice);

        // Then
        assertNotNull(dto);
        assertFalse(dto.isOverdue());
        assertEquals(0, dto.getDaysOverdue());
        assertTrue(dto.getDaysUntilDue() > 0);
    }

    @Test
    void toDto_handlesPaidInvoice() {
        // Given - fully paid invoice past due date should not be overdue
        APInvoice invoice = APInvoice.builder()
                .invoiceNumber("AP-000004")
                .invoiceDate(LocalDate.now().minusDays(45))
                .dueDate(LocalDate.now().minusDays(15))
                .status(APInvoiceStatus.PAID)
                .subtotal(new BigDecimal("500.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("500.0000"))
                .paidAmount(new BigDecimal("500.0000"))
                .balanceDue(BigDecimal.ZERO)
                .lines(new ArrayList<>())
                .build();
        invoice.setId(4L);

        // When
        APInvoiceDto dto = mapper.toDto(invoice);

        // Then
        assertNotNull(dto);
        assertFalse(dto.isOverdue());
        assertEquals(0, dto.getDaysOverdue());
        assertEquals(APInvoiceStatus.PAID, dto.getStatus());
    }

    // ========== toDtoWithoutLines ==========

    @Test
    void toDtoWithoutLines_excludesLines() {
        // Given
        APInvoiceLine line = APInvoiceLine.builder()
                .lineNumber(1)
                .description("Item")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("100.0000"))
                .lineTotal(new BigDecimal("100.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .build();

        APInvoice invoice = APInvoice.builder()
                .invoiceNumber("AP-000005")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(APInvoiceStatus.DRAFT)
                .subtotal(new BigDecimal("100.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100.0000"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("100.0000"))
                .lines(new ArrayList<>(List.of(line)))
                .build();
        invoice.setId(5L);

        // When
        APInvoiceDto dto = mapper.toDtoWithoutLines(invoice);

        // Then
        assertNotNull(dto);
        assertEquals(5L, dto.getId());
        assertEquals("AP-000005", dto.getInvoiceNumber());
        assertNull(dto.getLines());
    }

    // ========== toEntity (fromCreateRequest) ==========

    @Test
    void toEntity_fromCreateRequest() {
        // Given
        CreateAPInvoiceRequest request = CreateAPInvoiceRequest.builder()
                .vendorId(1L)
                .vendorInvoiceNumber("VINV-001")
                .invoiceDate(LocalDate.of(2025, 3, 15))
                .dueDate(LocalDate.of(2025, 4, 14))
                .discountAmount(new BigDecimal("10.00"))
                .taxAmount(new BigDecimal("25.00"))
                .shippingAmount(new BigDecimal("5.00"))
                .currency("USD")
                .exchangeRate(new BigDecimal("12500.0000"))
                .paymentTerms("Net 30")
                .paymentTermsDays(30)
                .expenseAccountId(100L)
                .apAccountId(200L)
                .description("Test invoice description")
                .notes("Test notes")
                .purchaseOrderId(10L)
                .receivingOrderId(20L)
                .lines(new ArrayList<>())
                .build();

        // When
        APInvoice entity = mapper.toEntity(request);

        // Then
        assertNotNull(entity);
        // id, tenantId, invoiceNumber, status should be ignored (null/default)
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getInvoiceNumber());
        assertNull(entity.getVendorName()); // ignored
        // mapped fields
        assertEquals(1L, entity.getVendorId());
        assertEquals("VINV-001", entity.getVendorInvoiceNumber());
        assertEquals(LocalDate.of(2025, 3, 15), entity.getInvoiceDate());
        assertEquals(LocalDate.of(2025, 4, 14), entity.getDueDate());
        assertEquals(new BigDecimal("10.00"), entity.getDiscountAmount());
        assertEquals(new BigDecimal("25.00"), entity.getTaxAmount());
        assertEquals(new BigDecimal("5.00"), entity.getShippingAmount());
        assertEquals("USD", entity.getCurrency());
        assertEquals(new BigDecimal("12500.0000"), entity.getExchangeRate());
        assertEquals("Net 30", entity.getPaymentTerms());
        assertEquals(30, entity.getPaymentTermsDays());
        assertEquals(100L, entity.getExpenseAccountId());
        assertEquals(200L, entity.getApAccountId());
        assertEquals("Test invoice description", entity.getDescription());
        assertEquals("Test notes", entity.getNotes());
        assertEquals(10L, entity.getPurchaseOrderId());
        assertEquals(20L, entity.getReceivingOrderId());
    }

    // ========== APInvoiceLineMapper tests ==========

    @Test
    void lineMapper_toDto_mapsAllFields() {
        // Given
        APInvoice parentInvoice = APInvoice.builder()
                .invoiceNumber("AP-000001")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(APInvoiceStatus.DRAFT)
                .subtotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(BigDecimal.ZERO)
                .lines(new ArrayList<>())
                .build();
        parentInvoice.setId(1L);

        APInvoiceLine line = APInvoiceLine.builder()
                .apInvoice(parentInvoice)
                .lineNumber(1)
                .productId(50L)
                .productSku("SKU-001")
                .productName("Office Paper")
                .description("A4 Paper")
                .expenseAccountId(100L)
                .expenseAccountCode("5100")
                .quantity(new BigDecimal("20.0000"))
                .unitOfMeasure("BOX")
                .unitPrice(new BigDecimal("15.0000"))
                .discountPercent(new BigDecimal("5.00"))
                .discountAmount(new BigDecimal("15.0000"))
                .lineTotal(new BigDecimal("285.0000"))
                .taxCode("VAT")
                .taxRate(new BigDecimal("12.00"))
                .taxAmount(new BigDecimal("34.2000"))
                .purchaseOrderLineId(30L)
                .receivingLineId(40L)
                .orderedQuantity(new BigDecimal("20.0000"))
                .receivedQuantity(new BigDecimal("20.0000"))
                .varianceQuantity(BigDecimal.ZERO)
                .varianceAmount(BigDecimal.ZERO)
                .notes("Test line notes")
                .build();
        line.setId(10L);

        // When
        APInvoiceLineDto dto = lineMapper.toDto(line);

        // Then
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals(1L, dto.getApInvoiceId());
        assertEquals(1, dto.getLineNumber());
        assertEquals(50L, dto.getProductId());
        assertEquals("SKU-001", dto.getProductSku());
        assertEquals("Office Paper", dto.getProductName());
        assertEquals("A4 Paper", dto.getDescription());
        assertEquals(100L, dto.getExpenseAccountId());
        assertEquals("5100", dto.getExpenseAccountCode());
        assertEquals(new BigDecimal("20.0000"), dto.getQuantity());
        assertEquals("BOX", dto.getUnitOfMeasure());
        assertEquals(new BigDecimal("15.0000"), dto.getUnitPrice());
        assertEquals(new BigDecimal("5.00"), dto.getDiscountPercent());
        assertEquals(new BigDecimal("15.0000"), dto.getDiscountAmount());
        assertEquals(new BigDecimal("285.0000"), dto.getLineTotal());
        assertEquals("VAT", dto.getTaxCode());
        assertEquals(new BigDecimal("12.00"), dto.getTaxRate());
        assertEquals(new BigDecimal("34.2000"), dto.getTaxAmount());
        assertEquals(30L, dto.getPurchaseOrderLineId());
        assertEquals(40L, dto.getReceivingLineId());
        assertEquals(new BigDecimal("20.0000"), dto.getOrderedQuantity());
        assertEquals(new BigDecimal("20.0000"), dto.getReceivedQuantity());
        assertEquals(BigDecimal.ZERO, dto.getVarianceQuantity());
        assertEquals(BigDecimal.ZERO, dto.getVarianceAmount());
        assertEquals("Test line notes", dto.getNotes());
    }

    @Test
    void lineMapper_toEntity_fromCreateRequest() {
        // Given
        CreateAPInvoiceLineRequest request = CreateAPInvoiceLineRequest.builder()
                .productId(50L)
                .description("A4 Paper")
                .expenseAccountId(100L)
                .quantity(new BigDecimal("20.0000"))
                .unitOfMeasure("BOX")
                .unitPrice(new BigDecimal("15.0000"))
                .discountPercent(new BigDecimal("5.00"))
                .discountAmount(new BigDecimal("15.0000"))
                .taxCode("VAT")
                .taxRate(new BigDecimal("12.00"))
                .purchaseOrderLineId(30L)
                .receivingLineId(40L)
                .notes("Test notes")
                .build();

        // When
        APInvoiceLine entity = lineMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertNull(entity.getId());
        assertNull(entity.getApInvoice());
        // mapped fields
        assertEquals(50L, entity.getProductId());
        assertEquals("A4 Paper", entity.getDescription());
        assertEquals(100L, entity.getExpenseAccountId());
        assertEquals(new BigDecimal("20.0000"), entity.getQuantity());
        assertEquals("BOX", entity.getUnitOfMeasure());
        assertEquals(new BigDecimal("15.0000"), entity.getUnitPrice());
        assertEquals(new BigDecimal("5.00"), entity.getDiscountPercent());
        assertEquals(new BigDecimal("15.0000"), entity.getDiscountAmount());
        assertEquals("VAT", entity.getTaxCode());
        assertEquals(new BigDecimal("12.00"), entity.getTaxRate());
        assertEquals(30L, entity.getPurchaseOrderLineId());
        assertEquals(40L, entity.getReceivingLineId());
        assertEquals("Test notes", entity.getNotes());
    }

    // ========== toDtoList ==========

    @Test
    void toDtoList_mapsMultipleInvoices() {
        // Given
        APInvoice invoice1 = APInvoice.builder()
                .invoiceNumber("AP-000001")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(APInvoiceStatus.DRAFT)
                .subtotal(new BigDecimal("100.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100.0000"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("100.0000"))
                .lines(new ArrayList<>())
                .build();
        invoice1.setId(1L);

        APInvoice invoice2 = APInvoice.builder()
                .invoiceNumber("AP-000002")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(APInvoiceStatus.APPROVED)
                .subtotal(new BigDecimal("200.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("200.0000"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("200.0000"))
                .lines(new ArrayList<>())
                .build();
        invoice2.setId(2L);

        // When
        List<APInvoiceDto> dtos = mapper.toDtoList(List.of(invoice1, invoice2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("AP-000001", dtos.get(0).getInvoiceNumber());
        assertEquals("AP-000002", dtos.get(1).getInvoiceNumber());
        // toDtoList uses toDtoWithoutLines, so lines should be null
        assertNull(dtos.get(0).getLines());
        assertNull(dtos.get(1).getLines());
    }
}
