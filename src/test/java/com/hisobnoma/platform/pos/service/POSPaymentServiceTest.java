package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.service.CustomerService;
import com.hisobnoma.platform.pos.dto.AddPaymentRequest;
import com.hisobnoma.platform.pos.dto.POSPaymentDto;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.mapper.POSPaymentMapper;
import com.hisobnoma.platform.pos.repository.POSPaymentRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class POSPaymentServiceTest {

    @Mock
    private POSPaymentRepository paymentRepository;

    @Mock
    private POSTransactionRepository transactionRepository;

    @Mock
    private POSPaymentMapper paymentMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private POSPaymentService posPaymentService;

    private POSTransaction transaction;
    private POSPayment payment;
    private POSPaymentDto paymentDto;
    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        transaction = POSTransaction.builder()
                .id(1L)
                .transactionNumber("TXN-001")
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.PENDING)
                .totalAmount(new BigDecimal("10000"))
                .paidAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .subtotal(new BigDecimal("10000"))
                .taxAmount(BigDecimal.ZERO)
                .changeAmount(BigDecimal.ZERO)
                .lines(new ArrayList<>())
                .payments(new ArrayList<>())
                .tenantId(TENANT_ID)
                .build();

        payment = POSPayment.builder()
                .id(1L)
                .paymentNumber(1)
                .paymentType(POSPaymentType.CASH)
                .status(POSPaymentStatus.APPROVED)
                .amount(new BigDecimal("10000"))
                .tenderedAmount(new BigDecimal("10000"))
                .changeAmount(BigDecimal.ZERO)
                .build();

        paymentDto = POSPaymentDto.builder()
                .id(1L)
                .transactionId(1L)
                .paymentNumber(1)
                .paymentType(POSPaymentType.CASH)
                .status(POSPaymentStatus.APPROVED)
                .amount(new BigDecimal("10000"))
                .build();
    }

    // ====== findByTransaction ======

    @Test
    void findByTransaction_found_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findByTransactionId(1L)).thenReturn(List.of(payment));
        when(paymentMapper.toDtoList(List.of(payment))).thenReturn(List.of(paymentDto));

        // When
        List<POSPaymentDto> result = posPaymentService.findByTransaction(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(POSPaymentType.CASH, result.get(0).getPaymentType());
    }

    @Test
    void findByTransaction_transactionNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> posPaymentService.findByTransaction(999L));
    }

    // ====== findById ======

    @Test
    void findById_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findByIdAndTransactionId(1L, 1L)).thenReturn(Optional.of(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        // When
        POSPaymentDto result = posPaymentService.findById(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_transactionNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> posPaymentService.findById(999L, 1L));
    }

    @Test
    void findById_paymentNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findByIdAndTransactionId(999L, 1L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> posPaymentService.findById(1L, 999L));
    }

    // ====== addPayment ======

    @Test
    void addPayment_cashPayment_success() {
        // Given
        AddPaymentRequest request = AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("10000"))
                .tenderedAmount(new BigDecimal("10000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findMaxPaymentNumberByTransactionId(1L)).thenReturn(null);
        when(paymentRepository.save(any(POSPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(POSTransaction.class))).thenReturn(transaction);
        when(paymentMapper.toDto(any(POSPayment.class))).thenReturn(paymentDto);

        // When
        POSPaymentDto result = posPaymentService.addPayment(1L, request);

        // Then
        assertNotNull(result);
        verify(paymentRepository).save(any(POSPayment.class));
        verify(transactionRepository).save(any(POSTransaction.class));
    }

    @Test
    void addPayment_transactionNotFound_throwsNotFoundException() {
        // Given
        AddPaymentRequest request = AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("10000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> posPaymentService.addPayment(999L, request));
    }

    @Test
    void addPayment_transactionNotPending_throwsBusinessException() {
        // Given
        transaction.setStatus(TransactionStatus.COMPLETED);
        AddPaymentRequest request = AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("10000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        // When/Then
        assertThrows(BusinessException.class, () -> posPaymentService.addPayment(1L, request));
    }

    @Test
    void addPayment_nonCashOverpayment_throwsBusinessException() {
        // Given
        AddPaymentRequest request = AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CARD)
                .amount(new BigDecimal("20000")) // exceeds total
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        // When/Then
        assertThrows(BusinessException.class, () -> posPaymentService.addPayment(1L, request));
    }

    @Test
    void addPayment_fullyPaid_throwsBusinessException() {
        // Given
        transaction.setPaidAmount(new BigDecimal("10000")); // already fully paid
        AddPaymentRequest request = AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("1000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        // When/Then
        assertThrows(BusinessException.class, () -> posPaymentService.addPayment(1L, request));
    }

    @Test
    void addPayment_creditPayment_validatesCredit() {
        // Given
        Customer customer = Customer.builder().id(5L).name("Test Customer").build();
        transaction.setCustomer(customer);

        AddPaymentRequest request = AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CREDIT)
                .amount(new BigDecimal("10000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findMaxPaymentNumberByTransactionId(1L)).thenReturn(null);
        when(customerService.canBeInvoiced(eq(5L), any(BigDecimal.class))).thenReturn(true);
        when(paymentRepository.save(any(POSPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(POSTransaction.class))).thenReturn(transaction);
        when(paymentMapper.toDto(any(POSPayment.class))).thenReturn(paymentDto);

        // When
        POSPaymentDto result = posPaymentService.addPayment(1L, request);

        // Then
        assertNotNull(result);
        verify(customerService).canBeInvoiced(eq(5L), any(BigDecimal.class));
    }

    @Test
    void addPayment_creditPayment_noCustomer_throwsBusinessException() {
        // Given
        transaction.setCustomer(null);

        AddPaymentRequest request = AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CREDIT)
                .amount(new BigDecimal("10000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findMaxPaymentNumberByTransactionId(1L)).thenReturn(null);

        // When/Then
        assertThrows(BusinessException.class, () -> posPaymentService.addPayment(1L, request));
    }

    @Test
    void addPayment_creditPayment_exceedsLimit_throwsBusinessException() {
        // Given
        Customer customer = Customer.builder().id(5L).name("Test Customer").build();
        transaction.setCustomer(customer);

        AddPaymentRequest request = AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CREDIT)
                .amount(new BigDecimal("10000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findMaxPaymentNumberByTransactionId(1L)).thenReturn(null);
        when(customerService.canBeInvoiced(eq(5L), any(BigDecimal.class))).thenReturn(false);

        // When/Then
        assertThrows(BusinessException.class, () -> posPaymentService.addPayment(1L, request));
    }

    // ====== voidPayment ======

    @Test
    void voidPayment_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findByIdAndTransactionId(1L, 1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(POSPayment.class))).thenReturn(payment);
        when(transactionRepository.save(any(POSTransaction.class))).thenReturn(transaction);
        when(paymentMapper.toDto(any(POSPayment.class))).thenReturn(paymentDto);

        // When
        POSPaymentDto result = posPaymentService.voidPayment(1L, 1L, "Customer request");

        // Then
        assertNotNull(result);
        verify(paymentRepository).save(any(POSPayment.class));
        verify(transactionRepository).save(any(POSTransaction.class));
    }

    @Test
    void voidPayment_transactionNotPending_throwsBusinessException() {
        // Given
        transaction.setStatus(TransactionStatus.COMPLETED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        // When/Then
        assertThrows(BusinessException.class, () -> posPaymentService.voidPayment(1L, 1L, "reason"));
    }

    @Test
    void voidPayment_alreadyVoided_throwsBusinessException() {
        // Given
        payment.setStatus(POSPaymentStatus.VOIDED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findByIdAndTransactionId(1L, 1L)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class, () -> posPaymentService.voidPayment(1L, 1L, "reason"));
    }

    @Test
    void voidPayment_paymentNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findByIdAndTransactionId(999L, 1L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> posPaymentService.voidPayment(1L, 999L, "reason"));
    }

    // ====== refundPayment ======

    @Test
    void refundPayment_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findByIdAndTransactionId(1L, 1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(POSPayment.class))).thenReturn(payment);
        when(transactionRepository.save(any(POSTransaction.class))).thenReturn(transaction);
        when(paymentMapper.toDto(any(POSPayment.class))).thenReturn(paymentDto);

        // When
        POSPaymentDto result = posPaymentService.refundPayment(1L, 1L, new BigDecimal("5000"), "Partial refund");

        // Then
        assertNotNull(result);
        verify(paymentRepository).save(any(POSPayment.class));
    }

    @Test
    void refundPayment_notApproved_throwsBusinessException() {
        // Given
        payment.setStatus(POSPaymentStatus.VOIDED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findByIdAndTransactionId(1L, 1L)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class,
                () -> posPaymentService.refundPayment(1L, 1L, new BigDecimal("5000"), "refund"));
    }

    @Test
    void refundPayment_amountExceedsPayment_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findByIdAndTransactionId(1L, 1L)).thenReturn(Optional.of(payment));

        // When/Then
        assertThrows(BusinessException.class,
                () -> posPaymentService.refundPayment(1L, 1L, new BigDecimal("20000"), "refund"));
    }

    @Test
    void refundPayment_paymentNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(transactionRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.findByIdAndTransactionId(999L, 1L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> posPaymentService.refundPayment(1L, 999L, new BigDecimal("5000"), "refund"));
    }

    // ====== getTotalPaidAmount ======

    @Test
    void getTotalPaidAmount_returnsTotal() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.sumApprovedByTransactionId(1L)).thenReturn(new BigDecimal("10000"));

        // When
        BigDecimal result = posPaymentService.getTotalPaidAmount(1L);

        // Then
        assertEquals(new BigDecimal("10000"), result);
    }

    @Test
    void getTotalPaidAmount_nullResult_returnsZero() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(paymentRepository.sumApprovedByTransactionId(1L)).thenReturn(null);

        // When
        BigDecimal result = posPaymentService.getTotalPaidAmount(1L);

        // Then
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void getTotalPaidAmount_transactionNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> posPaymentService.getTotalPaidAmount(999L));
    }

    // ====== getPaymentTotalByType ======

    @Test
    void getPaymentTotalByType_returnsTotal() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(paymentRepository.sumByShiftIdAndPaymentTypeAndTenantId(1L, POSPaymentType.CASH, TENANT_ID))
                .thenReturn(new BigDecimal("50000"));

        // When
        BigDecimal result = posPaymentService.getPaymentTotalByType(1L, POSPaymentType.CASH);

        // Then
        assertEquals(new BigDecimal("50000"), result);
    }

    @Test
    void getPaymentTotalByType_nullResult_returnsZero() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(paymentRepository.sumByShiftIdAndPaymentTypeAndTenantId(1L, POSPaymentType.CARD, TENANT_ID))
                .thenReturn(null);

        // When
        BigDecimal result = posPaymentService.getPaymentTotalByType(1L, POSPaymentType.CARD);

        // Then
        assertEquals(BigDecimal.ZERO, result);
    }
}
