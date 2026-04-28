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
import com.hisobnoma.platform.inventory.entity.MovementType;
import com.hisobnoma.platform.inventory.repository.*;
import org.springframework.data.domain.PageRequest;
import com.hisobnoma.platform.pos.dto.AddPaymentRequest;
import com.hisobnoma.platform.pos.dto.CreateReturnRequest;
import com.hisobnoma.platform.pos.dto.CreateTransactionRequest;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.repository.POSTerminalRepository;
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
 * Cross-module integration test: Returns & Refunds
 * POS -> Inventory -> Finance: Return -> restock -> credit note
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReturnsAndRefundsFlowTest {

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
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Location location;
    private Product product1;
    private Product product2;
    private POSTerminal terminal;
    private Shift shift;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Returns Flow Tenant").code("INT_RETURN_FLOW").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("returnflowuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        setupSecurityContext();

        location = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-RET-001").name("Return Store")
                .locationType(LocationType.STORE)
                .active(true).allowNegativeStock(true)
                .tenantId(tenant.getId()).build());

        Category category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-RET").name("Return Category")
                .active(true).tenantId(tenant.getId()).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS-RT").name("Pieces").symbol("pcs")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).tenantId(tenant.getId()).build());

        product1 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-RET-001").name("Return Product A")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("40000"))
                .costPrice(new BigDecimal("25000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        product2 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-RET-002").name("Return Product B")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("60000"))
                .costPrice(new BigDecimal("35000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        setupChartOfAccounts();
        setupFiscalPeriods();
        setupInitialStock();

        terminal = terminalRepository.saveAndFlush(POSTerminal.builder()
                .terminalCode("T-RET-001").name("Return Terminal")
                .location(location).active(true)
                .tenantId(tenant.getId()).build());

        shift = shiftRepository.saveAndFlush(Shift.builder()
                .shiftNumber("SH-RET-001")
                .terminal(terminal)
                .cashierId(user.getId())
                .cashierName("returnflowuser")
                .status(ShiftStatus.OPEN)
                .openedAt(Instant.now())
                .openingCash(new BigDecimal("500000"))
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
    void fullReturn_restoresStockAndCreatesRefund() {
        var saleDto = completeSale(product1, new BigDecimal("5"), new BigDecimal("200000"));

        entityManager.flush();
        entityManager.clear();

        var stockAfterSale = getStock(product1);
        assertThat(stockAfterSale.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("95"));

        var returnDto = posTransactionService.createReturn(CreateReturnRequest.builder()
                .originalTransactionId(saleDto.getId())
                .returnReason("Defective product")
                .refundMethod(CreateReturnRequest.RefundMethod.CASH)
                .items(List.of(CreateReturnRequest.ReturnLineItem.builder()
                        .productId(product1.getId())
                        .quantity(new BigDecimal("5"))
                        .build()))
                .build());

        assertThat(returnDto.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(returnDto.getTransactionType()).isEqualTo(TransactionType.RETURN);

        entityManager.flush();
        entityManager.clear();

        var stockAfterReturn = getStock(product1);
        assertThat(stockAfterReturn.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    void partialReturn_restoresOnlyReturnedQuantity() {
        var saleDto = completeSale(product1, new BigDecimal("10"), new BigDecimal("400000"));

        entityManager.flush();
        entityManager.clear();

        var returnDto = posTransactionService.createReturn(CreateReturnRequest.builder()
                .originalTransactionId(saleDto.getId())
                .returnReason("Customer changed mind")
                .refundMethod(CreateReturnRequest.RefundMethod.CASH)
                .items(List.of(CreateReturnRequest.ReturnLineItem.builder()
                        .productId(product1.getId())
                        .quantity(new BigDecimal("3"))
                        .build()))
                .build());

        assertThat(returnDto.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        entityManager.flush();
        entityManager.clear();

        var stock = getStock(product1);
        assertThat(stock.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("93"));
    }

    @Test
    void multiProductReturn_restoresAllReturnedProducts() {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .items(List.of(
                        CreateTransactionRequest.LineItem.builder()
                                .productId(product1.getId())
                                .quantity(new BigDecimal("4"))
                                .build(),
                        CreateTransactionRequest.LineItem.builder()
                                .productId(product2.getId())
                                .quantity(new BigDecimal("3"))
                                .build()))
                .build());

        BigDecimal total = txDto.getTotalAmount();
        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(total).tenderedAmount(total).build());
        var completed = posTransactionService.completeTransaction(txDto.getId());

        entityManager.flush();
        entityManager.clear();

        var returnDto = posTransactionService.createReturn(CreateReturnRequest.builder()
                .originalTransactionId(completed.getId())
                .returnReason("Bulk return")
                .refundMethod(CreateReturnRequest.RefundMethod.CASH)
                .items(List.of(
                        CreateReturnRequest.ReturnLineItem.builder()
                                .productId(product1.getId())
                                .quantity(new BigDecimal("2"))
                                .build(),
                        CreateReturnRequest.ReturnLineItem.builder()
                                .productId(product2.getId())
                                .quantity(new BigDecimal("1"))
                                .build()))
                .build());

        assertThat(returnDto.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        entityManager.flush();
        entityManager.clear();

        var stock1 = getStock(product1);
        assertThat(stock1.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("98"));

        var stock2 = getStock(product2);
        assertThat(stock2.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("48"));
    }

    @Test
    void return_createsStockInMovement() {
        var saleDto = completeSale(product1, new BigDecimal("3"), new BigDecimal("120000"));

        entityManager.flush();
        entityManager.clear();

        posTransactionService.createReturn(CreateReturnRequest.builder()
                .originalTransactionId(saleDto.getId())
                .returnReason("Wrong item")
                .refundMethod(CreateReturnRequest.RefundMethod.CASH)
                .items(List.of(CreateReturnRequest.ReturnLineItem.builder()
                        .productId(product1.getId())
                        .quantity(new BigDecimal("3"))
                        .build()))
                .build());

        entityManager.flush();
        entityManager.clear();

        var movements = stockMovementRepository.findByProductIdAndTenantIdOrderByMovementDateDesc(
                product1.getId(), tenant.getId(), PageRequest.of(0, 100));
        boolean hasReturn = movements.getContent().stream()
                .anyMatch(m -> m.getMovementType() == MovementType.RETURN
                        || m.getMovementType() == MovementType.STOCK_IN);
        assertThat(hasReturn).isTrue();
    }

    private com.hisobnoma.platform.pos.dto.POSTransactionDto completeSale(Product prod, BigDecimal qty, BigDecimal amount) {
        var txDto = posTransactionService.createTransaction(CreateTransactionRequest.builder()
                .terminalId(terminal.getId())
                .items(List.of(CreateTransactionRequest.LineItem.builder()
                        .productId(prod.getId())
                        .quantity(qty)
                        .build()))
                .build());

        posPaymentService.addPayment(txDto.getId(), AddPaymentRequest.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(amount).tenderedAmount(amount).build());

        return posTransactionService.completeTransaction(txDto.getId());
    }

    private Stock getStock(Product prod) {
        return stockRepository.findByProductIdAndTenantId(prod.getId(), tenant.getId())
                .stream().filter(s -> s.getLocation().getId().equals(location.getId()))
                .findFirst().orElseThrow();
    }

    private void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("POS_SALE_CREATE"),
                    new SimpleGrantedAuthority("POS_SALE_READ"),
                    new SimpleGrantedAuthority("POS_SALE_VOID"),
                    new SimpleGrantedAuthority("POS_RETURN_CREATE"),
                    new SimpleGrantedAuthority("POS_PAYMENT_PROCESS"),
                    new SimpleGrantedAuthority("POS_REFUND_PROCESS"),
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
                .averageCost(new BigDecimal("25000"))
                .tenantId(tenant.getId()).build());

        stockRepository.saveAndFlush(Stock.builder()
                .product(product2).location(location)
                .quantityOnHand(new BigDecimal("50"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("35000"))
                .tenantId(tenant.getId()).build());
    }
}
