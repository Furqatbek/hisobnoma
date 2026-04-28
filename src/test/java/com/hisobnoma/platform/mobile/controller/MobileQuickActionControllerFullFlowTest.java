package com.hisobnoma.platform.mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MobileQuickActionControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;

    private Tenant tenant;
    private User user;
    private Product productWithBarcode;
    private Product productWithoutBarcode;
    private Customer customer1;
    private Customer customer2;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Mobile QuickAction Tenant").code("FULLFLOW_MOBQA").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("mobqauser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        Category category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-MOBQA").name("MobQA Category")
                .active(true).tenantId(tenant.getId()).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS-MQA").name("Pieces").symbol("pcs")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).tenantId(tenant.getId()).build());

        productWithBarcode = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-MOBQA-001")
                .name("TestProduct Alpha")
                .barcode("BC001")
                .sellingPrice(new BigDecimal("25000.0000"))
                .costPrice(new BigDecimal("15000.0000"))
                .category(category)
                .baseUom(uom)
                .active(true)
                .trackInventory(true)
                .tenantId(tenant.getId())
                .build());

        productWithoutBarcode = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-MOBQA-002")
                .name("TestProduct Beta")
                .sellingPrice(new BigDecimal("40000.0000"))
                .costPrice(new BigDecimal("28000.0000"))
                .category(category)
                .baseUom(uom)
                .active(true)
                .trackInventory(false)
                .tenantId(tenant.getId())
                .build());

        customer1 = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-MQA-001")
                .name("TestCustomer One")
                .phone("+998901234567")
                .email("customer1@mobqa.test")
                .active(true)
                .creditLimit(new BigDecimal("5000000.0000"))
                .currentBalance(BigDecimal.ZERO)
                .tenantId(tenant.getId())
                .build());

        customer2 = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-MQA-002")
                .name("TestCustomer Two")
                .phone("+998907654321")
                .email("customer2@mobqa.test")
                .active(true)
                .creditLimit(new BigDecimal("3000000.0000"))
                .currentBalance(BigDecimal.ZERO)
                .tenantId(tenant.getId())
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("INVENTORY_PRODUCT_READ"),
                    new SimpleGrantedAuthority("POS_SALE_CREATE"),
                    new SimpleGrantedAuthority("MOBILE_INVENTORY_VIEW"),
                    new SimpleGrantedAuthority("MOBILE_QUICK_COUNT"),
                    new SimpleGrantedAuthority("MOBILE_QUICK_SALE"),
                    new SimpleGrantedAuthority("FINANCE_AR_CUSTOMER_VIEW"),
                    new SimpleGrantedAuthority("INVENTORY_COUNT_CREATE"),
                    new SimpleGrantedAuthority("INVENTORY_STOCK_ADJUST")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("SOME_OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ==================== GET /api/v1/mobile/inventory/barcode/{barcode} ====================

    @Test
    void lookupByBarcode_found_returnsProduct() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/inventory/barcode/BC001").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(productWithBarcode.getId()))
                .andExpect(jsonPath("$.data.sku").value("SKU-MOBQA-001"))
                .andExpect(jsonPath("$.data.barcode").value("BC001"))
                .andExpect(jsonPath("$.data.name").value("TestProduct Alpha"))
                .andExpect(jsonPath("$.data.sellingPrice").isNumber())
                .andExpect(jsonPath("$.data.trackInventory").value(true));
    }

    @Test
    void lookupByBarcode_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/inventory/barcode/NONEXIST").with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /api/v1/mobile/inventory/search ====================

    @Test
    void searchProducts_byName_returnsResults() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/inventory/search")
                        .param("query", "TestProduct")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].name", containsString("TestProduct")));
    }

    @Test
    void searchProducts_noMatch_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/inventory/search")
                        .param("query", "ZZZNOTFOUND")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    // ==================== GET /api/v1/mobile/products/search (alias) ====================

    @Test
    void searchProductsAlias_byQuery_returnsResults() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/products/search")
                        .param("query", "TestProduct")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].name", containsString("TestProduct")));
    }

    // ==================== GET /api/v1/mobile/customers/search ====================

    @Test
    void searchCustomers_byName_returnsResults() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/customers/search")
                        .param("query", "TestCustomer")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].name", containsString("TestCustomer")));
    }

    @Test
    void searchCustomers_noMatch_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/customers/search")
                        .param("query", "ZZZNOTFOUND")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    // ==================== Authorization tests ====================

    @Test
    void lookupByBarcode_forbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/inventory/barcode/BC001").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchCustomers_forbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/customers/search")
                        .param("query", "test")
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
