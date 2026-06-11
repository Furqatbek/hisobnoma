package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.entity.TaxRate;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.finance.service.GLIntegrationService;
import com.hisobnoma.platform.finance.service.TaxCalculationService;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductVariantRepository;
import com.hisobnoma.platform.inventory.service.StockService;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.mapper.POSTransactionMapper;
import com.hisobnoma.platform.pos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

/**
 * POS return (refund) transactions: validates the original sale, rebuilds
 * the returned lines with their original pricing/discount/tax, restores
 * stock and posts the reversal to the GL.
 *
 * Extracted from {@link POSTransactionService}, which keeps the sale
 * lifecycle and delegates returns here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class POSReturnService {

    private final POSTransactionRepository transactionRepository;
    private final POSTransactionLineRepository lineRepository;
    private final POSPaymentRepository paymentRepository;
    private final ShiftRepository shiftRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final POSTransactionMapper transactionMapper;
    private final SecurityContextHelper securityContextHelper;
    private final StockService stockService;
    private final TaxCalculationService taxCalculationService;
    private final GLIntegrationService glIntegrationService;
    private final ShiftService shiftService;

    private POSTransaction getTransactionById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
    }

    /**
     * Create a return transaction.
     */
    @Transactional
    public POSTransactionDto createReturn(CreateReturnRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        // Get current open shift for cashier or any open shift for tenant
        Shift shift = shiftRepository.findByCashierIdAndStatusAndTenantId(userId, ShiftStatus.OPEN, tenantId)
                .orElseGet(() -> shiftRepository.findOpenShiftsByTenantId(tenantId).stream()
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("No open shift found. Please open a shift first.")));

        // Validate original transaction if provided
        POSTransaction originalTransaction = null;
        if (request.getOriginalTransactionId() != null) {
            originalTransaction = getTransactionById(request.getOriginalTransactionId());
            if (originalTransaction.getStatus() != TransactionStatus.COMPLETED) {
                throw new BusinessException("Can only return completed transactions");
            }
        } else if (request.getOriginalTransactionNumber() != null) {
            originalTransaction = transactionRepository
                    .findByTransactionNumberAndTenantId(request.getOriginalTransactionNumber(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Original transaction not found: " + request.getOriginalTransactionNumber()));
            if (originalTransaction.getStatus() != TransactionStatus.COMPLETED) {
                throw new BusinessException("Can only return completed transactions");
            }
        }

        // Create return transaction
        POSTransaction returnTransaction = POSTransaction.builder()
                .tenantId(tenantId)
                .transactionNumber(generateReturnNumber(tenantId))
                .transactionType(TransactionType.RETURN)
                .status(TransactionStatus.PENDING)
                .shift(shift)
                .terminal(shift.getTerminal())
                .cashierId(userId)
                .cashierName(securityContextHelper.getCurrentUsername())
                .originalTransactionId(originalTransaction != null ? originalTransaction.getId() : null)
                .returnReason(request.getReturnReason())
                .notes(request.getNotes())
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        // Set customer if provided
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findByIdAndTenantId(request.getCustomerId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Customer", request.getCustomerId()));
            returnTransaction.setCustomer(customer);
            returnTransaction.setCustomerName(customer.getName());
            returnTransaction.setCustomerPhone(customer.getPhone());
        } else if (originalTransaction != null && originalTransaction.getCustomer() != null) {
            returnTransaction.setCustomer(originalTransaction.getCustomer());
            returnTransaction.setCustomerName(originalTransaction.getCustomerName());
            returnTransaction.setCustomerPhone(originalTransaction.getCustomerPhone());
        }

        returnTransaction = transactionRepository.save(returnTransaction);

        // Add return lines — calculateLineTotal() handles negation for isReturn lines,
        // so we set taxCode/taxRate and let the entity compute lineTotal and taxAmount.
        BigDecimal lineDiscountTotal = BigDecimal.ZERO;
        int lineNumber = 0;

        for (CreateReturnRequest.ReturnLineItem item : request.getItems()) {
            lineNumber++;
            Product product = productRepository.findByIdAndTenantId(item.getProductId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Product", item.getProductId()));

            ProductVariant variant = null;
            if (item.getVariantId() != null) {
                variant = variantRepository.findById(item.getVariantId())
                        .filter(v -> v.getProduct().getId().equals(product.getId()))
                        .orElseThrow(() -> new NotFoundException("Variant", item.getVariantId()));
            }

            // Look up original line for price and discount info
            BigDecimal unitPrice = item.getUnitPrice();
            BigDecimal lineDiscount = BigDecimal.ZERO;
            BigDecimal lineDiscountPercent = BigDecimal.ZERO;
            String lineTaxCode = product.getTaxCode();
            BigDecimal lineTaxRate = BigDecimal.ZERO;

            if (item.getOriginalLineId() != null && originalTransaction != null) {
                POSTransactionLine originalLine = originalTransaction.getLines().stream()
                        .filter(l -> l.getId().equals(item.getOriginalLineId()))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Original transaction line", item.getOriginalLineId()));

                // Validate return quantity does not exceed remaining returnable quantity
                BigDecimal alreadyReturned = lineRepository.sumReturnedQuantityByOriginalLineId(
                        item.getOriginalLineId());
                BigDecimal remainingReturnable = originalLine.getQuantity().subtract(alreadyReturned);
                if (item.getQuantity().compareTo(remainingReturnable) > 0) {
                    throw new BusinessException(
                            "Cannot return " + item.getQuantity() + " of product " + product.getName() +
                            ". Only " + remainingReturnable + " remaining (original: " +
                            originalLine.getQuantity() + ", already returned: " + alreadyReturned + ")");
                }

                if (unitPrice == null) {
                    unitPrice = originalLine.getUnitPrice();
                }

                // Carry forward tax code/rate from original line
                if (originalLine.getTaxCode() != null) {
                    lineTaxCode = originalLine.getTaxCode();
                }
                if (originalLine.getTaxRate() != null) {
                    lineTaxRate = originalLine.getTaxRate();
                }

                // Proportional discount: (originalDiscount / originalQty) * returnQty
                if (originalLine.getDiscountAmount() != null
                        && originalLine.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    lineDiscount = originalLine.getDiscountAmount()
                            .multiply(item.getQuantity())
                            .divide(originalLine.getQuantity(), 4, RoundingMode.HALF_UP);
                    lineDiscountPercent = originalLine.getDiscountPercent();
                }
            }

            if (unitPrice == null) {
                unitPrice = variant != null ? variant.getEffectiveSellingPrice() : product.getSellingPrice();
            }

            // If no tax rate from original line, look up default
            if (lineTaxRate.compareTo(BigDecimal.ZERO) == 0) {
                TaxRate defaultRate = taxCalculationService.getDefaultTaxRate(tenantId);
                if (defaultRate != null && defaultRate.getRate() != null) {
                    lineTaxRate = defaultRate.getRate();
                }
            }

            POSTransactionLine line = POSTransactionLine.builder()
                    .transaction(returnTransaction)
                    .lineNumber(lineNumber)
                    .product(product)
                    .variant(variant)
                    .productName(product.getName())
                    .variantName(variant != null ? variant.getName() : null)
                    .productCode(variant != null ? variant.getSku() : product.getSku())
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .costPrice(variant != null ? variant.getEffectiveCostPrice() : product.getCostPrice())
                    .taxCode(lineTaxCode)
                    .taxRate(lineTaxRate)
                    .discountAmount(lineDiscount)
                    .discountPercent(lineDiscountPercent != null ? lineDiscountPercent : BigDecimal.ZERO)
                    .isReturn(true)
                    .returnReason(item.getReason())
                    .originalLineId(item.getOriginalLineId())
                    .build();

            // calculateLineTotal() fires via @PrePersist and negates for isReturn
            returnTransaction.addLine(line);
            lineDiscountTotal = lineDiscountTotal.add(lineDiscount);
        }

        // Proportionally allocate transaction-level discount for linked returns.
        // Use absolute subtotal for the ratio since return subtotal is negative.
        BigDecimal absSubtotal = returnTransaction.getSubtotal().abs();
        BigDecimal txLevelDiscount = BigDecimal.ZERO;
        if (originalTransaction != null && originalTransaction.getDiscountAmount() != null
                && originalTransaction.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                && originalTransaction.getSubtotal().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal returnRatio = absSubtotal.divide(
                    originalTransaction.getSubtotal(), 6, RoundingMode.HALF_UP);
            txLevelDiscount = originalTransaction.getDiscountAmount()
                    .multiply(returnRatio).setScale(4, RoundingMode.HALF_UP);
        }

        // Set transaction-level discount and let recalculateTotals() handle the rest
        returnTransaction.setDiscountAmount(lineDiscountTotal.add(txLevelDiscount));
        returnTransaction.recalculateTotals();
        returnTransaction = transactionRepository.save(returnTransaction);

        // Create refund payment record based on the requested refund method
        BigDecimal refundAmount = returnTransaction.getTotalAmount().abs();
        POSPaymentType refundPaymentType = mapRefundMethodToPaymentType(
                request.getRefundMethod(), originalTransaction);
        POSPayment refundPayment = POSPayment.builder()
                .transaction(returnTransaction)
                .paymentNumber(1)
                .paymentType(refundPaymentType)
                .amount(refundAmount)
                .notes("Refund for return: " + returnTransaction.getTransactionNumber())
                .build();
        refundPayment.approve();
        refundPayment.setProcessedBy(userId);
        refundPayment = paymentRepository.save(refundPayment);
        returnTransaction.addPayment(refundPayment);
        returnTransaction = transactionRepository.save(returnTransaction);

        // Restore stock for returned items
        restoreStockForReturn(returnTransaction);

        // Complete the return
        returnTransaction.setStatus(TransactionStatus.COMPLETED);
        returnTransaction.setCompletedAt(Instant.now());
        returnTransaction.setCompletedBy(userId);
        returnTransaction.setStockDeducted(true);
        returnTransaction = transactionRepository.save(returnTransaction);

        // Post to GL
        try {
            Long journalEntryId = glIntegrationService.postPOSTransaction(returnTransaction);
            returnTransaction.setGlJournalEntryId(journalEntryId);
            returnTransaction.setGlPosted(true);
            transactionRepository.save(returnTransaction);
        } catch (Exception e) {
            log.error("Failed to post return transaction {} to GL: {}. Retry via POST /api/v1/pos/transactions/{}/retry-gl",
                    returnTransaction.getTransactionNumber(), e.getMessage(), returnTransaction.getId());
        }

        // Recalculate shift totals
        shiftService.recalculateShiftTotals(returnTransaction.getShift().getId());

        log.info("Created return transaction: {}", returnTransaction.getTransactionNumber());
        return transactionMapper.toDto(returnTransaction);
    }

    private void restoreStockForReturn(POSTransaction transaction) {
        for (POSTransactionLine line : transaction.getLines()) {
            if (line.getProduct().isTrackInventory()) {
                Long locationId = line.getLocationId() != null ? line.getLocationId() :
                        transaction.getTerminal().getLocation().getId();
                stockService.addStock(
                        line.getProduct().getId(),
                        locationId,
                        line.getQuantity(),
                        "POS_RETURN",
                        transaction.getId(),
                        "POS Return: " + transaction.getTransactionNumber()
                );
            }
        }
    }

    /**
     * Map the refund method from the request to a POSPaymentType.
     * ORIGINAL_PAYMENT_METHOD looks at the original transaction's primary payment type.
     */
    private POSPaymentType mapRefundMethodToPaymentType(
            CreateReturnRequest.RefundMethod refundMethod, POSTransaction originalTransaction) {
        if (refundMethod == null) {
            return POSPaymentType.CASH;
        }
        switch (refundMethod) {
            case CASH:
                return POSPaymentType.CASH;
            case CARD:
                return POSPaymentType.CARD;
            case STORE_CREDIT:
                return POSPaymentType.CREDIT;
            case ORIGINAL_PAYMENT_METHOD:
                if (originalTransaction != null && originalTransaction.getPayments() != null) {
                    // Use the primary (largest approved) payment type from original transaction
                    return originalTransaction.getPayments().stream()
                            .filter(p -> p.getStatus() == POSPaymentStatus.APPROVED)
                            .max((a, b) -> a.getAmount().compareTo(b.getAmount()))
                            .map(POSPayment::getPaymentType)
                            .orElse(POSPaymentType.CASH);
                }
                return POSPaymentType.CASH;
            default:
                return POSPaymentType.CASH;
        }
    }

    private String generateReturnNumber(Long tenantId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String maxNumber = transactionRepository.findMaxTransactionNumberByPrefixAndTenantId(
                "RET" + datePart + "-", tenantId);

        int next;
        if (maxNumber != null) {
            String suffix = maxNumber.substring(maxNumber.lastIndexOf('-') + 1);
            next = Integer.parseInt(suffix) + 1;
        } else {
            next = 1;
        }

        return String.format("RET%s-%05d", datePart, next);
    }
}
