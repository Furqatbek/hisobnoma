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
import com.hisobnoma.platform.inventory.entity.ProductUom;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductUomRepository;
import com.hisobnoma.platform.inventory.repository.ProductVariantRepository;
import com.hisobnoma.platform.inventory.service.StockService;
import com.hisobnoma.platform.delivery.repository.DeliveryRegionRepository;
import com.hisobnoma.platform.delivery.repository.DeliveryVillageRepository;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.event.PosSaleCompletedEvent;
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
    private final ProductUomRepository productUomRepository;
    private final CustomerRepository customerRepository;
    private final DeliveryRegionRepository deliveryRegionRepository;
    private final DeliveryVillageRepository deliveryVillageRepository;
    private final POSTransactionMapper transactionMapper;
    private final SecurityContextHelper securityContextHelper;
    private final StockService stockService;
    private final TaxCalculationService taxCalculationService;
    private final GLIntegrationService glIntegrationService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final ARInvoiceService arInvoiceService;
    private final ShiftService shiftService;
    private final POSReturnService posReturnService;

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

    @Transactional(readOnly = true)
    public Page<POSTransactionDto> findByCustomer(Long customerId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionRepository.findByCustomerIdAndTenantId(customerId, tenantId, pageable)
                .map(transactionMapper::toDtoWithoutDetails);
    }

    @Transactional(readOnly = true)
    public List<POSTransactionDto> findUnresolvedTransactions(Long shiftId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return transactionMapper.toDtoList(
                transactionRepository.findUnresolvedByShiftIdAndTenantId(shiftId, tenantId));
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

        // Resolve delivery address names and original transaction number before save
        {
            final POSTransaction txn = transaction;
            if (request.getDeliveryRegionId() != null) {
                deliveryRegionRepository.findByIdAndTenantId(request.getDeliveryRegionId(), tenantId)
                        .ifPresent(r -> txn.setDeliveryRegionName(r.getName()));
            }
            if (request.getDeliveryVillageId() != null) {
                deliveryVillageRepository.findByIdAndTenantId(request.getDeliveryVillageId(), tenantId)
                        .ifPresent(v -> txn.setDeliveryVillageName(v.getName()));
            }
            if (request.getOriginalTransactionId() != null) {
                transactionRepository.findByIdAndTenantId(request.getOriginalTransactionId(), tenantId)
                        .ifPresent(orig -> txn.setOriginalTransactionNumber(orig.getTransactionNumber()));
            }
        }

        transaction = transactionRepository.save(transaction);
        log.info("Created POS transaction {} of type {}", transactionNumber, request.getTransactionType());

        // Add line items if provided
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            // Bulk-fetch products/variants/UOMs once instead of 3 queries per line
            var productsById = productRepository.findByIdInAndTenantId(
                            request.getItems().stream()
                                    .map(CreateTransactionRequest.LineItem::getProductId)
                                    .collect(java.util.stream.Collectors.toSet()), tenantId)
                    .stream().collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
            var variantIds = request.getItems().stream()
                    .map(CreateTransactionRequest.LineItem::getVariantId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            var variantsById = variantIds.isEmpty()
                    ? java.util.Map.<Long, ProductVariant>of()
                    : variantRepository.findAllById(variantIds)
                            .stream().collect(java.util.stream.Collectors.toMap(ProductVariant::getId, v -> v));
            var uomIds = request.getItems().stream()
                    .map(CreateTransactionRequest.LineItem::getProductUomId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            var uomsById = uomIds.isEmpty()
                    ? java.util.Map.<Long, ProductUom>of()
                    : productUomRepository.findByIdInAndTenantId(uomIds, tenantId)
                            .stream().collect(java.util.stream.Collectors.toMap(ProductUom::getId, u -> u));

            int lineNumber = 0;
            for (CreateTransactionRequest.LineItem item : request.getItems()) {
                lineNumber++;
                Product product = productsById.get(item.getProductId());
                if (product == null) {
                    throw new NotFoundException("Product not found: " + item.getProductId());
                }

                ProductVariant variant = null;
                if (item.getVariantId() != null) {
                    variant = variantsById.get(item.getVariantId());
                    if (variant == null || !variant.getProduct().getId().equals(product.getId())) {
                        throw new NotFoundException("Variant not found: " + item.getVariantId());
                    }
                }

                // Resolve alternate UOM if specified
                ProductUom productUom = null;
                BigDecimal saleQuantity = item.getQuantity();
                BigDecimal baseQuantity = item.getQuantity();

                if (item.getProductUomId() != null) {
                    productUom = uomsById.get(item.getProductUomId());
                    if (productUom == null) {
                        throw new NotFoundException("Product UOM not found: " + item.getProductUomId());
                    }
                    baseQuantity = productUom.toBaseQuantity(saleQuantity);
                }

                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() :
                        (productUom != null ? productUom.getEffectiveSellingPrice() :
                        (variant != null ? variant.getEffectiveSellingPrice() : product.getSellingPrice()));

                // Calculate tax — unitPrice and saleQuantity are in the sale UOM
                BigDecimal taxAmount = BigDecimal.ZERO;
                BigDecimal taxRate = BigDecimal.ZERO;
                String taxCode = null;

                BigDecimal lineDiscount = item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO;

                if (product.getTaxCode() != null) {
                    taxCode = product.getTaxCode();
                    TaxRate rate = taxCalculationService.getApplicableRate(taxCode, LocalDate.now());
                    if (rate != null) {
                        taxRate = rate.getRate();
                        BigDecimal grossAmount = unitPrice.multiply(saleQuantity);
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
                        .quantity(baseQuantity)
                        .unitPrice(unitPrice)
                        .originalPrice(product.getSellingPrice())
                        .costPrice(variant != null ? variant.getEffectiveCostPrice() : product.getCostPrice())
                        .discountAmount(lineDiscount)
                        .discountReason(item.getDiscountReason())
                        .taxCode(taxCode)
                        .taxRate(taxRate)
                        .taxAmount(taxAmount)
                        .isReturn(transaction.getTransactionType() == TransactionType.RETURN)
                        .saleUomId(productUom != null ? productUom.getUom().getId() : null)
                        .saleQuantity(productUom != null ? saleQuantity : null)
                        .saleUomCode(productUom != null ? productUom.getUom().getCode() : null)
                        .saleUomName(productUom != null ? productUom.getUom().getName() : null)
                        .build();

                line.calculateLineTotal();
                line = lineRepository.save(line);
                transaction.addLine(line);

                // Reserve stock in BASE UOM to prevent overselling
                if (!line.isReturn() && product.isTrackInventory()) {
                    Long locId = transaction.getTerminal().getLocation().getId();
                    reserveStockForLine(product.getId(), locId, baseQuantity, transaction);
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

        // Get variant if provided — must belong to the (tenant-scoped) product
        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .filter(v -> v.getProduct().getId().equals(product.getId()))
                    .orElseThrow(() -> new NotFoundException("Variant not found: " + request.getVariantId()));
        }

        // Resolve alternate UOM if specified
        ProductUom productUom = null;
        BigDecimal saleQuantity = request.getQuantity();
        BigDecimal baseQuantity = request.getQuantity();

        if (request.getProductUomId() != null) {
            productUom = productUomRepository.findByIdAndTenantId(request.getProductUomId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Product UOM not found: " + request.getProductUomId()));
            baseQuantity = productUom.toBaseQuantity(saleQuantity);
        }

        // Determine unit price (per sale UOM unit)
        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() :
                (productUom != null ? productUom.getEffectiveSellingPrice() :
                (variant != null ? variant.getEffectiveSellingPrice() : product.getSellingPrice()));

        // Get next line number
        Integer maxLineNumber = lineRepository.findMaxLineNumberByTransactionId(transactionId);
        int lineNumber = (maxLineNumber != null ? maxLineNumber : 0) + 1;

        // Calculate tax on post-discount amount (using sale UOM quantity)
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal taxRate = BigDecimal.ZERO;
        String taxCode = null;
        BigDecimal lineDiscount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;

        if (product.getTaxCode() != null) {
            taxCode = product.getTaxCode();
            TaxRate rate = taxCalculationService.getApplicableRate(taxCode, LocalDate.now());
            if (rate != null) {
                taxRate = rate.getRate();
                BigDecimal grossAmount = unitPrice.multiply(saleQuantity);
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
                .quantity(baseQuantity)
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
                .saleUomId(productUom != null ? productUom.getUom().getId() : null)
                .saleQuantity(productUom != null ? saleQuantity : null)
                .saleUomCode(productUom != null ? productUom.getUom().getCode() : null)
                .saleUomName(productUom != null ? productUom.getUom().getName() : null)
                .notes(request.getNotes())
                .build();

        line.calculateLineTotal();
        line = lineRepository.save(line);
        transaction.addLine(line);
        transaction = transactionRepository.save(transaction);

        // Reserve stock in BASE UOM to prevent overselling
        if (!line.isReturn() && product.isTrackInventory()) {
            Long locId = request.getLocationId() != null ? request.getLocationId() :
                    transaction.getTerminal().getLocation().getId();
            reserveStockForLine(product.getId(), locId, baseQuantity, transaction);
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

        // GL posting is deferred to a PosSaleCompletedEvent handled AFTER this transaction commits
        // (see PosGlPostingListener), so the GL entry is only ever created for a sale that actually
        // committed — no phantom revenue if a later step here rolls the sale back.

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

        // Fires only if this transaction commits → GL is posted for committed sales only.
        eventPublisher.publishEvent(new PosSaleCompletedEvent(transaction.getId()));

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

    // ==================== Return Operations (delegates to POSReturnService) ====================

    /**
     * Create a return transaction.
     */
    @Transactional
    public POSTransactionDto createReturn(CreateReturnRequest request) {
        return posReturnService.createReturn(request);
    }
}
