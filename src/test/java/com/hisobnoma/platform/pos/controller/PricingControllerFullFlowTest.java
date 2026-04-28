package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import com.hisobnoma.platform.pos.dto.ApplyCouponRequest;
import com.hisobnoma.platform.pos.dto.PriceCalculationRequest;
import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.CouponStatus;
import com.hisobnoma.platform.pos.enums.PromotionScope;
import com.hisobnoma.platform.pos.enums.PromotionType;
import com.hisobnoma.platform.pos.repository.CouponRepository;
import com.hisobnoma.platform.pos.repository.PromotionRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PricingControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/pos/pricing";

    private Tenant tenant;
    private User user;
    private Product product;
    private Promotion promotion;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Pricing Test Tenant").code("PRICING_FLOW").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("pricingtestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        Category category = categoryRepository.saveAndFlush(Category.builder()
                .code("CAT-PRICING").name("Pricing Test Category")
                .active(true).tenantId(tenant.getId()).build());

        UnitOfMeasure uom = uomRepository.saveAndFlush(UnitOfMeasure.builder()
                .code("PCS-PR").name("Pieces").symbol("pcs")
                .isBaseUnit(true).conversionFactor(BigDecimal.ONE)
                .active(true).tenantId(tenant.getId()).build());

        product = productRepository.saveAndFlush(Product.builder()
                .sku("PROD-PRICING-001")
                .name("Pricing Test Product")
                .sellingPrice(new BigDecimal("50000.0000"))
                .costPrice(new BigDecimal("30000.0000"))
                .category(category)
                .baseUom(uom)
                .active(true)
                .tenantId(tenant.getId())
                .build());

        promotion = promotionRepository.saveAndFlush(Promotion.builder()
                .code("PROMO-SAVE10")
                .name("Save 10%")
                .description("10% off with coupon")
                .type(PromotionType.PERCENTAGE_OFF)
                .scope(PromotionScope.ORDER)
                .discountValue(new BigDecimal("10.0000"))
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().plusDays(30))
                .active(true)
                .requiresCoupon(true)
                .tenantId(tenant.getId())
                .build());

        coupon = couponRepository.saveAndFlush(Coupon.builder()
                .code("SAVE10")
                .promotion(promotion)
                .status(CouponStatus.ACTIVE)
                .description("10% off coupon")
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(60))
                .maxUses(100)
                .currentUses(0)
                .maxUsesPerCustomer(5)
                .tenantId(tenant.getId())
                .build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor pricingAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("POS_PRICING_CALCULATE"),
                        new SimpleGrantedAuthority("POS_COUPON_APPLY"),
                        new SimpleGrantedAuthority("POS_COUPON_REDEEM")));
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

    // ---- GET /product/{productId} ----

    @Test
    void getProductPrice_returnsPrice() throws Exception {
        mockMvc.perform(get(BASE_URL + "/product/" + product.getId())
                        .with(pricingAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void getProductPrice_withQuantity_returnsPrice() throws Exception {
        mockMvc.perform(get(BASE_URL + "/product/" + product.getId())
                        .param("quantity", "5")
                        .with(pricingAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    // ---- POST /calculate ----

    @Test
    void calculatePrices_returnsResult() throws Exception {
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .items(List.of(
                        PriceCalculationRequest.PriceCalculationItem.builder()
                                .productId(product.getId())
                                .quantity(new BigDecimal("2"))
                                .build()
                ))
                .applyPromotions(true)
                .build();

        mockMvc.perform(post(BASE_URL + "/calculate")
                        .with(pricingAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.subtotal").isNumber())
                .andExpect(jsonPath("$.grandTotal").isNumber())
                .andExpect(jsonPath("$.totalDiscount").isNumber())
                .andExpect(jsonPath("$.taxAmount").isNumber());
    }

    // ---- POST /apply-coupon ----

    @Test
    void applyCoupon_validCoupon_returnsDiscount() throws Exception {
        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .couponCode("SAVE10")
                .orderTotal(new BigDecimal("100000"))
                .build();

        mockMvc.perform(post(BASE_URL + "/apply-coupon")
                        .with(pricingAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.couponCode").value("SAVE10"))
                .andExpect(jsonPath("$.discountAmount").isNumber())
                .andExpect(jsonPath("$.promotionId").value(promotion.getId()))
                .andExpect(jsonPath("$.promotionName").value("Save 10%"));
    }

    @Test
    void applyCoupon_invalidCoupon_returnsNotValid() throws Exception {
        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .couponCode("INVALID_CODE")
                .orderTotal(new BigDecimal("100000"))
                .build();

        mockMvc.perform(post(BASE_URL + "/apply-coupon")
                        .with(pricingAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.couponCode").value("INVALID_CODE"));
    }

    // ---- POST /validate-coupon ----

    @Test
    void validateCoupon_validCode_returnsValid() throws Exception {
        mockMvc.perform(post(BASE_URL + "/validate-coupon")
                        .param("couponCode", "SAVE10")
                        .with(pricingAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.couponCode").value("SAVE10"))
                .andExpect(jsonPath("$.promotionId").value(promotion.getId()))
                .andExpect(jsonPath("$.promotionName").value("Save 10%"));
    }

    @Test
    void validateCoupon_invalidCode_returnsNotValid() throws Exception {
        mockMvc.perform(post(BASE_URL + "/validate-coupon")
                        .param("couponCode", "INVALID")
                        .with(pricingAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.couponCode").value("INVALID"));
    }

    // ---- POST /record-coupon-redemption ----

    @Test
    void recordCouponRedemption_recordsRedemption() throws Exception {
        mockMvc.perform(post(BASE_URL + "/record-coupon-redemption")
                        .param("couponCode", "SAVE10")
                        .param("customerId", "1")
                        .param("orderId", "1001")
                        .param("discountApplied", "10000")
                        .with(pricingAuth()))
                .andExpect(status().isOk());
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/product/" + product.getId())
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE_URL + "/calculate")
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        ApplyCouponRequest couponRequest = ApplyCouponRequest.builder()
                .couponCode("SAVE10")
                .orderTotal(new BigDecimal("100000"))
                .build();

        mockMvc.perform(post(BASE_URL + "/apply-coupon")
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(couponRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE_URL + "/validate-coupon")
                        .param("couponCode", "SAVE10")
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE_URL + "/record-coupon-redemption")
                        .param("couponCode", "SAVE10")
                        .param("customerId", "1")
                        .param("orderId", "1001")
                        .param("discountApplied", "10000")
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
