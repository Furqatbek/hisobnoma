package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.dto.CreateInventoryCountRequest;
import com.hisobnoma.platform.inventory.dto.RecordCountRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InventoryCountControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private InventoryCountRepository inventoryCountRepository;
    @Autowired private InventoryCountLineRepository inventoryCountLineRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private UnitOfMeasureRepository unitOfMeasureRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/inventory/counts";

    private Tenant tenant;
    private User user;
    private UnitOfMeasure uom;
    private Category category;
    private Location warehouse;
    private Product product1;
    private Product product2;
    private Stock stock1;
    private Stock stock2;
    private InventoryCount existingCount;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Inventory Count Test Tenant").code("FULLFLOW_CNT").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("cnttestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        uom = unitOfMeasureRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS").name("Pieces").symbol("pcs")
                .isBaseUnit(true).active(true)
                .tenantId(tenant.getId()).build());

        category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-001").name("Count Category")
                .active(true).sortOrder(0).level(0)
                .tenantId(tenant.getId()).build());

        warehouse = locationRepository.saveAndFlush(Location.builder()
                .code("WH-001").name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true).sortOrder(1)
                .tenantId(tenant.getId()).build());

        product1 = productRepository.saveAndFlush(Product.builder()
                .sku("CNT-001").name("Count Product 1")
                .baseUom(uom).sellingPrice(new BigDecimal("10000"))
                .costPrice(new BigDecimal("5000")).category(category)
                .trackInventory(true).active(true)
                .tenantId(tenant.getId()).build());

        product2 = productRepository.saveAndFlush(Product.builder()
                .sku("CNT-002").name("Count Product 2")
                .baseUom(uom).sellingPrice(new BigDecimal("20000"))
                .costPrice(new BigDecimal("10000")).category(category)
                .trackInventory(true).active(true)
                .tenantId(tenant.getId()).build());

        stock1 = stockRepository.saveAndFlush(Stock.builder()
                .product(product1).location(warehouse)
                .quantityOnHand(new BigDecimal("100"))
                .averageCost(new BigDecimal("5000"))
                .tenantId(tenant.getId()).build());

        stock2 = stockRepository.saveAndFlush(Stock.builder()
                .product(product2).location(warehouse)
                .quantityOnHand(new BigDecimal("50"))
                .averageCost(new BigDecimal("10000"))
                .tenantId(tenant.getId()).build());

        existingCount = InventoryCount.builder()
                .countNumber("CNT-202604-0001")
                .location(warehouse)
                .status(CountStatus.DRAFT)
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.of(2026, 4, 15))
                .description("Test Count")
                .tenantId(tenant.getId())
                .build();
        inventoryCountRepository.saveAndFlush(existingCount);

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor allPermsAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("INVENTORY_COUNT_VIEW"),
                        new SimpleGrantedAuthority("INVENTORY_COUNT_CREATE"),
                        new SimpleGrantedAuthority("INVENTORY_COUNT_PERFORM"),
                        new SimpleGrantedAuthority("INVENTORY_COUNT_APPROVE")));
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
    void getInventoryCounts_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].countNumber").value("CNT-202604-0001"))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.content[0].countType").value("PARTIAL"))
                .andExpect(jsonPath("$.content[0].description").value("Test Count"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- GET /{id} ----

    @Test
    void getInventoryCountById_returnsCount() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + existingCount.getId()).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingCount.getId()))
                .andExpect(jsonPath("$.countNumber").value("CNT-202604-0001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.countType").value("PARTIAL"))
                .andExpect(jsonPath("$.countDate").value("2026-04-15"))
                .andExpect(jsonPath("$.description").value("Test Count"))
                .andExpect(jsonPath("$.locationName").value("Main Warehouse"));
    }

    @Test
    void getInventoryCountById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(allPermsAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /status/{status} ----

    @Test
    void getCountsByStatus_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/status/DRAFT").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));

        // No IN_PROGRESS counts exist yet
        mockMvc.perform(get(BASE_URL + "/status/IN_PROGRESS").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    // ---- GET /search ----

    @Test
    void searchCounts_returnsResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("q", "CNT-202604").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].countNumber").value("CNT-202604-0001"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));

        // Search by description
        mockMvc.perform(get(BASE_URL + "/search").param("q", "Test Count").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));

        // Search with no results
        mockMvc.perform(get(BASE_URL + "/search").param("q", "NONEXISTENT").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ---- POST / ----

    @Test
    void createCount_partialType_returnsCreated() throws Exception {
        // Delete the existing count to avoid active count conflict
        inventoryCountRepository.deleteAll();
        entityManager.flush();

        CreateInventoryCountRequest request = CreateInventoryCountRequest.builder()
                .locationId(warehouse.getId())
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.of(2026, 4, 28))
                .description("Partial Stock Count")
                .notes("Testing partial count creation")
                .freezeStock(false)
                .productIds(List.of(product1.getId(), product2.getId()))
                .build();

        mockMvc.perform(post(BASE_URL).with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countNumber").value(startsWith("CNT-")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.countType").value("PARTIAL"))
                .andExpect(jsonPath("$.countDate").value("2026-04-28"))
                .andExpect(jsonPath("$.description").value("Partial Stock Count"))
                .andExpect(jsonPath("$.notes").value("Testing partial count creation"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    // ---- PUT /{id}/start ----

    @Test
    void startCount_draftToInProgress() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + existingCount.getId() + "/start").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingCount.getId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty());
    }

    // ---- PUT /{countId}/lines/{lineId}/count ----

    @Test
    void recordCount_updatesLineQuantity() throws Exception {
        // Create a count with lines via the API
        inventoryCountRepository.deleteAll();
        entityManager.flush();

        CreateInventoryCountRequest createRequest = CreateInventoryCountRequest.builder()
                .locationId(warehouse.getId())
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.of(2026, 4, 28))
                .description("Record Count Test")
                .productIds(List.of(product1.getId()))
                .build();

        MvcResult createResult = mockMvc.perform(post(BASE_URL).with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long countId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();
        Long lineId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("lines").get(0).get("id").asLong();

        // Start the count
        mockMvc.perform(put(BASE_URL + "/" + countId + "/start").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Record count
        RecordCountRequest recordRequest = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("95"))
                .batchNumber("BATCH-001")
                .serialNumber("SER-001")
                .notes("Found 5 units missing")
                .build();

        mockMvc.perform(put(BASE_URL + "/" + countId + "/lines/" + lineId + "/count").with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lineId))
                .andExpect(jsonPath("$.countedQuantity").value(95))
                .andExpect(jsonPath("$.counted").value(true))
                .andExpect(jsonPath("$.batchNumber").value("BATCH-001"))
                .andExpect(jsonPath("$.serialNumber").value("SER-001"))
                .andExpect(jsonPath("$.notes").value("Found 5 units missing"))
                .andExpect(jsonPath("$.systemQuantity").value(100))
                .andExpect(jsonPath("$.varianceQuantity").value(-5));
    }

    // ---- PUT /{id}/complete ----

    @Test
    void completeCount_allCounted_completesSuccessfully() throws Exception {
        // Create count with lines, start, count all lines, then complete
        inventoryCountRepository.deleteAll();
        entityManager.flush();

        CreateInventoryCountRequest createRequest = CreateInventoryCountRequest.builder()
                .locationId(warehouse.getId())
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.of(2026, 4, 28))
                .description("Complete Count Test")
                .productIds(List.of(product1.getId()))
                .build();

        MvcResult createResult = mockMvc.perform(post(BASE_URL).with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long countId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();
        Long lineId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("lines").get(0).get("id").asLong();

        // Start
        mockMvc.perform(put(BASE_URL + "/" + countId + "/start").with(allPermsAuth()))
                .andExpect(status().isOk());

        // Record count for the single line
        RecordCountRequest recordRequest = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("100"))
                .build();

        mockMvc.perform(put(BASE_URL + "/" + countId + "/lines/" + lineId + "/count").with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordRequest)))
                .andExpect(status().isOk());

        // Complete
        mockMvc.perform(put(BASE_URL + "/" + countId + "/complete").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void completeCount_notAllCounted_returns400() throws Exception {
        // Create count with 2 lines, start, but do NOT count all, then try to complete
        inventoryCountRepository.deleteAll();
        entityManager.flush();

        CreateInventoryCountRequest createRequest = CreateInventoryCountRequest.builder()
                .locationId(warehouse.getId())
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.of(2026, 4, 28))
                .description("Incomplete Count Test")
                .productIds(List.of(product1.getId(), product2.getId()))
                .build();

        MvcResult createResult = mockMvc.perform(post(BASE_URL).with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long countId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Start
        mockMvc.perform(put(BASE_URL + "/" + countId + "/start").with(allPermsAuth()))
                .andExpect(status().isOk());

        // Try to complete without counting any lines
        mockMvc.perform(put(BASE_URL + "/" + countId + "/complete").with(allPermsAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /{id}/cancel ----

    @Test
    void cancelCount_updatesStatus() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + existingCount.getId() + "/cancel").with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "No longer needed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingCount.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("No longer needed"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + existingCount.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/status/DRAFT").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/search").param("q", "test").with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreateInventoryCountRequest request = CreateInventoryCountRequest.builder()
                .locationId(warehouse.getId())
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.of(2026, 4, 28))
                .build();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + existingCount.getId() + "/start").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + existingCount.getId() + "/complete").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + existingCount.getId() + "/approve").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + existingCount.getId() + "/cancel").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "test"))))
                .andExpect(status().isForbidden());

        RecordCountRequest recordRequest = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("10"))
                .build();

        mockMvc.perform(put(BASE_URL + "/1/lines/1/count").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/1/lines/1/recount").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "test"))))
                .andExpect(status().isForbidden());
    }

    // ---- Full Lifecycle ----

    @Test
    void fullLifecycle_createStartCountCompleteApprove() throws Exception {
        // Clear existing counts to avoid active count conflict
        inventoryCountRepository.deleteAll();
        entityManager.flush();

        // 1. CREATE
        CreateInventoryCountRequest createRequest = CreateInventoryCountRequest.builder()
                .locationId(warehouse.getId())
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.of(2026, 4, 28))
                .description("Full Lifecycle Test")
                .notes("End-to-end lifecycle test")
                .productIds(List.of(product1.getId(), product2.getId()))
                .build();

        MvcResult createResult = mockMvc.perform(post(BASE_URL).with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.countType").value("PARTIAL"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.countedItems").value(0))
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andReturn();

        Long countId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();
        Long line1Id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("lines").get(0).get("id").asLong();
        Long line2Id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("lines").get(1).get("id").asLong();

        // 2. START
        mockMvc.perform(put(BASE_URL + "/" + countId + "/start").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty());

        // 3. RECORD COUNT for line 1 (variance: counted 95, system 100 => variance -5)
        RecordCountRequest record1 = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("95"))
                .notes("5 units missing")
                .build();

        mockMvc.perform(put(BASE_URL + "/" + countId + "/lines/" + line1Id + "/count").with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(record1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counted").value(true))
                .andExpect(jsonPath("$.countedQuantity").value(95))
                .andExpect(jsonPath("$.varianceQuantity").value(-5));

        // 4. RECORD COUNT for line 2 (no variance: counted 50, system 50)
        RecordCountRequest record2 = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("50"))
                .build();

        mockMvc.perform(put(BASE_URL + "/" + countId + "/lines/" + line2Id + "/count").with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(record2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counted").value(true))
                .andExpect(jsonPath("$.countedQuantity").value(50))
                .andExpect(jsonPath("$.varianceQuantity").value(0));

        // 5. COMPLETE
        mockMvc.perform(put(BASE_URL + "/" + countId + "/complete").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.countedItems").value(2))
                .andExpect(jsonPath("$.totalItems").value(2));

        // 6. APPROVE (posts stock adjustments for variances)
        mockMvc.perform(put(BASE_URL + "/" + countId + "/approve").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedAt").isNotEmpty())
                .andExpect(jsonPath("$.approvedBy").isNotEmpty())
                .andExpect(jsonPath("$.adjustmentsPosted").value(true));

        // 7. Verify final state via GET
        mockMvc.perform(get(BASE_URL + "/" + countId).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.countedItems").value(2));
    }
}
