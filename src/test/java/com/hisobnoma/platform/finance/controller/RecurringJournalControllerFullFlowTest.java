package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.RecurringJournalTemplateDto;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.FiscalPeriodRepository;
import com.hisobnoma.platform.finance.repository.FiscalYearRepository;
import com.hisobnoma.platform.finance.repository.RecurringJournalTemplateRepository;
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

import jakarta.persistence.EntityManager;
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
class RecurringJournalControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/finance/recurring-journals";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RecurringJournalTemplateRepository templateRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User adminUser;
    private Account cashAccount;
    private Account expenseAccount;
    private RecurringJournalTemplate existingTemplate;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Recurring Tenant").code("FULLFLOW_RJ").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("recurringadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        // Accounts needed for template lines
        cashAccount = accountRepository.saveAndFlush(Account.builder()
                .code("1000").name("Cash").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1)
                .allowsDirectPosting(true).active(true)
                .tenantId(tenant.getId()).build());

        expenseAccount = accountRepository.saveAndFlush(Account.builder()
                .code("5000").name("Rent Expense").accountType(AccountType.EXPENSE)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1)
                .allowsDirectPosting(true).active(true)
                .tenantId(tenant.getId()).build());

        // FiscalYear + FiscalPeriod covering today (needed for execute endpoint)
        LocalDate today = LocalDate.now();
        FiscalYear fiscalYear = fiscalYearRepository.saveAndFlush(FiscalYear.builder()
                .year(today.getYear()).name("FY " + today.getYear())
                .startDate(LocalDate.of(today.getYear(), 1, 1))
                .endDate(LocalDate.of(today.getYear(), 12, 31))
                .status(PeriodStatus.OPEN).current(true).closed(false)
                .tenantId(tenant.getId()).build());

        fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                .fiscalYear(fiscalYear).periodNumber(today.getMonthValue())
                .name(today.getMonth().name())
                .startDate(today.withDayOfMonth(1))
                .endDate(today.withDayOfMonth(today.lengthOfMonth()))
                .status(PeriodStatus.OPEN).adjustmentPeriod(false)
                .tenantId(tenant.getId()).build());

        // Pre-existing template for GET / update / delete / activate / deactivate / execute tests
        existingTemplate = RecurringJournalTemplate.builder()
                .tenantId(tenant.getId())
                .name("Monthly Rent")
                .description("Monthly rent payment")
                .frequency(RecurringJournalTemplate.RecurrenceFrequency.MONTHLY)
                .frequencyDay(1)
                .startDate(LocalDate.of(today.getYear(), 1, 1))
                .active(true)
                .autoPost(false)
                .runCount(0)
                .nextRunDate(LocalDate.of(today.getYear(), 1, 1))
                .build();

        RecurringJournalLine debitLine = RecurringJournalLine.builder()
                .tenantId(tenant.getId())
                .account(expenseAccount)
                .description("Rent debit")
                .debitAmount(new BigDecimal("1000.0000"))
                .creditAmount(BigDecimal.ZERO)
                .sortOrder(1)
                .build();
        existingTemplate.addLine(debitLine);

        RecurringJournalLine creditLine = RecurringJournalLine.builder()
                .tenantId(tenant.getId())
                .account(cashAccount)
                .description("Rent credit")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("1000.0000"))
                .sortOrder(2)
                .build();
        existingTemplate.addLine(creditLine);

        existingTemplate = templateRepository.saveAndFlush(existingTemplate);

        entityManager.clear();
    }

    // ---- Authentication helpers ----

    private RequestPostProcessor manageAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_RECURRING_VIEW"),
                        new SimpleGrantedAuthority("FINANCE_RECURRING_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor viewOnlyAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_RECURRING_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RecurringJournalTemplateDto buildValidCreateDto(String name) {
        return RecurringJournalTemplateDto.builder()
                .name(name)
                .description("Test template")
                .frequency(RecurringJournalTemplate.RecurrenceFrequency.MONTHLY)
                .frequencyDay(15)
                .startDate(LocalDate.now())
                .active(true)
                .autoPost(false)
                .lines(List.of(
                        RecurringJournalTemplateDto.LineDto.builder()
                                .accountId(expenseAccount.getId())
                                .description("Debit line")
                                .debitAmount(new BigDecimal("500.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .sortOrder(1)
                                .build(),
                        RecurringJournalTemplateDto.LineDto.builder()
                                .accountId(cashAccount.getId())
                                .description("Credit line")
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("500.00"))
                                .sortOrder(2)
                                .build()))
                .build();
    }

    // ---- GET /api/v1/finance/recurring-journals ----

    @Test
    void getTemplates_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].name").value("Monthly Rent"));
    }

    // ---- GET /api/v1/finance/recurring-journals/{id} ----

    @Test
    void getTemplate_returnsTemplateWithLines() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + existingTemplate.getId()).with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingTemplate.getId()))
                .andExpect(jsonPath("$.name").value("Monthly Rent"))
                .andExpect(jsonPath("$.description").value("Monthly rent payment"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[?(@.accountCode=='5000')]").exists())
                .andExpect(jsonPath("$.lines[?(@.accountCode=='1000')]").exists());
    }

    @Test
    void getTemplate_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/99999").with(manageAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/finance/recurring-journals ----

    @Test
    void createTemplate_validRequest_returnsCreated() throws Exception {
        RecurringJournalTemplateDto request = buildValidCreateDto("New Template");

        mockMvc.perform(post(BASE_URL)
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("New Template"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.frequencyDay").value(15))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.runCount").value(0))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    @Test
    void createTemplate_duplicateName_returns400() throws Exception {
        RecurringJournalTemplateDto request = buildValidCreateDto("Monthly Rent");

        mockMvc.perform(post(BASE_URL)
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTemplate_unbalancedLines_returns400() throws Exception {
        RecurringJournalTemplateDto request = RecurringJournalTemplateDto.builder()
                .name("Unbalanced Template")
                .description("Lines do not balance")
                .frequency(RecurringJournalTemplate.RecurrenceFrequency.MONTHLY)
                .startDate(LocalDate.now())
                .active(true)
                .lines(List.of(
                        RecurringJournalTemplateDto.LineDto.builder()
                                .accountId(expenseAccount.getId())
                                .description("Debit line")
                                .debitAmount(new BigDecimal("500.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .sortOrder(1)
                                .build(),
                        RecurringJournalTemplateDto.LineDto.builder()
                                .accountId(cashAccount.getId())
                                .description("Credit line")
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("300.00"))
                                .sortOrder(2)
                                .build()))
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/finance/recurring-journals/{id} ----

    @Test
    void updateTemplate_validRequest_returnsUpdated() throws Exception {
        RecurringJournalTemplateDto request = RecurringJournalTemplateDto.builder()
                .name("Updated Rent Template")
                .description("Updated description")
                .frequency(RecurringJournalTemplate.RecurrenceFrequency.QUARTERLY)
                .frequencyDay(1)
                .startDate(LocalDate.now())
                .active(true)
                .autoPost(true)
                .lines(List.of(
                        RecurringJournalTemplateDto.LineDto.builder()
                                .accountId(expenseAccount.getId())
                                .description("Updated debit")
                                .debitAmount(new BigDecimal("2000.00"))
                                .creditAmount(BigDecimal.ZERO)
                                .sortOrder(1)
                                .build(),
                        RecurringJournalTemplateDto.LineDto.builder()
                                .accountId(cashAccount.getId())
                                .description("Updated credit")
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("2000.00"))
                                .sortOrder(2)
                                .build()))
                .build();

        mockMvc.perform(put(BASE_URL + "/" + existingTemplate.getId())
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Rent Template"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.frequency").value("QUARTERLY"))
                .andExpect(jsonPath("$.autoPost").value(true))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    // ---- DELETE /api/v1/finance/recurring-journals/{id} ----

    @Test
    void deleteTemplate_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + existingTemplate.getId())
                        .with(manageAuth()))
                .andExpect(status().isNoContent());

        // Verify it is gone
        mockMvc.perform(get(BASE_URL + "/" + existingTemplate.getId())
                        .with(manageAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /api/v1/finance/recurring-journals/{id}/activate ----

    @Test
    void activateTemplate_setsActiveTrue() throws Exception {
        // First deactivate so we can test activation
        mockMvc.perform(put(BASE_URL + "/" + existingTemplate.getId() + "/deactivate")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Now activate
        mockMvc.perform(put(BASE_URL + "/" + existingTemplate.getId() + "/activate")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---- PUT /api/v1/finance/recurring-journals/{id}/deactivate ----

    @Test
    void deactivateTemplate_setsActiveFalse() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + existingTemplate.getId() + "/deactivate")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ---- POST /api/v1/finance/recurring-journals/{id}/execute ----

    @Test
    void executeTemplate_createsJournalEntry() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + existingTemplate.getId() + "/execute")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.journalEntryId").isNumber());

        // Verify template tracking fields were updated
        mockMvc.perform(get(BASE_URL + "/" + existingTemplate.getId())
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runCount").value(1))
                .andExpect(jsonPath("$.lastRunDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.nextRunDate").isNotEmpty());
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_viewOnly_cannotCreate() throws Exception {
        RecurringJournalTemplateDto request = buildValidCreateDto("Permission Test");

        mockMvc.perform(post(BASE_URL)
                        .with(viewOnlyAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
