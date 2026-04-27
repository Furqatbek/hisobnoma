package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateARPaymentAllocationRequest;
import com.hisobnoma.platform.finance.dto.CreateARPaymentRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.ARPaymentRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.finance.repository.FiscalPeriodRepository;
import com.hisobnoma.platform.finance.repository.FiscalYearRepository;
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
class ARPaymentControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ARPaymentRepository arPaymentRepository;
    @Autowired private ARInvoiceRepository arInvoiceRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User adminUser;
    private Customer customer;
    private Customer customer2;
    private ARInvoice invoice;
    private ARInvoice invoice2;
    private ARPayment pendingPayment;
    private ARPayment completedPayment;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("AR Payment Tenant").code("FULLFLOW_ARPAY").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("arpaymentadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        customer = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-000001").name("Acme Corp")
                .email("acme@example.com").phone("+998901234567")
                .address("123 Main St").city("Tashkent").country("Uzbekistan")
                .creditLimit(new BigDecimal("100000.0000"))
                .currentBalance(BigDecimal.ZERO)
                .totalInvoiced(BigDecimal.ZERO).totalReceived(BigDecimal.ZERO)
                .active(true).creditHold(false).customerType("WHOLESALE")
                .tenantId(tenant.getId()).build());

        customer2 = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-000002").name("Beta LLC")
                .email("beta@example.com").phone("+998907654321")
                .creditLimit(new BigDecimal("50000.0000"))
                .currentBalance(BigDecimal.ZERO)
                .totalInvoiced(BigDecimal.ZERO).totalReceived(BigDecimal.ZERO)
                .active(true).creditHold(false).customerType("RETAIL")
                .tenantId(tenant.getId()).build());

        // GL accounts required by GLIntegrationService for completePayment
        accountRepository.saveAndFlush(Account.builder()
                .code("1110").name("Cash")
                .accountType(AccountType.ASSET).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true).systemAccount(false)
                .tenantId(tenant.getId()).build());
        accountRepository.saveAndFlush(Account.builder()
                .code("1130").name("Accounts Receivable")
                .accountType(AccountType.ASSET).normalBalance(NormalBalance.DEBIT)
                .active(true).allowsDirectPosting(true).systemAccount(false)
                .tenantId(tenant.getId()).build());

        // Fiscal year and period for GL posting
        FiscalYear fiscalYear = fiscalYearRepository.saveAndFlush(FiscalYear.builder()
                .year(LocalDate.now().getYear()).name("FY " + LocalDate.now().getYear())
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

        invoice = arInvoiceRepository.saveAndFlush(ARInvoice.builder()
                .invoiceNumber("INV-000001")
                .customer(customer)
                .customerName(customer.getName())
                .invoiceDate(LocalDate.now().minusDays(30))
                .dueDate(LocalDate.now().plusDays(30))
                .status(ARInvoiceStatus.SENT)
                .subtotal(new BigDecimal("5000.0000"))
                .totalAmount(new BigDecimal("5000.0000"))
                .balanceDue(new BigDecimal("5000.0000"))
                .paidAmount(BigDecimal.ZERO)
                .creditApplied(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId()).build());

        invoice2 = arInvoiceRepository.saveAndFlush(ARInvoice.builder()
                .invoiceNumber("INV-000002")
                .customer(customer)
                .customerName(customer.getName())
                .invoiceDate(LocalDate.now().minusDays(15))
                .dueDate(LocalDate.now().plusDays(45))
                .status(ARInvoiceStatus.SENT)
                .subtotal(new BigDecimal("3000.0000"))
                .totalAmount(new BigDecimal("3000.0000"))
                .balanceDue(new BigDecimal("3000.0000"))
                .paidAmount(BigDecimal.ZERO)
                .creditApplied(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId()).build());

        pendingPayment = arPaymentRepository.saveAndFlush(ARPayment.builder()
                .paymentNumber("REC-000001")
                .customer(customer)
                .customerName(customer.getName())
                .paymentDate(LocalDate.now())
                .status(ARPaymentStatus.PENDING)
                .paymentMethod(ARPaymentMethod.CASH)
                .paymentAmount(new BigDecimal("1000.0000"))
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(new BigDecimal("1000.0000"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .deposited(false)
                .glPosted(false)
                .tenantId(tenant.getId()).build());

        completedPayment = arPaymentRepository.saveAndFlush(ARPayment.builder()
                .paymentNumber("REC-000002")
                .customer(customer2)
                .customerName(customer2.getName())
                .paymentDate(LocalDate.now().minusDays(5))
                .status(ARPaymentStatus.COMPLETED)
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("2500.0000"))
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(new BigDecimal("2500.0000"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .deposited(false)
                .glPosted(false)
                .tenantId(tenant.getId()).build());

        entityManager.flush();
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

    private CreateARPaymentRequest validCreateRequest() {
        return CreateARPaymentRequest.builder()
                .customerId(customer.getId())
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.CASH)
                .paymentAmount(new BigDecimal("2000.00"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .referenceNumber("REF-001")
                .memo("Test payment")
                .notes("Integration test payment")
                .allocations(List.of(
                        CreateARPaymentAllocationRequest.builder()
                                .arInvoiceId(invoice.getId())
                                .allocatedAmount(new BigDecimal("2000.00"))
                                .build()))
                .build();
    }

    // ---- GET /api/v1/finance/ar-payments ----

    @Test
    void getPayments_returnsPaginatedList() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-payments").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/ar-payments/{id} ----

    @Test
    void getPaymentById_returnsPayment() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-payments/" + pendingPayment.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pendingPayment.getId()))
                .andExpect(jsonPath("$.paymentNumber").value("REC-000001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.paymentAmount").value(closeTo(1000.0, 0.01)));
    }

    // ---- GET /api/v1/finance/ar-payments/customer/{customerId} ----

    @Test
    void getPaymentsByCustomer_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-payments/customer/" + customer.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].paymentNumber").value("REC-000001"));

        // Customer2 should have only the completed payment
        mockMvc.perform(get("/api/v1/finance/ar-payments/customer/" + customer2.getId())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].paymentNumber").value("REC-000002"));
    }

    // ---- GET /api/v1/finance/ar-payments/status/{status} ----

    @Test
    void getPaymentsByStatus_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-payments/status/PENDING").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].paymentNumber").value("REC-000001"));

        mockMvc.perform(get("/api/v1/finance/ar-payments/status/COMPLETED").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].paymentNumber").value("REC-000002"));
    }

    // ---- POST /api/v1/finance/ar-payments ----

    @Test
    void createPayment_validRequest_returnsCreated() throws Exception {
        CreateARPaymentRequest request = validCreateRequest();

        mockMvc.perform(post("/api/v1/finance/ar-payments")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentNumber").value(startsWith("REC-")))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.paymentAmount").value(closeTo(2000.0, 0.01)))
                .andExpect(jsonPath("$.currency").value("UZS"))
                .andExpect(jsonPath("$.referenceNumber").value("REF-001"))
                .andExpect(jsonPath("$.memo").value("Test payment"))
                .andExpect(jsonPath("$.allocations").isArray())
                .andExpect(jsonPath("$.allocations.length()").value(1));
    }

    @Test
    void createPayment_missingAmount_returns400() throws Exception {
        CreateARPaymentRequest request = CreateARPaymentRequest.builder()
                .customerId(customer.getId())
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.CASH)
                // paymentAmount intentionally omitted
                .allocations(List.of(
                        CreateARPaymentAllocationRequest.builder()
                                .arInvoiceId(invoice.getId())
                                .allocatedAmount(new BigDecimal("100.00"))
                                .build()))
                .build();

        mockMvc.perform(post("/api/v1/finance/ar-payments")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_missingCustomerId_returns400() throws Exception {
        CreateARPaymentRequest request = CreateARPaymentRequest.builder()
                // customerId intentionally omitted
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.CASH)
                .paymentAmount(new BigDecimal("500.00"))
                .allocations(List.of(
                        CreateARPaymentAllocationRequest.builder()
                                .arInvoiceId(invoice.getId())
                                .allocatedAmount(new BigDecimal("500.00"))
                                .build()))
                .build();

        mockMvc.perform(post("/api/v1/finance/ar-payments")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /api/v1/finance/ar-payments/{id}/complete ----

    @Test
    void completePayment_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + pendingPayment.getId() + "/complete")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pendingPayment.getId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void completePayment_alreadyCompleted_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + completedPayment.getId() + "/complete")
                        .with(fullAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /api/v1/finance/ar-payments/{id}/cancel ----

    @Test
    void cancelPayment_pendingPayment_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + pendingPayment.getId() + "/cancel")
                        .param("reason", "Customer requested cancellation")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pendingPayment.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelPayment_completedPayment_returns200() throws Exception {
        // Cancelling a completed payment should also succeed (reverses GL, invoices, balance)
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + completedPayment.getId() + "/cancel")
                        .param("reason", "Erroneous payment")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ---- POST /api/v1/finance/ar-payments/{id}/deposit ----

    @Test
    void markAsDeposited_updatesDepositInfo() throws Exception {
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + completedPayment.getId() + "/deposit")
                        .param("bankReference", "DEP-2024-0001")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(completedPayment.getId()))
                .andExpect(jsonPath("$.deposited").value(true))
                .andExpect(jsonPath("$.depositReference").value("DEP-2024-0001"));
    }

    @Test
    void markAsDeposited_pendingPayment_returns400() throws Exception {
        // Only completed payments can be marked as deposited
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + pendingPayment.getId() + "/deposit")
                        .param("bankReference", "DEP-2024-0002")
                        .with(fullAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/v1/finance/ar-payments/undeposited ----

    @Test
    void getUndepositedPayments_returnsUndeposited() throws Exception {
        // completedPayment is COMPLETED and not deposited
        mockMvc.perform(get("/api/v1/finance/ar-payments/undeposited").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].paymentNumber").value("REC-000002"))
                .andExpect(jsonPath("$[0].deposited").value(false));
    }

    @Test
    void getUndepositedPayments_afterDeposit_excludesDeposited() throws Exception {
        // Deposit the completed payment first
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + completedPayment.getId() + "/deposit")
                        .param("bankReference", "DEP-2024-0003")
                        .with(fullAuth()))
                .andExpect(status().isOk());

        // Now undeposited should be empty
        mockMvc.perform(get("/api/v1/finance/ar-payments/undeposited").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- GET /api/v1/finance/ar-payments/number/{paymentNumber} ----

    @Test
    void getPaymentByNumber_returnsPayment() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-payments/number/REC-000001").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentNumber").value("REC-000001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getPaymentByNumber_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-payments/number/REC-999999").with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_readOnly_cannotWrite() throws Exception {
        CreateARPaymentRequest request = validCreateRequest();

        // Read-only user should be able to read
        mockMvc.perform(get("/api/v1/finance/ar-payments").with(readOnlyAuth()))
                .andExpect(status().isOk());

        // Read-only user should NOT be able to create (requires FINANCE_AR_WRITE)
        mockMvc.perform(post("/api/v1/finance/ar-payments")
                        .with(readOnlyAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Read-only user should NOT be able to complete (requires FINANCE_AR_APPROVE)
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + pendingPayment.getId() + "/complete")
                        .with(readOnlyAuth()))
                .andExpect(status().isForbidden());

        // Read-only user should NOT be able to cancel (requires FINANCE_AR_APPROVE)
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + pendingPayment.getId() + "/cancel")
                        .param("reason", "test")
                        .with(readOnlyAuth()))
                .andExpect(status().isForbidden());

        // Read-only user should NOT be able to deposit (requires FINANCE_AR_WRITE)
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + completedPayment.getId() + "/deposit")
                        .param("bankReference", "DEP-X")
                        .with(readOnlyAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/finance/ar-payments").with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // ---- Full lifecycle: create -> complete -> deposit ----

    @Test
    void fullLifecycle_createCompleteDeposit() throws Exception {
        // Step 1: Create a payment
        CreateARPaymentRequest createRequest = CreateARPaymentRequest.builder()
                .customerId(customer.getId())
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.CHECK)
                .paymentAmount(new BigDecimal("3000.00"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .referenceNumber("CHK-101")
                .checkNumber("10001")
                .checkDate(LocalDate.now())
                .memo("Full lifecycle test")
                .allocations(List.of(
                        CreateARPaymentAllocationRequest.builder()
                                .arInvoiceId(invoice.getId())
                                .allocatedAmount(new BigDecimal("3000.00"))
                                .build()))
                .build();

        String createResult = mockMvc.perform(post("/api/v1/finance/ar-payments")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentNumber").value(startsWith("REC-")))
                .andExpect(jsonPath("$.paymentMethod").value("CHECK"))
                .andExpect(jsonPath("$.checkNumber").value("10001"))
                .andExpect(jsonPath("$.deposited").value(false))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(createResult).get("id").asLong();
        String paymentNumber = objectMapper.readTree(createResult).get("paymentNumber").asText();

        // Step 2: Verify we can look it up by number
        mockMvc.perform(get("/api/v1/finance/ar-payments/number/" + paymentNumber)
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId));

        // Step 3: Complete the payment
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + createdId + "/complete")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Step 4: Verify it appears in undeposited
        mockMvc.perform(get("/api/v1/finance/ar-payments/undeposited").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + createdId + ")]").exists());

        // Step 5: Mark as deposited
        mockMvc.perform(post("/api/v1/finance/ar-payments/" + createdId + "/deposit")
                        .param("bankReference", "BANK-DEP-001")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deposited").value(true))
                .andExpect(jsonPath("$.depositReference").value("BANK-DEP-001"));

        // Step 6: Verify it no longer appears in undeposited
        mockMvc.perform(get("/api/v1/finance/ar-payments/undeposited").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + createdId + ")]").doesNotExist());

        // Step 7: Verify final state by fetching by ID
        mockMvc.perform(get("/api/v1/finance/ar-payments/" + createdId).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.deposited").value(true))
                .andExpect(jsonPath("$.depositReference").value("BANK-DEP-001"))
                .andExpect(jsonPath("$.paymentAmount").value(closeTo(3000.0, 0.01)));
    }

    // ---- Date range queries ----

    @Test
    void getPaymentsByDateRange_returnsFiltered() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(1);

        mockMvc.perform(get("/api/v1/finance/ar-payments/date-range")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    // ---- Payment auto-numbering ----

    @Test
    void createPayment_generatesAutoNumber() throws Exception {
        CreateARPaymentRequest request = validCreateRequest();

        String result1 = mockMvc.perform(post("/api/v1/finance/ar-payments")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String number1 = objectMapper.readTree(result1).get("paymentNumber").asText();

        // Create a second payment — number should be sequential
        CreateARPaymentRequest request2 = CreateARPaymentRequest.builder()
                .customerId(customer.getId())
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("500.00"))
                .currency("UZS")
                .allocations(List.of(
                        CreateARPaymentAllocationRequest.builder()
                                .arInvoiceId(invoice2.getId())
                                .allocatedAmount(new BigDecimal("500.00"))
                                .build()))
                .build();

        String result2 = mockMvc.perform(post("/api/v1/finance/ar-payments")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String number2 = objectMapper.readTree(result2).get("paymentNumber").asText();

        // Both numbers should start with REC- and the second should be higher
        org.assertj.core.api.Assertions.assertThat(number1).startsWith("REC-");
        org.assertj.core.api.Assertions.assertThat(number2).startsWith("REC-");
        int seq1 = Integer.parseInt(number1.substring(4));
        int seq2 = Integer.parseInt(number2.substring(4));
        org.assertj.core.api.Assertions.assertThat(seq2).isGreaterThan(seq1);
    }

    // ---- Create and complete in one step ----

    @Test
    void createAndCompletePayment_returnsCompleted() throws Exception {
        CreateARPaymentRequest request = CreateARPaymentRequest.builder()
                .customerId(customer.getId())
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.MOBILE_PAYMENT)
                .paymentAmount(new BigDecimal("1500.00"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .referenceNumber("MOB-001")
                .memo("One-step pay")
                .allocations(List.of(
                        CreateARPaymentAllocationRequest.builder()
                                .arInvoiceId(invoice2.getId())
                                .allocatedAmount(new BigDecimal("1500.00"))
                                .build()))
                .build();

        mockMvc.perform(post("/api/v1/finance/ar-payments/pay")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentNumber").value(startsWith("REC-")))
                .andExpect(jsonPath("$.paymentAmount").value(closeTo(1500.0, 0.01)));
    }
}
