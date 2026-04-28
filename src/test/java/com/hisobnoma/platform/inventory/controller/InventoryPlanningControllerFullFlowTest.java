package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.repository.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InventoryPlanningControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductRepository productRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UnitOfMeasureRepository unitOfMeasureRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/inventory/planning";

    private Tenant tenant;
    private User user;
    private UnitOfMeasure uom;
    private Category category;
    private Location warehouse;
    private Product lowStockProduct;
    private Product healthyStockProduct;
    private Stock lowStock;
    private Stock healthyStock;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Planning Test Tenant").code("FULLFLOW_PLN").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("plntestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        uom = unitOfMeasureRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS").name("Pieces").symbol("pcs")
                .isBaseUnit(true).active(true)
                .tenantId(tenant.getId()).build());

        category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-001").name("Planning Category")
                .active(true).sortOrder(0).level(0)
                .tenantId(tenant.getId()).build());

        warehouse = locationRepository.saveAndFlush(Location.builder()
                .code("WH-001").name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true).sortOrder(1)
                .tenantId(tenant.getId()).build());

        lowStockProduct = productRepository.saveAndFlush(Product.builder()
                .sku("PLN-001").name("Low Stock Item")
                .baseUom(uom).category(category)
                .sellingPrice(new BigDecimal("10000"))
                .costPrice(new BigDecimal("5000"))
                .reorderPoint(new BigDecimal("50"))
                .reorderQuantity(new BigDecimal("100"))
                .trackInventory(true).active(true)
                .tenantId(tenant.getId()).build());

        healthyStockProduct = productRepository.saveAndFlush(Product.builder()
                .sku("PLN-002").name("Healthy Stock Item")
                .baseUom(uom).category(category)
                .sellingPrice(new BigDecimal("20000"))
                .costPrice(new BigDecimal("10000"))
                .reorderPoint(new BigDecimal("10"))
                .reorderQuantity(new BigDecimal("50"))
                .trackInventory(true).active(true)
                .tenantId(tenant.getId()).build());

        // lowStock: quantityOnHand=5, well below reorderPoint=50
        lowStock = stockRepository.saveAndFlush(Stock.builder()
                .product(lowStockProduct).location(warehouse)
                .quantityOnHand(new BigDecimal("5"))
                .averageCost(new BigDecimal("5000"))
                .tenantId(tenant.getId()).build());

        // healthyStock: quantityOnHand=100, well above reorderPoint=10
        healthyStock = stockRepository.saveAndFlush(Stock.builder()
                .product(healthyStockProduct).location(warehouse)
                .quantityOnHand(new BigDecimal("100"))
                .averageCost(new BigDecimal("10000"))
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor viewAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("INVENTORY_STOCK_VIEW")));
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

    // ==================== Reorder Suggestions ====================

    @Test
    void getReorderSuggestions_returnsLowStockProducts() throws Exception {
        mockMvc.perform(get(BASE_URL + "/reorder-suggestions").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(lowStockProduct.getId()))
                .andExpect(jsonPath("$[0].productSku").value("PLN-001"))
                .andExpect(jsonPath("$[0].productName").value("Low Stock Item"))
                .andExpect(jsonPath("$[0].locationId").value(warehouse.getId()))
                .andExpect(jsonPath("$[0].locationName").value("Main Warehouse"))
                .andExpect(jsonPath("$[0].currentStock").value(5))
                .andExpect(jsonPath("$[0].reorderPoint").value(50))
                .andExpect(jsonPath("$[0].suggestedQuantity").value(100))
                .andExpect(jsonPath("$[0].estimatedCost").value(500000))
                .andExpect(jsonPath("$[0].priority").value("HIGH"));
    }

    @Test
    void getReorderSuggestions_noLowStock_returnsEmptyList() throws Exception {
        // Update low stock product to have stock above reorder point
        stockRepository.findById(lowStock.getId()).ifPresent(s -> {
            s.setQuantityOnHand(new BigDecimal("200"));
            stockRepository.saveAndFlush(s);
        });
        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/reorder-suggestions").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== ABC Analysis ====================

    @Test
    void getAbcAnalysis_returnsClassifications() throws Exception {
        mockMvc.perform(get(BASE_URL + "/abc-analysis").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                // Both products should have classifications
                .andExpect(jsonPath("$[*].productSku", containsInAnyOrder("PLN-001", "PLN-002")))
                .andExpect(jsonPath("$[*].classification", everyItem(isOneOf("A", "B", "C"))))
                // healthyStockProduct has higher stock value (100*10000=1000000) vs lowStockProduct (5*5000=25000)
                // So healthyStockProduct should be first (sorted by totalValue desc)
                .andExpect(jsonPath("$[0].productSku").value("PLN-002"))
                .andExpect(jsonPath("$[0].stockValue").value(1000000))
                .andExpect(jsonPath("$[0].cumulativePercent").exists())
                .andExpect(jsonPath("$[0].percentOfTotal").exists())
                .andExpect(jsonPath("$[1].productSku").value("PLN-001"))
                .andExpect(jsonPath("$[1].stockValue").value(25000));
    }

    // ==================== Slow-Moving Products ====================

    @Test
    void getSlowMoving_returnsSlowMovingProducts() throws Exception {
        // Both products have stock but no sales movements, so both are slow-moving
        mockMvc.perform(get(BASE_URL + "/slow-moving").param("days", "90").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].productSku", containsInAnyOrder("PLN-001", "PLN-002")))
                .andExpect(jsonPath("$[*].recommendation", everyItem(is("DISCOUNT"))))
                .andExpect(jsonPath("$[*].daysSinceLastSale", everyItem(greaterThanOrEqualTo(90))))
                .andExpect(jsonPath("$[*].currentStock", everyItem(greaterThan(0))));
    }

    // ==================== Dead Stock ====================

    @Test
    void getDeadStock_returnsDeadStockProducts() throws Exception {
        // Both products have stock but no movements at all, so both are dead stock
        mockMvc.perform(get(BASE_URL + "/dead-stock").param("days", "180").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].productSku", containsInAnyOrder("PLN-001", "PLN-002")))
                .andExpect(jsonPath("$[*].recommendation", everyItem(is("WRITE_OFF"))))
                .andExpect(jsonPath("$[*].daysSinceLastSale", everyItem(greaterThanOrEqualTo(180))))
                .andExpect(jsonPath("$[*].currentStock", everyItem(greaterThan(0))));
    }

    // ==================== Permission Check ====================

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/reorder-suggestions").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/abc-analysis").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/slow-moving").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/dead-stock").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
