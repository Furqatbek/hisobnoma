package com.hisobnoma.platform.integration;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.FiscalPeriodRepository;
import com.hisobnoma.platform.finance.repository.FiscalYearRepository;
import com.hisobnoma.platform.finance.repository.JournalEntryRepository;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.entity.MovementType;
import com.hisobnoma.platform.inventory.repository.*;
import org.springframework.data.domain.PageRequest;
import com.hisobnoma.platform.pos.dto.AddPaymentRequest;
import com.hisobnoma.platform.pos.dto.CreateTransactionRequest;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.repository.POSTerminalRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import com.hisobnoma.platform.pos.repository.ShiftRepository;
import com.hisobnoma.platform.pos.service.POSPaymentService;
import com.hisobnoma.platform.pos.service.POSTransactionService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-module integration test: Complete Sale Flow
 * POS -> Inventory -> Finance: stock deduction + revenue journal entry
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CompleteSaleFlowTest {

    @Autowired private POSTransactionService posTransactionService;
    @Autowired private POSPaymentService posPaymentService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LocationRepository locationRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private POSTerminalRepository terminalRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private POSTransactionRepository posTransactionRepository;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Location location;
    private Product product;
    private POSTerminal terminal;
    private Shift shift;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Complete Sale Flow Tenant").code("INT_SALE_FLOW").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("salesflowuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        setupSecurityContext();

        location = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-SALE-001").name("Sales Store")
                .locationType(LocationType.STORE)
                .active(true).allowNegativeStock(true)
                .tenantId(tenant.getId()).build());

        Category category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-SALE").name("Sale Category")
                .active(true).tenantId(tenant.getId()).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS-SF").name("Pieces").symbol("pcs")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).tenantId(tenant.getId()).build());

        product = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-SALE-001").name("Test Product A")
                .barcode("BC-SALE-001")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("50000"))
                .costPrice(new BigDecimal("30000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        setupChartOfAccounts();
        setupFiscalPeriods();
        setupInitialStock();

        terminal = terminalRepository.saveAndFlush(POSTerminal.builder()
                .terminalCode("T-SALE-001").name("Sales Terminal")
                .location(location).active(true)
                .tenantId(tenant.getId()).build());

        shift = shiftRepository.saveAndFlush(Shift.builder()
                .shiftNumber("SH-SALE-001")
                .terminal(terminal)
                .cashierId(user.getId())
                .cashierName("salesflowuser")
                .status(ShiftStatus.OPEN)
                .openedAt(Instant.now())
                .openingCash(new BigDecimal("100000"))
                .tenantId(tenant.getId()).build());

        terminal.setCurrentShiftId(shift.getId());
        terminalRepository.saveAndFlush(terminal);

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void completeSale_deductsStockAndPostsToGL() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("3"))
                        .build()))
                .build());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("150000"))
                .tenderedAmount(new BigDecimal("150000"))
                .build());

        var completedDto = posTransactionService.completeTransaction(txDto.getId());

        assertThat(completedDto.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(completedDto.isStockDeducted()).isTrue();

        entityManager.flush();
        entityManager.clear();

        var stocks = stockRepository.findByProductIdAndTenantId(product.getId(), tenant.getId());
        assertThat(stocks).isNotEmpty();
        var stock = stocks.stream()
                .filter(s -> s.getLocation().getId().equals(location.getId()))
                .findFirst().orElseThrow();
        assertThat(stock.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("97"));
        assertThat(stock.getQuantityReserved()).isEqualByComparingTo(BigDecimal.ZERO);

        var movements = stockMovementRepository.findByProductIdAndTenantIdOrderByMovementDateDesc(
                product.getId(), tenant.getId(), PageRequest.of(0, 100));
        assertThat(movements.getContent()).isNotEmpty();
        boolean hasStockOut = movements.getContent().stream()
                .anyMatch(m -> m.getMovementType() == MovementType.STOCK_OUT);
        assertThat(hasStockOut).isTrue();
    }

    @Test
    void completeSale_withMultipleItems_allStockDeducted() {
        Product product2 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-SALE-002").name("Test Product B")
                .category(product.getCategory()).baseUom(product.getBaseUom())
                .sellingPrice(new BigDecimal("25000"))
                .costPrice(new BigDecimal("15000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        Stock stock2 = Stock.builder()
                .product(product2).location(location)
                .quantityOnHand(new BigDecimal("50"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("15000"))
                .tenantId(tenant.getId()).build();
        stockRepository.saveAndFlush(stock2);
        entityManager.flush();
        entityManager.clear();

        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .items(List.of(
                        CreateTransactionRequest.LineItem.builder()
                                .productId(product.getId())
                                .quantity(new BigDecimal("2"))
                                .build(),
                        CreateTransactionRequest.LineItem.builder()
                                .productId(product2.getId())
                                .quantity(new BigDecimal("5"))
                                .build()))
                .build());

        BigDecimal totalAmount = txDto.getTotalAmount();
        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(totalAmount)
                .tenderedAmount(totalAmount)
                .build());

        posTransactionService.completeTransaction(txDto.getId());
        entityManager.flush();
        entityManager.clear();

        var stock1After = stockRepository.findByProductIdAndTenantId(product.getId(), tenant.getId())
                .stream().filter(s -> s.getLocation().getId().equals(location.getId())).findFirst().orElseThrow();
        assertThat(stock1After.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("98"));

        var stock2After = stockRepository.findByProductIdAndTenantId(product2.getId(), tenant.getId())
                .stream().filter(s -> s.getLocation().getId().equals(location.getId())).findFirst().orElseThrow();
        assertThat(stock2After.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("45"));
    }

    @Test
    void completeSale_transactionAmountsConsistent() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("2"))
                        .build()))
                .build());

        assertThat(txDto.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100000"));

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("100000"))
                .tenderedAmount(new BigDecimal("100000"))
                .build());

        var completed = posTransactionService.completeTransaction(txDto.getId());
        assertThat(completed.getPaidAmount()).isGreaterThanOrEqualTo(completed.getTotalAmount());
        assertThat(completed.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void voidCompletedSale_restoresStock() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("5"))
                        .build()))
                .build());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("250000"))
                .tenderedAmount(new BigDecimal("250000"))
                .build());

        posTransactionService.completeTransaction(txDto.getId());
        entityManager.flush();
        entityManager.clear();

        var stockBefore = stockRepository.findByProductIdAndTenantId(product.getId(), tenant.getId())
                .stream().filter(s -> s.getLocation().getId().equals(location.getId())).findFirst().orElseThrow();
        assertThat(stockBefore.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("95"));

        posTransactionService.voidTransaction(txDto.getId(),
                new com.hisobnoma.platform.pos.dto.VoidTransactionRequest("Test void"));
        entityManager.flush();
        entityManager.clear();

        var stockAfter = stockRepository.findByProductIdAndTenantId(product.getId(), tenant.getId())
                .stream().filter(s -> s.getLocation().getId().equals(location.getId())).findFirst().orElseThrow();
        assertThat(stockAfter.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    void completeSale_reservationReleasedOnCompletion() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("10"))
                        .build()))
                .build());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("500000"))
                .tenderedAmount(new BigDecimal("500000"))
                .build());

        posTransactionService.completeTransaction(txDto.getId());
        entityManager.flush();
        entityManager.clear();

        var stockAfter = stockRepository.findByProductIdAndTenantId(product.getId(), tenant.getId())
                .stream().filter(s -> s.getLocation().getId().equals(location.getId())).findFirst().orElseThrow();
        assertThat(stockAfter.getQuantityReserved()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stockAfter.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("90"));
    }

    private void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("POS_SALE_CREATE"),
                    new SimpleGrantedAuthority("POS_SALE_READ"),
                    new SimpleGrantedAuthority("POS_SALE_VOID"),
                    new SimpleGrantedAuthority("POS_PAYMENT_PROCESS"),
                    new SimpleGrantedAuthority("INVENTORY_STOCK_VIEW"),
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
            FiscalPeriod fp = FiscalPeriod.builder()
                    .fiscalYear(fy).periodNumber(m)
                    .name(start.getMonth().toString())
                    .startDate(start).endDate(end)
                    .status(PeriodStatus.OPEN)
                    .tenantId(tenant.getId()).build();
            fiscalPeriodRepository.saveAndFlush(fp);
        }
    }

    private void setupInitialStock() {
        Stock stock = Stock.builder()
                .product(product).location(location)
                .quantityOnHand(new BigDecimal("100"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("30000"))
                .tenantId(tenant.getId()).build();
        stockRepository.saveAndFlush(stock);
    }
}
