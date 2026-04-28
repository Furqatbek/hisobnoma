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
class MobileSyncControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private UnitOfMeasureRepository unitOfMeasureRepository;

    private Tenant tenant;
    private User user;
    private Category category;
    private Product product1;
    private Product product2;
    private Customer customer1;
    private Customer customer2;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Mobile Sync Tenant").code("FULLFLOW_MOBSYNC").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("mobsyncuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-SYNC-001").name("Sync Category")
                .sortOrder(1).active(true)
                .tenantId(tenant.getId()).build());

        UnitOfMeasure uom = unitOfMeasureRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS").name("Pieces").symbol("pcs")
                .active(true).isBaseUnit(true)
                .tenantId(tenant.getId()).build());

        product1 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-SYNC-001").name("Sync Product A").barcode("9990001111")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("10000"))
                .costPrice(new BigDecimal("7000"))
                .trackInventory(true).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        product2 = productRepository.saveAndFlush(Product.builder()
                .sku("SKU-SYNC-002").name("Sync Product B").barcode("9990002222")
                .category(category).baseUom(uom)
                .sellingPrice(new BigDecimal("25000"))
                .costPrice(new BigDecimal("18000"))
                .trackInventory(false).active(true).sellable(true)
                .tenantId(tenant.getId()).build());

        customer1 = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-SYNC-001").name("Sync Customer A")
                .phone("+998901234567").email("custA@test.com")
                .creditLimit(new BigDecimal("5000000"))
                .currentBalance(new BigDecimal("100000"))
                .active(true)
                .tenantId(tenant.getId()).build());

        customer2 = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-SYNC-002").name("Sync Customer B")
                .phone("+998907654321").email("custB@test.com")
                .creditLimit(new BigDecimal("3000000"))
                .currentBalance(BigDecimal.ZERO)
                .active(true)
                .tenantId(tenant.getId()).build());

        entityManager.flush();
        entityManager.clear();
    }

    // ==================== Auth helpers ====================

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("MOBILE_SYNC_ACCESS"),
                    new SimpleGrantedAuthority("INVENTORY_PRODUCT_READ"),
                    new SimpleGrantedAuthority("FINANCE_AR_CUSTOMER_VIEW"),
                    new SimpleGrantedAuthority("POS_SALE_CREATE")));
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

    // ==================== GET /api/v1/mobile/sync/products ====================

    @Test
    void syncProducts_fullSync_returnsAllProducts() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/products").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.products", hasSize(2)))
                .andExpect(jsonPath("$.data.fullSyncRequired").value(true))
                .andExpect(jsonPath("$.data.syncVersion").value("1.0"));
    }

    @Test
    void syncProducts_incrementalSync_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/products")
                        .param("lastSyncAt", "2099-01-01T00:00:00Z")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.products", hasSize(0)))
                .andExpect(jsonPath("$.data.fullSyncRequired").value(false));
    }

    @Test
    void syncProducts_forbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/products").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/sync/customers ====================

    @Test
    void syncCustomers_fullSync_returnsAllCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/customers").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customers", hasSize(2)))
                .andExpect(jsonPath("$.data.fullSyncRequired").value(true))
                .andExpect(jsonPath("$.data.syncVersion").value("1.0"));
    }

    @Test
    void syncCustomers_incrementalSync_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/customers")
                        .param("lastSyncAt", "2099-01-01T00:00:00Z")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customers", hasSize(0)))
                .andExpect(jsonPath("$.data.fullSyncRequired").value(false));
    }

    @Test
    void syncCustomers_forbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/customers").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/sync/categories ====================

    @Test
    void syncCategories_returnsAllCategories() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/categories").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories", hasSize(1)))
                .andExpect(jsonPath("$.data.syncVersion").value("1.0"));
    }

    @Test
    void syncCategories_forbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/categories").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/sync/last-updated ====================

    @Test
    void getLastUpdated_withData_returnsTimestamp() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/last-updated").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastUpdated").isNotEmpty())
                .andExpect(jsonPath("$.data.syncRequired").value(true));
    }

    @Test
    void getLastUpdated_forbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/last-updated").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
