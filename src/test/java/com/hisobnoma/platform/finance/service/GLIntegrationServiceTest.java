package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateJournalEntryRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GLIntegrationServiceTest {

    @Mock
    private JournalEntryService journalEntryService;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private GLIntegrationService glIntegrationService;

    private static final Long TENANT_ID = 1L;
    private JournalEntry postedEntry;

    @BeforeEach
    void setUp() {
        postedEntry = JournalEntry.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .entryNumber("JE-202401-0001")
                .status(JournalStatus.POSTED)
                .lines(new ArrayList<>())
                .build();
    }

    private void stubAccountLookup(String code, Long id) {
        Account account = Account.builder()
                .id(id)
                .code(code)
                .tenantId(TENANT_ID)
                .build();
        lenient().when(accountRepository.findByCodeAndTenantId(code, TENANT_ID))
                .thenReturn(Optional.of(account));
    }

    // ==================== postAPInvoice ====================

    @Test
    void postAPInvoice_success_createsJournalEntry() {
        // Given
        APInvoiceLine line = APInvoiceLine.builder()
                .description("Office Supplies")
                .lineTotal(new BigDecimal("5000.00"))
                .expenseAccountId(10L)
                .build();

        APInvoice invoice = APInvoice.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AP-001")
                .vendorName("Vendor ABC")
                .invoiceDate(LocalDate.of(2024, 1, 15))
                .totalAmount(new BigDecimal("5000.00"))
                .apAccountId(20L)
                .lines(new ArrayList<>(List.of(line)))
                .build();

        when(journalEntryService.createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID)))
                .thenReturn(postedEntry);

        // When
        Long entryId = glIntegrationService.postAPInvoice(invoice);

        // Then
        assertNotNull(entryId);
        assertEquals(1L, entryId);
        verify(journalEntryService).createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID));
    }

    @Test
    void postAPInvoice_withDefaultAccounts_resolvesFromRepository() {
        // Given
        APInvoiceLine line = APInvoiceLine.builder()
                .description("Office Supplies")
                .lineTotal(new BigDecimal("3000.00"))
                .expenseAccountId(null)
                .build();

        APInvoice invoice = APInvoice.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AP-002")
                .vendorName("Vendor XYZ")
                .invoiceDate(LocalDate.of(2024, 1, 20))
                .totalAmount(new BigDecimal("3000.00"))
                .expenseAccountId(null)
                .apAccountId(null)
                .lines(new ArrayList<>(List.of(line)))
                .build();

        stubAccountLookup("5200", 100L);
        stubAccountLookup("2110", 200L);

        when(journalEntryService.createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID)))
                .thenReturn(postedEntry);

        // When
        Long entryId = glIntegrationService.postAPInvoice(invoice);

        // Then
        assertNotNull(entryId);
        verify(accountRepository).findByCodeAndTenantId("5200", TENANT_ID);
        verify(accountRepository).findByCodeAndTenantId("2110", TENANT_ID);
    }

    // ==================== postARInvoice ====================

    @Test
    void postARInvoice_success_createsJournalEntry() {
        // Given
        Customer customer = Customer.builder()
                .id(1L)
                .code("CUST-001")
                .name("Customer ABC")
                .build();

        ARInvoiceLine line = ARInvoiceLine.builder()
                .description("Product Sale")
                .lineTotal(new BigDecimal("10000.00"))
                .quantity(new BigDecimal("5"))
                .unitPrice(new BigDecimal("2000.00"))
                .build();

        ARInvoice invoice = ARInvoice.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AR-001")
                .customer(customer)
                .invoiceDate(LocalDate.of(2024, 1, 15))
                .totalAmount(new BigDecimal("10000.00"))
                .lines(new ArrayList<>(List.of(line)))
                .build();

        stubAccountLookup("1130", 10L);
        stubAccountLookup("4100", 20L);

        when(journalEntryService.createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID)))
                .thenReturn(postedEntry);

        // When
        Long entryId = glIntegrationService.postARInvoice(invoice);

        // Then
        assertNotNull(entryId);
        assertEquals(1L, entryId);
        verify(journalEntryService).createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID));
    }

    @Test
    void postARInvoice_withCOGS_createsAdditionalLines() {
        // Given
        Customer customer = Customer.builder()
                .id(1L)
                .code("CUST-001")
                .name("Customer ABC")
                .build();

        ARInvoiceLine line = ARInvoiceLine.builder()
                .description("Product Sale")
                .lineTotal(new BigDecimal("10000.00"))
                .quantity(new BigDecimal("5"))
                .unitPrice(new BigDecimal("2000.00"))
                .unitCost(new BigDecimal("1500.00"))
                .build();

        ARInvoice invoice = ARInvoice.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AR-002")
                .customer(customer)
                .invoiceDate(LocalDate.of(2024, 1, 15))
                .totalAmount(new BigDecimal("10000.00"))
                .lines(new ArrayList<>(List.of(line)))
                .build();

        stubAccountLookup("1130", 10L);
        stubAccountLookup("4100", 20L);
        stubAccountLookup("5100", 30L);
        stubAccountLookup("1140", 40L);

        when(journalEntryService.createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID)))
                .thenReturn(postedEntry);

        // When
        Long entryId = glIntegrationService.postARInvoice(invoice);

        // Then
        assertNotNull(entryId);
        verify(journalEntryService).createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID));
    }

    @Test
    void postARInvoice_withHeaderDiscount_postsBalancedEntry() {
        Customer customer = Customer.builder().id(1L).code("C").name("Cust").build();
        // Two lines gross 10000; header discount 1500 → total owed 8500, no tax.
        ARInvoiceLine line = ARInvoiceLine.builder()
                .description("Sale").lineTotal(new BigDecimal("10000.00"))
                .quantity(new BigDecimal("1")).unitPrice(new BigDecimal("10000.00"))
                .taxAmount(BigDecimal.ZERO).build();
        ARInvoice invoice = ARInvoice.builder()
                .id(1L).tenantId(TENANT_ID).invoiceNumber("AR-D1").customer(customer)
                .invoiceDate(LocalDate.of(2024, 1, 15))
                .discountAmount(new BigDecimal("1500.00"))
                .totalAmount(new BigDecimal("8500.00"))
                .lines(new ArrayList<>(List.of(line))).build();
        stubAccountLookup("1130", 10L);
        stubAccountLookup("4100", 20L);
        when(journalEntryService.createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID)))
                .thenReturn(postedEntry);

        glIntegrationService.postARInvoice(invoice);

        CreateJournalEntryRequest req = captureEntry();
        assertBalanced(req);
        // AR debit 8500 == revenue credit 8500 (net of the discount already baked into the total).
        assertEquals(0, new BigDecimal("8500.00").compareTo(totalDebits(req)));
    }

    @Test
    void postARInvoice_withLineTax_segregatesOutputVatFromRevenue() {
        Customer customer = Customer.builder().id(1L).code("C").name("Cust").build();
        // lineTotal 11200 includes 1200 tax; total owed 11200.
        ARInvoiceLine line = ARInvoiceLine.builder()
                .description("Sale").lineTotal(new BigDecimal("11200.00"))
                .quantity(new BigDecimal("1")).unitPrice(new BigDecimal("10000.00"))
                .taxAmount(new BigDecimal("1200.00")).build();
        ARInvoice invoice = ARInvoice.builder()
                .id(1L).tenantId(TENANT_ID).invoiceNumber("AR-T1").customer(customer)
                .invoiceDate(LocalDate.of(2024, 1, 15))
                .totalAmount(new BigDecimal("11200.00"))
                .lines(new ArrayList<>(List.of(line))).build();
        stubAccountLookup("1130", 10L);
        stubAccountLookup("4100", 20L);
        stubAccountLookup("2130", 25L); // VAT Payable
        when(journalEntryService.createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID)))
                .thenReturn(postedEntry);

        glIntegrationService.postARInvoice(invoice);

        CreateJournalEntryRequest req = captureEntry();
        assertBalanced(req);
        // Revenue is net of tax (10000), VAT (1200) credited to the liability account 25.
        BigDecimal revenueCredit = creditToAccount(req, 20L);
        BigDecimal vatCredit = creditToAccount(req, 25L);
        assertEquals(0, new BigDecimal("10000.00").compareTo(revenueCredit));
        assertEquals(0, new BigDecimal("1200.00").compareTo(vatCredit));
    }

    private CreateJournalEntryRequest captureEntry() {
        org.mockito.ArgumentCaptor<CreateJournalEntryRequest> captor =
                org.mockito.ArgumentCaptor.forClass(CreateJournalEntryRequest.class);
        verify(journalEntryService).createAndPostEntry(captor.capture(), eq(TENANT_ID));
        return captor.getValue();
    }

    private BigDecimal totalDebits(CreateJournalEntryRequest req) {
        return req.getLines().stream().map(l -> l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalCredits(CreateJournalEntryRequest req) {
        return req.getLines().stream().map(l -> l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertBalanced(CreateJournalEntryRequest req) {
        assertEquals(0, totalDebits(req).compareTo(totalCredits(req)),
                "journal entry must balance: debits=" + totalDebits(req) + " credits=" + totalCredits(req));
    }

    private BigDecimal creditToAccount(CreateJournalEntryRequest req, Long accountId) {
        return req.getLines().stream().filter(l -> accountId.equals(l.getAccountId()))
                .map(l -> l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== reverseEntry (reverseAPInvoice / reverseARInvoice) ====================

    @Test
    void reverseAPInvoice_success_reversesEntry() {
        // Given
        APInvoice invoice = APInvoice.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AP-001")
                .glJournalEntryId(1L)
                .build();

        JournalEntry reversedEntry = JournalEntry.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .status(JournalStatus.POSTED)
                .lines(new ArrayList<>())
                .build();

        when(journalEntryService.reverseEntry(1L)).thenReturn(reversedEntry);

        // When
        glIntegrationService.reverseAPInvoice(invoice);

        // Then
        verify(journalEntryService).reverseEntry(1L);
    }

    @Test
    void reverseAPInvoice_notPosted_throwsBusinessException() {
        // Given
        APInvoice invoice = APInvoice.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AP-001")
                .glJournalEntryId(null)
                .build();

        // When/Then
        assertThrows(BusinessException.class,
                () -> glIntegrationService.reverseAPInvoice(invoice));
        verify(journalEntryService, never()).reverseEntry(any());
    }

    @Test
    void reverseARInvoice_success_reversesEntry() {
        // Given
        ARInvoice invoice = ARInvoice.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AR-001")
                .glJournalEntryId(1L)
                .build();

        JournalEntry reversedEntry = JournalEntry.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .status(JournalStatus.POSTED)
                .lines(new ArrayList<>())
                .build();

        when(journalEntryService.reverseEntry(1L)).thenReturn(reversedEntry);

        // When
        glIntegrationService.reverseARInvoice(invoice, "Customer credit note");

        // Then
        verify(journalEntryService).reverseEntry(1L);
    }

    @Test
    void reverseARInvoice_notPosted_doesNotThrow() {
        // Given - ARInvoice reversal silently returns when no GL entry
        ARInvoice invoice = ARInvoice.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .invoiceNumber("AR-001")
                .glJournalEntryId(null)
                .build();

        // When
        glIntegrationService.reverseARInvoice(invoice, "Test reason");

        // Then - no exception, just a warning log
        verify(journalEntryService, never()).reverseEntry(any());
    }

    // ==================== postPurchaseInvoice ====================

    @Test
    void postPurchaseInvoice_success() {
        // Given
        stubAccountLookup("5200", 100L);
        stubAccountLookup("2110", 200L);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryService.createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID)))
                .thenReturn(postedEntry);

        // When
        JournalEntry result = glIntegrationService.postPurchaseInvoice(
                1L, "PO-001", new BigDecimal("3000.00"), "Vendor A", LocalDate.now());

        // Then
        assertNotNull(result);
        verify(journalEntryService).createAndPostEntry(any(), eq(TENANT_ID));
    }

    // ==================== postInventoryMovement ====================

    @Test
    void postInventoryMovement_receiving_success() {
        // Given
        stubAccountLookup("1140", 100L);
        stubAccountLookup("2110", 200L);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryService.createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID)))
                .thenReturn(postedEntry);

        // When
        JournalEntry result = glIntegrationService.postInventoryMovement(
                "RECEIVING", 1L, "RCV-001", new BigDecimal("2000.00"),
                "Goods received", LocalDate.now());

        // Then
        assertNotNull(result);
        verify(journalEntryService).createAndPostEntry(any(), eq(TENANT_ID));
    }

    @Test
    void postInventoryMovement_unknownType_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);

        // When/Then
        assertThrows(BusinessException.class,
                () -> glIntegrationService.postInventoryMovement(
                        "UNKNOWN", 1L, "RCV-001", new BigDecimal("2000.00"),
                        "Unknown movement", LocalDate.now()));
    }

    // ==================== postSalesTransaction ====================

    @Test
    void postSalesTransaction_cash_success() {
        // Given
        stubAccountLookup("1110", 100L);
        stubAccountLookup("4100", 200L);
        stubAccountLookup("5100", 300L);
        stubAccountLookup("1140", 400L);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryService.createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID)))
                .thenReturn(postedEntry);

        // When
        JournalEntry result = glIntegrationService.postSalesTransaction(
                1L, "TXN-001", new BigDecimal("5000.00"), new BigDecimal("3000.00"),
                "CASH", "Cash sale", LocalDate.now());

        // Then
        assertNotNull(result);
        verify(journalEntryService).createAndPostEntry(any(), eq(TENANT_ID));
    }

    // ==================== postPayment ====================

    @Test
    void postPayment_AP_success() {
        // Given
        stubAccountLookup("2110", 100L);
        stubAccountLookup("1110", 200L);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryService.createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID)))
                .thenReturn(postedEntry);

        // When
        JournalEntry result = glIntegrationService.postPayment(
                "AP", 1L, "PAY-001", new BigDecimal("5000.00"), "Vendor A", LocalDate.now());

        // Then
        assertNotNull(result);
        verify(journalEntryService).createAndPostEntry(any(), eq(TENANT_ID));
    }

    @Test
    void postPayment_AR_success() {
        // Given
        stubAccountLookup("1110", 100L);
        stubAccountLookup("1130", 200L);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(journalEntryService.createAndPostEntry(any(CreateJournalEntryRequest.class), eq(TENANT_ID)))
                .thenReturn(postedEntry);

        // When
        JournalEntry result = glIntegrationService.postPayment(
                "AR", 1L, "PAY-002", new BigDecimal("3000.00"), "Customer B", LocalDate.now());

        // Then
        assertNotNull(result);
        verify(journalEntryService).createAndPostEntry(any(), eq(TENANT_ID));
    }

    @Test
    void postPayment_unknownType_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);

        // When/Then
        assertThrows(BusinessException.class,
                () -> glIntegrationService.postPayment(
                        "UNKNOWN", 1L, "PAY-003", new BigDecimal("1000.00"),
                        "Unknown", LocalDate.now()));
    }

    // ==================== reverseSalesTransaction ====================

    @Test
    void reverseSalesTransaction_success() {
        // Given
        JournalEntry reversedEntry = JournalEntry.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .status(JournalStatus.POSTED)
                .lines(new ArrayList<>())
                .build();
        when(journalEntryService.reverseEntry(1L)).thenReturn(reversedEntry);

        // When
        glIntegrationService.reverseSalesTransaction(1L);

        // Then
        verify(journalEntryService).reverseEntry(1L);
    }

    @Test
    void reverseSalesTransaction_nullId_doesNotThrow() {
        // Given / When
        glIntegrationService.reverseSalesTransaction(null);

        // Then
        verify(journalEntryService, never()).reverseEntry(any());
    }
}
