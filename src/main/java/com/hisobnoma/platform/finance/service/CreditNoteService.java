package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateCreditNoteRequest;
import com.hisobnoma.platform.finance.dto.CreditNoteDto;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.CreditNoteMapper;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.CreditNoteRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final ARInvoiceRepository arInvoiceRepository;
    private final CustomerRepository customerRepository;
    private final CreditNoteMapper creditNoteMapper;
    private final CustomerService customerService;
    private final GLIntegrationService glIntegrationService;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<CreditNoteDto> getCreditNotes(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<CreditNote> page = creditNoteRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(creditNoteMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<CreditNoteDto> getCreditNotesByCustomer(Long customerId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<CreditNote> page = creditNoteRepository.findByTenantIdAndCustomer_Id(tenantId, customerId, pageable);
        return PageResponse.of(page.map(creditNoteMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<CreditNoteDto> getCreditNotesByStatus(CreditNoteStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<CreditNote> page = creditNoteRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return PageResponse.of(page.map(creditNoteMapper::toDto));
    }

    @Transactional(readOnly = true)
    public CreditNoteDto getCreditNote(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        CreditNote creditNote = creditNoteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Credit Note not found with id: " + id));
        return creditNoteMapper.toDto(creditNote);
    }

    @Transactional(readOnly = true)
    public CreditNoteDto getCreditNoteByNumber(String creditNoteNumber) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        CreditNote creditNote = creditNoteRepository.findByCreditNoteNumberAndTenantId(creditNoteNumber, tenantId)
                .orElseThrow(() -> new NotFoundException("Credit Note not found with number: " + creditNoteNumber));
        return creditNoteMapper.toDto(creditNote);
    }

    @Transactional(readOnly = true)
    public List<CreditNoteDto> getAvailableCreditNotes(Long customerId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<CreditNoteStatus> statuses = List.of(CreditNoteStatus.APPROVED, CreditNoteStatus.PARTIAL);
        List<CreditNote> creditNotes = creditNoteRepository.findAvailableCreditNotesByCustomer(
                tenantId, customerId, statuses);
        return creditNoteMapper.toDtoList(creditNotes);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableCredit(Long customerId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<CreditNoteStatus> statuses = List.of(CreditNoteStatus.APPROVED, CreditNoteStatus.PARTIAL);
        return creditNoteRepository.sumAvailableCreditByCustomer(tenantId, customerId, statuses);
    }

    @Transactional
    public CreditNoteDto createCreditNote(CreateCreditNoteRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Validate customer exists
        Customer customer = customerRepository.findByIdAndTenantId(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + request.getCustomerId()));

        // Validate original invoice if provided
        ARInvoice originalInvoice = null;
        if (request.getOriginalInvoiceId() != null) {
            originalInvoice = arInvoiceRepository.findByIdAndTenantId(request.getOriginalInvoiceId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Original invoice not found with id: " + request.getOriginalInvoiceId()));

            // Validate credit note amount doesn't exceed invoice amount
            BigDecimal existingCredits = getCreditNoteTotalForInvoice(request.getOriginalInvoiceId());
            BigDecimal remainingAmount = originalInvoice.getTotalAmount().subtract(existingCredits);

            if (request.getCreditAmount().compareTo(remainingAmount) > 0) {
                throw new BusinessException("Credit note amount exceeds remaining invoice amount. Maximum: " + remainingAmount);
            }
        }

        // Generate credit note number
        String creditNoteNumber = generateCreditNoteNumber(tenantId);

        CreditNote creditNote = creditNoteMapper.toEntity(request);
        creditNote.setTenantId(tenantId);
        creditNote.setCreditNoteNumber(creditNoteNumber);
        creditNote.setCustomer(customer);
        creditNote.setOriginalInvoice(originalInvoice);
        creditNote.setStatus(CreditNoteStatus.DRAFT);
        creditNote.setBalance(request.getCreditAmount());
        creditNote.setAppliedAmount(BigDecimal.ZERO);

        creditNote = creditNoteRepository.save(creditNote);
        log.info("Created credit note: {} for customer: {}", creditNoteNumber, customer.getCode());

        return creditNoteMapper.toDto(creditNote);
    }

    @Transactional
    public CreditNoteDto updateCreditNote(Long id, CreateCreditNoteRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        CreditNote creditNote = creditNoteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Credit Note not found with id: " + id));

        if (creditNote.getStatus() != CreditNoteStatus.DRAFT) {
            throw new BusinessException("Only draft credit notes can be updated");
        }

        // Validate new amount if original invoice exists
        if (creditNote.getOriginalInvoice() != null && request.getCreditAmount() != null) {
            BigDecimal existingCredits = getCreditNoteTotalForInvoice(creditNote.getOriginalInvoice().getId())
                    .subtract(creditNote.getCreditAmount());
            BigDecimal remainingAmount = creditNote.getOriginalInvoice().getTotalAmount().subtract(existingCredits);

            if (request.getCreditAmount().compareTo(remainingAmount) > 0) {
                throw new BusinessException("Credit note amount exceeds remaining invoice amount. Maximum: " + remainingAmount);
            }
        }

        creditNoteMapper.updateEntity(request, creditNote);
        creditNote.setBalance(request.getCreditAmount());

        creditNote = creditNoteRepository.save(creditNote);
        log.info("Updated credit note: {}", creditNote.getCreditNoteNumber());

        return creditNoteMapper.toDto(creditNote);
    }

    @Transactional
    public CreditNoteDto approveCreditNote(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        CreditNote creditNote = creditNoteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Credit Note not found with id: " + id));

        if (creditNote.getStatus() != CreditNoteStatus.DRAFT) {
            throw new BusinessException("Only draft credit notes can be approved");
        }

        // Post to GL
        glIntegrationService.postCreditNote(creditNote);

        // Update customer balance (reduce what they owe)
        customerService.updateCustomerBalance(creditNote.getCustomer().getId(), creditNote.getCreditAmount().negate());

        creditNote.setStatus(CreditNoteStatus.APPROVED);
        creditNote = creditNoteRepository.save(creditNote);

        log.info("Approved credit note: {}", creditNote.getCreditNoteNumber());

        return creditNoteMapper.toDto(creditNote);
    }

    @Transactional
    public CreditNoteDto applyToInvoice(Long creditNoteId, Long invoiceId, BigDecimal amount) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        CreditNote creditNote = creditNoteRepository.findByIdAndTenantId(creditNoteId, tenantId)
                .orElseThrow(() -> new NotFoundException("Credit Note not found with id: " + creditNoteId));

        if (creditNote.getStatus() != CreditNoteStatus.APPROVED && creditNote.getStatus() != CreditNoteStatus.PARTIAL) {
            throw new BusinessException("Only approved credit notes can be applied to invoices");
        }

        if (amount.compareTo(creditNote.getBalance()) > 0) {
            throw new BusinessException("Amount exceeds credit note balance. Available: " + creditNote.getBalance());
        }

        ARInvoice invoice = arInvoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Invoice not found with id: " + invoiceId));

        // Validate same customer
        if (!creditNote.getCustomer().getId().equals(invoice.getCustomer().getId())) {
            throw new BusinessException("Credit note and invoice must belong to the same customer");
        }

        if (amount.compareTo(invoice.getBalanceDue()) > 0) {
            throw new BusinessException("Amount exceeds invoice balance due. Balance: " + invoice.getBalanceDue());
        }

        // Apply credit to invoice
        invoice.setPaidAmount(invoice.getPaidAmount().add(amount));
        invoice.setBalanceDue(invoice.getBalanceDue().subtract(amount));

        if (invoice.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(ARInvoiceStatus.PAID);
        } else {
            invoice.setStatus(ARInvoiceStatus.PARTIAL);
        }

        arInvoiceRepository.save(invoice);

        // Update credit note
        creditNote.setAppliedAmount(creditNote.getAppliedAmount().add(amount));
        creditNote.setBalance(creditNote.getBalance().subtract(amount));

        if (creditNote.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            creditNote.setStatus(CreditNoteStatus.APPLIED);
        } else {
            creditNote.setStatus(CreditNoteStatus.PARTIAL);
        }

        creditNote = creditNoteRepository.save(creditNote);

        log.info("Applied {} from credit note {} to invoice {}", amount,
                creditNote.getCreditNoteNumber(), invoice.getInvoiceNumber());

        return creditNoteMapper.toDto(creditNote);
    }

    @Transactional
    public CreditNoteDto cancelCreditNote(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        CreditNote creditNote = creditNoteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Credit Note not found with id: " + id));

        if (creditNote.getStatus() == CreditNoteStatus.CANCELLED) {
            throw new BusinessException("Credit note is already cancelled");
        }

        if (creditNote.getAppliedAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Cannot cancel credit note that has been applied to invoices");
        }

        // Reverse GL entries if approved
        if (creditNote.getStatus() == CreditNoteStatus.APPROVED) {
            glIntegrationService.reverseCreditNote(creditNote, reason);

            // Reverse customer balance
            customerService.updateCustomerBalance(creditNote.getCustomer().getId(), creditNote.getCreditAmount());
        }

        creditNote.setStatus(CreditNoteStatus.CANCELLED);
        creditNote.setNotes(creditNote.getNotes() != null ? creditNote.getNotes() + "\nCancelled: " + reason : "Cancelled: " + reason);
        creditNote = creditNoteRepository.save(creditNote);

        log.info("Cancelled credit note: {}", creditNote.getCreditNoteNumber());

        return creditNoteMapper.toDto(creditNote);
    }

    private BigDecimal getCreditNoteTotalForInvoice(Long invoiceId) {
        List<CreditNote> creditNotes = creditNoteRepository.findByOriginalInvoice_Id(invoiceId);
        return creditNotes.stream()
                .filter(cn -> cn.getStatus() != CreditNoteStatus.CANCELLED)
                .map(CreditNote::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateCreditNoteNumber(Long tenantId) {
        Integer maxNumber = creditNoteRepository.findMaxCreditNoteNumber(tenantId);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("CN-%06d", nextNumber);
    }
}
