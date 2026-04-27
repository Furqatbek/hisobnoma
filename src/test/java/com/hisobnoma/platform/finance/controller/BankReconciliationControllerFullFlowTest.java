package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateBankReconciliationRequest;
import com.hisobnoma.platform.finance.dto.ReconcileTransactionsRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
import com.hisobnoma.platform.finance.repository.BankReconciliationRepository;
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
class BankReconciliationControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/finance/bank-reconciliations";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BankReconciliationRepository reconciliationRepository;
    @Autowired private BankAccountRepository bankAccountRepository;
    @Autowired private BankTransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User adminUser;
    private BankAccount bankAccount;
    private BankReconciliation draftReconciliation;
    private BankTransaction pendingTransaction;
    private BankTransaction clearedTransaction;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Recon Tenant").code("FULLFLOW_RECON").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("reconadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        bankAccount = bankAccountRepository.saveAndFlush(BankAccount.builder()
                .accountCode("BA-001")
                .accountName("Main Checking")
                .accountType(BankAccountType.CHECKING)
                .currency("UZS")
                .currentBalance(new BigDecimal("10000.0000"))
                .availableBalance(new BigDecimal("10000.0000"))
                .openingBalance(BigDecimal.ZERO)
                .active(true)
                .tenantId(tenant.getId())
                .build());

        draftReconciliation = reconciliationRepository.saveAndFlush(BankReconciliation.builder()
                .bankAccount(bankAccount)
                .reconciliationNumber("REC-000001")
                .statementDate(LocalDate.of(2026, 3, 31))
                .statementStartDate(LocalDate.of(2026, 3, 1))
                .statementEndDate(LocalDate.of(2026, 3, 31))
                .status(ReconciliationStatus.DRAFT)
                .openingBalance(BigDecimal.ZERO)
                .statementEndingBalance(new BigDecimal("10000.0000"))
                .bookBalance(new BigDecimal("10000.0000"))
                .clearedDeposits(BigDecimal.ZERO)
                .clearedWithdrawals(BigDecimal.ZERO)
                .outstandingDeposits(BigDecimal.ZERO)
                .outstandingWithdrawals(BigDecimal.ZERO)
                .difference(BigDecimal.ZERO)
                .transactionsCleared(0)
                .transactionsOutstanding(0)
                .tenantId(tenant.getId())
                .build());

        pendingTransaction = transactionRepository.saveAndFlush(BankTransaction.builder()
                .bankAccount(bankAccount)
                .transactionNumber("TXN-000001")
                .transactionDate(LocalDate.of(2026, 3, 15))
                .transactionType(BankTransactionType.DEPOSIT)
                .status(BankTransactionStatus.PENDING)
                .description("Customer deposit")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("5000.0000"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId())
                .build());

        clearedTransaction = transactionRepository.saveAndFlush(BankTransaction.builder()
                .bankAccount(bankAccount)
                .transactionNumber("TXN-000002")
                .transactionDate(LocalDate.of(2026, 3, 20))
                .transactionType(BankTransactionType.WITHDRAWAL)
                .status(BankTransactionStatus.CLEARED)
                .description("Supplier payment")
                .debitAmount(new BigDecimal("3000.0000"))
                .creditAmount(BigDecimal.ZERO)
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId())
                .build());

        entityManager.clear();
    }

    private RequestPostProcessor reconcileAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("FINANCE_BANK_RECONCILE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/finance/bank-reconciliations ----

    @Test
    void getReconciliations_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(reconcileAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].reconciliationNumber").value("REC-000001"));
    }

    // ---- GET /api/v1/finance/bank-reconciliations/by-account/{bankAccountId} ----

    @Test
    void getReconciliationsByBankAccount_returnsFiltered() throws Exception {
        // Create a second bank account with its own reconciliation
        BankAccount otherAccount = bankAccountRepository.saveAndFlush(BankAccount.builder()
                .accountCode("BA-002").accountName("Savings")
                .accountType(BankAccountType.SAVINGS).currency("UZS")
                .currentBalance(BigDecimal.ZERO).availableBalance(BigDecimal.ZERO)
                .openingBalance(BigDecimal.ZERO).active(true)
                .tenantId(tenant.getId()).build());

        reconciliationRepository.saveAndFlush(BankReconciliation.builder()
                .bankAccount(otherAccount)
                .reconciliationNumber("REC-000002")
                .statementDate(LocalDate.of(2026, 3, 31))
                .statementStartDate(LocalDate.of(2026, 3, 1))
                .statementEndDate(LocalDate.of(2026, 3, 31))
                .status(ReconciliationStatus.DRAFT)
                .openingBalance(BigDecimal.ZERO)
                .statementEndingBalance(BigDecimal.ZERO)
                .bookBalance(BigDecimal.ZERO)
                .difference(BigDecimal.ZERO)
                .transactionsCleared(0).transactionsOutstanding(0)
                .tenantId(tenant.getId()).build());

        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/by-account/" + bankAccount.getId()).with(reconcileAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].reconciliationNumber").value("REC-000001"));
    }

    // ---- GET /api/v1/finance/bank-reconciliations/{id} ----

    @Test
    void getReconciliationById_returnsReconciliation() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + draftReconciliation.getId()).with(reconcileAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draftReconciliation.getId()))
                .andExpect(jsonPath("$.reconciliationNumber").value("REC-000001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.bankAccountId").value(bankAccount.getId()));
    }

    // ---- GET /api/v1/finance/bank-reconciliations/unreconciled/{bankAccountId} ----

    @Test
    void getUnreconciledTransactions_returnsUnreconciled() throws Exception {
        mockMvc.perform(get(BASE_URL + "/unreconciled/" + bankAccount.getId())
                        .param("endDate", "2026-03-31")
                        .with(reconcileAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].transactionNumber",
                        containsInAnyOrder("TXN-000001", "TXN-000002")));
    }

    // ---- POST /api/v1/finance/bank-reconciliations ----

    @Test
    void createReconciliation_validRequest_returnsCreated() throws Exception {
        // Remove the existing draft so there is no in-progress reconciliation
        reconciliationRepository.delete(draftReconciliation);
        reconciliationRepository.flush();
        entityManager.clear();

        CreateBankReconciliationRequest request = CreateBankReconciliationRequest.builder()
                .bankAccountId(bankAccount.getId())
                .statementDate(LocalDate.of(2026, 4, 30))
                .statementStartDate(LocalDate.of(2026, 4, 1))
                .statementEndDate(LocalDate.of(2026, 4, 30))
                .openingBalance(new BigDecimal("10000.00"))
                .statementEndingBalance(new BigDecimal("12000.00"))
                .notes("April reconciliation")
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(reconcileAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reconciliationNumber").value(startsWith("REC-")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.bankAccountId").value(bankAccount.getId()))
                .andExpect(jsonPath("$.statementEndingBalance").value(closeTo(12000.00, 0.01)))
                .andExpect(jsonPath("$.notes").value("April reconciliation"));
    }

    // ---- PUT /api/v1/finance/bank-reconciliations/{id}/start ----

    @Test
    void startReconciliation_draftToInProgress() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + draftReconciliation.getId() + "/start")
                        .with(reconcileAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draftReconciliation.getId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // ---- POST /api/v1/finance/bank-reconciliations/reconcile-transactions ----

    @Test
    void reconcileTransactions_marksTransactionsReconciled() throws Exception {
        ReconcileTransactionsRequest request = ReconcileTransactionsRequest.builder()
                .reconciliationId(draftReconciliation.getId())
                .transactionIds(List.of(clearedTransaction.getId()))
                .clear(true)
                .build();

        mockMvc.perform(post(BASE_URL + "/reconcile-transactions")
                        .with(reconcileAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.transactionsCleared").value(greaterThanOrEqualTo(1)));
    }

    // ---- PUT /api/v1/finance/bank-reconciliations/{id}/cancel ----

    @Test
    void cancelReconciliation_nonCompleted_returns200() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + draftReconciliation.getId() + "/cancel")
                        .param("reason", "Created by mistake")
                        .with(reconcileAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Created by mistake"));
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_cannotReconcileWithoutPermission() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get(BASE_URL).with(authentication(auth)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + draftReconciliation.getId()).with(authentication(auth)))
                .andExpect(status().isForbidden());

        CreateBankReconciliationRequest createReq = CreateBankReconciliationRequest.builder()
                .bankAccountId(bankAccount.getId())
                .statementDate(LocalDate.of(2026, 5, 31))
                .statementStartDate(LocalDate.of(2026, 5, 1))
                .statementEndDate(LocalDate.of(2026, 5, 31))
                .openingBalance(BigDecimal.ZERO)
                .statementEndingBalance(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + draftReconciliation.getId() + "/start")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + draftReconciliation.getId() + "/cancel")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }
}
