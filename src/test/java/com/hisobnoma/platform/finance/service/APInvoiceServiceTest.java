package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.APInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceLineRequest;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.APInvoiceLineMapper;
import com.hisobnoma.platform.finance.mapper.APInvoiceMapper;
import com.hisobnoma.platform.finance.repository.APInvoiceLineRepository;
import com.hisobnoma.platform.finance.repository.APInvoiceRepository;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.inventory.entity.PurchaseOrder;
import com.hisobnoma.platform.inventory.entity.ReceivingOrder;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.repository.PurchaseOrderRepository;
import com.hisobnoma.platform.inventory.repository.ReceivingOrderRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class APInvoiceServiceTest {

    @Mock
    private APInvoiceRepository apInvoiceRepository;

    @Mock
    private APInvoiceLineRepository apInvoiceLineRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private ReceivingOrderRepository receivingOrderRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private APInvoiceMapper apInvoiceMapper;

    @Mock
    private APInvoiceLineMapper apInvoiceLineMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private GLIntegrationService glIntegrationService;

    @InjectMocks
    private APInvoiceService apInvoiceService;

    private APInvoice invoice;
    private APInvoiceLine invoiceLine;
    private Vendor vendor;
    private APInvoiceDto invoiceDto;
    private CreateAPInvoiceRequest createRequest;
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
                .defaultExpenseAccountId(100L)
                .currentBalance(BigDecimal.ZERO)
                .build();

        invoiceLine = APInvoiceLine.builder()
                .id(1L)
                .lineNumber(1)
                .description("Office Supplies")
                .quantity(new BigDecimal("10.0000"))
                .unitPrice(new BigDecimal("25.0000"))
                .lineTotal(new BigDecimal("250.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .build();

        invoice = APInvoice.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AP-000001")
                .vendorId(1L)
                .vendorName("Test Vendor")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(APInvoiceStatus.DRAFT)
                .subtotal(new BigDecimal("250.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("250.0000"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("250.0000"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .glPosted(false)
                .lines(new ArrayList<>(List.of(invoiceLine)))
                .build();

        invoiceDto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .vendorId(1L)
                .vendorName("Test Vendor")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(APInvoiceStatus.DRAFT)
                .totalAmount(new BigDecimal("250.0000"))
                .balanceDue(new BigDecimal("250.0000"))
                .build();

        CreateAPInvoiceLineRequest lineRequest = CreateAPInvoiceLineRequest.builder()
                .description("Office Supplies")
                .quantity(new BigDecimal("10.0000"))
                .unitPrice(new BigDecimal("25.0000"))
                .build();

        createRequest = CreateAPInvoiceRequest.builder()
                .vendorId(1L)
                .vendorInvoiceNumber("VINV-001")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .description("Test invoice")
                .currency("UZS")
                .lines(List.of(lineRequest))
                .build();
    }

    // ========== getInvoices ==========

    @Test
    void shouldGetInvoicesSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<APInvoice> page = new PageImpl<>(List.of(invoice), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(apInvoiceMapper.toDtoWithoutLines(invoice)).thenReturn(invoiceDto);

        // When
        PageResponse<APInvoiceDto> result = apInvoiceService.getInvoices(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("AP-000001", result.getContent().get(0).getInvoiceNumber());
        verify(apInvoiceRepository).findAllByTenantId(TENANT_ID, pageable);
    }

    // ========== getInvoicesByVendor ==========

    @Test
    void shouldGetInvoicesByVendorSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<APInvoice> page = new PageImpl<>(List.of(invoice), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findByTenantIdAndVendorId(TENANT_ID, 1L, pageable)).thenReturn(page);
        when(apInvoiceMapper.toDtoWithoutLines(invoice)).thenReturn(invoiceDto);

        // When
        PageResponse<APInvoiceDto> result = apInvoiceService.getInvoicesByVendor(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(apInvoiceRepository).findByTenantIdAndVendorId(TENANT_ID, 1L, pageable);
    }

    // ========== getInvoicesByStatus ==========

    @Test
    void shouldGetInvoicesByStatusSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<APInvoice> page = new PageImpl<>(List.of(invoice), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findByTenantIdAndStatus(TENANT_ID, APInvoiceStatus.DRAFT, pageable)).thenReturn(page);
        when(apInvoiceMapper.toDtoWithoutLines(invoice)).thenReturn(invoiceDto);

        // When
        PageResponse<APInvoiceDto> result = apInvoiceService.getInvoicesByStatus(APInvoiceStatus.DRAFT, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(apInvoiceRepository).findByTenantIdAndStatus(TENANT_ID, APInvoiceStatus.DRAFT, pageable);
    }

    // ========== getInvoice ==========

    @Test
    void shouldGetInvoiceSuccessfully() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(apInvoiceMapper.toDto(invoice)).thenReturn(invoiceDto);

        // When
        APInvoiceDto result = apInvoiceService.getInvoice(1L);

        // Then
        assertNotNull(result);
        assertEquals("AP-000001", result.getInvoiceNumber());
        assertEquals(1L, result.getVendorId());
        verify(apInvoiceRepository).findByIdAndTenantId(1L, TENANT_ID);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenInvoiceNotFound() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> apInvoiceService.getInvoice(999L));
    }

    // ========== getUnpaidInvoicesByVendor ==========

    @Test
    void shouldGetUnpaidInvoicesByVendorSuccessfully() {
        // Given
        APInvoice approvedInvoice = APInvoice.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AP-000002")
                .vendorId(1L)
                .status(APInvoiceStatus.APPROVED)
                .totalAmount(new BigDecimal("500.0000"))
                .balanceDue(new BigDecimal("500.0000"))
                .build();

        List<APInvoiceStatus> payableStatuses = Arrays.asList(APInvoiceStatus.APPROVED, APInvoiceStatus.PARTIAL);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findUnpaidInvoicesByVendor(TENANT_ID, 1L, payableStatuses))
                .thenReturn(List.of(approvedInvoice));

        APInvoiceDto approvedDto = APInvoiceDto.builder()
                .id(2L)
                .invoiceNumber("AP-000002")
                .status(APInvoiceStatus.APPROVED)
                .build();
        when(apInvoiceMapper.toDtoList(List.of(approvedInvoice))).thenReturn(List.of(approvedDto));

        // When
        List<APInvoiceDto> result = apInvoiceService.getUnpaidInvoicesByVendor(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(APInvoiceStatus.APPROVED, result.get(0).getStatus());
        verify(apInvoiceRepository).findUnpaidInvoicesByVendor(TENANT_ID, 1L, payableStatuses);
    }

    // ========== getOverdueInvoices ==========

    @Test
    void shouldGetOverdueInvoicesSuccessfully() {
        // Given
        APInvoice overdueInvoice = APInvoice.builder()
                .id(3L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AP-000003")
                .dueDate(LocalDate.now().minusDays(10))
                .status(APInvoiceStatus.APPROVED)
                .totalAmount(new BigDecimal("300.0000"))
                .balanceDue(new BigDecimal("300.0000"))
                .build();

        List<APInvoiceStatus> excludedStatuses = Arrays.asList(APInvoiceStatus.CANCELLED, APInvoiceStatus.PAID);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findOverdueInvoices(eq(TENANT_ID), any(LocalDate.class), eq(excludedStatuses)))
                .thenReturn(List.of(overdueInvoice));

        APInvoiceDto overdueDto = APInvoiceDto.builder()
                .id(3L)
                .invoiceNumber("AP-000003")
                .overdue(true)
                .build();
        when(apInvoiceMapper.toDtoList(List.of(overdueInvoice))).thenReturn(List.of(overdueDto));

        // When
        List<APInvoiceDto> result = apInvoiceService.getOverdueInvoices();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isOverdue());
    }

    // ========== createInvoice ==========

    @Test
    void shouldCreateInvoiceSuccessfully() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceMapper.toEntity(createRequest)).thenReturn(invoice);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(apInvoiceRepository.findMaxInvoiceNumber(TENANT_ID)).thenReturn(null);
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(invoice);
        when(apInvoiceLineMapper.toEntity(any(CreateAPInvoiceLineRequest.class))).thenReturn(invoiceLine);
        when(apInvoiceMapper.toDto(any(APInvoice.class))).thenReturn(invoiceDto);

        // When
        APInvoiceDto result = apInvoiceService.createInvoice(createRequest);

        // Then
        assertNotNull(result);
        assertEquals("AP-000001", result.getInvoiceNumber());
        verify(apInvoiceRepository, times(2)).save(any(APInvoice.class));
        verify(vendorRepository).findByIdAndTenantId(1L, TENANT_ID);
    }

    @Test
    void shouldThrowNotFoundWhenVendorNotFoundOnCreate() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceMapper.toEntity(createRequest)).thenReturn(invoice);
        when(apInvoiceRepository.findMaxInvoiceNumber(TENANT_ID)).thenReturn(null);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> apInvoiceService.createInvoice(createRequest));
        verify(apInvoiceRepository, never()).save(any(APInvoice.class));
    }

    @Test
    void shouldCreateInvoiceWithoutVendor() {
        // Given
        CreateAPInvoiceLineRequest lineRequest = CreateAPInvoiceLineRequest.builder()
                .description("Rent Payment")
                .quantity(new BigDecimal("1.0000"))
                .unitPrice(new BigDecimal("5000.0000"))
                .build();

        CreateAPInvoiceRequest vendorlessRequest = CreateAPInvoiceRequest.builder()
                .vendorId(null)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .description("Monthly Rent")
                .lines(List.of(lineRequest))
                .build();

        APInvoice vendorlessInvoice = APInvoice.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AP-000002")
                .status(APInvoiceStatus.DRAFT)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .subtotal(new BigDecimal("5000.0000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("5000.0000"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("5000.0000"))
                .lines(new ArrayList<>())
                .build();

        APInvoiceDto vendorlessDto = APInvoiceDto.builder()
                .id(2L)
                .invoiceNumber("AP-000002")
                .vendorName("Monthly Rent")
                .status(APInvoiceStatus.DRAFT)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceMapper.toEntity(vendorlessRequest)).thenReturn(vendorlessInvoice);
        when(apInvoiceRepository.findMaxInvoiceNumber(TENANT_ID)).thenReturn(1);
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(vendorlessInvoice);
        when(apInvoiceLineMapper.toEntity(any(CreateAPInvoiceLineRequest.class))).thenReturn(invoiceLine);
        when(apInvoiceMapper.toDto(any(APInvoice.class))).thenReturn(vendorlessDto);

        // When
        APInvoiceDto result = apInvoiceService.createInvoice(vendorlessRequest);

        // Then
        assertNotNull(result);
        verify(vendorRepository, never()).findByIdAndTenantId(any(), any());
    }

    // ========== approveInvoice ==========

    @Test
    void shouldApproveInvoiceSuccessfully() {
        // Given
        invoice.setStatus(APInvoiceStatus.PENDING_APPROVAL);
        Account apAccount = Account.builder()
                .id(10L)
                .code("2100")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(accountRepository.findByCodeAndTenantId("2100", TENANT_ID)).thenReturn(Optional.of(apAccount));
        when(glIntegrationService.postAPInvoice(any(APInvoice.class))).thenReturn(100L);
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(invoice);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));

        APInvoiceDto approvedDto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.APPROVED)
                .glPosted(true)
                .build();
        when(apInvoiceMapper.toDto(any(APInvoice.class))).thenReturn(approvedDto);

        // When
        APInvoiceDto result = apInvoiceService.approveInvoice(1L);

        // Then
        assertNotNull(result);
        assertEquals(APInvoiceStatus.APPROVED, result.getStatus());
        assertTrue(result.isGlPosted());
        verify(glIntegrationService).postAPInvoice(any(APInvoice.class));
        verify(apInvoiceRepository).save(any(APInvoice.class));
    }

    @Test
    void shouldThrowWhenApprovingNonDraftNonPendingInvoice() {
        // Given
        invoice.setStatus(APInvoiceStatus.PAID);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> apInvoiceService.approveInvoice(1L));
        verify(apInvoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenApprovingInvoiceWithFailedMatching() {
        // Given
        invoice.setStatus(APInvoiceStatus.PENDING_APPROVAL);
        invoice.setMatchingStatus("FAILED");
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> apInvoiceService.approveInvoice(1L));
        verify(apInvoiceRepository, never()).save(any());
    }

    // ========== cancelInvoice ==========

    @Test
    void shouldCancelInvoiceSuccessfully() {
        // Given
        invoice.setStatus(APInvoiceStatus.DRAFT);
        invoice.setPaidAmount(BigDecimal.ZERO);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(invoice);

        APInvoiceDto cancelledDto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.CANCELLED)
                .build();
        when(apInvoiceMapper.toDto(any(APInvoice.class))).thenReturn(cancelledDto);

        // When
        APInvoiceDto result = apInvoiceService.cancelInvoice(1L, "No longer needed");

        // Then
        assertNotNull(result);
        assertEquals(APInvoiceStatus.CANCELLED, result.getStatus());
        verify(apInvoiceRepository).save(any(APInvoice.class));
    }

    @Test
    void shouldThrowWhenCancellingPaidInvoice() {
        // Given
        invoice.setStatus(APInvoiceStatus.PAID);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> apInvoiceService.cancelInvoice(1L, "Test"));
        verify(apInvoiceRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCancellingInvoiceWithPayments() {
        // Given
        invoice.setStatus(APInvoiceStatus.PARTIAL);
        invoice.setPaidAmount(new BigDecimal("100.0000"));
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> apInvoiceService.cancelInvoice(1L, "Test"));
        verify(apInvoiceRepository, never()).save(any());
    }

    @Test
    void shouldCancelInvoiceAndReverseGLIfPosted() {
        // Given
        invoice.setStatus(APInvoiceStatus.APPROVED);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setGlPosted(true);
        invoice.setGlJournalEntryId(100L);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(invoice);

        APInvoiceDto cancelledDto = APInvoiceDto.builder()
                .id(1L)
                .status(APInvoiceStatus.CANCELLED)
                .build();
        when(apInvoiceMapper.toDto(any(APInvoice.class))).thenReturn(cancelledDto);

        // When
        APInvoiceDto result = apInvoiceService.cancelInvoice(1L, "Test");

        // Then
        assertNotNull(result);
        assertEquals(APInvoiceStatus.CANCELLED, result.getStatus());
        verify(glIntegrationService).reverseAPInvoice(any(APInvoice.class));
    }

    // ========== createFromReceiving ==========

    @Test
    void shouldCreateInvoiceFromReceivingSuccessfully() {
        // Given
        ReceivingOrder receiving = mock(ReceivingOrder.class);
        when(receiving.isApInvoiceCreated()).thenReturn(false);
        when(receiving.getVendor()).thenReturn(vendor);
        when(receiving.getReceivingNumber()).thenReturn("RCV-000001");
        when(receiving.getVendorInvoiceNumber()).thenReturn("VINV-001");
        when(receiving.getVendorInvoiceDate()).thenReturn(LocalDate.now());
        when(receiving.getReceivingDate()).thenReturn(LocalDate.now());
        when(receiving.getDiscountAmount()).thenReturn(BigDecimal.ZERO);
        when(receiving.getTaxAmount()).thenReturn(BigDecimal.ZERO);
        when(receiving.getShippingAmount()).thenReturn(BigDecimal.ZERO);
        when(receiving.getCurrency()).thenReturn("UZS");
        when(receiving.getPurchaseOrder()).thenReturn(null);
        when(receiving.getLines()).thenReturn(new ArrayList<>());

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(receivingOrderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(receiving));
        when(apInvoiceRepository.findMaxInvoiceNumber(TENANT_ID)).thenReturn(null);
        when(apInvoiceRepository.save(any(APInvoice.class))).thenAnswer(inv -> {
            APInvoice saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(apInvoiceMapper.toDto(any(APInvoice.class))).thenReturn(APInvoiceDto.builder()
                .id(10L)
                .invoiceNumber("AP-000001")
                .vendorName("Test Vendor")
                .status(APInvoiceStatus.DRAFT)
                .build());

        // When
        APInvoiceDto result = apInvoiceService.createFromReceiving(1L);

        // Then
        assertNotNull(result);
        assertEquals(APInvoiceStatus.DRAFT, result.getStatus());
        verify(receiving).setApInvoiceCreated(true);
        verify(receivingOrderRepository).save(receiving);
    }

    @Test
    void shouldThrowWhenReceivingOrderNotFound() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(receivingOrderRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> apInvoiceService.createFromReceiving(999L));
    }

    @Test
    void shouldThrowWhenAPInvoiceAlreadyCreatedFromReceiving() {
        // Given
        ReceivingOrder receiving = mock(ReceivingOrder.class);
        when(receiving.isApInvoiceCreated()).thenReturn(true);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(receivingOrderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(receiving));

        // When/Then
        assertThrows(BusinessException.class, () -> apInvoiceService.createFromReceiving(1L));
        verify(apInvoiceRepository, never()).save(any());
    }

    // ========== submitForApproval ==========

    @Test
    void shouldSubmitInvoiceForApprovalSuccessfully() {
        // Given
        invoice.setStatus(APInvoiceStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(invoice);

        APInvoiceDto pendingDto = APInvoiceDto.builder()
                .id(1L)
                .status(APInvoiceStatus.PENDING_APPROVAL)
                .build();
        when(apInvoiceMapper.toDto(any(APInvoice.class))).thenReturn(pendingDto);

        // When
        APInvoiceDto result = apInvoiceService.submitForApproval(1L);

        // Then
        assertNotNull(result);
        assertEquals(APInvoiceStatus.PENDING_APPROVAL, result.getStatus());
    }

    @Test
    void shouldThrowWhenSubmittingNonDraftInvoice() {
        // Given
        invoice.setStatus(APInvoiceStatus.APPROVED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> apInvoiceService.submitForApproval(1L));
    }

    // ========== holdInvoice / releaseHold ==========

    @Test
    void shouldHoldInvoiceSuccessfully() {
        // Given
        invoice.setStatus(APInvoiceStatus.APPROVED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(invoice);

        APInvoiceDto holdDto = APInvoiceDto.builder()
                .id(1L)
                .status(APInvoiceStatus.ON_HOLD)
                .build();
        when(apInvoiceMapper.toDto(any(APInvoice.class))).thenReturn(holdDto);

        // When
        APInvoiceDto result = apInvoiceService.holdInvoice(1L);

        // Then
        assertNotNull(result);
        assertEquals(APInvoiceStatus.ON_HOLD, result.getStatus());
    }

    @Test
    void shouldThrowWhenHoldingPaidInvoice() {
        // Given
        invoice.setStatus(APInvoiceStatus.PAID);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> apInvoiceService.holdInvoice(1L));
    }

    @Test
    void shouldReleaseHoldSuccessfully() {
        // Given
        invoice.setStatus(APInvoiceStatus.ON_HOLD);
        invoice.setApprovedAt(null);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(invoice);

        APInvoiceDto releasedDto = APInvoiceDto.builder()
                .id(1L)
                .status(APInvoiceStatus.DRAFT)
                .build();
        when(apInvoiceMapper.toDto(any(APInvoice.class))).thenReturn(releasedDto);

        // When
        APInvoiceDto result = apInvoiceService.releaseHold(1L);

        // Then
        assertNotNull(result);
        assertEquals(APInvoiceStatus.DRAFT, result.getStatus());
    }

    @Test
    void shouldThrowWhenReleasingNonHoldInvoice() {
        // Given
        invoice.setStatus(APInvoiceStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> apInvoiceService.releaseHold(1L));
    }

    // ========== Reporting methods ==========

    @Test
    void shouldGetTotalPayable() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.sumTotalBalanceDue(eq(TENANT_ID), any())).thenReturn(new BigDecimal("10000.00"));

        // When
        BigDecimal result = apInvoiceService.getTotalPayable();

        // Then
        assertEquals(new BigDecimal("10000.00"), result);
    }

    @Test
    void shouldGetVendorBalance() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.sumBalanceDueByVendor(eq(TENANT_ID), eq(1L), any())).thenReturn(new BigDecimal("5000.00"));

        // When
        BigDecimal result = apInvoiceService.getVendorBalance(1L);

        // Then
        assertEquals(new BigDecimal("5000.00"), result);
    }

    @Test
    void shouldGetOverdueBalance() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.sumOverdueBalance(eq(TENANT_ID), any(LocalDate.class), any())).thenReturn(new BigDecimal("2000.00"));

        // When
        BigDecimal result = apInvoiceService.getOverdueBalance();

        // Then
        assertEquals(new BigDecimal("2000.00"), result);
    }

    // ========== rejectInvoice ==========

    @Test
    void shouldRejectInvoiceSuccessfully() {
        // Given
        invoice.setStatus(APInvoiceStatus.PENDING_APPROVAL);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(invoice);

        APInvoiceDto rejectedDto = APInvoiceDto.builder()
                .id(1L)
                .status(APInvoiceStatus.DRAFT)
                .rejectionReason("Incorrect amounts")
                .build();
        when(apInvoiceMapper.toDto(any(APInvoice.class))).thenReturn(rejectedDto);

        // When
        APInvoiceDto result = apInvoiceService.rejectInvoice(1L, "Incorrect amounts");

        // Then
        assertNotNull(result);
        assertEquals(APInvoiceStatus.DRAFT, result.getStatus());
        assertEquals("Incorrect amounts", result.getRejectionReason());
    }

    @Test
    void shouldThrowWhenRejectingNonPendingInvoice() {
        // Given
        invoice.setStatus(APInvoiceStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> apInvoiceService.rejectInvoice(1L, "Test"));
    }

    // ========== updateInvoice ==========

    @Test
    void shouldUpdateInvoiceSuccessfully() {
        // Given
        invoice.setStatus(APInvoiceStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(apInvoiceLineMapper.toEntity(any(CreateAPInvoiceLineRequest.class))).thenReturn(invoiceLine);
        when(apInvoiceRepository.save(any(APInvoice.class))).thenReturn(invoice);
        when(apInvoiceMapper.toDto(any(APInvoice.class))).thenReturn(invoiceDto);

        // When
        APInvoiceDto result = apInvoiceService.updateInvoice(1L, createRequest);

        // Then
        assertNotNull(result);
        verify(apInvoiceMapper).updateEntity(eq(createRequest), any(APInvoice.class));
        verify(apInvoiceRepository).save(any(APInvoice.class));
    }

    @Test
    void shouldThrowWhenUpdatingNonDraftInvoice() {
        // Given
        invoice.setStatus(APInvoiceStatus.APPROVED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class, () -> apInvoiceService.updateInvoice(1L, createRequest));
    }
}
