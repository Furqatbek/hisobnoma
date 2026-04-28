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
import com.hisobnoma.platform.finance.service.JournalEntryService;
import com.hisobnoma.platform.finance.dto.CreateJournalEntryRequest;
import com.hisobnoma.platform.finance.dto.CreateJournalLineRequest;
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
 * Cross-module integration test: Period Close
 * Finance: Close period -> validate all entries posted -> lock
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PeriodCloseFlowTest {

    @Autowired private FiscalPeriodService fiscalPeriodService;
    @Autowired private JournalEntryService journalEntryService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private FiscalYear fiscalYear;
    private FiscalPeriod januaryPeriod;
    private FiscalPeriod februaryPeriod;
    private Account cashAccount;
    private Account revenueAccount;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Period Close Tenant").code("INT_PERIOD_CLOSE").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("periodcloseuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        setupSecurityContext();
        setupChartOfAccounts();

        fiscalYear = FiscalYear.builder()
                .year(2026)
                .name("FY 2026")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(PeriodStatus.OPEN).current(true)
                .tenantId(tenant.getId()).build();
        fiscalYearRepository.saveAndFlush(fiscalYear);

        januaryPeriod = fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                .fiscalYear(fiscalYear).periodNumber(1)
                .name("JANUARY")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 1, 31))
                .status(PeriodStatus.OPEN)
                .tenantId(tenant.getId()).build());

        februaryPeriod = fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                .fiscalYear(fiscalYear).periodNumber(2)
                .name("FEBRUARY")
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 28))
                .status(PeriodStatus.OPEN)
                .tenantId(tenant.getId()).build());

        for (int m = 3; m <= 12; m++) {
            LocalDate start = LocalDate.of(2026, m, 1);
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
    void closePeriod_setsStatusToClosed() {
        var closedPeriod = fiscalPeriodService.closePeriod(januaryPeriod.getId());
        assertThat(closedPeriod.getStatus()).isEqualTo(PeriodStatus.CLOSED);

        entityManager.flush();
        entityManager.clear();

        var reloaded = fiscalPeriodRepository.findById(januaryPeriod.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PeriodStatus.CLOSED);
    }

    @Test
    void closedPeriod_preventsPosting() {
        fiscalPeriodService.closePeriod(januaryPeriod.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> fiscalPeriodService.validatePostingAllowed(LocalDate.of(2026, 1, 15)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void openPeriod_allowsPosting() {
        fiscalPeriodService.validatePostingAllowed(LocalDate.of(2026, 2, 15));

        var entry = journalEntryService.createAndPostEntry(CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2026, 2, 15))
                .description("Test entry in open period")
                .source(JournalSource.MANUAL)
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(cashAccount.getId())
                                .debitAmount(new BigDecimal("100000"))
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(revenueAccount.getId())
                                .creditAmount(new BigDecimal("100000"))
                                .build()))
                .build(), tenant.getId());

        assertThat(entry).isNotNull();
        assertThat(entry.getStatus()).isEqualTo(JournalStatus.POSTED);
    }

    @Test
    void postEntry_thenClosePeriod_entryRemains() {
        var entry = journalEntryService.createAndPostEntry(CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2026, 1, 20))
                .description("Pre-close entry")
                .source(JournalSource.MANUAL)
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(cashAccount.getId())
                                .debitAmount(new BigDecimal("50000"))
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(revenueAccount.getId())
                                .creditAmount(new BigDecimal("50000"))
                                .build()))
                .build(), tenant.getId());

        entityManager.flush();
        entityManager.clear();

        fiscalPeriodService.closePeriod(januaryPeriod.getId());

        entityManager.flush();
        entityManager.clear();

        var entryAfterClose = journalEntryRepository.findById(entry.getId()).orElseThrow();
        assertThat(entryAfterClose.getStatus()).isEqualTo(JournalStatus.POSTED);
    }

    @Test
    void closePeriod_updatesAccountBalances() {
        journalEntryService.createAndPostEntry(CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.of(2026, 1, 15))
                .description("Test entry for balance update")
                .source(JournalSource.MANUAL)
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(cashAccount.getId())
                                .debitAmount(new BigDecimal("200000"))
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(revenueAccount.getId())
                                .creditAmount(new BigDecimal("200000"))
                                .build()))
                .build(), tenant.getId());

        entityManager.flush();
        entityManager.clear();

        var cash = accountRepository.findByCodeAndTenantId("1110", tenant.getId()).orElseThrow();
        assertThat(cash.getYtdDebit()).isGreaterThan(BigDecimal.ZERO);
    }

    private void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("FINANCE_GL_POST"),
                    new SimpleGrantedAuthority("FINANCE_GL_VIEW"),
                    new SimpleGrantedAuthority("FINANCE_PERIOD_CLOSE"),
                    new SimpleGrantedAuthority("FINANCE_PERIOD_MANAGE")));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setCurrentTenant(tenant.getId());
    }

    private void setupChartOfAccounts() {
        cashAccount = accountRepository.saveAndFlush(Account.builder()
                .code("1110").name("Cash")
                .accountType(AccountType.ASSET).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("1130").name("Accounts Receivable")
                .accountType(AccountType.ASSET).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("1140").name("Inventory")
                .accountType(AccountType.ASSET).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("2110").name("Accounts Payable")
                .accountType(AccountType.LIABILITY).normalBalance(NormalBalance.CREDIT)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
        revenueAccount = accountRepository.saveAndFlush(Account.builder()
                .code("4100").name("Sales Revenue")
                .accountType(AccountType.REVENUE).normalBalance(NormalBalance.CREDIT)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("4200").name("Sales Discounts")
                .accountType(AccountType.REVENUE).normalBalance(NormalBalance.CREDIT)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("4300").name("Purchase Discounts")
                .accountType(AccountType.REVENUE).normalBalance(NormalBalance.CREDIT)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("5100").name("Cost of Goods Sold")
                .accountType(AccountType.EXPENSE).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("5200").name("Purchase Expense")
                .accountType(AccountType.EXPENSE).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("6100").name("Salary Expense")
                .accountType(AccountType.EXPENSE).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("1400").name("Salary Advances")
                .accountType(AccountType.ASSET).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());
    }
}
