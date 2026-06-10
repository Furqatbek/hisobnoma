package com.hisobnoma.platform.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.entity.CouponRedemption;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.enums.PromotionType;
import com.hisobnoma.platform.pos.repository.CouponRedemptionRepository;
import com.hisobnoma.platform.pos.repository.CouponRepository;
import com.hisobnoma.platform.pos.repository.PromotionRepository;
import com.hisobnoma.platform.web.dto.CheckoutRequest;
import com.hisobnoma.platform.web.dto.UpdateOrderStatusRequest;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
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
 * Phase 2 (loyalty plan) end-to-end: coupons validated and applied at
 * checkout, redeemed with a web-order-linked audit row when staff confirm,
 * reversed (and usable again) when a confirmed order is cancelled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebCouponFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private WebCatalogItemRepository catalogRepository;
    @Autowired private WebOrderRepository orderRepository;
    @Autowired private WebCustomerRepository webCustomerRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponRedemptionRepository redemptionRepository;

    private static final String VALIDATE_URL = "/api/v1/web/cart/validate-coupon";
    private static final String CHECKOUT_URL = "/api/v1/web/orders";
    private static final String ADMIN_ORDERS_URL = "/api/v1/web-orders";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    private static final AtomicLong PHONE_SEQ = new AtomicLong(8400000);

    private Tenant tenant;
    private User adminUser;
    private WebCatalogItem liveItem;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Web Coupon Tenant").code("FULLFLOW_WEBCPN").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("webcouponadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("WCPCS").name("Dona").active(true).isBaseUnit(true)
                .tenantId(tenant.getId()).build());

        Product product = productRepository.saveAndFlush(Product.builder()
                .sku("WC-SKU-001").name("Cola Bottle").baseUom(uom)
                .sellingPrice(new BigDecimal("12000.0000"))
                .active(true).sellable(true).trackInventory(false)
                .tenantId(tenant.getId()).build());

        liveItem = catalogRepository.saveAndFlush(WebCatalogItem.builder()
                .product(product).status(WebCatalogStatus.LIVE).sortOrder(1)
                .tenantId(tenant.getId()).build());
    }

    private Coupon saveCoupon(String code, PromotionChannel channel, String fixedDiscount,
                              Integer maxUses, Integer maxUsesPerCustomer) {
        Promotion promotion = promotionRepository.saveAndFlush(Promotion.builder()
                .code(code + "-PROMO").name(code + " promo")
                .type(PromotionType.FIXED_AMOUNT_OFF)
                .discountValue(new BigDecimal(fixedDiscount))
                .channel(channel)
                .requiresCoupon(true)
                .tenantId(tenant.getId())
                .build());
        return couponRepository.saveAndFlush(Coupon.builder()
                .code(code).promotion(promotion)
                .maxUses(maxUses).maxUsesPerCustomer(maxUsesPerCustomer)
                .tenantId(tenant.getId())
                .build());
    }

    private String uniquePhone() {
        return "+99890" + PHONE_SEQ.incrementAndGet();
    }

    private CheckoutRequest checkoutRequest(String phone, String qty, String couponCode) {
        return CheckoutRequest.builder()
                .customerName("Ali Valiyev")
                .phone(phone)
                .couponCode(couponCode)
                .lines(List.of(CheckoutRequest.CheckoutLine.builder()
                        .catalogItemId(liveItem.getId())
                        .quantity(new BigDecimal(qty))
                        .build()))
                .build();
    }

    private String validateBody(String code, String qty) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "code", code,
                "lines", List.of(java.util.Map.of(
                        "catalogItemId", liveItem.getId(), "quantity", qty))));
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

    private Long checkout(String phone, String qty, String couponCode) throws Exception {
        MvcResult result = mockMvc.perform(post(CHECKOUT_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest(phone, qty, couponCode))))
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

    // ---- validate endpoint ----

    @Test
    void validate_validWebCouponReturnsDiscount() throws Exception {
        saveCoupon("WELCOME5K", PromotionChannel.WEB, "5000", null, null);

        mockMvc.perform(post(VALIDATE_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .header("X-Forwarded-For", "10.99.99.50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateBody("WELCOME5K", "3")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.discount", is(5000)));
    }

    @Test
    void validate_unknownAndPosChannelCouponsGetSameGenericOutcome() throws Exception {
        saveCoupon("POSONLY", PromotionChannel.POS, "5000", null, null);

        MvcResult unknown = mockMvc.perform(post(VALIDATE_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .header("X-Forwarded-For", "10.99.99.51")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateBody("NO-SUCH-CODE", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(false)))
                .andReturn();
        MvcResult wrongChannel = mockMvc.perform(post(VALIDATE_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .header("X-Forwarded-For", "10.99.99.51")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateBody("POSONLY", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(false)))
                .andReturn();

        // No oracle: both invalid reasons produce identical payloads (bar the echoed code)
        String a = objectMapper.readTree(unknown.getResponse().getContentAsString())
                .at("/data").toString().replace("NO-SUCH-CODE", "X");
        String b = objectMapper.readTree(wrongChannel.getResponse().getContentAsString())
                .at("/data").toString().replace("POSONLY", "X");
        assertEquals(a, b);
    }

    @Test
    void validate_burstIsRateLimited() throws Exception {
        // Strict checkout window: 5/min per IP — a dedicated IP avoids
        // throttling the other coupon tests.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(VALIDATE_URL)
                            .header(TENANT_HEADER, tenant.getId())
                            .header("X-Forwarded-For", "10.99.99.43")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validateBody("ANY", "1")))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post(VALIDATE_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .header("X-Forwarded-For", "10.99.99.43")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateBody("ANY", "1")))
                .andExpect(status().isTooManyRequests());
    }

    // ---- checkout ----

    @Test
    void checkout_validCouponDiscountsOrder() throws Exception {
        saveCoupon("WELCOME5K", PromotionChannel.WEB, "5000", null, null);

        mockMvc.perform(post(CHECKOUT_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                checkoutRequest(uniquePhone(), "3", "WELCOME5K"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.couponCode", is("WELCOME5K")))
                .andExpect(jsonPath("$.data.couponDiscount", is(5000)))
                .andExpect(jsonPath("$.data.totalAmount", closeTo(31000.0, 0.01)));
    }

    @Test
    void checkout_invalidCouponReturns400AndCreatesNothing() throws Exception {
        long before = orderRepository.count();

        mockMvc.perform(post(CHECKOUT_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                checkoutRequest(uniquePhone(), "3", "NO-SUCH-CODE"))))
                .andExpect(status().isBadRequest());

        assertEquals(before, orderRepository.count());
    }

    @Test
    void checkout_posChannelCouponRejected() throws Exception {
        saveCoupon("POSONLY", PromotionChannel.POS, "5000", null, null);

        mockMvc.perform(post(CHECKOUT_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                checkoutRequest(uniquePhone(), "3", "POSONLY"))))
                .andExpect(status().isBadRequest());
    }

    // ---- redemption lifecycle ----

    @Test
    void confirm_recordsRedemptionLinkedToWebOrder() throws Exception {
        Coupon coupon = saveCoupon("WELCOME5K", PromotionChannel.WEB, "5000", 10, null);
        Long orderId = checkout(uniquePhone(), "3", "WELCOME5K");

        assertEquals(0, couponRepository.findById(coupon.getId()).orElseThrow().getCurrentUses());

        updateStatus(orderId, WebOrderStatus.CONFIRMED, null);

        List<CouponRedemption> redemptions = redemptionRepository.findByWebOrderIdAndReversedFalse(orderId);
        assertEquals(1, redemptions.size());
        assertEquals(0, new BigDecimal("5000.0000").compareTo(redemptions.get(0).getDiscountAmount()));
        assertEquals(1, couponRepository.findById(coupon.getId()).orElseThrow().getCurrentUses());
        assertEquals(1, promotionRepository.findById(coupon.getPromotion().getId())
                .orElseThrow().getCurrentUses());
    }

    @Test
    void cancelAfterConfirm_reversesRedemptionAndCouponIsUsableAgain() throws Exception {
        Coupon coupon = saveCoupon("ONCE", PromotionChannel.WEB, "5000", 1, null);
        Long orderId = checkout(uniquePhone(), "3", "ONCE");

        updateStatus(orderId, WebOrderStatus.CONFIRMED, null);
        // Depleted: validating again fails
        mockMvc.perform(post(VALIDATE_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .header("X-Forwarded-For", "10.99.99.52")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateBody("ONCE", "1")))
                .andExpect(jsonPath("$.data.valid", is(false)));

        updateStatus(orderId, WebOrderStatus.CANCELLED, "Мижоз бекор қилди");

        assertTrue(redemptionRepository.findByWebOrderIdAndReversedFalse(orderId).isEmpty());
        Coupon released = couponRepository.findById(coupon.getId()).orElseThrow();
        assertEquals(0, released.getCurrentUses());
        // Usable again after the reversal re-activated it
        mockMvc.perform(post(VALIDATE_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .header("X-Forwarded-For", "10.99.99.52")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateBody("ONCE", "1")))
                .andExpect(jsonPath("$.data.valid", is(true)));
    }

    @Test
    void perCustomerLimit_blocksSecondUseForLinkedCustomer() throws Exception {
        saveCoupon("PERCUST", PromotionChannel.WEB, "5000", null, 1);
        String phone = uniquePhone();
        String digits = phone.replaceAll("[^0-9]", "");

        Customer arCustomer = customerRepository.saveAndFlush(Customer.builder()
                .code("WCCUST-1").name("Ali Valiyev").phone(phone)
                .tenantId(tenant.getId()).build());
        webCustomerRepository.saveAndFlush(WebCustomer.builder()
                .phone(digits).name("Ali").customerId(arCustomer.getId())
                .tenantId(tenant.getId()).build());

        Long orderId = checkout(phone, "1", "PERCUST");
        updateStatus(orderId, WebOrderStatus.CONFIRMED, null);

        // Second checkout with the same phone (linked customer) is rejected
        mockMvc.perform(post(CHECKOUT_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                checkoutRequest(phone, "1", "PERCUST"))))
                .andExpect(status().isBadRequest());

        // A different (unlinked) phone only sees the coupon's global limits
        mockMvc.perform(post(CHECKOUT_URL)
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                checkoutRequest(uniquePhone(), "1", "PERCUST"))))
                .andExpect(status().isCreated());
    }

    @Test
    void staffOrder_showsCouponCodeAndDiscount() throws Exception {
        saveCoupon("WELCOME5K", PromotionChannel.WEB, "5000", null, null);
        Long orderId = checkout(uniquePhone(), "3", "WELCOME5K");

        mockMvc.perform(get(ADMIN_ORDERS_URL + "/" + orderId).with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.couponCode", is("WELCOME5K")))
                .andExpect(jsonPath("$.data.couponDiscount", is(5000)));
    }

    @Test
    void convertToInvoice_couponDiscountInHeaderTotalsMatch() throws Exception {
        saveCoupon("WELCOME5K", PromotionChannel.WEB, "5000", null, null);
        Long orderId = checkout(uniquePhone(), "3", "WELCOME5K");

        mockMvc.perform(post(ADMIN_ORDERS_URL + "/" + orderId + "/convert-to-invoice")
                        .with(manageAuth()))
                .andExpect(status().isOk());

        WebOrder order = orderRepository.findByIdAndTenantId(orderId, tenant.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("31000.0000").compareTo(order.getTotalAmount()));
        assertNotNull(order.getArInvoiceId());
    }
}
