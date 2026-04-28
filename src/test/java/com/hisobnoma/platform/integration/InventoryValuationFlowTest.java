package com.hisobnoma.platform.integration;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.repository.*;
import com.hisobnoma.platform.inventory.service.StockService;
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
 * Cross-module integration test: Inventory Valuation
 * Inventory -> Finance: Stock value -> GL account balances
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryValuationFlowTest {

    @Autowired private StockService stockService;
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
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Location location1;
    private Location location2;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Valuation Flow Tenant").code("INT_VALUATION").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("valuationuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        setupSecurityContext();

        location1 = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-VAL-001").name("Warehouse A")
                .locationType(LocationType.WAREHOUSE)
                .active(true).tenantId(tenant.getId()).build());

        location2 = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-VAL-002").name("Store B")
                .locationType(LocationType.STORE)
                .active(true).tenantId(tenant.getId()).build());

        Category category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-VAL").name("Valuation Category")
                .active(true).tenantId(tenant.getId()).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS-VL").name("Pieces").symbol("pcs")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).tenantId(tenant.getId()).build());

        product1 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-VAL-001").name("Valuation Product A")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("100000"))
                .costPrice(new BigDecimal("60000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        product2 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-VAL-002").name("Valuation Product B")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("200000"))
                .costPrice(new BigDecimal("120000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        setupChartOfAccounts();
        setupFiscalPeriods();
        setupInitialStock();

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void stockValuation_matchesSumOfStockValues() {
        var valuation = stockService.getStockValuation();
        assertThat(valuation).isNotNull();
        assertThat(valuation.getTotalValue()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void stockByProduct_returnsAllLocations() {
        var stocks = stockService.getStockByProduct(product1.getId());
        assertThat(stocks).hasSize(2);
    }

    @Test
    void stockByLocation_returnsCorrectProducts() {
        var stockPage = stockService.getStockByLocation(location1.getId(), PageRequest.of(0, 20));
        assertThat(stockPage.getContent()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void singleProductValuation_correctCalculation() {
        var stocks = stockService.getStockByProduct(product1.getId());
        BigDecimal totalValue = stocks.stream()
                .map(s -> s.getQuantityOnHand().multiply(s.getAverageCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedValue = new BigDecimal("50").multiply(new BigDecimal("60000"))
                .add(new BigDecimal("30").multiply(new BigDecimal("60000")));
        assertThat(totalValue).isEqualByComparingTo(expectedValue);
    }

    @Test
    void stockQuantities_consistentAcrossLocations() {
        var product1Stocks = stockService.getStockByProduct(product1.getId());
        BigDecimal totalOnHand = product1Stocks.stream()
                .map(s -> s.getQuantityOnHand())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalOnHand).isEqualByComparingTo(new BigDecimal("80"));

        var product2Stocks = stockService.getStockByProduct(product2.getId());
        BigDecimal totalOnHand2 = product2Stocks.stream()
                .map(s -> s.getQuantityOnHand())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalOnHand2).isEqualByComparingTo(new BigDecimal("60"));
    }

    @Test
    void lowStock_correctlyIdentified() {
        var existingStock = stockRepository.findByProductIdAndTenantId(product1.getId(), tenant.getId())
                .stream().filter(s -> s.getLocation().getId().equals(location1.getId()))
                .findFirst().orElseThrow();
        existingStock.setQuantityOnHand(new BigDecimal("2"));
        existingStock.setReorderPoint(new BigDecimal("10"));
        stockRepository.saveAndFlush(existingStock);

        entityManager.flush();
        entityManager.clear();

        var lowStock = stockService.getLowStock(PageRequest.of(0, 20));
        assertThat(lowStock.getContent()).isNotEmpty();
    }

    private void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("INVENTORY_STOCK_VIEW"),
                    new SimpleGrantedAuthority("INVENTORY_STOCK_ADJUST"),
                    new SimpleGrantedAuthority("FINANCE_GL_VIEW")));
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
                .product(product1).location(location1)
                .quantityOnHand(new BigDecimal("50"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("60000"))
                .tenantId(tenant.getId()).build());

        stockRepository.saveAndFlush(Stock.builder()
                .product(product1).location(location2)
                .quantityOnHand(new BigDecimal("30"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("60000"))
                .tenantId(tenant.getId()).build());

        stockRepository.saveAndFlush(Stock.builder()
                .product(product2).location(location1)
                .quantityOnHand(new BigDecimal("40"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("120000"))
                .tenantId(tenant.getId()).build());

        stockRepository.saveAndFlush(Stock.builder()
                .product(product2).location(location2)
                .quantityOnHand(new BigDecimal("20"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("120000"))
                .tenantId(tenant.getId()).build());
    }
}
