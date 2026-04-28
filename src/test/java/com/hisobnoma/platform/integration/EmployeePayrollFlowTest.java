package com.hisobnoma.platform.integration;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.hr.dto.CreateSalaryRecordRequest;
import com.hisobnoma.platform.hr.entity.*;
import com.hisobnoma.platform.hr.repository.*;
import com.hisobnoma.platform.hr.service.SalaryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-module integration test: Employee Payroll
 * HR -> Finance: Salary calculation -> Journal entry -> Payment
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmployeePayrollFlowTest {

    @Autowired private SalaryService salaryService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private SalaryRecordRepository salaryRecordRepository;
    @Autowired private SalaryAdvanceRepository salaryAdvanceRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Employee employee;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Payroll Flow Tenant").code("INT_PAYROLL").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("payrolluser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        setupSecurityContext();

        Department dept = departmentRepository.saveAndFlush(Department.builder()
                .code("DEP-PAY").name("Engineering")
                .active(true).tenantId(tenant.getId()).build());

        Position position = positionRepository.saveAndFlush(Position.builder()
                .code("POS-PAY").name("Developer")
                .department(dept).baseSalary(new BigDecimal("5000000"))
                .active(true).tenantId(tenant.getId()).build());

        employee = employeeRepository.saveAndFlush(Employee.builder()
                .employeeCode("EMP-PAY-001")
                .firstName("John").lastName("Doe")
                .phone("+998901234567")
                .hireDate(LocalDate.of(2023, 1, 1))
                .department(dept).position(position)
                .currentSalary(new BigDecimal("5000000"))
                .status(Employee.EmployeeStatus.ACTIVE)
                .active(true)
                .tenantId(tenant.getId()).build());

        setupChartOfAccounts();
        setupFiscalPeriods();

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void createSalaryRecord_calculatesNetAmount() {
        var salaryDto = salaryService.create(createSalaryRequest(employee.getId(), 2026, 4,
                new BigDecimal("5000000"), new BigDecimal("500000"), new BigDecimal("200000")));

        assertThat(salaryDto.getNetAmount()).isEqualByComparingTo(new BigDecimal("5300000"));
        assertThat(salaryDto.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void markSalaryPaid_postsToGL() {
        var salaryDto = salaryService.create(createSalaryRequest(employee.getId(), 2026, 4,
                new BigDecimal("5000000"), null, null));

        entityManager.flush();
        entityManager.clear();

        var paidDto = salaryService.markPaid(salaryDto.getId());
        assertThat(paidDto.getStatus()).isEqualTo("PAID");
        assertThat(paidDto.getGlJournalEntryId()).isNotNull();

        entityManager.flush();
        entityManager.clear();

        var journalEntry = journalEntryRepository.findById(paidDto.getGlJournalEntryId()).orElseThrow();
        assertThat(journalEntry.getStatus()).isEqualTo(JournalStatus.POSTED);
        assertThat(journalEntry.getTotalDebit()).isGreaterThan(BigDecimal.ZERO);
        assertThat(journalEntry.getTotalDebit()).isEqualByComparingTo(journalEntry.getTotalCredit());
    }

    @Test
    void markPaid_withAdvance_deductsAdvanceFromPay() {
        SalaryAdvance advance = SalaryAdvance.builder()
                .employee(employee)
                .amount(new BigDecimal("1000000"))
                .advanceDate(LocalDate.now().minusDays(10))
                .periodYear(2026).periodMonth(4)
                .status(SalaryAdvance.AdvanceStatus.GIVEN)
                .tenantId(tenant.getId()).build();
        salaryAdvanceRepository.saveAndFlush(advance);

        var salaryDto = salaryService.create(createSalaryRequest(employee.getId(), 2026, 4,
                new BigDecimal("5000000"), null, null));

        entityManager.flush();
        entityManager.clear();

        var paidDto = salaryService.markPaid(salaryDto.getId());
        assertThat(paidDto.getStatus()).isEqualTo("PAID");
        assertThat(paidDto.getAdvanceAmount()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(paidDto.getPayAmount()).isEqualByComparingTo(new BigDecimal("4000000"));

        entityManager.flush();
        entityManager.clear();

        var updatedAdvance = salaryAdvanceRepository.findById(advance.getId()).orElseThrow();
        assertThat(updatedAdvance.getStatus()).isEqualTo(SalaryAdvance.AdvanceStatus.DEDUCTED);
    }

    @Test
    void markPaid_withMultipleAdvances_deductsAll() {
        salaryAdvanceRepository.saveAndFlush(SalaryAdvance.builder()
                .employee(employee)
                .amount(new BigDecimal("500000"))
                .advanceDate(LocalDate.now().minusDays(15))
                .periodYear(2026).periodMonth(4)
                .status(SalaryAdvance.AdvanceStatus.GIVEN)
                .tenantId(tenant.getId()).build());

        salaryAdvanceRepository.saveAndFlush(SalaryAdvance.builder()
                .employee(employee)
                .amount(new BigDecimal("300000"))
                .advanceDate(LocalDate.now().minusDays(5))
                .periodYear(2026).periodMonth(4)
                .status(SalaryAdvance.AdvanceStatus.GIVEN)
                .tenantId(tenant.getId()).build());

        var salaryDto = salaryService.create(createSalaryRequest(employee.getId(), 2026, 4,
                new BigDecimal("5000000"), null, null));

        entityManager.flush();
        entityManager.clear();

        var paidDto = salaryService.markPaid(salaryDto.getId());
        assertThat(paidDto.getAdvanceAmount()).isEqualByComparingTo(new BigDecimal("800000"));
        assertThat(paidDto.getPayAmount()).isEqualByComparingTo(new BigDecimal("4200000"));
    }

    @Test
    void salaryRecord_withBonusAndDeduction_netAmountCorrect() {
        var salaryDto = salaryService.create(createSalaryRequest(employee.getId(), 2026, 3,
                new BigDecimal("5000000"), new BigDecimal("1000000"), new BigDecimal("500000")));

        assertThat(salaryDto.getNetAmount()).isEqualByComparingTo(new BigDecimal("5500000"));
    }

    private CreateSalaryRecordRequest createSalaryRequest(Long employeeId, int year, int month,
                                                           BigDecimal base, BigDecimal bonus, BigDecimal deduction) {
        CreateSalaryRecordRequest req = new CreateSalaryRecordRequest();
        req.setEmployeeId(employeeId);
        req.setPeriodYear(year);
        req.setPeriodMonth(month);
        req.setBaseAmount(base);
        req.setBonusAmount(bonus);
        req.setDeductionAmount(deduction);
        return req;
    }

    private void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("HR_SALARY_CREATE"),
                    new SimpleGrantedAuthority("HR_SALARY_READ"),
                    new SimpleGrantedAuthority("HR_SALARY_PAY"),
                    new SimpleGrantedAuthority("HR_ADVANCE_CREATE"),
                    new SimpleGrantedAuthority("FINANCE_GL_POST")));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setCurrentTenant(tenant.getId());
    }

    private void setupChartOfAccounts() {
        createAccount("1110", "Cash", AccountType.ASSET, NormalBalance.DEBIT);
        createAccount("1130", "Accounts Receivable", AccountType.ASSET, NormalBalance.DEBIT);
        createAccount("1140", "Inventory", AccountType.ASSET, NormalBalance.DEBIT);
        createAccount("2110", "Accounts Payable", AccountType.LIABILITY, NormalBalance.CREDIT);
        createAccount("4100", "Sales Revenue", AccountType.REVENUE, NormalBalance.CREDIT);
        createAccount("4200", "Sales Discounts", AccountType.REVENUE, NormalBalance.CREDIT);
        createAccount("4300", "Purchase Discounts", AccountType.REVENUE, NormalBalance.CREDIT);
        createAccount("5100", "Cost of Goods Sold", AccountType.EXPENSE, NormalBalance.DEBIT);
        createAccount("5200", "Purchase Expense", AccountType.EXPENSE, NormalBalance.DEBIT);
        createAccount("6100", "Salary Expense", AccountType.EXPENSE, NormalBalance.DEBIT);
        createAccount("1400", "Salary Advances", AccountType.ASSET, NormalBalance.DEBIT);
    }

    private void createAccount(String code, String name, AccountType type, NormalBalance normalBalance) {
        accountRepository.saveAndFlush(Account.builder()
                .code(code).name(name)
                .accountType(type).normalBalance(normalBalance)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
    }

    private void setupFiscalPeriods() {
        FiscalYear fy = FiscalYear.builder()
                .year(2026)
                .name("FY 2026")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(PeriodStatus.OPEN).current(true)
                .tenantId(tenant.getId()).build();
        fiscalYearRepository.saveAndFlush(fy);

        for (int m = 1; m <= 12; m++) {
            LocalDate start = LocalDate.of(2026, m, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                    .fiscalYear(fy).periodNumber(m)
                    .name(start.getMonth().toString())
                    .startDate(start).endDate(end)
                    .status(PeriodStatus.OPEN)
                    .tenantId(tenant.getId()).build());
        }
    }
}
