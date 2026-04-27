package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateCustomerRequest;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User adminUser;
    private Customer customer1;
    private Customer customer2;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Customer Tenant").code("FULLFLOW_CUST").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("customeradmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        customer1 = customerRepository.saveAndFlush(Customer.builder()
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
                .currentBalance(new BigDecimal("60000.0000"))
                .totalInvoiced(new BigDecimal("60000.0000")).totalReceived(BigDecimal.ZERO)
                .active(true).creditHold(false).customerType("RETAIL")
                .tenantId(tenant.getId()).build());
    }

    private RequestPostProcessor arAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_AR_READ"),
                        new SimpleGrantedAuthority("FINANCE_AR_WRITE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/finance/customers ----

    @Test
    void getCustomers_returnsPaginatedList() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers").with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/customers/active ----

    @Test
    void getActiveCustomers_returnsOnlyActive() throws Exception {
        Customer inactive = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-000003").name("Inactive Co")
                .currentBalance(BigDecimal.ZERO)
                .totalInvoiced(BigDecimal.ZERO).totalReceived(BigDecimal.ZERO)
                .active(false).tenantId(tenant.getId()).build());

        mockMvc.perform(get("/api/v1/finance/customers/active").with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- GET /api/v1/finance/customers/{id} ----

    @Test
    void getCustomer_existingId_returnsCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers/" + customer1.getId()).with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CUST-000001"))
                .andExpect(jsonPath("$.name").value("Acme Corp"));
    }

    @Test
    void getCustomer_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers/99999").with(arAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/customers/code/{code} ----

    @Test
    void getCustomerByCode_existingCode_returnsCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers/code/CUST-000001").with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Corp"));
    }

    @Test
    void getCustomerByCode_nonExistentCode_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers/code/NONEXIST").with(arAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/customers/search ----

    @Test
    void searchCustomers_byName_returnsMatching() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers/search")
                        .param("query", "Acme").with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Acme Corp"));
    }

    @Test
    void searchCustomers_byEmail_returnsMatching() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers/search")
                        .param("query", "beta@").with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void searchCustomers_noMatch_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers/search")
                        .param("query", "NONEXISTENT").with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ---- GET /api/v1/finance/customers/next-code ----

    @Test
    void getNextCustomerCode_returnsNextCode() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers/next-code").with(arAuth()))
                .andExpect(status().isOk());
    }

    // ---- POST /api/v1/finance/customers ----

    @Test
    void createCustomer_validRequest_returnsCreated() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("Gamma Inc")
                .email("gamma@example.com").phone("+998901111111")
                .address("456 Business Ave").city("Samarkand")
                .creditLimit(new BigDecimal("75000.00"))
                .customerType("WHOLESALE").active(true).build();

        mockMvc.perform(post("/api/v1/finance/customers")
                        .with(arAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gamma Inc"))
                .andExpect(jsonPath("$.code").isNotEmpty());
    }

    @Test
    void createCustomer_withExplicitCode_usesProvidedCode() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .code("CUSTOM-001").name("Custom Code Customer")
                .active(true).build();

        mockMvc.perform(post("/api/v1/finance/customers")
                        .with(arAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CUSTOM-001"));
    }

    @Test
    void createCustomer_duplicateCode_returns400() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .code("CUST-000001").name("Duplicate Code")
                .active(true).build();

        mockMvc.perform(post("/api/v1/finance/customers")
                        .with(arAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCustomer_missingName_returns400() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .code("NONAME").active(true).build();

        mockMvc.perform(post("/api/v1/finance/customers")
                        .with(arAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCustomer_autoGeneratesCode_whenCodeNotProvided() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("Auto Code Customer").active(true).build();

        mockMvc.perform(post("/api/v1/finance/customers")
                        .with(arAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(startsWith("CUST-")));
    }

    // ---- PUT /api/v1/finance/customers/{id} ----

    @Test
    void updateCustomer_validRequest_updatesFields() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .code("CUST-000001").name("Updated Acme Corp")
                .email("updated@acme.com").active(true).build();

        mockMvc.perform(put("/api/v1/finance/customers/" + customer1.getId())
                        .with(arAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Acme Corp"))
                .andExpect(jsonPath("$.email").value("updated@acme.com"));
    }

    @Test
    void updateCustomer_nonExistentId_returns404() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("Update Non Existent").active(true).build();

        mockMvc.perform(put("/api/v1/finance/customers/99999")
                        .with(arAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /api/v1/finance/customers/{id} ----

    @Test
    void deleteCustomer_zeroBalance_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/finance/customers/" + customer1.getId())
                        .with(arAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCustomer_nonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/finance/customers/99999").with(arAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- PATCH /api/v1/finance/customers/{id}/activate ----

    @Test
    void activateCustomer_deactivatedCustomer_activates() throws Exception {
        Customer inactive = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-INACT").name("Inactive")
                .currentBalance(BigDecimal.ZERO)
                .totalInvoiced(BigDecimal.ZERO).totalReceived(BigDecimal.ZERO)
                .active(false).tenantId(tenant.getId()).build());

        mockMvc.perform(patch("/api/v1/finance/customers/" + inactive.getId() + "/activate")
                        .with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---- PATCH /api/v1/finance/customers/{id}/credit-hold ----

    @Test
    void setCreditHold_setsHoldTrue() throws Exception {
        mockMvc.perform(patch("/api/v1/finance/customers/" + customer1.getId() + "/credit-hold")
                        .param("hold", "true")
                        .with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditHold").value(true));
    }

    @Test
    void setCreditHold_removesHold() throws Exception {
        customer1.setCreditHold(true);
        customerRepository.saveAndFlush(customer1);

        mockMvc.perform(patch("/api/v1/finance/customers/" + customer1.getId() + "/credit-hold")
                        .param("hold", "false")
                        .with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditHold").value(false));
    }

    // ---- PATCH /api/v1/finance/customers/{id}/credit-limit ----

    @Test
    void updateCreditLimit_updatesLimit() throws Exception {
        mockMvc.perform(patch("/api/v1/finance/customers/" + customer1.getId() + "/credit-limit")
                        .param("creditLimit", "200000.00")
                        .with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditLimit").value(200000.00));
    }

    // ---- GET /api/v1/finance/customers/credit-hold ----

    @Test
    void getCustomersOnCreditHold_returnsHeldCustomers() throws Exception {
        customer1.setCreditHold(true);
        customerRepository.saveAndFlush(customer1);

        mockMvc.perform(get("/api/v1/finance/customers/credit-hold").with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---- GET /api/v1/finance/customers/over-credit-limit ----

    @Test
    void getCustomersOverCreditLimit_returnsOverLimitCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers/over-credit-limit").with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("CUST-000002"));
    }

    // ---- GET /api/v1/finance/customers/{id}/can-invoice ----

    @Test
    void canBeInvoiced_underLimit_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers/" + customer1.getId() + "/can-invoice")
                        .param("amount", "50000")
                        .with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void canBeInvoiced_overLimit_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/v1/finance/customers/" + customer2.getId() + "/can-invoice")
                        .param("amount", "1000")
                        .with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    void canBeInvoiced_onCreditHold_returnsFalse() throws Exception {
        customer1.setCreditHold(true);
        customerRepository.saveAndFlush(customer1);

        mockMvc.perform(get("/api/v1/finance/customers/" + customer1.getId() + "/can-invoice")
                        .param("amount", "1000")
                        .with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    // ---- Permission checks ----

    @Test
    void getCustomers_noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/finance/customers")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCustomer_readOnlyPermission_returns403() throws Exception {
        UserPrincipal readOnly = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("FINANCE_AR_READ")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                readOnly, null, readOnly.getAuthorities());

        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("No Permission Customer").active(true).build();

        mockMvc.perform(post("/api/v1/finance/customers")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---- Full CRUD lifecycle ----

    @Test
    void fullCrudLifecycle() throws Exception {
        CreateCustomerRequest createReq = CreateCustomerRequest.builder()
                .name("Lifecycle Customer")
                .email("lifecycle@test.com").phone("+998900000000")
                .creditLimit(new BigDecimal("50000.00"))
                .customerType("RETAIL").active(true).build();

        String result = mockMvc.perform(post("/api/v1/finance/customers")
                        .with(arAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(result).get("id").asLong();
        String code = objectMapper.readTree(result).get("code").asText();

        mockMvc.perform(get("/api/v1/finance/customers/code/" + code).with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lifecycle Customer"));

        CreateCustomerRequest updateReq = CreateCustomerRequest.builder()
                .code(code).name("Updated Lifecycle")
                .email("updated@test.com").active(true).build();

        mockMvc.perform(put("/api/v1/finance/customers/" + createdId)
                        .with(arAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Lifecycle"));

        mockMvc.perform(patch("/api/v1/finance/customers/" + createdId + "/credit-hold")
                        .param("hold", "true").with(arAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditHold").value(true));

        mockMvc.perform(delete("/api/v1/finance/customers/" + createdId).with(arAuth()))
                .andExpect(status().isNoContent());
    }
}
