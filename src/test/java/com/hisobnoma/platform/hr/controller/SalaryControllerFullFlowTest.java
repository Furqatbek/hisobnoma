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
import com.hisobnoma.platform.hr.dto.CreateSalaryRecordRequest;
import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.entity.Position;
import com.hisobnoma.platform.hr.entity.SalaryRecord;
import com.hisobnoma.platform.hr.repository.DepartmentRepository;
import com.hisobnoma.platform.hr.repository.EmployeeRepository;
import com.hisobnoma.platform.hr.repository.PositionRepository;
import com.hisobnoma.platform.hr.repository.SalaryRecordRepository;
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
class SalaryControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/hr/salary";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SalaryRecordRepository salaryRecordRepository;
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
    private SalaryRecord salaryRecord;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("HR Salary Test Tenant").code("HR_SAL_TEST").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("hrsalaryuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        department = departmentRepository.saveAndFlush(Department.builder()
                .code("DEP-SAL-001").name("Engineering")
                .active(true).tenantId(tenant.getId()).build());

        position = positionRepository.saveAndFlush(Position.builder()
                .code("POS-SAL-001").name("Developer")
                .department(department)
                .baseSalary(new BigDecimal("5000000"))
                .active(true).tenantId(tenant.getId()).build());

        employee = employeeRepository.saveAndFlush(Employee.builder()
                .employeeCode("EMP-SAL-001")
                .firstName("Ali").lastName("Valiyev").middleName("Karimovich")
                .phone("+998901234567").email("ali@test.com")
                .hireDate(LocalDate.of(2024, 1, 15))
                .department(department).position(position)
                .currentSalary(new BigDecimal("5000000"))
                .status(Employee.EmployeeStatus.ACTIVE).active(true)
                .tenantId(tenant.getId()).build());

        // Required GL accounts for salary payment posting
        accountRepository.saveAndFlush(Account.builder()
                .code("1110").name("Cash").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).active(true)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("1400").name("Salary Advance").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).active(true)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("6100").name("Salary Expense").accountType(AccountType.EXPENSE)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).active(true)
                .tenantId(tenant.getId()).build());

        // Fiscal year and period required for GL journal entry posting
        FiscalYear fiscalYear = fiscalYearRepository.saveAndFlush(FiscalYear.builder()
                .year(2026).name("FY 2026")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(PeriodStatus.OPEN)
                .current(true).closed(false)
                .tenantId(tenant.getId()).build());

        fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                .fiscalYear(fiscalYear)
                .periodNumber(LocalDate.now().getMonthValue())
                .name("Period " + LocalDate.now().getMonthValue())
                .startDate(LocalDate.now().withDayOfMonth(1))
                .endDate(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()))
                .status(PeriodStatus.OPEN)
                .tenantId(tenant.getId()).build());

        salaryRecord = salaryRecordRepository.saveAndFlush(SalaryRecord.builder()
                .employee(employee)
                .periodYear(2026).periodMonth(4)
                .baseAmount(new BigDecimal("5000000"))
                .bonusAmount(new BigDecimal("500000"))
                .deductionAmount(new BigDecimal("200000"))
                .netAmount(new BigDecimal("5300000"))
                .status(SalaryRecord.SalaryStatus.PENDING)
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

    // ---- GET /api/v1/hr/salary ----

    @Test
    void getAll_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].employeeId").value(employee.getId().intValue()))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    // ---- GET /api/v1/hr/salary/period ----

    @Test
    void getByPeriod_returnsRecords() throws Exception {
        mockMvc.perform(get(BASE_URL + "/period")
                        .param("year", "2026").param("month", "4")
                        .with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].periodYear").value(2026))
                .andExpect(jsonPath("$.content[0].periodMonth").value(4))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    // ---- GET /api/v1/hr/salary/employee/{employeeId} ----

    @Test
    void getByEmployee_returnsRecords() throws Exception {
        mockMvc.perform(get(BASE_URL + "/employee/{employeeId}", employee.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employeeId").value(employee.getId().intValue()));
    }

    // ---- GET /api/v1/hr/salary/{id} ----

    @Test
    void getById_returnsSalaryRecord() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", salaryRecord.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salaryRecord.getId().intValue()))
                .andExpect(jsonPath("$.employeeId").value(employee.getId().intValue()))
                .andExpect(jsonPath("$.employeeName").value("Valiyev Ali Karimovich"))
                .andExpect(jsonPath("$.employeeCode").value("EMP-SAL-001"))
                .andExpect(jsonPath("$.periodYear").value(2026))
                .andExpect(jsonPath("$.periodMonth").value(4))
                .andExpect(jsonPath("$.baseAmount").value(5000000))
                .andExpect(jsonPath("$.bonusAmount").value(500000))
                .andExpect(jsonPath("$.deductionAmount").value(200000))
                .andExpect(jsonPath("$.netAmount").value(5300000))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 999999L)
                        .with(readWriteAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/hr/salary ----

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        CreateSalaryRecordRequest request = new CreateSalaryRecordRequest();
        request.setEmployeeId(employee.getId());
        request.setPeriodYear(2026);
        request.setPeriodMonth(5);
        request.setBaseAmount(new BigDecimal("6000000"));
        request.setBonusAmount(new BigDecimal("1000000"));
        request.setDeductionAmount(new BigDecimal("500000"));
        request.setNotes("May salary");

        mockMvc.perform(post(BASE_URL)
                        .with(readWriteAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(employee.getId().intValue()))
                .andExpect(jsonPath("$.periodYear").value(2026))
                .andExpect(jsonPath("$.periodMonth").value(5))
                .andExpect(jsonPath("$.baseAmount").value(6000000))
                .andExpect(jsonPath("$.bonusAmount").value(1000000))
                .andExpect(jsonPath("$.deductionAmount").value(500000))
                .andExpect(jsonPath("$.netAmount").value(6500000))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.notes").value("May salary"));
    }

    @Test
    void create_duplicatePeriod_returnsBadRequest() throws Exception {
        CreateSalaryRecordRequest request = new CreateSalaryRecordRequest();
        request.setEmployeeId(employee.getId());
        request.setPeriodYear(2026);
        request.setPeriodMonth(4);
        request.setBaseAmount(new BigDecimal("5000000"));

        mockMvc.perform(post(BASE_URL)
                        .with(readWriteAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/hr/salary/{id}/pay ----

    @Test
    void markPaid_returnsPaidRecord() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}/pay", salaryRecord.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidDate").isNotEmpty())
                .andExpect(jsonPath("$.glJournalEntryId").isNotEmpty());
    }

    // ---- PUT /api/v1/hr/salary/{id}/cancel ----

    @Test
    void cancel_returnsCancelledRecord() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}/cancel", salaryRecord.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_paidRecord_returnsBadRequest() throws Exception {
        // First mark as paid
        mockMvc.perform(put(BASE_URL + "/{id}/pay", salaryRecord.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isOk());

        // Then try to cancel — should fail
        mockMvc.perform(put(BASE_URL + "/{id}/cancel", salaryRecord.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get(BASE_URL)
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }
}
