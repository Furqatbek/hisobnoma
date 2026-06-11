package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.ARPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateARPaymentAllocationRequest;
import com.hisobnoma.platform.finance.dto.CreateARPaymentRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.ARPaymentMapper;
import com.hisobnoma.platform.finance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ARPaymentService {

    private final ARPaymentRepository arPaymentRepository;
    private final ARPaymentAllocationRepository arPaymentAllocationRepository;
    private final ARInvoiceRepository arInvoiceRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final CustomerRepository customerRepository;
    private final ARPaymentMapper arPaymentMapper;
    private final ARInvoiceService arInvoiceService;
    private final CustomerService customerService;
    private final GLIntegrationService glIntegrationService;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<ARPaymentDto> getPayments(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<ARPayment> page = arPaymentRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(arPaymentMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<ARPaymentDto> getPaymentsByCustomer(Long customerId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<ARPayment> page = arPaymentRepository.findByTenantIdAndCustomer_Id(tenantId, customerId, pageable);
        return PageResponse.of(page.map(arPaymentMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<ARPaymentDto> getPaymentsByStatus(ARPaymentStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<ARPayment> page = arPaymentRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return PageResponse.of(page.map(arPaymentMapper::toDto));
    }

    @Transactional(readOnly = true)
    public ARPaymentDto getPayment(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        ARPayment payment = arPaymentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Payment not found with id: " + id));
        return arPaymentMapper.toDto(payment);
    }

    @Transactional(readOnly = true)
    public ARPaymentDto getPaymentByNumber(String paymentNumber) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        ARPayment payment = arPaymentRepository.findByPaymentNumberAndTenantId(paymentNumber, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Payment not found with number: " + paymentNumber));
        return arPaymentMapper.toDto(payment);
    }

    @Transactional(readOnly = true)
    public List<ARPaymentDto> getPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ARPayment> payments = arPaymentRepository.findByDateRange(tenantId, startDate, endDate);
        return arPaymentMapper.toDtoList(payments);
    }

    @Transactional
    public ARPaymentDto createPayment(CreateARPaymentRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Validate customer exists
        Customer customer = customerRepository.findByIdAndTenantId(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + request.getCustomerId()));

        // Generate payment number
        String paymentNumber = generatePaymentNumber(tenantId);

        ARPayment payment = arPaymentMapper.toEntity(request);
        payment.setTenantId(tenantId);
        payment.setPaymentNumber(paymentNumber);
        payment.setCustomer(customer);
        payment.setStatus(ARPaymentStatus.PENDING);
        payment.setAllocatedAmount(BigDecimal.ZERO);
        payment.setUnallocatedAmount(request.getPaymentAmount());

        // Handle allocations if provided
        if (request.getAllocations() != null && !request.getAllocations().isEmpty()) {
            List<ARPaymentAllocation> allocations = new ArrayList<>();
            BigDecimal totalAllocated = BigDecimal.ZERO;

            for (CreateARPaymentAllocationRequest allocRequest : request.getAllocations()) {
                ARPaymentAllocation allocation = createAllocation(payment, allocRequest);
                allocations.add(allocation);
                totalAllocated = totalAllocated.add(allocRequest.getAllocatedAmount());
            }

            if (totalAllocated.compareTo(request.getPaymentAmount()) > 0) {
                throw new BusinessException("Total allocated amount exceeds payment amount");
            }

            payment.setAllocations(allocations);
            payment.setAllocatedAmount(totalAllocated);
            payment.setUnallocatedAmount(request.getPaymentAmount().subtract(totalAllocated));
        }

        payment = arPaymentRepository.save(payment);
        log.info("Created AR payment: {} for customer: {}", paymentNumber, customer.getCode());

        return arPaymentMapper.toDto(payment);
    }

    @Transactional
    public ARPaymentDto completePayment(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ARPayment payment = arPaymentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Payment not found with id: " + id));

        if (payment.getStatus() != ARPaymentStatus.PENDING) {
            throw new BusinessException("Only pending payments can be completed");
        }

        // Post to GL
        glIntegrationService.postARPayment(payment);

        // Apply allocations to invoices
        if (payment.getAllocations() != null) {
            for (ARPaymentAllocation allocation : payment.getAllocations()) {
                if (allocation.getArInvoice() != null) {
                    arInvoiceService.applyPayment(
                            allocation.getArInvoice(),
                            allocation.getAllocatedAmount(),
                            allocation.getDiscountTaken(),
                            allocation.getWriteOffAmount()
                    );
                }
            }
        }

        // Update customer balance
        customerService.updateCustomerBalance(payment.getCustomer(), payment.getPaymentAmount().negate());

        payment.setStatus(ARPaymentStatus.COMPLETED);
        payment = arPaymentRepository.save(payment);

        log.info("Completed AR payment: {}", payment.getPaymentNumber());

        return arPaymentMapper.toDto(payment);
    }

    /**
     * Creates a payment and immediately completes it in a single transaction.
     * This ensures atomicity — either the full operation succeeds (payment created,
     * GL posted, invoices updated, customer balance adjusted) or nothing happens.
     */
    @Transactional
    public ARPaymentDto createAndCompletePayment(CreateARPaymentRequest request) {
        ARPaymentDto created = createPayment(request);
        return completePayment(created.getId());
    }

    @Transactional
    public ARPaymentDto addAllocation(Long paymentId, CreateARPaymentAllocationRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ARPayment payment = arPaymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Payment not found with id: " + paymentId));

        if (payment.getStatus() != ARPaymentStatus.PENDING) {
            throw new BusinessException("Cannot add allocations to non-pending payments");
        }

        if (request.getAllocatedAmount().compareTo(payment.getUnallocatedAmount()) > 0) {
            throw new BusinessException("Allocation amount exceeds unallocated balance");
        }

        ARPaymentAllocation allocation = createAllocation(payment, request);
        arPaymentAllocationRepository.save(allocation);

        // Update payment amounts
        payment.setAllocatedAmount(payment.getAllocatedAmount().add(request.getAllocatedAmount()));
        payment.setUnallocatedAmount(payment.getUnallocatedAmount().subtract(request.getAllocatedAmount()));
        payment = arPaymentRepository.save(payment);

        log.info("Added allocation to payment: {}", payment.getPaymentNumber());

        return arPaymentMapper.toDto(payment);
    }

    @Transactional
    public ARPaymentDto removeAllocation(Long paymentId, Long allocationId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ARPayment payment = arPaymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Payment not found with id: " + paymentId));

        if (payment.getStatus() != ARPaymentStatus.PENDING) {
            throw new BusinessException("Cannot remove allocations from non-pending payments");
        }

        ARPaymentAllocation allocation = arPaymentAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new NotFoundException("Allocation not found with id: " + allocationId));

        if (!allocation.getArPayment().getId().equals(paymentId)) {
            throw new BusinessException("Allocation does not belong to this payment");
        }

        // Update payment amounts
        payment.setAllocatedAmount(payment.getAllocatedAmount().subtract(allocation.getAllocatedAmount()));
        payment.setUnallocatedAmount(payment.getUnallocatedAmount().add(allocation.getAllocatedAmount()));

        arPaymentAllocationRepository.delete(allocation);
        payment = arPaymentRepository.save(payment);

        log.info("Removed allocation from payment: {}", payment.getPaymentNumber());

        return arPaymentMapper.toDto(payment);
    }

    @Transactional
    public ARPaymentDto cancelPayment(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ARPayment payment = arPaymentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Payment not found with id: " + id));

        if (payment.getStatus() == ARPaymentStatus.CANCELLED) {
            throw new BusinessException("Payment is already cancelled");
        }

        if (payment.getStatus() == ARPaymentStatus.COMPLETED) {
            // Reverse GL entries
            glIntegrationService.reverseARPayment(payment, reason);

            // Reverse allocations from invoices
            if (payment.getAllocations() != null) {
                for (ARPaymentAllocation allocation : payment.getAllocations()) {
                    if (allocation.getArInvoice() != null) {
                        arInvoiceService.applyPayment(
                                allocation.getArInvoice(),
                                allocation.getAllocatedAmount().negate(),
                                allocation.getDiscountTaken() != null ? allocation.getDiscountTaken().negate() : BigDecimal.ZERO,
                                allocation.getWriteOffAmount() != null ? allocation.getWriteOffAmount().negate() : BigDecimal.ZERO
                        );
                    }
                }
            }

            // Reverse customer balance
            customerService.updateCustomerBalance(payment.getCustomer(), payment.getPaymentAmount());
        }

        payment.setStatus(ARPaymentStatus.CANCELLED);
        payment.setNotes(payment.getNotes() != null ? payment.getNotes() + "\nCancelled: " + reason : "Cancelled: " + reason);
        payment = arPaymentRepository.save(payment);

        log.info("Cancelled AR payment: {}", payment.getPaymentNumber());

        return arPaymentMapper.toDto(payment);
    }

    @Transactional
    public ARPaymentDto markAsDeposited(Long id, String bankReference) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ARPayment payment = arPaymentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Payment not found with id: " + id));

        if (payment.getStatus() != ARPaymentStatus.COMPLETED) {
            throw new BusinessException("Only completed payments can be marked as deposited");
        }

        payment.setDeposited(true);
        payment.setDepositReference(bankReference);
        payment = arPaymentRepository.save(payment);

        log.info("Marked payment {} as deposited with reference: {}", payment.getPaymentNumber(), bankReference);

        return arPaymentMapper.toDto(payment);
    }

    @Transactional(readOnly = true)
    public List<ARPaymentDto> getUndepositedPayments() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ARPayment> payments = arPaymentRepository.findByTenantIdAndDepositedFalseAndStatus(
                tenantId, ARPaymentStatus.COMPLETED);
        return arPaymentMapper.toDtoList(payments);
    }

    private ARPaymentAllocation createAllocation(ARPayment payment, CreateARPaymentAllocationRequest request) {
        ARPaymentAllocation allocation = new ARPaymentAllocation();
        allocation.setArPayment(payment);
        allocation.setAllocatedAmount(request.getAllocatedAmount());
        allocation.setDiscountTaken(request.getDiscountTaken() != null ? request.getDiscountTaken() : BigDecimal.ZERO);
        allocation.setWriteOffAmount(request.getWriteOffAmount() != null ? request.getWriteOffAmount() : BigDecimal.ZERO);

        if (request.getArInvoiceId() != null) {
            ARInvoice invoice = arInvoiceRepository.findByIdAndTenantId(request.getArInvoiceId(), payment.getTenantId())
                    .orElseThrow(() -> new NotFoundException("AR Invoice not found with id: " + request.getArInvoiceId()));

            // Validate allocation doesn't exceed balance
            BigDecimal totalApplied = request.getAllocatedAmount()
                    .add(request.getDiscountTaken() != null ? request.getDiscountTaken() : BigDecimal.ZERO)
                    .add(request.getWriteOffAmount() != null ? request.getWriteOffAmount() : BigDecimal.ZERO);

            if (totalApplied.compareTo(invoice.getBalanceDue()) > 0) {
                throw new BusinessException("Allocation exceeds invoice balance due");
            }

            allocation.setArInvoice(invoice);
        } else if (request.getCreditNoteId() != null) {
            CreditNote creditNote = creditNoteRepository.findByIdAndTenantId(request.getCreditNoteId(), payment.getTenantId())
                    .orElseThrow(() -> new NotFoundException("Credit Note not found with id: " + request.getCreditNoteId()));
            allocation.setCreditNote(creditNote);
        }

        return allocation;
    }

    private String generatePaymentNumber(Long tenantId) {
        Integer maxNumber = arPaymentRepository.findMaxPaymentNumber(tenantId);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("REC-%06d", nextNumber);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return arPaymentRepository.sumPaymentsByDateRangeAndStatus(tenantId, startDate, endDate, ARPaymentStatus.COMPLETED);
    }
}
