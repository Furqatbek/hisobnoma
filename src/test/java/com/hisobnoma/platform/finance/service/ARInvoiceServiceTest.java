package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceLineRequest;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.mapper.ARInvoiceMapper;
import com.hisobnoma.platform.finance.repository.ARInvoiceLineRepository;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ARInvoiceServiceTest {

    @Mock
    private ARInvoiceRepository arInvoiceRepository;

    @Mock
    private ARInvoiceLineRepository arInvoiceLineRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ARInvoiceMapper arInvoiceMapper;

    @Mock
    private CustomerService customerService;

    @Mock
    private GLIntegrationService glIntegrationService;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ARInvoiceService arInvoiceService;

    private Customer customer;
    private ARInvoice invoice;
    private ARInvoiceDto invoiceDto;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .code("CUST-000001")
                .name("Test Customer")
                .email("test@example.com")
                .tenantId(TENANT_ID)
                .active(true)
                .currentBalance(BigDecimal.ZERO)
                .creditLimit(new BigDecimal("50000.00"))
                .creditHold(false)
                .paymentTermsDays(30)
                .build();

        invoice = ARInvoice.builder()
                .id(1L)
                .invoiceNumber("INV-000001")
                .customer(customer)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(ARInvoiceStatus.DRAFT)
                .subtotal(new BigDecimal("1000.00"))
                .totalAmount(new BigDecimal("1000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("1000.00"))
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .tenantId(TENANT_ID)
                .lines(new ArrayList<>())
                .build();

        invoiceDto = ARInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("INV-000001")
                .customerId(1L)
                .customerName("Test Customer")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(ARInvoiceStatus.DRAFT)
                .totalAmount(new BigDecimal("1000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void shouldGetInvoicesSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ARInvoice> page = new PageImpl<>(List.of(invoice), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(arInvoiceMapper.toDto(invoice)).thenReturn(invoiceDto);

        // When
        PageResponse<ARInvoiceDto> result = arInvoiceService.getInvoices(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("INV-000001", result.getContent().get(0).getInvoiceNumber());
        verify(arInvoiceRepository).findAllByTenantId(TENANT_ID, pageable);
    }

    @Test
    void shouldGetInvoiceSuccessfully() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(arInvoiceMapper.toDto(invoice)).thenReturn(invoiceDto);

        // When
        ARInvoiceDto result = arInvoiceService.getInvoice(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("INV-000001", result.getInvoiceNumber());
    }

    @Test
    void shouldThrowNotFoundWhenInvoiceDoesNotExist() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> arInvoiceService.getInvoice(999L));
    }

    @Test
    void shouldCreateInvoiceSuccessfully() {
        // Given
        CreateARInvoiceLineRequest lineRequest = CreateARInvoiceLineRequest.builder()
                .description("Test Item")
                .quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("500.00"))
                .build();

        CreateARInvoiceRequest request = CreateARInvoiceRequest.builder()
                .customerId(1L)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .totalAmount(new BigDecimal("1000.00"))
                .paymentTerms(30)
                .lines(List.of(lineRequest))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerService.canBeInvoiced(eq(1L), any())).thenReturn(true);
        when(arInvoiceRepository.findMaxInvoiceNumber(TENANT_ID)).thenReturn(null);
        when(arInvoiceMapper.toEntity(any())).thenReturn(invoice);
        when(arInvoiceRepository.save(any())).thenReturn(invoice);
        when(arInvoiceMapper.toDto(invoice)).thenReturn(invoiceDto);

        // When
        ARInvoiceDto result = arInvoiceService.createInvoice(request);

        // Then
        assertNotNull(result);
        assertEquals("INV-000001", result.getInvoiceNumber());
        verify(arInvoiceRepository).save(any());
    }

    @Test
    void shouldThrowNotFoundWhenCustomerNotFoundOnCreate() {
        // Given
        CreateARInvoiceRequest request = CreateARInvoiceRequest.builder()
                .customerId(999L)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .totalAmount(new BigDecimal("1000.00"))
                .lines(List.of(CreateARInvoiceLineRequest.builder()
                        .description("Test")
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("1000.00"))
                        .build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> arInvoiceService.createInvoice(request));
        verify(arInvoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenCreditLimitExceeded() {
        // Given
        CreateARInvoiceRequest request = CreateARInvoiceRequest.builder()
                .customerId(1L)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .totalAmount(new BigDecimal("100000.00"))
                .lines(List.of(CreateARInvoiceLineRequest.builder()
                        .description("Test")
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("100000.00"))
                        .build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerService.canBeInvoiced(eq(1L), any())).thenReturn(false);

        // When/Then
        assertThrows(BusinessException.class, () -> arInvoiceService.createInvoice(request));
        verify(arInvoiceRepository, never()).save(any());
    }

    @Test
    void shouldPostInvoiceSuccessfully() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(arInvoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(arInvoiceMapper.toDto(any(ARInvoice.class))).thenReturn(
                ARInvoiceDto.builder()
                        .id(1L)
                        .invoiceNumber("INV-000001")
                        .status(ARInvoiceStatus.PENDING)
                        .build());

        // When
        ARInvoiceDto result = arInvoiceService.postInvoice(1L);

        // Then
        assertNotNull(result);
        assertEquals(ARInvoiceStatus.PENDING, result.getStatus());
        verify(glIntegrationService).postARInvoice(invoice);
        verify(customerService).updateCustomerBalance(any(Customer.class), any());
    }

    @Test
    void shouldFailToPostNonDraftInvoice() {
        // Given
        invoice.setStatus(ARInvoiceStatus.PENDING);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> arInvoiceService.postInvoice(1L));
        verify(glIntegrationService, never()).postARInvoice(any());
    }

    @Test
    void shouldCancelInvoiceSuccessfully() {
        // Given
        invoice.setStatus(ARInvoiceStatus.DRAFT);
        invoice.setPaidAmount(BigDecimal.ZERO);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(arInvoiceRepository.save(any())).thenReturn(invoice);
        when(arInvoiceMapper.toDto(any(ARInvoice.class))).thenReturn(
                ARInvoiceDto.builder()
                        .id(1L)
                        .status(ARInvoiceStatus.CANCELLED)
                        .build());

        // When
        ARInvoiceDto result = arInvoiceService.cancelInvoice(1L, "No longer needed");

        // Then
        assertNotNull(result);
        assertEquals(ARInvoiceStatus.CANCELLED, result.getStatus());
        verify(arInvoiceRepository).save(any());
    }

    @Test
    void shouldFailToCancelInvoiceWithPayments() {
        // Given
        invoice.setPaidAmount(new BigDecimal("500.00"));
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> arInvoiceService.cancelInvoice(1L, "Cancel reason"));
        verify(arInvoiceRepository, never()).save(any());
    }

    @Test
    void shouldFailToCancelAlreadyCancelledInvoice() {
        // Given
        invoice.setStatus(ARInvoiceStatus.CANCELLED);
        invoice.setPaidAmount(BigDecimal.ZERO);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> arInvoiceService.cancelInvoice(1L, "Cancel again"));
    }

    @Test
    void shouldCancelPostedInvoiceAndReverseGL() {
        // Given
        invoice.setStatus(ARInvoiceStatus.PENDING);
        invoice.setPaidAmount(BigDecimal.ZERO);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(arInvoiceRepository.save(any())).thenReturn(invoice);
        when(arInvoiceMapper.toDto(any(ARInvoice.class))).thenReturn(
                ARInvoiceDto.builder().id(1L).status(ARInvoiceStatus.CANCELLED).build());

        // When
        ARInvoiceDto result = arInvoiceService.cancelInvoice(1L, "Reverse it");

        // Then
        assertNotNull(result);
        verify(glIntegrationService).reverseARInvoice(eq(invoice), eq("Reverse it"));
        verify(customerService).updateCustomerBalance(any(Customer.class), any());
    }

    @Test
    void shouldGetOverdueInvoices() {
        // Given
        List<ARInvoice> overdueInvoices = List.of(invoice);
        List<ARInvoiceDto> overdueDtos = List.of(invoiceDto);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findOverdueInvoices(eq(TENANT_ID), any(LocalDate.class), any()))
                .thenReturn(overdueInvoices);
        when(arInvoiceMapper.toDtoList(overdueInvoices)).thenReturn(overdueDtos);

        // When
        List<ARInvoiceDto> result = arInvoiceService.getOverdueInvoices();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetCustomerTotalOutstanding() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.sumBalanceDueByCustomer(eq(TENANT_ID), eq(1L), any()))
                .thenReturn(new BigDecimal("5000.00"));

        // When
        BigDecimal result = arInvoiceService.getCustomerTotalOutstanding(1L);

        // Then
        assertEquals(new BigDecimal("5000.00"), result);
    }

    @Test
    void shouldGetInvoicesByCustomer() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ARInvoice> page = new PageImpl<>(List.of(invoice), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByTenantIdAndCustomer_Id(TENANT_ID, 1L, pageable)).thenReturn(page);
        when(arInvoiceMapper.toDto(invoice)).thenReturn(invoiceDto);

        // When
        PageResponse<ARInvoiceDto> result = arInvoiceService.getInvoicesByCustomer(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(arInvoiceRepository).findByTenantIdAndCustomer_Id(TENANT_ID, 1L, pageable);
    }

    @Test
    void shouldGetInvoicesByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ARInvoice> page = new PageImpl<>(List.of(invoice), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByTenantIdAndStatus(TENANT_ID, ARInvoiceStatus.DRAFT, pageable)).thenReturn(page);
        when(arInvoiceMapper.toDto(invoice)).thenReturn(invoiceDto);

        // When
        PageResponse<ARInvoiceDto> result = arInvoiceService.getInvoicesByStatus(ARInvoiceStatus.DRAFT, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(arInvoiceRepository).findByTenantIdAndStatus(TENANT_ID, ARInvoiceStatus.DRAFT, pageable);
    }

    @Test
    void shouldSendInvoiceSuccessfully() {
        // Given
        invoice.setStatus(ARInvoiceStatus.PENDING);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(arInvoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(arInvoiceMapper.toDto(any(ARInvoice.class))).thenReturn(
                ARInvoiceDto.builder().id(1L).status(ARInvoiceStatus.SENT).build());

        // When
        ARInvoiceDto result = arInvoiceService.sendInvoice(1L);

        // Then
        assertNotNull(result);
        assertEquals(ARInvoiceStatus.SENT, result.getStatus());
    }

    @Test
    void shouldFailToSendNonPendingInvoice() {
        // Given
        invoice.setStatus(ARInvoiceStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> arInvoiceService.sendInvoice(1L));
    }

    @Test
    void shouldGetInvoiceByNumber() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByInvoiceNumberAndTenantId("INV-000001", TENANT_ID))
                .thenReturn(Optional.of(invoice));
        when(arInvoiceMapper.toDto(invoice)).thenReturn(invoiceDto);

        // When
        ARInvoiceDto result = arInvoiceService.getInvoiceByNumber("INV-000001");

        // Then
        assertNotNull(result);
        assertEquals("INV-000001", result.getInvoiceNumber());
    }

    @Test
    void shouldGetUnpaidInvoicesByCustomer() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findUnpaidByCustomer(eq(TENANT_ID), eq(1L), any()))
                .thenReturn(List.of(invoice));
        when(arInvoiceMapper.toDto(invoice)).thenReturn(invoiceDto);

        // When
        List<ARInvoiceDto> result = arInvoiceService.getUnpaidInvoicesByCustomer(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldApplyPaymentAndMarkAsPaid() {
        // Given
        invoice.setStatus(ARInvoiceStatus.PENDING);
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setBalanceDue(new BigDecimal("1000.00"));

        when(arInvoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        arInvoiceService.applyPayment(invoice, new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        // Then
        assertEquals(0, new BigDecimal("1000.00").compareTo(invoice.getPaidAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getBalanceDue()));
        assertEquals(ARInvoiceStatus.PAID, invoice.getStatus());
        verify(arInvoiceRepository).save(invoice);
    }

    @Test
    void shouldApplyPartialPayment() {
        // Given
        invoice.setStatus(ARInvoiceStatus.PENDING);
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setBalanceDue(new BigDecimal("1000.00"));

        when(arInvoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        arInvoiceService.applyPayment(invoice, new BigDecimal("500.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        // Then
        assertEquals(new BigDecimal("500.00"), invoice.getPaidAmount());
        assertEquals(new BigDecimal("500.00"), invoice.getBalanceDue());
        assertEquals(ARInvoiceStatus.PARTIAL, invoice.getStatus());
    }

    @Test
    void shouldUpdateDraftInvoice() {
        // Given
        CreateARInvoiceRequest request = CreateARInvoiceRequest.builder()
                .customerId(1L)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(45))
                .totalAmount(new BigDecimal("2000.00"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(arInvoiceRepository.save(any())).thenReturn(invoice);
        when(arInvoiceMapper.toDto(any(ARInvoice.class))).thenReturn(invoiceDto);

        // When
        ARInvoiceDto result = arInvoiceService.updateInvoice(1L, request);

        // Then
        assertNotNull(result);
        verify(arInvoiceMapper).updateEntity(eq(request), eq(invoice));
        verify(arInvoiceRepository).save(any());
    }

    @Test
    void shouldFailToUpdateNonEditableInvoice() {
        // Given: DRAFT and PENDING invoices are editable; PAID is not
        invoice.setStatus(ARInvoiceStatus.PAID);
        CreateARInvoiceRequest request = CreateARInvoiceRequest.builder()
                .customerId(1L)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .totalAmount(new BigDecimal("1000.00"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> arInvoiceService.updateInvoice(1L, request));
    }

    @Test
    void shouldCheckOverdueInvoices() {
        // Given
        ARInvoice pendingInvoice = ARInvoice.builder()
                .id(2L)
                .invoiceNumber("INV-000002")
                .customer(customer)
                .status(ARInvoiceStatus.PENDING)
                .dueDate(LocalDate.now().minusDays(5))
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByTenantIdAndDueDateBeforeAndStatusIn(eq(TENANT_ID), any(LocalDate.class), any()))
                .thenReturn(List.of(pendingInvoice));
        when(arInvoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        arInvoiceService.checkOverdueInvoices();

        // Then
        assertEquals(ARInvoiceStatus.OVERDUE, pendingInvoice.getStatus());
        verify(arInvoiceRepository).save(pendingInvoice);
    }
}
