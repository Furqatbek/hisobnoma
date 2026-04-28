package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.delivery.repository.DeliveryRegionRepository;
import com.hisobnoma.platform.delivery.repository.DeliveryVillageRepository;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
import com.hisobnoma.platform.finance.service.GLIntegrationService;
import com.hisobnoma.platform.finance.service.TaxCalculationService;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductUomRepository;
import com.hisobnoma.platform.inventory.repository.ProductVariantRepository;
import com.hisobnoma.platform.inventory.service.StockService;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.mapper.POSTransactionMapper;
import com.hisobnoma.platform.pos.repository.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class POSTransactionServiceTest {

    @Mock
    private POSTransactionRepository transactionRepository;
    @Mock
    private POSTransactionLineRepository lineRepository;
    @Mock
    private POSPaymentRepository paymentRepository;
    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private POSTerminalRepository terminalRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private ProductUomRepository productUomRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private DeliveryRegionRepository deliveryRegionRepository;
    @Mock
    private DeliveryVillageRepository deliveryVillageRepository;
    @Mock
    private POSTransactionMapper transactionMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;
    @Mock
    private StockService stockService;
    @Mock
    private TaxCalculationService taxCalculationService;
    @Mock
    private GLIntegrationService glIntegrationService;
    @Mock
    private ARInvoiceService arInvoiceService;
    @Mock
    private ShiftService shiftService;

    @InjectMocks
    private POSTransactionService transactionService;

    private POSTerminal terminal;
    private Shift shift;
    private POSTransaction transaction;
    private Product product;
    private Location location;
    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        location = Location.builder()
                .id(1L)
                .name("Main Store")
                .build();

        terminal = POSTerminal.builder()
                .id(1L)
                .terminalCode("TERM-001")
                .name("Terminal 1")
                .location(location)
                .tenantId(TENANT_ID)
                .active(true)
                .build();

        shift = Shift.builder()
                .id(1L)
                .shiftNumber("SH20260426-0001")
                .terminal(terminal)
                .cashierId(USER_ID)
                .cashierName("Test Cashier")
                .status(ShiftStatus.OPEN)
                .openedAt(Instant.now())
                .openingCash(new BigDecimal("100000"))
                .tenantId(TENANT_ID)
                .build();

        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .sku("PROD-001")
                .sellingPrice(new BigDecimal("50000"))
                .costPrice(new BigDecimal("30000"))
                .trackInventory(true)
                .build();

        transaction = POSTransaction.builder()
                .id(1L)
                .transactionNumber("TX20260426-00001")
                .terminal(terminal)
                .shift(shift)
                .cashierId(USER_ID)
                .cashierName("Test Cashier")
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.PENDING)
                .tenantId(TENANT_ID)
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .lines(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();
    }

    // ==================== findAll ====================

    @Test
    void findAll_returnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<POSTransaction> page = new PageImpl<>(List.of(transaction), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(transactionMapper.toDtoWithoutDetails(any())).thenReturn(
                POSTransactionDto.builder().id(1L).transactionNumber("TX20260426-00001").build());

        // When
        Page<POSTransactionDto> result = transactionService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findByTenantId(TENANT_ID, pageable);
    }

    @Test
    void findAll_returnsEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<POSTransaction> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

        // When
        Page<POSTransactionDto> result = transactionService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== findById ====================

    @Test
    void findById_found() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toDto(transaction)).thenReturn(
                POSTransactionDto.builder().id(1L).transactionNumber("TX20260426-00001").build());

        // When
        POSTransactionDto result = transactionService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("TX20260426-00001", result.getTransactionNumber());
    }

    @Test
    void findById_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> transactionService.findById(999L));
    }

    // ==================== createTransaction ====================

    @Test
    void createTransaction_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(securityContextHelper.getCurrentUsername()).thenReturn("testuser");
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));
        when(shiftRepository.findByTerminalIdAndStatusAndTenantId(1L, ShiftStatus.OPEN, TENANT_ID))
                .thenReturn(Optional.of(shift));
        when(transactionRepository.findMaxTransactionNumberByPrefixAndTenantId(any(), eq(TENANT_ID)))
                .thenReturn(null);
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            POSTransaction saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(transactionMapper.toDto(any())).thenReturn(
                POSTransactionDto.builder().id(1L).status(TransactionStatus.PENDING).build());

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .terminalId(1L)
                .transactionType(TransactionType.SALE)
                .build();

        // When
        POSTransactionDto result = transactionService.createTransaction(request);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        verify(transactionRepository).save(any());
    }

    @Test
    void createTransaction_terminalNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .terminalId(999L)
                .build();

        // When/Then
        assertThrows(NotFoundException.class, () -> transactionService.createTransaction(request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_noOpenShift_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));
        when(shiftRepository.findByTerminalIdAndStatusAndTenantId(1L, ShiftStatus.OPEN, TENANT_ID))
                .thenReturn(Optional.empty());

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .terminalId(1L)
                .build();

        // When/Then
        assertThrows(BusinessException.class, () -> transactionService.createTransaction(request));
        verify(transactionRepository, never()).save(any());
    }

    // ==================== addLine ====================

    @Test
    void addLine_productExists_lineAdded() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(lineRepository.findMaxLineNumberByTransactionId(1L)).thenReturn(null);
        when(lineRepository.save(any(POSTransactionLine.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toDto(any())).thenReturn(
                POSTransactionDto.builder().id(1L).build());

        AddLineRequest request = AddLineRequest.builder()
                .productId(1L)
                .quantity(new BigDecimal("2"))
                .build();

        // When
        POSTransactionDto result = transactionService.addLine(1L, request);

        // Then
        assertNotNull(result);
        verify(transactionRepository).save(any());
    }

    @Test
    void addLine_productNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        AddLineRequest request = AddLineRequest.builder()
                .productId(999L)
                .quantity(new BigDecimal("1"))
                .build();

        // When/Then
        assertThrows(NotFoundException.class, () -> transactionService.addLine(1L, request));
    }

    @Test
    void addLine_transactionCompleted_throwsBusinessException() {
        // Given
        transaction.setStatus(TransactionStatus.COMPLETED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        AddLineRequest request = AddLineRequest.builder()
                .productId(1L)
                .quantity(new BigDecimal("1"))
                .build();

        // When/Then
        assertThrows(BusinessException.class, () -> transactionService.addLine(1L, request));
    }

    // ==================== updateLine ====================

    @Test
    void updateLine_qtyUpdated_lineTotalRecalculated() {
        // Given
        POSTransactionLine line = POSTransactionLine.builder()
                .id(10L)
                .product(product)
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("50000"))
                .lineTotal(new BigDecimal("50000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .isReturn(false)
                .build();
        line.setTransaction(transaction);
        transaction.getLines().add(line);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(lineRepository.findByIdAndTransactionId(10L, 1L)).thenReturn(Optional.of(line));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toDto(any())).thenReturn(
                POSTransactionDto.builder().id(1L).build());

        UpdateLineRequest request = UpdateLineRequest.builder()
                .quantity(new BigDecimal("3"))
                .build();

        // When
        POSTransactionDto result = transactionService.updateLine(1L, 10L, request);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("3"), line.getQuantity());
        verify(transactionRepository).save(any());
    }

    @Test
    void updateLine_lineNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(lineRepository.findByIdAndTransactionId(999L, 1L)).thenReturn(Optional.empty());

        UpdateLineRequest request = UpdateLineRequest.builder()
                .quantity(new BigDecimal("2"))
                .build();

        // When/Then
        assertThrows(NotFoundException.class, () -> transactionService.updateLine(1L, 999L, request));
    }

    @Test
    void updateLine_completedTransaction_throwsBusinessException() {
        // Given
        transaction.setStatus(TransactionStatus.COMPLETED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        UpdateLineRequest request = UpdateLineRequest.builder()
                .quantity(new BigDecimal("2"))
                .build();

        // When/Then
        assertThrows(BusinessException.class, () -> transactionService.updateLine(1L, 10L, request));
    }

    // ==================== removeLine ====================

    @Test
    void removeLine_lineRemoved() {
        // Given
        POSTransactionLine line1 = POSTransactionLine.builder()
                .id(10L)
                .product(product)
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("50000"))
                .lineTotal(new BigDecimal("50000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .isReturn(false)
                .build();
        POSTransactionLine line2 = POSTransactionLine.builder()
                .id(11L)
                .product(product)
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("30000"))
                .lineTotal(new BigDecimal("30000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .isReturn(false)
                .build();
        line1.setTransaction(transaction);
        line2.setTransaction(transaction);
        transaction.getLines().add(line1);
        transaction.getLines().add(line2);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(lineRepository.findByIdAndTransactionId(10L, 1L)).thenReturn(Optional.of(line1));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toDto(any())).thenReturn(
                POSTransactionDto.builder().id(1L).build());

        // When
        POSTransactionDto result = transactionService.removeLine(1L, 10L);

        // Then
        assertNotNull(result);
        verify(lineRepository).delete(line1);
    }

    @Test
    void removeLine_lineNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(lineRepository.findByIdAndTransactionId(999L, 1L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> transactionService.removeLine(1L, 999L));
    }

    // ==================== voidTransaction ====================

    @Test
    void voidTransaction_pending_statusSetToVoided() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toDto(any())).thenReturn(
                POSTransactionDto.builder().id(1L).status(TransactionStatus.VOIDED).build());

        VoidTransactionRequest request = VoidTransactionRequest.builder().reason("Customer request").build();

        // When
        POSTransactionDto result = transactionService.voidTransaction(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.VOIDED, result.getStatus());
        verify(transactionRepository).save(any());
        verify(shiftService).recalculateShiftTotals(shift.getId());
    }

    @Test
    void voidTransaction_alreadyVoided_throwsBusinessException() {
        // Given
        transaction.setStatus(TransactionStatus.VOIDED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        VoidTransactionRequest request = VoidTransactionRequest.builder().reason("Test").build();

        // When/Then
        assertThrows(BusinessException.class, () -> transactionService.voidTransaction(1L, request));
    }

    @Test
    void voidTransaction_completed_reversesEffects() {
        // Given
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setStockDeducted(true);
        transaction.setGlPosted(true);
        transaction.setGlJournalEntryId(100L);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toDto(any())).thenReturn(
                POSTransactionDto.builder().id(1L).status(TransactionStatus.VOIDED).build());

        VoidTransactionRequest request = VoidTransactionRequest.builder().reason("Reversal needed").build();

        // When
        POSTransactionDto result = transactionService.voidTransaction(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.VOIDED, result.getStatus());
        verify(glIntegrationService).reverseSalesTransaction(100L);
    }

    // ==================== completeTransaction ====================

    @Test
    void completeTransaction_fullPayment_statusCompletedAndStockDecremented() {
        // Given
        POSTransactionLine line = POSTransactionLine.builder()
                .id(10L)
                .product(product)
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("50000"))
                .lineTotal(new BigDecimal("50000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .isReturn(false)
                .build();
        line.setTransaction(transaction);
        transaction.getLines().add(line);
        transaction.setSubtotal(new BigDecimal("50000"));
        transaction.setTotalAmount(new BigDecimal("50000"));
        transaction.setPaidAmount(new BigDecimal("50000"));

        POSPayment payment = POSPayment.builder()
                .id(1L)
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("50000"))
                .status(POSPaymentStatus.APPROVED)
                .build();
        payment.setTransaction(transaction);
        transaction.getPayments().add(payment);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toDto(any())).thenReturn(
                POSTransactionDto.builder().id(1L).status(TransactionStatus.COMPLETED).build());

        // When
        POSTransactionDto result = transactionService.completeTransaction(1L);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        verify(stockService).deductStock(eq(product.getId()), eq(location.getId()),
                eq(BigDecimal.ONE), eq("POS_SALE"), eq(1L), any());
    }

    @Test
    void completeTransaction_underpayment_throwsBusinessException() {
        // Given
        POSTransactionLine line = POSTransactionLine.builder()
                .id(10L)
                .product(product)
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("50000"))
                .lineTotal(new BigDecimal("50000"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .isReturn(false)
                .build();
        line.setTransaction(transaction);
        transaction.getLines().add(line);
        transaction.setSubtotal(new BigDecimal("50000"));
        transaction.setTotalAmount(new BigDecimal("50000"));
        transaction.setPaidAmount(new BigDecimal("10000"));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        // When/Then
        assertThrows(BusinessException.class, () -> transactionService.completeTransaction(1L));
    }

    @Test
    void completeTransaction_noLines_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        // When/Then
        assertThrows(BusinessException.class, () -> transactionService.completeTransaction(1L));
    }

    @Test
    void completeTransaction_notPending_throwsBusinessException() {
        // Given
        transaction.setStatus(TransactionStatus.COMPLETED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        // When/Then
        assertThrows(BusinessException.class, () -> transactionService.completeTransaction(1L));
    }

    // ==================== holdTransaction ====================

    @Test
    void holdTransaction_pending_statusSetToHeld() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toDto(any())).thenReturn(
                POSTransactionDto.builder().id(1L).status(TransactionStatus.HELD).build());

        HoldTransactionRequest request = HoldTransactionRequest.builder().reason("Customer stepped away").build();

        // When
        POSTransactionDto result = transactionService.holdTransaction(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.HELD, result.getStatus());
        verify(transactionRepository).save(any());
    }

    @Test
    void holdTransaction_notPending_throwsBusinessException() {
        // Given
        transaction.setStatus(TransactionStatus.COMPLETED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        HoldTransactionRequest request = HoldTransactionRequest.builder().reason("Test").build();

        // When/Then
        assertThrows(BusinessException.class, () -> transactionService.holdTransaction(1L, request));
    }

    // ==================== recallTransaction ====================

    @Test
    void recallTransaction_held_statusSetToPending() {
        // Given
        transaction.setStatus(TransactionStatus.HELD);
        transaction.setHeldAt(Instant.now());
        transaction.setHeldBy(USER_ID);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toDto(any())).thenReturn(
                POSTransactionDto.builder().id(1L).status(TransactionStatus.PENDING).build());

        // When
        POSTransactionDto result = transactionService.recallTransaction(1L);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.PENDING, result.getStatus());
    }

    @Test
    void recallTransaction_notHeld_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        // When/Then
        assertThrows(BusinessException.class, () -> transactionService.recallTransaction(1L));
    }

    // ==================== findByShift ====================

    @Test
    void findByShift_returnsTransactionsForShift() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByShiftIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(transaction));
        when(transactionMapper.toDtoList(any())).thenReturn(
                List.of(POSTransactionDto.builder().id(1L).build()));

        // When
        List<POSTransactionDto> result = transactionService.findByShift(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== findHeldTransactions ====================

    @Test
    void findHeldTransactions_returnsHeldOnly() {
        // Given
        transaction.setStatus(TransactionStatus.HELD);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByShiftIdAndStatusAndTenantId(1L, TransactionStatus.HELD, TENANT_ID))
                .thenReturn(List.of(transaction));
        when(transactionMapper.toDtoList(any())).thenReturn(
                List.of(POSTransactionDto.builder().id(1L).status(TransactionStatus.HELD).build()));

        // When
        List<POSTransactionDto> result = transactionService.findHeldTransactions(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
