package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.dto.CreateReceivingLineRequest;
import com.hisobnoma.platform.inventory.dto.CreateReceivingRequest;
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
class ReceivingControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ReceivingOrderRepository receivingOrderRepository;
    @Autowired private ReceivingLineRepository receivingLineRepository;
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

    private static final String BASE_URL = "/api/v1/inventory/receiving";

    private Tenant tenant;
    private User user;
    private UnitOfMeasure uom;
    private Vendor vendor;
    private Location warehouse;
    private Product product;
    private PurchaseOrder approvedPO;
    private PurchaseOrderLine poLine;
    private ReceivingOrder receivingOrder;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Receiving Test Tenant").code("FULLFLOW_RCV").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("rcvtestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        uom = unitOfMeasureRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS").name("Pieces").symbol("pcs")
                .isBaseUnit(true).active(true)
                .tenantId(tenant.getId()).build());

        vendor = vendorRepository.saveAndFlush(Vendor.builder()
                .code("VND-001").name("Receiving Vendor")
                .active(true)
                .tenantId(tenant.getId()).build());

        warehouse = locationRepository.saveAndFlush(Location.builder()
                .code("WH-001").name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true)
                .tenantId(tenant.getId()).build());

        product = productRepository.saveAndFlush(Product.builder()
                .sku("RCV-001").name("Receiving Product")
                .baseUom(uom)
                .sellingPrice(new BigDecimal("10000"))
                .costPrice(new BigDecimal("5000"))
                .active(true)
                .tenantId(tenant.getId()).build());

        // Create an APPROVED PurchaseOrder with one line
        approvedPO = purchaseOrderRepository.saveAndFlush(PurchaseOrder.builder()
                .poNumber("PO-000001")
                .vendor(vendor)
                .location(warehouse)
                .status(POStatus.APPROVED)
                .orderDate(LocalDate.of(2026, 4, 1))
                .tenantId(tenant.getId())
                .build());

        poLine = purchaseOrderLineRepository.saveAndFlush(PurchaseOrderLine.builder()
                .purchaseOrder(approvedPO)
                .product(product)
                .lineNumber(1)
                .quantity(new BigDecimal("100"))
                .uom(uom)
                .unitPrice(new BigDecimal("5000"))
                .tenantId(tenant.getId())
                .build());

        // Create a DRAFT receiving order for GET/cancel tests
        receivingOrder = receivingOrderRepository.saveAndFlush(ReceivingOrder.builder()
                .receivingNumber("RCV-202604-0001")
                .vendor(vendor)
                .location(warehouse)
                .status(ReceivingStatus.DRAFT)
                .receivingDate(LocalDate.of(2026, 4, 15))
                .tenantId(tenant.getId())
                .build());

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
        return authWith("INVENTORY_RECEIVING_VIEW");
    }

    private RequestPostProcessor createAuth() {
        return authWith("INVENTORY_RECEIVING_VIEW", "INVENTORY_RECEIVING_CREATE");
    }

    private RequestPostProcessor confirmAuth() {
        return authWith("INVENTORY_RECEIVING_VIEW", "INVENTORY_RECEIVING_CONFIRM");
    }

    private RequestPostProcessor fullAuth() {
        return authWith("INVENTORY_RECEIVING_VIEW", "INVENTORY_RECEIVING_CREATE", "INVENTORY_RECEIVING_CONFIRM");
    }

    private RequestPostProcessor noPermAuth() {
        return authWith("SOME_OTHER_PERM");
    }

    // ---- GET / (paginated) ----

    @Test
    void getReceivingOrders_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.receivingNumber == 'RCV-202604-0001')]").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- GET /{id} ----

    @Test
    void getReceivingOrderById_returnsOrder() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + receivingOrder.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(receivingOrder.getId()))
                .andExpect(jsonPath("$.receivingNumber").value("RCV-202604-0001"))
                .andExpect(jsonPath("$.vendorId").value(vendor.getId()))
                .andExpect(jsonPath("$.vendorName").value("Receiving Vendor"))
                .andExpect(jsonPath("$.locationId").value(warehouse.getId()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.receivingDate").value("2026-04-15"));
    }

    @Test
    void getReceivingOrderById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(viewAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /status/{status} ----

    @Test
    void getReceivingOrdersByStatus_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/status/DRAFT").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.receivingNumber == 'RCV-202604-0001')]").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));

        // No COMPLETED orders exist
        mockMvc.perform(get(BASE_URL + "/status/COMPLETED").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    // ---- GET /search?q={query} ----

    @Test
    void searchReceivingOrders_returnsResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("q", "RCV-202604").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.receivingNumber == 'RCV-202604-0001')]").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- POST / (create without PO) ----

    @Test
    void createReceivingOrder_validRequest_returnsCreated() throws Exception {
        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .vendorId(vendor.getId())
                .locationId(warehouse.getId())
                .receivingDate(LocalDate.of(2026, 4, 20))
                .vendorDeliveryNote("DN-001")
                .vendorInvoiceNumber("INV-001")
                .vendorInvoiceDate(LocalDate.of(2026, 4, 20))
                .currency("UZS")
                .notes("Test receiving order")
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .productId(product.getId())
                        .receivedQuantity(new BigDecimal("10"))
                        .uomId(uom.getId())
                        .unitCost(new BigDecimal("5000"))
                        .description("Test line")
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL).with(createAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receivingNumber").value(startsWith("RCV-")))
                .andExpect(jsonPath("$.vendorId").value(vendor.getId()))
                .andExpect(jsonPath("$.vendorName").value("Receiving Vendor"))
                .andExpect(jsonPath("$.locationId").value(warehouse.getId()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.receivingDate").value("2026-04-20"))
                .andExpect(jsonPath("$.vendorDeliveryNote").value("DN-001"))
                .andExpect(jsonPath("$.vendorInvoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.vendorInvoiceDate").value("2026-04-20"))
                .andExpect(jsonPath("$.currency").value("UZS"))
                .andExpect(jsonPath("$.notes").value("Test receiving order"))
                .andExpect(jsonPath("$.subtotal").value(50000))
                .andExpect(jsonPath("$.totalAmount").value(50000))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.lines[0].receivedQuantity").value(10))
                .andExpect(jsonPath("$.lines[0].unitCost").value(5000));
    }

    // ---- POST / (create with PO) ----

    @Test
    void createReceivingOrder_withPO_returnsCreated() throws Exception {
        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .purchaseOrderId(approvedPO.getId())
                .vendorId(vendor.getId())
                .locationId(warehouse.getId())
                .receivingDate(LocalDate.of(2026, 4, 22))
                .currency("UZS")
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .poLineId(poLine.getId())
                        .productId(product.getId())
                        .expectedQuantity(new BigDecimal("100"))
                        .receivedQuantity(new BigDecimal("50"))
                        .uomId(uom.getId())
                        .unitCost(new BigDecimal("5000"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL).with(createAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receivingNumber").value(startsWith("RCV-")))
                .andExpect(jsonPath("$.purchaseOrderId").value(approvedPO.getId()))
                .andExpect(jsonPath("$.vendorId").value(vendor.getId()))
                .andExpect(jsonPath("$.locationId").value(warehouse.getId()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.receivingDate").value("2026-04-22"))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].poLineId").value(poLine.getId()))
                .andExpect(jsonPath("$.lines[0].expectedQuantity").value(100))
                .andExpect(jsonPath("$.lines[0].receivedQuantity").value(50))
                .andExpect(jsonPath("$.lines[0].unitCost").value(5000));
    }

    // ---- GET /purchase-order/{poId} ----

    @Test
    void getReceivingOrdersForPO_returnsLinkedOrders() throws Exception {
        // First create a receiving order linked to the PO
        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .purchaseOrderId(approvedPO.getId())
                .vendorId(vendor.getId())
                .locationId(warehouse.getId())
                .receivingDate(LocalDate.of(2026, 4, 22))
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .productId(product.getId())
                        .receivedQuantity(new BigDecimal("10"))
                        .uomId(uom.getId())
                        .unitCost(new BigDecimal("5000"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL).with(createAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Now query by PO id
        mockMvc.perform(get(BASE_URL + "/purchase-order/" + approvedPO.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].purchaseOrderId").value(approvedPO.getId()));
    }

    // ---- PUT /{id}/confirm ----

    @Test
    void confirmReceivingOrder_updatesStatus() throws Exception {
        // Create a fresh receiving order with lines to confirm
        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .vendorId(vendor.getId())
                .locationId(warehouse.getId())
                .receivingDate(LocalDate.of(2026, 4, 25))
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .productId(product.getId())
                        .receivedQuantity(new BigDecimal("5"))
                        .uomId(uom.getId())
                        .unitCost(new BigDecimal("5000"))
                        .build()))
                .build();

        // Create the order
        String createResponse = mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(createResponse).get("id").asLong();

        // Confirm the order
        mockMvc.perform(put(BASE_URL + "/" + createdId + "/confirm").with(confirmAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.stockUpdated").value(true));
    }

    // ---- PUT /{id}/cancel ----

    @Test
    void cancelReceivingOrder_updatesStatus() throws Exception {
        Map<String, String> body = Map.of("reason", "No longer needed");

        mockMvc.perform(put(BASE_URL + "/" + receivingOrder.getId() + "/cancel").with(confirmAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(receivingOrder.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("No longer needed"));
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        // GET endpoints require INVENTORY_RECEIVING_VIEW
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + receivingOrder.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/status/DRAFT").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/purchase-order/" + approvedPO.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/search").param("q", "RCV").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST requires INVENTORY_RECEIVING_CREATE
        CreateReceivingRequest createRequest = CreateReceivingRequest.builder()
                .vendorId(vendor.getId())
                .locationId(warehouse.getId())
                .receivingDate(LocalDate.of(2026, 4, 20))
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .productId(product.getId())
                        .receivedQuantity(new BigDecimal("1"))
                        .uomId(uom.getId())
                        .unitCost(new BigDecimal("5000"))
                        .build()))
                .build();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        // PUT confirm/cancel requires INVENTORY_RECEIVING_CONFIRM
        mockMvc.perform(put(BASE_URL + "/" + receivingOrder.getId() + "/confirm").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + receivingOrder.getId() + "/cancel").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "test"))))
                .andExpect(status().isForbidden());
    }
}
