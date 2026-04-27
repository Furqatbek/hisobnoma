package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.APInvoice;
import com.hisobnoma.platform.finance.entity.APInvoiceStatus;
import com.hisobnoma.platform.finance.repository.APInvoiceRepository;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
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
class APReportControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/ap/reports";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private APInvoiceRepository apInvoiceRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User adminUser;
    private Vendor vendorA;
    private Vendor vendorB;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("AP Report Tenant").code("FULLFLOW_APRPT").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("aprptadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        vendorA = vendorRepository.saveAndFlush(Vendor.builder()
                .code("VEND-A").name("Vendor Alpha")
                .contactPerson("Alice").email("alice@vendora.com").phone("+998901111111")
                .creditLimit(new BigDecimal("100000.0000"))
                .active(true)
                .tenantId(tenant.getId()).build());

        vendorB = vendorRepository.saveAndFlush(Vendor.builder()
                .code("VEND-B").name("Vendor Beta")
                .contactPerson("Bob").email("bob@vendorb.com").phone("+998902222222")
                .creditLimit(new BigDecimal("50000.0000"))
                .active(true)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor apViewAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_AP_VIEW"),
                        new SimpleGrantedAuthority("FINANCE_VENDOR_STATEMENT")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermissionAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- Data helpers ----

    /**
     * Creates an APPROVED AP invoice with the given balance due and due date.
     */
    private APInvoice createApprovedInvoice(Vendor vendor, String invoiceNumber,
                                            BigDecimal totalAmount, BigDecimal balanceDue,
                                            LocalDate invoiceDate, LocalDate dueDate) {
        return apInvoiceRepository.saveAndFlush(APInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .vendorId(vendor.getId())
                .vendorName(vendor.getName())
                .invoiceDate(invoiceDate)
                .dueDate(dueDate)
                .status(APInvoiceStatus.APPROVED)
                .subtotal(totalAmount)
                .totalAmount(totalAmount)
                .balanceDue(balanceDue)
                .paidAmount(totalAmount.subtract(balanceDue))
                .tenantId(tenant.getId())
                .build());
    }

    // ---- GET /aging ----

    @Test
    void getAgingReport_noInvoices_returnsEmptyReport() throws Exception {
        mockMvc.perform(get(BASE_URL + "/aging").with(apViewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportDate").exists())
                .andExpect(jsonPath("$.data.tenantId").value(tenant.getId()))
                .andExpect(jsonPath("$.data.totalPayable").value(comparesEqualTo(0)))
                .andExpect(jsonPath("$.data.current").value(comparesEqualTo(0)))
                .andExpect(jsonPath("$.data.overdue1To30").value(comparesEqualTo(0)))
                .andExpect(jsonPath("$.data.overdue31To60").value(comparesEqualTo(0)))
                .andExpect(jsonPath("$.data.overdue61To90").value(comparesEqualTo(0)))
                .andExpect(jsonPath("$.data.overdueOver90").value(comparesEqualTo(0)))
                .andExpect(jsonPath("$.data.vendorAging").isArray())
                .andExpect(jsonPath("$.data.vendorAging").isEmpty());
    }

    @Test
    void getAgingReport_withInvoices_showsAgingBuckets() throws Exception {
        LocalDate today = LocalDate.now();

        // Current (not yet due) - due in 10 days
        createApprovedInvoice(vendorA, "AP-0001",
                new BigDecimal("1000.0000"), new BigDecimal("1000.0000"),
                today.minusDays(5), today.plusDays(10));

        // 1-30 days overdue - due 15 days ago
        createApprovedInvoice(vendorA, "AP-0002",
                new BigDecimal("2000.0000"), new BigDecimal("2000.0000"),
                today.minusDays(45), today.minusDays(15));

        // 31-60 days overdue - due 45 days ago
        createApprovedInvoice(vendorB, "AP-0003",
                new BigDecimal("3000.0000"), new BigDecimal("3000.0000"),
                today.minusDays(75), today.minusDays(45));

        // 61-90 days overdue - due 75 days ago
        createApprovedInvoice(vendorA, "AP-0004",
                new BigDecimal("4000.0000"), new BigDecimal("4000.0000"),
                today.minusDays(105), today.minusDays(75));

        // >90 days overdue - due 120 days ago
        createApprovedInvoice(vendorB, "AP-0005",
                new BigDecimal("5000.0000"), new BigDecimal("5000.0000"),
                today.minusDays(150), today.minusDays(120));

        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/aging").with(apViewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPayable").value(closeTo(15000.0, 0.01)))
                .andExpect(jsonPath("$.data.current").value(closeTo(1000.0, 0.01)))
                .andExpect(jsonPath("$.data.overdue1To30").value(closeTo(2000.0, 0.01)))
                .andExpect(jsonPath("$.data.overdue31To60").value(closeTo(3000.0, 0.01)))
                .andExpect(jsonPath("$.data.overdue61To90").value(closeTo(4000.0, 0.01)))
                .andExpect(jsonPath("$.data.overdueOver90").value(closeTo(5000.0, 0.01)))
                // Two vendors in the aging breakdown
                .andExpect(jsonPath("$.data.vendorAging.length()").value(2))
                // Vendor A: current=1000 + 1-30=2000 + 61-90=4000 = 7000
                .andExpect(jsonPath("$.data.vendorAging[?(@.vendorName=='Vendor Alpha')].totalBalance",
                        hasItem(closeTo(7000.0, 0.01))))
                .andExpect(jsonPath("$.data.vendorAging[?(@.vendorName=='Vendor Alpha')].invoiceCount",
                        hasItem(3)))
                // Vendor B: 31-60=3000 + >90=5000 = 8000
                .andExpect(jsonPath("$.data.vendorAging[?(@.vendorName=='Vendor Beta')].totalBalance",
                        hasItem(closeTo(8000.0, 0.01))))
                .andExpect(jsonPath("$.data.vendorAging[?(@.vendorName=='Vendor Beta')].invoiceCount",
                        hasItem(2)));
    }

    // ---- GET /vendor-balance ----

    @Test
    void getVendorBalanceReport_returnsVendorBalances() throws Exception {
        LocalDate today = LocalDate.now();

        // Vendor A has two open invoices
        createApprovedInvoice(vendorA, "AP-0010",
                new BigDecimal("5000.0000"), new BigDecimal("5000.0000"),
                today.minusDays(10), today.plusDays(20));
        createApprovedInvoice(vendorA, "AP-0011",
                new BigDecimal("3000.0000"), new BigDecimal("3000.0000"),
                today.minusDays(5), today.plusDays(25));

        // Vendor B has one open invoice
        createApprovedInvoice(vendorB, "AP-0012",
                new BigDecimal("2000.0000"), new BigDecimal("2000.0000"),
                today.minusDays(3), today.plusDays(27));

        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/vendor-balance").with(apViewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportDate").exists())
                .andExpect(jsonPath("$.data.tenantId").value(tenant.getId()))
                .andExpect(jsonPath("$.data.totalPayable").value(closeTo(10000.0, 0.01)))
                .andExpect(jsonPath("$.data.vendorCount").value(2))
                .andExpect(jsonPath("$.data.vendorBalances").isArray())
                .andExpect(jsonPath("$.data.vendorBalances.length()").value(2))
                // Vendor A has higher balance so sorted first
                .andExpect(jsonPath("$.data.vendorBalances[0].vendorName").value("Vendor Alpha"))
                .andExpect(jsonPath("$.data.vendorBalances[0].currentBalance").value(closeTo(8000.0, 0.01)))
                .andExpect(jsonPath("$.data.vendorBalances[0].openInvoiceCount").value(2))
                .andExpect(jsonPath("$.data.vendorBalances[0].creditLimit").value(closeTo(100000.0, 0.01)))
                .andExpect(jsonPath("$.data.vendorBalances[0].availableCredit").value(closeTo(92000.0, 0.01)))
                // Vendor B
                .andExpect(jsonPath("$.data.vendorBalances[1].vendorName").value("Vendor Beta"))
                .andExpect(jsonPath("$.data.vendorBalances[1].currentBalance").value(closeTo(2000.0, 0.01)))
                .andExpect(jsonPath("$.data.vendorBalances[1].openInvoiceCount").value(1));
    }

    // ---- GET /vendor/{vendorId}/statement ----

    @Test
    void getVendorStatement_returnsVendorBalance() throws Exception {
        LocalDate today = LocalDate.now();

        createApprovedInvoice(vendorA, "AP-0020",
                new BigDecimal("7500.0000"), new BigDecimal("7500.0000"),
                today.minusDays(10), today.plusDays(20));
        createApprovedInvoice(vendorA, "AP-0021",
                new BigDecimal("2500.0000"), new BigDecimal("2500.0000"),
                today.minusDays(5), today.plusDays(25));

        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/vendor/" + vendorA.getId() + "/statement")
                        .with(apViewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vendorId").value(vendorA.getId()))
                .andExpect(jsonPath("$.data.vendorCode").value("VEND-A"))
                .andExpect(jsonPath("$.data.vendorName").value("Vendor Alpha"))
                .andExpect(jsonPath("$.data.contactPerson").value("Alice"))
                .andExpect(jsonPath("$.data.email").value("alice@vendora.com"))
                .andExpect(jsonPath("$.data.currentBalance").value(closeTo(10000.0, 0.01)))
                .andExpect(jsonPath("$.data.openInvoiceCount").value(2))
                .andExpect(jsonPath("$.data.creditLimit").value(closeTo(100000.0, 0.01)))
                .andExpect(jsonPath("$.data.availableCredit").value(closeTo(90000.0, 0.01)))
                .andExpect(jsonPath("$.data.lastInvoiceDate").exists());
    }

    @Test
    void getVendorStatement_notFound_returns500() throws Exception {
        // The service throws RuntimeException("Vendor not found") which the
        // GlobalExceptionHandler maps to 500 Internal Server Error
        mockMvc.perform(get(BASE_URL + "/vendor/99999/statement")
                        .with(apViewAuth()))
                .andExpect(status().isInternalServerError());
    }

    // ---- Permission checks ----

    @Test
    void agingReport_withoutPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/aging").with(noPermissionAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendorBalanceReport_withoutPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/vendor-balance").with(noPermissionAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendorStatement_withoutPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/vendor/" + vendorA.getId() + "/statement")
                        .with(noPermissionAuth()))
                .andExpect(status().isForbidden());
    }
}
