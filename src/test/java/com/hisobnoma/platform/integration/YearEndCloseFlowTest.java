package com.hisobnoma.platform.integration;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.finance.service.FiscalPeriodService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cross-module integration test: Year-End Close
 * Finance: Close all periods -> retained earnings -> new year
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class YearEndCloseFlowTest {

    @Autowired private FiscalPeriodService fiscalPeriodService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private FiscalYear fiscalYear;
    private List<FiscalPeriod> periods;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Year End Tenant").code("INT_YEAR_END").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("yearenduser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        setupSecurityContext();
        setupChartOfAccounts();

        fiscalYear = FiscalYear.builder()
                .year(2025)
                .name("FY 2025")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .status(PeriodStatus.OPEN).current(true)
                .tenantId(tenant.getId()).build();
        fiscalYearRepository.saveAndFlush(fiscalYear);

        for (int m = 1; m <= 12; m++) {
            LocalDate start = LocalDate.of(2025, m, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                    .fiscalYear(fiscalYear).periodNumber(m)
                    .name(start.getMonth().toString())
                    .startDate(start).endDate(end)
                    .status(PeriodStatus.OPEN)
                    .tenantId(tenant.getId()).build());
        }

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void closeAllPeriods_thenCloseYear() {
        var allPeriods = fiscalPeriodRepository.findByFiscalYearId(fiscalYear.getId());
        for (var period : allPeriods) {
            fiscalPeriodService.closePeriod(period.getId());
        }

        entityManager.flush();
        entityManager.clear();

        var closedYear = fiscalPeriodService.closeFiscalYear(fiscalYear.getId());
        assertThat(closedYear.getStatus()).isEqualTo(PeriodStatus.CLOSED);

        entityManager.flush();
        entityManager.clear();

        var reloadedYear = fiscalYearRepository.findById(fiscalYear.getId()).orElseThrow();
        assertThat(reloadedYear.getStatus()).isEqualTo(PeriodStatus.CLOSED);
        assertThat(reloadedYear.isClosed()).isTrue();
    }

    @Test
    void closeYear_failsWithOpenPeriods() {
        var allPeriods = fiscalPeriodRepository.findByFiscalYearId(fiscalYear.getId());
        for (int i = 0; i < allPeriods.size() - 1; i++) {
            fiscalPeriodService.closePeriod(allPeriods.get(i).getId());
        }

        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> fiscalPeriodService.closeFiscalYear(fiscalYear.getId()))
                .isInstanceOf(Exception.class);
    }

    @Test
    void closedYear_locksAllPeriods() {
        var allPeriods = fiscalPeriodRepository.findByFiscalYearId(fiscalYear.getId());
        for (var period : allPeriods) {
            fiscalPeriodService.closePeriod(period.getId());
        }

        entityManager.flush();
        entityManager.clear();

        fiscalPeriodService.closeFiscalYear(fiscalYear.getId());

        entityManager.flush();
        entityManager.clear();

        var lockedPeriods = fiscalPeriodRepository.findByFiscalYearId(fiscalYear.getId());
        for (var period : lockedPeriods) {
            assertThat(period.getStatus()).isEqualTo(PeriodStatus.LOCKED);
        }
    }

    @Test
    void closedYear_preventsPostingToAnyPeriod() {
        var allPeriods = fiscalPeriodRepository.findByFiscalYearId(fiscalYear.getId());
        for (var period : allPeriods) {
            fiscalPeriodService.closePeriod(period.getId());
        }
        fiscalPeriodService.closeFiscalYear(fiscalYear.getId());

        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> fiscalPeriodService.validatePostingAllowed(LocalDate.of(2025, 6, 15)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void closePeriodSequentially_allClose() {
        var allPeriods = fiscalPeriodRepository.findByFiscalYearId(fiscalYear.getId());
        assertThat(allPeriods).hasSize(12);

        for (var period : allPeriods) {
            var closed = fiscalPeriodService.closePeriod(period.getId());
            assertThat(closed.getStatus()).isEqualTo(PeriodStatus.CLOSED);
        }

        entityManager.flush();
        entityManager.clear();

        var reloadedPeriods = fiscalPeriodRepository.findByFiscalYearId(fiscalYear.getId());
        long closedCount = reloadedPeriods.stream()
                .filter(p -> p.getStatus() == PeriodStatus.CLOSED)
                .count();
        assertThat(closedCount).isEqualTo(12);
    }

    private void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("FINANCE_GL_POST"),
                    new SimpleGrantedAuthority("FINANCE_GL_VIEW"),
                    new SimpleGrantedAuthority("FINANCE_PERIOD_CLOSE"),
                    new SimpleGrantedAuthority("FINANCE_PERIOD_MANAGE"),
                    new SimpleGrantedAuthority("FINANCE_YEAR_CLOSE")));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setCurrentTenant(tenant.getId());
    }

    private void setupChartOfAccounts() {
        createAccount("1110", "Cash", AccountType.ASSET, NormalBalance.DEBIT);
        createAccount("1130", "Accounts Receivable", AccountType.ASSET, NormalBalance.DEBIT);
        createAccount("1140", "Inventory", AccountType.ASSET, NormalBalance.DEBIT);
        createAccount("2110", "Accounts Payable", AccountType.LIABILITY, NormalBalance.CREDIT);
        createAccount("3100", "Retained Earnings", AccountType.EQUITY, NormalBalance.CREDIT);
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
}
