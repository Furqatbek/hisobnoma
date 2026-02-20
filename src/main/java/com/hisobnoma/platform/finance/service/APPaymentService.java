package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.*;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.APPaymentAllocationMapper;
import com.hisobnoma.platform.finance.mapper.APPaymentMapper;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.inventory.entity.Vendor;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class APPaymentService {

    private final APPaymentRepository apPaymentRepository;
    private final APPaymentAllocationRepository apPaymentAllocationRepository;
    private final APInvoiceRepository apInvoiceRepository;
    private final AccountRepository accountRepository;
    private final VendorRepository vendorRepository;
    private final APPaymentMapper apPaymentMapper;
    private final APPaymentAllocationMapper apPaymentAllocationMapper;
    private final SecurityContextHelper securityContextHelper;
    private final GLIntegrationService glIntegrationService;

    @Transactional(readOnly = true)
    public PageResponse<APPaymentDto> getPayments(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<APPayment> page = apPaymentRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(apPaymentMapper::toDtoWithoutAllocations));
    }

    @Transactional(readOnly = true)
    public PageResponse<APPaymentDto> getPaymentsByVendor(Long vendorId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<APPayment> page = apPaymentRepository.findByTenantIdAndVendorId(tenantId, vendorId, pageable);
        return PageResponse.of(page.map(apPaymentMapper::toDtoWithoutAllocations));
    }

    @Transactional(readOnly = true)
    public PageResponse<APPaymentDto> getPaymentsByStatus(APPaymentStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<APPayment> page = apPaymentRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return PageResponse.of(page.map(apPaymentMapper::toDtoWithoutAllocations));
    }

    @Transactional(readOnly = true)
    public APPaymentDto getPayment(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        APPayment payment = apPaymentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Payment not found with id: " + id));
        return apPaymentMapper.toDto(payment);
    }

    @Transactional
    public APPaymentDto createPayment(CreateAPPaymentRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Validate vendor
        Vendor vendor = vendorRepository.findByIdAndTenantId(request.getVendorId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Vendor not found with id: " + request.getVendorId()));

        // Validate total allocations
        BigDecimal totalAllocated = request.getAllocations().stream()
                .map(a -> a.getAllocatedAmount()
                        .add(a.getDiscountTaken() != null ? a.getDiscountTaken() : BigDecimal.ZERO)
                        .add(a.getWriteOffAmount() != null ? a.getWriteOffAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaymentWithExtras = request.getAllocations().stream()
                .map(CreateAPPaymentAllocationRequest::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaymentWithExtras.compareTo(request.getPaymentAmount()) > 0) {
            throw new BusinessException("Total allocated amount exceeds payment amount");
        }

        // Create payment
        APPayment payment = apPaymentMapper.toEntity(request);
        payment.setTenantId(tenantId);
        payment.setPaymentNumber(generatePaymentNumber(tenantId));
        payment.setVendorName(vendor.getName());
        payment.setStatus(APPaymentStatus.DRAFT);

        // Set bank account name if provided
        if (request.getBankAccountId() != null) {
            // TODO: Lookup bank account name when BankAccount entity is implemented
            payment.setBankAccountName("Bank Account #" + request.getBankAccountId());
        }

        payment = apPaymentRepository.save(payment);

        // Create allocations
        for (CreateAPPaymentAllocationRequest allocationRequest : request.getAllocations()) {
            APInvoice invoice = apInvoiceRepository.findByIdAndTenantId(allocationRequest.getApInvoiceId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Invoice not found: " + allocationRequest.getApInvoiceId()));

            if (invoice.getStatus() != APInvoiceStatus.APPROVED && invoice.getStatus() != APInvoiceStatus.PARTIAL) {
                throw new BusinessException("Invoice " + invoice.getInvoiceNumber() + " is not approved for payment");
            }

            if (invoice.getVendorId().longValue() != request.getVendorId().longValue()) {
                throw new BusinessException("Invoice " + invoice.getInvoiceNumber() + " belongs to a different vendor");
            }

            APPaymentAllocation allocation = APPaymentAllocation.builder()
                    .apInvoice(invoice)
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .invoiceAmount(invoice.getTotalAmount())
                    .invoiceBalanceBefore(invoice.getBalanceDue())
                    .allocatedAmount(allocationRequest.getAllocatedAmount())
                    .discountTaken(allocationRequest.getDiscountTaken())
                    .writeOffAmount(allocationRequest.getWriteOffAmount())
                    .notes(allocationRequest.getNotes())
                    .build();

            allocation.calculateBalanceAfter();
            payment.addAllocation(allocation);
        }

        payment.recalculateAllocations();
        payment = apPaymentRepository.save(payment);

        log.info("Created AP Payment: {} for vendor: {}, amount: {}",
                payment.getPaymentNumber(), vendor.getName(), payment.getPaymentAmount());

        return apPaymentMapper.toDto(payment);
    }

    @Transactional
    public APPaymentDto submitForApproval(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        APPayment payment = apPaymentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Payment not found with id: " + id));

        if (payment.getStatus() != APPaymentStatus.DRAFT) {
            throw new BusinessException("Only draft payments can be submitted for approval");
        }

        payment.setStatus(APPaymentStatus.PENDING_APPROVAL);
        payment.setSubmittedBy(userId);
        payment.setSubmittedAt(Instant.now());

        payment = apPaymentRepository.save(payment);
        log.info("AP Payment {} submitted for approval by user {}", payment.getPaymentNumber(), userId);

        return apPaymentMapper.toDto(payment);
    }

    @Transactional
    public APPaymentDto approvePayment(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        APPayment payment = apPaymentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Payment not found with id: " + id));

        if (payment.getStatus() != APPaymentStatus.PENDING_APPROVAL && payment.getStatus() != APPaymentStatus.DRAFT) {
            throw new BusinessException("Payment cannot be approved in current status: " + payment.getStatus());
        }

        payment.setStatus(APPaymentStatus.APPROVED);
        payment.setApprovedBy(userId);
        payment.setApprovedAt(Instant.now());

        payment = apPaymentRepository.save(payment);
        log.info("AP Payment {} approved by user {}", payment.getPaymentNumber(), userId);

        return apPaymentMapper.toDto(payment);
    }

    @Transactional
    public APPaymentDto processPayment(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        APPayment payment = apPaymentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Payment not found with id: " + id));

        if (payment.getStatus() != APPaymentStatus.APPROVED) {
            throw new BusinessException("Only approved payments can be processed");
        }

        // Apply payments to invoices
        for (APPaymentAllocation allocation : payment.getAllocations()) {
            APInvoice invoice = allocation.getApInvoice();

            // Update invoice paid amount
            invoice.updatePaidAmount(allocation.getTotalApplied());

            apInvoiceRepository.save(invoice);
        }

        // Post to GL
        postToGL(payment);

        // Update vendor balance
        updateVendorBalance(payment.getVendorId(), payment.getPaymentAmount().negate());

        payment.setStatus(APPaymentStatus.COMPLETED);
        payment.setCompletedBy(userId);
        payment.setCompletedAt(Instant.now());

        payment = apPaymentRepository.save(payment);
        log.info("AP Payment {} processed by user {}", payment.getPaymentNumber(), userId);

        return apPaymentMapper.toDto(payment);
    }

    @Transactional
    public APPaymentDto voidPayment(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        APPayment payment = apPaymentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Payment not found with id: " + id));

        if (payment.getStatus() == APPaymentStatus.VOIDED) {
            throw new BusinessException("Payment is already voided");
        }

        if (payment.isReconciled()) {
            throw new BusinessException("Cannot void a reconciled payment");
        }

        // If payment was completed, reverse the effects
        if (payment.getStatus() == APPaymentStatus.COMPLETED) {
            // Reverse invoice payments
            for (APPaymentAllocation allocation : payment.getAllocations()) {
                APInvoice invoice = allocation.getApInvoice();
                invoice.updatePaidAmount(allocation.getTotalApplied().negate());

                // Reset status if needed
                if (invoice.getStatus() == APInvoiceStatus.PAID) {
                    invoice.setStatus(invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0 ?
                            APInvoiceStatus.PARTIAL : APInvoiceStatus.APPROVED);
                }

                apInvoiceRepository.save(invoice);
            }

            // Reverse GL posting
            if (payment.isGlPosted()) {
                reverseGLPosting(payment);
            }

            // Restore vendor balance
            updateVendorBalance(payment.getVendorId(), payment.getPaymentAmount());
        }

        payment.setStatus(APPaymentStatus.VOIDED);
        payment.setVoidedBy(userId);
        payment.setVoidedAt(Instant.now());
        payment.setVoidReason(reason);

        payment = apPaymentRepository.save(payment);
        log.info("AP Payment {} voided by user {}: {}", payment.getPaymentNumber(), userId, reason);

        return apPaymentMapper.toDto(payment);
    }

    @Transactional
    public APPaymentDto reconcilePayment(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        APPayment payment = apPaymentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AP Payment not found with id: " + id));

        if (payment.getStatus() != APPaymentStatus.COMPLETED) {
            throw new BusinessException("Only completed payments can be reconciled");
        }

        if (payment.isReconciled()) {
            throw new BusinessException("Payment is already reconciled");
        }

        payment.setReconciled(true);
        payment.setReconciledBy(userId);
        payment.setReconciledAt(Instant.now());

        payment = apPaymentRepository.save(payment);
        log.info("AP Payment {} reconciled by user {}", payment.getPaymentNumber(), userId);

        return apPaymentMapper.toDto(payment);
    }

    // ============ GL Integration ============

    private void postToGL(APPayment payment) {
        try {
            // Get accounts
            Long apAccountId = payment.getApAccountId();
            if (apAccountId == null) {
                Account apAccount = accountRepository.findByCodeAndTenantId("2100", payment.getTenantId())
                        .orElseThrow(() -> new BusinessException("AP control account not found"));
                apAccountId = apAccount.getId();
                payment.setApAccountId(apAccountId);
            }

            Long cashAccountId = payment.getCashAccountId();
            if (cashAccountId == null) {
                // Use default cash account
                Account cashAccount = accountRepository.findByCodeAndTenantId("1110", payment.getTenantId())
                        .orElseThrow(() -> new BusinessException("Cash account not found"));
                cashAccountId = cashAccount.getId();
                payment.setCashAccountId(cashAccountId);
            }

            // Create journal entry
            // Debit: Accounts Payable
            // Credit: Cash/Bank
            Long journalEntryId = glIntegrationService.postAPPayment(payment);

            payment.setGlPosted(true);
            payment.setGlJournalEntryId(journalEntryId);
            payment.setGlPostedAt(Instant.now());

            log.info("Posted AP Payment {} to GL, Journal Entry ID: {}", payment.getPaymentNumber(), journalEntryId);
        } catch (Exception e) {
            log.error("Failed to post AP Payment {} to GL: {}", payment.getPaymentNumber(), e.getMessage());
            throw new BusinessException("Failed to post to GL: " + e.getMessage());
        }
    }

    private void reverseGLPosting(APPayment payment) {
        if (payment.getGlJournalEntryId() != null) {
            try {
                glIntegrationService.reverseAPPayment(payment);
                payment.setGlPosted(false);
                log.info("Reversed GL posting for AP Payment {}", payment.getPaymentNumber());
            } catch (Exception e) {
                log.error("Failed to reverse GL posting for AP Payment {}: {}", payment.getPaymentNumber(), e.getMessage());
                throw new BusinessException("Failed to reverse GL posting: " + e.getMessage());
            }
        }
    }

    // ============ Helper Methods ============

    private String generatePaymentNumber(Long tenantId) {
        Integer maxNumber = apPaymentRepository.findMaxPaymentNumber(tenantId);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("PAY-%06d", nextNumber);
    }

    private void updateVendorBalance(Long vendorId, BigDecimal amount) {
        vendorRepository.findById(vendorId).ifPresent(vendor -> {
            BigDecimal currentBalance = vendor.getCurrentBalance() != null ? vendor.getCurrentBalance() : BigDecimal.ZERO;
            vendor.setCurrentBalance(currentBalance.add(amount));
            vendorRepository.save(vendor);
        });
    }

    // ============ Reporting Methods ============

    @Transactional(readOnly = true)
    public List<APPaymentDto> getPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<APPayment> payments = apPaymentRepository.findByDateRange(tenantId, startDate, endDate);
        return apPaymentMapper.toDtoList(payments);
    }

    @Transactional(readOnly = true)
    public List<APPaymentDto> getUnreconciledPayments() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<APPayment> payments = apPaymentRepository.findByTenantIdAndReconciledFalseAndStatus(
                tenantId, APPaymentStatus.COMPLETED);
        return apPaymentMapper.toDtoList(payments);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaymentsByVendor(Long vendorId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return apPaymentRepository.sumPaymentsByVendorAndStatus(tenantId, vendorId, APPaymentStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return apPaymentRepository.sumPaymentsByDateRangeAndStatus(tenantId, startDate, endDate, APPaymentStatus.COMPLETED);
    }
}
