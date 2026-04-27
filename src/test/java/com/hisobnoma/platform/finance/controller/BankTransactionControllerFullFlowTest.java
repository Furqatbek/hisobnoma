package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateBankTransactionRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
import com.hisobnoma.platform.finance.repository.BankTransactionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BankTransactionControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BankTransactionRepository bankTransactionRepository;
    @Autowired private BankAccountRepository bankAccountRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User adminUser;
    private Account glAccount;
    private Account expenseAccount;
    private BankAccount checkingAccount;
    private BankAccount savingsAccount;
    private BankTransaction depositTx;
    private BankTransaction withdrawalTx;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("BankTx Tenant").code("FULLFLOW_BTX").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("banktxadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        glAccount = accountRepository.saveAndFlush(Account.builder()
                .code("1120").name("Bank Accounts").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("1120")
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());

        expenseAccount = accountRepository.saveAndFlush(Account.builder()
                .code("5010").name("Office Expenses").accountType(AccountType.EXPENSE)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("5010")
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());

        checkingAccount = bankAccountRepository.saveAndFlush(BankAccount.builder()
                .accountCode("BA-001").accountName("Main Checking")
                .accountType(BankAccountType.CHECKING)
                .bankName("National Bank").bankBranch("Downtown")
                .accountNumber("123456789").currency("UZS")
                .currentBalance(new BigDecimal("50000000.0000"))
                .availableBalance(new BigDecimal("50000000.0000"))
                .openingBalance(new BigDecimal("50000000.0000"))
                .active(true).defaultAccount(true)
                .allowPayments(true).allowReceipts(true)
                .glAccount(glAccount)
                .tenantId(tenant.getId()).build());

        savingsAccount = bankAccountRepository.saveAndFlush(BankAccount.builder()
                .accountCode("BA-002").accountName("Business Savings")
                .accountType(BankAccountType.SAVINGS)
                .bankName("National Bank").bankBranch("Downtown")
                .accountNumber("987654321").currency("UZS")
                .currentBalance(new BigDecimal("100000000.0000"))
                .availableBalance(new BigDecimal("100000000.0000"))
                .openingBalance(new BigDecimal("100000000.0000"))
                .active(true).defaultAccount(false)
                .allowPayments(true).allowReceipts(true)
                .tenantId(tenant.getId()).build());

        depositTx = bankTransactionRepository.saveAndFlush(BankTransaction.builder()
                .bankAccount(checkingAccount)
                .transactionNumber("BT-000001")
                .transactionDate(LocalDate.of(2026, 4, 10))
                .transactionType(BankTransactionType.DEPOSIT)
                .status(BankTransactionStatus.PENDING)
                .description("Customer payment received")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("5000000.0000"))
                .runningBalance(new BigDecimal("55000000.0000"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .baseAmount(new BigDecimal("5000000.0000"))
                .referenceNumber("REF-001")
                .payeePayer("Customer A")
                .tenantId(tenant.getId()).build());

        withdrawalTx = bankTransactionRepository.saveAndFlush(BankTransaction.builder()
                .bankAccount(checkingAccount)
                .transactionNumber("BT-000002")
                .transactionDate(LocalDate.of(2026, 4, 15))
                .transactionType(BankTransactionType.WITHDRAWAL)
                .status(BankTransactionStatus.PENDING)
                .description("Office supplies purchase")
                .debitAmount(new BigDecimal("1000000.0000"))
                .creditAmount(BigDecimal.ZERO)
                .runningBalance(new BigDecimal("54000000.0000"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .baseAmount(new BigDecimal("1000000.0000"))
                .counterAccount(expenseAccount)
                .payeePayer("Supplier B")
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_BANK_VIEW"),
                        new SimpleGrantedAuthority("FINANCE_BANK_TRANSACT"),
                        new SimpleGrantedAuthority("FINANCE_BANK_TRANSFER"),
                        new SimpleGrantedAuthority("FINANCE_BANK_RECONCILE"),
                        new SimpleGrantedAuthority("FINANCE_BANK_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor viewOnlyAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_BANK_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/finance/bank-transactions ----

    @Test
    void getTransactions_returnsPaginatedList() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-transactions").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/bank-transactions/{id} ----

    @Test
    void getTransactionById_returnsTransaction() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-transactions/" + depositTx.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(depositTx.getId()))
                .andExpect(jsonPath("$.transactionNumber").value("BT-000001"))
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.description").value("Customer payment received"))
                .andExpect(jsonPath("$.creditAmount").value(5000000))
                .andExpect(jsonPath("$.bankAccountId").value(checkingAccount.getId()))
                .andExpect(jsonPath("$.referenceNumber").value("REF-001"))
                .andExpect(jsonPath("$.payeePayer").value("Customer A"));
    }

    @Test
    void getTransactionById_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-transactions/99999")
                        .with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/bank-transactions/by-account/{bankAccountId} ----

    @Test
    void getTransactionsByBankAccount_returnsFiltered() throws Exception {
        // Both depositTx and withdrawalTx belong to checkingAccount
        mockMvc.perform(get("/api/v1/finance/bank-transactions/by-account/" + checkingAccount.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].bankAccountId",
                        everyItem(equalTo(checkingAccount.getId().intValue()))));
    }

    @Test
    void getTransactionsByBankAccount_emptyForOtherAccount() throws Exception {
        // savingsAccount has no transactions
        mockMvc.perform(get("/api/v1/finance/bank-transactions/by-account/" + savingsAccount.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ---- GET /api/v1/finance/bank-transactions/by-account/{id}/date-range ----

    @Test
    void getTransactionsByDateRange_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-transactions/by-account/" + checkingAccount.getId() + "/date-range")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-12")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionNumber").value("BT-000001"));
    }

    @Test
    void getTransactionsByDateRange_returnsAllInRange() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-transactions/by-account/" + checkingAccount.getId() + "/date-range")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-30")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- POST /api/v1/finance/bank-transactions ----

    @Test
    void createTransaction_deposit_returnsCreated() throws Exception {
        CreateBankTransactionRequest request = CreateBankTransactionRequest.builder()
                .bankAccountId(checkingAccount.getId())
                .transactionDate(LocalDate.of(2026, 4, 20))
                .transactionType(BankTransactionType.DEPOSIT)
                .description("Cash deposit at branch")
                .amount(new BigDecimal("10000000"))
                .isDebit(false)
                .currency("UZS")
                .referenceNumber("DEP-100")
                .payeePayer("Branch Cashier")
                .notes("Monthly deposit")
                .build();

        mockMvc.perform(post("/api/v1/finance/bank-transactions")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionNumber").value(startsWith("BT-")))
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.creditAmount").value(10000000))
                .andExpect(jsonPath("$.debitAmount").value(0))
                .andExpect(jsonPath("$.bankAccountId").value(checkingAccount.getId()))
                .andExpect(jsonPath("$.description").value("Cash deposit at branch"))
                .andExpect(jsonPath("$.referenceNumber").value("DEP-100"))
                .andExpect(jsonPath("$.payeePayer").value("Branch Cashier"));
    }

    @Test
    void createTransaction_withdrawal_returnsCreated() throws Exception {
        CreateBankTransactionRequest request = CreateBankTransactionRequest.builder()
                .bankAccountId(checkingAccount.getId())
                .transactionDate(LocalDate.of(2026, 4, 21))
                .transactionType(BankTransactionType.WITHDRAWAL)
                .description("Vendor payment")
                .amount(new BigDecimal("2000000"))
                .isDebit(true)
                .currency("UZS")
                .counterAccountId(expenseAccount.getId())
                .payeePayer("Vendor C")
                .build();

        mockMvc.perform(post("/api/v1/finance/bank-transactions")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionNumber").value(startsWith("BT-")))
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.debitAmount").value(2000000))
                .andExpect(jsonPath("$.creditAmount").value(0))
                .andExpect(jsonPath("$.counterAccountId").value(expenseAccount.getId()))
                .andExpect(jsonPath("$.payeePayer").value("Vendor C"));
    }

    @Test
    void createTransaction_withExchangeRate_calculatesBaseAmount() throws Exception {
        CreateBankTransactionRequest request = CreateBankTransactionRequest.builder()
                .bankAccountId(checkingAccount.getId())
                .transactionDate(LocalDate.of(2026, 4, 22))
                .transactionType(BankTransactionType.RECEIPT)
                .description("Foreign currency receipt")
                .amount(new BigDecimal("1000"))
                .isDebit(false)
                .currency("USD")
                .exchangeRate(new BigDecimal("12500"))
                .payeePayer("International Client")
                .build();

        mockMvc.perform(post("/api/v1/finance/bank-transactions")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baseAmount").value(12500000))
                .andExpect(jsonPath("$.exchangeRate").value(12500));
    }

    // ---- POST /api/v1/finance/bank-transactions/transfer ----

    @Test
    void createTransfer_createsTwoTransactions() throws Exception {
        mockMvc.perform(post("/api/v1/finance/bank-transactions/transfer")
                        .param("fromAccountId", checkingAccount.getId().toString())
                        .param("toAccountId", savingsAccount.getId().toString())
                        .param("amount", "15000000")
                        .param("transactionDate", "2026-04-25")
                        .param("description", "Monthly savings transfer")
                        .with(fullAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.status").value("CLEARED"))
                .andExpect(jsonPath("$.debitAmount").value(15000000))
                .andExpect(jsonPath("$.bankAccountId").value(checkingAccount.getId()))
                .andExpect(jsonPath("$.transferToAccountId").value(savingsAccount.getId()));

        // Verify the matching TRANSFER_IN was also created on the savings account
        mockMvc.perform(get("/api/v1/finance/bank-transactions/by-account/" + savingsAccount.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].transactionType").value("TRANSFER_IN"))
                .andExpect(jsonPath("$.content[0].status").value("CLEARED"))
                .andExpect(jsonPath("$.content[0].creditAmount").value(15000000));
    }

    @Test
    void createTransfer_withoutDescription_usesDefaultDescription() throws Exception {
        mockMvc.perform(post("/api/v1/finance/bank-transactions/transfer")
                        .param("fromAccountId", checkingAccount.getId().toString())
                        .param("toAccountId", savingsAccount.getId().toString())
                        .param("amount", "5000000")
                        .param("transactionDate", "2026-04-26")
                        .with(fullAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(containsString("Transfer to")));
    }

    // ---- PUT /api/v1/finance/bank-transactions/{id}/clear ----

    @Test
    void clearTransaction_pendingToCleared() throws Exception {
        mockMvc.perform(put("/api/v1/finance/bank-transactions/" + depositTx.getId() + "/clear")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(depositTx.getId()))
                .andExpect(jsonPath("$.status").value("CLEARED"));

        // Verify persistence
        mockMvc.perform(get("/api/v1/finance/bank-transactions/" + depositTx.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEARED"));
    }

    @Test
    void clearTransaction_alreadyCleared_returns400() throws Exception {
        // First clear it
        mockMvc.perform(put("/api/v1/finance/bank-transactions/" + depositTx.getId() + "/clear")
                        .with(fullAuth()))
                .andExpect(status().isOk());

        // Try to clear again
        mockMvc.perform(put("/api/v1/finance/bank-transactions/" + depositTx.getId() + "/clear")
                        .with(fullAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/finance/bank-transactions/{id}/void ----

    @Test
    void voidTransaction_pendingToVoided() throws Exception {
        mockMvc.perform(put("/api/v1/finance/bank-transactions/" + withdrawalTx.getId() + "/void")
                        .param("reason", "Duplicate entry")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(withdrawalTx.getId()))
                .andExpect(jsonPath("$.status").value("VOIDED"))
                .andExpect(jsonPath("$.notes").value(containsString("Voided: Duplicate entry")));
    }

    @Test
    void voidTransaction_clearedToVoided() throws Exception {
        // Clear first
        mockMvc.perform(put("/api/v1/finance/bank-transactions/" + depositTx.getId() + "/clear")
                        .with(fullAuth()))
                .andExpect(status().isOk());

        // Void the cleared transaction
        mockMvc.perform(put("/api/v1/finance/bank-transactions/" + depositTx.getId() + "/void")
                        .param("reason", "Customer refund")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOIDED"));
    }

    @Test
    void voidTransaction_alreadyVoided_returns400() throws Exception {
        // Void it once
        mockMvc.perform(put("/api/v1/finance/bank-transactions/" + withdrawalTx.getId() + "/void")
                        .with(fullAuth()))
                .andExpect(status().isOk());

        // Try to void again
        mockMvc.perform(put("/api/v1/finance/bank-transactions/" + withdrawalTx.getId() + "/void")
                        .with(fullAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/v1/finance/bank-transactions/cash-flow/{bankAccountId} ----

    @Test
    void getCashFlow_returnsCashFlowData() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-transactions/cash-flow/" + checkingAccount.getId())
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-30")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodStart").value("2026-04-01"))
                .andExpect(jsonPath("$.periodEnd").value("2026-04-30"))
                .andExpect(jsonPath("$.totalInflows").value(5000000))
                .andExpect(jsonPath("$.totalOutflows").value(1000000))
                .andExpect(jsonPath("$.netCashFlow").value(4000000))
                .andExpect(jsonPath("$.inflowDetails").isArray())
                .andExpect(jsonPath("$.outflowDetails").isArray());
    }

    @Test
    void getCashFlow_emptyPeriod_returnsZeros() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-transactions/cash-flow/" + savingsAccount.getId())
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-30")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInflows").value(0))
                .andExpect(jsonPath("$.totalOutflows").value(0))
                .andExpect(jsonPath("$.netCashFlow").value(0));
    }

    // ---- Permission checks ----

    @Test
    void getTransactions_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-transactions")
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTransaction_viewOnly_returns403() throws Exception {
        CreateBankTransactionRequest request = CreateBankTransactionRequest.builder()
                .bankAccountId(checkingAccount.getId())
                .transactionDate(LocalDate.of(2026, 4, 20))
                .transactionType(BankTransactionType.DEPOSIT)
                .description("Should fail")
                .amount(new BigDecimal("1000000"))
                .isDebit(false)
                .build();

        mockMvc.perform(post("/api/v1/finance/bank-transactions")
                        .with(viewOnlyAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTransfer_viewOnly_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/finance/bank-transactions/transfer")
                        .param("fromAccountId", checkingAccount.getId().toString())
                        .param("toAccountId", savingsAccount.getId().toString())
                        .param("amount", "5000000")
                        .param("transactionDate", "2026-04-25")
                        .with(viewOnlyAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void voidTransaction_viewOnly_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/finance/bank-transactions/" + depositTx.getId() + "/void")
                        .with(viewOnlyAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void clearTransaction_viewOnly_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/finance/bank-transactions/" + depositTx.getId() + "/clear")
                        .with(viewOnlyAuth()))
                .andExpect(status().isForbidden());
    }

    // ---- Auto-number generation ----

    @Test
    void createTransaction_generatesSequentialTransactionNumbers() throws Exception {
        CreateBankTransactionRequest request1 = CreateBankTransactionRequest.builder()
                .bankAccountId(checkingAccount.getId())
                .transactionDate(LocalDate.of(2026, 4, 23))
                .transactionType(BankTransactionType.DEPOSIT)
                .description("First new transaction")
                .amount(new BigDecimal("1000000"))
                .isDebit(false)
                .build();

        String result1 = mockMvc.perform(post("/api/v1/finance/bank-transactions")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionNumber").value("BT-000003"))
                .andReturn().getResponse().getContentAsString();

        CreateBankTransactionRequest request2 = CreateBankTransactionRequest.builder()
                .bankAccountId(checkingAccount.getId())
                .transactionDate(LocalDate.of(2026, 4, 24))
                .transactionType(BankTransactionType.BANK_CHARGE)
                .description("Second new transaction")
                .amount(new BigDecimal("50000"))
                .isDebit(true)
                .build();

        mockMvc.perform(post("/api/v1/finance/bank-transactions")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionNumber").value("BT-000004"));
    }
}
