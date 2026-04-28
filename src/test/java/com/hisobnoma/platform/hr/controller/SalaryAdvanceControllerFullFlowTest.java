package com.hisobnoma.platform.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.AccountType;
import com.hisobnoma.platform.finance.entity.FiscalPeriod;
import com.hisobnoma.platform.finance.entity.FiscalYear;
import com.hisobnoma.platform.finance.entity.NormalBalance;
import com.hisobnoma.platform.finance.entity.PeriodStatus;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.FiscalPeriodRepository;
import com.hisobnoma.platform.finance.repository.FiscalYearRepository;
import com.hisobnoma.platform.hr.dto.CreateSalaryAdvanceRequest;
import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.entity.Position;
import com.hisobnoma.platform.hr.entity.SalaryAdvance;
import com.hisobnoma.platform.hr.repository.DepartmentRepository;
import com.hisobnoma.platform.hr.repository.EmployeeRepository;
import com.hisobnoma.platform.hr.repository.PositionRepository;
import com.hisobnoma.platform.hr.repository.SalaryAdvanceRepository;
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
class SalaryAdvanceControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/hr/advances";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SalaryAdvanceRepository advanceRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Department department;
    private Position position;
    private Employee employee;
    private SalaryAdvance advance;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("HR Adv Test Tenant").code("HR_ADV_TEST").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("hradvuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        department = departmentRepository.saveAndFlush(Department.builder()
                .code("DEP-ADV-001").name("Engineering")
                .active(true).tenantId(tenant.getId()).build());

        position = positionRepository.saveAndFlush(Position.builder()
                .code("POS-ADV-001").name("Developer")
                .department(department)
                .baseSalary(new BigDecimal("5000000"))
                .active(true).tenantId(tenant.getId()).build());

        employee = employeeRepository.saveAndFlush(Employee.builder()
                .employeeCode("EMP-ADV-001")
                .firstName("John").lastName("Doe").middleName("M")
                .phone("+998901234567").email("john.adv@test.com")
                .hireDate(LocalDate.of(2024, 1, 15))
                .department(department).position(position)
                .currentSalary(new BigDecimal("5000000"))
                .status(Employee.EmployeeStatus.ACTIVE).active(true)
                .tenantId(tenant.getId()).build());

        // GL accounts required for salary advance posting
        accountRepository.saveAndFlush(Account.builder()
                .code("1110").name("Cash").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).active(true)
                .tenantId(tenant.getId()).build());

        accountRepository.saveAndFlush(Account.builder()
                .code("1400").name("Salary Advance").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).active(true)
                .tenantId(tenant.getId()).build());

        // Fiscal year and period for GL posting
        FiscalYear fiscalYear = fiscalYearRepository.saveAndFlush(FiscalYear.builder()
                .year(2026).name("FY 2026")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(PeriodStatus.OPEN).current(true).closed(false)
                .tenantId(tenant.getId()).build());

        fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                .fiscalYear(fiscalYear).periodNumber(4).name("April")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 30))
                .status(PeriodStatus.OPEN).adjustmentPeriod(false)
                .tenantId(tenant.getId()).build());

        // Existing GIVEN advance for query tests
        advance = advanceRepository.saveAndFlush(SalaryAdvance.builder()
                .employee(employee)
                .amount(new BigDecimal("500000"))
                .advanceDate(LocalDate.of(2026, 4, 10))
                .periodYear(2026).periodMonth(4)
                .status(SalaryAdvance.AdvanceStatus.GIVEN)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    private RequestPostProcessor readWriteAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("HR_SALARY_READ"),
                        new SimpleGrantedAuthority("HR_SALARY_WRITE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/hr/advances/period?year=2026&month=4 ----

    @Test
    void getByPeriod_returnsAdvances() throws Exception {
        mockMvc.perform(get(BASE_URL + "/period")
                        .param("year", "2026")
                        .param("month", "4")
                        .with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].employeeId").value(employee.getId().intValue()))
                .andExpect(jsonPath("$[0].amount").value(500000))
                .andExpect(jsonPath("$[0].periodYear").value(2026))
                .andExpect(jsonPath("$[0].periodMonth").value(4))
                .andExpect(jsonPath("$[0].status").value("GIVEN"));
    }

    // ---- GET /api/v1/hr/advances/employee/{employeeId} ----

    @Test
    void getByEmployee_returnsAdvances() throws Exception {
        mockMvc.perform(get(BASE_URL + "/employee/{employeeId}", employee.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].employeeId").value(employee.getId().intValue()))
                .andExpect(jsonPath("$[0].employeeCode").value("EMP-ADV-001"))
                .andExpect(jsonPath("$[0].status").value("GIVEN"));
    }

    // ---- GET /api/v1/hr/advances/employee/{employeeId}/total?year=2026&month=4 ----

    @Test
    void getUndeductedTotal_returnsTotal() throws Exception {
        mockMvc.perform(get(BASE_URL + "/employee/{employeeId}/total", employee.getId())
                        .param("year", "2026")
                        .param("month", "4")
                        .with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(500000));
    }

    // ---- POST /api/v1/hr/advances ----

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        CreateSalaryAdvanceRequest request = new CreateSalaryAdvanceRequest();
        request.setEmployeeId(employee.getId());
        request.setAmount(new BigDecimal("300000"));
        request.setAdvanceDate(LocalDate.of(2026, 4, 15));
        request.setPeriodYear(2026);
        request.setPeriodMonth(4);
        request.setNotes("April advance");

        mockMvc.perform(post(BASE_URL)
                        .with(readWriteAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(employee.getId().intValue()))
                .andExpect(jsonPath("$.amount").value(300000))
                .andExpect(jsonPath("$.status").value("GIVEN"))
                .andExpect(jsonPath("$.periodYear").value(2026))
                .andExpect(jsonPath("$.periodMonth").value(4))
                .andExpect(jsonPath("$.notes").value("April advance"))
                .andExpect(jsonPath("$.glJournalEntryId").isNotEmpty());
    }

    // ---- PUT /api/v1/hr/advances/{id}/cancel ----

    @Test
    void cancel_returnsCancelledAdvance() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}/cancel", advance.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(advance.getId().intValue()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_nonGivenAdvance_returnsBadRequest() throws Exception {
        // Change the advance status to DEDUCTED directly in the DB
        advance = advanceRepository.findById(advance.getId()).orElseThrow();
        advance.setStatus(SalaryAdvance.AdvanceStatus.DEDUCTED);
        advanceRepository.saveAndFlush(advance);
        entityManager.clear();

        mockMvc.perform(put(BASE_URL + "/{id}/cancel", advance.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/period")
                        .param("year", "2026")
                        .param("month", "4")
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/employee/{employeeId}", employee.getId())
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/employee/{employeeId}/total", employee.getId())
                        .param("year", "2026")
                        .param("month", "4")
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreateSalaryAdvanceRequest request = new CreateSalaryAdvanceRequest();
        request.setEmployeeId(employee.getId());
        request.setAmount(new BigDecimal("100000"));
        request.setPeriodYear(2026);
        request.setPeriodMonth(4);

        mockMvc.perform(post(BASE_URL)
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/{id}/cancel", advance.getId())
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
