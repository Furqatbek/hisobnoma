package com.hisobnoma.platform.integration;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import com.hisobnoma.platform.delivery.repository.DeliveryRegionRepository;
import com.hisobnoma.platform.delivery.repository.DeliveryVillageRepository;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.repository.*;
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
 * Cross-module integration test: Delivery Costing
 * POS -> Delivery -> Finance: Order -> delivery region tracking -> revenue
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeliveryCostingFlowTest {

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
    @Autowired private DeliveryRegionRepository deliveryRegionRepository;
    @Autowired private DeliveryVillageRepository deliveryVillageRepository;
    @Autowired private POSTransactionRepository posTransactionRepository;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Location location;
    private Product product;
    private POSTerminal terminal;
    private Shift shift;
    private DeliveryRegion region;
    private DeliveryVillage village;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Delivery Cost Tenant").code("INT_DELIV_COST").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("delivcostuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        setupSecurityContext();

        location = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-DEL-001").name("Delivery Store")
                .locationType(LocationType.STORE)
                .active(true).allowNegativeStock(true)
                .tenantId(tenant.getId()).build());

        Category category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-DEL").name("Delivery Category")
                .active(true).tenantId(tenant.getId()).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS-DL").name("Pieces").symbol("pcs")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).tenantId(tenant.getId()).build());

        product = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-DEL-001").name("Delivery Product A")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("75000"))
                .costPrice(new BigDecimal("45000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        region = deliveryRegionRepository.saveAndFlush(DeliveryRegion.builder()
                .name("Tashkent Region").code("TASH-001")
                .active(true).sortOrder(1)
                .tenantId(tenant.getId()).build());

        village = deliveryVillageRepository.saveAndFlush(DeliveryVillage.builder()
                .name("Chirchiq").code("CHR-001")
                .region(region)
                .active(true).sortOrder(1)
                .tenantId(tenant.getId()).build());

        setupChartOfAccounts();
        setupFiscalPeriods();
        setupInitialStock();

        terminal = terminalRepository.saveAndFlush(POSTerminal.builder()
                .terminalCode("T-DEL-001").name("Delivery Terminal")
                .location(location).active(true)
                .tenantId(tenant.getId()).build());

        shift = shiftRepository.saveAndFlush(Shift.builder()
                .shiftNumber("SH-DEL-001")
                .terminal(terminal)
                .cashierId(user.getId())
                .cashierName("delivcostuser")
                .status(ShiftStatus.OPEN)
                .openedAt(Instant.now())
                .openingCash(new BigDecimal("200000"))
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
    void saleWithDeliveryRegion_tracksRegionInfo() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .deliveryRegionId(region.getId())
                .deliveryVillageId(village.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("2"))
                        .build()))
                .build());

        assertThat(txDto.getDeliveryRegionId()).isEqualTo(region.getId());
        assertThat(txDto.getDeliveryVillageId()).isEqualTo(village.getId());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("150000"))
                .tenderedAmount(new BigDecimal("150000"))
                .build());

        var completed = posTransactionService.completeTransaction(txDto.getId());
        assertThat(completed.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(completed.getDeliveryRegionId()).isEqualTo(region.getId());
    }

    @Test
    void saleWithoutDelivery_noRegionTracked() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("1"))
                        .build()))
                .build());

        assertThat(txDto.getDeliveryRegionId()).isNull();

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("75000"))
                .tenderedAmount(new BigDecimal("75000"))
                .build());

        var completed = posTransactionService.completeTransaction(txDto.getId());
        assertThat(completed.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void multipleDeliverySales_differentRegions() {
        DeliveryRegion region2 = deliveryRegionRepository.saveAndFlush(DeliveryRegion.builder()
                .name("Samarkand Region").code("SAM-001")
                .active(true).sortOrder(2)
                .tenantId(tenant.getId()).build());

        entityManager.flush();
        entityManager.clear();

        var tx1 = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .deliveryRegionId(region.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("1"))
                        .build()))
                .build());

        posPaymentService.addPayment(tx1.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("75000"))
                .tenderedAmount(new BigDecimal("75000"))
                .build());
        var completed1 = posTransactionService.completeTransaction(tx1.getId());

        var tx2 = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .deliveryRegionId(region2.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("1"))
                        .build()))
                .build());

        posPaymentService.addPayment(tx2.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("75000"))
                .tenderedAmount(new BigDecimal("75000"))
                .build());
        var completed2 = posTransactionService.completeTransaction(tx2.getId());

        assertThat(completed1.getDeliveryRegionId()).isEqualTo(region.getId());
        assertThat(completed2.getDeliveryRegionId()).isEqualTo(region2.getId());
    }

    @Test
    void deliverySale_stockDeductedNormally() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .deliveryRegionId(region.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("5"))
                        .build()))
                .build());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("375000"))
                .tenderedAmount(new BigDecimal("375000"))
                .build());

        var completed = posTransactionService.completeTransaction(txDto.getId());
        assertThat(completed.isStockDeducted()).isTrue();

        entityManager.flush();
        entityManager.clear();

        var stock = stockRepository.findByProductIdAndTenantId(product.getId(), tenant.getId())
                .stream().filter(s -> s.getLocation().getId().equals(location.getId()))
                .findFirst().orElseThrow();
        assertThat(stock.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("95"));
    }

    private void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("POS_SALE_CREATE"),
                    new SimpleGrantedAuthority("POS_SALE_READ"),
                    new SimpleGrantedAuthority("POS_PAYMENT_PROCESS"),
                    new SimpleGrantedAuthority("DELIVERY_VIEW"),
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
                .product(product).location(location)
                .quantityOnHand(new BigDecimal("100"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("45000"))
                .tenantId(tenant.getId()).build());
    }
}
