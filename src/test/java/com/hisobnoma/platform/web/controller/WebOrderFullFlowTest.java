package com.hisobnoma.platform.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import com.hisobnoma.platform.delivery.repository.DeliveryRegionRepository;
import com.hisobnoma.platform.delivery.repository.DeliveryVillageRepository;
import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import com.hisobnoma.platform.web.dto.CheckoutRequest;
import com.hisobnoma.platform.web.dto.UpdateOrderStatusRequest;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebOrderFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private WebCatalogItemRepository catalogRepository;
    @Autowired private WebOrderRepository orderRepository;
    @Autowired private DeliveryRegionRepository regionRepository;
    @Autowired private DeliveryVillageRepository villageRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ARInvoiceRepository arInvoiceRepository;

    private static final String PUBLIC_URL = "/api/v1/web/orders";
    private static final String ADMIN_URL = "/api/v1/web-orders";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    /**
     * The in-memory rate limiter is an application-scoped singleton keyed by
     * ip|phone; each test uses a unique phone so tests never throttle each other.
     */
    private static final AtomicLong PHONE_SEQ = new AtomicLong(7000000);

    private Tenant tenant;
    private User adminUser;
    private WebCatalogItem liveItem;
    private WebCatalogItem draftItem;
    private DeliveryRegion region;
    private DeliveryVillage village;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Web Order Tenant").code("FULLFLOW_WEBORD").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("weborderadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("WOPCS").name("Dona").active(true).isBaseUnit(true)
                .tenantId(tenant.getId()).build());

        Product liveProduct = productRepository.saveAndFlush(Product.builder()
                .sku("WO-SKU-001").name("Cola Bottle").baseUom(uom)
                .sellingPrice(new BigDecimal("12000.0000"))
                .active(true).sellable(true).trackInventory(false)
                .tenantId(tenant.getId()).build());

        Product draftProduct = productRepository.saveAndFlush(Product.builder()
                .sku("WO-SKU-002").name("Hidden Juice").baseUom(uom)
                .sellingPrice(new BigDecimal("8000.0000"))
                .active(true).sellable(true).trackInventory(false)
                .tenantId(tenant.getId()).build());

        liveItem = catalogRepository.saveAndFlush(WebCatalogItem.builder()
                .product(liveProduct).status(WebCatalogStatus.LIVE).sortOrder(1)
                .tenantId(tenant.getId()).build());

        draftItem = catalogRepository.saveAndFlush(WebCatalogItem.builder()
                .product(draftProduct).status(WebCatalogStatus.DRAFT).sortOrder(2)
                .tenantId(tenant.getId()).build());

        region = regionRepository.saveAndFlush(DeliveryRegion.builder()
                .name("Тошкент").active(true).sortOrder(1).tenantId(tenant.getId()).build());

        village = villageRepository.saveAndFlush(DeliveryVillage.builder()
                .name("Чилонзор").region(region).active(true).sortOrder(1)
                .tenantId(tenant.getId()).build());
    }

    private String uniquePhone() {
        return "+99890" + PHONE_SEQ.incrementAndGet();
    }

    private CheckoutRequest checkoutRequest(String phone, Long catalogItemId, String qty) {
        return CheckoutRequest.builder()
                .customerName("Ali Valiyev")
                .phone(phone)
                .regionId(region.getId())
                .villageId(village.getId())
                .note("Тезроқ керак")
                .lines(List.of(CheckoutRequest.CheckoutLine.builder()
                        .catalogItemId(catalogItemId)
                        .quantity(new BigDecimal(qty))
                        .build()))
                .build();
    }

    private RequestPostProcessor manageAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("WEB_ORDER_VIEW"),
                        new SimpleGrantedAuthority("WEB_ORDER_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor viewOnlyAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("WEB_ORDER_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private Long checkoutAndGetOrderId(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post(PUBLIC_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                checkoutRequest(phone, liveItem.getId(), "2"))))
                .andExpect(status().isCreated())
                .andReturn();
        String orderNumber = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/orderNumber").asText();
        return orderRepository.findByTenantIdAndOrderNumber(tenant.getId(), orderNumber)
                .orElseThrow().getId();
    }

    // ---- Public: checkout ----

    @Test
    void checkout_anonymousCreatesOrderWithServerPrices() throws Exception {
        mockMvc.perform(post(PUBLIC_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                checkoutRequest(uniquePhone(), liveItem.getId(), "3"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderNumber", startsWith("WO-")))
                .andExpect(jsonPath("$.data.status", is("NEW")))
                .andExpect(jsonPath("$.data.totalAmount", closeTo(36000.0, 0.01)))
                .andExpect(jsonPath("$.data.lines[0].unitPrice", closeTo(12000.0, 0.01)));
    }

    @Test
    void checkout_draftCatalogItemReturns400() throws Exception {
        mockMvc.perform(post(PUBLIC_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                checkoutRequest(uniquePhone(), draftItem.getId(), "1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_invalidQuantityReturns400() throws Exception {
        mockMvc.perform(post(PUBLIC_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                checkoutRequest(uniquePhone(), liveItem.getId(), "999999"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_missingNameReturns400() throws Exception {
        CheckoutRequest request = checkoutRequest(uniquePhone(), liveItem.getId(), "1");
        request.setCustomerName("  ");

        mockMvc.perform(post(PUBLIC_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_burstFromSamePhoneIsRateLimited() throws Exception {
        String phone = uniquePhone();
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(PUBLIC_URL)
                            .header(TENANT_HEADER, tenant.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    checkoutRequest(phone, liveItem.getId(), "1"))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post(PUBLIC_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                checkoutRequest(phone, liveItem.getId(), "1"))))
                .andExpect(status().isTooManyRequests());
    }

    // ---- Public: status lookup & delivery ----

    @Test
    void orderStatusLookup_requiresMatchingPhone() throws Exception {
        String phone = uniquePhone();
        checkoutAndGetOrderId(phone);
        String orderNumber = orderRepository.findAllByTenant(tenant.getId(),
                org.springframework.data.domain.PageRequest.of(0, 1)).getContent().get(0).getOrderNumber();

        mockMvc.perform(get(PUBLIC_URL + "/" + orderNumber)
                        .header(TENANT_HEADER, tenant.getId())
                        .param("phone", phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("NEW")));

        mockMvc.perform(get(PUBLIC_URL + "/" + orderNumber)
                        .header(TENANT_HEADER, tenant.getId())
                        .param("phone", "+998907777777"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deliveryEndpoints_anonymousReturnRegionsAndVillages() throws Exception {
        mockMvc.perform(get("/api/v1/web/delivery/regions")
                        .header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Тошкент")));

        mockMvc.perform(get("/api/v1/web/delivery/villages")
                        .header(TENANT_HEADER, tenant.getId())
                        .param("regionId", region.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Чилонзор")));
    }

    // ---- Staff: security ----

    @Test
    void adminEndpoints_anonymousIsRejected() throws Exception {
        mockMvc.perform(get(ADMIN_URL)).andExpect(status().isForbidden());
    }

    @Test
    void adminMutations_viewOnlyPermissionGets403() throws Exception {
        Long orderId = checkoutAndGetOrderId(uniquePhone());

        mockMvc.perform(post(ADMIN_URL + "/" + orderId + "/status")
                        .with(viewOnlyAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateOrderStatusRequest.builder()
                                        .status(WebOrderStatus.CONFIRMED).build())))
                .andExpect(status().isForbidden());
    }

    // ---- Staff: inbox & lifecycle ----

    @Test
    void staffInbox_showsNewOrderWithDeliveryAndCount() throws Exception {
        checkoutAndGetOrderId(uniquePhone());

        mockMvc.perform(get(ADMIN_URL).with(viewOnlyAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("NEW")))
                .andExpect(jsonPath("$.content[0].deliveryRegionName", is("Тошкент")))
                .andExpect(jsonPath("$.content[0].deliveryVillageName", is("Чилонзор")))
                .andExpect(jsonPath("$.content[0].customerNote", is("Тезроқ керак")));

        mockMvc.perform(get(ADMIN_URL + "/counts/new").with(viewOnlyAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newOrders", is(1)));
    }

    @Test
    void confirmOrder_changesStatus() throws Exception {
        Long orderId = checkoutAndGetOrderId(uniquePhone());

        mockMvc.perform(post(ADMIN_URL + "/" + orderId + "/status")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateOrderStatusRequest.builder()
                                        .status(WebOrderStatus.CONFIRMED).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));
    }

    @Test
    void invalidStatusTransition_returns400() throws Exception {
        Long orderId = checkoutAndGetOrderId(uniquePhone());

        mockMvc.perform(post(ADMIN_URL + "/" + orderId + "/status")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateOrderStatusRequest.builder()
                                        .status(WebOrderStatus.COMPLETED).build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void convertToInvoice_createsCustomerAndInvoiceWithMatchingTotals() throws Exception {
        Long orderId = checkoutAndGetOrderId(uniquePhone());

        mockMvc.perform(post(ADMIN_URL + "/" + orderId + "/convert-to-invoice")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.arInvoiceId", notNullValue()))
                .andExpect(jsonPath("$.data.arInvoiceNumber", notNullValue()))
                .andExpect(jsonPath("$.data.customerId", notNullValue()));

        WebOrder order = orderRepository.findByIdAndTenantId(orderId, tenant.getId()).orElseThrow();
        ARInvoice invoice = arInvoiceRepository.findById(order.getArInvoiceId()).orElseThrow();
        assertEquals(0, order.getTotalAmount().compareTo(invoice.getTotalAmount()));
        assertEquals(1, invoice.getLines().size());
        assertEquals(0, new BigDecimal("12000").compareTo(invoice.getLines().get(0).getUnitPrice()));

        // Customer auto-created from order contact data
        assertTrue(customerRepository.findById(order.getCustomerId()).isPresent());
        assertEquals("Ali Valiyev",
                customerRepository.findById(order.getCustomerId()).orElseThrow().getName());
    }

    @Test
    void convertTwice_returns400() throws Exception {
        Long orderId = checkoutAndGetOrderId(uniquePhone());

        mockMvc.perform(post(ADMIN_URL + "/" + orderId + "/convert-to-invoice")
                        .with(manageAuth()))
                .andExpect(status().isOk());

        mockMvc.perform(post(ADMIN_URL + "/" + orderId + "/convert-to-invoice")
                        .with(manageAuth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelledOrder_cannotBeConverted() throws Exception {
        Long orderId = checkoutAndGetOrderId(uniquePhone());

        mockMvc.perform(post(ADMIN_URL + "/" + orderId + "/status")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateOrderStatusRequest.builder()
                                        .status(WebOrderStatus.CANCELLED)
                                        .reason("Мижоз бекор қилди").build())))
                .andExpect(status().isOk());

        mockMvc.perform(post(ADMIN_URL + "/" + orderId + "/convert-to-invoice")
                        .with(manageAuth()))
                .andExpect(status().isBadRequest());
    }
}
