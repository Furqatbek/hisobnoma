package com.hisobnoma.platform.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import com.hisobnoma.platform.web.dto.AddCatalogItemsRequest;
import com.hisobnoma.platform.web.dto.UpdateCatalogItemRequest;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebCatalogFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private WebCatalogItemRepository catalogRepository;

    private static final String PUBLIC_URL = "/api/v1/web/catalog";
    private static final String ADMIN_URL = "/api/v1/web-catalog";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    private Tenant tenant;
    private User adminUser;
    private Category category;
    private Product liveProduct;
    private Product draftProduct;
    private Product unlistedProduct;
    private WebCatalogItem liveItem;
    private WebCatalogItem draftItem;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Web Catalog Tenant").code("FULLFLOW_WEBCAT").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("webcatalogadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("WCPCS").name("Pieces").active(true).isBaseUnit(true)
                .tenantId(tenant.getId()).build());

        category = categoryRepository.saveAndFlush(Category.builder()
                .code("WCCAT").name("Drinks").active(true)
                .tenantId(tenant.getId()).build());

        liveProduct = productRepository.saveAndFlush(Product.builder()
                .sku("WC-SKU-001").name("Cola Bottle")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("12000.0000"))
                .costPrice(new BigDecimal("8000.0000"))
                .active(true).sellable(true).trackInventory(false)
                .tenantId(tenant.getId()).build());

        draftProduct = productRepository.saveAndFlush(Product.builder()
                .sku("WC-SKU-002").name("Orange Juice")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("8000.0000"))
                .active(true).sellable(true).trackInventory(false)
                .tenantId(tenant.getId()).build());

        unlistedProduct = productRepository.saveAndFlush(Product.builder()
                .sku("WC-SKU-003").name("Mineral Water")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("3000.0000"))
                .active(true).sellable(true).trackInventory(false)
                .tenantId(tenant.getId()).build());

        liveItem = catalogRepository.saveAndFlush(WebCatalogItem.builder()
                .product(liveProduct).status(WebCatalogStatus.LIVE).sortOrder(1)
                .tenantId(tenant.getId()).build());

        draftItem = catalogRepository.saveAndFlush(WebCatalogItem.builder()
                .product(draftProduct).status(WebCatalogStatus.DRAFT).sortOrder(2)
                .tenantId(tenant.getId()).build());
    }

    // ---- Auth helpers ----

    private RequestPostProcessor manageAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("WEB_CATALOG_VIEW"),
                        new SimpleGrantedAuthority("WEB_CATALOG_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor viewOnlyAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("WEB_CATALOG_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- Public catalog endpoints ----

    @Test
    void publicProducts_anonymousAccessReturnsOnlyLiveItems() throws Exception {
        mockMvc.perform(get(PUBLIC_URL + "/products")
                        .header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Cola Bottle")))
                .andExpect(jsonPath("$.content[0].price", closeTo(12000.0, 0.01)))
                .andExpect(jsonPath("$.content[0].currency", is("UZS")))
                .andExpect(jsonPath("$.content[0].inStock", is(true)));
    }

    @Test
    void publicProducts_neverExposeCostPriceOrStockNumbers() throws Exception {
        mockMvc.perform(get(PUBLIC_URL + "/products")
                        .header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].costPrice").doesNotExist())
                .andExpect(jsonPath("$.content[0].minSellingPrice").doesNotExist())
                .andExpect(jsonPath("$.content[0].wholesalePrice").doesNotExist())
                .andExpect(jsonPath("$.content[0].quantityOnHand").doesNotExist());
    }

    @Test
    void publicProducts_searchFiltersByName() throws Exception {
        mockMvc.perform(get(PUBLIC_URL + "/products")
                        .header(TENANT_HEADER, tenant.getId())
                        .param("search", "cola"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(get(PUBLIC_URL + "/products")
                        .header(TENANT_HEADER, tenant.getId())
                        .param("search", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void publicProductDetail_liveReturns200_draftReturns404() throws Exception {
        mockMvc.perform(get(PUBLIC_URL + "/products/" + liveItem.getId())
                        .header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Cola Bottle")));

        mockMvc.perform(get(PUBLIC_URL + "/products/" + draftItem.getId())
                        .header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicCategories_returnsCategoriesOfLiveItems() throws Exception {
        mockMvc.perform(get(PUBLIC_URL + "/categories")
                        .header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Drinks")));
    }

    @Test
    void publicProducts_otherTenantSeesNothing() throws Exception {
        mockMvc.perform(get(PUBLIC_URL + "/products")
                        .header(TENANT_HEADER, tenant.getId() + 999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ---- Staff admin endpoints: security ----

    @Test
    void adminEndpoints_anonymousIsRejected() throws Exception {
        mockMvc.perform(get(ADMIN_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminMutations_viewOnlyPermissionGets403() throws Exception {
        AddCatalogItemsRequest request = new AddCatalogItemsRequest(List.of(unlistedProduct.getId()));

        mockMvc.perform(post(ADMIN_URL + "/items")
                        .with(viewOnlyAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminList_viewPermissionSeesAllItemsIncludingDrafts() throws Exception {
        mockMvc.perform(get(ADMIN_URL).with(viewOnlyAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    // ---- Staff admin endpoints: catalog management flow ----

    @Test
    void addProducts_createsDraftItem() throws Exception {
        AddCatalogItemsRequest request = new AddCatalogItemsRequest(List.of(unlistedProduct.getId()));

        mockMvc.perform(post(ADMIN_URL + "/items")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status", is("DRAFT")))
                .andExpect(jsonPath("$.data[0].productName", is("Mineral Water")));

        assertEquals(3, catalogRepository.findByTenantIdOrderBySortOrderAsc(tenant.getId()).size());
    }

    @Test
    void publishDraft_makesItemVisibleInPublicCatalog() throws Exception {
        mockMvc.perform(post(ADMIN_URL + "/items/" + draftItem.getId() + "/publish")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("LIVE")));

        mockMvc.perform(get(PUBLIC_URL + "/products")
                        .header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void unpublishLive_hidesItemFromPublicCatalog() throws Exception {
        mockMvc.perform(post(ADMIN_URL + "/items/" + liveItem.getId() + "/unpublish")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DRAFT")));

        mockMvc.perform(get(PUBLIC_URL + "/products")
                        .header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void updateOverrides_changePublicNameAndPrice() throws Exception {
        UpdateCatalogItemRequest request = UpdateCatalogItemRequest.builder()
                .displayName("Cola Special Offer")
                .priceOverride(new BigDecimal("9999.50"))
                .build();

        mockMvc.perform(put(ADMIN_URL + "/items/" + liveItem.getId())
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectivePrice", closeTo(9999.5, 0.01)));

        mockMvc.perform(get(PUBLIC_URL + "/products/" + liveItem.getId())
                        .header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Cola Special Offer")))
                .andExpect(jsonPath("$.data.price", closeTo(9999.5, 0.01)));
    }

    @Test
    void moveDown_swapsOrderInAdminList() throws Exception {
        mockMvc.perform(post(ADMIN_URL + "/items/" + liveItem.getId() + "/move-down")
                        .with(manageAuth()))
                .andExpect(status().isOk());

        mockMvc.perform(get(ADMIN_URL).with(viewOnlyAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productName", is("Orange Juice")))
                .andExpect(jsonPath("$.content[1].productName", is("Cola Bottle")));
    }

    @Test
    void publishAllAndUnpublishAll_toggleWholeList() throws Exception {
        mockMvc.perform(post(ADMIN_URL + "/publish-all").with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(1)));

        mockMvc.perform(get(PUBLIC_URL + "/products")
                        .header(TENANT_HEADER, tenant.getId()))
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(post(ADMIN_URL + "/unpublish-all").with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(2)));

        mockMvc.perform(get(PUBLIC_URL + "/products")
                        .header(TENANT_HEADER, tenant.getId()))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void deleteItem_removesFromCatalog() throws Exception {
        mockMvc.perform(delete(ADMIN_URL + "/items/" + draftItem.getId())
                        .with(manageAuth()))
                .andExpect(status().isOk());

        assertEquals(1, catalogRepository.findByTenantIdOrderBySortOrderAsc(tenant.getId()).size());
    }

    @Test
    void counts_reflectLiveAndDraftItems() throws Exception {
        mockMvc.perform(get(ADMIN_URL + "/counts").with(viewOnlyAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.live", is(1)))
                .andExpect(jsonPath("$.data.draft", is(1)))
                .andExpect(jsonPath("$.data.total", is(2)));
    }

    @Test
    void addProducts_duplicateProductIsSkipped() throws Exception {
        AddCatalogItemsRequest request = new AddCatalogItemsRequest(List.of(liveProduct.getId()));

        mockMvc.perform(post(ADMIN_URL + "/items")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data", hasSize(0)));

        assertEquals(2, catalogRepository.findByTenantIdOrderBySortOrderAsc(tenant.getId()).size());
    }
}
