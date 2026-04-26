package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.mapper.CashOperationMapper;
import com.hisobnoma.platform.pos.mapper.ShiftMapper;
import com.hisobnoma.platform.pos.repository.*;
import com.hisobnoma.platform.telegram.service.TelegramDailyReportService;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private CashOperationRepository cashOperationRepository;
    @Mock
    private POSTerminalRepository terminalRepository;
    @Mock
    private POSTransactionRepository transactionRepository;
    @Mock
    private POSPaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ShiftMapper shiftMapper;
    @Mock
    private CashOperationMapper cashOperationMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;
    @Mock
    private POSTerminalService terminalService;
    @Mock
    private TelegramDailyReportService telegramDailyReportService;

    @InjectMocks
    private ShiftService shiftService;

    private POSTerminal terminal;
    private Shift shift;
    private User user;
    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        Location location = Location.builder()
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
                .cashierName("John Doe")
                .status(ShiftStatus.OPEN)
                .openedAt(Instant.now())
                .openingCash(new BigDecimal("100000"))
                .tenantId(TENANT_ID)
                .totalSales(BigDecimal.ZERO)
                .totalReturns(BigDecimal.ZERO)
                .totalDiscounts(BigDecimal.ZERO)
                .totalTaxes(BigDecimal.ZERO)
                .cashPayments(BigDecimal.ZERO)
                .cardPayments(BigDecimal.ZERO)
                .otherPayments(BigDecimal.ZERO)
                .cashRefunds(BigDecimal.ZERO)
                .cardRefunds(BigDecimal.ZERO)
                .otherRefunds(BigDecimal.ZERO)
                .cashIn(BigDecimal.ZERO)
                .cashOut(BigDecimal.ZERO)
                .build();

        user = new User();
        user.setId(USER_ID);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setUsername("johndoe");
    }

    // ==================== findAll ====================

    @Test
    void findAll_returnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shift> page = new PageImpl<>(List.of(shift), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(shiftMapper.toDto(any())).thenReturn(ShiftDto.builder().id(1L).build());

        // When
        Page<ShiftDto> result = shiftService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ==================== findById ====================

    @Test
    void findById_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(shift));
        when(shiftMapper.toDto(shift)).thenReturn(ShiftDto.builder().id(1L).status(ShiftStatus.OPEN).build());

        // When
        ShiftDto result = shiftService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(ShiftStatus.OPEN, result.getStatus());
    }

    @Test
    void findById_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> shiftService.findById(999L));
    }

    // ==================== openShift ====================

    @Test
    void openShift_noExistingOpenShift_shiftCreatedWithOpenStatus() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));
        when(shiftRepository.findByTerminalIdAndStatusAndTenantId(1L, ShiftStatus.OPEN, TENANT_ID))
                .thenReturn(Optional.empty());
        when(shiftRepository.findByCashierIdAndStatusAndTenantId(USER_ID, ShiftStatus.OPEN, TENANT_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(shiftRepository.findMaxShiftNumberByPrefixAndTenantId(any(), eq(TENANT_ID))).thenReturn(null);
        when(shiftRepository.save(any())).thenAnswer(inv -> {
            Shift saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(shiftMapper.toDto(any())).thenReturn(
                ShiftDto.builder().id(1L).status(ShiftStatus.OPEN)
                        .openingCash(new BigDecimal("100000")).build());

        OpenShiftRequest request = OpenShiftRequest.builder()
                .terminalId(1L)
                .openingCash(new BigDecimal("100000"))
                .build();

        // When
        ShiftDto result = shiftService.openShift(request);

        // Then
        assertNotNull(result);
        assertEquals(ShiftStatus.OPEN, result.getStatus());
        assertEquals(new BigDecimal("100000"), result.getOpeningCash());
        verify(shiftRepository).save(any());
        verify(terminalService).updateCurrentShift(eq(1L), any());
    }

    @Test
    void openShift_alreadyOpenShift_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));
        when(shiftRepository.findByTerminalIdAndStatusAndTenantId(1L, ShiftStatus.OPEN, TENANT_ID))
                .thenReturn(Optional.of(shift));

        OpenShiftRequest request = OpenShiftRequest.builder()
                .terminalId(1L)
                .openingCash(new BigDecimal("100000"))
                .build();

        // When/Then
        assertThrows(BusinessException.class, () -> shiftService.openShift(request));
        verify(shiftRepository, never()).save(any());
    }

    @Test
    void openShift_terminalNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(terminalRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        OpenShiftRequest request = OpenShiftRequest.builder()
                .terminalId(999L)
                .openingCash(new BigDecimal("100000"))
                .build();

        // When/Then
        assertThrows(NotFoundException.class, () -> shiftService.openShift(request));
    }

    @Test
    void openShift_terminalNotActive_throwsBusinessException() {
        // Given
        terminal.setActive(false);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));

        OpenShiftRequest request = OpenShiftRequest.builder()
                .terminalId(1L)
                .openingCash(new BigDecimal("100000"))
                .build();

        // When/Then
        assertThrows(BusinessException.class, () -> shiftService.openShift(request));
    }

    // ==================== closeShift ====================

    @Test
    void closeShift_openShift_statusClosedAndSummaryCalculated() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(shift));
        when(transactionRepository.countUnresolvedByShiftId(1L, TENANT_ID)).thenReturn(0L);
        // Mock recalculation queries
        when(transactionRepository.sumSalesByShiftId(1L, TENANT_ID)).thenReturn(new BigDecimal("500000"));
        when(transactionRepository.sumReturnsByShiftId(1L, TENANT_ID)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumDiscountsByShiftId(1L, TENANT_ID)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumTaxesByShiftId(1L, TENANT_ID)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countCompletedByShiftId(1L, TENANT_ID)).thenReturn(5L);
        when(transactionRepository.countVoidedByShiftId(1L, TENANT_ID)).thenReturn(0L);
        when(transactionRepository.countReturnsByShiftId(1L, TENANT_ID)).thenReturn(0L);
        when(paymentRepository.sumSalesByShiftIdAndPaymentType(1L, POSPaymentType.CASH)).thenReturn(new BigDecimal("300000"));
        when(paymentRepository.sumSalesByShiftIdAndPaymentType(1L, POSPaymentType.CARD)).thenReturn(new BigDecimal("200000"));
        when(paymentRepository.sumAllSalesByShiftId(1L)).thenReturn(new BigDecimal("500000"));
        when(paymentRepository.sumRefundsByShiftIdAndPaymentType(1L, POSPaymentType.CASH)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.sumRefundsByShiftIdAndPaymentType(1L, POSPaymentType.CARD)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.sumAllRefundsByShiftId(1L)).thenReturn(BigDecimal.ZERO);
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shiftMapper.toDto(any())).thenReturn(
                ShiftDto.builder().id(1L).status(ShiftStatus.CLOSED)
                        .totalSales(new BigDecimal("500000")).transactionCount(5).build());

        CloseShiftRequest request = CloseShiftRequest.builder()
                .closingCash(new BigDecimal("400000"))
                .closingNotes("End of day")
                .build();

        // When
        ShiftDto result = shiftService.closeShift(1L, request);

        // Then
        assertNotNull(result);
        assertEquals(ShiftStatus.CLOSED, result.getStatus());
        verify(shiftRepository).save(any());
        verify(terminalService).updateCurrentShift(eq(terminal.getId()), isNull());
    }

    @Test
    void closeShift_alreadyClosed_throwsBusinessException() {
        // Given
        shift.setStatus(ShiftStatus.CLOSED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(shift));

        CloseShiftRequest request = CloseShiftRequest.builder()
                .closingCash(new BigDecimal("400000"))
                .build();

        // When/Then
        assertThrows(BusinessException.class, () -> shiftService.closeShift(1L, request));
    }

    @Test
    void closeShift_openTransactionsExist_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByIdAndTenantIdForUpdate(1L, TENANT_ID)).thenReturn(Optional.of(shift));
        when(transactionRepository.countUnresolvedByShiftId(1L, TENANT_ID)).thenReturn(2L);

        CloseShiftRequest request = CloseShiftRequest.builder()
                .closingCash(new BigDecimal("400000"))
                .build();

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class, () -> shiftService.closeShift(1L, request));
        assertTrue(ex.getMessage().contains("unresolved"));
    }

    // ==================== getCurrentShift ====================

    @Test
    void getCurrentShift_found_returnsShiftDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByTerminalIdAndStatusAndTenantId(1L, ShiftStatus.OPEN, TENANT_ID))
                .thenReturn(Optional.of(shift));
        when(shiftMapper.toDto(shift)).thenReturn(ShiftDto.builder().id(1L).status(ShiftStatus.OPEN).build());

        // When
        ShiftDto result = shiftService.getCurrentShift(1L);

        // Then
        assertNotNull(result);
        assertEquals(ShiftStatus.OPEN, result.getStatus());
    }

    @Test
    void getCurrentShift_noOpenShift_returnsNull() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByTerminalIdAndStatusAndTenantId(1L, ShiftStatus.OPEN, TENANT_ID))
                .thenReturn(Optional.empty());

        // When
        ShiftDto result = shiftService.getCurrentShift(1L);

        // Then
        assertNull(result);
    }

    // ==================== findByTerminal ====================

    @Test
    void findByTerminal_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Shift> page = new PageImpl<>(List.of(shift), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByTerminalIdAndTenantId(1L, TENANT_ID, pageable)).thenReturn(page);
        when(shiftMapper.toDto(any())).thenReturn(ShiftDto.builder().id(1L).build());

        // When
        Page<ShiftDto> result = shiftService.findByTerminal(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ==================== findOpenShifts ====================

    @Test
    void findOpenShifts_returnsOpenShiftsOnly() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findOpenShiftsByTenantId(TENANT_ID)).thenReturn(List.of(shift));
        when(shiftMapper.toDtoList(any())).thenReturn(
                List.of(ShiftDto.builder().id(1L).status(ShiftStatus.OPEN).build()));

        // When
        List<ShiftDto> result = shiftService.findOpenShifts();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ShiftStatus.OPEN, result.get(0).getStatus());
    }

    // ==================== recalculateShiftTotals ====================

    @Test
    void recalculateShiftTotals_correctTotals() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(shift));
        when(transactionRepository.sumSalesByShiftId(1L, TENANT_ID)).thenReturn(new BigDecimal("600000"));
        when(transactionRepository.sumReturnsByShiftId(1L, TENANT_ID)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumDiscountsByShiftId(1L, TENANT_ID)).thenReturn(new BigDecimal("10000"));
        when(transactionRepository.sumTaxesByShiftId(1L, TENANT_ID)).thenReturn(new BigDecimal("72000"));
        when(transactionRepository.countCompletedByShiftId(1L, TENANT_ID)).thenReturn(3L);
        when(transactionRepository.countVoidedByShiftId(1L, TENANT_ID)).thenReturn(1L);
        when(transactionRepository.countReturnsByShiftId(1L, TENANT_ID)).thenReturn(0L);
        when(paymentRepository.sumSalesByShiftIdAndPaymentType(1L, POSPaymentType.CASH)).thenReturn(new BigDecimal("400000"));
        when(paymentRepository.sumSalesByShiftIdAndPaymentType(1L, POSPaymentType.CARD)).thenReturn(new BigDecimal("200000"));
        when(paymentRepository.sumAllSalesByShiftId(1L)).thenReturn(new BigDecimal("600000"));
        when(paymentRepository.sumRefundsByShiftIdAndPaymentType(1L, POSPaymentType.CASH)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.sumRefundsByShiftIdAndPaymentType(1L, POSPaymentType.CARD)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.sumAllRefundsByShiftId(1L)).thenReturn(BigDecimal.ZERO);
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        shiftService.recalculateShiftTotals(1L);

        // Then
        assertEquals(new BigDecimal("600000"), shift.getTotalSales());
        assertEquals(new BigDecimal("400000"), shift.getCashPayments());
        assertEquals(3, shift.getTransactionCount());
        assertEquals(1, shift.getVoidedCount());
        verify(shiftRepository).save(shift);
    }

    // ==================== reconcileShift ====================

    @Test
    void reconcileShift_closedShift_statusReconciled() {
        // Given
        shift.setStatus(ShiftStatus.CLOSED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(shift));
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shiftMapper.toDto(any())).thenReturn(
                ShiftDto.builder().id(1L).status(ShiftStatus.RECONCILED).build());

        // When
        ShiftDto result = shiftService.reconcileShift(1L);

        // Then
        assertNotNull(result);
        assertEquals(ShiftStatus.RECONCILED, result.getStatus());
    }

    @Test
    void reconcileShift_notClosed_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(shiftRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(shift));

        // When/Then
        assertThrows(BusinessException.class, () -> shiftService.reconcileShift(1L));
    }
}
