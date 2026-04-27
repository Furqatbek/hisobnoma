package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ARReportControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/finance/ar-reports";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ARInvoiceRepository arInvoiceRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User adminUser;
    private Customer customerA;
    private Customer customerB;

    /** Fixed "as of" date used by most aging tests. */
    private static final LocalDate AS_OF_DATE = LocalDate.of(2025, 6, 15);

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("AR Report Tenant").code("FULLFLOW_ARRPT").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("arrptadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        customerA = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-A01").name("Alpha Industries")
                .email("alpha@example.com").phone("+998901111111")
                .creditLimit(new BigDecimal("500000.0000"))
                .currentBalance(BigDecimal.ZERO)
                .totalInvoiced(BigDecimal.ZERO).totalReceived(BigDecimal.ZERO)
                .paymentTermsDays(30)
                .active(true).creditHold(false)
                .tenantId(tenant.getId()).build());

        customerB = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-B02").name("Bravo Trading")
                .email("bravo@example.com").phone("+998902222222")
                .creditLimit(new BigDecimal("200000.0000"))
                .currentBalance(BigDecimal.ZERO)
                .totalInvoiced(BigDecimal.ZERO).totalReceived(BigDecimal.ZERO)
                .paymentTermsDays(15)
                .active(true).creditHold(false)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // --------------- auth helpers ---------------

    private RequestPostProcessor arReadAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("FINANCE_AR_READ")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERMISSION")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // --------------- invoice helper ---------------

    private ARInvoice createInvoice(Customer customer, String invoiceNumber,
                                     ARInvoiceStatus status, LocalDate invoiceDate,
                                     LocalDate dueDate, BigDecimal totalAmount,
                                     BigDecimal balanceDue) {
        return arInvoiceRepository.saveAndFlush(ARInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .customer(customer)
                .customerName(customer.getName())
                .invoiceDate(invoiceDate)
                .dueDate(dueDate)
                .status(status)
                .subtotal(totalAmount)
                .totalAmount(totalAmount)
                .balanceDue(balanceDue)
                .paidAmount(totalAmount.subtract(balanceDue))
                .creditApplied(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId())
                .build());
    }

    // ==================== GET /aging ====================

    @Test
    void getAgingReport_noInvoices_returnsEmptyReport() throws Exception {
        mockMvc.perform(get(BASE_URL + "/aging")
                        .param("asOfDate", AS_OF_DATE.toString())
                        .with(arReadAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportDate").value(AS_OF_DATE.toString()))
                .andExpect(jsonPath("$.customerAgingList").isArray())
                .andExpect(jsonPath("$.customerAgingList.length()").value(0))
                .andExpect(jsonPath("$.grandTotal").value(0))
                .andExpect(jsonPath("$.customerCount").value(0));
    }

    @Test
    void getAgingReport_withInvoices_showsAgingBuckets() throws Exception {
        // Current (not yet due): dueDate AFTER asOfDate
        createInvoice(customerA, "INV-1001", ARInvoiceStatus.PENDING,
                AS_OF_DATE.minusDays(5), AS_OF_DATE.plusDays(10),
                new BigDecimal("10000.0000"), new BigDecimal("10000.0000"));

        // 1-30 days overdue: dueDate = asOfDate - 15 days  (15 days overdue)
        createInvoice(customerA, "INV-1002", ARInvoiceStatus.SENT,
                AS_OF_DATE.minusDays(45), AS_OF_DATE.minusDays(15),
                new BigDecimal("20000.0000"), new BigDecimal("20000.0000"));

        // 31-60 days overdue: dueDate = asOfDate - 45 days  (45 days overdue)
        createInvoice(customerA, "INV-1003", ARInvoiceStatus.OVERDUE,
                AS_OF_DATE.minusDays(75), AS_OF_DATE.minusDays(45),
                new BigDecimal("30000.0000"), new BigDecimal("30000.0000"));

        // 61-90 days overdue: dueDate = asOfDate - 75 days  (75 days overdue)
        createInvoice(customerB, "INV-1004", ARInvoiceStatus.PARTIAL,
                AS_OF_DATE.minusDays(105), AS_OF_DATE.minusDays(75),
                new BigDecimal("50000.0000"), new BigDecimal("40000.0000"));

        // Over 90 days: dueDate = asOfDate - 120 days  (120 days overdue)
        createInvoice(customerB, "INV-1005", ARInvoiceStatus.OVERDUE,
                AS_OF_DATE.minusDays(150), AS_OF_DATE.minusDays(120),
                new BigDecimal("15000.0000"), new BigDecimal("15000.0000"));

        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/aging")
                        .param("asOfDate", AS_OF_DATE.toString())
                        .with(arReadAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportDate").value(AS_OF_DATE.toString()))
                .andExpect(jsonPath("$.customerCount").value(2))
                // Summary totals
                .andExpect(jsonPath("$.totalCurrent").value(closeTo(10000.0, 0.01)))
                .andExpect(jsonPath("$.total1To30Days").value(closeTo(20000.0, 0.01)))
                .andExpect(jsonPath("$.total31To60Days").value(closeTo(30000.0, 0.01)))
                .andExpect(jsonPath("$.total61To90Days").value(closeTo(40000.0, 0.01)))
                .andExpect(jsonPath("$.totalOver90Days").value(closeTo(15000.0, 0.01)))
                .andExpect(jsonPath("$.grandTotal").value(closeTo(115000.0, 0.01)))
                // Customer A: totalBalance = 10000 + 20000 + 30000 = 60000
                .andExpect(jsonPath("$.customerAgingList[?(@.customerCode=='CUST-A01')].currentAmount",
                        hasItem(closeTo(10000.0, 0.01))))
                .andExpect(jsonPath("$.customerAgingList[?(@.customerCode=='CUST-A01')].days1To30",
                        hasItem(closeTo(20000.0, 0.01))))
                .andExpect(jsonPath("$.customerAgingList[?(@.customerCode=='CUST-A01')].days31To60",
                        hasItem(closeTo(30000.0, 0.01))))
                .andExpect(jsonPath("$.customerAgingList[?(@.customerCode=='CUST-A01')].totalBalance",
                        hasItem(closeTo(60000.0, 0.01))))
                // Customer B: totalBalance = 40000 + 15000 = 55000
                .andExpect(jsonPath("$.customerAgingList[?(@.customerCode=='CUST-B02')].days61To90",
                        hasItem(closeTo(40000.0, 0.01))))
                .andExpect(jsonPath("$.customerAgingList[?(@.customerCode=='CUST-B02')].over90Days",
                        hasItem(closeTo(15000.0, 0.01))))
                .andExpect(jsonPath("$.customerAgingList[?(@.customerCode=='CUST-B02')].totalBalance",
                        hasItem(closeTo(55000.0, 0.01))));
    }

    @Test
    void getAgingReport_defaultsToTodayWhenNoAsOfDate() throws Exception {
        // Simply verify the endpoint works without the asOfDate param
        mockMvc.perform(get(BASE_URL + "/aging")
                        .with(arReadAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.customerAgingList").isArray());
    }

    // ==================== GET /customer-balance ====================

    @Test
    void getCustomerBalanceReport_returnsReport() throws Exception {
        // Create an unpaid invoice so there is a non-zero balance
        createInvoice(customerA, "INV-2001", ARInvoiceStatus.PENDING,
                LocalDate.of(2025, 5, 1), LocalDate.of(2025, 6, 1),
                new BigDecimal("75000.0000"), new BigDecimal("75000.0000"));

        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/customer-balance")
                        .with(arReadAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportDate").isNotEmpty())
                .andExpect(jsonPath("$.customerBalances").isArray())
                .andExpect(jsonPath("$.customerCount").value(2))
                .andExpect(jsonPath("$.totalReceivables").value(closeTo(75000.0, 0.01)))
                // Customer A has outstanding balance
                .andExpect(jsonPath("$.customerBalances[?(@.customerCode=='CUST-A01')].outstandingInvoices",
                        hasItem(closeTo(75000.0, 0.01))))
                .andExpect(jsonPath("$.customerBalances[?(@.customerCode=='CUST-A01')].netBalance",
                        hasItem(closeTo(75000.0, 0.01))))
                // Customer B has zero balance
                .andExpect(jsonPath("$.customerBalances[?(@.customerCode=='CUST-B02')].outstandingInvoices",
                        hasItem(0)));
    }

    // ==================== GET /customer-balance/{customerId} ====================

    @Test
    void getCustomerBalance_returnsCustomerBalance() throws Exception {
        createInvoice(customerA, "INV-3001", ARInvoiceStatus.SENT,
                LocalDate.of(2025, 5, 10), LocalDate.of(2025, 6, 10),
                new BigDecimal("120000.0000"), new BigDecimal("120000.0000"));

        createInvoice(customerA, "INV-3002", ARInvoiceStatus.PARTIAL,
                LocalDate.of(2025, 5, 15), LocalDate.of(2025, 6, 15),
                new BigDecimal("80000.0000"), new BigDecimal("30000.0000"));

        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/customer-balance/" + customerA.getId())
                        .with(arReadAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerA.getId()))
                .andExpect(jsonPath("$.customerCode").value("CUST-A01"))
                .andExpect(jsonPath("$.customerName").value("Alpha Industries"))
                .andExpect(jsonPath("$.outstandingInvoices").value(closeTo(150000.0, 0.01)))
                .andExpect(jsonPath("$.netBalance").value(closeTo(150000.0, 0.01)))
                .andExpect(jsonPath("$.creditLimit").value(closeTo(500000.0, 0.01)))
                .andExpect(jsonPath("$.availableCreditLimit").value(closeTo(350000.0, 0.01)))
                .andExpect(jsonPath("$.overCreditLimit").value(false))
                .andExpect(jsonPath("$.paymentTerms").value(30));
    }

    @Test
    void getCustomerBalance_notFound_returnsError() throws Exception {
        // Non-existent customer ID results in RuntimeException -> 500
        mockMvc.perform(get(BASE_URL + "/customer-balance/999999")
                        .with(arReadAuth()))
                .andExpect(status().isInternalServerError());
    }

    // ==================== Permission checks ====================

    @Test
    void agingReport_noPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/aging")
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerBalanceReport_noPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/customer-balance")
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerBalance_noPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/customer-balance/" + customerA.getId())
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
