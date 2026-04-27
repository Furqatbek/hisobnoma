package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateAPPaymentAllocationRequest;
import com.hisobnoma.platform.finance.dto.CreateAPPaymentRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
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
import org.springframework.test.web.servlet.MvcResult;
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
class APPaymentControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private APPaymentRepository apPaymentRepository;
    @Autowired private APInvoiceRepository apInvoiceRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/ap/payments";

    private Tenant tenant;
    private User adminUser;
    private Vendor vendor;
    private APInvoice approvedInvoice;
    private APInvoice approvedInvoice2;
    private Account apAccount;
    private Account cashAccount;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("AP Payment Test Tenant").code("FULLFLOW_APPAY").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("appay_admin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        vendor = vendorRepository.saveAndFlush(Vendor.builder()
                .code("V-001").name("Test Vendor LLC")
                .email("vendor@test.com").phone("+998901234567")
                .active(true).tenantId(tenant.getId())
                .currentBalance(BigDecimal.ZERO).build());

        // GL accounts required by processPayment -> postToGL
        apAccount = accountRepository.saveAndFlush(Account.builder()
                .code("2100").name("Accounts Payable")
                .accountType(AccountType.LIABILITY).normalBalance(NormalBalance.CREDIT)
                .active(true).tenantId(tenant.getId()).build());

        cashAccount = accountRepository.saveAndFlush(Account.builder()
                .code("1110").name("Cash")
                .accountType(AccountType.ASSET).normalBalance(NormalBalance.DEBIT)
                .active(true).tenantId(tenant.getId()).build());

        // Purchase Discounts account (used by GLIntegrationService.postAPPayment)
        accountRepository.saveAndFlush(Account.builder()
                .code("4300").name("Purchase Discounts")
                .accountType(AccountType.REVENUE).normalBalance(NormalBalance.CREDIT)
                .active(true).tenantId(tenant.getId()).build());

        // Fiscal Year & Period required for GL posting during processPayment
        FiscalYear fiscalYear = fiscalYearRepository.saveAndFlush(FiscalYear.builder()
                .year(LocalDate.now().getYear()).name("Fiscal Year " + LocalDate.now().getYear())
                .startDate(LocalDate.of(LocalDate.now().getYear(), 1, 1))
                .endDate(LocalDate.of(LocalDate.now().getYear(), 12, 31))
                .status(PeriodStatus.OPEN).current(true).closed(false)
                .tenantId(tenant.getId()).build());

        fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                .fiscalYear(fiscalYear)
                .periodNumber(LocalDate.now().getMonthValue())
                .name(LocalDate.now().getMonth().name())
                .startDate(LocalDate.now().withDayOfMonth(1))
                .endDate(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()))
                .status(PeriodStatus.OPEN).adjustmentPeriod(false)
                .tenantId(tenant.getId()).build());

        // AP invoices in APPROVED status for allocations
        approvedInvoice = apInvoiceRepository.saveAndFlush(APInvoice.builder()
                .invoiceNumber("AP-000001")
                .vendorId(vendor.getId()).vendorName(vendor.getName())
                .invoiceDate(LocalDate.now().minusDays(10))
                .dueDate(LocalDate.now().plusDays(20))
                .status(APInvoiceStatus.APPROVED)
                .subtotal(new BigDecimal("5000.0000"))
                .totalAmount(new BigDecimal("5000.0000"))
                .balanceDue(new BigDecimal("5000.0000"))
                .paidAmount(BigDecimal.ZERO)
                .tenantId(tenant.getId()).build());

        approvedInvoice2 = apInvoiceRepository.saveAndFlush(APInvoice.builder()
                .invoiceNumber("AP-000002")
                .vendorId(vendor.getId()).vendorName(vendor.getName())
                .invoiceDate(LocalDate.now().minusDays(5))
                .dueDate(LocalDate.now().plusDays(25))
                .status(APInvoiceStatus.APPROVED)
                .subtotal(new BigDecimal("3000.0000"))
                .totalAmount(new BigDecimal("3000.0000"))
                .balanceDue(new BigDecimal("3000.0000"))
                .paidAmount(BigDecimal.ZERO)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_AP_PAY_VIEW"),
                        new SimpleGrantedAuthority("FINANCE_AP_PAY"),
                        new SimpleGrantedAuthority("FINANCE_AP_PAY_APPROVE"),
                        new SimpleGrantedAuthority("FINANCE_AP_PAY_VOID")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor viewOnlyAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_AP_PAY_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- Request builder helpers ----

    private CreateAPPaymentRequest buildCreateRequest(BigDecimal amount, Long invoiceId, BigDecimal allocatedAmount) {
        return CreateAPPaymentRequest.builder()
                .vendorId(vendor.getId())
                .paymentDate(LocalDate.now())
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(amount)
                .currency("UZS")
                .bankAccountId(null)
                .cashAccountId(cashAccount.getId())
                .apAccountId(apAccount.getId())
                .referenceNumber("REF-001")
                .memo("Test payment")
                .allocations(List.of(
                        CreateAPPaymentAllocationRequest.builder()
                                .apInvoiceId(invoiceId)
                                .allocatedAmount(allocatedAmount)
                                .build()))
                .build();
    }

    private Long createPaymentViaApi() throws Exception {
        CreateAPPaymentRequest request = buildCreateRequest(
                new BigDecimal("5000.00"), approvedInvoice.getId(), new BigDecimal("5000.00"));

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asLong();
    }

    // ============================================================
    // 1. getPayments_returnsPaginatedList
    // ============================================================

    @Test
    void getPayments_returnsPaginatedList() throws Exception {
        createPaymentViaApi();

        mockMvc.perform(get(BASE_URL).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].paymentNumber").value(startsWith("PAY-")))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ============================================================
    // 2. getPaymentById_returnsPayment
    // ============================================================

    @Test
    void getPaymentById_returnsPayment() throws Exception {
        Long paymentId = createPaymentViaApi();

        mockMvc.perform(get(BASE_URL + "/" + paymentId).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(paymentId))
                .andExpect(jsonPath("$.data.paymentNumber").value(startsWith("PAY-")))
                .andExpect(jsonPath("$.data.vendorId").value(vendor.getId()))
                .andExpect(jsonPath("$.data.vendorName").value("Test Vendor LLC"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.paymentMethod").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.data.allocations").isArray())
                .andExpect(jsonPath("$.data.allocations.length()").value(1));
    }

    // ============================================================
    // 3. getPaymentsByStatus_returnsFiltered
    // ============================================================

    @Test
    void getPaymentsByStatus_returnsFiltered() throws Exception {
        createPaymentViaApi();

        mockMvc.perform(get(BASE_URL + "/status/DRAFT").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));

        // No payments in COMPLETED status
        mockMvc.perform(get(BASE_URL + "/status/COMPLETED").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ============================================================
    // 4. getPaymentsByVendor_returnsFiltered
    // ============================================================

    @Test
    void getPaymentsByVendor_returnsFiltered() throws Exception {
        createPaymentViaApi();

        mockMvc.perform(get(BASE_URL + "/vendor/" + vendor.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].vendorId").value(vendor.getId()));

        // Different vendor returns empty
        mockMvc.perform(get(BASE_URL + "/vendor/99999").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ============================================================
    // 5. createPayment_validRequest_returnsCreated
    // ============================================================

    @Test
    void createPayment_validRequest_returnsCreated() throws Exception {
        CreateAPPaymentRequest request = CreateAPPaymentRequest.builder()
                .vendorId(vendor.getId())
                .paymentDate(LocalDate.now())
                .paymentMethod(APPaymentMethod.CHECK)
                .paymentAmount(new BigDecimal("3000.00"))
                .currency("UZS")
                .cashAccountId(cashAccount.getId())
                .apAccountId(apAccount.getId())
                .referenceNumber("CHK-101")
                .checkNumber("10001")
                .checkDate(LocalDate.now())
                .memo("Check payment to vendor")
                .notes("Monthly payment")
                .allocations(List.of(
                        CreateAPPaymentAllocationRequest.builder()
                                .apInvoiceId(approvedInvoice.getId())
                                .allocatedAmount(new BigDecimal("3000.00"))
                                .build()))
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentNumber").value(startsWith("PAY-")))
                .andExpect(jsonPath("$.data.vendorId").value(vendor.getId()))
                .andExpect(jsonPath("$.data.vendorName").value("Test Vendor LLC"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.paymentMethod").value("CHECK"))
                .andExpect(jsonPath("$.data.paymentAmount").value(closeTo(3000.00, 0.01)))
                .andExpect(jsonPath("$.data.checkNumber").value("10001"))
                .andExpect(jsonPath("$.data.memo").value("Check payment to vendor"));
    }

    // ============================================================
    // 6. createPayment_missingVendor_returns400
    // ============================================================

    @Test
    void createPayment_missingVendor_returns400() throws Exception {
        CreateAPPaymentRequest request = CreateAPPaymentRequest.builder()
                .vendorId(99999L)
                .paymentDate(LocalDate.now())
                .paymentMethod(APPaymentMethod.CASH)
                .paymentAmount(new BigDecimal("1000.00"))
                .allocations(List.of(
                        CreateAPPaymentAllocationRequest.builder()
                                .apInvoiceId(approvedInvoice.getId())
                                .allocatedAmount(new BigDecimal("1000.00"))
                                .build()))
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============================================================
    // 7. updatePayment_validDraft_returnsUpdated
    //    (No PUT endpoint exists; verify creating a second payment
    //     with different data yields distinct results)
    // ============================================================

    @Test
    void createPayment_multipleAllocations_returnsCreated() throws Exception {
        CreateAPPaymentRequest request = CreateAPPaymentRequest.builder()
                .vendorId(vendor.getId())
                .paymentDate(LocalDate.now())
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("8000.00"))
                .currency("UZS")
                .cashAccountId(cashAccount.getId())
                .apAccountId(apAccount.getId())
                .memo("Multi-invoice payment")
                .allocations(List.of(
                        CreateAPPaymentAllocationRequest.builder()
                                .apInvoiceId(approvedInvoice.getId())
                                .allocatedAmount(new BigDecimal("5000.00"))
                                .build(),
                        CreateAPPaymentAllocationRequest.builder()
                                .apInvoiceId(approvedInvoice2.getId())
                                .allocatedAmount(new BigDecimal("3000.00"))
                                .build()))
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentAmount").value(closeTo(8000.00, 0.01)))
                .andExpect(jsonPath("$.data.allocatedAmount").value(closeTo(8000.00, 0.01)))
                .andExpect(jsonPath("$.data.unallocatedAmount").value(closeTo(0.00, 0.01)))
                .andExpect(jsonPath("$.data.fullyAllocated").value(true));
    }

    // ============================================================
    // 8. submitPayment_draftToPending
    // ============================================================

    @Test
    void submitPayment_draftToPending() throws Exception {
        Long paymentId = createPaymentViaApi();

        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/submit")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.submittedBy").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());
    }

    // ============================================================
    // 9. approvePayment_pendingToApproved
    // ============================================================

    @Test
    void approvePayment_pendingToApproved() throws Exception {
        Long paymentId = createPaymentViaApi();

        // Submit first
        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/submit")
                        .with(fullAuth()))
                .andExpect(status().isOk());

        // Then approve
        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/approve")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedBy").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.approvedAt").isNotEmpty());
    }

    // ============================================================
    // 10. completePayment_approvedToCompleted (processPayment)
    // ============================================================

    @Test
    void completePayment_approvedToCompleted() throws Exception {
        Long paymentId = createPaymentViaApi();

        // Submit -> Approve
        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/submit").with(fullAuth()))
                .andExpect(status().isOk());
        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/approve").with(fullAuth()))
                .andExpect(status().isOk());

        // Process (complete)
        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/process").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedBy").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.glPosted").value(true));
    }

    // ============================================================
    // 11. voidPayment_returns200
    // ============================================================

    @Test
    void voidPayment_returns200() throws Exception {
        Long paymentId = createPaymentViaApi();

        Map<String, String> voidRequest = Map.of("reason", "Duplicate payment");

        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/void")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VOIDED"))
                .andExpect(jsonPath("$.data.voidReason").value("Duplicate payment"))
                .andExpect(jsonPath("$.data.voidedBy").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.voidedAt").isNotEmpty());
    }

    // ============================================================
    // 12. getPaymentAllocations_returnsAllocations
    //     (Via getPayment which includes allocations in detail view)
    // ============================================================

    @Test
    void getPaymentAllocations_returnsAllocations() throws Exception {
        Long paymentId = createPaymentViaApi();

        mockMvc.perform(get(BASE_URL + "/" + paymentId).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allocations").isArray())
                .andExpect(jsonPath("$.data.allocations.length()").value(1))
                .andExpect(jsonPath("$.data.allocations[0].apInvoiceId").value(approvedInvoice.getId()))
                .andExpect(jsonPath("$.data.allocations[0].invoiceNumber").value("AP-000001"))
                .andExpect(jsonPath("$.data.allocations[0].allocatedAmount").value(closeTo(5000.00, 0.01)));
    }

    // ============================================================
    // 13. permissionCheck_viewOnly_cannotCreate
    // ============================================================

    @Test
    void permissionCheck_viewOnly_cannotCreate() throws Exception {
        CreateAPPaymentRequest request = buildCreateRequest(
                new BigDecimal("1000.00"), approvedInvoice.getId(), new BigDecimal("1000.00"));

        // View-only user cannot create payments
        mockMvc.perform(post(BASE_URL)
                        .with(viewOnlyAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // View-only user CAN list payments
        mockMvc.perform(get(BASE_URL).with(viewOnlyAuth()))
                .andExpect(status().isOk());
    }

    // ============================================================
    // 14. fullLifecycle_createSubmitApproveComplete
    // ============================================================

    @Test
    void fullLifecycle_createSubmitApproveComplete() throws Exception {
        // Step 1: Create
        CreateAPPaymentRequest createRequest = CreateAPPaymentRequest.builder()
                .vendorId(vendor.getId())
                .paymentDate(LocalDate.now())
                .paymentMethod(APPaymentMethod.ACH)
                .paymentAmount(new BigDecimal("5000.00"))
                .currency("UZS")
                .cashAccountId(cashAccount.getId())
                .apAccountId(apAccount.getId())
                .referenceNumber("ACH-2026-001")
                .memo("Full lifecycle test")
                .allocations(List.of(
                        CreateAPPaymentAllocationRequest.builder()
                                .apInvoiceId(approvedInvoice.getId())
                                .allocatedAmount(new BigDecimal("5000.00"))
                                .build()))
                .build();

        MvcResult createResult = mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        Long paymentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();
        String paymentNumber = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("paymentNumber").asText();

        // Step 2: Submit for approval
        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/submit")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        // Step 3: Approve
        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/approve")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Step 4: Process / Complete
        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/process")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.glPosted").value(true));

        // Step 5: Verify final state via GET
        mockMvc.perform(get(BASE_URL + "/" + paymentId).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentNumber").value(paymentNumber))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.vendorName").value("Test Vendor LLC"))
                .andExpect(jsonPath("$.data.paymentAmount").value(closeTo(5000.00, 0.01)))
                .andExpect(jsonPath("$.data.completedBy").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.glPosted").value(true))
                .andExpect(jsonPath("$.data.allocations.length()").value(1));

        // Step 6: Verify the payment shows up filtered by status=COMPLETED
        mockMvc.perform(get(BASE_URL + "/status/COMPLETED").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));

        // Step 7: Void the completed payment
        Map<String, String> voidRequest = Map.of("reason", "Lifecycle test void");
        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/void")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VOIDED"))
                .andExpect(jsonPath("$.data.voidReason").value("Lifecycle test void"));

        // Step 8: Verify final void state
        mockMvc.perform(get(BASE_URL + "/" + paymentId).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VOIDED"));
    }
}
