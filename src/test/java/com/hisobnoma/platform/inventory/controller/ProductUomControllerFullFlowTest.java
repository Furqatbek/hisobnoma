package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.dto.CreateProductUomRequest;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductUom;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductUomRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
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
class ProductUomControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductUomRepository productUomRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private UnitOfMeasure baseUom;
    private UnitOfMeasure boxUom;
    private UnitOfMeasure inactiveUom;
    private Product product;
    private ProductUom productUom1;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("ProductUom Test Tenant").code("FULLFLOW_PUOM").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("puomtestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        baseUom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS").name("Pieces").symbol("pcs")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).tenantId(tenant.getId()).build());

        boxUom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("BOX").name("Box").symbol("box")
                .isBaseUnit(false).baseUom(baseUom)
                .conversionFactor(new BigDecimal("12"))
                .active(true).tenantId(tenant.getId()).build());

        inactiveUom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("OLD").name("Old Unit")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(false).tenantId(tenant.getId()).build());

        product = productRepository.saveAndFlush(Product.builder()
                .sku("PUOM-001").name("Test Product")
                .baseUom(baseUom)
                .sellingPrice(new BigDecimal("10000"))
                .active(true)
                .tenantId(tenant.getId()).build());

        productUom1 = productUomRepository.saveAndFlush(ProductUom.builder()
                .product(product)
                .uom(boxUom)
                .conversionFactor(new BigDecimal("12"))
                .defaultSale(false)
                .active(true)
                .sortOrder(0)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private String baseUrl() {
        return "/api/v1/inventory/products/" + product.getId() + "/uoms";
    }

    private RequestPostProcessor authWith(String... permissions) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(permissions)
                .map(SimpleGrantedAuthority::new)
                .toList();
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, authorities);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor readAuth() {
        return authWith("INVENTORY_PRODUCT_READ", "INVENTORY_PRODUCT_UPDATE", "INVENTORY_PRODUCT_DELETE");
    }

    private RequestPostProcessor noPermAuth() {
        return authWith("SOME_OTHER_PERM");
    }

    // ---- GET / ----

    @Test
    void getProductUoms_returnsList() throws Exception {
        mockMvc.perform(get(baseUrl()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].id").value(productUom1.getId()))
                .andExpect(jsonPath("$[0].productId").value(product.getId()))
                .andExpect(jsonPath("$[0].productName").value("Test Product"))
                .andExpect(jsonPath("$[0].uomId").value(boxUom.getId()))
                .andExpect(jsonPath("$[0].uomCode").value("BOX"))
                .andExpect(jsonPath("$[0].uomName").value("Box"))
                .andExpect(jsonPath("$[0].conversionFactor").value(12))
                .andExpect(jsonPath("$[0].defaultSale").value(false))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].effectiveSellingPrice").isNumber());
    }

    // ---- GET /active ----

    @Test
    void getActiveProductUoms_returnsOnlyActive() throws Exception {
        // Add an inactive product UOM
        productUomRepository.saveAndFlush(ProductUom.builder()
                .product(product)
                .uom(inactiveUom)
                .conversionFactor(BigDecimal.ONE)
                .defaultSale(false)
                .active(false)
                .sortOrder(1)
                .tenantId(tenant.getId()).build());
        entityManager.clear();

        mockMvc.perform(get(baseUrl() + "/active").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].uomCode").value("BOX"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    // ---- POST / ----

    @Test
    void createProductUom_validRequest_returnsCreated() throws Exception {
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(inactiveUom.getId())
                .conversionFactor(new BigDecimal("1"))
                .defaultSale(false)
                .active(true)
                .sortOrder(1)
                .build();

        mockMvc.perform(post(baseUrl()).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.productId").value(product.getId()))
                .andExpect(jsonPath("$.productName").value("Test Product"))
                .andExpect(jsonPath("$.uomId").value(inactiveUom.getId()))
                .andExpect(jsonPath("$.uomCode").value("OLD"))
                .andExpect(jsonPath("$.uomName").value("Old Unit"))
                .andExpect(jsonPath("$.conversionFactor").value(1))
                .andExpect(jsonPath("$.defaultSale").value(false))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.sellingPrice").doesNotExist())
                .andExpect(jsonPath("$.effectiveSellingPrice").value(10000));
    }

    @Test
    void createProductUom_withSellingPrice_returnsCreated() throws Exception {
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(inactiveUom.getId())
                .conversionFactor(new BigDecimal("1"))
                .sellingPrice(new BigDecimal("15000"))
                .defaultSale(true)
                .active(true)
                .sortOrder(2)
                .build();

        mockMvc.perform(post(baseUrl()).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.uomId").value(inactiveUom.getId()))
                .andExpect(jsonPath("$.conversionFactor").value(1))
                .andExpect(jsonPath("$.sellingPrice").value(15000))
                .andExpect(jsonPath("$.effectiveSellingPrice").value(15000))
                .andExpect(jsonPath("$.defaultSale").value(true))
                .andExpect(jsonPath("$.sortOrder").value(2));
    }

    // ---- PUT /{id} ----

    @Test
    void updateProductUom_validRequest_returnsUpdated() throws Exception {
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(boxUom.getId())
                .conversionFactor(new BigDecimal("24"))
                .sellingPrice(new BigDecimal("200000"))
                .defaultSale(true)
                .active(true)
                .sortOrder(5)
                .build();

        mockMvc.perform(put(baseUrl() + "/" + productUom1.getId()).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productUom1.getId()))
                .andExpect(jsonPath("$.productId").value(product.getId()))
                .andExpect(jsonPath("$.uomId").value(boxUom.getId()))
                .andExpect(jsonPath("$.uomCode").value("BOX"))
                .andExpect(jsonPath("$.conversionFactor").value(24))
                .andExpect(jsonPath("$.sellingPrice").value(200000))
                .andExpect(jsonPath("$.effectiveSellingPrice").value(200000))
                .andExpect(jsonPath("$.defaultSale").value(true))
                .andExpect(jsonPath("$.sortOrder").value(5));
    }

    // ---- DELETE /{id} ----

    @Test
    void deleteProductUom_returns204() throws Exception {
        mockMvc.perform(delete(baseUrl() + "/" + productUom1.getId()).with(readAuth()))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get(baseUrl()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(baseUrl()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(baseUrl() + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(inactiveUom.getId())
                .conversionFactor(BigDecimal.ONE)
                .build();

        mockMvc.perform(post(baseUrl()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(baseUrl() + "/" + productUom1.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(baseUrl() + "/" + productUom1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    // ---- Full lifecycle ----

    @Test
    void fullLifecycle_createUpdateDelete() throws Exception {
        // 1. Create a new product UOM
        CreateProductUomRequest createRequest = CreateProductUomRequest.builder()
                .uomId(inactiveUom.getId())
                .conversionFactor(new BigDecimal("6"))
                .defaultSale(false)
                .active(true)
                .sortOrder(10)
                .build();

        String createResponse = mockMvc.perform(post(baseUrl()).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uomId").value(inactiveUom.getId()))
                .andExpect(jsonPath("$.conversionFactor").value(6))
                .andExpect(jsonPath("$.sellingPrice").doesNotExist())
                .andExpect(jsonPath("$.effectiveSellingPrice").value(60000))
                .andExpect(jsonPath("$.defaultSale").value(false))
                .andExpect(jsonPath("$.sortOrder").value(10))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(createResponse).get("id").asLong();

        // 2. Verify it appears in the list
        mockMvc.perform(get(baseUrl()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == " + createdId + ")]").exists());

        // 3. Update the product UOM
        CreateProductUomRequest updateRequest = CreateProductUomRequest.builder()
                .uomId(inactiveUom.getId())
                .conversionFactor(new BigDecimal("8"))
                .sellingPrice(new BigDecimal("75000"))
                .defaultSale(true)
                .active(true)
                .sortOrder(20)
                .build();

        mockMvc.perform(put(baseUrl() + "/" + createdId).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.conversionFactor").value(8))
                .andExpect(jsonPath("$.sellingPrice").value(75000))
                .andExpect(jsonPath("$.effectiveSellingPrice").value(75000))
                .andExpect(jsonPath("$.defaultSale").value(true))
                .andExpect(jsonPath("$.sortOrder").value(20));

        // 4. Delete the product UOM
        mockMvc.perform(delete(baseUrl() + "/" + createdId).with(readAuth()))
                .andExpect(status().isNoContent());

        // 5. Verify it no longer appears in the list
        mockMvc.perform(get(baseUrl()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + createdId + ")]").doesNotExist());
    }
}
