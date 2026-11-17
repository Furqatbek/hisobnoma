package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.pos.dto.AddPaymentRequest;
import com.hisobnoma.platform.pos.dto.ApplyDiscountRequest;
import com.hisobnoma.platform.pos.dto.CreateTransactionRequest;
import com.hisobnoma.platform.pos.dto.POSTransactionDto;
import com.hisobnoma.platform.pos.dto.PosCheckoutRequest;
import com.hisobnoma.platform.pos.dto.PosQuoteDto;
import com.hisobnoma.platform.pos.entity.TransactionType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Savepoint;

/**
 * Sells a whole cart in ONE database transaction: create the sale, apply the
 * sale-level discount, take the payments, complete it and record the coupon
 * redemption. Any failure rolls everything back, so there is never a
 * half-finished sale left behind to void by hand.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class POSCheckoutService {

    private static final BigDecimal TOTAL_TOLERANCE = new BigDecimal("0.01");

    private final POSTransactionService transactionService;
    private final POSPaymentService paymentService;
    private final PromotionService promotionService;
    private final SecurityContextHelper securityContextHelper;
    private final EntityManager entityManager;

    /**
     * The totals this cart would be charged, computed by the same code path as a
     * real sale and then undone — nothing is saved and no stock stays reserved.
     *
     * <p>The sale is built after a database savepoint and the savepoint is rolled
     * back afterwards, so the quote can never leave a pending sale behind.
     */
    @Transactional
    public PosQuoteDto quote(PosCheckoutRequest request) {
        Session session = entityManager.unwrap(Session.class);
        entityManager.flush();
        Savepoint[] savepoint = new Savepoint[1];
        session.doWork(connection -> savepoint[0] = connection.setSavepoint());
        try {
            POSTransactionDto pending = createPendingSale(request);
            entityManager.flush();
            return PosQuoteDto.builder()
                    .subtotal(pending.getSubtotal())
                    .discountAmount(pending.getDiscountAmount())
                    .taxAmount(pending.getTaxAmount())
                    .totalAmount(pending.getTotalAmount())
                    .build();
        } finally {
            // Drop the in-memory copies, then undo every row the quote wrote.
            entityManager.clear();
            session.doWork(connection -> connection.rollback(savepoint[0]));
        }
    }

    @Transactional
    public POSTransactionDto checkout(PosCheckoutRequest request) {
        if (request.getPayments() == null || request.getPayments().isEmpty()) {
            throw new BusinessException("At least one payment is required", "PAYMENT_REQUIRED");
        }

        POSTransactionDto pending = createPendingSale(request);
        BigDecimal total = pending.getTotalAmount();

        if (request.getExpectedTotal() != null
                && request.getExpectedTotal().subtract(total).abs().compareTo(TOTAL_TOLERANCE) > 0) {
            throw new BusinessException(
                    "The sale total is " + total + ", not " + request.getExpectedTotal()
                            + ". Review the total and take payment again.",
                    "TOTAL_CHANGED", HttpStatus.CONFLICT);
        }

        for (PosCheckoutRequest.Payment payment : request.getPayments()) {
            paymentService.addPayment(pending.getId(), AddPaymentRequest.builder()
                    .paymentType(payment.getPaymentType())
                    .amount(payment.getAmount())
                    .tenderedAmount(payment.getTenderedAmount())
                    .build());
        }

        POSTransactionDto completed = transactionService.completeTransaction(pending.getId());

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            BigDecimal discount = pending.getDiscountAmount() != null
                    ? pending.getDiscountAmount() : BigDecimal.ZERO;
            // Inside the same transaction: a coupon that is no longer valid rolls the sale back.
            promotionService.recordCouponRedemption(request.getCouponCode().trim(),
                    request.getCustomerId(), null, pending.getId(), total, discount);
        }

        log.info("Checkout completed: {} total {}", completed.getTransactionNumber(), total);
        return completed;
    }

    private POSTransactionDto createPendingSale(PosCheckoutRequest request) {
        POSTransactionDto sale = transactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(request.getTerminalId())
                .customerId(request.getCustomerId())
                .transactionType(TransactionType.SALE)
                .deliveryRegionId(request.getDeliveryRegionId())
                .deliveryVillageId(request.getDeliveryVillageId())
                .notes(request.getNotes())
                .items(request.getItems())
                .build());

        boolean percent = request.getDiscountPercent() != null && request.getDiscountPercent().signum() > 0;
        boolean amount = request.getDiscountAmount() != null && request.getDiscountAmount().signum() > 0;
        if (percent || amount) {
            if (!securityContextHelper.hasPermission("POS_DISCOUNT_APPLY")) {
                throw new BusinessException("You don't have permission to apply discounts",
                        "FORBIDDEN", HttpStatus.FORBIDDEN);
            }
            sale = transactionService.applyDiscount(sale.getId(), ApplyDiscountRequest.builder()
                    .percent(percent ? request.getDiscountPercent() : null)
                    .amount(percent ? null : request.getDiscountAmount())
                    .reason(request.getDiscountReason())
                    .build());
        }
        return sale;
    }
}
