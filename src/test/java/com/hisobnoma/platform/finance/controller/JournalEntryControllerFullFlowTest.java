package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateJournalEntryRequest;
import com.hisobnoma.platform.finance.dto.CreateJournalLineRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.FiscalPeriodRepository;
import com.hisobnoma.platform.finance.repository.FiscalYearRepository;
import com.hisobnoma.platform.finance.repository.JournalEntryRepository;
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
class JournalEntryControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/finance/journal-entries";

    private Tenant tenant;
    private User adminUser;
    private FiscalYear fiscalYear;
    private FiscalPeriod fiscalPeriod;
    private Account assetAccount;
    private Account liabilityAccount;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("JE Test Tenant").code("FULLFLOW_JE").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("jeadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        fiscalYear = fiscalYearRepository.saveAndFlush(FiscalYear.builder()
                .year(2024).name("Fiscal Year 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .status(PeriodStatus.OPEN).current(true).closed(false)
                .tenantId(tenant.getId()).build());

        fiscalPeriod = fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                .fiscalYear(fiscalYear).periodNumber(1).name("January")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN).adjustmentPeriod(false)
                .tenantId(tenant.getId()).build());

        assetAccount = accountRepository.saveAndFlush(Account.builder()
                .code("1000").name("Cash")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true).systemAccount(false)
                .tenantId(tenant.getId()).build());

        liabilityAccount = accountRepository.saveAndFlush(Account.builder()
                .code("2000").name("Accounts Payable")
                .accountType(AccountType.LIABILITY)
                .normalBalance(NormalBalance.CREDIT)
                .active(true).allowsDirectPosting(true).systemAccount(false)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_JE_VIEW"),
                        new SimpleGrantedAuthority("FINANCE_JE_CREATE"),
                        new SimpleGrantedAuthority("FINANCE_GL_POST"),
                        new SimpleGrantedAuthority("FINANCE_GL_REVERSE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor viewOnlyAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_JE_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private CreateJournalEntryRequest balancedEntryRequest() {
        return CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Test balanced journal entry")
                .source(JournalSource.MANUAL)
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(assetAccount.getId())
                                .description("Debit cash")
                                .debitAmount(new BigDecimal("1000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(liabilityAccount.getId())
                                .description("Credit payable")
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("1000.00"))
                                .build()))
                .build();
    }

    /**
     * Helper: creates a DRAFT journal entry via the API and returns its id.
     */
    private Long createDraftEntry() throws Exception {
        String result = mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(balancedEntryRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(result).get("id").asLong();
    }

    /**
     * Helper: creates a POSTED journal entry via the API and returns its id.
     */
    private Long createAndPostEntry() throws Exception {
        Long id = createDraftEntry();
        mockMvc.perform(post(BASE_URL + "/" + id + "/post")
                        .with(fullAuth()))
                .andExpect(status().isOk());
        return id;
    }

    // ---- GET /api/v1/finance/journal-entries ----

    @Test
    void getJournalEntries_returnsPaginatedList() throws Exception {
        createDraftEntry();
        createDraftEntry();

        mockMvc.perform(get(BASE_URL).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/journal-entries/status/{status} ----

    @Test
    void getJournalEntriesByStatus_returnsFilteredEntries() throws Exception {
        createDraftEntry();
        createAndPostEntry();

        mockMvc.perform(get(BASE_URL + "/status/DRAFT").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));

        mockMvc.perform(get(BASE_URL + "/status/POSTED").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].status").value("POSTED"));
    }

    // ---- GET /api/v1/finance/journal-entries/source/{source} ----

    @Test
    void getJournalEntriesBySource_returnsFilteredEntries() throws Exception {
        createDraftEntry();

        mockMvc.perform(get(BASE_URL + "/source/MANUAL").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].source").value("MANUAL"));
    }

    // ---- GET /api/v1/finance/journal-entries/date-range ----

    @Test
    void getJournalEntriesByDateRange_returnsEntriesInRange() throws Exception {
        createDraftEntry();

        mockMvc.perform(get(BASE_URL + "/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));

        // Outside range should return empty
        mockMvc.perform(get(BASE_URL + "/date-range")
                        .param("startDate", "2024-06-01")
                        .param("endDate", "2024-06-30")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- GET /api/v1/finance/journal-entries/search ----

    @Test
    void searchJournalEntries_findsByDescription() throws Exception {
        createDraftEntry();

        mockMvc.perform(get(BASE_URL + "/search")
                        .param("q", "balanced journal")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));
    }

    // ---- GET /api/v1/finance/journal-entries/{id} ----

    @Test
    void getJournalEntryById_returnsEntry() throws Exception {
        Long id = createDraftEntry();

        mockMvc.perform(get(BASE_URL + "/" + id).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.description").value("Test balanced journal entry"))
                .andExpect(jsonPath("$.entryNumber").value(startsWith("JE-202401-")));
    }

    // ---- GET /api/v1/finance/journal-entries/{id}/lines ----

    @Test
    void getJournalEntryWithLines_returnsEntryWithLines() throws Exception {
        Long id = createDraftEntry();

        mockMvc.perform(get(BASE_URL + "/" + id + "/lines").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    // ---- POST /api/v1/finance/journal-entries ----

    @Test
    void createJournalEntry_validRequest_returnsCreated() throws Exception {
        CreateJournalEntryRequest request = balancedEntryRequest();

        mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.description").value("Test balanced journal entry"))
                .andExpect(jsonPath("$.entryNumber").value(startsWith("JE-202401-")))
                .andExpect(jsonPath("$.totalDebit").value(closeTo(1000.0, 0.01)))
                .andExpect(jsonPath("$.totalCredit").value(closeTo(1000.0, 0.01)))
                .andExpect(jsonPath("$.balanced").value(true));
    }

    @Test
    void createJournalEntry_unbalanced_returns400() throws Exception {
        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("Unbalanced entry")
                .source(JournalSource.MANUAL)
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(assetAccount.getId())
                                .debitAmount(new BigDecimal("1000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(liabilityAccount.getId())
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("500.00"))
                                .build()))
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJournalEntry_missingLines_returns400() throws Exception {
        CreateJournalEntryRequest request = CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2024, 1, 15))
                .description("No lines entry")
                .source(JournalSource.MANUAL)
                .lines(List.of())
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /api/v1/finance/journal-entries/{id}/post ----

    @Test
    void postJournalEntry_validDraft_returnsPosted() throws Exception {
        Long id = createDraftEntry();

        mockMvc.perform(post(BASE_URL + "/" + id + "/post")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.postedAt").isNotEmpty());
    }

    // ---- POST /api/v1/finance/journal-entries/{id}/reverse ----

    @Test
    void reverseJournalEntry_postedEntry_returnsReversed() throws Exception {
        Long originalId = createAndPostEntry();

        mockMvc.perform(post(BASE_URL + "/" + originalId + "/reverse")
                        .param("reversalDate", "2024-01-20")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.reversedEntryId").value(originalId))
                .andExpect(jsonPath("$.description").value(startsWith("Reversal of")));

        // Verify the original entry is now REVERSED
        mockMvc.perform(get(BASE_URL + "/" + originalId).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"))
                .andExpect(jsonPath("$.reversingEntryId").isNotEmpty());
    }

    // ---- DELETE /api/v1/finance/journal-entries/{id} ----

    @Test
    void deleteJournalEntry_draftEntry_returns204() throws Exception {
        Long id = createDraftEntry();

        mockMvc.perform(delete(BASE_URL + "/" + id)
                        .with(fullAuth()))
                .andExpect(status().isNoContent());

        // Verify it is deleted
        mockMvc.perform(get(BASE_URL + "/" + id).with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteJournalEntry_postedEntry_returns400() throws Exception {
        Long id = createAndPostEntry();

        mockMvc.perform(delete(BASE_URL + "/" + id)
                        .with(fullAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_viewOnly_cannotCreate() throws Exception {
        CreateJournalEntryRequest request = balancedEntryRequest();

        mockMvc.perform(post(BASE_URL)
                        .with(viewOnlyAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
