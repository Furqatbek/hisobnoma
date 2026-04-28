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
import com.hisobnoma.platform.pos.dto.AddPaymentRequest;
import com.hisobnoma.platform.pos.dto.CreateTransactionRequest;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.entity.TransactionStatus;
import com.hisobnoma.platform.pos.repository.POSTerminalRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import com.hisobnoma.platform.pos.repository.ShiftRepository;
import com.hisobnoma.platform.pos.service.POSPaymentService;
import com.hisobnoma.platform.pos.service.POSTransactionService;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
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
 * Cross-module integration test: Customer Credit Sale
 * POS -> Finance: Sale on credit -> AR Invoice -> Payment tracking
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerCreditSaleFlowTest {

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
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ARInvoiceRepository arInvoiceRepository;
    @Autowired private POSTransactionRepository posTransactionRepository;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Location location;
    private Product product;
    private POSTerminal terminal;
    private Shift shift;
    private Customer customer;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Credit Sale Tenant").code("INT_CREDIT_SALE").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("creditsaleuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        setupSecurityContext();

        location = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-CRED-001").name("Credit Store")
                .locationType(LocationType.STORE)
                .active(true).allowNegativeStock(true)
                .tenantId(tenant.getId()).build());

        Category category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-CRED").name("Credit Category")
                .active(true).tenantId(tenant.getId()).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS-CR").name("Pieces").symbol("pcs")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).tenantId(tenant.getId()).build());

        product = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-CRED-001").name("Credit Product A")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("100000"))
                .costPrice(new BigDecimal("60000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        customer = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-CRED-001").name("Credit Customer")
                .phone("+998901234567")
                .creditLimit(new BigDecimal("5000000"))
                .currentBalance(BigDecimal.ZERO)
                .active(true)
                .tenantId(tenant.getId()).build());

        setupChartOfAccounts();
        setupFiscalPeriods();
        setupInitialStock();

        terminal = terminalRepository.saveAndFlush(POSTerminal.builder()
                .terminalCode("T-CRED-001").name("Credit Terminal")
                .location(location).active(true)
                .tenantId(tenant.getId()).build());

        shift = shiftRepository.saveAndFlush(Shift.builder()
                .shiftNumber("SH-CRED-001")
                .terminal(terminal)
                .cashierId(user.getId())
                .cashierName("creditsaleuser")
                .status(ShiftStatus.OPEN)
                .openedAt(Instant.now())
                .openingCash(BigDecimal.ZERO)
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
    void creditSale_createsARInvoice() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .customerId(customer.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("2"))
                        .build()))
                .build());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CREDIT)
                .amount(new BigDecimal("200000"))
                .build());

        var completed = posTransactionService.completeTransaction(txDto.getId());
        assertThat(completed.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(completed.getArInvoiceId()).isNotNull();

        entityManager.flush();
        entityManager.clear();

        var arInvoice = arInvoiceRepository.findById(completed.getArInvoiceId()).orElseThrow();
        assertThat(arInvoice.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(arInvoice.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(arInvoice.getPosTransactionId()).isEqualTo(completed.getId());
    }

    @Test
    void creditSale_updatesCustomerBalance() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .customerId(customer.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("3"))
                        .build()))
                .build());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CREDIT)
                .amount(new BigDecimal("300000"))
                .build());

        posTransactionService.completeTransaction(txDto.getId());
        entityManager.flush();
        entityManager.clear();

        var updatedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(updatedCustomer.getCurrentBalance()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void mixedPayment_partialCreditCreatesARInvoice() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .customerId(customer.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("2"))
                        .build()))
                .build());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("100000"))
                .tenderedAmount(new BigDecimal("100000"))
                .build());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CREDIT)
                .amount(new BigDecimal("100000"))
                .build());

        var completed = posTransactionService.completeTransaction(txDto.getId());
        assertThat(completed.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(completed.getArInvoiceId()).isNotNull();

        entityManager.flush();
        entityManager.clear();

        var arInvoice = arInvoiceRepository.findById(completed.getArInvoiceId()).orElseThrow();
        assertThat(arInvoice.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    void cashOnlySale_noARInvoiceCreated() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("1"))
                        .build()))
                .build());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(new BigDecimal("100000"))
                .tenderedAmount(new BigDecimal("100000"))
                .build());

        var completed = posTransactionService.completeTransaction(txDto.getId());
        assertThat(completed.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(completed.getArInvoiceId()).isNull();
    }

    @Test
    void creditSale_stockAlsoDeducted() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .customerId(customer.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("4"))
                        .build()))
                .build());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CREDIT)
                .amount(new BigDecimal("400000"))
                .build());

        var completed = posTransactionService.completeTransaction(txDto.getId());
        assertThat(completed.isStockDeducted()).isTrue();

        entityManager.flush();
        entityManager.clear();

        var stock = stockRepository.findByProductIdAndTenantId(product.getId(), tenant.getId())
                .stream().filter(s -> s.getLocation().getId().equals(location.getId())).findFirst().orElseThrow();
        assertThat(stock.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("46"));
    }

    private void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("POS_SALE_CREATE"),
                    new SimpleGrantedAuthority("POS_SALE_READ"),
                    new SimpleGrantedAuthority("POS_PAYMENT_PROCESS"),
                    new SimpleGrantedAuthority("FINANCE_AR_CREATE"),
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
                .quantityOnHand(new BigDecimal("50"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("60000"))
                .tenantId(tenant.getId()).build());
    }
}
