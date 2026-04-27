package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceLineRequest;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ARInvoiceControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ARInvoiceRepository arInvoiceRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/finance/ar-invoices";

    private Tenant tenant;
    private User adminUser;
    private Customer customer;
    private Customer customer2;
    private ARInvoice draftInvoice;
    private ARInvoice sentInvoice;
    private ARInvoice overdueInvoice;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("AR Invoice Tenant").code("FULLFLOW_ARINV").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("arinvoiceadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        customer = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-000001").name("Acme Corp")
                .email("acme@example.com").phone("+998901234567")
                .address("123 Main St").city("Tashkent").country("Uzbekistan")
                .paymentTerms("Net 30").paymentTermsDays(30)
                .creditLimit(new BigDecimal("100000.0000"))
                .currentBalance(BigDecimal.ZERO)
                .totalInvoiced(BigDecimal.ZERO).totalReceived(BigDecimal.ZERO)
                .active(true).creditHold(false).customerType("WHOLESALE")
                .tenantId(tenant.getId()).build());

        customer2 = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-000002").name("Beta LLC")
                .email("beta@example.com").phone("+998907654321")
                .paymentTerms("Net 15").paymentTermsDays(15)
                .creditLimit(new BigDecimal("50000.0000"))
                .currentBalance(BigDecimal.ZERO)
                .totalInvoiced(BigDecimal.ZERO).totalReceived(BigDecimal.ZERO)
                .active(true).creditHold(false).customerType("RETAIL")
                .tenantId(tenant.getId()).build());

        // GL accounts needed by GLIntegrationService for postInvoice flow
        accountRepository.saveAndFlush(Account.builder()
                .code("1130").name("Accounts Receivable")
                .accountType(AccountType.ASSET).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true).systemAccount(false)
                .tenantId(tenant.getId()).build());

        accountRepository.saveAndFlush(Account.builder()
                .code("4100").name("Sales Revenue")
                .accountType(AccountType.REVENUE).normalBalance(NormalBalance.CREDIT)
                .active(true).allowsDirectPosting(true).systemAccount(false)
                .tenantId(tenant.getId()).build());

        accountRepository.saveAndFlush(Account.builder()
                .code("5100").name("Cost of Goods Sold")
                .accountType(AccountType.EXPENSE).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true).systemAccount(false)
                .tenantId(tenant.getId()).build());

        accountRepository.saveAndFlush(Account.builder()
                .code("1140").name("Inventory")
                .accountType(AccountType.ASSET).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true).systemAccount(false)
                .tenantId(tenant.getId()).build());

        // Fiscal year and period (needed for GL posting)
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

        // Pre-existing invoices for read-oriented tests
        draftInvoice = arInvoiceRepository.saveAndFlush(ARInvoice.builder()
                .invoiceNumber("INV-000001")
                .customer(customer).customerName(customer.getName())
                .invoiceDate(LocalDate.now().minusDays(5))
                .dueDate(LocalDate.now().plusDays(25))
                .status(ARInvoiceStatus.DRAFT)
                .subtotal(new BigDecimal("5000.0000"))
                .totalAmount(new BigDecimal("5000.0000"))
                .balanceDue(new BigDecimal("5000.0000"))
                .paidAmount(BigDecimal.ZERO)
                .creditApplied(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .currency("UZS").exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId()).build());

        sentInvoice = arInvoiceRepository.saveAndFlush(ARInvoice.builder()
                .invoiceNumber("INV-000002")
                .customer(customer).customerName(customer.getName())
                .invoiceDate(LocalDate.now().minusDays(20))
                .dueDate(LocalDate.now().plusDays(10))
                .status(ARInvoiceStatus.SENT)
                .subtotal(new BigDecimal("3000.0000"))
                .totalAmount(new BigDecimal("3000.0000"))
                .balanceDue(new BigDecimal("3000.0000"))
                .paidAmount(BigDecimal.ZERO)
                .creditApplied(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .currency("UZS").exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId()).build());

        overdueInvoice = arInvoiceRepository.saveAndFlush(ARInvoice.builder()
                .invoiceNumber("INV-000003")
                .customer(customer2).customerName(customer2.getName())
                .invoiceDate(LocalDate.now().minusDays(60))
                .dueDate(LocalDate.now().minusDays(30))
                .status(ARInvoiceStatus.SENT)
                .subtotal(new BigDecimal("2000.0000"))
                .totalAmount(new BigDecimal("2000.0000"))
                .balanceDue(new BigDecimal("2000.0000"))
                .paidAmount(BigDecimal.ZERO)
                .creditApplied(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .currency("UZS").exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_AR_READ"),
                        new SimpleGrantedAuthority("FINANCE_AR_WRITE"),
                        new SimpleGrantedAuthority("FINANCE_AR_APPROVE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor readOnlyAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_AR_READ")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- Request builder helpers ----

    private CreateARInvoiceRequest buildValidCreateRequest(Long customerId) {
        return CreateARInvoiceRequest.builder()
                .customerId(customerId)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .totalAmount(new BigDecimal("1500.00"))
                .currency("UZS")
                .description("Test invoice")
                .notes("Integration test notes")
                .lines(List.of(
                        CreateARInvoiceLineRequest.builder()
                                .description("Widget A")
                                .quantity(new BigDecimal("10"))
                                .unitPrice(new BigDecimal("100.00"))
                                .unitCost(new BigDecimal("60.00"))
                                .build(),
                        CreateARInvoiceLineRequest.builder()
                                .description("Widget B")
                                .quantity(new BigDecimal("5"))
                                .unitPrice(new BigDecimal("100.00"))
                                .build()))
                .build();
    }

    // ======== 1. GET /api/v1/finance/ar-invoices ========

    @Test
    void getInvoices_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(fullAuth())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.content[0].invoiceNumber").isNotEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(3)));
    }

    // ======== 2. GET /api/v1/finance/ar-invoices/{id} ========

    @Test
    void getInvoiceById_returnsInvoice() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + draftInvoice.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draftInvoice.getId()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-000001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.customerName").value("Acme Corp"))
                .andExpect(jsonPath("$.totalAmount").value(closeTo(5000.0, 0.01)));
    }

    // ======== 3. GET /api/v1/finance/ar-invoices/status/{status} ========

    @Test
    void getInvoicesByStatus_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/status/DRAFT")
                        .with(fullAuth())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-000001"))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    // ======== 4. GET /api/v1/finance/ar-invoices/customer/{customerId} ========

    @Test
    void getInvoicesByCustomer_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/customer/" + customer.getId())
                        .with(fullAuth())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].customerName",
                        everyItem(equalTo("Acme Corp"))));
    }

    // ======== 5. POST /api/v1/finance/ar-invoices ========

    @Test
    void createInvoice_validRequest_returnsCreated() throws Exception {
        CreateARInvoiceRequest request = buildValidCreateRequest(customer.getId());

        mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value(startsWith("INV-")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    // ======== 6. POST /api/v1/finance/ar-invoices (missing lines -> 400) ========

    @Test
    void createInvoice_missingLines_returns400() throws Exception {
        CreateARInvoiceRequest request = CreateARInvoiceRequest.builder()
                .customerId(customer.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .totalAmount(new BigDecimal("1000.00"))
                .lines(null)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ======== 7. PUT /api/v1/finance/ar-invoices/{id} ========

    @Test
    void updateInvoice_validDraft_returnsUpdated() throws Exception {
        CreateARInvoiceRequest updateRequest = CreateARInvoiceRequest.builder()
                .customerId(customer.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(45))
                .totalAmount(new BigDecimal("6000.00"))
                .description("Updated invoice description")
                .lines(List.of(
                        CreateARInvoiceLineRequest.builder()
                                .description("Updated Widget")
                                .quantity(new BigDecimal("20"))
                                .unitPrice(new BigDecimal("300.00"))
                                .build()))
                .build();

        mockMvc.perform(put(BASE_URL + "/" + draftInvoice.getId())
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draftInvoice.getId()))
                .andExpect(jsonPath("$.description").value("Updated invoice description"))
                .andExpect(jsonPath("$.lines.length()").value(1));
    }

    // ======== 8. POST /api/v1/finance/ar-invoices/{id}/send (PENDING -> SENT) ========

    @Test
    void sendInvoice_pendingToSent() throws Exception {
        // First create an invoice
        CreateARInvoiceRequest request = buildValidCreateRequest(customer.getId());
        String createResult = mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long invoiceId = objectMapper.readTree(createResult).get("id").asLong();

        // Post the invoice (DRAFT -> PENDING) via the approve endpoint
        mockMvc.perform(post(BASE_URL + "/" + invoiceId + "/post")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Send the invoice (PENDING -> SENT)
        mockMvc.perform(post(BASE_URL + "/" + invoiceId + "/send")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    // ======== 9. POST /api/v1/finance/ar-invoices/{id}/cancel ========

    @Test
    void cancelInvoice_returns200() throws Exception {
        // Cancel the draft invoice (no payments, so it should work)
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/cancel")
                        .with(fullAuth())
                        .param("reason", "Customer requested cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.notes").value(containsString("Customer requested cancellation")));
    }

    // ======== 10. Dispute test (no explicit dispute endpoint - verify CANCELLED/status behavior) ========

    @Test
    void cancelInvoice_alreadyCancelled_returns400() throws Exception {
        // Cancel the invoice first
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/cancel")
                        .with(fullAuth())
                        .param("reason", "First cancellation"))
                .andExpect(status().isOk());

        // Try to cancel again - should fail with business exception
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/cancel")
                        .with(fullAuth())
                        .param("reason", "Second cancellation"))
                .andExpect(status().isBadRequest());
    }

    // ======== 11. GET /api/v1/finance/ar-invoices/overdue ========

    @Test
    void getOverdueInvoices_returnsOverdue() throws Exception {
        // The overdueInvoice has dueDate in the past and status SENT with balance > 0
        mockMvc.perform(get(BASE_URL + "/overdue")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-000003"));
    }

    // ======== 12. Permission check: read-only user cannot write ========

    @Test
    void permissionCheck_readOnly_cannotWrite() throws Exception {
        // Read should work
        mockMvc.perform(get(BASE_URL)
                        .with(readOnlyAuth()))
                .andExpect(status().isOk());

        // Create should be forbidden
        CreateARInvoiceRequest request = buildValidCreateRequest(customer.getId());
        mockMvc.perform(post(BASE_URL)
                        .with(readOnlyAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Update should be forbidden
        mockMvc.perform(put(BASE_URL + "/" + draftInvoice.getId())
                        .with(readOnlyAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Cancel should be forbidden (requires FINANCE_AR_APPROVE)
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/cancel")
                        .with(readOnlyAuth())
                        .param("reason", "unauthorized"))
                .andExpect(status().isForbidden());

        // Post should be forbidden (requires FINANCE_AR_APPROVE)
        mockMvc.perform(post(BASE_URL + "/" + draftInvoice.getId() + "/post")
                        .with(readOnlyAuth()))
                .andExpect(status().isForbidden());
    }

    // ======== 13. Full lifecycle: create -> post -> send -> pay ========

    @Test
    void fullLifecycle_createSendPay() throws Exception {
        // Step 1: Create invoice
        CreateARInvoiceRequest request = buildValidCreateRequest(customer2.getId());

        String createResult = mockMvc.perform(post(BASE_URL)
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.invoiceNumber").value(startsWith("INV-")))
                .andReturn().getResponse().getContentAsString();

        Long invoiceId = objectMapper.readTree(createResult).get("id").asLong();
        String invoiceNumber = objectMapper.readTree(createResult).get("invoiceNumber").asText();

        // Step 2: Verify the invoice is visible in the list
        mockMvc.perform(get(BASE_URL + "/customer/" + customer2.getId())
                        .with(fullAuth())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].invoiceNumber", hasItem(invoiceNumber)));

        // Step 3: Post the invoice (DRAFT -> PENDING)
        mockMvc.perform(post(BASE_URL + "/" + invoiceId + "/post")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Step 4: Send the invoice (PENDING -> SENT)
        mockMvc.perform(post(BASE_URL + "/" + invoiceId + "/send")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));

        // Step 5: Verify the invoice can be fetched by number
        mockMvc.perform(get(BASE_URL + "/number/" + invoiceNumber)
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.id").value(invoiceId));

        // Step 6: Verify the invoice shows up in unpaid list for the customer
        mockMvc.perform(get(BASE_URL + "/customer/" + customer2.getId() + "/unpaid")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].invoiceNumber", hasItem(invoiceNumber)));

        // Step 7: Verify customer outstanding balance includes this invoice
        mockMvc.perform(get(BASE_URL + "/customer/" + customer2.getId() + "/outstanding")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(greaterThan(0.0)));
    }
}
