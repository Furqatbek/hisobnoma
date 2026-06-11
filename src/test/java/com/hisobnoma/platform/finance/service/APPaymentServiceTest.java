package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.*;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.APPaymentAllocationMapper;
import com.hisobnoma.platform.finance.mapper.APPaymentMapper;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class APPaymentServiceTest {

    @Mock
    private APPaymentRepository apPaymentRepository;

    @Mock
    private APPaymentAllocationRepository apPaymentAllocationRepository;

    @Mock
    private APInvoiceRepository apInvoiceRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private APPaymentMapper apPaymentMapper;

    @Mock
    private APPaymentAllocationMapper apPaymentAllocationMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private GLIntegrationService glIntegrationService;

    @InjectMocks
    private APPaymentService apPaymentService;

    private APPayment payment;
    private APPaymentDto paymentDto;
    private Vendor vendor;
    private APInvoice invoice;
    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder()
                .id(1L)
                .code("V-001")
                .name("Test Vendor")
                .paymentTerms("Net 30")
                .paymentTermsDays(30)
                .currentBalance(new BigDecimal("500.0000"))
                .build();

        invoice = APInvoice.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AP-000001")
                .vendorId(1L)
                .vendorName("Test Vendor")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
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

        payment = APPayment.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .paymentNumber("PAY-000001")
                .vendorId(1L)
                .vendorName("Test Vendor")
                .paymentDate(LocalDate.now())
                .status(APPaymentStatus.DRAFT)
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("500.0000"))
                .allocatedAmount(new BigDecimal("500.0000"))
                .unallocatedAmount(BigDecimal.ZERO)
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .glPosted(false)
                .reconciled(false)
                .allocations(new ArrayList<>())
                .build();

        paymentDto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .vendorId(1L)
                .vendorName("Test Vendor")
                .paymentDate(LocalDate.now())
                .status(APPaymentStatus.DRAFT)
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("500.0000"))
                .allocatedAmount(new BigDecimal("500.0000"))
                .unallocatedAmount(BigDecimal.ZERO)
                .fullyAllocated(true)
                .availableAmount(BigDecimal.ZERO)
                .build();
    }

    // ========== getPayments ==========

    @Test
    void shouldGetPaymentsSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<APPayment> page = new PageImpl<>(List.of(payment), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apPaymentRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(apPaymentMapper.toDtoWithoutAllocations(payment)).thenReturn(paymentDto);

        // When
        PageResponse<APPaymentDto> result = apPaymentService.getPayments(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("PAY-000001", result.getContent().get(0).getPaymentNumber());
        verify(apPaymentRepository).findAllByTenantId(TENANT_ID, pageable);
    }

    // ========== getPaymentsByVendor ==========

    @Test
    void shouldGetPaymentsByVendorSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<APPayment> page = new PageImpl<>(List.of(payment), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apPaymentRepository.findByTenantIdAndVendorId(TENANT_ID, 1L, pageable)).thenReturn(page);
        when(apPaymentMapper.toDtoWithoutAllocations(payment)).thenReturn(paymentDto);

        // When
        PageResponse<APPaymentDto> result = apPaymentService.getPaymentsByVendor(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(apPaymentRepository).findByTenantIdAndVendorId(TENANT_ID, 1L, pageable);
    }

    // ========== getPayment ==========

    @Test
    void shouldGetPaymentSuccessfully() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(apPaymentMapper.toDto(payment)).thenReturn(paymentDto);

        // When
        APPaymentDto result = apPaymentService.getPayment(1L);

        // Then
        assertNotNull(result);
        assertEquals("PAY-000001", result.getPaymentNumber());
        assertEquals(1L, result.getVendorId());
        verify(apPaymentRepository).findByIdAndTenantId(1L, TENANT_ID);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenPaymentNotFound() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apPaymentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> apPaymentService.getPayment(999L));
    }

    // ========== createPayment ==========

    @Test
    void shouldCreatePaymentSuccessfully() {
        // Given
        CreateAPPaymentAllocationRequest allocationRequest = CreateAPPaymentAllocationRequest.builder()
                .apInvoiceId(1L)
                .allocatedAmount(new BigDecimal("500.0000"))
                .discountTaken(BigDecimal.ZERO)
                .writeOffAmount(BigDecimal.ZERO)
                .build();

        CreateAPPaymentRequest request = CreateAPPaymentRequest.builder()
                .vendorId(1L)
                .paymentDate(LocalDate.now())
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("500.0000"))
                .bankAccountId(1L)
                .allocations(List.of(allocationRequest))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(apPaymentMapper.toEntity(request)).thenReturn(payment);
        when(apPaymentRepository.findMaxPaymentNumber(TENANT_ID)).thenReturn(null);
        when(apPaymentRepository.save(any(APPayment.class))).thenReturn(payment);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(apPaymentMapper.toDto(any(APPayment.class))).thenReturn(paymentDto);

        // When
        APPaymentDto result = apPaymentService.createPayment(request);

        // Then
        assertNotNull(result);
        assertEquals("PAY-000001", result.getPaymentNumber());
        verify(apPaymentRepository, times(2)).save(any(APPayment.class));
        verify(vendorRepository).findByIdAndTenantId(1L, TENANT_ID);
    }

    @Test
    void shouldThrowNotFoundWhenVendorNotFoundOnCreate() {
        // Given
        CreateAPPaymentAllocationRequest allocationRequest = CreateAPPaymentAllocationRequest.builder()
                .apInvoiceId(1L)
                .allocatedAmount(new BigDecimal("500.0000"))
                .build();

        CreateAPPaymentRequest request = CreateAPPaymentRequest.builder()
                .vendorId(999L)
                .paymentDate(LocalDate.now())
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("500.0000"))
                .allocations(List.of(allocationRequest))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> apPaymentService.createPayment(request));
        verify(apPaymentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenAllocationExceedsPaymentAmount() {
        // Given
        CreateAPPaymentAllocationRequest allocationRequest = CreateAPPaymentAllocationRequest.builder()
                .apInvoiceId(1L)
                .allocatedAmount(new BigDecimal("600.0000"))
                .build();

        CreateAPPaymentRequest request = CreateAPPaymentRequest.builder()
                .vendorId(1L)
                .paymentDate(LocalDate.now())
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("500.0000"))
                .allocations(List.of(allocationRequest))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));

        // When/Then
        assertThrows(BusinessException.class, () -> apPaymentService.createPayment(request));
        verify(apPaymentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenInvoiceNotApprovedForPayment() {
        // Given
        APInvoice draftInvoice = APInvoice.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AP-000002")
                .vendorId(1L)
                .status(APInvoiceStatus.DRAFT)
                .totalAmount(new BigDecimal("500.0000"))
                .balanceDue(new BigDecimal("500.0000"))
                .build();

        CreateAPPaymentAllocationRequest allocationRequest = CreateAPPaymentAllocationRequest.builder()
                .apInvoiceId(2L)
                .allocatedAmount(new BigDecimal("500.0000"))
                .build();

        CreateAPPaymentRequest request = CreateAPPaymentRequest.builder()
                .vendorId(1L)
                .paymentDate(LocalDate.now())
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("500.0000"))
                .allocations(List.of(allocationRequest))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(apPaymentMapper.toEntity(request)).thenReturn(payment);
        when(apPaymentRepository.findMaxPaymentNumber(TENANT_ID)).thenReturn(null);
        when(apPaymentRepository.save(any(APPayment.class))).thenReturn(payment);
        when(apInvoiceRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(draftInvoice));

        // When/Then
        assertThrows(BusinessException.class, () -> apPaymentService.createPayment(request));
    }

    // ========== approvePayment ==========

    @Test
    void shouldApprovePaymentSuccessfully() {
        // Given
        payment.setStatus(APPaymentStatus.PENDING_APPROVAL);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(apPaymentRepository.save(any(APPayment.class))).thenReturn(payment);

        APPaymentDto approvedDto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .status(APPaymentStatus.APPROVED)
                .build();
        when(apPaymentMapper.toDto(any(APPayment.class))).thenReturn(approvedDto);

        // When
        APPaymentDto result = apPaymentService.approvePayment(1L);

        // Then
        assertNotNull(result);
        assertEquals(APPaymentStatus.APPROVED, result.getStatus());
        verify(apPaymentRepository).save(any(APPayment.class));
    }

    @Test
    void shouldApproveDraftPaymentDirectly() {
        // Given
        payment.setStatus(APPaymentStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(apPaymentRepository.save(any(APPayment.class))).thenReturn(payment);

        APPaymentDto approvedDto = APPaymentDto.builder()
                .id(1L)
                .status(APPaymentStatus.APPROVED)
                .build();
        when(apPaymentMapper.toDto(any(APPayment.class))).thenReturn(approvedDto);

        // When
        APPaymentDto result = apPaymentService.approvePayment(1L);

        // Then
        assertNotNull(result);
        assertEquals(APPaymentStatus.APPROVED, result.getStatus());
    }

    @Test
    void shouldThrowWhenApprovingCompletedPayment() {
        // Given
        payment.setStatus(APPaymentStatus.COMPLETED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> apPaymentService.approvePayment(1L));
        verify(apPaymentRepository, never()).save(any());
    }

    // ========== voidPayment ==========

    @Test
    void shouldVoidDraftPaymentSuccessfully() {
        // Given
        payment.setStatus(APPaymentStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(apPaymentRepository.save(any(APPayment.class))).thenReturn(payment);

        APPaymentDto voidedDto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .status(APPaymentStatus.VOIDED)
                .voidReason("Duplicate payment")
                .build();
        when(apPaymentMapper.toDto(any(APPayment.class))).thenReturn(voidedDto);

        // When
        APPaymentDto result = apPaymentService.voidPayment(1L, "Duplicate payment");

        // Then
        assertNotNull(result);
        assertEquals(APPaymentStatus.VOIDED, result.getStatus());
        assertEquals("Duplicate payment", result.getVoidReason());
        verify(apPaymentRepository).save(any(APPayment.class));
    }

    @Test
    void shouldVoidCompletedPaymentAndReverseEffects() {
        // Given
        APPaymentAllocation allocation = APPaymentAllocation.builder()
                .id(1L)
                .apInvoice(invoice)
                .invoiceNumber("AP-000001")
                .allocatedAmount(new BigDecimal("500.0000"))
                .discountTaken(BigDecimal.ZERO)
                .writeOffAmount(BigDecimal.ZERO)
                .invoiceBalanceBefore(new BigDecimal("500.0000"))
                .invoiceBalanceAfter(BigDecimal.ZERO)
                .build();
        allocation.setApPayment(payment);

        payment.setStatus(APPaymentStatus.COMPLETED);
        payment.setGlPosted(true);
        payment.setGlJournalEntryId(200L);
        payment.setAllocations(new ArrayList<>(List.of(allocation)));

        // Mark invoice as paid
        invoice.setStatus(APInvoiceStatus.PAID);
        invoice.setPaidAmount(new BigDecimal("500.0000"));
        invoice.setBalanceDue(BigDecimal.ZERO);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(invoice);
        when(apPaymentRepository.save(any(APPayment.class))).thenReturn(payment);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));

        APPaymentDto voidedDto = APPaymentDto.builder()
                .id(1L)
                .status(APPaymentStatus.VOIDED)
                .build();
        when(apPaymentMapper.toDto(any(APPayment.class))).thenReturn(voidedDto);

        // When
        APPaymentDto result = apPaymentService.voidPayment(1L, "Incorrect vendor");

        // Then
        assertNotNull(result);
        assertEquals(APPaymentStatus.VOIDED, result.getStatus());
        verify(glIntegrationService).reverseAPPayment(any(APPayment.class));
        verify(apInvoiceRepository).save(any(APInvoice.class));
        verify(vendorRepository).findByIdAndTenantId(1L, TENANT_ID);
    }

    @Test
    void shouldThrowWhenVoidingAlreadyVoidedPayment() {
        // Given
        payment.setStatus(APPaymentStatus.VOIDED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> apPaymentService.voidPayment(1L, "Test"));
        verify(apPaymentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenVoidingReconciledPayment() {
        // Given
        payment.setStatus(APPaymentStatus.COMPLETED);
        payment.setReconciled(true);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> apPaymentService.voidPayment(1L, "Test"));
        verify(apPaymentRepository, never()).save(any());
    }

    // ========== submitForApproval ==========

    @Test
    void shouldSubmitPaymentForApprovalSuccessfully() {
        // Given
        payment.setStatus(APPaymentStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(apPaymentRepository.save(any(APPayment.class))).thenReturn(payment);

        APPaymentDto pendingDto = APPaymentDto.builder()
                .id(1L)
                .status(APPaymentStatus.PENDING_APPROVAL)
                .build();
        when(apPaymentMapper.toDto(any(APPayment.class))).thenReturn(pendingDto);

        // When
        APPaymentDto result = apPaymentService.submitForApproval(1L);

        // Then
        assertNotNull(result);
        assertEquals(APPaymentStatus.PENDING_APPROVAL, result.getStatus());
    }

    @Test
    void shouldThrowWhenSubmittingNonDraftPayment() {
        // Given
        payment.setStatus(APPaymentStatus.APPROVED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> apPaymentService.submitForApproval(1L));
    }

    // ========== processPayment ==========

    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given
        APPaymentAllocation allocation = APPaymentAllocation.builder()
                .id(1L)
                .apInvoice(invoice)
                .invoiceNumber("AP-000001")
                .allocatedAmount(new BigDecimal("500.0000"))
                .discountTaken(BigDecimal.ZERO)
                .writeOffAmount(BigDecimal.ZERO)
                .build();
        allocation.setApPayment(payment);

        payment.setStatus(APPaymentStatus.APPROVED);
        payment.setAllocations(new ArrayList<>(List.of(allocation)));

        Account apAccount = Account.builder()
                .id(10L)
                .code("2100")
                .build();
        Account cashAccount = Account.builder()
                .id(11L)
                .code("1110")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(invoice);
        when(accountRepository.findByCodeAndTenantId("2100", TENANT_ID)).thenReturn(Optional.of(apAccount));
        when(accountRepository.findByCodeAndTenantId("1110", TENANT_ID)).thenReturn(Optional.of(cashAccount));
        when(glIntegrationService.postAPPayment(any(APPayment.class))).thenReturn(200L);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(apPaymentRepository.save(any(APPayment.class))).thenReturn(payment);

        APPaymentDto completedDto = APPaymentDto.builder()
                .id(1L)
                .status(APPaymentStatus.COMPLETED)
                .glPosted(true)
                .build();
        when(apPaymentMapper.toDto(any(APPayment.class))).thenReturn(completedDto);

        // When
        APPaymentDto result = apPaymentService.processPayment(1L);

        // Then
        assertNotNull(result);
        assertEquals(APPaymentStatus.COMPLETED, result.getStatus());
        assertTrue(result.isGlPosted());
        verify(glIntegrationService).postAPPayment(any(APPayment.class));
        verify(apInvoiceRepository).save(any(APInvoice.class));
    }

    @Test
    void shouldThrowWhenProcessingNonApprovedPayment() {
        // Given
        payment.setStatus(APPaymentStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> apPaymentService.processPayment(1L));
    }

    // ========== reconcilePayment ==========

    @Test
    void shouldReconcilePaymentSuccessfully() {
        // Given
        payment.setStatus(APPaymentStatus.COMPLETED);
        payment.setReconciled(false);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));
        when(apPaymentRepository.save(any(APPayment.class))).thenReturn(payment);

        APPaymentDto reconciledDto = APPaymentDto.builder()
                .id(1L)
                .status(APPaymentStatus.COMPLETED)
                .reconciled(true)
                .build();
        when(apPaymentMapper.toDto(any(APPayment.class))).thenReturn(reconciledDto);

        // When
        APPaymentDto result = apPaymentService.reconcilePayment(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isReconciled());
    }

    @Test
    void shouldThrowWhenReconcilingNonCompletedPayment() {
        // Given
        payment.setStatus(APPaymentStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> apPaymentService.reconcilePayment(1L));
    }

    @Test
    void shouldThrowWhenReconcilingAlreadyReconciledPayment() {
        // Given
        payment.setStatus(APPaymentStatus.COMPLETED);
        payment.setReconciled(true);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apPaymentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> apPaymentService.reconcilePayment(1L));
    }

    // ========== getPaymentsByStatus ==========

    @Test
    void shouldGetPaymentsByStatusSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<APPayment> page = new PageImpl<>(List.of(payment), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apPaymentRepository.findByTenantIdAndStatus(TENANT_ID, APPaymentStatus.DRAFT, pageable)).thenReturn(page);
        when(apPaymentMapper.toDtoWithoutAllocations(payment)).thenReturn(paymentDto);

        // When
        PageResponse<APPaymentDto> result = apPaymentService.getPaymentsByStatus(APPaymentStatus.DRAFT, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(apPaymentRepository).findByTenantIdAndStatus(TENANT_ID, APPaymentStatus.DRAFT, pageable);
    }

    // ========== Reporting methods ==========

    @Test
    void shouldGetPaymentsByDateRange() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apPaymentRepository.findByDateRange(TENANT_ID, startDate, endDate)).thenReturn(List.of(payment));
        when(apPaymentMapper.toDtoList(List.of(payment))).thenReturn(List.of(paymentDto));

        // When
        List<APPaymentDto> result = apPaymentService.getPaymentsByDateRange(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetUnreconciledPayments() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apPaymentRepository.findByTenantIdAndReconciledFalseAndStatus(TENANT_ID, APPaymentStatus.COMPLETED))
                .thenReturn(List.of(payment));
        when(apPaymentMapper.toDtoList(List.of(payment))).thenReturn(List.of(paymentDto));

        // When
        List<APPaymentDto> result = apPaymentService.getUnreconciledPayments();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetTotalPaymentsByVendor() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apPaymentRepository.sumPaymentsByVendorAndStatus(TENANT_ID, 1L, APPaymentStatus.COMPLETED))
                .thenReturn(new BigDecimal("5000.00"));

        // When
        BigDecimal result = apPaymentService.getTotalPaymentsByVendor(1L);

        // Then
        assertEquals(new BigDecimal("5000.00"), result);
    }

    @Test
    void shouldGetTotalPaymentsByDateRange() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apPaymentRepository.sumPaymentsByDateRangeAndStatus(TENANT_ID, startDate, endDate, APPaymentStatus.COMPLETED))
                .thenReturn(new BigDecimal("15000.00"));

        // When
        BigDecimal result = apPaymentService.getTotalPaymentsByDateRange(startDate, endDate);

        // Then
        assertEquals(new BigDecimal("15000.00"), result);
    }
}
