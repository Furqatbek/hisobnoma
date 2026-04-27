package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateBankAccountRequest;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.AccountType;
import com.hisobnoma.platform.finance.entity.BankAccount;
import com.hisobnoma.platform.finance.entity.BankAccountType;
import com.hisobnoma.platform.finance.entity.NormalBalance;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BankAccountControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BankAccountRepository bankAccountRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User adminUser;
    private Account glAccount;
    private BankAccount checkingAccount;
    private BankAccount cashAccount;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("BankAcct Tenant").code("FULLFLOW_BA").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("bankacctadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        glAccount = accountRepository.saveAndFlush(Account.builder()
                .code("1120").name("Bank Accounts").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("1120")
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

        cashAccount = bankAccountRepository.saveAndFlush(BankAccount.builder()
                .accountCode("BA-002").accountName("Petty Cash")
                .accountType(BankAccountType.PETTY_CASH)
                .currency("UZS")
                .currentBalance(new BigDecimal("5000000.0000"))
                .availableBalance(new BigDecimal("5000000.0000"))
                .openingBalance(new BigDecimal("5000000.0000"))
                .active(true).defaultAccount(false)
                .allowPayments(true).allowReceipts(true)
                .tenantId(tenant.getId()).build());
    }

    private RequestPostProcessor bankAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_BANK_VIEW"),
                        new SimpleGrantedAuthority("FINANCE_BANK_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/finance/bank-accounts ----

    @Test
    void getBankAccounts_returnsPaginatedList() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/bank-accounts/all ----

    @Test
    void getAllBankAccounts_returnsAll() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/all").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/bank-accounts/active ----

    @Test
    void getActiveBankAccounts_returnsOnlyActive() throws Exception {
        bankAccountRepository.saveAndFlush(BankAccount.builder()
                .accountCode("BA-INACT").accountName("Inactive")
                .accountType(BankAccountType.SAVINGS)
                .currency("UZS").active(false)
                .tenantId(tenant.getId()).build());

        mockMvc.perform(get("/api/v1/finance/bank-accounts/active").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- GET /api/v1/finance/bank-accounts/by-type/{type} ----

    @Test
    void getBankAccountsByType_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/by-type/CHECKING").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountCode").value("BA-001"));
    }

    @Test
    void getBankAccountsByType_pettyCash_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/by-type/PETTY_CASH").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---- GET /api/v1/finance/bank-accounts/payment-accounts ----

    @Test
    void getPaymentAccounts_returnsPaymentEnabled() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/payment-accounts").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- GET /api/v1/finance/bank-accounts/receipt-accounts ----

    @Test
    void getReceiptAccounts_returnsReceiptEnabled() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/receipt-accounts").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- GET /api/v1/finance/bank-accounts/default ----

    @Test
    void getDefaultBankAccount_returnsDefault() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/default").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountCode").value("BA-001"))
                .andExpect(jsonPath("$.defaultAccount").value(true));
    }

    // ---- GET /api/v1/finance/bank-accounts/{id} ----

    @Test
    void getBankAccount_existingId_returnsAccount() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/" + checkingAccount.getId())
                        .with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountCode").value("BA-001"))
                .andExpect(jsonPath("$.glAccountId").value(glAccount.getId()))
                .andExpect(jsonPath("$.glAccountCode").value("1120"));
    }

    @Test
    void getBankAccount_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/99999").with(bankAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/bank-accounts/code/{code} ----

    @Test
    void getBankAccountByCode_existingCode_returnsAccount() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/code/BA-001").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountName").value("Main Checking"));
    }

    @Test
    void getBankAccountByCode_nonExistentCode_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/code/NONEXIST").with(bankAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/bank-accounts/search ----

    @Test
    void searchBankAccounts_byName_returnsMatching() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/search")
                        .param("query", "Main").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void searchBankAccounts_byBankName_returnsMatching() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts/search")
                        .param("query", "National").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    // ---- POST /api/v1/finance/bank-accounts ----

    @Test
    void createBankAccount_validRequest_returnsCreated() throws Exception {
        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountCode("BA-003").accountName("Savings Account")
                .accountType(BankAccountType.SAVINGS)
                .bankName("Savings Bank").currency("UZS")
                .openingBalance(new BigDecimal("10000000.00"))
                .allowPayments(false).allowReceipts(true).build();

        mockMvc.perform(post("/api/v1/finance/bank-accounts")
                        .with(bankAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountCode").value("BA-003"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"));
    }

    @Test
    void createBankAccount_withGlAccount_linksToGl() throws Exception {
        Account newGl = accountRepository.saveAndFlush(Account.builder()
                .code("1121").name("Savings GL").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("1121")
                .active(true).tenantId(tenant.getId()).build());

        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountCode("BA-004").accountName("GL Linked")
                .accountType(BankAccountType.SAVINGS)
                .currency("UZS").glAccountId(newGl.getId()).build();

        mockMvc.perform(post("/api/v1/finance/bank-accounts")
                        .with(bankAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.glAccountId").value(newGl.getId()));
    }

    @Test
    void createBankAccount_duplicateCode_returns400() throws Exception {
        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountCode("BA-001").accountName("Duplicate")
                .accountType(BankAccountType.CHECKING).build();

        mockMvc.perform(post("/api/v1/finance/bank-accounts")
                        .with(bankAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBankAccount_missingCode_returns400() throws Exception {
        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountName("No Code").accountType(BankAccountType.CHECKING).build();

        mockMvc.perform(post("/api/v1/finance/bank-accounts")
                        .with(bankAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBankAccount_missingAccountType_returns400() throws Exception {
        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountCode("BA-005").accountName("No Type").build();

        mockMvc.perform(post("/api/v1/finance/bank-accounts")
                        .with(bankAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/finance/bank-accounts/{id} ----

    @Test
    void updateBankAccount_validRequest_updatesFields() throws Exception {
        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountCode("BA-002").accountName("Updated Petty Cash")
                .accountType(BankAccountType.PETTY_CASH).currency("UZS").build();

        mockMvc.perform(put("/api/v1/finance/bank-accounts/" + cashAccount.getId())
                        .with(bankAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountName").value("Updated Petty Cash"));
    }

    @Test
    void updateBankAccount_nonExistentId_returns404() throws Exception {
        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountCode("BA-XXX").accountName("Non Existent")
                .accountType(BankAccountType.CHECKING).build();

        mockMvc.perform(put("/api/v1/finance/bank-accounts/99999")
                        .with(bankAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /api/v1/finance/bank-accounts/{id}/activate ----

    @Test
    void activateBankAccount_deactivatedAccount_activates() throws Exception {
        BankAccount inactive = bankAccountRepository.saveAndFlush(BankAccount.builder()
                .accountCode("BA-INACT2").accountName("Inactive2")
                .accountType(BankAccountType.SAVINGS).currency("UZS")
                .active(false).tenantId(tenant.getId()).build());

        mockMvc.perform(put("/api/v1/finance/bank-accounts/" + inactive.getId() + "/activate")
                        .with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---- PUT /api/v1/finance/bank-accounts/{id}/deactivate ----

    @Test
    void deactivateBankAccount_nonDefaultZeroBalance_deactivates() throws Exception {
        BankAccount zeroBal = bankAccountRepository.saveAndFlush(BankAccount.builder()
                .accountCode("BA-ZERO").accountName("Zero Balance")
                .accountType(BankAccountType.SAVINGS).currency("UZS")
                .currentBalance(BigDecimal.ZERO)
                .active(true).defaultAccount(false)
                .tenantId(tenant.getId()).build());

        mockMvc.perform(put("/api/v1/finance/bank-accounts/" + zeroBal.getId() + "/deactivate")
                        .with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deactivateBankAccount_defaultAccount_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/finance/bank-accounts/" + checkingAccount.getId() + "/deactivate")
                        .with(bankAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateBankAccount_nonZeroBalance_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/finance/bank-accounts/" + cashAccount.getId() + "/deactivate")
                        .with(bankAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/finance/bank-accounts/{id}/set-default ----

    @Test
    void setDefaultBankAccount_switchesDefault() throws Exception {
        mockMvc.perform(put("/api/v1/finance/bank-accounts/" + cashAccount.getId() + "/set-default")
                        .with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAccount").value(true));

        mockMvc.perform(get("/api/v1/finance/bank-accounts/" + checkingAccount.getId())
                        .with(bankAuth()))
                .andExpect(jsonPath("$.defaultAccount").value(false));
    }

    // ---- Permission checks ----

    @Test
    void getBankAccounts_noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/finance/bank-accounts")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBankAccount_viewOnlyPermission_returns403() throws Exception {
        UserPrincipal viewOnly = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("FINANCE_BANK_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                viewOnly, null, viewOnly.getAuthorities());

        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountCode("BA-NOPERM").accountName("No Perm")
                .accountType(BankAccountType.CHECKING).build();

        mockMvc.perform(post("/api/v1/finance/bank-accounts")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---- Full CRUD lifecycle ----

    @Test
    void fullCrudLifecycle() throws Exception {
        CreateBankAccountRequest createReq = CreateBankAccountRequest.builder()
                .accountCode("BA-LIFE").accountName("Lifecycle Account")
                .accountType(BankAccountType.CHECKING)
                .bankName("Lifecycle Bank").currency("UZS")
                .openingBalance(new BigDecimal("1000000.00"))
                .allowPayments(true).allowReceipts(true).build();

        String result = mockMvc.perform(post("/api/v1/finance/bank-accounts")
                        .with(bankAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(result).get("id").asLong();

        mockMvc.perform(get("/api/v1/finance/bank-accounts/code/BA-LIFE").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountName").value("Lifecycle Account"));

        CreateBankAccountRequest updateReq = CreateBankAccountRequest.builder()
                .accountCode("BA-LIFE").accountName("Updated Lifecycle")
                .accountType(BankAccountType.CHECKING)
                .bankName("Updated Bank").currency("UZS").build();

        mockMvc.perform(put("/api/v1/finance/bank-accounts/" + createdId)
                        .with(bankAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankName").value("Updated Bank"));

        mockMvc.perform(get("/api/v1/finance/bank-accounts/search")
                        .param("query", "Lifecycle").with(bankAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}
