package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateCreditNoteRequest;
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
class CreditNoteControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/finance/credit-notes";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CreditNoteRepository creditNoteRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ARInvoiceRepository arInvoiceRepository;
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
    private Customer otherCustomer;
    private CreditNote draftCreditNote;
    private CreditNote approvedCreditNote;
    private ARInvoice arInvoice;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("CreditNote Tenant").code("FULLFLOW_CN").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("creditnoteadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        // GL accounts needed for credit note GL posting (codes match GLIntegrationService constants)
        accountRepository.saveAndFlush(Account.builder()
                .code("1130").name("Accounts Receivable").accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT).accountLevel(1).fullPath("1130")
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());

        accountRepository.saveAndFlush(Account.builder()
                .code("4300").name("Sales Returns & Allowances").accountType(AccountType.REVENUE)
                .normalBalance(NormalBalance.CREDIT).accountLevel(1).fullPath("4300")
                .active(true).allowsDirectPosting(true)
                .tenantId(tenant.getId()).build());

        // Fiscal year and period covering today's date (needed for GL posting during approve)
        LocalDate today = LocalDate.now();
        FiscalYear fiscalYear = fiscalYearRepository.saveAndFlush(FiscalYear.builder()
                .year(today.getYear()).name("Fiscal Year " + today.getYear())
                .startDate(LocalDate.of(today.getYear(), 1, 1))
                .endDate(LocalDate.of(today.getYear(), 12, 31))
                .status(PeriodStatus.OPEN).current(true).closed(false)
                .tenantId(tenant.getId()).build());

        fiscalPeriodRepository.saveAndFlush(FiscalPeriod.builder()
                .fiscalYear(fiscalYear).periodNumber(today.getMonthValue())
                .name(today.getMonth().name())
                .startDate(today.withDayOfMonth(1))
                .endDate(today.withDayOfMonth(today.lengthOfMonth()))
                .status(PeriodStatus.OPEN).adjustmentPeriod(false)
                .tenantId(tenant.getId()).build());

        // Customers
        customer = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-001").name("Test Customer LLC")
                .email("test@customer.com").phone("+998901234567")
                .active(true).tenantId(tenant.getId()).build());

        otherCustomer = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-002").name("Other Customer Inc")
                .email("other@customer.com").active(true)
                .tenantId(tenant.getId()).build());

        // AR Invoice for the customer (used in apply-to-invoice tests)
        arInvoice = arInvoiceRepository.saveAndFlush(ARInvoice.builder()
                .invoiceNumber("INV-000001")
                .customer(customer).customerName(customer.getName())
                .invoiceDate(today).dueDate(today.plusDays(30))
                .status(ARInvoiceStatus.SENT)
                .subtotal(new BigDecimal("1000.0000"))
                .totalAmount(new BigDecimal("1000.0000"))
                .balanceDue(new BigDecimal("1000.0000"))
                .paidAmount(BigDecimal.ZERO)
                .currency("UZS").exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId()).build());

        // Pre-existing DRAFT credit note
        draftCreditNote = creditNoteRepository.saveAndFlush(CreditNote.builder()
                .creditNoteNumber("CN-000001")
                .customer(customer).customerName(customer.getName())
                .creditNoteDate(today)
                .status(CreditNoteStatus.DRAFT)
                .reasonCode("RETURN").reason("Defective goods returned")
                .creditAmount(new BigDecimal("200.0000"))
                .subtotal(new BigDecimal("200.0000"))
                .totalAmount(new BigDecimal("200.0000"))
                .balance(new BigDecimal("200.0000"))
                .appliedAmount(BigDecimal.ZERO)
                .currency("UZS").exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId()).build());

        // Pre-existing APPROVED credit note (for apply/cancel tests)
        approvedCreditNote = creditNoteRepository.saveAndFlush(CreditNote.builder()
                .creditNoteNumber("CN-000002")
                .customer(customer).customerName(customer.getName())
                .creditNoteDate(today)
                .status(CreditNoteStatus.APPROVED)
                .reasonCode("OVERCHARGE").reason("Customer overcharged")
                .creditAmount(new BigDecimal("300.0000"))
                .subtotal(new BigDecimal("300.0000"))
                .totalAmount(new BigDecimal("300.0000"))
                .balance(new BigDecimal("300.0000"))
                .appliedAmount(BigDecimal.ZERO)
                .currency("UZS").exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ========================= Auth helpers =========================

    private RequestPostProcessor allPermsAuth() {
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

    private RequestPostProcessor noPermsAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ========================= Helper: build valid request =========================

    private CreateCreditNoteRequest buildValidRequest() {
        return CreateCreditNoteRequest.builder()
                .customerId(customer.getId())
                .creditNoteDate(LocalDate.now())
                .reasonCode("RETURN")
                .reason("Goods returned by customer")
                .creditAmount(new BigDecimal("150.00"))
                .subtotal(new BigDecimal("150.00"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .description("Test credit note")
                .build();
    }

    // ========================= GET /api/v1/finance/credit-notes =========================

    @Test
    void getCreditNotes_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.content[*].creditNoteNumber", hasItems("CN-000001", "CN-000002")));
    }

    // ========================= GET /api/v1/finance/credit-notes/{id} =========================

    @Test
    void getCreditNoteById_returnsCreditNote() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + draftCreditNote.getId()).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditNoteNumber").value("CN-000001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.customerName").value(customer.getName()))
                .andExpect(jsonPath("$.reason").value("Defective goods returned"));
    }

    @Test
    void getCreditNoteById_nonExistent_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/99999").with(allPermsAuth()))
                .andExpect(status().isNotFound());
    }

    // ========================= GET /api/v1/finance/credit-notes/number/{number} =========================

    @Test
    void getCreditNoteByNumber_returnsCreditNote() throws Exception {
        mockMvc.perform(get(BASE_URL + "/number/CN-000002").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditNoteNumber").value("CN-000002"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    // ========================= GET /api/v1/finance/credit-notes/customer/{customerId} =========================

    @Test
    void getCreditNotesByCustomer_returnsFiltered() throws Exception {
        // Create a credit note for the other customer
        creditNoteRepository.saveAndFlush(CreditNote.builder()
                .creditNoteNumber("CN-000003")
                .customer(otherCustomer).customerName(otherCustomer.getName())
                .creditNoteDate(LocalDate.now())
                .status(CreditNoteStatus.DRAFT)
                .creditAmount(new BigDecimal("100.0000"))
                .totalAmount(new BigDecimal("100.0000"))
                .balance(new BigDecimal("100.0000"))
                .appliedAmount(BigDecimal.ZERO)
                .currency("UZS").exchangeRate(BigDecimal.ONE)
                .tenantId(tenant.getId()).build());
        entityManager.clear();

        // Query for the first customer - should only get the 2 original credit notes
        mockMvc.perform(get(BASE_URL + "/customer/" + customer.getId()).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].creditNoteNumber", hasItems("CN-000001", "CN-000002")));

        // Query for the other customer - should get only 1
        mockMvc.perform(get(BASE_URL + "/customer/" + otherCustomer.getId()).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].creditNoteNumber").value("CN-000003"));
    }

    // ========================= GET /api/v1/finance/credit-notes/status/{status} =========================

    @Test
    void getCreditNotesByStatus_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/status/DRAFT").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].creditNoteNumber").value("CN-000001"));

        mockMvc.perform(get(BASE_URL + "/status/APPROVED").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].creditNoteNumber").value("CN-000002"));
    }

    // ========================= GET .../customer/{id}/available =========================

    @Test
    void getAvailableCreditNotes_returnsApprovedWithBalance() throws Exception {
        mockMvc.perform(get(BASE_URL + "/customer/" + customer.getId() + "/available").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].creditNoteNumber").value("CN-000002"))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    // ========================= GET .../customer/{id}/available-credit =========================

    @Test
    void getAvailableCredit_returnsSumOfBalances() throws Exception {
        mockMvc.perform(get(BASE_URL + "/customer/" + customer.getId() + "/available-credit").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(closeTo(300.0, 0.01)));
    }

    // ========================= POST /api/v1/finance/credit-notes =========================

    @Test
    void createCreditNote_validRequest_returnsCreated() throws Exception {
        CreateCreditNoteRequest request = buildValidRequest();

        mockMvc.perform(post(BASE_URL)
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditNoteNumber").value(startsWith("CN-")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.reason").value("Goods returned by customer"))
                .andExpect(jsonPath("$.balance").value(closeTo(150.0, 0.01)));
    }

    @Test
    void createCreditNote_withAutoNumber_generatesSequential() throws Exception {
        CreateCreditNoteRequest request = buildValidRequest();

        String result = mockMvc.perform(post(BASE_URL)
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Already have CN-000001 and CN-000002 in setUp, so next should be CN-000003
        String generatedNumber = objectMapper.readTree(result).get("creditNoteNumber").asText();
        org.assertj.core.api.Assertions.assertThat(generatedNumber).isEqualTo("CN-000003");
    }

    @Test
    void createCreditNote_missingAmount_returns400() throws Exception {
        CreateCreditNoteRequest request = CreateCreditNoteRequest.builder()
                .customerId(customer.getId())
                .creditNoteDate(LocalDate.now())
                .reason("Missing amount test")
                // creditAmount intentionally omitted
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCreditNote_missingCustomerId_returns400() throws Exception {
        CreateCreditNoteRequest request = CreateCreditNoteRequest.builder()
                // customerId intentionally omitted
                .creditNoteDate(LocalDate.now())
                .creditAmount(new BigDecimal("100.00"))
                .reason("Missing customer test")
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCreditNote_missingDate_returns400() throws Exception {
        CreateCreditNoteRequest request = CreateCreditNoteRequest.builder()
                .customerId(customer.getId())
                // creditNoteDate intentionally omitted
                .creditAmount(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCreditNote_zeroAmount_returns400() throws Exception {
        CreateCreditNoteRequest request = CreateCreditNoteRequest.builder()
                .customerId(customer.getId())
                .creditNoteDate(LocalDate.now())
                .creditAmount(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========================= PUT /api/v1/finance/credit-notes/{id} =========================

    @Test
    void updateCreditNote_draftNote_updatesSuccessfully() throws Exception {
        CreateCreditNoteRequest request = CreateCreditNoteRequest.builder()
                .customerId(customer.getId())
                .creditNoteDate(LocalDate.now())
                .creditAmount(new BigDecimal("250.00"))
                .reason("Updated reason")
                .currency("UZS")
                .build();

        mockMvc.perform(put(BASE_URL + "/" + draftCreditNote.getId())
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Updated reason"))
                .andExpect(jsonPath("$.balance").value(closeTo(250.0, 0.01)));
    }

    @Test
    void updateCreditNote_approvedNote_returns400() throws Exception {
        CreateCreditNoteRequest request = buildValidRequest();

        mockMvc.perform(put(BASE_URL + "/" + approvedCreditNote.getId())
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========================= POST /api/v1/finance/credit-notes/{id}/approve =========================

    @Test
    void approveCreditNote_draftToApproved() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + draftCreditNote.getId() + "/approve")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.creditNoteNumber").value("CN-000001"));
    }

    @Test
    void approveCreditNote_alreadyApproved_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + approvedCreditNote.getId() + "/approve")
                        .with(allPermsAuth()))
                .andExpect(status().isBadRequest());
    }

    // ========================= POST /api/v1/finance/credit-notes/{id}/apply-to-invoice =========================

    @Test
    void applyToInvoice_validAmount_appliesSuccessfully() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + approvedCreditNote.getId() + "/apply-to-invoice")
                        .param("invoiceId", arInvoice.getId().toString())
                        .param("amount", "200.00")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedAmount").value(closeTo(200.0, 0.01)))
                .andExpect(jsonPath("$.balance").value(closeTo(100.0, 0.01)))
                .andExpect(jsonPath("$.status").value("PARTIAL"));
    }

    @Test
    void applyToInvoice_fullAmount_statusBecomesApplied() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + approvedCreditNote.getId() + "/apply-to-invoice")
                        .param("invoiceId", arInvoice.getId().toString())
                        .param("amount", "300.00")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.balance").value(closeTo(0.0, 0.01)));
    }

    @Test
    void applyToInvoice_exceedsCreditBalance_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + approvedCreditNote.getId() + "/apply-to-invoice")
                        .param("invoiceId", arInvoice.getId().toString())
                        .param("amount", "500.00")
                        .with(allPermsAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void applyToInvoice_draftCreditNote_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + draftCreditNote.getId() + "/apply-to-invoice")
                        .param("invoiceId", arInvoice.getId().toString())
                        .param("amount", "100.00")
                        .with(allPermsAuth()))
                .andExpect(status().isBadRequest());
    }

    // ========================= POST /api/v1/finance/credit-notes/{id}/cancel =========================

    @Test
    void cancelCreditNote_draftNoApplied_returns200() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + draftCreditNote.getId() + "/cancel")
                        .param("reason", "No longer needed")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelCreditNote_approvedNoApplied_returns200() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + approvedCreditNote.getId() + "/cancel")
                        .param("reason", "Issued in error")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelCreditNote_withAppliedAmount_returns400() throws Exception {
        // First apply part of the approved credit note to an invoice
        mockMvc.perform(post(BASE_URL + "/" + approvedCreditNote.getId() + "/apply-to-invoice")
                        .param("invoiceId", arInvoice.getId().toString())
                        .param("amount", "100.00")
                        .with(allPermsAuth()))
                .andExpect(status().isOk());

        // Now try to cancel it - should fail because it has applied amount
        mockMvc.perform(post(BASE_URL + "/" + approvedCreditNote.getId() + "/cancel")
                        .param("reason", "Want to cancel")
                        .with(allPermsAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelCreditNote_alreadyCancelled_returns400() throws Exception {
        // Cancel the draft first
        mockMvc.perform(post(BASE_URL + "/" + draftCreditNote.getId() + "/cancel")
                        .param("reason", "First cancellation")
                        .with(allPermsAuth()))
                .andExpect(status().isOk());

        // Try to cancel again - should fail
        mockMvc.perform(post(BASE_URL + "/" + draftCreditNote.getId() + "/cancel")
                        .param("reason", "Second cancellation attempt")
                        .with(allPermsAuth()))
                .andExpect(status().isBadRequest());
    }

    // ========================= Permission checks =========================

    @Test
    void getCreditNotes_noPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(noPermsAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCreditNote_readOnlyPermission_returns403() throws Exception {
        CreateCreditNoteRequest request = buildValidRequest();

        mockMvc.perform(post(BASE_URL)
                        .with(readOnlyAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveCreditNote_readOnlyPermission_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + draftCreditNote.getId() + "/approve")
                        .with(readOnlyAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void applyToInvoice_readOnlyPermission_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + approvedCreditNote.getId() + "/apply-to-invoice")
                        .param("invoiceId", arInvoice.getId().toString())
                        .param("amount", "100.00")
                        .with(readOnlyAuth()))
                .andExpect(status().isForbidden());
    }

    // ========================= Full lifecycle =========================

    @Test
    void fullLifecycle_createApproveApplyCancel() throws Exception {
        // Step 1: Create a credit note
        CreateCreditNoteRequest createRequest = CreateCreditNoteRequest.builder()
                .customerId(customer.getId())
                .creditNoteDate(LocalDate.now())
                .reasonCode("RETURN")
                .reason("Full lifecycle test")
                .creditAmount(new BigDecimal("500.00"))
                .subtotal(new BigDecimal("500.00"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .description("Lifecycle test credit note")
                .build();

        String createResult = mockMvc.perform(post(BASE_URL)
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        Long creditNoteId = objectMapper.readTree(createResult).get("id").asLong();
        String creditNoteNumber = objectMapper.readTree(createResult).get("creditNoteNumber").asText();

        // Step 2: Verify it appears in listing
        mockMvc.perform(get(BASE_URL + "/number/" + creditNoteNumber).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // Step 3: Approve the credit note
        mockMvc.perform(post(BASE_URL + "/" + creditNoteId + "/approve")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Step 4: Apply partial amount to invoice
        mockMvc.perform(post(BASE_URL + "/" + creditNoteId + "/apply-to-invoice")
                        .param("invoiceId", arInvoice.getId().toString())
                        .param("amount", "250.00")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.appliedAmount").value(closeTo(250.0, 0.01)))
                .andExpect(jsonPath("$.balance").value(closeTo(250.0, 0.01)));

        // Step 5: Cannot cancel because it has applied amount
        mockMvc.perform(post(BASE_URL + "/" + creditNoteId + "/cancel")
                        .param("reason", "Trying to cancel after apply")
                        .with(allPermsAuth()))
                .andExpect(status().isBadRequest());

        // Step 6: Verify available credit for customer reflects remaining balance
        mockMvc.perform(get(BASE_URL + "/customer/" + customer.getId() + "/available-credit")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                // original approved CN-000002 has 300 balance + new lifecycle CN has 250 remaining
                .andExpect(jsonPath("$").value(closeTo(550.0, 0.01)));
    }
}
