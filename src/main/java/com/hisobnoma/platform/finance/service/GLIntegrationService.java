package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.finance.dto.CreateJournalEntryRequest;
import com.hisobnoma.platform.finance.dto.CreateJournalLineRequest;
import com.hisobnoma.platform.finance.entity.JournalEntry;
import com.hisobnoma.platform.finance.entity.JournalSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    // Account codes - these should be configurable per tenant
    // For now, these are placeholder values that should be set up in the chart of accounts
    private static final String INVENTORY_ACCOUNT = "1300";  // Asset: Inventory
    private static final String COGS_ACCOUNT = "5100";       // Expense: Cost of Goods Sold
    private static final String SALES_REVENUE_ACCOUNT = "4100"; // Revenue: Sales Revenue
    private static final String CASH_ACCOUNT = "1100";       // Asset: Cash
    private static final String ACCOUNTS_RECEIVABLE = "1200"; // Asset: Accounts Receivable
    private static final String ACCOUNTS_PAYABLE = "2100";   // Liability: Accounts Payable
    private static final String PURCHASE_EXPENSE = "5200";   // Expense: Purchases

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
        Long tenantId = securityContextHelper.getCurrentTenantId();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Revenue entry: Debit Cash/AR, Credit Sales Revenue
        String receivableAccount = "CASH".equals(paymentType) ? CASH_ACCOUNT : ACCOUNTS_RECEIVABLE;
        lines.add(createLine(receivableAccount, salesAmount, null, "Payment for sale"));
        lines.add(createLine(SALES_REVENUE_ACCOUNT, null, salesAmount, "Sales revenue"));

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

        log.info("Posting sales transaction to GL: {} - {} - {}", referenceNumber, salesAmount, costAmount);
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
     * In a real implementation, this would look up the account from the database.
     * For now, returns a placeholder that should be configured properly.
     */
    private Long resolveAccountId(String accountCode) {
        // TODO: Implement proper account lookup by code
        // For now, return a placeholder value
        // The actual implementation should query the AccountRepository
        return switch (accountCode) {
            case CASH_ACCOUNT -> 1L;
            case ACCOUNTS_RECEIVABLE -> 2L;
            case INVENTORY_ACCOUNT -> 3L;
            case ACCOUNTS_PAYABLE -> 4L;
            case SALES_REVENUE_ACCOUNT -> 5L;
            case COGS_ACCOUNT -> 6L;
            case PURCHASE_EXPENSE -> 7L;
            default -> throw new BusinessException("Unknown account code: " + accountCode);
        };
    }
}
