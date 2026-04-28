package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.dto.CreateVendorContactRequest;
import com.hisobnoma.platform.inventory.dto.CreateVendorRequest;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.entity.VendorContact;
import com.hisobnoma.platform.inventory.repository.VendorContactRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VendorControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private VendorContactRepository vendorContactRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/inventory/vendors";

    private Tenant tenant;
    private User user;
    private Vendor vendor1;
    private Vendor vendor2;
    private VendorContact vendorContact;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Vendor Test Tenant").code("FULLFLOW_VND").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("vendortestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        vendor1 = vendorRepository.saveAndFlush(Vendor.builder()
                .code("VND-001").name("Active Vendor")
                .email("active@vendor.com").phone("+998901234567")
                .city("Tashkent")
                .active(true).preferred(true)
                .tenantId(tenant.getId()).build());

        vendor2 = vendorRepository.saveAndFlush(Vendor.builder()
                .code("VND-002").name("Inactive Vendor")
                .active(false).preferred(false)
                .tenantId(tenant.getId()).build());

        vendorContact = vendorContactRepository.saveAndFlush(VendorContact.builder()
                .vendor(vendor1)
                .name("John Contact")
                .primary(true)
                .billingContact(true)
                .active(true)
                .build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor authWith(String... permissions) {
        List<SimpleGrantedAuthority> authorities = List.of(permissions).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, authorities);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor viewAuth() {
        return authWith("INVENTORY_VENDOR_VIEW");
    }

    private RequestPostProcessor fullAuth() {
        return authWith("INVENTORY_VENDOR_VIEW", "INVENTORY_VENDOR_MANAGE");
    }

    private RequestPostProcessor noPermAuth() {
        return authWith("SOME_OTHER_PERM");
    }

    // ---- GET / (paginated) ----

    @Test
    void getVendors_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.content[?(@.code == 'VND-001')]").exists())
                .andExpect(jsonPath("$.content[?(@.code == 'VND-002')]").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /active ----

    @Test
    void getActiveVendors_returnsOnlyActive() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.code == 'VND-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'VND-002')]").doesNotExist());
    }

    // ---- GET /preferred ----

    @Test
    void getPreferredVendors_returnsOnlyPreferred() throws Exception {
        mockMvc.perform(get(BASE_URL + "/preferred").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.code == 'VND-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'VND-002')]").doesNotExist());
    }

    // ---- GET /{id} ----

    @Test
    void getVendorById_returnsVendor() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + vendor1.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendor1.getId()))
                .andExpect(jsonPath("$.code").value("VND-001"))
                .andExpect(jsonPath("$.name").value("Active Vendor"))
                .andExpect(jsonPath("$.email").value("active@vendor.com"))
                .andExpect(jsonPath("$.phone").value("+998901234567"))
                .andExpect(jsonPath("$.city").value("Tashkent"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.preferred").value(true))
                .andExpect(jsonPath("$.defaultCurrency").value("UZS"))
                .andExpect(jsonPath("$.currentBalance").value(0));
    }

    @Test
    void getVendorById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(viewAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /code/{code} ----

    @Test
    void getVendorByCode_returnsVendor() throws Exception {
        mockMvc.perform(get(BASE_URL + "/code/VND-001").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendor1.getId()))
                .andExpect(jsonPath("$.code").value("VND-001"))
                .andExpect(jsonPath("$.name").value("Active Vendor"));
    }

    // ---- GET /search ----

    @Test
    void searchVendors_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("q", "Active").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].code").value("VND-001"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- POST / ----

    @Test
    void createVendor_validRequest_returnsCreated() throws Exception {
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-NEW").name("New Vendor")
                .contactPerson("Jane Doe")
                .email("new@vendor.com").phone("+998909999999")
                .address("123 Main St").city("Samarkand").country("UZ")
                .paymentTerms("Net 30").paymentTermsDays(30)
                .creditLimit(new BigDecimal("50000"))
                .defaultCurrency("UZS")
                .website("https://newvendor.com")
                .notes("Test vendor")
                .active(true).preferred(false)
                .leadTimeDays(7)
                .minOrderAmount(new BigDecimal("100"))
                .build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("VND-NEW"))
                .andExpect(jsonPath("$.name").value("New Vendor"))
                .andExpect(jsonPath("$.contactPerson").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("new@vendor.com"))
                .andExpect(jsonPath("$.phone").value("+998909999999"))
                .andExpect(jsonPath("$.address").value("123 Main St"))
                .andExpect(jsonPath("$.city").value("Samarkand"))
                .andExpect(jsonPath("$.country").value("UZ"))
                .andExpect(jsonPath("$.paymentTerms").value("Net 30"))
                .andExpect(jsonPath("$.paymentTermsDays").value(30))
                .andExpect(jsonPath("$.creditLimit").value(50000))
                .andExpect(jsonPath("$.defaultCurrency").value("UZS"))
                .andExpect(jsonPath("$.website").value("https://newvendor.com"))
                .andExpect(jsonPath("$.notes").value("Test vendor"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.preferred").value(false))
                .andExpect(jsonPath("$.leadTimeDays").value(7))
                .andExpect(jsonPath("$.minOrderAmount").value(100));
    }

    @Test
    void createVendor_duplicateCode_returnsBadRequest() throws Exception {
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-001").name("Duplicate Code Vendor")
                .active(true).build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /{id} ----

    @Test
    void updateVendor_validRequest_returnsUpdated() throws Exception {
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-001").name("Updated Vendor Name")
                .email("updated@vendor.com").phone("+998901111111")
                .city("Bukhara")
                .active(true).preferred(true)
                .build();

        mockMvc.perform(put(BASE_URL + "/" + vendor1.getId()).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendor1.getId()))
                .andExpect(jsonPath("$.code").value("VND-001"))
                .andExpect(jsonPath("$.name").value("Updated Vendor Name"))
                .andExpect(jsonPath("$.email").value("updated@vendor.com"))
                .andExpect(jsonPath("$.phone").value("+998901111111"))
                .andExpect(jsonPath("$.city").value("Bukhara"))
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---- DELETE /{id} (soft delete) ----

    @Test
    void deleteVendor_softDeletes() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + vendor1.getId()).with(fullAuth()))
                .andExpect(status().isNoContent());

        // Verify vendor still exists but is now inactive
        mockMvc.perform(get(BASE_URL + "/" + vendor1.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendor1.getId()))
                .andExpect(jsonPath("$.active").value(false));
    }

    // ---- PUT /{id}/activate ----

    @Test
    void activateVendor_reactivates() throws Exception {
        // vendor2 is inactive; activate it
        mockMvc.perform(put(BASE_URL + "/" + vendor2.getId() + "/activate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendor2.getId()))
                .andExpect(jsonPath("$.active").value(true));
    }

    // ============ Vendor Contacts ============

    // ---- GET /{vendorId}/contacts ----

    @Test
    void getVendorContacts_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + vendor1.getId() + "/contacts").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].name").value("John Contact"));
    }

    // ---- POST /{vendorId}/contacts ----

    @Test
    void createVendorContact_returnsCreated() throws Exception {
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(vendor1.getId())
                .name("New Contact")
                .title("Sales Manager")
                .department("Sales")
                .email("newcontact@vendor.com")
                .phone("+998905555555")
                .mobilePhone("+998906666666")
                .primary(false)
                .billingContact(false)
                .orderingContact(true)
                .notes("Ordering contact")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + vendor1.getId() + "/contacts").with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Contact"))
                .andExpect(jsonPath("$.data.title").value("Sales Manager"))
                .andExpect(jsonPath("$.data.department").value("Sales"))
                .andExpect(jsonPath("$.data.email").value("newcontact@vendor.com"))
                .andExpect(jsonPath("$.data.phone").value("+998905555555"))
                .andExpect(jsonPath("$.data.mobilePhone").value("+998906666666"))
                .andExpect(jsonPath("$.data.primary").value(false))
                .andExpect(jsonPath("$.data.billingContact").value(false))
                .andExpect(jsonPath("$.data.orderingContact").value(true))
                .andExpect(jsonPath("$.data.notes").value("Ordering contact"))
                .andExpect(jsonPath("$.data.vendorId").value(vendor1.getId()));
    }

    // ---- GET /contacts/{id} ----

    @Test
    void getContactById_returnsContact() throws Exception {
        mockMvc.perform(get(BASE_URL + "/contacts/" + vendorContact.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(vendorContact.getId()))
                .andExpect(jsonPath("$.data.name").value("John Contact"))
                .andExpect(jsonPath("$.data.primary").value(true))
                .andExpect(jsonPath("$.data.billingContact").value(true))
                .andExpect(jsonPath("$.data.vendorId").value(vendor1.getId()));
    }

    // ---- PUT /contacts/{id}/set-primary ----

    @Test
    void setPrimaryContact_setsFlag() throws Exception {
        // Create a second contact that is not primary
        VendorContact secondContact = vendorContactRepository.saveAndFlush(VendorContact.builder()
                .vendor(vendor1)
                .name("Second Contact")
                .primary(false)
                .active(true)
                .build());
        entityManager.clear();

        mockMvc.perform(put(BASE_URL + "/contacts/" + secondContact.getId() + "/set-primary").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(secondContact.getId()))
                .andExpect(jsonPath("$.data.primary").value(true));
    }

    // ---- DELETE /contacts/{id} ----

    @Test
    void deleteContact_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/contacts/" + vendorContact.getId()).with(fullAuth()))
                .andExpect(status().isNoContent());
    }

    // ============ Permission checks ============

    @Test
    void vendorEndpoints_noPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/preferred").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + vendor1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/code/VND-001").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/search").param("q", "Active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreateVendorRequest createRequest = CreateVendorRequest.builder()
                .code("NOPERM").name("No Permission").build();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + vendor1.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + vendor1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + vendor1.getId() + "/activate").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void contactEndpoints_noPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + vendor1.getId() + "/contacts").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + vendor1.getId() + "/contacts/primary").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + vendor1.getId() + "/contacts/billing").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + vendor1.getId() + "/contacts/ordering").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/contacts/" + vendorContact.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreateVendorContactRequest contactRequest = CreateVendorContactRequest.builder()
                .vendorId(vendor1.getId())
                .name("No Perm Contact")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + vendor1.getId() + "/contacts").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contactRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/contacts/" + vendorContact.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contactRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/contacts/" + vendorContact.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/contacts/" + vendorContact.getId() + "/set-primary").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
