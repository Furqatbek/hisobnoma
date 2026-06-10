package com.hisobnoma.platform.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.enums.PromotionType;
import com.hisobnoma.platform.pos.repository.PromotionRepository;
import com.hisobnoma.platform.web.dto.CartPriceRequest;
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

/**
 * Phase 1 (loyalty plan) end-to-end: a staff-created WEB promotion is visible
 * as a catalog sale badge, discounts the cart preview and checkout, lands on
 * the staff order with its code, and tracks usage on confirm/cancel — while
 * POS-channel promotions stay invisible to every web endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebPromotionFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private WebCatalogItemRepository catalogRepository;
    @Autowired private WebOrderRepository orderRepository;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private com.hisobnoma.platform.finance.repository.ARInvoiceRepository arInvoiceRepository;

    private static final String CATALOG_URL = "/api/v1/web/catalog/products";
    private static final String CART_PRICE_URL = "/api/v1/web/cart/price";
    private static final String CHECKOUT_URL = "/api/v1/web/orders";
    private static final String ADMIN_ORDERS_URL = "/api/v1/web-orders";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    private static final AtomicLong PHONE_SEQ = new AtomicLong(8200000);

    private Tenant tenant;
    private User adminUser;
    private WebCatalogItem liveItem;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Web Promo Tenant").code("FULLFLOW_WEBPROMO").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("webpromoadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("WPPCS").name("Dona").active(true).isBaseUnit(true)
                .tenantId(tenant.getId()).build());

        Product product = productRepository.saveAndFlush(Product.builder()
                .sku("WP-SKU-001").name("Cola Bottle").baseUom(uom)
                .sellingPrice(new BigDecimal("12000.0000"))
                .active(true).sellable(true).trackInventory(false)
                .tenantId(tenant.getId()).build());

        liveItem = catalogRepository.saveAndFlush(WebCatalogItem.builder()
                .product(product).status(WebCatalogStatus.LIVE).sortOrder(1)
                .tenantId(tenant.getId()).build());
    }

    private Promotion savePromotion(String code, PromotionChannel channel, String percent) {
        return promotionRepository.saveAndFlush(Promotion.builder()
                .code(code).name(code + " promo")
                .type(PromotionType.PERCENTAGE_OFF)
                .discountValue(new BigDecimal(percent))
                .channel(channel)
                .tenantId(tenant.getId())
                .build());
    }

    private String uniquePhone() {
        return "+99890" + PHONE_SEQ.incrementAndGet();
    }

    private CheckoutRequest checkoutRequest(String phone, String qty) {
        return CheckoutRequest.builder()
                .customerName("Ali Valiyev")
                .phone(phone)
                .lines(List.of(CheckoutRequest.CheckoutLine.builder()
                        .catalogItemId(liveItem.getId())
                        .quantity(new BigDecimal(qty))
                        .build()))
                .build();
    }

    private CartPriceRequest cartPriceRequest(String qty) {
        return CartPriceRequest.builder()
                .lines(List.of(CheckoutRequest.CheckoutLine.builder()
                        .catalogItemId(liveItem.getId())
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

    private Long checkout(String phone, String qty) throws Exception {
        MvcResult result = mockMvc.perform(post(CHECKOUT_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest(phone, qty))))
                .andExpect(status().isCreated())
                .andReturn();
        String orderNumber = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/orderNumber").asText();
        return orderRepository.findByTenantIdAndOrderNumber(tenant.getId(), orderNumber)
                .orElseThrow().getId();
    }

    private void updateStatus(Long orderId, WebOrderStatus status, String reason) throws Exception {
        mockMvc.perform(post(ADMIN_ORDERS_URL + "/" + orderId + "/status")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateOrderStatusRequest.builder()
                                        .status(status).reason(reason).build())))
                .andExpect(status().isOk());
    }

    // ---- catalog badge ----

    @Test
    void catalog_webPromotionShowsSaleBadge() throws Exception {
        savePromotion("WEB15", PromotionChannel.WEB, "15");

        mockMvc.perform(get(CATALOG_URL).header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price", closeTo(12000.0, 0.01)))
                .andExpect(jsonPath("$.content[0].salePrice", closeTo(10200.0, 0.01)))
                .andExpect(jsonPath("$.content[0].promotionLabel", is("-15%")));
    }

    @Test
    void catalog_posOnlyPromotionShowsNoBadge() throws Exception {
        savePromotion("POS15", PromotionChannel.POS, "15");

        mockMvc.perform(get(CATALOG_URL).header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].salePrice").doesNotExist())
                .andExpect(jsonPath("$.content[0].promotionLabel").doesNotExist());
    }

    @Test
    void catalog_allChannelPromotionShowsBadge() throws Exception {
        savePromotion("EVERYWHERE", PromotionChannel.ALL, "10");

        mockMvc.perform(get(CATALOG_URL).header(TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].salePrice", closeTo(10800.0, 0.01)))
                .andExpect(jsonPath("$.content[0].promotionLabel", is("-10%")));
    }

    // ---- cart price preview ----

    @Test
    void cartPrice_returnsDiscountAndPromotionName() throws Exception {
        savePromotion("WEB10", PromotionChannel.WEB, "10");

        mockMvc.perform(post(CART_PRICE_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartPriceRequest("3"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal", closeTo(36000.0, 0.01)))
                .andExpect(jsonPath("$.data.discountTotal", closeTo(3600.0, 0.01)))
                .andExpect(jsonPath("$.data.total", closeTo(32400.0, 0.01)))
                .andExpect(jsonPath("$.data.appliedPromotions", hasSize(1)))
                .andExpect(jsonPath("$.data.appliedPromotions[0]", is("WEB10 promo")))
                .andExpect(jsonPath("$.data.lines[0].unitPrice", closeTo(12000.0, 0.01)));
    }

    @Test
    void cartPrice_posPromotionDoesNotDiscount() throws Exception {
        savePromotion("POSONLY", PromotionChannel.POS, "10");

        mockMvc.perform(post(CART_PRICE_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartPriceRequest("3"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountTotal", is(0)))
                .andExpect(jsonPath("$.data.appliedPromotions", hasSize(0)));
    }

    @Test
    void cartPrice_emptyLinesReturns400() throws Exception {
        mockMvc.perform(post(CART_PRICE_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cartPrice_burstIsRateLimited() throws Exception {
        // 16 rapid calls span at most two 10s limiter buckets, so at least one
        // bucket receives more than the 5 allowed calls and returns 429. A
        // dedicated client IP keeps this burst from throttling other tests.
        int tooMany = 0;
        for (int i = 0; i < 16; i++) {
            MvcResult result = mockMvc.perform(post(CART_PRICE_URL)
                            .header(TENANT_HEADER, tenant.getId())
                            .header("X-Forwarded-For", "10.99.99.42")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cartPriceRequest("1"))))
                    .andReturn();
            if (result.getResponse().getStatus() == 429) {
                tooMany++;
            }
        }
        assertTrue(tooMany > 0, "expected at least one 429 from the preview rate limiter");
    }

    // ---- checkout ----

    @Test
    void checkout_webPromotionDiscountsOrderAndSnapshotsCode() throws Exception {
        savePromotion("WEB10", PromotionChannel.WEB, "10");

        MvcResult result = mockMvc.perform(post(CHECKOUT_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest(uniquePhone(), "3"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.discountTotal", closeTo(3600.0, 0.01)))
                .andExpect(jsonPath("$.data.totalAmount", closeTo(32400.0, 0.01)))
                // Line snapshots stay at full price
                .andExpect(jsonPath("$.data.lines[0].unitPrice", closeTo(12000.0, 0.01)))
                .andReturn();

        String orderNumber = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/orderNumber").asText();
        WebOrder order = orderRepository.findByTenantIdAndOrderNumber(tenant.getId(), orderNumber)
                .orElseThrow();
        assertEquals("WEB10", order.getAppliedPromotions());
        assertEquals(0, new BigDecimal("3600").compareTo(order.getDiscountTotal()));
    }

    @Test
    void checkout_posPromotionLeavesFullPrice() throws Exception {
        savePromotion("POSONLY", PromotionChannel.POS, "10");

        mockMvc.perform(post(CHECKOUT_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest(uniquePhone(), "3"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.discountTotal", is(0)))
                .andExpect(jsonPath("$.data.totalAmount", closeTo(36000.0, 0.01)));
    }

    // ---- staff view & usage tracking ----

    @Test
    void staffOrder_showsDiscountAndPromotionCodes() throws Exception {
        savePromotion("WEB10", PromotionChannel.WEB, "10");
        Long orderId = checkout(uniquePhone(), "3");

        mockMvc.perform(get(ADMIN_ORDERS_URL + "/" + orderId).with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountTotal", closeTo(3600.0, 0.01)))
                .andExpect(jsonPath("$.data.appliedPromotions", is("WEB10")))
                .andExpect(jsonPath("$.data.totalAmount", closeTo(32400.0, 0.01)));
    }

    @Test
    void confirm_incrementsPromotionUsage_cancelReleasesIt() throws Exception {
        Promotion promo = savePromotion("WEB10", PromotionChannel.WEB, "10");
        Long orderId = checkout(uniquePhone(), "3");

        assertEquals(0, promotionRepository.findById(promo.getId()).orElseThrow().getCurrentUses());

        updateStatus(orderId, WebOrderStatus.CONFIRMED, null);
        assertEquals(1, promotionRepository.findById(promo.getId()).orElseThrow().getCurrentUses());

        updateStatus(orderId, WebOrderStatus.CANCELLED, "Мижоз бекор қилди");
        assertEquals(0, promotionRepository.findById(promo.getId()).orElseThrow().getCurrentUses());
    }

    @Test
    void convertToInvoice_discountedOrderKeepsMatchingTotals() throws Exception {
        savePromotion("WEB10", PromotionChannel.WEB, "10");
        Long orderId = checkout(uniquePhone(), "3");

        mockMvc.perform(post(ADMIN_ORDERS_URL + "/" + orderId + "/convert-to-invoice")
                        .with(manageAuth()))
                .andExpect(status().isOk());

        WebOrder order = orderRepository.findByIdAndTenantId(orderId, tenant.getId()).orElseThrow();
        // 36000 - 3600 = 32400; invoice total must equal the order total
        assertEquals(0, new BigDecimal("32400.0000").compareTo(order.getTotalAmount()));
        assertNotNull(order.getArInvoiceId());

        com.hisobnoma.platform.finance.entity.ARInvoice invoice =
                arInvoiceRepository.findById(order.getArInvoiceId()).orElseThrow();
        assertEquals(0, order.getTotalAmount().compareTo(invoice.getTotalAmount()));
        // Lines stay at full snapshot price; the discount lives in the header
        assertEquals(0, new BigDecimal("3600.0000").compareTo(invoice.getDiscountAmount()));
        assertEquals(0, new BigDecimal("12000.0000").compareTo(invoice.getLines().get(0).getUnitPrice()));
    }
}
