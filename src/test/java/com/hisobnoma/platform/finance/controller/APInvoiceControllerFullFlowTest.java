package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceLineRequest;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.APInvoiceRepository;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.FiscalPeriodRepository;
import com.hisobnoma.platform.finance.repository.FiscalYearRepository;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class APInvoiceControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private APInvoiceRepository apInvoiceRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/ap/invoices";

    private Tenant tenant;
    private User adminUser;
    private Vendor vendor;
    private Account apAccount;
    private Account expenseAccount;
    private FiscalYear fiscalYear;
    private FiscalPeriod fiscalPeriod;
    private APInvoice draftInvoice;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("AP Invoice Tenant").code("FULLFLOW_AP").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("apinvoiceadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        // GL accounts needed for AP posting (codes match GLIntegrationService constants)
        apAccount = accountRepository.saveAndFlush(Account.builder()
                .code("2110").name("Accounts Payable").accountType(AccountType.LIABILITY)
                .normalBalance(NormalBalance.CREDIT).accountLevel(1).fullPath("2110")
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());

        expenseAccount = accountRepository.saveAndFlush(Account.builder()
                .code("5200").name("Purchases").accountType(AccountType.EXPENSE)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("5200")
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());

        // Also create the default AP control account referenced by the service (code "2100")
        accountRepository.saveAndFlush(Account.builder()
                .code("2100").name("AP Control").accountType(AccountType.LIABILITY)
                .normalBalance(NormalBalance.CREDIT).accountLevel(1).fullPath("2100")
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());

        // Vendor
        vendor = vendorRepository.saveAndFlush(Vendor.builder()
                .code("V-001").name("Test Vendor LLC")
                .paymentTerms("Net 30").paymentTermsDays(30)
                .defaultExpenseAccountId(expenseAccount.getId())
                .active(true)
                .tenantId(tenant.getId()).build());

        // Fiscal year and period covering the invoice dates used in tests
        LocalDate today = LocalDate.now();
        fiscalYear = fiscalYearRepository.saveAndFlush(FiscalYear.builder()
                .year(today.getYear()).name("Fiscal Year " + today.getYear())
                .startDate(LocalDate.of(today.getYear(), 1, 1))
                .endDate(LocalDate.of(today.getYear(), 12, 31))
                .status(PeriodStatus.OPEN).current(true).closed(false)
                .tenantId(tenant.getId()).build());

        fiscalPeriod = fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                .fiscalYear(fiscalYear).periodNumber(today.getMonthValue())
                .name(today.getMonth().name())
                .startDate(today.withDayOfMonth(1))
                .endDate(today.withDayOfMonth(today.lengthOfMonth()))
                .status(PeriodStatus.OPEN).adjustmentPeriod(false)
                .tenantId(tenant.getId()).build());

        // Pre-existing DRAFT invoice for read/update/workflow tests
        draftInvoice = APInvoice.builder()
                .tenantId(tenant.getId())
                .invoiceNumber("AP-000001")
                .vendorId(vendor.getId())
                .vendorName(vendor.getName())
                .invoiceDate(today)
                .dueDate(today.plusDays(30))
                .status(APInvoiceStatus.DRAFT)
                .expenseAccountId(expenseAccount.getId())
                .apAccountId(apAccount.getId())
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .paymentTerms("Net 30")
                .paymentTermsDays(30)
                .description("Test AP Invoice")
                .build();
        draftInvoice = apInvoiceRepository.saveAndFlush(draftInvoice);

        // Add a line to the draft invoice (no tax so GL posting stays balanced)
        APInvoiceLine line = APInvoiceLine.builder()
                .lineNumber(1)
                .description("Office Supplies")
                .quantity(new BigDecimal("10.0000"))
                .unitPrice(new BigDecimal("50.0000"))
                .lineTotal(new BigDecimal("500.0000"))
                .expenseAccountId(expenseAccount.getId())
                .build();
        draftInvoice.addLine(line);
        draftInvoice.recalculateTotals();
        draftInvoice = apInvoiceRepository.saveAndFlush(draftInvoice);

        entityManager.clear();
    }

    // ========================= Auth helpers =========================

    private RequestPostProcessor allPermsAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_AP_VIEW"),
                        new SimpleGrantedAuthority("FINANCE_AP_CREATE"),
                        new SimpleGrantedAuthority("FINANCE_AP_UPDATE"),
                        new SimpleGrantedAuthority("FINANCE_AP_APPROVE"),
                        new SimpleGrantedAuthority("FINANCE_AP_CANCEL")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor viewOnlyAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_AP_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ========================= Helper: build request =========================

    private CreateAPInvoiceRequest buildValidRequest() {
        LocalDate today = LocalDate.now();
        return CreateAPInvoiceRequest.builder()
                .vendorId(vendor.getId())
                .invoiceDate(today)
                .dueDate(today.plusDays(30))
                .description("New AP Invoice")
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .expenseAccountId(expenseAccount.getId())
                .apAccountId(apAccount.getId())
                .lines(List.of(
                        CreateAPInvoiceLineRequest.builder()
                                .description("Widget A")
                                .quantity(new BigDecimal("5"))
                                .unitPrice(new BigDecimal("100.00"))
                                .build(),
                        CreateAPInvoiceLineRequest.builder()
                                .description("Widget B")
                                .quantity(new BigDecimal("2"))
                                .unitPrice(new BigDecimal("250.00"))
                                .build()
                ))
                .build();
    }

    // ========================= 1. GET /api/v1/ap/invoices =========================

    @Test
    void getInvoices_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].invoiceNumber").exists());
    }

    // ========================= 2. GET /api/v1/ap/invoices/{id} =========================

    @Test
    void getInvoiceById_returnsInvoice() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + draftInvoice.getId()).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(draftInvoice.getId()))
                .andExpect(jsonPath("$.data.invoiceNumber").value("AP-000001"))
                .andExpect(jsonPath("$.data.vendorName").value("Test Vendor LLC"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.lines").isArray())
                .andExpect(jsonPath("$.data.lines.length()").value(1))
                .andExpect(jsonPath("$.data.lines[0].description").value("Office Supplies"));
    }

    // ========================= 3. GET /api/v1/ap/invoices/status/{status} =========================

    @Test
    void getInvoicesByStatus_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/status/DRAFT").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    // ========================= 4. GET /api/v1/ap/invoices/vendor/{vendorId} =========================

    @Test
    void getInvoicesByVendor_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/vendor/" + vendor.getId()).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].vendorId").value(vendor.getId()));
    }

    // ========================= 5. POST - create valid invoice =========================

    @Test
    void createInvoice_validRequest_returnsCreated() throws Exception {
        CreateAPInvoiceRequest request = buildValidRequest();

        mockMvc.perform(post(BASE_URL)
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invoiceNumber").value(startsWith("AP-")))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.vendorName").value("Test Vendor LLC"))
                .andExpect(jsonPath("$.data.lines").isArray())
                .andExpect(jsonPath("$.data.lines.length()").value(2))
                .andExpect(jsonPath("$.data.totalAmount").isNumber());
    }

    // ========================= 6. POST - missing lines => 400 =========================

    @Test
    void createInvoice_missingLines_returns400() throws Exception {
        CreateAPInvoiceRequest request = CreateAPInvoiceRequest.builder()
                .vendorId(vendor.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .lines(List.of()) // empty lines -> @NotEmpty violation
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========================= 7. POST - missing vendor still works (vendor-less) but missing dates => 400 =========================

    @Test
    void createInvoice_missingRequiredDates_returns400() throws Exception {
        // invoiceDate and dueDate are @NotNull
        CreateAPInvoiceRequest request = CreateAPInvoiceRequest.builder()
                .vendorId(vendor.getId())
                // invoiceDate omitted
                // dueDate omitted
                .lines(List.of(CreateAPInvoiceLineRequest.builder()
                        .description("Item")
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("100"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========================= 8. PUT - update draft invoice =========================

    @Test
    void updateInvoice_validDraft_returnsUpdated() throws Exception {
        CreateAPInvoiceRequest request = CreateAPInvoiceRequest.builder()
                .vendorId(vendor.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(45))
                .description("Updated Description")
                .expenseAccountId(expenseAccount.getId())
                .apAccountId(apAccount.getId())
                .lines(List.of(
                        CreateAPInvoiceLineRequest.builder()
                                .description("Updated Line Item")
                                .quantity(new BigDecimal("20"))
                                .unitPrice(new BigDecimal("75.00"))
                                .build()))
                .build();

        mockMvc.perform(put(BASE_URL + "/" + draftInvoice.getId())
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Updated Description"))
                .andExpect(jsonPath("$.data.lines.length()").value(1))
                .andExpect(jsonPath("$.data.lines[0].description").value("Updated Line Item"));
    }

    // ========================= 9. POST /{id}/submit - DRAFT -> PENDING_APPROVAL =========================

    @Test
    void submitInvoice_draftToApproval() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/submit")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.submittedBy").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());
    }

    // ========================= 10. POST /{id}/approve - PENDING_APPROVAL -> APPROVED =========================

    @Test
    void approveInvoice_pendingToApproved() throws Exception {
        // First submit the invoice
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/submit")
                        .with(allPermsAuth()))
                .andExpect(status().isOk());

        // Then approve
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/approve")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedBy").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.approvedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.glPosted").value(true));
    }

    // ========================= 11. POST /{id}/reject - PENDING_APPROVAL -> DRAFT =========================

    @Test
    void rejectInvoice_pendingToDraft() throws Exception {
        // Submit first
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/submit")
                        .with(allPermsAuth()))
                .andExpect(status().isOk());

        // Reject with reason
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/reject")
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Incorrect amounts"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Incorrect amounts"))
                .andExpect(jsonPath("$.data.rejectedBy").value(adminUser.getId()));
    }

    // ========================= 12. POST /{id}/cancel =========================

    @Test
    void cancelInvoice_returns200() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/cancel")
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Duplicate invoice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Duplicate invoice"))
                .andExpect(jsonPath("$.data.cancelledBy").value(adminUser.getId()));
    }

    // ========================= 13. POST /{id}/hold - sets ON_HOLD =========================

    @Test
    void holdInvoice_setsOnHold() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/hold")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_HOLD"));
    }

    // ========================= 14. POST /{id}/release-hold =========================

    @Test
    void releaseInvoice_releasesHold() throws Exception {
        // Put on hold first
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/hold")
                        .with(allPermsAuth()))
                .andExpect(status().isOk());

        // Release hold — no approvedAt so should go back to DRAFT
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/release-hold")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    // ========================= 15. Permission check: view-only cannot create =========================

    @Test
    void permissionCheck_viewOnly_cannotCreate() throws Exception {
        CreateAPInvoiceRequest request = buildValidRequest();

        mockMvc.perform(post(BASE_URL)
                        .with(viewOnlyAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ========================= 16. Full lifecycle: create -> submit -> approve =========================

    @Test
    void fullLifecycle_createSubmitApprove() throws Exception {
        // 1. Create
        CreateAPInvoiceRequest request = buildValidRequest();

        String createResult = mockMvc.perform(post(BASE_URL)
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        Long invoiceId = objectMapper.readTree(createResult).get("data").get("id").asLong();

        // 2. Verify it appears in the list
        mockMvc.perform(get(BASE_URL).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)));

        // 3. Submit for approval
        mockMvc.perform(post(BASE_URL + "/" + invoiceId + "/submit")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        // 4. Verify status filter
        mockMvc.perform(get(BASE_URL + "/status/PENDING_APPROVAL").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));

        // 5. Approve
        mockMvc.perform(post(BASE_URL + "/" + invoiceId + "/approve")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.glPosted").value(true));

        // 6. Verify the approved invoice via GET by id
        mockMvc.perform(get(BASE_URL + "/" + invoiceId).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedBy").value(adminUser.getId()));
    }
}
