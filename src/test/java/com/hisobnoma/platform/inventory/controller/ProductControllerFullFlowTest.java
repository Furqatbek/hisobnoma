package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.dto.CreateProductRequest;
import com.hisobnoma.platform.inventory.dto.CreateProductVendorRequest;
import com.hisobnoma.platform.inventory.dto.CreateVariantRequest;
import com.hisobnoma.platform.inventory.dto.UpdateProductRequest;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.repository.*;
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
class ProductControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private ProductVendorRepository productVendorRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/inventory/products";

    private Tenant tenant;
    private User user;
    private Category category;
    private Brand brand;
    private UnitOfMeasure uom;
    private Vendor vendor;
    private Product product1;
    private Product product2;
    private ProductVariant variant1;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Product Test Tenant").code("FULLFLOW_PRD").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("producttestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-001").name("Electronics")
                .active(true).sortOrder(0).level(0)
                .tenantId(tenant.getId()).build());

        brand = brandRepository.saveAndFlush(Brand.builder()
                .code("BRD-001").name("Samsung")
                .active(true).sortOrder(0)
                .tenantId(tenant.getId()).build());

        uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS").name("Pieces")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE).active(true)
                .tenantId(tenant.getId()).build());

        vendor = vendorRepository.saveAndFlush(Vendor.builder()
                .code("VND-001").name("Test Vendor")
                .active(true)
                .tenantId(tenant.getId()).build());

        product1 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-001").barcode("12345678").name("Test Product")
                .category(category).brand(brand).baseUom(uom)
                .sellingPrice(new BigDecimal("50000")).costPrice(new BigDecimal("30000"))
                .active(true)
                .tenantId(tenant.getId()).build());

        product2 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-002").name("Inactive Product")
                .baseUom(uom)
                .sellingPrice(new BigDecimal("25000"))
                .active(false)
                .tenantId(tenant.getId()).build());

        variant1 = productVariantRepository.saveAndFlush(ProductVariant.builder()
                .product(product1)
                .sku("SKU-001-RED").name("Red Variant")
                .option1Name("Color").option1Value("Red")
                .sortOrder(0).active(true)
                .tenantId(tenant.getId()).build());

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

    private RequestPostProcessor readAuth() {
        return authWith("INVENTORY_PRODUCT_READ");
    }

    private RequestPostProcessor fullAuth() {
        return authWith("INVENTORY_PRODUCT_READ", "INVENTORY_PRODUCT_CREATE",
                "INVENTORY_PRODUCT_UPDATE", "INVENTORY_PRODUCT_DELETE");
    }

    private RequestPostProcessor noPermAuth() {
        return authWith("SOME_OTHER_PERM");
    }

    // ==================== Product CRUD ====================

    // 1. GET / — paginated list
    @Test
    void getProducts_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.content[?(@.sku == 'SKU-001')]").exists())
                .andExpect(jsonPath("$.content[?(@.sku == 'SKU-002')]").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(2)));
    }

    // 2. GET /active — only active products
    @Test
    void getActiveProducts_returnsOnlyActive() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.sku == 'SKU-001')]").exists())
                .andExpect(jsonPath("$.content[?(@.sku == 'SKU-002')]").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // 3. GET /{id} — returns product with details
    @Test
    void getProductById_returnsProduct() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + product1.getId()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product1.getId()))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.barcode").value("12345678"))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.categoryId").value(category.getId()))
                .andExpect(jsonPath("$.categoryName").value("Electronics"))
                .andExpect(jsonPath("$.brandId").value(brand.getId()))
                .andExpect(jsonPath("$.brandName").value("Samsung"))
                .andExpect(jsonPath("$.baseUomId").value(uom.getId()))
                .andExpect(jsonPath("$.baseUomCode").value("PCS"))
                .andExpect(jsonPath("$.baseUomName").value("Pieces"))
                .andExpect(jsonPath("$.sellingPrice").value(50000))
                .andExpect(jsonPath("$.costPrice").value(30000))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.trackInventory").value(true));
    }

    // 4. GET /{id} — not found
    @Test
    void getProductById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(readAuth()))
                .andExpect(status().isNotFound());
    }

    // 5. GET /sku/{sku}
    @Test
    void getProductBySku_returnsProduct() throws Exception {
        mockMvc.perform(get(BASE_URL + "/sku/SKU-001").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product1.getId()))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    // 6. GET /barcode/{barcode}
    @Test
    void getProductByBarcode_returnsProduct() throws Exception {
        mockMvc.perform(get(BASE_URL + "/barcode/12345678").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product1.getId()))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.barcode").value("12345678"))
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    // 7. GET /search?q={query}
    @Test
    void searchProducts_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("q", "Test").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.sku == 'SKU-001')]").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // 8. POST / — create product
    @Test
    void createProduct_validRequest_returnsCreated() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-NEW")
                .barcode("NEW-BARCODE-001")
                .name("New Product")
                .description("A new product description")
                .baseUomId(uom.getId())
                .categoryId(category.getId())
                .brandId(brand.getId())
                .sellingPrice(new BigDecimal("100"))
                .costPrice(new BigDecimal("60"))
                .build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-NEW"))
                .andExpect(jsonPath("$.barcode").value("NEW-BARCODE-001"))
                .andExpect(jsonPath("$.name").value("New Product"))
                .andExpect(jsonPath("$.description").value("A new product description"))
                .andExpect(jsonPath("$.categoryId").value(category.getId()))
                .andExpect(jsonPath("$.categoryName").value("Electronics"))
                .andExpect(jsonPath("$.brandId").value(brand.getId()))
                .andExpect(jsonPath("$.brandName").value("Samsung"))
                .andExpect(jsonPath("$.baseUomId").value(uom.getId()))
                .andExpect(jsonPath("$.baseUomCode").value("PCS"))
                .andExpect(jsonPath("$.sellingPrice").value(100))
                .andExpect(jsonPath("$.costPrice").value(60))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.trackInventory").value(true));
    }

    // 9. POST / — missing required fields
    @Test
    void createProduct_missingRequired_returns400() throws Exception {
        // Missing name, baseUomId, and sellingPrice
        CreateProductRequest request = CreateProductRequest.builder().build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 10. PUT /{id} — update product
    @Test
    void updateProduct_validRequest_returnsUpdated() throws Exception {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Updated Product Name")
                .description("Updated description")
                .sellingPrice(new BigDecimal("75000"))
                .costPrice(new BigDecimal("45000"))
                .build();

        mockMvc.perform(put(BASE_URL + "/" + product1.getId()).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product1.getId()))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Updated Product Name"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.sellingPrice").value(75000))
                .andExpect(jsonPath("$.costPrice").value(45000));
    }

    // 11. DELETE /{id} — soft delete returns 204
    @Test
    void deleteProduct_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + product1.getId()).with(fullAuth()))
                .andExpect(status().isNoContent());

        // Verify product is soft-deleted (still exists but inactive)
        mockMvc.perform(get(BASE_URL + "/" + product1.getId()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product1.getId()))
                .andExpect(jsonPath("$.active").value(false));
    }

    // 12. GET /count — total and active counts
    @Test
    void getProductCount_returnsTotalAndActive() throws Exception {
        mockMvc.perform(get(BASE_URL + "/count").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.active").value(greaterThanOrEqualTo(1)));
    }

    // 13. GET /category/{categoryId} — filtered by category
    @Test
    void getProductsByCategory_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/category/" + category.getId()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.sku == 'SKU-001')]").exists())
                .andExpect(jsonPath("$.content[?(@.sku == 'SKU-002')]").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // 14. GET /brand/{brandId} — filtered by brand
    @Test
    void getProductsByBrand_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/brand/" + brand.getId()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.sku == 'SKU-001')]").exists())
                .andExpect(jsonPath("$.content[?(@.sku == 'SKU-002')]").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ==================== SKU & Barcode ====================

    // 15. GET /generate-sku?prefix=X
    @Test
    void generateSku_returnsGeneratedSku() throws Exception {
        mockMvc.perform(get(BASE_URL + "/generate-sku").param("prefix", "TEST").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").isString())
                .andExpect(jsonPath("$.sku", startsWith("TEST-")));
    }

    // 16. GET /generate-sku-from-name?name=X
    @Test
    void generateSkuFromName_returnsSku() throws Exception {
        mockMvc.perform(get(BASE_URL + "/generate-sku-from-name").param("name", "Laptop Computer").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").isString())
                .andExpect(jsonPath("$.sku").isNotEmpty());
    }

    // 17. GET /validate-sku/{sku} — existing SKU
    @Test
    void validateSku_existingSku_returnsNotAvailable() throws Exception {
        mockMvc.perform(get(BASE_URL + "/validate-sku/SKU-001").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.available").value(false));
    }

    // 18. GET /generate-barcode
    @Test
    void generateBarcode_returnsBarcode() throws Exception {
        mockMvc.perform(get(BASE_URL + "/generate-barcode").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").isString())
                .andExpect(jsonPath("$.barcode").isNotEmpty());
    }

    // 19. GET /validate-barcode/{barcode}
    @Test
    void validateBarcode_returnsValidation() throws Exception {
        mockMvc.perform(get(BASE_URL + "/validate-barcode/12345678").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.type").isString());
    }

    // ==================== Variants ====================

    // 20. GET /{id}/variants — returns list
    @Test
    void getProductVariants_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + product1.getId() + "/variants").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].sku").value("SKU-001-RED"))
                .andExpect(jsonPath("$[0].name").value("Red Variant"))
                .andExpect(jsonPath("$[0].option1Name").value("Color"))
                .andExpect(jsonPath("$[0].option1Value").value("Red"))
                .andExpect(jsonPath("$[0].productId").value(product1.getId()));
    }

    // 21. POST /{id}/variants — create variant
    @Test
    void createVariant_returnsCreated() throws Exception {
        CreateVariantRequest request = CreateVariantRequest.builder()
                .sku("SKU-001-BLU")
                .name("Blue Variant")
                .option1Name("Color")
                .option1Value("Blue")
                .sellingPrice(new BigDecimal("55000"))
                .active(true)
                .build();

        mockMvc.perform(post(BASE_URL + "/" + product1.getId() + "/variants").with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-001-BLU"))
                .andExpect(jsonPath("$.name").value("Blue Variant"))
                .andExpect(jsonPath("$.option1Name").value("Color"))
                .andExpect(jsonPath("$.option1Value").value("Blue"))
                .andExpect(jsonPath("$.sellingPrice").value(55000))
                .andExpect(jsonPath("$.productId").value(product1.getId()))
                .andExpect(jsonPath("$.active").value(true));
    }

    // 22. DELETE /{id}/variants/{variantId} — delete variant
    @Test
    void deleteVariant_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + product1.getId() + "/variants/" + variant1.getId())
                        .with(fullAuth()))
                .andExpect(status().isNoContent());

        // Verify variant is removed
        mockMvc.perform(get(BASE_URL + "/" + product1.getId() + "/variants").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.sku == 'SKU-001-RED')]").doesNotExist());
    }

    // ==================== Vendor Linkage ====================

    // 23. GET /{id}/vendors — returns list (initially empty)
    @Test
    void getProductVendors_returnsList() throws Exception {
        // First, add a vendor to the product
        ProductVendor pv = productVendorRepository.saveAndFlush(ProductVendor.builder()
                .product(product1).vendor(vendor)
                .vendorSku("V-SKU-001").vendorProductName("Vendor Product Name")
                .unitCost(new BigDecimal("28000"))
                .preferred(true).active(true)
                .tenantId(tenant.getId()).build());
        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/" + product1.getId() + "/vendors").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].vendorId").value(vendor.getId()))
                .andExpect(jsonPath("$[0].vendorName").value("Test Vendor"))
                .andExpect(jsonPath("$[0].vendorSku").value("V-SKU-001"))
                .andExpect(jsonPath("$[0].productId").value(product1.getId()))
                .andExpect(jsonPath("$[0].preferred").value(true))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    // 24. POST /{id}/vendors — add vendor
    @Test
    void addProductVendor_returnsCreated() throws Exception {
        CreateProductVendorRequest request = CreateProductVendorRequest.builder()
                .vendorId(vendor.getId())
                .vendorSku("V-SKU-NEW")
                .vendorProductName("Vendor Test Product")
                .unitCost(new BigDecimal("29000"))
                .leadTimeDays(7)
                .preferred(true)
                .active(true)
                .notes("Test vendor link")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + product1.getId() + "/vendors").with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendorId").value(vendor.getId()))
                .andExpect(jsonPath("$.vendorName").value("Test Vendor"))
                .andExpect(jsonPath("$.vendorSku").value("V-SKU-NEW"))
                .andExpect(jsonPath("$.vendorProductName").value("Vendor Test Product"))
                .andExpect(jsonPath("$.unitCost").value(29000))
                .andExpect(jsonPath("$.leadTimeDays").value(7))
                .andExpect(jsonPath("$.productId").value(product1.getId()))
                .andExpect(jsonPath("$.preferred").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.notes").value("Test vendor link"));
    }

    // 25. DELETE /{id}/vendors/{vendorLinkId} — remove vendor
    @Test
    void removeProductVendor_returns204() throws Exception {
        // First, add a vendor link
        ProductVendor pv = productVendorRepository.saveAndFlush(ProductVendor.builder()
                .product(product1).vendor(vendor)
                .vendorSku("V-SKU-DEL")
                .preferred(false).active(true)
                .tenantId(tenant.getId()).build());
        entityManager.clear();

        mockMvc.perform(delete(BASE_URL + "/" + product1.getId() + "/vendors/" + pv.getId())
                        .with(fullAuth()))
                .andExpect(status().isNoContent());

        // Verify vendor link is removed
        mockMvc.perform(get(BASE_URL + "/" + product1.getId() + "/vendors").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.vendorSku == 'V-SKU-DEL')]").doesNotExist());
    }

    // ==================== Permission Checks ====================

    // 26. No permission → 403
    @Test
    void noPermission_returns403() throws Exception {
        // Read endpoints
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + product1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/sku/SKU-001").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/barcode/12345678").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/search").param("q", "Test").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/count").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/category/" + category.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/brand/" + brand.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/validate-sku/SKU-001").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/validate-barcode/12345678").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + product1.getId() + "/variants").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + product1.getId() + "/vendors").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // Create endpoints
        CreateProductRequest createRequest = CreateProductRequest.builder()
                .name("No Perm Product").baseUomId(uom.getId())
                .sellingPrice(new BigDecimal("100")).build();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/generate-sku").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/generate-sku-from-name").param("name", "X").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/generate-barcode").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // Update endpoints
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .name("No Perm Update").build();

        mockMvc.perform(put(BASE_URL + "/" + product1.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        CreateVariantRequest variantRequest = CreateVariantRequest.builder()
                .option1Value("NoPermColor").build();

        mockMvc.perform(post(BASE_URL + "/" + product1.getId() + "/variants").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variantRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + product1.getId() + "/variants/" + variant1.getId())
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreateProductVendorRequest vendorRequest = CreateProductVendorRequest.builder()
                .vendorId(vendor.getId()).build();

        mockMvc.perform(post(BASE_URL + "/" + product1.getId() + "/vendors").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendorRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + product1.getId() + "/vendors/1").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // Delete endpoint
        mockMvc.perform(delete(BASE_URL + "/" + product1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
