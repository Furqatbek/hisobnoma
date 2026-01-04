package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.pos.dto.AddPaymentRequest;
import com.hisobnoma.platform.pos.dto.POSPaymentDto;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.mapper.POSPaymentMapper;
import com.hisobnoma.platform.pos.repository.POSPaymentRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class POSPaymentService {

    private final POSPaymentRepository paymentRepository;
    private final POSTransactionRepository transactionRepository;
    private final POSPaymentMapper paymentMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<POSPaymentDto> findByTransaction(Long transactionId) {
        return paymentMapper.toDtoList(paymentRepository.findByTransactionId(transactionId));
    }

    @Transactional(readOnly = true)
    public POSPaymentDto findById(Long transactionId, Long paymentId) {
        return paymentRepository.findByIdAndTransactionId(paymentId, transactionId)
                .map(paymentMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
    }

    @Transactional
    public POSPaymentDto addPayment(Long transactionId, AddPaymentRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        POSTransaction transaction = transactionRepository.findByIdAndTenantId(transactionId, tenantId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Cannot add payment to transaction in status: " + transaction.getStatus());
        }

        // Get next payment number
        Integer maxPaymentNumber = paymentRepository.findMaxPaymentNumberByTransactionId(transactionId);
        int paymentNumber = (maxPaymentNumber != null ? maxPaymentNumber : 0) + 1;

        POSPayment payment = POSPayment.builder()
                .transaction(transaction)
                .paymentNumber(paymentNumber)
                .paymentType(request.getPaymentType())
                .status(POSPaymentStatus.PENDING)
                .amount(request.getAmount())
                .tenderedAmount(request.getTenderedAmount())
                .cardType(request.getCardType())
                .cardLastFour(request.getCardLastFour())
                .authCode(request.getAuthCode())
                .gatewayReference(request.getGatewayReference())
                .giftCardNumber(request.getGiftCardNumber())
                .checkNumber(request.getCheckNumber())
                .mobileReference(request.getMobileReference())
                .notes(request.getNotes())
                .build();

        // Calculate change for cash payments
        if (request.getPaymentType() == POSPaymentType.CASH) {
            payment.calculateChange();
        }

        // Auto-approve the payment (in a real system, card payments would go through a gateway)
        payment.approve();
        payment.setProcessedBy(userId);

        transaction.addPayment(payment);
        transactionRepository.save(transaction);

        log.info("Added {} payment of {} to transaction {}",
                request.getPaymentType(), request.getAmount(), transaction.getTransactionNumber());

        return paymentMapper.toDto(payment);
    }

    @Transactional
    public POSPaymentDto voidPayment(Long transactionId, Long paymentId, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        POSTransaction transaction = transactionRepository.findByIdAndTenantId(transactionId, tenantId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Cannot void payment on transaction in status: " + transaction.getStatus());
        }

        POSPayment payment = paymentRepository.findByIdAndTransactionId(paymentId, transactionId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() == POSPaymentStatus.VOIDED) {
            throw new BusinessException("Payment is already voided");
        }

        payment.voidPayment(userId, reason);
        payment = paymentRepository.save(payment);

        // Recalculate transaction paid amount
        transaction.recalculatePaidAmount();
        transactionRepository.save(transaction);

        log.info("Voided payment {} on transaction {}: {}",
                paymentId, transaction.getTransactionNumber(), reason);

        return paymentMapper.toDto(payment);
    }

    @Transactional
    public POSPaymentDto refundPayment(Long transactionId, Long paymentId, BigDecimal refundAmount, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        POSTransaction transaction = transactionRepository.findByIdAndTenantId(transactionId, tenantId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        POSPayment payment = paymentRepository.findByIdAndTransactionId(paymentId, transactionId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != POSPaymentStatus.APPROVED) {
            throw new BusinessException("Cannot refund payment in status: " + payment.getStatus());
        }

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new BusinessException("Refund amount cannot exceed payment amount");
        }

        payment.refund(userId, refundAmount, reason);
        payment = paymentRepository.save(payment);

        // Recalculate transaction paid amount
        transaction.recalculatePaidAmount();
        transactionRepository.save(transaction);

        log.info("Refunded {} from payment {} on transaction {}: {}",
                refundAmount, paymentId, transaction.getTransactionNumber(), reason);

        return paymentMapper.toDto(payment);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaidAmount(Long transactionId) {
        BigDecimal total = paymentRepository.sumApprovedByTransactionId(transactionId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getPaymentTotalByType(Long shiftId, POSPaymentType paymentType) {
        BigDecimal total = paymentRepository.sumByShiftIdAndPaymentType(shiftId, paymentType);
        return total != null ? total : BigDecimal.ZERO;
    }
}
