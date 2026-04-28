package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.dto.CreatePurchaseOrderLineRequest;
import com.hisobnoma.platform.inventory.dto.CreatePurchaseOrderRequest;
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
class PurchaseOrderControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private PurchaseOrderLineRepository purchaseOrderLineRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UnitOfMeasureRepository unitOfMeasureRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/inventory/purchase-orders";

    private Tenant tenant;
    private User user;
    private UnitOfMeasure uom;
    private Vendor vendor;
    private Location warehouse;
    private Product product1;
    private Product product2;
    private PurchaseOrder draftPO;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("PO Test Tenant").code("FULLFLOW_PO").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("potestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        uom = unitOfMeasureRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS").name("Pieces").isBaseUnit(true).active(true)
                .tenantId(tenant.getId()).build());

        vendor = vendorRepository.saveAndFlush(Vendor.builder()
                .code("VND-001").name("PO Test Vendor").active(true)
                .tenantId(tenant.getId()).build());

        warehouse = locationRepository.saveAndFlush(Location.builder()
                .code("WH-001").name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE).active(true)
                .tenantId(tenant.getId()).build());

        product1 = productRepository.saveAndFlush(Product.builder()
                .sku("PO-PRD-001").name("Widget").baseUom(uom)
                .sellingPrice(new BigDecimal("10000")).costPrice(new BigDecimal("5000"))
                .active(true).tenantId(tenant.getId()).build());

        product2 = productRepository.saveAndFlush(Product.builder()
                .sku("PO-PRD-002").name("Gadget").baseUom(uom)
                .sellingPrice(new BigDecimal("20000")).costPrice(new BigDecimal("10000"))
                .active(true).tenantId(tenant.getId()).build());

        draftPO = PurchaseOrder.builder()
                .poNumber("PO-000001")
                .vendor(vendor)
                .location(warehouse)
                .status(POStatus.DRAFT)
                .orderDate(LocalDate.of(2026, 4, 1))
                .tenantId(tenant.getId())
                .build();
        draftPO = purchaseOrderRepository.saveAndFlush(draftPO);

        PurchaseOrderLine line = PurchaseOrderLine.builder()
                .purchaseOrder(draftPO)
                .product(product1)
                .lineNumber(1)
                .quantity(new BigDecimal("100"))
                .uom(uom)
                .unitPrice(new BigDecimal("5000"))
                .tenantId(tenant.getId())
                .build();
        purchaseOrderLineRepository.saveAndFlush(line);

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

    private RequestPostProcessor viewAuth() {
        return authWith("INVENTORY_PO_VIEW");
    }

    private RequestPostProcessor fullAuth() {
        return authWith("INVENTORY_PO_VIEW", "INVENTORY_PO_CREATE", "INVENTORY_PO_UPDATE",
                "INVENTORY_PO_APPROVE", "INVENTORY_PO_CANCEL");
    }

    private RequestPostProcessor noPermAuth() {
        return authWith("SOME_OTHER_PERM");
    }

    // ---- Helper to build a valid create request ----

    private CreatePurchaseOrderRequest buildValidCreateRequest() {
        return CreatePurchaseOrderRequest.builder()
                .vendorId(vendor.getId())
                .locationId(warehouse.getId())
                .orderDate(LocalDate.of(2026, 4, 15))
                .expectedDate(LocalDate.of(2026, 5, 15))
                .currency("UZS")
                .paymentTerms("Net 30")
                .shippingMethod("Truck")
                .shippingAddress("123 Test St")
                .notes("Test PO notes")
                .internalNotes("Internal only")
                .vendorReference("VREF-001")
                .lines(List.of(
                        CreatePurchaseOrderLineRequest.builder()
                                .productId(product1.getId())
                                .description("Widget order")
                                .quantity(new BigDecimal("50"))
                                .uomId(uom.getId())
                                .unitPrice(new BigDecimal("5000"))
                                .build(),
                        CreatePurchaseOrderLineRequest.builder()
                                .productId(product2.getId())
                                .description("Gadget order")
                                .quantity(new BigDecimal("25"))
                                .uomId(uom.getId())
                                .unitPrice(new BigDecimal("10000"))
                                .build()
                ))
                .build();
    }

    // ---- GET / (paginated) ----

    @Test
    void getPurchaseOrders_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.poNumber == 'PO-000001')]").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- GET /{id} ----

    @Test
    void getPurchaseOrderById_returnsPO() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + draftPO.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draftPO.getId()))
                .andExpect(jsonPath("$.poNumber").value("PO-000001"))
                .andExpect(jsonPath("$.vendorId").value(vendor.getId()))
                .andExpect(jsonPath("$.vendorCode").value("VND-001"))
                .andExpect(jsonPath("$.vendorName").value("PO Test Vendor"))
                .andExpect(jsonPath("$.locationId").value(warehouse.getId()))
                .andExpect(jsonPath("$.locationCode").value("WH-001"))
                .andExpect(jsonPath("$.locationName").value("Main Warehouse"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.orderDate").value("2026-04-01"))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].productSku").value("PO-PRD-001"))
                .andExpect(jsonPath("$.lines[0].productName").value("Widget"))
                .andExpect(jsonPath("$.lines[0].quantity").value(100))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(5000))
                .andExpect(jsonPath("$.lines[0].uomCode").value("PCS"));
    }

    @Test
    void getPurchaseOrderById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(viewAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /status/{status} ----

    @Test
    void getPurchaseOrdersByStatus_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/status/DRAFT").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.poNumber == 'PO-000001')]").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));

        // No APPROVED POs should exist
        mockMvc.perform(get(BASE_URL + "/status/APPROVED").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ---- GET /vendor/{vendorId} ----

    @Test
    void getPurchaseOrdersByVendor_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/vendor/" + vendor.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.poNumber == 'PO-000001')]").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- GET /search?q= ----

    @Test
    void searchPurchaseOrders_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("q", "PO-000001").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].poNumber").value("PO-000001"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));

        // Search by vendor name
        mockMvc.perform(get(BASE_URL + "/search").param("q", "PO Test Vendor").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));
    }

    // ---- POST / ----

    @Test
    void createPurchaseOrder_validRequest_returnsCreated() throws Exception {
        CreatePurchaseOrderRequest request = buildValidCreateRequest();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.poNumber").isString())
                .andExpect(jsonPath("$.vendorId").value(vendor.getId()))
                .andExpect(jsonPath("$.vendorCode").value("VND-001"))
                .andExpect(jsonPath("$.vendorName").value("PO Test Vendor"))
                .andExpect(jsonPath("$.locationId").value(warehouse.getId()))
                .andExpect(jsonPath("$.locationCode").value("WH-001"))
                .andExpect(jsonPath("$.locationName").value("Main Warehouse"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.orderDate").value("2026-04-15"))
                .andExpect(jsonPath("$.expectedDate").value("2026-05-15"))
                .andExpect(jsonPath("$.currency").value("UZS"))
                .andExpect(jsonPath("$.paymentTerms").value("Net 30"))
                .andExpect(jsonPath("$.shippingMethod").value("Truck"))
                .andExpect(jsonPath("$.shippingAddress").value("123 Test St"))
                .andExpect(jsonPath("$.notes").value("Test PO notes"))
                .andExpect(jsonPath("$.internalNotes").value("Internal only"))
                .andExpect(jsonPath("$.vendorReference").value("VREF-001"))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[0].productSku").value("PO-PRD-001"))
                .andExpect(jsonPath("$.lines[0].quantity").value(50))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(5000))
                .andExpect(jsonPath("$.lines[1].productSku").value("PO-PRD-002"))
                .andExpect(jsonPath("$.lines[1].quantity").value(25))
                .andExpect(jsonPath("$.lines[1].unitPrice").value(10000))
                .andExpect(jsonPath("$.subtotal").value(greaterThan(0)))
                .andExpect(jsonPath("$.totalAmount").value(greaterThan(0)));
    }

    @Test
    void createPurchaseOrder_missingLines_returns400() throws Exception {
        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .vendorId(vendor.getId())
                .locationId(warehouse.getId())
                .orderDate(LocalDate.of(2026, 4, 15))
                .lines(List.of()) // empty lines
                .build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /{id}/submit ----

    @Test
    void submitPurchaseOrder_draftToPending() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + draftPO.getId() + "/submit").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draftPO.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ---- PUT /{id}/approve ----

    @Test
    void approvePurchaseOrder_pendingToApproved() throws Exception {
        // First submit it to PENDING
        mockMvc.perform(put(BASE_URL + "/" + draftPO.getId() + "/submit").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Then approve
        mockMvc.perform(put(BASE_URL + "/" + draftPO.getId() + "/approve").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draftPO.getId()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value(user.getId()))
                .andExpect(jsonPath("$.approvedAt").isNotEmpty());
    }

    // ---- PUT /{id}/cancel ----

    @Test
    void cancelPurchaseOrder_returnsUpdated() throws Exception {
        Map<String, String> body = Map.of("reason", "No longer needed");

        mockMvc.perform(put(BASE_URL + "/" + draftPO.getId() + "/cancel").with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draftPO.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledBy").value(user.getId()))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty())
                .andExpect(jsonPath("$.cancellationReason").value("No longer needed"));
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + draftPO.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/status/DRAFT").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/vendor/" + vendor.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/search").param("q", "test").with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreatePurchaseOrderRequest createRequest = buildValidCreateRequest();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + draftPO.getId() + "/submit").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + draftPO.getId() + "/approve").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + draftPO.getId() + "/cancel").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "test"))))
                .andExpect(status().isForbidden());
    }

    // ---- Full lifecycle ----

    @Test
    void fullLifecycle_createSubmitApprove() throws Exception {
        CreatePurchaseOrderRequest request = buildValidCreateRequest();

        // Step 1: Create PO
        String createResult = mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.poNumber").isString())
                .andReturn().getResponse().getContentAsString();

        Long poId = objectMapper.readTree(createResult).get("id").asLong();

        // Step 2: Verify it appears in the list
        mockMvc.perform(get(BASE_URL).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(2)));

        // Step 3: Submit for approval
        mockMvc.perform(put(BASE_URL + "/" + poId + "/submit").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Step 4: Verify status filter
        mockMvc.perform(get(BASE_URL + "/status/PENDING").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));

        // Step 5: Approve
        mockMvc.perform(put(BASE_URL + "/" + poId + "/approve").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value(user.getId()))
                .andExpect(jsonPath("$.approvedAt").isNotEmpty());

        // Step 6: Fetch by ID and verify final state
        mockMvc.perform(get(BASE_URL + "/" + poId).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.subtotal").value(greaterThan(0)))
                .andExpect(jsonPath("$.totalAmount").value(greaterThan(0)));
    }
}
