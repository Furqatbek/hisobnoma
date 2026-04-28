package com.hisobnoma.platform.integration;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.finance.service.JournalEntryService;
import com.hisobnoma.platform.finance.dto.CreateJournalEntryRequest;
import com.hisobnoma.platform.finance.dto.CreateJournalLineRequest;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.repository.*;
import com.hisobnoma.platform.inventory.service.StockService;
import com.hisobnoma.platform.reports.entity.ReportDefinition;
import com.hisobnoma.platform.reports.repository.ReportDefinitionRepository;
import com.hisobnoma.platform.reports.service.ReportService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
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
 * Cross-module integration test: Report Accuracy
 * Reports -> Finance/Inventory/POS: verify report data matches source
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportAccuracyFlowTest {

    @Autowired private ReportService reportService;
    @Autowired private StockService stockService;
    @Autowired private JournalEntryService journalEntryService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LocationRepository locationRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private ReportDefinitionRepository reportDefinitionRepository;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Location location;
    private Product product1;
    private Product product2;
    private Account cashAccount;
    private Account revenueAccount;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Report Accuracy Tenant").code("INT_RPT_ACC").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("rptaccuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        setupSecurityContext();

        location = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-RPT-001").name("Report Store")
                .locationType(LocationType.STORE)
                .active(true).tenantId(tenant.getId()).build());

        Category category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-RPT").name("Report Category")
                .active(true).tenantId(tenant.getId()).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS-RP").name("Pieces").symbol("pcs")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).tenantId(tenant.getId()).build());

        product1 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-RPT-001").name("Report Product A")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("50000"))
                .costPrice(new BigDecimal("30000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        product2 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-RPT-002").name("Report Product B")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("80000"))
                .costPrice(new BigDecimal("50000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        setupChartOfAccounts();
        setupFiscalPeriods();
        setupInitialStock();
        setupReportDefinitions();

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void stockValuation_matchesDirectQuery() {
        var valuation = stockService.getStockValuation();
        assertThat(valuation).isNotNull();

        var allStock = stockRepository.findAll().stream()
                .filter(s -> s.getTenantId().equals(tenant.getId()))
                .toList();

        BigDecimal directTotal = allStock.stream()
                .map(s -> s.getQuantityOnHand().multiply(s.getAverageCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(valuation.getTotalValue()).isEqualByComparingTo(directTotal);
    }

    @Test
    void accountBalances_matchJournalEntries() {
        journalEntryService.createAndPostEntry(CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description("Test entry for balance verification")
                .source(JournalSource.MANUAL)
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(cashAccount.getId())
                                .debitAmount(new BigDecimal("500000"))
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(revenueAccount.getId())
                                .creditAmount(new BigDecimal("500000"))
                                .build()))
                .build(), tenant.getId());

        entityManager.flush();
        entityManager.clear();

        var cash = accountRepository.findByCodeAndTenantId("1110", tenant.getId()).orElseThrow();
        assertThat(cash.getYtdDebit()).isEqualByComparingTo(new BigDecimal("500000"));

        var revenue = accountRepository.findByCodeAndTenantId("4100", tenant.getId()).orElseThrow();
        assertThat(revenue.getYtdCredit()).isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    void journalEntries_alwaysBalanced() {
        journalEntryService.createAndPostEntry(CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description("Balance test entry 1")
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

        journalEntryService.createAndPostEntry(CreateJournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description("Balance test entry 2")
                .source(JournalSource.MANUAL)
                .lines(List.of(
                        CreateJournalLineRequest.builder()
                                .accountId(cashAccount.getId())
                                .debitAmount(new BigDecimal("250000"))
                                .build(),
                        CreateJournalLineRequest.builder()
                                .accountId(revenueAccount.getId())
                                .creditAmount(new BigDecimal("250000"))
                                .build()))
                .build(), tenant.getId());

        entityManager.flush();
        entityManager.clear();

        var allEntries = journalEntryRepository.findAll().stream()
                .filter(e -> e.getTenantId().equals(tenant.getId()))
                .toList();

        for (var entry : allEntries) {
            assertThat(entry.getTotalDebit()).isEqualByComparingTo(entry.getTotalCredit());
        }
    }

    @Test
    void reportDefinitions_accessibleAndActive() {
        var reports = reportService.getAvailableReports();
        assertThat(reports).isNotEmpty();
        reports.forEach(r -> assertThat(r.isActive()).isTrue());
    }

    @Test
    void inventoryReport_definitionExists() {
        var inventoryReports = reportService.getReportsByCategory(
                ReportDefinition.ReportCategory.INVENTORY, PageRequest.of(0, 20));
        assertThat(inventoryReports.getContent()).isNotEmpty();
    }

    @Test
    void financialReport_definitionExists() {
        var financialReports = reportService.getReportsByCategory(
                ReportDefinition.ReportCategory.FINANCIAL, PageRequest.of(0, 20));
        assertThat(financialReports.getContent()).isNotEmpty();
    }

    @Test
    void stockQuantities_consistentBetweenServiceAndRepository() {
        var serviceStocks = stockService.getStockByProduct(product1.getId());
        BigDecimal serviceTotal = serviceStocks.stream()
                .map(s -> s.getQuantityOnHand())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var repoStocks = stockRepository.findByProductIdAndTenantId(product1.getId(), tenant.getId());
        BigDecimal repoTotal = repoStocks.stream()
                .map(s -> s.getQuantityOnHand())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(serviceTotal).isEqualByComparingTo(repoTotal);
    }

    private void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("REPORT_VIEW"),
                    new SimpleGrantedAuthority("REPORT_INVENTORY_VIEW"),
                    new SimpleGrantedAuthority("REPORT_FINANCIAL_VIEW"),
                    new SimpleGrantedAuthority("REPORT_SALES_VIEW"),
                    new SimpleGrantedAuthority("INVENTORY_STOCK_VIEW"),
                    new SimpleGrantedAuthority("FINANCE_GL_VIEW"),
                    new SimpleGrantedAuthority("FINANCE_GL_POST")));
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

    private void setupFiscalPeriods() {
        FiscalYear fy = FiscalYear.builder()
                .year(LocalDate.now().getYear())
                .name("FY " + LocalDate.now().getYear())
                .startDate(LocalDate.of(LocalDate.now().getYear(), 1, 1))
                .endDate(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .status(PeriodStatus.OPEN).current(true)
                .tenantId(tenant.getId()).build();
        fiscalYearRepository.saveAndFlush(fy);

        for (int m = 1; m <= 12; m++) {
            LocalDate start = LocalDate.of(fy.getYear(), m, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                    .fiscalYear(fy).periodNumber(m)
                    .name(start.getMonth().toString())
                    .startDate(start).endDate(end)
                    .status(PeriodStatus.OPEN)
                    .tenantId(tenant.getId()).build());
        }
    }

    private void setupInitialStock() {
        stockRepository.saveAndFlush(Stock.builder()
                .product(product1).location(location)
                .quantityOnHand(new BigDecimal("100"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("30000"))
                .tenantId(tenant.getId()).build());

        stockRepository.saveAndFlush(Stock.builder()
                .product(product2).location(location)
                .quantityOnHand(new BigDecimal("75"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("50000"))
                .tenantId(tenant.getId()).build());
    }

    private void setupReportDefinitions() {
        reportDefinitionRepository.saveAndFlush(ReportDefinition.builder()
                .code("INV-STOCK-VAL")
                .name("Inventory Stock Valuation")
                .description("Current stock valuation across all locations")
                .category(ReportDefinition.ReportCategory.INVENTORY)
                .active(true).systemReport(true)
                .tenantId(tenant.getId()).build());

        reportDefinitionRepository.saveAndFlush(ReportDefinition.builder()
                .code("FIN-TRIAL-BAL")
                .name("Trial Balance")
                .description("Trial balance for all GL accounts")
                .category(ReportDefinition.ReportCategory.FINANCIAL)
                .active(true).systemReport(true)
                .tenantId(tenant.getId()).build());

        reportDefinitionRepository.saveAndFlush(ReportDefinition.builder()
                .code("SALES-SUMMARY")
                .name("Sales Summary")
                .description("Summary of sales transactions")
                .category(ReportDefinition.ReportCategory.SALES)
                .active(true).systemReport(true)
                .tenantId(tenant.getId()).build());
    }
}
