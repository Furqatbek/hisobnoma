package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.*;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.APInvoiceLineMapper;
import com.hisobnoma.platform.finance.mapper.APInvoiceMapper;
import com.hisobnoma.platform.finance.repository.APInvoiceLineRepository;
import com.hisobnoma.platform.finance.repository.APInvoiceRepository;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.inventory.entity.PurchaseOrder;
import com.hisobnoma.platform.inventory.entity.PurchaseOrderLine;
import com.hisobnoma.platform.inventory.entity.ReceivingOrder;
import com.hisobnoma.platform.inventory.entity.ReceivingLine;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.repository.PurchaseOrderRepository;
import com.hisobnoma.platform.inventory.repository.ReceivingOrderRepository;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class APInvoiceService {

    private final APInvoiceRepository apInvoiceRepository;
    private final APInvoiceLineRepository apInvoiceLineRepository;
    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ReceivingOrderRepository receivingOrderRepository;
    private final AccountRepository accountRepository;
    private final APInvoiceMapper apInvoiceMapper;
    private final APInvoiceLineMapper apInvoiceLineMapper;
    private final SecurityContextHelper securityContextHelper;
    private final GLIntegrationService glIntegrationService;

    private static final List<APInvoiceStatus> EXCLUDED_STATUSES = Arrays.asList(
            APInvoiceStatus.CANCELLED, APInvoiceStatus.PAID
    );

    @Transactional(readOnly = true)
    public PageResponse<APInvoiceDto> getInvoices(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<APInvoice> page = apInvoiceRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(apInvoiceMapper::toDtoWithoutLines));
    }

    @Transactional(readOnly = true)
    public PageResponse<APInvoiceDto> getInvoicesByVendor(Long vendorId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<APInvoice> page = apInvoiceRepository.findByTenantIdAndVendorId(tenantId, vendorId, pageable);
        return PageResponse.of(page.map(apInvoiceMapper::toDtoWithoutLines));
    }

    @Transactional(readOnly = true)
    public PageResponse<APInvoiceDto> getInvoicesByStatus(APInvoiceStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<APInvoice> page = apInvoiceRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return PageResponse.of(page.map(apInvoiceMapper::toDtoWithoutLines));
    }

    @Transactional(readOnly = true)
    public APInvoiceDto getInvoice(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        APInvoice invoice = apInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Invoice not found with id: " + id));
        return apInvoiceMapper.toDto(invoice);
    }

    @Transactional(readOnly = true)
    public List<APInvoiceDto> getUnpaidInvoicesByVendor(Long vendorId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<APInvoiceStatus> payableStatuses = Arrays.asList(
                APInvoiceStatus.APPROVED, APInvoiceStatus.PARTIAL
        );
        List<APInvoice> invoices = apInvoiceRepository.findUnpaidInvoicesByVendor(tenantId, vendorId, payableStatuses);
        return apInvoiceMapper.toDtoList(invoices);
    }

    @Transactional(readOnly = true)
    public List<APInvoiceDto> getOverdueInvoices() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<APInvoice> invoices = apInvoiceRepository.findOverdueInvoices(
                tenantId, LocalDate.now(), EXCLUDED_STATUSES);
        return apInvoiceMapper.toDtoList(invoices);
    }

    @Transactional
    public APInvoiceDto createInvoice(CreateAPInvoiceRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Create invoice
        APInvoice invoice = apInvoiceMapper.toEntity(request);
        invoice.setTenantId(tenantId);
        invoice.setInvoiceNumber(generateInvoiceNumber(tenantId));
        invoice.setStatus(APInvoiceStatus.DRAFT);

        // Validate and set vendor info (optional — allows vendor-less expenses like rent, utilities)
        if (request.getVendorId() != null) {
            Vendor vendor = vendorRepository.findByIdAndTenantId(request.getVendorId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Vendor not found with id: " + request.getVendorId()));
            invoice.setVendorName(vendor.getName());

            // Set default accounts if not provided
            if (invoice.getExpenseAccountId() == null && vendor.getDefaultExpenseAccountId() != null) {
                invoice.setExpenseAccountId(vendor.getDefaultExpenseAccountId());
            }

            // Set payment terms from vendor if not provided
            if (invoice.getPaymentTerms() == null) {
                invoice.setPaymentTerms(vendor.getPaymentTerms());
                invoice.setPaymentTermsDays(vendor.getPaymentTermsDays());
            }
        } else {
            // For vendor-less expenses, use description as display name
            invoice.setVendorName(request.getDescription());
        }

        // Handle PO and Receiving Order links
        if (request.getPurchaseOrderId() != null) {
            PurchaseOrder po = purchaseOrderRepository.findByIdAndTenantId(request.getPurchaseOrderId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Purchase Order not found"));
            invoice.setPurchaseOrderNumber(po.getPoNumber());
        }

        if (request.getReceivingOrderId() != null) {
            ReceivingOrder ro = receivingOrderRepository.findByIdAndTenantId(request.getReceivingOrderId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Receiving Order not found"));
            invoice.setReceivingOrderNumber(ro.getReceivingNumber());
        }

        invoice = apInvoiceRepository.save(invoice);

        // Add lines
        int lineNumber = 1;
        for (CreateAPInvoiceLineRequest lineRequest : request.getLines()) {
            APInvoiceLine line = apInvoiceLineMapper.toEntity(lineRequest);
            line.setLineNumber(lineNumber++);
            line.calculateLineTotal();
            invoice.addLine(line);
        }

        invoice.recalculateTotals();

        // Perform 3-way matching if linked to PO/Receiving
        if (invoice.getPurchaseOrderId() != null || invoice.getReceivingOrderId() != null) {
            performThreeWayMatching(invoice);
        }

        invoice = apInvoiceRepository.save(invoice);
        log.info("Created AP Invoice: {} for: {}", invoice.getInvoiceNumber(), invoice.getVendorName());

        return apInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public APInvoiceDto createFromReceiving(Long receivingOrderId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ReceivingOrder receiving = receivingOrderRepository.findByIdAndTenantId(receivingOrderId, tenantId)
                .orElseThrow(() -> new NotFoundException("Receiving Order not found"));

        if (receiving.isApInvoiceCreated()) {
            throw new BusinessException("AP Invoice already created for this receiving order");
        }

        Vendor vendor = receiving.getVendor();

        // Create invoice
        APInvoice invoice = APInvoice.builder()
                .tenantId(tenantId)
                .invoiceNumber(generateInvoiceNumber(tenantId))
                .vendorInvoiceNumber(receiving.getVendorInvoiceNumber())
                .vendorId(vendor.getId())
                .vendorName(vendor.getName())
                .invoiceDate(receiving.getVendorInvoiceDate() != null ? receiving.getVendorInvoiceDate() : LocalDate.now())
                .dueDate(calculateDueDate(vendor))
                .receivedDate(receiving.getReceivingDate())
                .status(APInvoiceStatus.DRAFT)
                .purchaseOrderId(receiving.getPurchaseOrder() != null ? receiving.getPurchaseOrder().getId() : null)
                .purchaseOrderNumber(receiving.getPurchaseOrder() != null ? receiving.getPurchaseOrder().getPoNumber() : null)
                .receivingOrderId(receivingOrderId)
                .receivingOrderNumber(receiving.getReceivingNumber())
                .discountAmount(receiving.getDiscountAmount())
                .taxAmount(receiving.getTaxAmount())
                .shippingAmount(receiving.getShippingAmount())
                .currency(receiving.getCurrency())
                .paymentTerms(vendor.getPaymentTerms())
                .paymentTermsDays(vendor.getPaymentTermsDays())
                .expenseAccountId(vendor.getDefaultExpenseAccountId())
                .build();

        invoice = apInvoiceRepository.save(invoice);

        // Create lines from receiving lines
        int lineNumber = 1;
        for (ReceivingLine receivingLine : receiving.getLines()) {
            APInvoiceLine line = APInvoiceLine.builder()
                    .lineNumber(lineNumber++)
                    .productId(receivingLine.getProduct().getId())
                    .productSku(receivingLine.getProduct().getSku())
                    .productName(receivingLine.getProduct().getName())
                    .description(receivingLine.getProduct().getName())
                    .quantity(receivingLine.getReceivedQuantity())
                    .unitOfMeasure(receivingLine.getUom() != null ? receivingLine.getUom().getCode() : null)
                    .unitPrice(receivingLine.getUnitCost())
                    .lineTotal(receivingLine.getLineTotal())
                    .taxAmount(receivingLine.getTaxAmount())
                    .receivingLineId(receivingLine.getId())
                    .orderedQuantity(receivingLine.getExpectedQuantity())
                    .receivedQuantity(receivingLine.getReceivedQuantity())
                    .build();

            if (receiving.getPurchaseOrder() != null) {
                line.setPurchaseOrderLineId(receivingLine.getPoLine() != null ?
                        receivingLine.getPoLine().getId() : null);
            }

            invoice.addLine(line);
        }

        invoice.recalculateTotals();
        performThreeWayMatching(invoice);

        invoice = apInvoiceRepository.save(invoice);

        // Update receiving order
        receiving.setApInvoiceCreated(true);
        receiving.setApInvoiceId(invoice.getId());
        receivingOrderRepository.save(receiving);

        log.info("Created AP Invoice {} from Receiving Order {}", invoice.getInvoiceNumber(), receiving.getReceivingNumber());

        return apInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public APInvoiceDto updateInvoice(Long id, CreateAPInvoiceRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        APInvoice invoice = apInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Invoice not found with id: " + id));

        if (invoice.getStatus() != APInvoiceStatus.DRAFT) {
            throw new BusinessException("Only draft invoices can be updated");
        }

        apInvoiceMapper.updateEntity(request, invoice);

        // Update vendor name
        if (request.getVendorId() != null) {
            Vendor vendor = vendorRepository.findByIdAndTenantId(request.getVendorId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Vendor not found with id: " + request.getVendorId()));
            invoice.setVendorName(vendor.getName());
        } else {
            invoice.setVendorId(null);
            invoice.setVendorName(request.getDescription());
        }

        // Update lines
        invoice.getLines().clear();
        int lineNumber = 1;
        for (CreateAPInvoiceLineRequest lineRequest : request.getLines()) {
            APInvoiceLine line = apInvoiceLineMapper.toEntity(lineRequest);
            line.setLineNumber(lineNumber++);
            line.calculateLineTotal();
            invoice.addLine(line);
        }

        invoice.recalculateTotals();

        if (invoice.getPurchaseOrderId() != null || invoice.getReceivingOrderId() != null) {
            performThreeWayMatching(invoice);
        }

        invoice = apInvoiceRepository.save(invoice);
        return apInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public APInvoiceDto submitForApproval(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        APInvoice invoice = apInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Invoice not found with id: " + id));

        if (invoice.getStatus() != APInvoiceStatus.DRAFT) {
            throw new BusinessException("Only draft invoices can be submitted for approval");
        }

        invoice.setStatus(APInvoiceStatus.PENDING_APPROVAL);
        invoice.setSubmittedBy(userId);
        invoice.setSubmittedAt(Instant.now());

        invoice = apInvoiceRepository.save(invoice);
        log.info("AP Invoice {} submitted for approval by user {}", invoice.getInvoiceNumber(), userId);

        return apInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public APInvoiceDto approveInvoice(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        APInvoice invoice = apInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Invoice not found with id: " + id));

        if (invoice.getStatus() != APInvoiceStatus.PENDING_APPROVAL && invoice.getStatus() != APInvoiceStatus.DRAFT) {
            throw new BusinessException("Invoice cannot be approved in current status: " + invoice.getStatus());
        }

        // Check 3-way matching if applicable
        if ("FAILED".equals(invoice.getMatchingStatus())) {
            throw new BusinessException("Invoice failed 3-way matching. Please review matching notes.");
        }

        invoice.setStatus(APInvoiceStatus.APPROVED);
        invoice.setApprovedBy(userId);
        invoice.setApprovedAt(Instant.now());

        // Post to GL
        postToGL(invoice);

        invoice = apInvoiceRepository.save(invoice);

        // Update vendor balance
        updateVendorBalance(invoice.getVendorId(), invoice.getTenantId(), invoice.getTotalAmount());

        log.info("AP Invoice {} approved by user {}", invoice.getInvoiceNumber(), userId);

        return apInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public APInvoiceDto rejectInvoice(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        APInvoice invoice = apInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Invoice not found with id: " + id));

        if (invoice.getStatus() != APInvoiceStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only pending invoices can be rejected");
        }

        invoice.setStatus(APInvoiceStatus.DRAFT);
        invoice.setRejectedBy(userId);
        invoice.setRejectedAt(Instant.now());
        invoice.setRejectionReason(reason);

        invoice = apInvoiceRepository.save(invoice);
        log.info("AP Invoice {} rejected by user {}: {}", invoice.getInvoiceNumber(), userId, reason);

        return apInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public APInvoiceDto cancelInvoice(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        APInvoice invoice = apInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Invoice not found with id: " + id));

        if (invoice.getStatus() == APInvoiceStatus.PAID) {
            throw new BusinessException("Paid invoices cannot be cancelled");
        }

        if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Invoice has payments applied. Cannot cancel.");
        }

        // Reverse GL if posted
        if (invoice.isGlPosted()) {
            reverseGLPosting(invoice);
        }

        invoice.setStatus(APInvoiceStatus.CANCELLED);
        invoice.setCancelledBy(userId);
        invoice.setCancelledAt(Instant.now());
        invoice.setCancellationReason(reason);

        invoice = apInvoiceRepository.save(invoice);
        log.info("AP Invoice {} cancelled by user {}: {}", invoice.getInvoiceNumber(), userId, reason);

        return apInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public APInvoiceDto holdInvoice(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        APInvoice invoice = apInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Invoice not found with id: " + id));

        if (invoice.getStatus() == APInvoiceStatus.PAID || invoice.getStatus() == APInvoiceStatus.CANCELLED) {
            throw new BusinessException("Cannot hold invoice in current status");
        }

        invoice.setStatus(APInvoiceStatus.ON_HOLD);
        invoice = apInvoiceRepository.save(invoice);

        return apInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public APInvoiceDto releaseHold(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        APInvoice invoice = apInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Invoice not found with id: " + id));

        if (invoice.getStatus() != APInvoiceStatus.ON_HOLD) {
            throw new BusinessException("Invoice is not on hold");
        }

        // Return to approved if it was approved before hold
        invoice.setStatus(invoice.getApprovedAt() != null ? APInvoiceStatus.APPROVED : APInvoiceStatus.DRAFT);
        invoice = apInvoiceRepository.save(invoice);

        return apInvoiceMapper.toDto(invoice);
    }

    // ============ 3-Way Matching ============

    private void performThreeWayMatching(APInvoice invoice) {
        StringBuilder notes = new StringBuilder();
        boolean allMatch = true;

        for (APInvoiceLine line : invoice.getLines()) {
            line.calculateVariance();

            if (line.getVarianceQuantity() != null &&
                line.getVarianceQuantity().abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                allMatch = false;
                notes.append(String.format("Line %d: Quantity variance of %s units. ",
                        line.getLineNumber(), line.getVarianceQuantity()));
            }

            if (line.getVarianceAmount() != null &&
                line.getVarianceAmount().abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                allMatch = false;
                notes.append(String.format("Line %d: Amount variance of %s. ",
                        line.getLineNumber(), line.getVarianceAmount()));
            }
        }

        invoice.setMatchingStatus(allMatch ? "MATCHED" : "VARIANCE");
        invoice.setMatchingNotes(notes.length() > 0 ? notes.toString() : "All lines matched successfully");
    }

    // ============ GL Integration ============

    private void postToGL(APInvoice invoice) {
        try {
            // Get AP control account
            Long apAccountId = invoice.getApAccountId();
            if (apAccountId == null) {
                // Find default AP account
                Account apAccount = accountRepository.findByCodeAndTenantId("2100", invoice.getTenantId())
                        .orElseThrow(() -> new BusinessException("AP control account not found"));
                apAccountId = apAccount.getId();
                invoice.setApAccountId(apAccountId);
            }

            // Create journal entry
            // Debit: Expense accounts (from lines)
            // Credit: Accounts Payable
            Long journalEntryId = glIntegrationService.postAPInvoice(invoice);

            invoice.setGlPosted(true);
            invoice.setGlJournalEntryId(journalEntryId);
            invoice.setGlPostedAt(Instant.now());

            log.info("Posted AP Invoice {} to GL, Journal Entry ID: {}", invoice.getInvoiceNumber(), journalEntryId);
        } catch (Exception e) {
            log.error("Failed to post AP Invoice {} to GL: {}", invoice.getInvoiceNumber(), e.getMessage());
            throw new BusinessException("Failed to post to GL: " + e.getMessage());
        }
    }

    private void reverseGLPosting(APInvoice invoice) {
        if (invoice.getGlJournalEntryId() != null) {
            try {
                glIntegrationService.reverseAPInvoice(invoice);
                invoice.setGlPosted(false);
                log.info("Reversed GL posting for AP Invoice {}", invoice.getInvoiceNumber());
            } catch (Exception e) {
                log.error("Failed to reverse GL posting for AP Invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage());
                throw new BusinessException("Failed to reverse GL posting: " + e.getMessage());
            }
        }
    }

    // ============ Helper Methods ============

    private String generateInvoiceNumber(Long tenantId) {
        Integer maxNumber = apInvoiceRepository.findMaxInvoiceNumber(tenantId);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("AP-%06d", nextNumber);
    }

    private LocalDate calculateDueDate(Vendor vendor) {
        int days = vendor.getPaymentTermsDays() != null ? vendor.getPaymentTermsDays() : 30;
        return LocalDate.now().plusDays(days);
    }

    private void updateVendorBalance(Long vendorId, Long tenantId, BigDecimal amount) {
        if (vendorId == null) return;
        vendorRepository.findByIdAndTenantId(vendorId, tenantId).ifPresent(vendor -> {
            BigDecimal currentBalance = vendor.getCurrentBalance() != null ? vendor.getCurrentBalance() : BigDecimal.ZERO;
            vendor.setCurrentBalance(currentBalance.add(amount));
            vendorRepository.save(vendor);
        });
    }

    // ============ Reporting Methods ============

    @Transactional(readOnly = true)
    public BigDecimal getTotalPayable() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return apInvoiceRepository.sumTotalBalanceDue(tenantId, EXCLUDED_STATUSES);
    }

    @Transactional(readOnly = true)
    public BigDecimal getVendorBalance(Long vendorId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return apInvoiceRepository.sumBalanceDueByVendor(tenantId, vendorId, EXCLUDED_STATUSES);
    }

    @Transactional(readOnly = true)
    public BigDecimal getOverdueBalance() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return apInvoiceRepository.sumOverdueBalance(tenantId, LocalDate.now(), EXCLUDED_STATUSES);
    }
}
