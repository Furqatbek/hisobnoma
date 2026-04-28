package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.dto.CreateCategoryRequest;
import com.hisobnoma.platform.inventory.dto.UpdateCategoryRequest;
import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
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
class CategoryControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/inventory/categories";

    private Tenant tenant;
    private User user;
    private Category rootCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Category Test Tenant").code("FULLFLOW_CAT").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("cattestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        rootCategory = categoryRepository.saveAndFlush(Category.builder()
                .code("ROOT-001").name("Root Category").description("Root description")
                .sortOrder(0).active(true).level(0).tenantId(tenant.getId()).build());
        rootCategory.setPath("/" + rootCategory.getId());
        rootCategory = categoryRepository.saveAndFlush(rootCategory);

        childCategory = categoryRepository.saveAndFlush(Category.builder()
                .code("CHILD-001").name("Child Category").description("Child description")
                .parent(rootCategory).sortOrder(1).active(true).level(1)
                .tenantId(tenant.getId()).build());
        childCategory.setPath(rootCategory.getPath() + "/" + childCategory.getId());
        childCategory = categoryRepository.saveAndFlush(childCategory);

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("INVENTORY_PRODUCT_READ"),
                        new SimpleGrantedAuthority("INVENTORY_PRODUCT_CREATE"),
                        new SimpleGrantedAuthority("INVENTORY_PRODUCT_UPDATE"),
                        new SimpleGrantedAuthority("INVENTORY_PRODUCT_DELETE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("SOME_OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET / ----

    @Test
    void getAllCategories_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[?(@.code == 'ROOT-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'CHILD-001')]").exists());
    }

    // ---- GET /tree ----

    @Test
    void getCategoryTree_returnsHierarchy() throws Exception {
        mockMvc.perform(get(BASE_URL + "/tree").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.code == 'ROOT-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'ROOT-001')].children").isNotEmpty())
                .andExpect(jsonPath("$[?(@.code == 'ROOT-001')].children[0].code").value("CHILD-001"));
    }

    // ---- GET /roots ----

    @Test
    void getRootCategories_returnsOnlyRoots() throws Exception {
        mockMvc.perform(get(BASE_URL + "/roots").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.code == 'ROOT-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'CHILD-001')]").doesNotExist());
    }

    // ---- GET /{id} ----

    @Test
    void getCategoryById_returnsCategory() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + rootCategory.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rootCategory.getId()))
                .andExpect(jsonPath("$.code").value("ROOT-001"))
                .andExpect(jsonPath("$.name").value("Root Category"))
                .andExpect(jsonPath("$.description").value("Root description"))
                .andExpect(jsonPath("$.sortOrder").value(0))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.level").value(0));
    }

    @Test
    void getCategoryById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /{id}/children ----

    @Test
    void getChildCategories_returnsChildren() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + rootCategory.getId() + "/children").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("CHILD-001"))
                .andExpect(jsonPath("$[0].name").value("Child Category"));
    }

    // ---- GET /search ----

    @Test
    void searchCategories_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("q", "Root").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].code").value("ROOT-001"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- POST / ----

    @Test
    void createCategory_validRequest_returnsCreated() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .code("NEW-001").name("New Category")
                .description("A new category").sortOrder(5).active(true).build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("NEW-001"))
                .andExpect(jsonPath("$.name").value("New Category"))
                .andExpect(jsonPath("$.description").value("A new category"))
                .andExpect(jsonPath("$.sortOrder").value(5))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.level").value(0))
                .andExpect(jsonPath("$.path").isNotEmpty());
    }

    @Test
    void createCategory_duplicateCode_returnsConflict() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .code("ROOT-001").name("Duplicate Code Category")
                .active(true).build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createCategory_withParent_setsParentRelation() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .code("CHILD-002").name("Second Child")
                .parentId(rootCategory.getId()).sortOrder(2).active(true).build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CHILD-002"))
                .andExpect(jsonPath("$.name").value("Second Child"))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.path", containsString("/" + rootCategory.getId() + "/")));
    }

    // ---- PUT /{id} ----

    @Test
    void updateCategory_validRequest_returnsUpdated() throws Exception {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Updated Root Category")
                .description("Updated description")
                .sortOrder(10)
                .active(false).build();

        mockMvc.perform(put(BASE_URL + "/" + rootCategory.getId()).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rootCategory.getId()))
                .andExpect(jsonPath("$.code").value("ROOT-001"))
                .andExpect(jsonPath("$.name").value("Updated Root Category"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.sortOrder").value(10))
                .andExpect(jsonPath("$.active").value(false));
    }

    // ---- DELETE /{id} ----

    @Test
    void deleteCategory_noChildren_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + childCategory.getId()).with(fullAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategory_withChildren_returnsBadRequest() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + rootCategory.getId()).with(fullAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/tree").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/roots").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + rootCategory.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + rootCategory.getId() + "/children").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/search").param("q", "Root").with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreateCategoryRequest createReq = CreateCategoryRequest.builder()
                .code("NOPE").name("No Permission").build();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        UpdateCategoryRequest updateReq = UpdateCategoryRequest.builder()
                .name("No Permission").build();

        mockMvc.perform(put(BASE_URL + "/" + rootCategory.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + rootCategory.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
