package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.entity.ARInvoiceLine;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.mapper.ARInvoiceMapper;
import com.hisobnoma.platform.finance.repository.ARInvoiceLineRepository;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.pos.entity.POSPayment;
import com.hisobnoma.platform.pos.entity.POSPaymentStatus;
import com.hisobnoma.platform.pos.entity.POSPaymentType;
import com.hisobnoma.platform.pos.entity.POSTransaction;
import com.hisobnoma.platform.pos.entity.POSTransactionLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ARInvoiceService {

    private final ARInvoiceRepository arInvoiceRepository;
    private final ARInvoiceLineRepository arInvoiceLineRepository;
    private final CustomerRepository customerRepository;
    private final ARInvoiceMapper arInvoiceMapper;
    private final CustomerService customerService;
    private final GLIntegrationService glIntegrationService;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<ARInvoiceDto> getInvoices(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<ARInvoice> page = arInvoiceRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(arInvoiceMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<ARInvoiceDto> getInvoicesByCustomer(Long customerId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<ARInvoice> page = arInvoiceRepository.findByTenantIdAndCustomer_Id(tenantId, customerId, pageable);
        return PageResponse.of(page.map(arInvoiceMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<ARInvoiceDto> getInvoicesByStatus(ARInvoiceStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<ARInvoice> page = arInvoiceRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return PageResponse.of(page.map(arInvoiceMapper::toDto));
    }

    @Transactional(readOnly = true)
    public ARInvoiceDto getInvoice(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        ARInvoice invoice = arInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Invoice not found with id: " + id));
        return arInvoiceMapper.toDto(invoice);
    }

    @Transactional(readOnly = true)
    public ARInvoiceDto getInvoiceByNumber(String invoiceNumber) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        ARInvoice invoice = arInvoiceRepository.findByInvoiceNumberAndTenantId(invoiceNumber, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Invoice not found with number: " + invoiceNumber));
        return arInvoiceMapper.toDto(invoice);
    }

    @Transactional(readOnly = true)
    public List<ARInvoiceDto> getUnpaidInvoicesByCustomer(Long customerId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ARInvoiceStatus> unpaidStatuses = List.of(
                ARInvoiceStatus.PENDING, ARInvoiceStatus.SENT,
                ARInvoiceStatus.PARTIAL, ARInvoiceStatus.OVERDUE
        );
        List<ARInvoice> invoices = arInvoiceRepository.findUnpaidByCustomer(tenantId, customerId, unpaidStatuses);
        return invoices.stream().map(arInvoiceMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ARInvoiceDto> getOverdueInvoices() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ARInvoiceStatus> overdueStatuses = List.of(ARInvoiceStatus.PENDING, ARInvoiceStatus.SENT, ARInvoiceStatus.PARTIAL);
        List<ARInvoice> invoices = arInvoiceRepository.findOverdueInvoices(tenantId, LocalDate.now(), overdueStatuses);
        return arInvoiceMapper.toDtoList(invoices);
    }

    @Transactional
    public ARInvoiceDto createInvoice(CreateARInvoiceRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Validate customer exists
        Customer customer = customerRepository.findByIdAndTenantId(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + request.getCustomerId()));

        // Check credit limit if applicable
        if (!customerService.canBeInvoiced(customer.getId(), request.getTotalAmount())) {
            throw new BusinessException("Customer has exceeded credit limit or is on credit hold");
        }

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber(tenantId);

        ARInvoice invoice = arInvoiceMapper.toEntity(request);
        invoice.setTenantId(tenantId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCustomer(customer);
        invoice.setStatus(ARInvoiceStatus.DRAFT);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setBalanceDue(request.getTotalAmount());

        // Calculate due date from payment terms
        if (request.getPaymentTerms() != null) {
            invoice.setDueDate(request.getInvoiceDate().plusDays(request.getPaymentTerms()));
        }

        // Handle lines
        if (request.getLines() != null && !request.getLines().isEmpty()) {
            List<ARInvoiceLine> lines = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            int lineNumber = 1;

            for (var lineRequest : request.getLines()) {
                ARInvoiceLine line = new ARInvoiceLine();
                line.setArInvoice(invoice);
                line.setLineNumber(lineNumber++);
                line.setItemId(lineRequest.getItemId());
                line.setDescription(lineRequest.getDescription());
                line.setQuantity(lineRequest.getQuantity());
                line.setUnitPrice(lineRequest.getUnitPrice());
                line.setUnitCost(lineRequest.getUnitCost());
                line.setDiscountPercent(lineRequest.getDiscountPercent() != null ? lineRequest.getDiscountPercent() : BigDecimal.ZERO);
                line.setTaxAmount(lineRequest.getTaxAmount() != null ? lineRequest.getTaxAmount() : BigDecimal.ZERO);

                // Calculate line total
                BigDecimal lineAmount = lineRequest.getQuantity().multiply(lineRequest.getUnitPrice());
                if (line.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discount = lineAmount.multiply(line.getDiscountPercent()).divide(BigDecimal.valueOf(100));
                    lineAmount = lineAmount.subtract(discount);
                }
                lineAmount = lineAmount.add(line.getTaxAmount());
                line.setLineTotal(lineAmount);

                // Calculate profit margin
                if (lineRequest.getUnitCost() != null && lineRequest.getUnitCost().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal profit = lineRequest.getUnitPrice().subtract(lineRequest.getUnitCost());
                    line.setProfitMargin(profit.divide(lineRequest.getUnitPrice(), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)));
                }

                lines.add(line);
                totalAmount = totalAmount.add(lineAmount);
            }

            invoice.setLines(lines);
            invoice.setSubtotal(totalAmount.subtract(invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO));
            invoice.setTotalAmount(totalAmount);
            invoice.setBalanceDue(totalAmount);
        }

        invoice = arInvoiceRepository.save(invoice);
        log.info("Created AR invoice: {} for customer: {}", invoiceNumber, customer.getCode());

        return arInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public ARInvoiceDto updateInvoice(Long id, CreateARInvoiceRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ARInvoice invoice = arInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Invoice not found with id: " + id));

        if (invoice.getStatus() != ARInvoiceStatus.DRAFT && invoice.getStatus() != ARInvoiceStatus.PENDING) {
            throw new BusinessException("Only draft or pending invoices can be updated");
        }

        arInvoiceMapper.updateEntity(request, invoice);

        // Recalculate totals if lines are updated
        if (request.getLines() != null) {
            // Delete existing lines
            arInvoiceLineRepository.deleteByArInvoiceId(id);

            List<ARInvoiceLine> lines = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            int lineNumber = 1;

            for (var lineRequest : request.getLines()) {
                ARInvoiceLine line = new ARInvoiceLine();
                line.setArInvoice(invoice);
                line.setLineNumber(lineNumber++);
                line.setItemId(lineRequest.getItemId());
                line.setDescription(lineRequest.getDescription());
                line.setQuantity(lineRequest.getQuantity());
                line.setUnitPrice(lineRequest.getUnitPrice());
                line.setUnitCost(lineRequest.getUnitCost());
                line.setDiscountPercent(lineRequest.getDiscountPercent() != null ? lineRequest.getDiscountPercent() : BigDecimal.ZERO);
                line.setTaxAmount(lineRequest.getTaxAmount() != null ? lineRequest.getTaxAmount() : BigDecimal.ZERO);

                BigDecimal lineAmount = lineRequest.getQuantity().multiply(lineRequest.getUnitPrice());
                if (line.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discount = lineAmount.multiply(line.getDiscountPercent()).divide(BigDecimal.valueOf(100));
                    lineAmount = lineAmount.subtract(discount);
                }
                lineAmount = lineAmount.add(line.getTaxAmount());
                line.setLineTotal(lineAmount);

                lines.add(line);
                totalAmount = totalAmount.add(lineAmount);
            }

            invoice.setLines(lines);
            invoice.setTotalAmount(totalAmount);
            invoice.setBalanceDue(totalAmount.subtract(invoice.getPaidAmount()));
        }

        invoice = arInvoiceRepository.save(invoice);
        log.info("Updated AR invoice: {}", invoice.getInvoiceNumber());

        return arInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public ARInvoiceDto postInvoice(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ARInvoice invoice = arInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Invoice not found with id: " + id));

        if (invoice.getStatus() != ARInvoiceStatus.DRAFT) {
            throw new BusinessException("Only draft invoices can be posted");
        }

        // Post to GL
        glIntegrationService.postARInvoice(invoice);

        invoice.setStatus(ARInvoiceStatus.PENDING);
        invoice = arInvoiceRepository.save(invoice);

        // Update customer balance
        customerService.updateCustomerBalance(invoice.getCustomer().getId(), invoice.getTotalAmount());

        log.info("Posted AR invoice: {} to GL", invoice.getInvoiceNumber());

        return arInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public ARInvoiceDto sendInvoice(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ARInvoice invoice = arInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Invoice not found with id: " + id));

        if (invoice.getStatus() != ARInvoiceStatus.PENDING) {
            throw new BusinessException("Only pending invoices can be sent");
        }

        invoice.setStatus(ARInvoiceStatus.SENT);
        invoice = arInvoiceRepository.save(invoice);

        log.info("Marked AR invoice as sent: {}", invoice.getInvoiceNumber());

        return arInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public ARInvoiceDto cancelInvoice(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ARInvoice invoice = arInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("AR Invoice not found with id: " + id));

        if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Cannot cancel invoice with payments. Create a credit note instead.");
        }

        if (invoice.getStatus() == ARInvoiceStatus.CANCELLED) {
            throw new BusinessException("Invoice is already cancelled");
        }

        // Reverse GL entries if posted
        if (invoice.getStatus() != ARInvoiceStatus.DRAFT) {
            glIntegrationService.reverseARInvoice(invoice, reason);

            // Update customer balance
            customerService.updateCustomerBalance(invoice.getCustomer().getId(), invoice.getTotalAmount().negate());
        }

        invoice.setStatus(ARInvoiceStatus.CANCELLED);
        invoice.setNotes(invoice.getNotes() != null ? invoice.getNotes() + "\nCancelled: " + reason : "Cancelled: " + reason);
        invoice = arInvoiceRepository.save(invoice);

        log.info("Cancelled AR invoice: {}", invoice.getInvoiceNumber());

        return arInvoiceMapper.toDto(invoice);
    }

    @Transactional
    public void applyPayment(Long invoiceId, BigDecimal amount, BigDecimal discountTaken, BigDecimal writeOff) {
        ARInvoice invoice = arInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("AR Invoice not found with id: " + invoiceId));

        BigDecimal totalApplied = amount.add(discountTaken != null ? discountTaken : BigDecimal.ZERO)
                .add(writeOff != null ? writeOff : BigDecimal.ZERO);

        BigDecimal newPaidAmount = invoice.getPaidAmount().add(totalApplied);
        BigDecimal newBalance = invoice.getTotalAmount().subtract(newPaidAmount);

        invoice.setPaidAmount(newPaidAmount);
        invoice.setBalanceDue(newBalance);

        // Update status based on balance
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(ARInvoiceStatus.PAID);
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(ARInvoiceStatus.PARTIAL);
        }

        arInvoiceRepository.save(invoice);
    }

    @Transactional
    public void checkOverdueInvoices() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ARInvoiceStatus> statuses = List.of(ARInvoiceStatus.PENDING, ARInvoiceStatus.SENT, ARInvoiceStatus.PARTIAL);

        List<ARInvoice> overdueInvoices = arInvoiceRepository.findByTenantIdAndDueDateBeforeAndStatusIn(
                tenantId, LocalDate.now(), statuses);

        for (ARInvoice invoice : overdueInvoices) {
            invoice.setStatus(ARInvoiceStatus.OVERDUE);
            arInvoiceRepository.save(invoice);
            log.info("Marked invoice {} as overdue", invoice.getInvoiceNumber());
        }
    }

    private String generateInvoiceNumber(Long tenantId) {
        Integer maxNumber = arInvoiceRepository.findMaxInvoiceNumber(tenantId);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("INV-%06d", nextNumber);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCustomerTotalOutstanding(Long customerId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ARInvoiceStatus> unpaidStatuses = List.of(
                ARInvoiceStatus.PENDING, ARInvoiceStatus.SENT,
                ARInvoiceStatus.PARTIAL, ARInvoiceStatus.OVERDUE
        );
        return arInvoiceRepository.sumBalanceDueByCustomer(tenantId, customerId, unpaidStatuses);
    }

    /**
     * Create AR Invoice from a POS transaction with credit payment.
     * This is called automatically when a POS transaction is completed with CREDIT payment type.
     *
     * @param transaction The POS transaction with credit payment
     * @return The created AR Invoice DTO
     */
    @Transactional
    public ARInvoiceDto createFromPOSTransaction(POSTransaction transaction) {
        if (transaction.getCustomer() == null) {
            throw new BusinessException("Credit sales require a customer to be associated with the transaction");
        }

        if (transaction.getArInvoiceId() != null) {
            throw new BusinessException("AR Invoice already created for this transaction");
        }

        // Calculate total credit amount from payments
        // Note: payments collection was already initialized in the calling transaction
        BigDecimal creditAmount = transaction.getPayments().stream()
                .filter(p -> p.getPaymentType() == POSPaymentType.CREDIT)
                .filter(p -> p.getStatus() == POSPaymentStatus.APPROVED)
                .map(POSPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (creditAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("No credit payment found in transaction");
        }

        Long tenantId = transaction.getTenantId();
        Customer customer = transaction.getCustomer();

        // Check credit limit
        if (!customerService.canBeInvoiced(customer.getId(), creditAmount)) {
            throw new BusinessException("Customer has exceeded credit limit or is on credit hold");
        }

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber(tenantId);

        // Calculate due date from customer payment terms
        Integer paymentTermsDays = customer.getPaymentTermsDays() != null ? customer.getPaymentTermsDays() : 30;
        LocalDate dueDate = LocalDate.now().plusDays(paymentTermsDays);

        ARInvoice invoice = ARInvoice.builder()
                .tenantId(tenantId)
                .invoiceNumber(invoiceNumber)
                .customer(customer)
                .invoiceDate(LocalDate.now())
                .dueDate(dueDate)
                .status(ARInvoiceStatus.PENDING)
                .subtotal(transaction.getSubtotal())
                .discountAmount(transaction.getDiscountAmount())
                .taxAmount(transaction.getTaxAmount())
                .totalAmount(creditAmount) // Only the credit portion
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(creditAmount)
                .currency(transaction.getCurrency())
                .posTransactionId(transaction.getId())
                .posTransactionNumber(transaction.getTransactionNumber())
                .notes("Auto-created from POS Transaction: " + transaction.getTransactionNumber())
                .build();

        invoice = arInvoiceRepository.save(invoice);

        // Create lines from transaction lines
        List<ARInvoiceLine> lines = new ArrayList<>();
        BigDecimal transactionTotal = transaction.getTotalAmount();
        BigDecimal creditRatio = creditAmount.divide(transactionTotal, 6, RoundingMode.HALF_UP);
        int lineNumber = 1;

        for (POSTransactionLine txLine : transaction.getLines()) {
            // Calculate proportional amount for this line based on credit ratio
            BigDecimal lineAmount = txLine.getLineTotal().multiply(creditRatio).setScale(4, RoundingMode.HALF_UP);

            ARInvoiceLine line = new ARInvoiceLine();
            line.setArInvoice(invoice);
            line.setLineNumber(lineNumber++);
            line.setItemId(txLine.getProduct() != null ? txLine.getProduct().getId() : null);
            line.setProductId(txLine.getProduct() != null ? txLine.getProduct().getId() : null);
            line.setProductSku(txLine.getProductCode());
            line.setProductName(txLine.getProductName());
            line.setDescription(txLine.getProductName() + (txLine.getVariantName() != null ? " - " + txLine.getVariantName() : ""));
            line.setQuantity(txLine.getQuantity());
            line.setUnitPrice(txLine.getUnitPrice());
            line.setUnitCost(txLine.getCostPrice());
            line.setDiscountPercent(txLine.getDiscountPercent() != null ? txLine.getDiscountPercent() : BigDecimal.ZERO);
            line.setTaxAmount(txLine.getTaxAmount() != null ?
                    txLine.getTaxAmount().multiply(creditRatio).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            line.setLineTotal(lineAmount);

            // Calculate profit margin
            if (txLine.getCostPrice() != null && txLine.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal profit = txLine.getUnitPrice().subtract(txLine.getCostPrice());
                line.setProfitMargin(profit.divide(txLine.getUnitPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
            }

            lines.add(line);
        }

        invoice.setLines(lines);
        invoice = arInvoiceRepository.save(invoice);

        // NOTE: Do NOT post AR invoice to GL here.
        // The POS transaction GL entry (postPOSTransaction) already debits Accounts Receivable
        // for the credit portion and credits Sales Revenue for the full amount.
        // Posting the AR invoice to GL again would double-count revenue, COGS, and AR.

        // Update customer balance
        customerService.updateCustomerBalance(customer.getId(), creditAmount);

        log.info("Created AR Invoice {} from POS Transaction {} for customer {}",
                invoice.getInvoiceNumber(), transaction.getTransactionNumber(), customer.getCode());

        return arInvoiceMapper.toDto(invoice);
    }
}
