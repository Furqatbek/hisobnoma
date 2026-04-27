package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateFiscalYearRequest;
import com.hisobnoma.platform.finance.entity.FiscalPeriod;
import com.hisobnoma.platform.finance.entity.FiscalYear;
import com.hisobnoma.platform.finance.entity.PeriodStatus;
import com.hisobnoma.platform.finance.repository.FiscalPeriodRepository;
import com.hisobnoma.platform.finance.repository.FiscalYearRepository;
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
class FiscalPeriodControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User adminUser;
    private FiscalYear fiscalYear2025;
    private FiscalPeriod janPeriod;
    private FiscalPeriod febPeriod;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Fiscal Tenant").code("FULLFLOW_FP").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("fiscaladmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        fiscalYear2025 = fiscalYearRepository.saveAndFlush(FiscalYear.builder()
                .year(2025).name("Fiscal Year 2025")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .status(PeriodStatus.OPEN).current(true).closed(false)
                .tenantId(tenant.getId()).build());

        janPeriod = fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                .fiscalYear(fiscalYear2025).periodNumber(1).name("January")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 31))
                .status(PeriodStatus.OPEN).adjustmentPeriod(false)
                .tenantId(tenant.getId()).build());

        febPeriod = fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                .fiscalYear(fiscalYear2025).periodNumber(2).name("February")
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 2, 28))
                .status(PeriodStatus.OPEN).adjustmentPeriod(false)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    private RequestPostProcessor fiscalAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_PERIOD_VIEW"),
                        new SimpleGrantedAuthority("FINANCE_PERIOD_MANAGE"),
                        new SimpleGrantedAuthority("FINANCE_PERIOD_CLOSE"),
                        new SimpleGrantedAuthority("FINANCE_PERIOD_REOPEN"),
                        new SimpleGrantedAuthority("FINANCE_YEAR_CLOSE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/finance/fiscal/years ----

    @Test
    void getFiscalYears_returnsPaginatedList() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years").with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));
    }

    // ---- GET /api/v1/finance/fiscal/years/all ----

    @Test
    void getAllFiscalYears_returnsAll() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years/all").with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    // ---- GET /api/v1/finance/fiscal/years/current ----

    @Test
    void getCurrentFiscalYear_returnsCurrentYear() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years/current").with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.current").value(true));
    }

    // ---- GET /api/v1/finance/fiscal/years/{id} ----

    @Test
    void getFiscalYear_existingId_returnsYear() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years/" + fiscalYear2025.getId())
                        .with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fiscal Year 2025"));
    }

    @Test
    void getFiscalYear_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years/99999").with(fiscalAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/fiscal/years/{id}/details ----

    @Test
    void getFiscalYearWithPeriods_returnsYearWithPeriods() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years/" + fiscalYear2025.getId() + "/details")
                        .with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.periods").isArray())
                .andExpect(jsonPath("$.periods.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/fiscal/years/by-date ----

    @Test
    void getFiscalYearByDate_dateWithinRange_returnsYear() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years/by-date")
                        .param("date", "2025-06-15").with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2025));
    }

    @Test
    void getFiscalYearByDate_dateOutsideRange_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years/by-date")
                        .param("date", "2020-01-01").with(fiscalAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/finance/fiscal/years ----

    @Test
    void createFiscalYear_validRequest_returnsCreated() throws Exception {
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2026).name("Fiscal Year 2026")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .generatePeriods(false).build();

        mockMvc.perform(post("/api/v1/finance/fiscal/years")
                        .with(fiscalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createFiscalYear_withPeriodGeneration_generatesMonthlyPeriods() throws Exception {
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2027).name("Fiscal Year 2027")
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2027, 12, 31))
                .generatePeriods(true).build();

        mockMvc.perform(post("/api/v1/finance/fiscal/years")
                        .with(fiscalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.year").value(2027));
    }

    @Test
    void createFiscalYear_duplicateYear_returns400() throws Exception {
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2025).name("Duplicate 2025")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .generatePeriods(false).build();

        mockMvc.perform(post("/api/v1/finance/fiscal/years")
                        .with(fiscalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFiscalYear_endBeforeStart_returns400() throws Exception {
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2028).name("Invalid Dates")
                .startDate(LocalDate.of(2028, 12, 31))
                .endDate(LocalDate.of(2028, 1, 1))
                .generatePeriods(false).build();

        mockMvc.perform(post("/api/v1/finance/fiscal/years")
                        .with(fiscalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFiscalYear_missingYear_returns400() throws Exception {
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .name("No Year")
                .startDate(LocalDate.of(2029, 1, 1))
                .endDate(LocalDate.of(2029, 12, 31)).build();

        mockMvc.perform(post("/api/v1/finance/fiscal/years")
                        .with(fiscalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/finance/fiscal/years/{id} ----

    @Test
    void updateFiscalYear_validRequest_updatesFields() throws Exception {
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2025).name("Updated FY 2025")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31)).build();

        mockMvc.perform(put("/api/v1/finance/fiscal/years/" + fiscalYear2025.getId())
                        .with(fiscalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated FY 2025"));
    }

    // ---- PUT /api/v1/finance/fiscal/years/{id}/set-current ----

    @Test
    void setCurrentFiscalYear_switchesCurrent() throws Exception {
        FiscalYear newYear = fiscalYearRepository.saveAndFlush(FiscalYear.builder()
                .year(2026).name("FY 2026")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(PeriodStatus.OPEN).current(false).closed(false)
                .tenantId(tenant.getId()).build());

        mockMvc.perform(put("/api/v1/finance/fiscal/years/" + newYear.getId() + "/set-current")
                        .with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(true));
    }

    // ---- GET /api/v1/finance/fiscal/years/{yearId}/periods ----

    @Test
    void getPeriodsByFiscalYear_returnsPeriods() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years/" + fiscalYear2025.getId() + "/periods")
                        .with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- GET /api/v1/finance/fiscal/periods/{id} ----

    @Test
    void getPeriod_existingId_returnsPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/periods/" + janPeriod.getId())
                        .with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("January"))
                .andExpect(jsonPath("$.periodNumber").value(1));
    }

    @Test
    void getPeriod_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/periods/99999").with(fiscalAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/fiscal/periods/by-date ----

    @Test
    void getPeriodByDate_dateWithinPeriod_returnsPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/periods/by-date")
                        .param("date", "2025-01-15").with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("January"));
    }

    // ---- GET /api/v1/finance/fiscal/periods/open ----

    @Test
    void getOpenPeriods_returnsOnlyOpen() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/periods/open").with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- POST /api/v1/finance/fiscal/periods/{id}/close ----

    @Test
    void closePeriod_openPeriod_closesPeriod() throws Exception {
        mockMvc.perform(post("/api/v1/finance/fiscal/periods/" + janPeriod.getId() + "/close")
                        .with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    // ---- POST /api/v1/finance/fiscal/periods/{id}/reopen ----

    @Test
    void reopenPeriod_closedPeriod_reopensPeriod() throws Exception {
        fiscalPeriodRepository.findById(janPeriod.getId()).ifPresent(p -> {
            p.setStatus(PeriodStatus.CLOSED);
            fiscalPeriodRepository.saveAndFlush(p);
        });
        entityManager.clear();

        mockMvc.perform(post("/api/v1/finance/fiscal/periods/" + janPeriod.getId() + "/reopen")
                        .param("reason", "Correction needed")
                        .with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void reopenPeriod_lockedPeriod_returns400() throws Exception {
        fiscalPeriodRepository.findById(janPeriod.getId()).ifPresent(p -> {
            p.setStatus(PeriodStatus.LOCKED);
            fiscalPeriodRepository.saveAndFlush(p);
        });
        entityManager.clear();

        mockMvc.perform(post("/api/v1/finance/fiscal/periods/" + janPeriod.getId() + "/reopen")
                        .param("reason", "Cannot reopen locked")
                        .with(fiscalAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /api/v1/finance/fiscal/years/{id}/close ----

    @Test
    void closeFiscalYear_allPeriodsClosed_closesYear() throws Exception {
        fiscalPeriodRepository.findById(janPeriod.getId()).ifPresent(p -> {
            p.setStatus(PeriodStatus.CLOSED);
            fiscalPeriodRepository.saveAndFlush(p);
        });
        fiscalPeriodRepository.findById(febPeriod.getId()).ifPresent(p -> {
            p.setStatus(PeriodStatus.CLOSED);
            fiscalPeriodRepository.saveAndFlush(p);
        });
        entityManager.clear();

        mockMvc.perform(post("/api/v1/finance/fiscal/years/" + fiscalYear2025.getId() + "/close")
                        .with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closeFiscalYear_openPeriodsExist_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/finance/fiscal/years/" + fiscalYear2025.getId() + "/close")
                        .with(fiscalAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- Permission checks ----

    @Test
    void getFiscalYears_noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/finance/fiscal/years")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // ---- Full lifecycle ----

    @Test
    void fullFiscalYearLifecycle_createOpenCloseReopen() throws Exception {
        CreateFiscalYearRequest createReq = CreateFiscalYearRequest.builder()
                .year(2030).name("Lifecycle FY 2030")
                .startDate(LocalDate.of(2030, 1, 1))
                .endDate(LocalDate.of(2030, 12, 31))
                .generatePeriods(true).build();

        String result = mockMvc.perform(post("/api/v1/finance/fiscal/years")
                        .with(fiscalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long yearId = objectMapper.readTree(result).get("id").asLong();

        mockMvc.perform(get("/api/v1/finance/fiscal/years/" + yearId + "/details")
                        .with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periods").isArray())
                .andExpect(jsonPath("$.periods.length()").value(12));

        mockMvc.perform(put("/api/v1/finance/fiscal/years/" + yearId + "/set-current")
                        .with(fiscalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(true));
    }
}
