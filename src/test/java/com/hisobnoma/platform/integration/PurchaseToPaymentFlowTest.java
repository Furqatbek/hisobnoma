package com.hisobnoma.platform.integration;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.inventory.dto.CreatePurchaseOrderLineRequest;
import com.hisobnoma.platform.inventory.dto.CreatePurchaseOrderRequest;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.entity.POStatus;
import com.hisobnoma.platform.inventory.repository.*;
import com.hisobnoma.platform.inventory.service.PurchaseOrderService;
import com.hisobnoma.platform.inventory.service.ReceivingService;
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
 * Cross-module integration test: Purchase to Payment
 * Inventory -> Finance: PO -> Receive -> AP Invoice -> Payment
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PurchaseToPaymentFlowTest {

    @Autowired private PurchaseOrderService purchaseOrderService;
    @Autowired private ReceivingService receivingService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LocationRepository locationRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private APInvoiceRepository apInvoiceRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Location location;
    private Product product;
    private Vendor vendor;
    private UnitOfMeasure uom;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Purchase Flow Tenant").code("INT_PO_FLOW").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("poflowuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        setupSecurityContext();

        location = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-PO-001").name("Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true).allowNegativeStock(false)
                .tenantId(tenant.getId()).build());

        Category category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-PO").name("PO Category")
                .active(true).tenantId(tenant.getId()).build());

        uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS-PO").name("Pieces").symbol("pcs")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).tenantId(tenant.getId()).build());

        product = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-PO-001").name("Purchase Product A")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("80000"))
                .costPrice(new BigDecimal("50000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        vendor = vendorRepository.saveAndFlush(Vendor.builder()
                .code("VND-PO-001").name("Test Vendor")
                .contactPerson("John Doe")
                .phone("+998901111111")
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
    void createAndApprovePO_thenReceive_updatesStock() {
        var poDto = purchaseOrderService.createPurchaseOrder(CreatePurchaseOrderRequest.builder()
                .vendorId(vendor.getId())
                .locationId(location.getId())
                .orderDate(LocalDate.now())
                .expectedDate(LocalDate.now().plusDays(7))
                .lines(List.of(CreatePurchaseOrderLineRequest.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("20"))
                        .uomId(uom.getId())
                        .unitPrice(new BigDecimal("50000"))
                        .build()))
                .build());

        assertThat(poDto.getStatus()).isEqualTo(POStatus.DRAFT);
        assertThat(poDto.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1000000"));

        var approved = purchaseOrderService.approvePurchaseOrder(poDto.getId());
        assertThat(approved.getStatus()).isEqualTo(POStatus.APPROVED);

        entityManager.flush();
        entityManager.clear();

        var received = purchaseOrderService.receivePurchaseOrder(poDto.getId());
        assertThat(received.getStatus()).isIn(POStatus.RECEIVED, POStatus.PARTIAL);

        entityManager.flush();
        entityManager.clear();

        var stocks = stockRepository.findByProductIdAndTenantId(product.getId(), tenant.getId());
        assertThat(stocks).isNotEmpty();
        var stock = stocks.stream()
                .filter(s -> s.getLocation().getId().equals(location.getId()))
                .findFirst().orElseThrow();
        assertThat(stock.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    void receivePO_createsAPInvoice() {
        var poDto = purchaseOrderService.createPurchaseOrder(CreatePurchaseOrderRequest.builder()
                .vendorId(vendor.getId())
                .locationId(location.getId())
                .orderDate(LocalDate.now())
                .lines(List.of(CreatePurchaseOrderLineRequest.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("10"))
                        .uomId(uom.getId())
                        .unitPrice(new BigDecimal("50000"))
                        .build()))
                .build());

        purchaseOrderService.approvePurchaseOrder(poDto.getId());

        entityManager.flush();
        entityManager.clear();

        purchaseOrderService.receivePurchaseOrder(poDto.getId());

        entityManager.flush();
        entityManager.clear();

        var apInvoices = apInvoiceRepository.findAll().stream()
                .filter(inv -> inv.getTenantId().equals(tenant.getId()))
                .toList();
        assertThat(apInvoices).isNotEmpty();
        var apInvoice = apInvoices.get(0);
        assertThat(apInvoice.getTotalAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(apInvoice.getVendorId()).isEqualTo(vendor.getId());
    }

    @Test
    void purchaseFlow_POTotalMatchesLineCalculation() {
        var poDto = purchaseOrderService.createPurchaseOrder(CreatePurchaseOrderRequest.builder()
                .vendorId(vendor.getId())
                .locationId(location.getId())
                .orderDate(LocalDate.now())
                .lines(List.of(
                        CreatePurchaseOrderLineRequest.builder()
                                .productId(product.getId())
                                .quantity(new BigDecimal("5"))
                                .uomId(uom.getId())
                                .unitPrice(new BigDecimal("50000"))
                                .build()))
                .build());

        assertThat(poDto.getTotalAmount()).isEqualByComparingTo(new BigDecimal("250000"));
    }

    @Test
    void multiplePOReceivings_accumulateStock() {
        var po1 = purchaseOrderService.createPurchaseOrder(CreatePurchaseOrderRequest.builder()
                .vendorId(vendor.getId())
                .locationId(location.getId())
                .orderDate(LocalDate.now())
                .lines(List.of(CreatePurchaseOrderLineRequest.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("10"))
                        .uomId(uom.getId())
                        .unitPrice(new BigDecimal("50000"))
                        .build()))
                .build());
        purchaseOrderService.approvePurchaseOrder(po1.getId());
        purchaseOrderService.receivePurchaseOrder(po1.getId());

        entityManager.flush();
        entityManager.clear();

        var po2 = purchaseOrderService.createPurchaseOrder(CreatePurchaseOrderRequest.builder()
                .vendorId(vendor.getId())
                .locationId(location.getId())
                .orderDate(LocalDate.now())
                .lines(List.of(CreatePurchaseOrderLineRequest.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("15"))
                        .uomId(uom.getId())
                        .unitPrice(new BigDecimal("55000"))
                        .build()))
                .build());
        purchaseOrderService.approvePurchaseOrder(po2.getId());
        purchaseOrderService.receivePurchaseOrder(po2.getId());

        entityManager.flush();
        entityManager.clear();

        var stocks = stockRepository.findByProductIdAndTenantId(product.getId(), tenant.getId());
        assertThat(stocks).isNotEmpty();
        var stock = stocks.stream()
                .filter(s -> s.getLocation().getId().equals(location.getId()))
                .findFirst().orElseThrow();
        assertThat(stock.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("25"));
    }

    private void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("INVENTORY_PO_CREATE"),
                    new SimpleGrantedAuthority("INVENTORY_PO_APPROVE"),
                    new SimpleGrantedAuthority("INVENTORY_PO_READ"),
                    new SimpleGrantedAuthority("INVENTORY_RECEIVING_CREATE"),
                    new SimpleGrantedAuthority("INVENTORY_RECEIVING_CONFIRM"),
                    new SimpleGrantedAuthority("INVENTORY_STOCK_VIEW"),
                    new SimpleGrantedAuthority("FINANCE_AP_CREATE"),
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
}
