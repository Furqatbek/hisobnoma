package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateAccountRequest;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.AccountType;
import com.hisobnoma.platform.finance.entity.NormalBalance;
import com.hisobnoma.platform.finance.repository.AccountRepository;
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
class AccountControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User adminUser;
    private Account rootAsset;
    private Account currentAssets;
    private Account cashAccount;
    private Account rootLiability;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Account Tenant").code("FULLFLOW_ACC").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("accountadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        rootAsset = accountRepository.saveAndFlush(Account.builder()
                .code("1000").name("Assets").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("1000")
                .active(true).allowsDirectPosting(false)
                .tenantId(tenant.getId()).build());

        currentAssets = Account.builder()
                .code("1100").name("Current Assets").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).active(true).allowsDirectPosting(false)
                .tenantId(tenant.getId()).build();
        rootAsset.addChildAccount(currentAssets);
        currentAssets = accountRepository.saveAndFlush(currentAssets);

        cashAccount = Account.builder()
                .code("1110").name("Cash").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build();
        currentAssets.addChildAccount(cashAccount);
        cashAccount = accountRepository.saveAndFlush(cashAccount);

        rootLiability = accountRepository.saveAndFlush(Account.builder()
                .code("2000").name("Liabilities").accountType(AccountType.LIABILITY)
                .normalBalance(NormalBalance.CREDIT).accountLevel(1).fullPath("2000")
                .active(true).allowsDirectPosting(false)
                .tenantId(tenant.getId()).build());
    }

    private RequestPostProcessor glAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_GL_VIEW"),
                        new SimpleGrantedAuthority("FINANCE_GL_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/finance/accounts ----

    @Test
    void getAccounts_returnsPaginatedList() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts").with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(4)));
    }

    // ---- GET /api/v1/finance/accounts/all ----

    @Test
    void getAllAccounts_returnsAllForTenant() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/all").with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)));
    }

    // ---- GET /api/v1/finance/accounts/roots ----

    @Test
    void getRootAccounts_returnsOnlyRoots() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/roots").with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- GET /api/v1/finance/accounts/{id}/children ----

    @Test
    void getChildAccounts_returnsChildrenOfParent() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/" + rootAsset.getId() + "/children")
                        .with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("1100"));
    }

    // ---- GET /api/v1/finance/accounts/active ----

    @Test
    void getActiveAccounts_returnsOnlyActive() throws Exception {
        Account inactive = accountRepository.saveAndFlush(Account.builder()
                .code("9999").name("Inactive").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("9999")
                .active(false).tenantId(tenant.getId()).build());

        mockMvc.perform(get("/api/v1/finance/accounts/active").with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    // ---- GET /api/v1/finance/accounts/postable ----

    @Test
    void getPostableAccounts_returnsOnlyPostable() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/postable").with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("1110"));
    }

    // ---- GET /api/v1/finance/accounts/type/{accountType} ----

    @Test
    void getAccountsByType_returnsFilteredByType() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/type/ASSET").with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getAccountsByType_liabilityType_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/type/LIABILITY").with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---- GET /api/v1/finance/accounts/{id} ----

    @Test
    void getAccount_existingId_returnsAccount() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/" + cashAccount.getId()).with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1110"))
                .andExpect(jsonPath("$.name").value("Cash"))
                .andExpect(jsonPath("$.accountType").value("ASSET"))
                .andExpect(jsonPath("$.normalBalance").value("DEBIT"));
    }

    @Test
    void getAccount_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/99999").with(glAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/accounts/{id}/tree ----

    @Test
    void getAccountWithChildren_returnsTree() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/" + rootAsset.getId() + "/tree")
                        .with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.childAccounts").isArray());
    }

    // ---- GET /api/v1/finance/accounts/code/{code} ----

    @Test
    void getAccountByCode_existingCode_returnsAccount() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/code/1110").with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cash"));
    }

    @Test
    void getAccountByCode_nonExistentCode_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/code/XXXX").with(glAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/accounts/search ----

    @Test
    void searchAccounts_matchingQuery_returnsResults() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/search")
                        .param("q", "Cash").with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void searchAccounts_noMatch_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/finance/accounts/search")
                        .param("q", "NonExistentXYZ").with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ---- POST /api/v1/finance/accounts ----

    @Test
    void createAccount_validRootAccount_returnsCreated() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("3000").name("Equity").accountType(AccountType.EQUITY)
                .allowsDirectPosting(false).build();

        mockMvc.perform(post("/api/v1/finance/accounts")
                        .with(glAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("3000"))
                .andExpect(jsonPath("$.normalBalance").value("CREDIT"));
    }

    @Test
    void createAccount_withParent_setsHierarchy() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1120").name("Bank Accounts").accountType(AccountType.ASSET)
                .parentAccountId(currentAssets.getId()).allowsDirectPosting(true).build();

        mockMvc.perform(post("/api/v1/finance/accounts")
                        .with(glAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentAccountId").value(currentAssets.getId()))
                .andExpect(jsonPath("$.accountLevel").value(3));
    }

    @Test
    void createAccount_duplicateCode_returns400() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1000").name("Duplicate").accountType(AccountType.ASSET).build();

        mockMvc.perform(post("/api/v1/finance/accounts")
                        .with(glAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccount_missingCode_returns400() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .name("No Code Account").accountType(AccountType.ASSET).build();

        mockMvc.perform(post("/api/v1/finance/accounts")
                        .with(glAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccount_parentTypeMismatch_returns400() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("2100").name("Current Liabilities").accountType(AccountType.LIABILITY)
                .parentAccountId(rootAsset.getId()).build();

        mockMvc.perform(post("/api/v1/finance/accounts")
                        .with(glAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccount_withOpeningBalance_setsBalance() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1130").name("Accounts Receivable").accountType(AccountType.ASSET)
                .parentAccountId(currentAssets.getId())
                .openingBalance(new BigDecimal("50000.00")).allowsDirectPosting(true).build();

        mockMvc.perform(post("/api/v1/finance/accounts")
                        .with(glAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.openingBalance").value(50000.00));
    }

    // ---- PUT /api/v1/finance/accounts/{id} ----

    @Test
    void updateAccount_validRequest_updatesFields() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1110").name("Cash on Hand").accountType(AccountType.ASSET)
                .allowsDirectPosting(true).build();

        mockMvc.perform(put("/api/v1/finance/accounts/" + cashAccount.getId())
                        .with(glAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cash on Hand"));
    }

    @Test
    void updateAccount_systemAccount_returns400() throws Exception {
        Account sysAccount = accountRepository.saveAndFlush(Account.builder()
                .code("SYS1").name("System Account").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("SYS1")
                .systemAccount(true).active(true).tenantId(tenant.getId()).build());

        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("SYS1").name("Modified System").accountType(AccountType.ASSET).build();

        mockMvc.perform(put("/api/v1/finance/accounts/" + sysAccount.getId())
                        .with(glAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAccount_nonExistentId_returns404() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1110").name("Cash").accountType(AccountType.ASSET).build();

        mockMvc.perform(put("/api/v1/finance/accounts/99999")
                        .with(glAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /api/v1/finance/accounts/{id} ----

    @Test
    void deleteAccount_leafAccount_returns204() throws Exception {
        Account leaf = accountRepository.saveAndFlush(Account.builder()
                .code("9001").name("Deletable").accountType(AccountType.EXPENSE)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("9001")
                .active(true).tenantId(tenant.getId()).build());

        mockMvc.perform(delete("/api/v1/finance/accounts/" + leaf.getId())
                        .with(glAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAccount_nonLeafAccount_returns400() throws Exception {
        mockMvc.perform(delete("/api/v1/finance/accounts/" + rootAsset.getId())
                        .with(glAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAccount_systemAccount_returns400() throws Exception {
        Account sysAccount = accountRepository.saveAndFlush(Account.builder()
                .code("SYS2").name("System Delete Test").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("SYS2")
                .systemAccount(true).active(true).tenantId(tenant.getId()).build());

        mockMvc.perform(delete("/api/v1/finance/accounts/" + sysAccount.getId())
                        .with(glAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAccount_withTransactions_returns400() throws Exception {
        Account withTx = accountRepository.saveAndFlush(Account.builder()
                .code("9002").name("Has Transactions").accountType(AccountType.EXPENSE)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("9002")
                .ytdDebit(new BigDecimal("1000")).ytdCredit(BigDecimal.ZERO)
                .active(true).tenantId(tenant.getId()).build());

        mockMvc.perform(delete("/api/v1/finance/accounts/" + withTx.getId())
                        .with(glAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/finance/accounts/{id}/activate ----

    @Test
    void activateAccount_deactivatedAccount_activates() throws Exception {
        Account inactive = accountRepository.saveAndFlush(Account.builder()
                .code("9003").name("Inactive Account").accountType(AccountType.EXPENSE)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("9003")
                .active(false).tenantId(tenant.getId()).build());

        mockMvc.perform(put("/api/v1/finance/accounts/" + inactive.getId() + "/activate")
                        .with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---- PUT /api/v1/finance/accounts/{id}/deactivate ----

    @Test
    void deactivateAccount_leafAccount_deactivates() throws Exception {
        mockMvc.perform(put("/api/v1/finance/accounts/" + cashAccount.getId() + "/deactivate")
                        .with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deactivateAccount_systemAccount_returns400() throws Exception {
        Account sysAccount = accountRepository.saveAndFlush(Account.builder()
                .code("SYS3").name("System Deact Test").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("SYS3")
                .systemAccount(true).active(true).tenantId(tenant.getId()).build());

        mockMvc.perform(put("/api/v1/finance/accounts/" + sysAccount.getId() + "/deactivate")
                        .with(glAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /api/v1/finance/accounts/generate-default ----

    @Test
    void generateDefaultAccounts_emptyTenant_generatesDefaults() throws Exception {
        Tenant emptyTenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Empty Tenant").code("EMPTY_ACC").active(true)
                .maxUsers(100).maxLocations(10).build());
        User emptyUser = userRepository.saveAndFlush(User.builder()
                .username("emptyaccadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(emptyTenant.getId()).enabled(true).build());

        UserPrincipal principal = new UserPrincipal(
                emptyUser.getId(), emptyUser.getUsername(), "admin123", emptyTenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("FINANCE_GL_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        mockMvc.perform(post("/api/v1/finance/accounts/generate-default")
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(30)));
    }

    @Test
    void generateDefaultAccounts_existingAccounts_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/finance/accounts/generate-default")
                        .with(glAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- Permission checks ----

    @Test
    void getAccounts_noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/finance/accounts")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAccount_viewOnlyPermission_returns403() throws Exception {
        UserPrincipal viewOnly = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("FINANCE_GL_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                viewOnly, null, viewOnly.getAuthorities());

        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("3000").name("Equity").accountType(AccountType.EQUITY).build();

        mockMvc.perform(post("/api/v1/finance/accounts")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---- Full CRUD lifecycle ----

    @Test
    void fullCrudLifecycle() throws Exception {
        CreateAccountRequest createReq = CreateAccountRequest.builder()
                .code("4000").name("Revenue").accountType(AccountType.REVENUE)
                .allowsDirectPosting(true).build();

        String result = mockMvc.perform(post("/api/v1/finance/accounts")
                        .with(glAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(result).get("id").asLong();

        mockMvc.perform(get("/api/v1/finance/accounts/code/4000").with(glAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Revenue"))
                .andExpect(jsonPath("$.normalBalance").value("CREDIT"));

        CreateAccountRequest updateReq = CreateAccountRequest.builder()
                .code("4000").name("Total Revenue").accountType(AccountType.REVENUE)
                .allowsDirectPosting(false).build();

        mockMvc.perform(put("/api/v1/finance/accounts/" + createdId)
                        .with(glAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Total Revenue"));

        mockMvc.perform(put("/api/v1/finance/accounts/" + createdId + "/deactivate")
                        .with(glAuth()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/finance/accounts/" + createdId + "/activate")
                        .with(glAuth()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/finance/accounts/" + createdId)
                        .with(glAuth()))
                .andExpect(status().isNoContent());
    }
}
