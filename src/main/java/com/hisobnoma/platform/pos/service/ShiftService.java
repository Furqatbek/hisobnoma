package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.entity.POSTerminal;
import com.hisobnoma.platform.pos.entity.Shift;
import com.hisobnoma.platform.pos.entity.ShiftStatus;
import com.hisobnoma.platform.pos.mapper.ShiftMapper;
import com.hisobnoma.platform.pos.repository.POSPaymentRepository;
import com.hisobnoma.platform.pos.repository.POSTerminalRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import com.hisobnoma.platform.pos.repository.ShiftRepository;
import com.hisobnoma.platform.pos.entity.POSPaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final POSTerminalRepository terminalRepository;
    private final POSTransactionRepository transactionRepository;
    private final POSPaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ShiftMapper shiftMapper;
    private final SecurityContextHelper securityContextHelper;
    private final POSTerminalService terminalService;

    @Transactional(readOnly = true)
    public Page<ShiftDto> findAll(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return shiftRepository.findByTenantId(tenantId, pageable)
                .map(shiftMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ShiftDto findById(Long id) {
        return shiftMapper.toDto(getShiftById(id));
    }

    @Transactional(readOnly = true)
    public Page<ShiftDto> findByTerminal(Long terminalId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return shiftRepository.findByTerminalIdAndTenantId(terminalId, tenantId, pageable)
                .map(shiftMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ShiftDto> findByCashier(Long cashierId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return shiftRepository.findByCashierIdAndTenantId(cashierId, tenantId, pageable)
                .map(shiftMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ShiftDto getCurrentShift(Long terminalId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return shiftRepository.findByTerminalIdAndStatusAndTenantId(terminalId, ShiftStatus.OPEN, tenantId)
                .map(shiftMapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public ShiftDto getCurrentShiftForUser() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();
        return shiftRepository.findByCashierIdAndStatusAndTenantId(userId, ShiftStatus.OPEN, tenantId)
                .map(shiftMapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ShiftDto> findOpenShifts() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return shiftMapper.toDtoList(shiftRepository.findOpenShiftsByTenantId(tenantId));
    }

    @Transactional
    public ShiftDto openShift(OpenShiftRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        // Check if terminal exists and is active
        POSTerminal terminal = terminalRepository.findByIdAndTenantId(request.getTerminalId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Terminal not found: " + request.getTerminalId()));

        if (!terminal.isActive()) {
            throw new BusinessException("Terminal is not active");
        }

        // Check if terminal already has an open shift
        if (shiftRepository.findByTerminalIdAndStatusAndTenantId(request.getTerminalId(), ShiftStatus.OPEN, tenantId).isPresent()) {
            throw new BusinessException("Terminal already has an open shift");
        }

        // Check if user already has an open shift
        if (shiftRepository.findByCashierIdAndStatusAndTenantId(userId, ShiftStatus.OPEN, tenantId).isPresent()) {
            throw new BusinessException("You already have an open shift on another terminal");
        }

        // Get user info
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Generate shift number
        String shiftNumber = generateShiftNumber(tenantId);

        Shift shift = Shift.builder()
                .tenantId(tenantId)
                .shiftNumber(shiftNumber)
                .terminal(terminal)
                .cashierId(userId)
                .cashierName(user.getFirstName() + " " + user.getLastName())
                .status(ShiftStatus.OPEN)
                .openedAt(Instant.now())
                .openingCash(request.getOpeningCash())
                .notes(request.getNotes())
                .build();

        shift = shiftRepository.save(shift);

        // Update terminal's current shift
        terminalService.updateCurrentShift(terminal.getId(), shift.getId());

        log.info("Opened shift {} on terminal {} by user {}", shiftNumber, terminal.getTerminalCode(), user.getUsername());

        return shiftMapper.toDto(shift);
    }

    @Transactional
    public ShiftDto closeShift(Long shiftId, CloseShiftRequest request) {
        Shift shift = getShiftById(shiftId);

        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new BusinessException("Shift is not open");
        }

        // Block close if there are unresolved (PENDING/HELD) transactions
        Long unresolvedCount = transactionRepository.countUnresolvedByShiftId(shiftId, shift.getTenantId());
        if (unresolvedCount != null && unresolvedCount > 0) {
            throw new BusinessException(
                    "Cannot close shift with " + unresolvedCount + " unresolved transaction(s). " +
                    "Complete, void, or cancel all pending/held transactions first.");
        }

        recalculateShiftTotals(shift);

        // Close the shift
        shift.setStatus(ShiftStatus.CLOSED);
        shift.setClosedAt(Instant.now());
        shift.setClosingCash(request.getClosingCash());
        shift.setClosingNotes(request.getClosingNotes());
        shift.calculateExpectedCash();
        shift.calculateCashDifference();

        shift = shiftRepository.save(shift);

        // Clear terminal's current shift
        terminalService.updateCurrentShift(shift.getTerminal().getId(), null);

        log.info("Closed shift {} on terminal {} with difference {}",
                shift.getShiftNumber(), shift.getTerminal().getTerminalCode(), shift.getCashDifference());

        return shiftMapper.toDto(shift);
    }

    /**
     * Recalculates all shift totals from transaction and payment queries.
     * Single source of truth — called after every transaction change and at shift close.
     */
    @Transactional
    public void recalculateShiftTotals(Long shiftId) {
        Shift shift = getShiftById(shiftId);
        recalculateShiftTotals(shift);
        shiftRepository.save(shift);
    }

    private void recalculateShiftTotals(Shift shift) {
        Long shiftId = shift.getId();
        Long tenantId = shift.getTenantId();

        // Transaction totals
        BigDecimal totalSales = transactionRepository.sumSalesByShiftId(shiftId, tenantId);
        BigDecimal totalReturns = transactionRepository.sumReturnsByShiftId(shiftId, tenantId);
        BigDecimal totalDiscounts = transactionRepository.sumDiscountsByShiftId(shiftId, tenantId);
        BigDecimal totalTaxes = transactionRepository.sumTaxesByShiftId(shiftId, tenantId);
        Long completedCount = transactionRepository.countCompletedByShiftId(shiftId, tenantId);
        Long voidedCount = transactionRepository.countVoidedByShiftId(shiftId, tenantId);
        Long returnCount = transactionRepository.countReturnsByShiftId(shiftId, tenantId);

        // Sales payments (from SALE transactions only)
        BigDecimal cashPayments = paymentRepository.sumSalesByShiftIdAndPaymentType(shiftId, POSPaymentType.CASH);
        BigDecimal cardPayments = paymentRepository.sumSalesByShiftIdAndPaymentType(shiftId, POSPaymentType.CARD);
        BigDecimal allSalePayments = paymentRepository.sumAllSalesByShiftId(shiftId);
        BigDecimal otherPayments = allSalePayments.subtract(cashPayments).subtract(cardPayments);

        // Refund payments (from RETURN transactions only, stored as positive for display)
        BigDecimal cashRefunds = paymentRepository.sumRefundsByShiftIdAndPaymentType(shiftId, POSPaymentType.CASH).abs();
        BigDecimal cardRefunds = paymentRepository.sumRefundsByShiftIdAndPaymentType(shiftId, POSPaymentType.CARD).abs();
        BigDecimal allRefundPayments = paymentRepository.sumAllRefundsByShiftId(shiftId).abs();
        BigDecimal otherRefunds = allRefundPayments.subtract(cashRefunds).subtract(cardRefunds);

        shift.setTotalSales(totalSales != null ? totalSales : BigDecimal.ZERO);
        shift.setTotalReturns(totalReturns != null ? totalReturns : BigDecimal.ZERO);
        shift.setTotalDiscounts(totalDiscounts != null ? totalDiscounts : BigDecimal.ZERO);
        shift.setTotalTaxes(totalTaxes != null ? totalTaxes : BigDecimal.ZERO);
        shift.setCashPayments(cashPayments);
        shift.setCardPayments(cardPayments);
        shift.setOtherPayments(otherPayments);
        shift.setCashRefunds(cashRefunds);
        shift.setCardRefunds(cardRefunds);
        shift.setOtherRefunds(otherRefunds);
        shift.setTransactionCount(completedCount != null ? completedCount.intValue() : 0);
        shift.setVoidedCount(voidedCount != null ? voidedCount.intValue() : 0);
        shift.setReturnCount(returnCount != null ? returnCount.intValue() : 0);
    }

    @Transactional
    public ShiftDto cashOperation(Long shiftId, CashOperationRequest request) {
        Shift shift = getShiftById(shiftId);

        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new BusinessException("Shift is not open");
        }

        if (request.getOperationType() == CashOperationRequest.OperationType.CASH_IN) {
            shift.setCashIn(shift.getCashIn().add(request.getAmount()));
            log.info("Cash in {} on shift {}", request.getAmount(), shift.getShiftNumber());
        } else {
            shift.setCashOut(shift.getCashOut().add(request.getAmount()));
            log.info("Cash out {} on shift {}", request.getAmount(), shift.getShiftNumber());
        }

        shift = shiftRepository.save(shift);
        return shiftMapper.toDto(shift);
    }

    @Transactional
    public ShiftDto reconcileShift(Long shiftId) {
        Shift shift = getShiftById(shiftId);

        if (shift.getStatus() != ShiftStatus.CLOSED) {
            throw new BusinessException("Shift must be closed before reconciliation");
        }

        shift.setStatus(ShiftStatus.RECONCILED);
        shift = shiftRepository.save(shift);

        log.info("Reconciled shift {}", shift.getShiftNumber());
        return shiftMapper.toDto(shift);
    }

    private Shift getShiftById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return shiftRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Shift not found: " + id));
    }

    private String generateShiftNumber(Long tenantId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Long count = shiftRepository.countByDateRangeAndTenantId(startOfDay, endOfDay, tenantId);
        return String.format("SH%s-%04d", datePart, count + 1);
    }
}
