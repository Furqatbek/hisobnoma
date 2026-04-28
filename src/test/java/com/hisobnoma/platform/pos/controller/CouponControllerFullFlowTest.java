package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.pos.dto.CreateCouponRequest;
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
class CouponControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CouponRepository couponRepository;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/pos/coupons";

    private Tenant tenant;
    private User user;
    private Promotion promotion;
    private Coupon activeCoupon;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Coupon Test Tenant").code("COUPON_FLOW").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("coupontestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        promotion = promotionRepository.saveAndFlush(Promotion.builder()
                .code("PROMO-10PCT")
                .name("10% Off Everything")
                .description("Test promotion")
                .type(PromotionType.PERCENTAGE_OFF)
                .scope(PromotionScope.ORDER)
                .discountValue(BigDecimal.TEN)
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().plusDays(30))
                .active(true)
                .tenantId(tenant.getId())
                .build());

        activeCoupon = couponRepository.saveAndFlush(Coupon.builder()
                .code("TESTCOUP01")
                .promotion(promotion)
                .status(CouponStatus.ACTIVE)
                .description("Test coupon")
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(60))
                .maxUses(100)
                .currentUses(0)
                .maxUsesPerCustomer(5)
                .customerEmail("test@example.com")
                .notes("Integration test coupon")
                .tenantId(tenant.getId())
                .build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("POS_COUPON_READ"),
                        new SimpleGrantedAuthority("POS_COUPON_CREATE"),
                        new SimpleGrantedAuthority("POS_COUPON_UPDATE"),
                        new SimpleGrantedAuthority("POS_COUPON_DELETE"),
                        new SimpleGrantedAuthority("POS_COUPON_GENERATE"),
                        new SimpleGrantedAuthority("POS_COUPON_REDEMPTIONS_VIEW")));
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
    void getAll_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("TESTCOUP01"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---- GET /promotion/{promotionId} ----

    @Test
    void getByPromotion_returnsCoupons() throws Exception {
        mockMvc.perform(get(BASE_URL + "/promotion/" + promotion.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("TESTCOUP01"))
                .andExpect(jsonPath("$.content[0].promotionId").value(promotion.getId()));
    }

    // ---- GET /status/{status} ----

    @Test
    void getByStatus_returnsFilteredCoupons() throws Exception {
        mockMvc.perform(get(BASE_URL + "/status/ACTIVE").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    // ---- GET /{id} ----

    @Test
    void getById_returnsCoupon() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + activeCoupon.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TESTCOUP01"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.promotionId").value(promotion.getId()))
                .andExpect(jsonPath("$.description").value("Test coupon"))
                .andExpect(jsonPath("$.maxUses").value(100))
                .andExpect(jsonPath("$.currentUses").value(0))
                .andExpect(jsonPath("$.maxUsesPerCustomer").value(5))
                .andExpect(jsonPath("$.customerEmail").value("test@example.com"))
                .andExpect(jsonPath("$.notes").value("Integration test coupon"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /code/{code} ----

    @Test
    void getByCode_returnsCoupon() throws Exception {
        mockMvc.perform(get(BASE_URL + "/code/TESTCOUP01").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TESTCOUP01"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.promotionId").value(promotion.getId()));
    }

    // ---- POST / ----

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("NEWCOUPON01")
                .promotionId(promotion.getId())
                .description("Newly created coupon")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(90))
                .maxUses(50)
                .maxUsesPerCustomer(2)
                .customerEmail("new@example.com")
                .minimumOrderAmount(BigDecimal.valueOf(25))
                .notes("Created via test")
                .build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("NEWCOUPON01"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.promotionId").value(promotion.getId()))
                .andExpect(jsonPath("$.description").value("Newly created coupon"))
                .andExpect(jsonPath("$.maxUses").value(50))
                .andExpect(jsonPath("$.maxUsesPerCustomer").value(2))
                .andExpect(jsonPath("$.notes").value("Created via test"));
    }

    // ---- POST /generate/{promotionId}?count=3 ----

    @Test
    void generateBulk_returnsMultipleCoupons() throws Exception {
        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("BULK")
                .promotionId(promotion.getId())
                .maxUses(10)
                .maxUsesPerCustomer(1)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .build();

        mockMvc.perform(post(BASE_URL + "/generate/" + promotion.getId())
                        .param("count", "3")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].status").value("ACTIVE"))
                .andExpect(jsonPath("$[2].status").value("ACTIVE"));
    }

    // ---- PUT /{id} ----

    @Test
    void update_validRequest_returnsUpdated() throws Exception {
        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("TESTCOUP01")
                .promotionId(promotion.getId())
                .description("Updated description")
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(120))
                .maxUses(200)
                .maxUsesPerCustomer(10)
                .customerEmail("updated@example.com")
                .notes("Updated notes")
                .build();

        mockMvc.perform(put(BASE_URL + "/" + activeCoupon.getId()).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activeCoupon.getId()))
                .andExpect(jsonPath("$.code").value("TESTCOUP01"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.maxUses").value(200))
                .andExpect(jsonPath("$.maxUsesPerCustomer").value(10))
                .andExpect(jsonPath("$.customerEmail").value("updated@example.com"))
                .andExpect(jsonPath("$.notes").value("Updated notes"));
    }

    // ---- DELETE /{id} ----

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + activeCoupon.getId()).with(fullAuth()))
                .andExpect(status().isNoContent());

        // Verify coupon no longer exists
        mockMvc.perform(get(BASE_URL + "/" + activeCoupon.getId()).with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- POST /{id}/activate ----

    @Test
    void activate_returnsActivated() throws Exception {
        // First deactivate the coupon
        mockMvc.perform(post(BASE_URL + "/" + activeCoupon.getId() + "/deactivate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        // Now activate it
        mockMvc.perform(post(BASE_URL + "/" + activeCoupon.getId() + "/activate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.code").value("TESTCOUP01"));
    }

    // ---- POST /{id}/deactivate ----

    @Test
    void deactivate_returnsDeactivated() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + activeCoupon.getId() + "/deactivate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.code").value("TESTCOUP01"));
    }

    // ---- POST /{id}/cancel ----

    @Test
    void cancel_returnsCancelled() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + activeCoupon.getId() + "/cancel").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.code").value("TESTCOUP01"));
    }

    // ---- GET /{id}/redemptions ----

    @Test
    void getRedemptions_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + activeCoupon.getId() + "/redemptions").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ---- POST /update-expired ----

    @Test
    void updateExpired_returnsOk() throws Exception {
        mockMvc.perform(post(BASE_URL + "/update-expired").with(fullAuth()))
                .andExpect(status().isOk());
    }

    // ---- POST /update-depleted ----

    @Test
    void updateDepleted_returnsOk() throws Exception {
        mockMvc.perform(post(BASE_URL + "/update-depleted").with(fullAuth()))
                .andExpect(status().isOk());
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        // GET /
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /{id}
        mockMvc.perform(get(BASE_URL + "/" + activeCoupon.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /code/{code}
        mockMvc.perform(get(BASE_URL + "/code/TESTCOUP01").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /promotion/{promotionId}
        mockMvc.perform(get(BASE_URL + "/promotion/" + promotion.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /status/{status}
        mockMvc.perform(get(BASE_URL + "/status/ACTIVE").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /
        CreateCouponRequest createReq = CreateCouponRequest.builder()
                .code("NOPERM").promotionId(promotion.getId()).build();
        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // POST /generate/{promotionId}
        mockMvc.perform(post(BASE_URL + "/generate/" + promotion.getId())
                        .param("count", "1").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // PUT /{id}
        mockMvc.perform(put(BASE_URL + "/" + activeCoupon.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // DELETE /{id}
        mockMvc.perform(delete(BASE_URL + "/" + activeCoupon.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /{id}/activate
        mockMvc.perform(post(BASE_URL + "/" + activeCoupon.getId() + "/activate").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /{id}/deactivate
        mockMvc.perform(post(BASE_URL + "/" + activeCoupon.getId() + "/deactivate").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /{id}/cancel
        mockMvc.perform(post(BASE_URL + "/" + activeCoupon.getId() + "/cancel").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /{id}/redemptions
        mockMvc.perform(get(BASE_URL + "/" + activeCoupon.getId() + "/redemptions").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /update-expired
        mockMvc.perform(post(BASE_URL + "/update-expired").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /update-depleted
        mockMvc.perform(post(BASE_URL + "/update-depleted").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
