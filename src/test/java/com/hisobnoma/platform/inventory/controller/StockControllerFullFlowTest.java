package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.dto.StockAdjustRequest;
import com.hisobnoma.platform.inventory.dto.StockIssueRequest;
import com.hisobnoma.platform.inventory.dto.StockReceiveRequest;
import com.hisobnoma.platform.inventory.dto.StockTransferRequest;
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
class StockControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StockRepository stockRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UnitOfMeasureRepository unitOfMeasureRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/inventory/stock";

    private Tenant tenant;
    private User user;
    private UnitOfMeasure uom;
    private Category category;
    private Product product1;
    private Product product2;
    private Location warehouse;
    private Location store;
    private Stock stock1;
    private Stock stock2;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Stock Test Tenant").code("FULLFLOW_STK").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("stktestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        uom = unitOfMeasureRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS").name("Pieces").symbol("pcs")
                .isBaseUnit(true).active(true)
                .tenantId(tenant.getId()).build());

        category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-001").name("Test Category")
                .active(true).sortOrder(0).level(0)
                .tenantId(tenant.getId()).build());

        product1 = productRepository.saveAndFlush(Product.builder()
                .sku("STK-001").name("Stock Product")
                .baseUom(uom).category(category)
                .sellingPrice(new BigDecimal("10000"))
                .costPrice(new BigDecimal("5000"))
                .active(true).trackInventory(true)
                .tenantId(tenant.getId()).build());

        product2 = productRepository.saveAndFlush(Product.builder()
                .sku("STK-002").name("Another Product")
                .baseUom(uom).category(category)
                .sellingPrice(new BigDecimal("20000"))
                .costPrice(new BigDecimal("10000"))
                .active(true)
                .tenantId(tenant.getId()).build());

        warehouse = locationRepository.saveAndFlush(Location.builder()
                .code("WH-001").name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true).sortOrder(1)
                .tenantId(tenant.getId()).build());

        store = locationRepository.saveAndFlush(Location.builder()
                .code("STORE-001").name("Retail Store")
                .locationType(LocationType.STORE)
                .active(true).sortOrder(2)
                .tenantId(tenant.getId()).build());

        stock1 = stockRepository.saveAndFlush(Stock.builder()
                .product(product1).location(warehouse)
                .quantityOnHand(new BigDecimal("100"))
                .averageCost(new BigDecimal("5000"))
                .tenantId(tenant.getId()).build());

        stock2 = stockRepository.saveAndFlush(Stock.builder()
                .product(product1).location(store)
                .quantityOnHand(new BigDecimal("50"))
                .averageCost(new BigDecimal("5000"))
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor viewAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("INVENTORY_STOCK_VIEW"),
                        new SimpleGrantedAuthority("INVENTORY_STOCK_RECEIVE"),
                        new SimpleGrantedAuthority("INVENTORY_STOCK_ISSUE"),
                        new SimpleGrantedAuthority("INVENTORY_STOCK_TRANSFER"),
                        new SimpleGrantedAuthority("INVENTORY_STOCK_ADJUST")));
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

    // ==================== Query Tests ====================

    @Test
    void getAllStock_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void getStockByProduct_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/product/" + product1.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.locationCode == 'WH-001')]").exists())
                .andExpect(jsonPath("$[?(@.locationCode == 'STORE-001')]").exists());
    }

    @Test
    void getStockByLocation_returnsPaginated() throws Exception {
        mockMvc.perform(get(BASE_URL + "/location/" + warehouse.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].locationCode").value("WH-001"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void getStockByProductAndLocation_returnsStock() throws Exception {
        mockMvc.perform(get(BASE_URL + "/product/" + product1.getId() + "/location/" + warehouse.getId())
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productSku").value("STK-001"))
                .andExpect(jsonPath("$.productName").value("Stock Product"))
                .andExpect(jsonPath("$.locationCode").value("WH-001"))
                .andExpect(jsonPath("$.quantityOnHand").value(100))
                .andExpect(jsonPath("$.averageCost").value(5000));
    }

    @Test
    void getStockByProductAndLocation_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/product/" + product2.getId() + "/location/" + warehouse.getId())
                        .with(viewAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchStock_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("q", "Stock Product").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.productSku == 'STK-001')]").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void getStockValuation_returnsValuation() throws Exception {
        mockMvc.perform(get(BASE_URL + "/valuation").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").exists())
                .andExpect(jsonPath("$.totalProducts").exists());
    }

    @Test
    void getAvailableQuantity_returnsMap() throws Exception {
        mockMvc.perform(get(BASE_URL + "/available/" + product1.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(150));
    }

    @Test
    void checkAvailability_sufficient_returnsTrue() throws Exception {
        mockMvc.perform(get(BASE_URL + "/check-availability")
                        .param("productId", product1.getId().toString())
                        .param("locationId", warehouse.getId().toString())
                        .param("quantity", "50")
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void checkAvailability_insufficient_returnsFalse() throws Exception {
        mockMvc.perform(get(BASE_URL + "/check-availability")
                        .param("productId", product1.getId().toString())
                        .param("locationId", warehouse.getId().toString())
                        .param("quantity", "999999")
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    // ==================== Operation Tests ====================

    @Test
    void receiveStock_createsMovementAndUpdatesStock() throws Exception {
        StockReceiveRequest request = StockReceiveRequest.builder()
                .locationId(warehouse.getId())
                .referenceType(MovementReferenceType.PURCHASE_ORDER)
                .referenceNumber("PO-001")
                .notes("Test receive")
                .items(List.of(StockReceiveRequest.ReceiveItem.builder()
                        .productId(product1.getId())
                        .quantity(new BigDecimal("25"))
                        .unitCost(new BigDecimal("5500"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL + "/receive").with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].movementType").value("STOCK_IN"))
                .andExpect(jsonPath("$[0].quantity").value(25))
                .andExpect(jsonPath("$[0].quantityBefore").value(100))
                .andExpect(jsonPath("$[0].quantityAfter").value(125))
                .andExpect(jsonPath("$[0].referenceType").value("PURCHASE_ORDER"))
                .andExpect(jsonPath("$[0].referenceNumber").value("PO-001"));
    }

    @Test
    void issueStock_createsMovementAndUpdatesStock() throws Exception {
        StockIssueRequest request = StockIssueRequest.builder()
                .locationId(warehouse.getId())
                .referenceType(MovementReferenceType.POS_SALE)
                .referenceNumber("SALE-001")
                .reason("Customer sale")
                .notes("Test issue")
                .items(List.of(StockIssueRequest.IssueItem.builder()
                        .productId(product1.getId())
                        .quantity(new BigDecimal("10"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL + "/issue").with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].movementType").value("STOCK_OUT"))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].quantityBefore").value(100))
                .andExpect(jsonPath("$[0].quantityAfter").value(90))
                .andExpect(jsonPath("$[0].reason").value("Customer sale"));
    }

    @Test
    void transferStock_createsMovementsAndUpdatesStock() throws Exception {
        StockTransferRequest request = StockTransferRequest.builder()
                .fromLocationId(warehouse.getId())
                .toLocationId(store.getId())
                .referenceNumber("TRF-001")
                .reason("Replenish store")
                .notes("Test transfer")
                .items(List.of(StockTransferRequest.TransferItem.builder()
                        .productId(product1.getId())
                        .quantity(new BigDecimal("20"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL + "/transfer").with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].movementType").value("TRANSFER"))
                .andExpect(jsonPath("$[0].quantity").value(20))
                .andExpect(jsonPath("$[0].quantityBefore").value(100))
                .andExpect(jsonPath("$[0].quantityAfter").value(80))
                .andExpect(jsonPath("$[0].referenceNumber").value("TRF-001"))
                .andExpect(jsonPath("$[0].reason").value("Replenish store"));
    }

    @Test
    void adjustStock_positiveAdjustment_updatesStock() throws Exception {
        StockAdjustRequest request = StockAdjustRequest.builder()
                .locationId(warehouse.getId())
                .reason("Found extra stock")
                .notes("Positive adjustment test")
                .items(List.of(StockAdjustRequest.AdjustItem.builder()
                        .productId(product1.getId())
                        .adjustmentQuantity(new BigDecimal("15"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL + "/adjust").with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].movementType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$[0].quantity").value(15))
                .andExpect(jsonPath("$[0].quantityBefore").value(100))
                .andExpect(jsonPath("$[0].quantityAfter").value(115));
    }

    @Test
    void adjustStock_negativeAdjustment_updatesStock() throws Exception {
        StockAdjustRequest request = StockAdjustRequest.builder()
                .locationId(warehouse.getId())
                .reason("Damaged goods")
                .notes("Negative adjustment test")
                .items(List.of(StockAdjustRequest.AdjustItem.builder()
                        .productId(product1.getId())
                        .adjustmentQuantity(new BigDecimal("-5"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL + "/adjust").with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].movementType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$[0].quantity").value(5))
                .andExpect(jsonPath("$[0].quantityBefore").value(100))
                .andExpect(jsonPath("$[0].quantityAfter").value(95));
    }

    // ==================== Movement Query Tests ====================

    @Test
    void getMovements_returnsPaginatedList() throws Exception {
        // First create a movement by receiving stock
        StockReceiveRequest receiveRequest = StockReceiveRequest.builder()
                .locationId(warehouse.getId())
                .referenceType(MovementReferenceType.MANUAL_ADJUSTMENT)
                .notes("Movement query test")
                .items(List.of(StockReceiveRequest.ReceiveItem.builder()
                        .productId(product1.getId())
                        .quantity(new BigDecimal("5"))
                        .unitCost(new BigDecimal("5000"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL + "/receive").with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiveRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(BASE_URL + "/movements").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void getMovementsByProduct_returnsFiltered() throws Exception {
        // Create a movement for product1
        StockReceiveRequest receiveRequest = StockReceiveRequest.builder()
                .locationId(warehouse.getId())
                .referenceType(MovementReferenceType.MANUAL_ADJUSTMENT)
                .notes("Product movement test")
                .items(List.of(StockReceiveRequest.ReceiveItem.builder()
                        .productId(product1.getId())
                        .quantity(new BigDecimal("3"))
                        .unitCost(new BigDecimal("5000"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL + "/receive").with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiveRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(BASE_URL + "/movements/product/" + product1.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].productSku").value("STK-001"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ==================== Permission Test ====================

    @Test
    void noPermission_returns403() throws Exception {
        // GET endpoints - INVENTORY_STOCK_VIEW
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/product/" + product1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/location/" + warehouse.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/product/" + product1.getId() + "/location/" + warehouse.getId())
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/low-stock").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/search").param("q", "test").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/valuation").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/available/" + product1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/check-availability")
                        .param("productId", product1.getId().toString())
                        .param("locationId", warehouse.getId().toString())
                        .param("quantity", "10")
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/movements").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/movements/product/" + product1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/movements/location/" + warehouse.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST endpoints - various perms
        StockReceiveRequest receiveRequest = StockReceiveRequest.builder()
                .locationId(warehouse.getId())
                .items(List.of(StockReceiveRequest.ReceiveItem.builder()
                        .productId(product1.getId())
                        .quantity(new BigDecimal("1"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL + "/receive").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiveRequest)))
                .andExpect(status().isForbidden());

        StockIssueRequest issueRequest = StockIssueRequest.builder()
                .locationId(warehouse.getId())
                .items(List.of(StockIssueRequest.IssueItem.builder()
                        .productId(product1.getId())
                        .quantity(new BigDecimal("1"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL + "/issue").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isForbidden());

        StockTransferRequest transferRequest = StockTransferRequest.builder()
                .fromLocationId(warehouse.getId())
                .toLocationId(store.getId())
                .items(List.of(StockTransferRequest.TransferItem.builder()
                        .productId(product1.getId())
                        .quantity(new BigDecimal("1"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL + "/transfer").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isForbidden());

        StockAdjustRequest adjustRequest = StockAdjustRequest.builder()
                .locationId(warehouse.getId())
                .reason("Test")
                .items(List.of(StockAdjustRequest.AdjustItem.builder()
                        .productId(product1.getId())
                        .adjustmentQuantity(new BigDecimal("1"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL + "/adjust").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustRequest)))
                .andExpect(status().isForbidden());
    }
}
