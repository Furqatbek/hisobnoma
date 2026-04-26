package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import com.hisobnoma.platform.finance.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ARInvoiceMapperTest {

    private ARInvoiceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(ARInvoiceMapper.class);
        // The ARInvoiceMapperImpl has an @Autowired ARInvoiceLineMapper dependency;
        // inject it manually for unit testing.
        ARInvoiceLineMapper lineMapper = Mappers.getMapper(ARInvoiceLineMapper.class);
        ReflectionTestUtils.setField(mapper, "aRInvoiceLineMapper", lineMapper);
    }

    @Test
    void toDto_mapsAllFields() {
        // Given
        Customer customer = Customer.builder()
                .id(1L)
                .code("CUST-001")
                .name("Test Customer")
                .build();

        ARInvoice invoice = ARInvoice.builder()
                .invoiceNumber("INV-000001")
                .customer(customer)
                .customerName("Test Customer")
                .customerEmail("test@example.com")
                .invoiceDate(LocalDate.of(2025, 1, 15))
                .dueDate(LocalDate.of(2025, 2, 14))
                .status(ARInvoiceStatus.PENDING)
                .subtotal(new BigDecimal("1000.00"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(new BigDecimal("120.00"))
                .totalAmount(new BigDecimal("1120.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("1120.00"))
                .currency("UZS")
                .paymentTerms("Net 30")
                .paymentTermsDays(30)
                .description("Test invoice")
                .notes("Some notes")
                .billingAddress("123 Main St")
                .lines(new ArrayList<>())
                .build();
        invoice.setId(1L);
        invoice.setTenantId(1L);

        // When
        ARInvoiceDto dto = mapper.toDto(invoice);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("INV-000001", dto.getInvoiceNumber());
        assertEquals("Test Customer", dto.getCustomerName());
        assertEquals("test@example.com", dto.getCustomerEmail());
        assertEquals(LocalDate.of(2025, 1, 15), dto.getInvoiceDate());
        assertEquals(LocalDate.of(2025, 2, 14), dto.getDueDate());
        assertEquals(ARInvoiceStatus.PENDING, dto.getStatus());
        assertEquals(new BigDecimal("1000.00"), dto.getSubtotal());
        assertEquals(new BigDecimal("120.00"), dto.getTaxAmount());
        assertEquals(new BigDecimal("1120.00"), dto.getTotalAmount());
        assertEquals(BigDecimal.ZERO, dto.getPaidAmount());
        assertEquals(new BigDecimal("1120.00"), dto.getBalanceDue());
        assertEquals("UZS", dto.getCurrency());
        assertEquals("Net 30", dto.getPaymentTerms());
        assertEquals(30, dto.getPaymentTermsDays());
        assertEquals("Test invoice", dto.getDescription());
        assertEquals("Some notes", dto.getNotes());
        assertEquals("123 Main St", dto.getBillingAddress());
    }

    @Test
    void toDto_mapsOverdueFieldsCorrectly() {
        // Given - invoice that is overdue
        ARInvoice overdueInvoice = ARInvoice.builder()
                .invoiceNumber("INV-000002")
                .invoiceDate(LocalDate.now().minusDays(60))
                .dueDate(LocalDate.now().minusDays(30))
                .status(ARInvoiceStatus.PENDING)
                .subtotal(new BigDecimal("500.00"))
                .totalAmount(new BigDecimal("500.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("500.00"))
                .lines(new ArrayList<>())
                .build();
        overdueInvoice.setId(2L);

        // When
        ARInvoiceDto dto = mapper.toDto(overdueInvoice);

        // Then
        assertNotNull(dto);
        assertTrue(dto.isOverdue());
        assertTrue(dto.getDaysOverdue() > 0);
    }

    @Test
    void toDtoWithoutLines_excludesLines() {
        // Given
        ARInvoice invoice = ARInvoice.builder()
                .invoiceNumber("INV-000003")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(ARInvoiceStatus.DRAFT)
                .subtotal(new BigDecimal("200.00"))
                .totalAmount(new BigDecimal("200.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("200.00"))
                .lines(new ArrayList<>())
                .build();
        invoice.setId(3L);

        // When
        ARInvoiceDto dto = mapper.toDtoWithoutLines(invoice);

        // Then
        assertNotNull(dto);
        assertEquals(3L, dto.getId());
        assertEquals("INV-000003", dto.getInvoiceNumber());
        assertNull(dto.getLines());
    }

    @Test
    void toDto_handlesNonOverdueInvoice() {
        // Given - invoice that is not overdue
        ARInvoice invoice = ARInvoice.builder()
                .invoiceNumber("INV-000004")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(ARInvoiceStatus.PENDING)
                .subtotal(new BigDecimal("300.00"))
                .totalAmount(new BigDecimal("300.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("300.00"))
                .lines(new ArrayList<>())
                .build();
        invoice.setId(4L);

        // When
        ARInvoiceDto dto = mapper.toDto(invoice);

        // Then
        assertNotNull(dto);
        assertFalse(dto.isOverdue());
        assertEquals(0, dto.getDaysOverdue());
        assertTrue(dto.getDaysUntilDue() > 0);
    }

    @Test
    void toDto_handlesPaidInvoice() {
        // Given - fully paid invoice
        ARInvoice invoice = ARInvoice.builder()
                .invoiceNumber("INV-000005")
                .invoiceDate(LocalDate.now().minusDays(45))
                .dueDate(LocalDate.now().minusDays(15))
                .status(ARInvoiceStatus.PAID)
                .subtotal(new BigDecimal("500.00"))
                .totalAmount(new BigDecimal("500.00"))
                .paidAmount(new BigDecimal("500.00"))
                .balanceDue(BigDecimal.ZERO)
                .lines(new ArrayList<>())
                .build();
        invoice.setId(5L);

        // When
        ARInvoiceDto dto = mapper.toDto(invoice);

        // Then
        assertNotNull(dto);
        assertFalse(dto.isOverdue());
        assertEquals(0, dto.getDaysOverdue());
        assertEquals(ARInvoiceStatus.PAID, dto.getStatus());
    }
}
