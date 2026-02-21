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
import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductVariantRepository;
import com.hisobnoma.platform.inventory.service.StockService;
import com.hisobnoma.platform.delivery.repository.DeliveryRegionRepository;
import com.hisobnoma.platform.delivery.repository.DeliveryVillageRepository;
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
    private final POSPaymentRepository paymentRepository;
    private final ShiftRepository shiftRepository;
    private final POSTerminalRepository terminalRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final DeliveryRegionRepository deliveryRegionRepository;
    private final DeliveryVillageRepository deliveryVillageRepository;
    private final POSTransactionMapper transactionMapper;
    private final SecurityContextHelper securityContextHelper;
    private final StockService stockService;
    private final TaxCalculationService taxCalculationService;
    private final GLIntegrationService glIntegrationService;
    private final ARInvoiceService arInvoiceService;
    private final ShiftService shiftService;

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

        Long userId = securityContextHelper.getCurrentUserId();
        String userName = securityContextHelper.getCurrentUsername();

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
                .cashierId(userId)
                .cashierName(userName)
                .transactionType(request.getTransactionType())
                .status(TransactionStatus.PENDING)
                .originalTransactionId(request.getOriginalTransactionId())
                .deliveryRegionId(request.getDeliveryRegionId())
                .deliveryVillageId(request.getDeliveryVillageId())
                .notes(request.getNotes())
                .build();

        // Resolve delivery address names
        if (request.getDeliveryRegionId() != null) {
            deliveryRegionRepository.findByIdAndTenantId(request.getDeliveryRegionId(), tenantId)
                    .ifPresent(r -> transaction.setDeliveryRegionName(r.getName()));
        }
        if (request.getDeliveryVillageId() != null) {
            deliveryVillageRepository.findByIdAndTenantId(request.getDeliveryVillageId(), tenantId)
                    .ifPresent(v -> transaction.setDeliveryVillageName(v.getName()));
        }

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
                        (variant != null ? variant.getEffectiveSellingPrice() : product.getSellingPrice());

                // Calculate tax
                BigDecimal taxAmount = BigDecimal.ZERO;
                BigDecimal taxRate = BigDecimal.ZERO;
                String taxCode = null;

                BigDecimal lineDiscount = item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO;

                if (product.getTaxCode() != null) {
                    taxCode = product.getTaxCode();
                    TaxRate rate = taxCalculationService.getApplicableRate(taxCode, LocalDate.now());
                    if (rate != null) {
                        taxRate = rate.getRate();
                        BigDecimal grossAmount = unitPrice.multiply(item.getQuantity());
                        BigDecimal discountedAmount = grossAmount.subtract(lineDiscount);
                        taxAmount = discountedAmount.multiply(taxRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
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
                        .costPrice(variant != null ? variant.getEffectiveCostPrice() : product.getCostPrice())
                        .discountAmount(lineDiscount)
                        .discountReason(item.getDiscountReason())
                        .taxCode(taxCode)
                        .taxRate(taxRate)
                        .taxAmount(taxAmount)
                        .isReturn(transaction.getTransactionType() == TransactionType.RETURN)
                        .build();

                line.calculateLineTotal();
                transaction.addLine(line);

                // Reserve stock to prevent overselling between creation and completion
                if (!line.isReturn() && product.isTrackInventory()) {
                    Long locId = transaction.getTerminal().getLocation().getId();
                    reserveStockForLine(product.getId(), locId, item.getQuantity(), transaction);
                }
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
                (variant != null ? variant.getEffectiveSellingPrice() : product.getSellingPrice());

        // Get next line number
        Integer maxLineNumber = lineRepository.findMaxLineNumberByTransactionId(transactionId);
        int lineNumber = (maxLineNumber != null ? maxLineNumber : 0) + 1;

        // Calculate tax on post-discount amount
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal taxRate = BigDecimal.ZERO;
        String taxCode = null;
        BigDecimal lineDiscount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;

        if (product.getTaxCode() != null) {
            taxCode = product.getTaxCode();
            TaxRate rate = taxCalculationService.getApplicableRate(taxCode, LocalDate.now());
            if (rate != null) {
                taxRate = rate.getRate();
                BigDecimal grossAmount = unitPrice.multiply(request.getQuantity());
                BigDecimal discountedAmount = grossAmount.subtract(lineDiscount);
                taxAmount = discountedAmount.multiply(taxRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
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
                .costPrice(variant != null ? variant.getEffectiveCostPrice() : product.getCostPrice())
                .discountAmount(lineDiscount)
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

        // Reserve stock to prevent overselling
        if (!line.isReturn() && product.isTrackInventory()) {
            Long locId = request.getLocationId() != null ? request.getLocationId() :
                    transaction.getTerminal().getLocation().getId();
            reserveStockForLine(product.getId(), locId, request.getQuantity(), transaction);
        }

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

        // Validate quantity is positive when provided
        if (request.getQuantity() != null && request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Quantity must be greater than zero");
        }

        BigDecimal oldQuantity = line.getQuantity();

        if (request.getQuantity() != null) {
            line.setQuantity(request.getQuantity());
        }
        if (request.getUnitPrice() != null) {
            line.setUnitPrice(request.getUnitPrice());
        }
        if (request.getDiscountPercent() != null) {
            line.applyPercentDiscount(request.getDiscountPercent(), request.getDiscountReason());
        } else if (request.getDiscountAmount() != null) {
            line.applyFixedDiscount(request.getDiscountAmount(), request.getDiscountReason());
        }
        if (request.getNotes() != null) {
            line.setNotes(request.getNotes());
        }

        // calculateLineTotal() recalculates tax on the post-discount amount
        line.calculateLineTotal();
        transaction.recalculateTotals();
        transaction = transactionRepository.save(transaction);

        // Update stock reservation if quantity changed on a non-return, tracked product
        if (request.getQuantity() != null && request.getQuantity().compareTo(oldQuantity) != 0
                && !line.isReturn() && line.getProduct().isTrackInventory()) {
            // Release old reservation and create new one with updated quantity
            releaseStockForLine(line, transaction);
            Long locId = line.getLocationId() != null ? line.getLocationId() :
                    transaction.getTerminal().getLocation().getId();
            reserveStockForLine(line.getProduct().getId(), locId, request.getQuantity(), transaction);
        }

        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public POSTransactionDto removeLine(Long transactionId, Long lineId) {
        POSTransaction transaction = getTransactionById(transactionId);
        validateTransactionModifiable(transaction);

        POSTransactionLine line = lineRepository.findByIdAndTransactionId(lineId, transactionId)
                .orElseThrow(() -> new NotFoundException("Line not found: " + lineId));

        // Release stock reservation for the removed line
        if (!line.isReturn() && line.getProduct().isTrackInventory()) {
            releaseStockForLine(line, transaction);
        }

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
        // Lock the transaction row to prevent concurrent void/complete
        POSTransaction transaction = getTransactionByIdForUpdate(transactionId);

        if (transaction.getStatus() == TransactionStatus.VOIDED) {
            throw new BusinessException("Transaction is already voided");
        }

        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            // If already completed, we need to reverse the effects
            if (transaction.isStockDeducted()) {
                restoreStock(transaction);
            }
            if (transaction.isGlPosted()) {
                try {
                    glIntegrationService.reverseSalesTransaction(transaction.getGlJournalEntryId());
                    transaction.setGlPosted(false);
                    transaction.setGlJournalEntryId(null);
                } catch (Exception e) {
                    log.error("Failed to reverse GL entry for voided transaction {}: {}",
                            transaction.getTransactionNumber(), e.getMessage());
                }
            }
        } else {
            // Pending/Held transaction — release any stock reservations
            releaseAllReservations(transaction);
        }

        transaction.setStatus(TransactionStatus.VOIDED);
        transaction.setVoidedAt(Instant.now());
        transaction.setVoidedBy(userId);
        transaction.setVoidReason(request.getReason());
        transaction = transactionRepository.save(transaction);

        // Recalculate shift totals (voidedCount, totalSales, etc. all recomputed from queries)
        shiftService.recalculateShiftTotals(transaction.getShift().getId());

        log.info("Voided transaction {}: {}", transaction.getTransactionNumber(), request.getReason());

        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public POSTransactionDto completeTransaction(Long transactionId) {
        Long userId = securityContextHelper.getCurrentUserId();
        // Lock the transaction row to prevent concurrent complete/void
        POSTransaction transaction = getTransactionByIdForUpdate(transactionId);

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Only pending transactions can be completed");
        }

        if (transaction.getLines().isEmpty()) {
            throw new BusinessException("Transaction has no items");
        }

        // Validate exchange transactions have both return and sale lines
        if (transaction.getTransactionType() == TransactionType.EXCHANGE) {
            boolean hasReturnLines = transaction.getLines().stream().anyMatch(POSTransactionLine::isReturn);
            boolean hasSaleLines = transaction.getLines().stream().anyMatch(l -> !l.isReturn());
            if (!hasReturnLines || !hasSaleLines) {
                throw new BusinessException("Exchange transactions must have both return and sale line items");
            }
            if (transaction.getOriginalTransactionId() == null) {
                throw new BusinessException("Exchange transactions must reference an original transaction");
            }
        }

        if (!transaction.isFullyPaid()) {
            throw new BusinessException("Transaction is not fully paid. Balance due: " + transaction.getBalanceDue());
        }

        // Release reservations and deduct actual stock
        if (!transaction.isStockDeducted()) {
            releaseAllReservations(transaction);
            deductStock(transaction);
            transaction.setStockDeducted(true);
        }

        // Post to GL (non-blocking — POS must work even if GL accounts aren't configured)
        if (!transaction.isGlPosted()) {
            try {
                Long journalEntryId = glIntegrationService.postPOSTransaction(transaction);
                transaction.setGlJournalEntryId(journalEntryId);
                transaction.setGlPosted(true);
            } catch (Exception e) {
                log.error("Failed to post transaction {} to GL: {}. Retry via POST /api/v1/pos/transactions/{}/retry-gl",
                        transaction.getTransactionNumber(), e.getMessage(), transaction.getId());
            }
        }

        // Create AR Invoice for credit sales BEFORE marking COMPLETED.
        // A credit sale without an AR invoice means the debt is untracked — this must be atomic.
        if (hasCreditPayment(transaction) && transaction.getArInvoiceId() == null) {
            ARInvoiceDto arInvoice = arInvoiceService.createFromPOSTransaction(transaction);
            transaction.setArInvoiceId(arInvoice.getId());
            log.info("Created AR Invoice {} for credit sale transaction {}",
                    arInvoice.getInvoiceNumber(), transaction.getTransactionNumber());
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(Instant.now());
        transaction.setCompletedBy(userId);
        transaction = transactionRepository.save(transaction);

        // Recalculate shift totals from queries (single source of truth)
        shiftService.recalculateShiftTotals(transaction.getShift().getId());

        log.info("Completed transaction {}", transaction.getTransactionNumber());

        return transactionMapper.toDto(transaction);
    }

    // ==================== GL / AR Invoice Retry ====================

    @Transactional(readOnly = true)
    public List<POSTransactionDto> findFailedGlPostings() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionRepository.findCompletedWithoutGlPosting(tenantId).stream()
                .map(transactionMapper::toDtoWithoutDetails)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<POSTransactionDto> findFailedArInvoices() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionRepository.findCompletedCreditWithoutArInvoice(tenantId).stream()
                .map(transactionMapper::toDtoWithoutDetails)
                .toList();
    }

    @Transactional
    public POSTransactionDto retryGlPosting(Long transactionId) {
        POSTransaction transaction = getTransactionById(transactionId);

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new BusinessException("Only completed transactions can be posted to GL");
        }
        if (transaction.isGlPosted()) {
            throw new BusinessException("Transaction is already posted to GL");
        }

        Long journalEntryId = glIntegrationService.postPOSTransaction(transaction);
        transaction.setGlJournalEntryId(journalEntryId);
        transaction.setGlPosted(true);
        transaction = transactionRepository.save(transaction);

        log.info("Retry GL posting succeeded for transaction {}", transaction.getTransactionNumber());
        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public POSTransactionDto retryArInvoiceCreation(Long transactionId) {
        POSTransaction transaction = getTransactionById(transactionId);

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new BusinessException("Only completed transactions can have AR invoices created");
        }
        if (transaction.getArInvoiceId() != null) {
            throw new BusinessException("Transaction already has an AR invoice");
        }
        if (!hasCreditPayment(transaction)) {
            throw new BusinessException("Transaction has no credit payments");
        }

        ARInvoiceDto arInvoice = arInvoiceService.createFromPOSTransaction(transaction);
        transaction.setArInvoiceId(arInvoice.getId());
        transaction = transactionRepository.save(transaction);

        log.info("Retry AR invoice creation succeeded for transaction {} -> invoice {}",
                transaction.getTransactionNumber(), arInvoice.getInvoiceNumber());
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
            if (!line.getProduct().isTrackInventory()) {
                continue;
            }
            Long locationId = line.getLocationId() != null ? line.getLocationId() :
                    transaction.getTerminal().getLocation().getId();
            if (line.isReturn()) {
                // Return lines within an exchange: restore stock
                stockService.addStock(
                        line.getProduct().getId(),
                        locationId,
                        line.getQuantity(),
                        "POS_RETURN",
                        transaction.getId(),
                        "POS Exchange Return: " + transaction.getTransactionNumber()
                );
            } else {
                // Sale lines: deduct stock
                stockService.deductStock(
                        line.getProduct().getId(),
                        locationId,
                        line.getQuantity(),
                        "POS_SALE",
                        transaction.getId(),
                        "POS Transaction: " + transaction.getTransactionNumber()
                );
            }
        }
    }

    private void restoreStock(POSTransaction transaction) {
        // Reverses whatever deductStock did: sale lines get stock back,
        // return lines (in exchanges) get stock re-deducted.
        for (POSTransactionLine line : transaction.getLines()) {
            if (!line.getProduct().isTrackInventory()) {
                continue;
            }
            Long locationId = line.getLocationId() != null ? line.getLocationId() :
                    transaction.getTerminal().getLocation().getId();
            if (line.isReturn()) {
                // Reverse the restoration that happened during completion
                stockService.deductStock(
                        line.getProduct().getId(),
                        locationId,
                        line.getQuantity(),
                        "POS_VOID",
                        transaction.getId(),
                        "Voided POS Exchange Return: " + transaction.getTransactionNumber()
                );
            } else {
                // Reverse the deduction that happened during completion
                stockService.addStock(
                        line.getProduct().getId(),
                        locationId,
                        line.getQuantity(),
                        "POS_VOID",
                        transaction.getId(),
                        "Voided POS Transaction: " + transaction.getTransactionNumber()
                );
            }
        }
    }

    // ==================== Stock Reservation Helpers ====================

    private void reserveStockForLine(Long productId, Long locationId, BigDecimal quantity,
                                     POSTransaction transaction) {
        try {
            stockService.reserveStock(
                    productId, locationId, quantity,
                    MovementReferenceType.POS_TRANSACTION,
                    transaction.getId(),
                    transaction.getTransactionNumber()
            );
        } catch (Exception e) {
            log.warn("Could not reserve stock for product {} on transaction {}: {}",
                    productId, transaction.getTransactionNumber(), e.getMessage());
        }
    }

    private void releaseStockForLine(POSTransactionLine line, POSTransaction transaction) {
        try {
            Long locationId = line.getLocationId() != null ? line.getLocationId() :
                    transaction.getTerminal().getLocation().getId();
            stockService.releaseReservation(
                    line.getProduct().getId(), locationId,
                    MovementReferenceType.POS_TRANSACTION,
                    transaction.getId()
            );
        } catch (Exception e) {
            log.warn("Could not release stock reservation for product {} on transaction {}: {}",
                    line.getProduct().getId(), transaction.getTransactionNumber(), e.getMessage());
        }
    }

    private void releaseAllReservations(POSTransaction transaction) {
        for (POSTransactionLine line : transaction.getLines()) {
            if (!line.isReturn() && line.getProduct().isTrackInventory()) {
                releaseStockForLine(line, transaction);
            }
        }
    }

    private POSTransaction getTransactionById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
    }

    private POSTransaction getTransactionByIdForUpdate(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionRepository.findByIdAndTenantIdForUpdate(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
    }

    private void validateTransactionModifiable(POSTransaction transaction) {
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Transaction cannot be modified in status: " + transaction.getStatus());
        }
    }

    private String generateTransactionNumber(Long tenantId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Use MAX query + retry to handle concurrent generation atomically.
        // If a duplicate constraint fires, the caller's @Transactional will handle retry.
        String maxNumber = transactionRepository.findMaxTransactionNumberByPrefixAndTenantId(
                "TX" + datePart + "-", tenantId);

        int next;
        if (maxNumber != null) {
            // Extract the numeric suffix: TX20260221-00005 → 5
            String suffix = maxNumber.substring(maxNumber.lastIndexOf('-') + 1);
            next = Integer.parseInt(suffix) + 1;
        } else {
            next = 1;
        }

        return String.format("TX%s-%05d", datePart, next);
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
