package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.ARPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateARPaymentAllocationRequest;
import com.hisobnoma.platform.finance.dto.CreateARPaymentRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.ARPaymentMapper;
import com.hisobnoma.platform.finance.repository.*;
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
class ARPaymentServiceTest {

    @Mock
    private ARPaymentRepository arPaymentRepository;

    @Mock
    private ARPaymentAllocationRepository arPaymentAllocationRepository;

    @Mock
    private ARInvoiceRepository arInvoiceRepository;

    @Mock
    private CreditNoteRepository creditNoteRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ARPaymentMapper arPaymentMapper;

    @Mock
    private ARInvoiceService arInvoiceService;

    @Mock
    private CustomerService customerService;

    @Mock
    private GLIntegrationService glIntegrationService;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ARPaymentService arPaymentService;

    private Customer customer;
    private ARPayment payment;
    private ARPaymentDto paymentDto;
    private ARInvoice invoice;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .code("CUST-000001")
                .name("Test Customer")
                .tenantId(TENANT_ID)
                .active(true)
                .currentBalance(new BigDecimal("1000.00"))
                .build();

        invoice = ARInvoice.builder()
                .id(1L)
                .invoiceNumber("INV-000001")
                .customer(customer)
                .status(ARInvoiceStatus.PENDING)
                .totalAmount(new BigDecimal("1000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("1000.00"))
                .tenantId(TENANT_ID)
                .lines(new ArrayList<>())
                .build();

        payment = ARPayment.builder()
                .id(1L)
                .paymentNumber("REC-000001")
                .customer(customer)
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("1000.00"))
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(new BigDecimal("1000.00"))
                .status(ARPaymentStatus.PENDING)
                .tenantId(TENANT_ID)
                .allocations(new ArrayList<>())
                .build();

        paymentDto = ARPaymentDto.builder()
                .id(1L)
                .paymentNumber("REC-000001")
                .customerId(1L)
                .customerName("Test Customer")
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("1000.00"))
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(new BigDecimal("1000.00"))
                .status(ARPaymentStatus.PENDING)
                .build();
    }

    @Test
    void shouldGetPaymentsSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ARPayment> page = new PageImpl<>(List.of(payment), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(arPaymentMapper.toDto(payment)).thenReturn(paymentDto);

        // When
        PageResponse<ARPaymentDto> result = arPaymentService.getPayments(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("REC-000001", result.getContent().get(0).getPaymentNumber());
    }

    @Test
    void shouldGetPaymentSuccessfully() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(arPaymentMapper.toDto(payment)).thenReturn(paymentDto);

        // When
        ARPaymentDto result = arPaymentService.getPayment(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("REC-000001", result.getPaymentNumber());
    }

    @Test
    void shouldThrowNotFoundWhenPaymentDoesNotExist() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> arPaymentService.getPayment(999L));
    }

    @Test
    void shouldCreatePaymentSuccessfully() {
        // Given
        CreateARPaymentAllocationRequest allocRequest = CreateARPaymentAllocationRequest.builder()
                .arInvoiceId(1L)
                .allocatedAmount(new BigDecimal("1000.00"))
                .build();

        CreateARPaymentRequest request = CreateARPaymentRequest.builder()
                .customerId(1L)
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("1000.00"))
                .allocations(List.of(allocRequest))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(arPaymentRepository.findMaxPaymentNumber(TENANT_ID)).thenReturn(null);
        when(arPaymentMapper.toEntity(any())).thenReturn(payment);
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(arPaymentRepository.save(any())).thenReturn(payment);
        when(arPaymentMapper.toDto(payment)).thenReturn(paymentDto);

        // When
        ARPaymentDto result = arPaymentService.createPayment(request);

        // Then
        assertNotNull(result);
        assertEquals("REC-000001", result.getPaymentNumber());
        verify(arPaymentRepository).save(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenOverpayment() {
        // Given
        CreateARPaymentAllocationRequest allocRequest = CreateARPaymentAllocationRequest.builder()
                .arInvoiceId(1L)
                .allocatedAmount(new BigDecimal("2000.00"))
                .build();

        CreateARPaymentRequest request = CreateARPaymentRequest.builder()
                .customerId(1L)
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("1000.00"))
                .allocations(List.of(allocRequest))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(arPaymentRepository.findMaxPaymentNumber(TENANT_ID)).thenReturn(null);
        when(arPaymentMapper.toEntity(any())).thenReturn(
                ARPayment.builder()
                        .id(null)
                        .paymentAmount(new BigDecimal("1000.00"))
                        .allocatedAmount(BigDecimal.ZERO)
                        .unallocatedAmount(new BigDecimal("1000.00"))
                        .status(ARPaymentStatus.PENDING)
                        .allocations(new ArrayList<>())
                        .build());
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> arPaymentService.createPayment(request));
    }

    @Test
    void shouldThrowNotFoundWhenInvoiceNotFoundOnCreatePayment() {
        // Given
        CreateARPaymentAllocationRequest allocRequest = CreateARPaymentAllocationRequest.builder()
                .arInvoiceId(999L)
                .allocatedAmount(new BigDecimal("500.00"))
                .build();

        CreateARPaymentRequest request = CreateARPaymentRequest.builder()
                .customerId(1L)
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("500.00"))
                .allocations(List.of(allocRequest))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(arPaymentRepository.findMaxPaymentNumber(TENANT_ID)).thenReturn(null);
        when(arPaymentMapper.toEntity(any())).thenReturn(
                ARPayment.builder()
                        .paymentAmount(new BigDecimal("500.00"))
                        .allocatedAmount(BigDecimal.ZERO)
                        .unallocatedAmount(new BigDecimal("500.00"))
                        .status(ARPaymentStatus.PENDING)
                        .allocations(new ArrayList<>())
                        .build());
        when(arInvoiceRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> arPaymentService.createPayment(request));
    }

    @Test
    void shouldCancelPaymentSuccessfully() {
        // Given
        payment.setStatus(ARPaymentStatus.PENDING);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(arPaymentRepository.save(any())).thenReturn(payment);
        when(arPaymentMapper.toDto(any(ARPayment.class))).thenReturn(
                ARPaymentDto.builder()
                        .id(1L)
                        .status(ARPaymentStatus.CANCELLED)
                        .build());

        // When
        ARPaymentDto result = arPaymentService.cancelPayment(1L, "Duplicate payment");

        // Then
        assertNotNull(result);
        assertEquals(ARPaymentStatus.CANCELLED, result.getStatus());
        verify(arPaymentRepository).save(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenCancellingAlreadyCancelledPayment() {
        // Given
        payment.setStatus(ARPaymentStatus.CANCELLED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> arPaymentService.cancelPayment(1L, "Try again"));
    }

    @Test
    void shouldCancelCompletedPaymentAndReverseGL() {
        // Given
        ARPaymentAllocation allocation = ARPaymentAllocation.builder()
                .id(1L)
                .arPayment(payment)
                .arInvoice(invoice)
                .allocatedAmount(new BigDecimal("1000.00"))
                .discountTaken(BigDecimal.ZERO)
                .writeOffAmount(BigDecimal.ZERO)
                .build();
        payment.setStatus(ARPaymentStatus.COMPLETED);
        payment.setAllocations(new ArrayList<>(List.of(allocation)));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(arPaymentRepository.save(any())).thenReturn(payment);
        when(arPaymentMapper.toDto(any(ARPayment.class))).thenReturn(
                ARPaymentDto.builder().id(1L).status(ARPaymentStatus.CANCELLED).build());

        // When
        ARPaymentDto result = arPaymentService.cancelPayment(1L, "Reverse completed");

        // Then
        assertNotNull(result);
        verify(glIntegrationService).reverseARPayment(eq(payment), eq("Reverse completed"));
        verify(arInvoiceService).applyPayment(any(ARInvoice.class), any(), any(), any());
        verify(customerService).updateCustomerBalance(any(Customer.class), any());
    }

    @Test
    void shouldCompletePaymentSuccessfully() {
        // Given
        payment.setStatus(ARPaymentStatus.PENDING);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(arPaymentRepository.save(any())).thenReturn(payment);
        when(glIntegrationService.postARPayment(payment)).thenReturn(888L);
        when(arPaymentMapper.toDto(any(ARPayment.class))).thenReturn(
                ARPaymentDto.builder().id(1L).status(ARPaymentStatus.COMPLETED).build());

        // When
        ARPaymentDto result = arPaymentService.completePayment(1L);

        // Then
        assertNotNull(result);
        assertEquals(ARPaymentStatus.COMPLETED, result.getStatus());
        verify(glIntegrationService).postARPayment(payment);
        verify(customerService).updateCustomerBalance(any(Customer.class), any());
        // The journal-entry id must be captured so a later void can actually reverse it.
        assertEquals(888L, payment.getGlJournalEntryId());
        assertTrue(payment.isGlPosted());
        assertNotNull(payment.getGlPostedAt());
    }

    @Test
    void shouldFailToCompleteNonPendingPayment() {
        // Given
        payment.setStatus(ARPaymentStatus.COMPLETED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> arPaymentService.completePayment(1L));
    }

    @Test
    void shouldGetPaymentsByCustomer() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ARPayment> page = new PageImpl<>(List.of(payment), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByTenantIdAndCustomer_Id(TENANT_ID, 1L, pageable)).thenReturn(page);
        when(arPaymentMapper.toDto(payment)).thenReturn(paymentDto);

        // When
        PageResponse<ARPaymentDto> result = arPaymentService.getPaymentsByCustomer(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(arPaymentRepository).findByTenantIdAndCustomer_Id(TENANT_ID, 1L, pageable);
    }

    @Test
    void shouldGetPaymentsByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ARPayment> page = new PageImpl<>(List.of(payment), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByTenantIdAndStatus(TENANT_ID, ARPaymentStatus.PENDING, pageable)).thenReturn(page);
        when(arPaymentMapper.toDto(payment)).thenReturn(paymentDto);

        // When
        PageResponse<ARPaymentDto> result = arPaymentService.getPaymentsByStatus(ARPaymentStatus.PENDING, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void shouldGetPaymentByNumber() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByPaymentNumberAndTenantId("REC-000001", TENANT_ID))
                .thenReturn(Optional.of(payment));
        when(arPaymentMapper.toDto(payment)).thenReturn(paymentDto);

        // When
        ARPaymentDto result = arPaymentService.getPaymentByNumber("REC-000001");

        // Then
        assertNotNull(result);
        assertEquals("REC-000001", result.getPaymentNumber());
    }

    @Test
    void shouldGetPaymentsByDateRange() {
        // Given
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByDateRange(TENANT_ID, start, end)).thenReturn(List.of(payment));
        when(arPaymentMapper.toDtoList(List.of(payment))).thenReturn(List.of(paymentDto));

        // When
        List<ARPaymentDto> result = arPaymentService.getPaymentsByDateRange(start, end);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldMarkPaymentAsDeposited() {
        // Given
        payment.setStatus(ARPaymentStatus.COMPLETED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(arPaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(arPaymentMapper.toDto(any(ARPayment.class))).thenReturn(
                ARPaymentDto.builder().id(1L).deposited(true).depositReference("DEP-001").build());

        // When
        ARPaymentDto result = arPaymentService.markAsDeposited(1L, "DEP-001");

        // Then
        assertNotNull(result);
        assertTrue(result.isDeposited());
        assertEquals("DEP-001", result.getDepositReference());
    }

    @Test
    void shouldFailToDepositNonCompletedPayment() {
        // Given
        payment.setStatus(ARPaymentStatus.PENDING);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> arPaymentService.markAsDeposited(1L, "DEP-001"));
    }

    @Test
    void shouldGetUndepositedPayments() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByTenantIdAndDepositedFalseAndStatus(TENANT_ID, ARPaymentStatus.COMPLETED))
                .thenReturn(List.of(payment));
        when(arPaymentMapper.toDtoList(List.of(payment))).thenReturn(List.of(paymentDto));

        // When
        List<ARPaymentDto> result = arPaymentService.getUndepositedPayments();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldAddAllocationToPayment() {
        // Given
        CreateARPaymentAllocationRequest allocRequest = CreateARPaymentAllocationRequest.builder()
                .arInvoiceId(1L)
                .allocatedAmount(new BigDecimal("500.00"))
                .build();

        payment.setUnallocatedAmount(new BigDecimal("1000.00"));
        payment.setAllocatedAmount(BigDecimal.ZERO);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(arInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(arPaymentAllocationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(arPaymentRepository.save(any())).thenReturn(payment);
        when(arPaymentMapper.toDto(any(ARPayment.class))).thenReturn(paymentDto);

        // When
        ARPaymentDto result = arPaymentService.addAllocation(1L, allocRequest);

        // Then
        assertNotNull(result);
        verify(arPaymentAllocationRepository).save(any());
        verify(arPaymentRepository).save(any());
    }

    @Test
    void shouldFailToAddAllocationExceedingUnallocatedBalance() {
        // Given
        CreateARPaymentAllocationRequest allocRequest = CreateARPaymentAllocationRequest.builder()
                .arInvoiceId(1L)
                .allocatedAmount(new BigDecimal("2000.00"))
                .build();

        payment.setUnallocatedAmount(new BigDecimal("1000.00"));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> arPaymentService.addAllocation(1L, allocRequest));
    }

    @Test
    void shouldGetTotalPaymentsByDateRange() {
        // Given
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arPaymentRepository.sumPaymentsByDateRangeAndStatus(TENANT_ID, start, end, ARPaymentStatus.COMPLETED))
                .thenReturn(new BigDecimal("5000.00"));

        // When
        BigDecimal result = arPaymentService.getTotalPaymentsByDateRange(start, end);

        // Then
        assertEquals(new BigDecimal("5000.00"), result);
    }
}
