package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.finance.dto.CreateJournalEntryRequest;
import com.hisobnoma.platform.finance.dto.CreateJournalLineRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Integration service for posting journal entries from other modules.
 * This service provides methods for inventory, POS, AR, and AP modules to create GL entries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GLIntegrationService {

    private final JournalEntryService journalEntryService;
    private final SecurityContextHelper securityContextHelper;
    private final AccountRepository accountRepository;

    // Account codes - these should be configurable per tenant
    // For now, these are placeholder values that should be set up in the chart of accounts
    private static final String INVENTORY_ACCOUNT = "1300";  // Asset: Inventory
    private static final String COGS_ACCOUNT = "5100";       // Expense: Cost of Goods Sold
    private static final String SALES_REVENUE_ACCOUNT = "4100"; // Revenue: Sales Revenue
    private static final String CASH_ACCOUNT = "1100";       // Asset: Cash
    private static final String ACCOUNTS_RECEIVABLE = "1200"; // Asset: Accounts Receivable
    private static final String ACCOUNTS_PAYABLE = "2100";   // Liability: Accounts Payable
    private static final String PURCHASE_EXPENSE = "5200";   // Expense: Purchases
    private static final String SALES_DISCOUNTS_ACCOUNT = "4200"; // Contra-Revenue: Sales Discounts

    /**
     * Posts an inventory movement to the general ledger.
     * This is called when stock is received, adjusted, or transferred.
     *
     * @param movementType The type of movement (RECEIVING, ADJUSTMENT, TRANSFER)
     * @param referenceId The ID of the source document (e.g., receiving order ID)
     * @param referenceNumber The document number
     * @param amount The total value of the movement
     * @param description Description of the movement
     * @param movementDate The date of the movement
     * @return The created journal entry
     */
    @Transactional
    public JournalEntry postInventoryMovement(
            String movementType,
            Long referenceId,
            String referenceNumber,
            BigDecimal amount,
            String description,
            LocalDate movementDate
    ) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        switch (movementType) {
            case "RECEIVING" -> {
                // Debit Inventory, Credit AP
                lines.add(createLine(INVENTORY_ACCOUNT, amount, null, "Inventory received"));
                lines.add(createLine(ACCOUNTS_PAYABLE, null, amount, "Payable for goods received"));
            }
            case "ADJUSTMENT_INCREASE" -> {
                // Debit Inventory, Credit COGS (reversal)
                lines.add(createLine(INVENTORY_ACCOUNT, amount, null, "Inventory adjustment increase"));
                lines.add(createLine(COGS_ACCOUNT, null, amount, "COGS adjustment"));
            }
            case "ADJUSTMENT_DECREASE" -> {
                // Debit COGS, Credit Inventory
                lines.add(createLine(COGS_ACCOUNT, amount, null, "COGS from inventory adjustment"));
                lines.add(createLine(INVENTORY_ACCOUNT, null, amount, "Inventory adjustment decrease"));
            }
            default -> throw new BusinessException("Unknown inventory movement type: " + movementType);
        }

        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(movementDate)
                .description(description)
                .source(JournalSource.INVENTORY_ADJUSTMENT)
                .referenceType(movementType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .lines(lines)
                .build();

        log.info("Posting inventory movement to GL: {} - {} - {}", movementType, referenceNumber, amount);
        return journalEntryService.createAndPostEntry(request, tenantId);
    }

    /**
     * Posts a sales transaction to the general ledger.
     * This is called when a POS sale or web order is completed.
     *
     * @param referenceId The transaction/order ID
     * @param referenceNumber The transaction/order number
     * @param salesAmount The total sales amount
     * @param costAmount The cost of goods sold
     * @param paymentType The payment method (CASH, CARD, CREDIT)
     * @param description Description of the sale
     * @param saleDate The date of the sale
     * @return The created journal entry
     */
    @Transactional
    public JournalEntry postSalesTransaction(
            Long referenceId,
            String referenceNumber,
            BigDecimal salesAmount,
            BigDecimal costAmount,
            String paymentType,
            String description,
            LocalDate saleDate
    ) {
        return postSalesTransaction(referenceId, referenceNumber, salesAmount, costAmount,
                BigDecimal.ZERO, paymentType, description, saleDate);
    }

    @Transactional
    public JournalEntry postSalesTransaction(
            Long referenceId,
            String referenceNumber,
            BigDecimal salesAmount,
            BigDecimal costAmount,
            BigDecimal discountAmount,
            String paymentType,
            String description,
            LocalDate saleDate
    ) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Revenue entry: Debit Cash/AR, Credit Sales Revenue
        // Cash/AR is debited for the net amount (what customer actually pays)
        String receivableAccount = "CASH".equals(paymentType) ? CASH_ACCOUNT : ACCOUNTS_RECEIVABLE;
        lines.add(createLine(receivableAccount, salesAmount, null, "Payment for sale"));

        // If discount was given, record gross revenue and debit discount account
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal grossRevenue = salesAmount.add(discountAmount);
            lines.add(createLine(SALES_REVENUE_ACCOUNT, null, grossRevenue, "Sales revenue (gross)"));
            lines.add(createLine(SALES_DISCOUNTS_ACCOUNT, discountAmount, null, "Sales discount given"));
        } else {
            lines.add(createLine(SALES_REVENUE_ACCOUNT, null, salesAmount, "Sales revenue"));
        }

        // COGS entry if cost is provided: Debit COGS, Credit Inventory
        if (costAmount != null && costAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(createLine(COGS_ACCOUNT, costAmount, null, "Cost of goods sold"));
            lines.add(createLine(INVENTORY_ACCOUNT, null, costAmount, "Inventory reduction"));
        }

        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(saleDate)
                .description(description)
                .source(JournalSource.POS_SALE)
                .referenceType("SALE")
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .lines(lines)
                .build();

        log.info("Posting sales transaction to GL: {} - {} - {} (discount: {})",
                referenceNumber, salesAmount, costAmount, discountAmount);
        return journalEntryService.createAndPostEntry(request, tenantId);
    }

    /**
     * Posts a purchase invoice to the general ledger.
     * This is called when an AP invoice is approved.
     *
     * @param referenceId The invoice ID
     * @param referenceNumber The invoice number
     * @param amount The invoice total
     * @param vendorName The vendor name
     * @param invoiceDate The invoice date
     * @return The created journal entry
     */
    @Transactional
    public JournalEntry postPurchaseInvoice(
            Long referenceId,
            String referenceNumber,
            BigDecimal amount,
            String vendorName,
            LocalDate invoiceDate
    ) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Debit Purchase/Expense, Credit AP
        lines.add(createLine(PURCHASE_EXPENSE, amount, null, "Purchase from " + vendorName));
        lines.add(createLine(ACCOUNTS_PAYABLE, null, amount, "Payable to " + vendorName));

        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(invoiceDate)
                .description("AP Invoice " + referenceNumber + " - " + vendorName)
                .source(JournalSource.ACCOUNTS_PAYABLE)
                .referenceType("AP_INVOICE")
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .lines(lines)
                .build();

        log.info("Posting purchase invoice to GL: {} - {} - {}", referenceNumber, vendorName, amount);
        return journalEntryService.createAndPostEntry(request, tenantId);
    }

    /**
     * Posts a payment to the general ledger.
     * This is called when a payment is made or received.
     *
     * @param paymentType "AP" for vendor payment, "AR" for customer payment
     * @param referenceId The payment ID
     * @param referenceNumber The payment reference number
     * @param amount The payment amount
     * @param counterpartyName The vendor or customer name
     * @param paymentDate The payment date
     * @return The created journal entry
     */
    @Transactional
    public JournalEntry postPayment(
            String paymentType,
            Long referenceId,
            String referenceNumber,
            BigDecimal amount,
            String counterpartyName,
            LocalDate paymentDate
    ) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        if ("AP".equals(paymentType)) {
            // Vendor payment: Debit AP, Credit Cash
            lines.add(createLine(ACCOUNTS_PAYABLE, amount, null, "Payment to " + counterpartyName));
            lines.add(createLine(CASH_ACCOUNT, null, amount, "Cash disbursement"));
        } else if ("AR".equals(paymentType)) {
            // Customer payment: Debit Cash, Credit AR
            lines.add(createLine(CASH_ACCOUNT, amount, null, "Payment from " + counterpartyName));
            lines.add(createLine(ACCOUNTS_RECEIVABLE, null, amount, "AR collection"));
        } else {
            throw new BusinessException("Unknown payment type: " + paymentType);
        }

        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(paymentDate)
                .description(paymentType + " Payment " + referenceNumber + " - " + counterpartyName)
                .source(JournalSource.PAYMENT)
                .referenceType(paymentType + "_PAYMENT")
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .lines(lines)
                .build();

        log.info("Posting payment to GL: {} - {} - {} - {}", paymentType, referenceNumber, counterpartyName, amount);
        return journalEntryService.createAndPostEntry(request, tenantId);
    }

    /**
     * Posts a receiving order to the general ledger.
     * This is called when goods are received from a vendor.
     *
     * @param receivingId The receiving order ID
     * @param receivingNumber The receiving order number
     * @param amount The total value of goods received
     * @param vendorName The vendor name
     * @param receivingDate The receiving date
     * @return The created journal entry
     */
    @Transactional
    public JournalEntry postReceiving(
            Long receivingId,
            String receivingNumber,
            BigDecimal amount,
            String vendorName,
            LocalDate receivingDate
    ) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Debit Inventory, Credit AP
        lines.add(createLine(INVENTORY_ACCOUNT, amount, null, "Inventory received from " + vendorName));
        lines.add(createLine(ACCOUNTS_PAYABLE, null, amount, "Payable to " + vendorName));

        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(receivingDate)
                .description("Receiving " + receivingNumber + " from " + vendorName)
                .source(JournalSource.RECEIVING)
                .referenceType("RECEIVING")
                .referenceId(receivingId)
                .referenceNumber(receivingNumber)
                .lines(lines)
                .build();

        log.info("Posting receiving to GL: {} - {} - {}", receivingNumber, vendorName, amount);
        return journalEntryService.createAndPostEntry(request, tenantId);
    }

    private CreateJournalLineRequest createLine(String accountCode, BigDecimal debit, BigDecimal credit, String description) {
        // Note: In a real implementation, we would look up the account ID by code
        // For now, we're using the account code as a placeholder
        // The JournalEntryService should resolve the account ID from the code
        return CreateJournalLineRequest.builder()
                .accountId(resolveAccountId(accountCode))
                .debitAmount(debit != null ? debit : BigDecimal.ZERO)
                .creditAmount(credit != null ? credit : BigDecimal.ZERO)
                .description(description)
                .build();
    }

    /**
     * Resolves an account ID from an account code.
     * Looks up the account from the database for the current tenant.
     */
    private Long resolveAccountId(String accountCode) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Optional<Account> account = accountRepository.findByCodeAndTenantId(accountCode, tenantId);
        return account.map(Account::getId)
                .orElseThrow(() -> new BusinessException("Account not found with code: " + accountCode));
    }

    /**
     * Resolves an account ID from an account code for a specific tenant.
     */
    private Long resolveAccountId(String accountCode, Long tenantId) {
        Optional<Account> account = accountRepository.findByCodeAndTenantId(accountCode, tenantId);
        return account.map(Account::getId)
                .orElseThrow(() -> new BusinessException("Account not found with code: " + accountCode));
    }

    // ============ AP Invoice Integration ============

    /**
     * Posts an AP Invoice to the general ledger.
     * Creates a journal entry that debits expense accounts and credits AP.
     *
     * @param invoice The AP invoice to post
     * @return The ID of the created journal entry
     */
    @Transactional
    public Long postAPInvoice(APInvoice invoice) {
        Long tenantId = invoice.getTenantId();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Debit expense accounts from invoice lines
        for (APInvoiceLine line : invoice.getLines()) {
            Long expenseAccountId = line.getExpenseAccountId();
            if (expenseAccountId == null) {
                expenseAccountId = invoice.getExpenseAccountId();
            }
            if (expenseAccountId == null) {
                expenseAccountId = resolveAccountId(PURCHASE_EXPENSE, tenantId);
            }

            lines.add(CreateJournalLineRequest.builder()
                    .accountId(expenseAccountId)
                    .debitAmount(line.getLineTotal())
                    .creditAmount(BigDecimal.ZERO)
                    .description("Invoice line: " + line.getDescription())
                    .build());
        }

        // Credit Accounts Payable for total
        Long apAccountId = invoice.getApAccountId();
        if (apAccountId == null) {
            apAccountId = resolveAccountId(ACCOUNTS_PAYABLE, tenantId);
        }

        lines.add(CreateJournalLineRequest.builder()
                .accountId(apAccountId)
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(invoice.getTotalAmount())
                .description("Payable to " + invoice.getVendorName())
                .build());

        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(invoice.getInvoiceDate())
                .description("AP Invoice " + invoice.getInvoiceNumber() + " - " + invoice.getVendorName())
                .source(JournalSource.ACCOUNTS_PAYABLE)
                .referenceType("AP_INVOICE")
                .referenceId(invoice.getId())
                .referenceNumber(invoice.getInvoiceNumber())
                .lines(lines)
                .build();

        log.info("Posting AP Invoice {} to GL: {}", invoice.getInvoiceNumber(), invoice.getTotalAmount());
        JournalEntry entry = journalEntryService.createAndPostEntry(request, tenantId);
        return entry.getId();
    }

    /**
     * Reverses an AP Invoice posting in the general ledger.
     *
     * @param invoice The AP invoice to reverse
     */
    @Transactional
    public void reverseAPInvoice(APInvoice invoice) {
        if (invoice.getGlJournalEntryId() == null) {
            throw new BusinessException("Invoice has not been posted to GL");
        }

        journalEntryService.reverseEntry(invoice.getGlJournalEntryId());
        log.info("Reversed GL posting for AP Invoice {}", invoice.getInvoiceNumber());
    }

    // ============ AP Payment Integration ============

    /**
     * Posts an AP Payment to the general ledger.
     * Creates a journal entry that debits AP and credits cash/bank.
     *
     * @param payment The AP payment to post
     * @return The ID of the created journal entry
     */
    @Transactional
    public Long postAPPayment(APPayment payment) {
        Long tenantId = payment.getTenantId();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Debit Accounts Payable
        Long apAccountId = payment.getApAccountId();
        if (apAccountId == null) {
            apAccountId = resolveAccountId(ACCOUNTS_PAYABLE, tenantId);
        }

        lines.add(CreateJournalLineRequest.builder()
                .accountId(apAccountId)
                .debitAmount(payment.getPaymentAmount())
                .creditAmount(BigDecimal.ZERO)
                .description("Payment to " + payment.getVendorName())
                .build());

        // Handle discounts taken
        BigDecimal totalDiscount = payment.getAllocations().stream()
                .map(a -> a.getDiscountTaken() != null ? a.getDiscountTaken() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            Long discountAccountId = resolveAccountId("5200", tenantId); // Purchase Discounts
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(discountAccountId)
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(totalDiscount)
                    .description("Purchase discount taken")
                    .build());
        }

        // Credit Cash/Bank
        Long cashAccountId = payment.getCashAccountId();
        if (cashAccountId == null) {
            cashAccountId = resolveAccountId(CASH_ACCOUNT, tenantId);
        }

        BigDecimal cashAmount = payment.getPaymentAmount().subtract(totalDiscount);
        lines.add(CreateJournalLineRequest.builder()
                .accountId(cashAccountId)
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(cashAmount)
                .description("Cash disbursement - " + payment.getPaymentMethod())
                .build());

        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(payment.getPaymentDate())
                .description("AP Payment " + payment.getPaymentNumber() + " - " + payment.getVendorName())
                .source(JournalSource.PAYMENT)
                .referenceType("AP_PAYMENT")
                .referenceId(payment.getId())
                .referenceNumber(payment.getPaymentNumber())
                .lines(lines)
                .build();

        log.info("Posting AP Payment {} to GL: {}", payment.getPaymentNumber(), payment.getPaymentAmount());
        JournalEntry entry = journalEntryService.createAndPostEntry(request, tenantId);
        return entry.getId();
    }

    /**
     * Reverses an AP Payment posting in the general ledger.
     *
     * @param payment The AP payment to reverse
     */
    @Transactional
    public void reverseAPPayment(APPayment payment) {
        if (payment.getGlJournalEntryId() == null) {
            throw new BusinessException("Payment has not been posted to GL");
        }

        journalEntryService.reverseEntry(payment.getGlJournalEntryId());
        log.info("Reversed GL posting for AP Payment {}", payment.getPaymentNumber());
    }

    // ============ AR Invoice Integration ============

    /**
     * Posts an AR Invoice to the general ledger.
     * Creates a journal entry that debits AR and credits revenue.
     *
     * @param invoice The AR invoice to post
     * @return The ID of the created journal entry
     */
    @Transactional
    public Long postARInvoice(ARInvoice invoice) {
        Long tenantId = invoice.getTenantId();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Debit Accounts Receivable
        Long arAccountId = resolveAccountId(ACCOUNTS_RECEIVABLE, tenantId);

        lines.add(CreateJournalLineRequest.builder()
                .accountId(arAccountId)
                .debitAmount(invoice.getTotalAmount())
                .creditAmount(BigDecimal.ZERO)
                .description("Receivable from " + invoice.getCustomer().getName())
                .build());

        // Credit Revenue accounts from invoice lines
        for (ARInvoiceLine line : invoice.getLines()) {
            Long revenueAccountId = resolveAccountId(SALES_REVENUE_ACCOUNT, tenantId);

            lines.add(CreateJournalLineRequest.builder()
                    .accountId(revenueAccountId)
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(line.getLineTotal())
                    .description("Sales: " + line.getDescription())
                    .build());

            // If cost is tracked, record COGS
            if (line.getUnitCost() != null && line.getUnitCost().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal totalCost = line.getUnitCost().multiply(line.getQuantity());
                Long cogsAccountId = resolveAccountId(COGS_ACCOUNT, tenantId);
                Long inventoryAccountId = resolveAccountId(INVENTORY_ACCOUNT, tenantId);

                lines.add(CreateJournalLineRequest.builder()
                        .accountId(cogsAccountId)
                        .debitAmount(totalCost)
                        .creditAmount(BigDecimal.ZERO)
                        .description("COGS: " + line.getDescription())
                        .build());

                lines.add(CreateJournalLineRequest.builder()
                        .accountId(inventoryAccountId)
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(totalCost)
                        .description("Inventory sold: " + line.getDescription())
                        .build());
            }
        }

        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(invoice.getInvoiceDate())
                .description("AR Invoice " + invoice.getInvoiceNumber() + " - " + invoice.getCustomer().getName())
                .source(JournalSource.ACCOUNTS_RECEIVABLE)
                .referenceType("AR_INVOICE")
                .referenceId(invoice.getId())
                .referenceNumber(invoice.getInvoiceNumber())
                .lines(lines)
                .build();

        log.info("Posting AR Invoice {} to GL: {}", invoice.getInvoiceNumber(), invoice.getTotalAmount());
        JournalEntry entry = journalEntryService.createAndPostEntry(request, tenantId);
        return entry.getId();
    }

    /**
     * Reverses an AR Invoice posting in the general ledger.
     *
     * @param invoice The AR invoice to reverse
     * @param reason The reason for reversal
     */
    @Transactional
    public void reverseARInvoice(ARInvoice invoice, String reason) {
        if (invoice.getGlJournalEntryId() == null) {
            log.warn("Invoice {} has not been posted to GL", invoice.getInvoiceNumber());
            return;
        }

        journalEntryService.reverseEntry(invoice.getGlJournalEntryId());
        log.info("Reversed GL posting for AR Invoice {}: {}", invoice.getInvoiceNumber(), reason);
    }

    // ============ AR Payment Integration ============

    /**
     * Posts an AR Payment to the general ledger.
     * Creates a journal entry that debits cash and credits AR.
     *
     * @param payment The AR payment to post
     * @return The ID of the created journal entry
     */
    @Transactional
    public Long postARPayment(ARPayment payment) {
        Long tenantId = payment.getTenantId();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Debit Cash/Bank
        Long cashAccountId = resolveAccountId(CASH_ACCOUNT, tenantId);

        lines.add(CreateJournalLineRequest.builder()
                .accountId(cashAccountId)
                .debitAmount(payment.getPaymentAmount())
                .creditAmount(BigDecimal.ZERO)
                .description("Payment from " + payment.getCustomer().getName() + " - " + payment.getPaymentMethod())
                .build());

        // Handle discounts given
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalWriteOff = BigDecimal.ZERO;

        if (payment.getAllocations() != null) {
            for (ARPaymentAllocation allocation : payment.getAllocations()) {
                if (allocation.getDiscountTaken() != null) {
                    totalDiscount = totalDiscount.add(allocation.getDiscountTaken());
                }
                if (allocation.getWriteOffAmount() != null) {
                    totalWriteOff = totalWriteOff.add(allocation.getWriteOffAmount());
                }
            }
        }

        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            Long discountAccountId = resolveAccountId("4200", tenantId); // Sales Discounts
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(discountAccountId)
                    .debitAmount(totalDiscount)
                    .creditAmount(BigDecimal.ZERO)
                    .description("Sales discount given")
                    .build());
        }

        if (totalWriteOff.compareTo(BigDecimal.ZERO) > 0) {
            Long writeOffAccountId = resolveAccountId("6200", tenantId); // Bad Debt Expense
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(writeOffAccountId)
                    .debitAmount(totalWriteOff)
                    .creditAmount(BigDecimal.ZERO)
                    .description("Write-off amount")
                    .build());
        }

        // Credit Accounts Receivable
        Long arAccountId = resolveAccountId(ACCOUNTS_RECEIVABLE, tenantId);
        BigDecimal arAmount = payment.getPaymentAmount().add(totalDiscount).add(totalWriteOff);

        lines.add(CreateJournalLineRequest.builder()
                .accountId(arAccountId)
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(arAmount)
                .description("AR collection from " + payment.getCustomer().getName())
                .build());

        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(payment.getPaymentDate())
                .description("AR Payment " + payment.getPaymentNumber() + " - " + payment.getCustomer().getName())
                .source(JournalSource.PAYMENT)
                .referenceType("AR_PAYMENT")
                .referenceId(payment.getId())
                .referenceNumber(payment.getPaymentNumber())
                .lines(lines)
                .build();

        log.info("Posting AR Payment {} to GL: {}", payment.getPaymentNumber(), payment.getPaymentAmount());
        JournalEntry entry = journalEntryService.createAndPostEntry(request, tenantId);
        return entry.getId();
    }

    /**
     * Reverses an AR Payment posting in the general ledger.
     *
     * @param payment The AR payment to reverse
     * @param reason The reason for reversal
     */
    @Transactional
    public void reverseARPayment(ARPayment payment, String reason) {
        if (payment.getGlJournalEntryId() == null) {
            log.warn("Payment {} has not been posted to GL", payment.getPaymentNumber());
            return;
        }

        journalEntryService.reverseEntry(payment.getGlJournalEntryId());
        log.info("Reversed GL posting for AR Payment {}: {}", payment.getPaymentNumber(), reason);
    }

    // ============ Credit Note Integration ============

    /**
     * Posts a Credit Note to the general ledger.
     * Creates a journal entry that debits revenue/returns and credits AR.
     *
     * @param creditNote The credit note to post
     * @return The ID of the created journal entry
     */
    @Transactional
    public Long postCreditNote(CreditNote creditNote) {
        Long tenantId = creditNote.getTenantId();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Debit Sales Returns & Allowances
        Long returnsAccountId = resolveAccountId("4300", tenantId); // Sales Returns & Allowances
        lines.add(CreateJournalLineRequest.builder()
                .accountId(returnsAccountId)
                .debitAmount(creditNote.getCreditAmount())
                .creditAmount(BigDecimal.ZERO)
                .description("Credit note: " + creditNote.getReason())
                .build());

        // Credit Accounts Receivable
        Long arAccountId = resolveAccountId(ACCOUNTS_RECEIVABLE, tenantId);
        lines.add(CreateJournalLineRequest.builder()
                .accountId(arAccountId)
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(creditNote.getCreditAmount())
                .description("Credit to " + creditNote.getCustomer().getName())
                .build());

        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(creditNote.getCreditNoteDate())
                .description("Credit Note " + creditNote.getCreditNoteNumber() + " - " + creditNote.getCustomer().getName())
                .source(JournalSource.ACCOUNTS_RECEIVABLE)
                .referenceType("CREDIT_NOTE")
                .referenceId(creditNote.getId())
                .referenceNumber(creditNote.getCreditNoteNumber())
                .lines(lines)
                .build();

        log.info("Posting Credit Note {} to GL: {}", creditNote.getCreditNoteNumber(), creditNote.getCreditAmount());
        JournalEntry entry = journalEntryService.createAndPostEntry(request, tenantId);
        return entry.getId();
    }

    /**
     * Reverses a Credit Note posting in the general ledger.
     *
     * @param creditNote The credit note to reverse
     * @param reason The reason for reversal
     */
    @Transactional
    public void reverseCreditNote(CreditNote creditNote, String reason) {
        if (creditNote.getGlJournalEntryId() == null) {
            log.warn("Credit Note {} has not been posted to GL", creditNote.getCreditNoteNumber());
            return;
        }

        journalEntryService.reverseEntry(creditNote.getGlJournalEntryId());
        log.info("Reversed GL posting for Credit Note {}: {}", creditNote.getCreditNoteNumber(), reason);
    }

    // ============ POS Transaction Integration ============

    /**
     * Posts a POS Transaction to the general ledger.
     * This is a convenience method that extracts data from a POSTransaction object.
     *
     * @param transaction The POS transaction to post
     * @return The ID of the created journal entry
     */
    @Transactional
    public Long postPOSTransaction(Object transaction) {
        // Use reflection or duck typing to get transaction properties
        // This allows us to avoid circular dependency with POS module
        try {
            java.lang.reflect.Method getId = transaction.getClass().getMethod("getId");
            java.lang.reflect.Method getTransactionNumber = transaction.getClass().getMethod("getTransactionNumber");
            java.lang.reflect.Method getTotalAmount = transaction.getClass().getMethod("getTotalAmount");
            java.lang.reflect.Method getLines = transaction.getClass().getMethod("getLines");

            Long id = (Long) getId.invoke(transaction);
            String transactionNumber = (String) getTransactionNumber.invoke(transaction);
            BigDecimal salesAmount = (BigDecimal) getTotalAmount.invoke(transaction);

            // Extract discount amount (transaction-level + line-level)
            BigDecimal discountAmount = BigDecimal.ZERO;
            try {
                java.lang.reflect.Method getDiscountAmount = transaction.getClass().getMethod("getDiscountAmount");
                BigDecimal txDiscount = (BigDecimal) getDiscountAmount.invoke(transaction);
                if (txDiscount != null) {
                    discountAmount = discountAmount.add(txDiscount);
                }
            } catch (NoSuchMethodException ignored) {}

            // Calculate COGS and line-level discounts from line items
            BigDecimal costAmount = BigDecimal.ZERO;
            @SuppressWarnings("unchecked")
            java.util.List<Object> lines = (java.util.List<Object>) getLines.invoke(transaction);
            for (Object line : lines) {
                java.lang.reflect.Method getCostPrice = line.getClass().getMethod("getCostPrice");
                java.lang.reflect.Method getQuantity = line.getClass().getMethod("getQuantity");
                BigDecimal costPrice = (BigDecimal) getCostPrice.invoke(line);
                BigDecimal quantity = (BigDecimal) getQuantity.invoke(line);
                if (costPrice != null && quantity != null) {
                    costAmount = costAmount.add(costPrice.multiply(quantity));
                }
                // Accumulate line-level discounts
                try {
                    java.lang.reflect.Method getLineDiscount = line.getClass().getMethod("getDiscountAmount");
                    BigDecimal lineDiscount = (BigDecimal) getLineDiscount.invoke(line);
                    if (lineDiscount != null && lineDiscount.compareTo(BigDecimal.ZERO) > 0) {
                        discountAmount = discountAmount.add(lineDiscount);
                    }
                } catch (NoSuchMethodException ignored) {}
            }

            JournalEntry entry = postSalesTransaction(
                    id,
                    transactionNumber,
                    salesAmount,
                    costAmount,
                    discountAmount,
                    "POS",
                    "POS Sale: " + transactionNumber,
                    LocalDate.now()
            );
            return entry.getId();
        } catch (Exception e) {
            log.error("Failed to post POS transaction to GL: {}", e.getMessage());
            throw new BusinessException("Failed to post POS transaction to GL: " + e.getMessage());
        }
    }

    /**
     * Reverses a sales transaction in the general ledger.
     *
     * @param journalEntryId The ID of the journal entry to reverse
     */
    @Transactional
    public void reverseSalesTransaction(Long journalEntryId) {
        if (journalEntryId == null) {
            log.warn("No journal entry ID provided for reversal");
            return;
        }
        journalEntryService.reverseEntry(journalEntryId);
        log.info("Reversed GL posting for sales transaction with journal entry ID {}", journalEntryId);
    }
}
