package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.entity.TaxRate;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class POSTransactionService {

    private final POSTransactionRepository transactionRepository;
    private final POSTransactionLineRepository lineRepository;
    private final ShiftRepository shiftRepository;
    private final POSTerminalRepository terminalRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final POSTransactionMapper transactionMapper;
    private final SecurityContextHelper securityContextHelper;
    private final StockService stockService;
    private final TaxCalculationService taxCalculationService;
    private final GLIntegrationService glIntegrationService;
    private final ARInvoiceService arInvoiceService;

    @Transactional(readOnly = true)
    public Page<POSTransactionDto> findAll(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionRepository.findByTenantId(tenantId, pageable)
                .map(transactionMapper::toDtoWithoutDetails);
    }

    @Transactional(readOnly = true)
    public Page<POSTransactionDto> search(String query, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionRepository.searchByTenantId(query, tenantId, pageable)
                .map(transactionMapper::toDtoWithoutDetails);
    }

    @Transactional(readOnly = true)
    public POSTransactionDto findById(Long id) {
        return transactionMapper.toDto(getTransactionById(id));
    }

    @Transactional(readOnly = true)
    public POSTransactionDto findByNumber(String transactionNumber) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionRepository.findByTransactionNumberAndTenantId(transactionNumber, tenantId)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionNumber));
    }

    @Transactional(readOnly = true)
    public List<POSTransactionDto> findByShift(Long shiftId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionMapper.toDtoList(
                transactionRepository.findByShiftIdAndTenantId(shiftId, tenantId));
    }

    @Transactional(readOnly = true)
    public List<POSTransactionDto> findHeldTransactions(Long shiftId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionMapper.toDtoList(
                transactionRepository.findByShiftIdAndStatusAndTenantId(shiftId, TransactionStatus.HELD, tenantId));
    }

    @Transactional
    public POSTransactionDto createTransaction(CreateTransactionRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Get terminal
        POSTerminal terminal = terminalRepository.findByIdAndTenantId(request.getTerminalId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Terminal not found: " + request.getTerminalId()));

        // Get current open shift for terminal
        Shift shift = shiftRepository.findByTerminalIdAndStatusAndTenantId(
                        request.getTerminalId(), ShiftStatus.OPEN, tenantId)
                .orElseThrow(() -> new BusinessException("No open shift on this terminal"));

        // Get customer if provided
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findByIdAndTenantId(request.getCustomerId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + request.getCustomerId()));
        }

        // Generate transaction number
        String transactionNumber = generateTransactionNumber(tenantId);

        POSTransaction transaction = POSTransaction.builder()
                .tenantId(tenantId)
                .transactionNumber(transactionNumber)
                .terminal(terminal)
                .shift(shift)
                .customer(customer)
                .customerName(request.getCustomerName() != null ? request.getCustomerName() :
                        (customer != null ? customer.getName() : null))
                .customerPhone(request.getCustomerPhone() != null ? request.getCustomerPhone() :
                        (customer != null ? customer.getPhone() : null))
                .transactionType(request.getTransactionType())
                .status(TransactionStatus.PENDING)
                .originalTransactionId(request.getOriginalTransactionId())
                .notes(request.getNotes())
                .build();

        // For returns, get original transaction number
        if (request.getOriginalTransactionId() != null) {
            final POSTransaction txn = transaction;
            transactionRepository.findByIdAndTenantId(request.getOriginalTransactionId(), tenantId)
                    .ifPresent(orig -> txn.setOriginalTransactionNumber(orig.getTransactionNumber()));
        }

        transaction = transactionRepository.save(transaction);
        log.info("Created POS transaction {} of type {}", transactionNumber, request.getTransactionType());

        // Add line items if provided
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            int lineNumber = 0;
            for (CreateTransactionRequest.LineItem item : request.getItems()) {
                lineNumber++;
                Product product = productRepository.findByIdAndTenantId(item.getProductId(), tenantId)
                        .orElseThrow(() -> new NotFoundException("Product not found: " + item.getProductId()));

                ProductVariant variant = null;
                if (item.getVariantId() != null) {
                    variant = variantRepository.findById(item.getVariantId())
                            .orElseThrow(() -> new NotFoundException("Variant not found: " + item.getVariantId()));
                }

                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() :
                        (variant != null && variant.getSellingPrice() != null ? variant.getSellingPrice() : product.getSellingPrice());

                // Calculate tax
                BigDecimal taxAmount = BigDecimal.ZERO;
                BigDecimal taxRate = BigDecimal.ZERO;
                String taxCode = null;

                if (product.getTaxCode() != null) {
                    taxCode = product.getTaxCode();
                    TaxRate rate = taxCalculationService.getApplicableRate(taxCode, LocalDate.now());
                    if (rate != null) {
                        taxRate = rate.getRate();
                        BigDecimal grossAmount = unitPrice.multiply(item.getQuantity());
                        taxAmount = grossAmount.multiply(taxRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    }
                }

                POSTransactionLine line = POSTransactionLine.builder()
                        .transaction(transaction)
                        .lineNumber(lineNumber)
                        .product(product)
                        .variant(variant)
                        .productCode(product.getSku())
                        .productName(product.getName())
                        .variantName(variant != null ? variant.getName() : null)
                        .barcode(product.getBarcode())
                        .quantity(item.getQuantity())
                        .unitPrice(unitPrice)
                        .originalPrice(product.getSellingPrice())
                        .costPrice(product.getCostPrice())
                        .discountAmount(item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO)
                        .discountReason(item.getDiscountReason())
                        .taxCode(taxCode)
                        .taxRate(taxRate)
                        .taxAmount(taxAmount)
                        .isReturn(transaction.getTransactionType() == TransactionType.RETURN)
                        .build();

                line.calculateLineTotal();
                transaction.addLine(line);
            }
            transaction = transactionRepository.save(transaction);
        }

        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public POSTransactionDto addLine(Long transactionId, AddLineRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        POSTransaction transaction = getTransactionById(transactionId);

        validateTransactionModifiable(transaction);

        // Get product
        Product product = productRepository.findByIdAndTenantId(request.getProductId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + request.getProductId()));

        // Get variant if provided
        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new NotFoundException("Variant not found: " + request.getVariantId()));
        }

        // Determine unit price
        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() :
                (variant != null && variant.getSellingPrice() != null ? variant.getSellingPrice() : product.getSellingPrice());

        // Get next line number
        Integer maxLineNumber = lineRepository.findMaxLineNumberByTransactionId(transactionId);
        int lineNumber = (maxLineNumber != null ? maxLineNumber : 0) + 1;

        // Calculate tax
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal taxRate = BigDecimal.ZERO;
        String taxCode = null;

        if (product.getTaxCode() != null) {
            taxCode = product.getTaxCode();
            TaxRate rate = taxCalculationService.getApplicableRate(taxCode, LocalDate.now());
            if (rate != null) {
                taxRate = rate.getRate();
                BigDecimal grossAmount = unitPrice.multiply(request.getQuantity());
                taxAmount = grossAmount.multiply(taxRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            }
        }

        POSTransactionLine line = POSTransactionLine.builder()
                .transaction(transaction)
                .lineNumber(lineNumber)
                .product(product)
                .variant(variant)
                .productCode(product.getSku())
                .productName(product.getName())
                .variantName(variant != null ? variant.getName() : null)
                .barcode(product.getBarcode())
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .originalPrice(product.getSellingPrice())
                .costPrice(product.getCostPrice())
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .discountPercent(request.getDiscountPercent())
                .discountReason(request.getDiscountReason())
                .taxCode(taxCode)
                .taxRate(taxRate)
                .taxAmount(taxAmount)
                .isReturn(transaction.getTransactionType() == TransactionType.RETURN)
                .serialNumber(request.getSerialNumber())
                .batchNumber(request.getBatchNumber())
                .locationId(request.getLocationId())
                .notes(request.getNotes())
                .build();

        line.calculateLineTotal();
        transaction.addLine(line);
        transaction = transactionRepository.save(transaction);

        log.info("Added line to transaction {}: {} x {} @ {}",
                transaction.getTransactionNumber(), request.getQuantity(), product.getName(), unitPrice);

        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public POSTransactionDto updateLine(Long transactionId, Long lineId, UpdateLineRequest request) {
        POSTransaction transaction = getTransactionById(transactionId);
        validateTransactionModifiable(transaction);

        POSTransactionLine line = lineRepository.findByIdAndTransactionId(lineId, transactionId)
                .orElseThrow(() -> new NotFoundException("Line not found: " + lineId));

        if (request.getQuantity() != null) {
            line.setQuantity(request.getQuantity());
        }
        if (request.getUnitPrice() != null) {
            line.setUnitPrice(request.getUnitPrice());
        }
        if (request.getDiscountAmount() != null) {
            line.setDiscountAmount(request.getDiscountAmount());
        }
        if (request.getDiscountPercent() != null) {
            line.applyPercentDiscount(request.getDiscountPercent(), request.getDiscountReason());
        }
        if (request.getNotes() != null) {
            line.setNotes(request.getNotes());
        }

        // Recalculate tax
        if (line.getTaxCode() != null && line.getTaxRate() != null) {
            BigDecimal grossAmount = line.getUnitPrice().multiply(line.getQuantity());
            BigDecimal discountedAmount = grossAmount.subtract(line.getDiscountAmount() != null ? line.getDiscountAmount() : BigDecimal.ZERO);
            line.setTaxAmount(discountedAmount.multiply(line.getTaxRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        }

        line.calculateLineTotal();
        transaction.recalculateTotals();
        transaction = transactionRepository.save(transaction);

        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public POSTransactionDto removeLine(Long transactionId, Long lineId) {
        POSTransaction transaction = getTransactionById(transactionId);
        validateTransactionModifiable(transaction);

        POSTransactionLine line = lineRepository.findByIdAndTransactionId(lineId, transactionId)
                .orElseThrow(() -> new NotFoundException("Line not found: " + lineId));

        transaction.removeLine(line);
        lineRepository.delete(line);
        transaction = transactionRepository.save(transaction);

        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public POSTransactionDto applyDiscount(Long transactionId, ApplyDiscountRequest request) {
        POSTransaction transaction = getTransactionById(transactionId);
        validateTransactionModifiable(transaction);

        if (request.getPercent() != null) {
            transaction.applyPercentDiscount(request.getPercent(), request.getReason());
        } else if (request.getAmount() != null) {
            transaction.applyFixedDiscount(request.getAmount(), request.getReason());
        }

        transaction = transactionRepository.save(transaction);
        log.info("Applied discount to transaction {}: {} / {}%",
                transaction.getTransactionNumber(), request.getAmount(), request.getPercent());

        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public POSTransactionDto holdTransaction(Long transactionId, HoldTransactionRequest request) {
        Long userId = securityContextHelper.getCurrentUserId();
        POSTransaction transaction = getTransactionById(transactionId);

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Only pending transactions can be held");
        }

        transaction.setStatus(TransactionStatus.HELD);
        transaction.setHeldAt(Instant.now());
        transaction.setHeldBy(userId);
        transaction.setHeldReason(request.getReason());

        transaction = transactionRepository.save(transaction);
        log.info("Held transaction {}", transaction.getTransactionNumber());

        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public POSTransactionDto recallTransaction(Long transactionId) {
        POSTransaction transaction = getTransactionById(transactionId);

        if (transaction.getStatus() != TransactionStatus.HELD) {
            throw new BusinessException("Only held transactions can be recalled");
        }

        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setHeldAt(null);
        transaction.setHeldBy(null);
        transaction.setHeldReason(null);

        transaction = transactionRepository.save(transaction);
        log.info("Recalled transaction {}", transaction.getTransactionNumber());

        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public POSTransactionDto voidTransaction(Long transactionId, VoidTransactionRequest request) {
        Long userId = securityContextHelper.getCurrentUserId();
        POSTransaction transaction = getTransactionById(transactionId);

        if (transaction.getStatus() == TransactionStatus.VOIDED) {
            throw new BusinessException("Transaction is already voided");
        }

        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            // If already completed, we need to reverse the effects
            if (transaction.isStockDeducted()) {
                // Restore stock
                restoreStock(transaction);
            }
            if (transaction.isGlPosted()) {
                // Reverse GL entry
                glIntegrationService.reverseSalesTransaction(transaction.getGlJournalEntryId());
            }
        }

        transaction.setStatus(TransactionStatus.VOIDED);
        transaction.setVoidedAt(Instant.now());
        transaction.setVoidedBy(userId);
        transaction.setVoidReason(request.getReason());

        // Update shift voided count
        Shift shift = transaction.getShift();
        shift.setVoidedCount(shift.getVoidedCount() + 1);
        shiftRepository.save(shift);

        transaction = transactionRepository.save(transaction);
        log.info("Voided transaction {}: {}", transaction.getTransactionNumber(), request.getReason());

        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public POSTransactionDto completeTransaction(Long transactionId) {
        Long userId = securityContextHelper.getCurrentUserId();
        POSTransaction transaction = getTransactionById(transactionId);

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Only pending transactions can be completed");
        }

        if (transaction.getLines().isEmpty()) {
            throw new BusinessException("Transaction has no items");
        }

        if (!transaction.isFullyPaid()) {
            throw new BusinessException("Transaction is not fully paid. Balance due: " + transaction.getBalanceDue());
        }

        // Deduct stock
        if (!transaction.isStockDeducted()) {
            deductStock(transaction);
            transaction.setStockDeducted(true);
        }

        // Post to GL
        if (!transaction.isGlPosted()) {
            try {
                Long journalEntryId = glIntegrationService.postPOSTransaction(transaction);
                transaction.setGlJournalEntryId(journalEntryId);
                transaction.setGlPosted(true);
            } catch (Exception e) {
                log.warn("Failed to post transaction to GL: {}", e.getMessage());
                // Continue without failing the transaction
            }
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(Instant.now());
        transaction.setCompletedBy(userId);

        // Update shift totals
        Shift shift = transaction.getShift();
        shift.addTransaction(transaction);
        shiftRepository.save(shift);

        transaction = transactionRepository.save(transaction);

        // Auto-create AR Invoice if transaction has credit payment
        if (hasCreditPayment(transaction) && transaction.getArInvoiceId() == null) {
            try {
                ARInvoiceDto arInvoice = arInvoiceService.createFromPOSTransaction(transaction);
                transaction.setArInvoiceId(arInvoice.getId());
                transaction = transactionRepository.save(transaction);
                log.info("Auto-created AR Invoice {} for credit sale transaction {}",
                        arInvoice.getInvoiceNumber(), transaction.getTransactionNumber());
            } catch (Exception e) {
                log.warn("Failed to auto-create AR Invoice for credit sale transaction {}: {}",
                        transaction.getTransactionNumber(), e.getMessage());
                // Don't fail the transaction - AR Invoice can be created manually
            }
        }

        log.info("Completed transaction {}", transaction.getTransactionNumber());

        return transactionMapper.toDto(transaction);
    }

    private boolean hasCreditPayment(POSTransaction transaction) {
        return transaction.getPayments().stream()
                .anyMatch(p -> p.getPaymentType() == POSPaymentType.CREDIT &&
                               p.getStatus() == POSPaymentStatus.APPROVED &&
                               p.getAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    private void deductStock(POSTransaction transaction) {
        for (POSTransactionLine line : transaction.getLines()) {
            if (!line.isReturn()) {
                Long locationId = line.getLocationId() != null ? line.getLocationId() :
                        transaction.getTerminal().getLocation().getId();
                try {
                    stockService.deductStock(
                            line.getProduct().getId(),
                            locationId,
                            line.getQuantity(),
                            "POS_SALE",
                            transaction.getId(),
                            "POS Transaction: " + transaction.getTransactionNumber()
                    );
                } catch (Exception e) {
                    log.warn("Failed to deduct stock for product {}: {}",
                            line.getProduct().getSku(), e.getMessage());
                }
            }
        }
    }

    private void restoreStock(POSTransaction transaction) {
        for (POSTransactionLine line : transaction.getLines()) {
            if (!line.isReturn()) {
                Long locationId = line.getLocationId() != null ? line.getLocationId() :
                        transaction.getTerminal().getLocation().getId();
                try {
                    stockService.addStock(
                            line.getProduct().getId(),
                            locationId,
                            line.getQuantity(),
                            "POS_VOID",
                            transaction.getId(),
                            "Voided POS Transaction: " + transaction.getTransactionNumber()
                    );
                } catch (Exception e) {
                    log.warn("Failed to restore stock for product {}: {}",
                            line.getProduct().getSku(), e.getMessage());
                }
            }
        }
    }

    private POSTransaction getTransactionById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
    }

    private void validateTransactionModifiable(POSTransaction transaction) {
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Transaction cannot be modified in status: " + transaction.getStatus());
        }
    }

    private String generateTransactionNumber(Long tenantId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<POSTransaction> todayTransactions = transactionRepository.findByDateRangeAndTenantId(
                startOfDay, endOfDay, tenantId);

        int count = todayTransactions.size() + 1;
        String number;
        do {
            number = String.format("TX%s-%05d", datePart, count++);
        } while (transactionRepository.existsByTransactionNumberAndTenantId(number, tenantId));

        return number;
    }

    // ==================== Return Operations ====================

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

        // Add return lines
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        for (CreateReturnRequest.ReturnLineItem item : request.getItems()) {
            Product product = productRepository.findByIdAndTenantId(item.getProductId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Product", item.getProductId()));

            ProductVariant variant = null;
            if (item.getVariantId() != null) {
                variant = variantRepository.findById(item.getVariantId())
                        .orElseThrow(() -> new NotFoundException("Variant", item.getVariantId()));
            }

            // Determine unit price
            BigDecimal unitPrice = item.getUnitPrice();
            if (unitPrice == null) {
                // Use original transaction price if available, otherwise product price
                unitPrice = variant != null ? variant.getEffectiveSellingPrice() : product.getSellingPrice();
            }

            BigDecimal lineTotal = unitPrice.multiply(item.getQuantity());

            // Calculate tax for return line
            TaxRate taxRate = taxCalculationService.getDefaultTaxRate(tenantId);
            BigDecimal lineTax = BigDecimal.ZERO;
            if (taxRate != null && taxRate.getRate() != null) {
                lineTax = lineTotal.multiply(taxRate.getRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            }

            POSTransactionLine line = POSTransactionLine.builder()
                    .transaction(returnTransaction)
                    .product(product)
                    .variant(variant)
                    .productName(product.getName())
                    .variantName(variant != null ? variant.getName() : null)
                    .productCode(variant != null ? variant.getSku() : product.getSku())
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .costPrice(variant != null ? variant.getEffectiveCostPrice() : product.getCostPrice())
                    .lineTotal(lineTotal.negate()) // Negative for returns
                    .taxAmount(lineTax.negate())
                    .discountAmount(BigDecimal.ZERO)
                    .discountPercent(BigDecimal.ZERO)
                    .isReturn(true)
                    .returnReason(item.getReason())
                    .build();

            lineRepository.save(line);

            subtotal = subtotal.add(lineTotal);
            taxAmount = taxAmount.add(lineTax);
        }

        // Update return transaction totals (negative values for returns)
        returnTransaction.setSubtotal(subtotal.negate());
        returnTransaction.setTaxAmount(taxAmount.negate());
        returnTransaction.setTotalAmount(subtotal.add(taxAmount).negate());
        returnTransaction = transactionRepository.save(returnTransaction);

        // Restore stock for returned items
        restoreStockForReturn(returnTransaction);

        // Complete the return immediately
        returnTransaction.setStatus(TransactionStatus.COMPLETED);
        returnTransaction.setCompletedAt(Instant.now());
        returnTransaction = transactionRepository.save(returnTransaction);

        // Post to GL
        try {
            glIntegrationService.postPOSTransaction(returnTransaction);
            returnTransaction.setGlPosted(true);
            transactionRepository.save(returnTransaction);
        } catch (Exception e) {
            log.warn("Failed to post return transaction to GL: {}", e.getMessage());
        }

        log.info("Created return transaction: {}", returnTransaction.getTransactionNumber());
        return transactionMapper.toDto(returnTransaction);
    }

    private void restoreStockForReturn(POSTransaction transaction) {
        for (POSTransactionLine line : transaction.getLines()) {
            Long locationId = line.getLocationId() != null ? line.getLocationId() :
                    transaction.getTerminal().getLocation().getId();
            try {
                stockService.addStock(
                        line.getProduct().getId(),
                        locationId,
                        line.getQuantity(),
                        "POS_RETURN",
                        transaction.getId(),
                        "POS Return: " + transaction.getTransactionNumber()
                );
            } catch (Exception e) {
                log.warn("Failed to restore stock for returned product {}: {}",
                        line.getProduct().getSku(), e.getMessage());
            }
        }
    }

    private String generateReturnNumber(Long tenantId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int count = 1;
        String number;
        do {
            number = String.format("RET%s-%05d", datePart, count++);
        } while (transactionRepository.existsByTransactionNumberAndTenantId(number, tenantId));
        return number;
    }
}
