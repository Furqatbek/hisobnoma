package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.dto.CreateBrandRequest;
import com.hisobnoma.platform.inventory.dto.UpdateBrandRequest;
import com.hisobnoma.platform.inventory.entity.Brand;
import com.hisobnoma.platform.inventory.repository.BrandRepository;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BrandControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BrandRepository brandRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/inventory/brands";

    private Tenant tenant;
    private User user;
    private Brand brand1;
    private Brand brand2;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Brand Test Tenant").code("FULLFLOW_BRD").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("brandtestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        brand1 = brandRepository.saveAndFlush(Brand.builder()
                .code("BRD-001").name("Alpha Brand")
                .description("First test brand").logoUrl("https://example.com/alpha.png")
                .website("https://alpha.example.com").sortOrder(1)
                .active(true).tenantId(tenant.getId()).build());

        brand2 = brandRepository.saveAndFlush(Brand.builder()
                .code("BRD-002").name("Beta Brand")
                .description("Second test brand").logoUrl("https://example.com/beta.png")
                .website("https://beta.example.com").sortOrder(2)
                .active(false).tenantId(tenant.getId()).build());

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

    private RequestPostProcessor fullAuth() {
        return authWith(
                "INVENTORY_PRODUCT_READ",
                "INVENTORY_PRODUCT_CREATE",
                "INVENTORY_PRODUCT_UPDATE",
                "INVENTORY_PRODUCT_DELETE");
    }

    private RequestPostProcessor noPermAuth() {
        return authWith("SOME_OTHER_PERM");
    }

    // ---- GET / ----

    @Test
    void getAllBrands_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[?(@.code == 'BRD-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'BRD-002')]").exists());
    }

    // ---- GET /paginated ----

    @Test
    void getBrandsPaginated_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/paginated").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /active ----

    @Test
    void getActiveBrands_returnsOnlyActive() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.code == 'BRD-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'BRD-002')]").doesNotExist());
    }

    // ---- GET /{id} ----

    @Test
    void getBrandById_returnsBrand() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + brand1.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(brand1.getId()))
                .andExpect(jsonPath("$.code").value("BRD-001"))
                .andExpect(jsonPath("$.name").value("Alpha Brand"))
                .andExpect(jsonPath("$.description").value("First test brand"))
                .andExpect(jsonPath("$.logoUrl").value("https://example.com/alpha.png"))
                .andExpect(jsonPath("$.website").value("https://alpha.example.com"))
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getBrandById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /search ----

    @Test
    void searchBrands_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("q", "Alpha").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].code").value("BRD-001"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- POST / ----

    @Test
    void createBrand_validRequest_returnsCreated() throws Exception {
        CreateBrandRequest request = CreateBrandRequest.builder()
                .code("BRD-NEW").name("New Brand")
                .description("A brand new brand")
                .logoUrl("https://example.com/new.png")
                .website("https://new.example.com")
                .sortOrder(5)
                .active(true).build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BRD-NEW"))
                .andExpect(jsonPath("$.name").value("New Brand"))
                .andExpect(jsonPath("$.description").value("A brand new brand"))
                .andExpect(jsonPath("$.logoUrl").value("https://example.com/new.png"))
                .andExpect(jsonPath("$.website").value("https://new.example.com"))
                .andExpect(jsonPath("$.sortOrder").value(5))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createBrand_duplicateCode_returnsBadRequest() throws Exception {
        CreateBrandRequest request = CreateBrandRequest.builder()
                .code("BRD-001").name("Duplicate Brand")
                .active(true).build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ---- PUT /{id} ----

    @Test
    void updateBrand_validRequest_returnsUpdated() throws Exception {
        UpdateBrandRequest request = UpdateBrandRequest.builder()
                .name("Updated Alpha Brand")
                .description("Updated description")
                .website("https://updated.example.com")
                .sortOrder(10)
                .build();

        mockMvc.perform(put(BASE_URL + "/" + brand1.getId()).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(brand1.getId()))
                .andExpect(jsonPath("$.code").value("BRD-001"))
                .andExpect(jsonPath("$.name").value("Updated Alpha Brand"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.website").value("https://updated.example.com"))
                .andExpect(jsonPath("$.sortOrder").value(10))
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---- DELETE /{id} ----

    @Test
    void deleteBrand_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + brand2.getId()).with(fullAuth()))
                .andExpect(status().isNoContent());

        // Verify it is actually deleted
        mockMvc.perform(get(BASE_URL + "/" + brand2.getId()).with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/paginated").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + brand1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/search").param("q", "Alpha").with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreateBrandRequest createRequest = CreateBrandRequest.builder()
                .code("NOPE").name("No Permission").build();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + brand1.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + brand1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
