package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.pos.dto.CreatePromotionActionRequest;
import com.hisobnoma.platform.pos.dto.CreatePromotionConditionRequest;
import com.hisobnoma.platform.pos.dto.CreatePromotionRequest;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.entity.PromotionAction;
import com.hisobnoma.platform.pos.entity.PromotionCondition;
import com.hisobnoma.platform.pos.enums.PromotionConditionType;
import com.hisobnoma.platform.pos.enums.PromotionScope;
import com.hisobnoma.platform.pos.enums.PromotionType;
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
class PromotionControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/pos/promotions";

    private Tenant tenant;
    private User user;
    private Promotion promotion;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Promo Test Tenant").code("PROMO_FLOW").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("promotestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        promotion = promotionRepository.saveAndFlush(Promotion.builder()
                .code("SUMMER10")
                .name("Summer Sale 10%")
                .description("10 percent off for summer")
                .type(PromotionType.PERCENTAGE_OFF)
                .scope(PromotionScope.ORDER)
                .priority(5)
                .discountValue(BigDecimal.TEN)
                .maxDiscountAmount(new BigDecimal("50.0000"))
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().plusDays(30))
                .active(true)
                .stackable(false)
                .requiresCoupon(false)
                .maxUses(1000)
                .currentUses(0)
                .maxUsesPerCustomer(5)
                .minOrderAmount(new BigDecimal("20.0000"))
                .notes("Integration test promotion")
                .tenantId(tenant.getId())
                .build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("POS_PROMOTION_READ"),
                        new SimpleGrantedAuthority("POS_PROMOTION_CREATE"),
                        new SimpleGrantedAuthority("POS_PROMOTION_UPDATE"),
                        new SimpleGrantedAuthority("POS_PROMOTION_DELETE"),
                        new SimpleGrantedAuthority("POS_PROMOTION_CONDITIONS_MANAGE"),
                        new SimpleGrantedAuthority("POS_PROMOTION_ACTIONS_MANAGE")));
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
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("SUMMER10"));
    }

    // ---- GET /search ----

    @Test
    void search_returnsResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("query", "Summer").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("SUMMER10"));
    }

    // ---- GET /active ----

    @Test
    void getActive_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("SUMMER10"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    // ---- GET /{id} ----

    @Test
    void getById_returnsPromotion() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + promotion.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(promotion.getId()))
                .andExpect(jsonPath("$.code").value("SUMMER10"))
                .andExpect(jsonPath("$.name").value("Summer Sale 10%"))
                .andExpect(jsonPath("$.description").value("10 percent off for summer"))
                .andExpect(jsonPath("$.type").value("PERCENTAGE_OFF"))
                .andExpect(jsonPath("$.scope").value("ORDER"))
                .andExpect(jsonPath("$.priority").value(5))
                .andExpect(jsonPath("$.discountValue").value(closeTo(10.0, 0.01)))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.stackable").value(false))
                .andExpect(jsonPath("$.requiresCoupon").value(false))
                .andExpect(jsonPath("$.maxUses").value(1000))
                .andExpect(jsonPath("$.currentUses").value(0))
                .andExpect(jsonPath("$.maxUsesPerCustomer").value(5))
                .andExpect(jsonPath("$.notes").value("Integration test promotion"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /code/{code} ----

    @Test
    void getByCode_returnsPromotion() throws Exception {
        mockMvc.perform(get(BASE_URL + "/code/SUMMER10").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUMMER10"))
                .andExpect(jsonPath("$.name").value("Summer Sale 10%"))
                .andExpect(jsonPath("$.type").value("PERCENTAGE_OFF"));
    }

    // ---- POST / ----

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .code("WINTER20")
                .name("Winter Sale 20%")
                .description("20 percent off for winter")
                .type(PromotionType.PERCENTAGE_OFF)
                .scope(PromotionScope.ORDER)
                .priority(10)
                .discountValue(new BigDecimal("20"))
                .maxDiscountAmount(new BigDecimal("100"))
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(60))
                .stackable(true)
                .maxUses(500)
                .maxUsesPerCustomer(3)
                .minOrderAmount(new BigDecimal("50"))
                .notes("Winter promotion")
                .build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("WINTER20"))
                .andExpect(jsonPath("$.name").value("Winter Sale 20%"))
                .andExpect(jsonPath("$.description").value("20 percent off for winter"))
                .andExpect(jsonPath("$.type").value("PERCENTAGE_OFF"))
                .andExpect(jsonPath("$.scope").value("ORDER"))
                .andExpect(jsonPath("$.priority").value(10))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.stackable").value(true))
                .andExpect(jsonPath("$.maxUses").value(500))
                .andExpect(jsonPath("$.notes").value("Winter promotion"));
    }

    @Test
    void create_withConditionsAndActions() throws Exception {
        CreatePromotionConditionRequest condition = CreatePromotionConditionRequest.builder()
                .conditionType(PromotionConditionType.MINIMUM_PURCHASE)
                .operator("GTE")
                .thresholdAmount(new BigDecimal("100"))
                .required(true)
                .notes("Min purchase condition")
                .build();

        CreatePromotionActionRequest action = CreatePromotionActionRequest.builder()
                .actionType("PERCENTAGE_OFF")
                .discountPercent(new BigDecimal("15.00"))
                .maxDiscount(new BigDecimal("50"))
                .applyTo("ALL")
                .sortOrder(0)
                .notes("Percentage off action")
                .build();

        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .code("COMBO15")
                .name("Combo 15% Off")
                .description("15% off on orders over 100")
                .type(PromotionType.PERCENTAGE_OFF)
                .scope(PromotionScope.ORDER)
                .priority(3)
                .discountValue(new BigDecimal("15"))
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(90))
                .conditions(List.of(condition))
                .actions(List.of(action))
                .build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMBO15"))
                .andExpect(jsonPath("$.name").value("Combo 15% Off"))
                .andExpect(jsonPath("$.conditions").isArray())
                .andExpect(jsonPath("$.conditions.length()").value(1))
                .andExpect(jsonPath("$.conditions[0].conditionType").value("MINIMUM_PURCHASE"))
                .andExpect(jsonPath("$.conditions[0].operator").value("GTE"))
                .andExpect(jsonPath("$.conditions[0].required").value(true))
                .andExpect(jsonPath("$.actions").isArray())
                .andExpect(jsonPath("$.actions.length()").value(1))
                .andExpect(jsonPath("$.actions[0].actionType").value("PERCENTAGE_OFF"))
                .andExpect(jsonPath("$.actions[0].notes").value("Percentage off action"));
    }

    // ---- PUT /{id} ----

    @Test
    void update_validRequest_returnsUpdated() throws Exception {
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .code("SUMMER10")
                .name("Updated Summer Sale")
                .description("Updated description")
                .type(PromotionType.PERCENTAGE_OFF)
                .scope(PromotionScope.LINE_ITEM)
                .priority(8)
                .discountValue(new BigDecimal("15"))
                .maxDiscountAmount(new BigDecimal("75"))
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(60))
                .stackable(true)
                .maxUses(2000)
                .notes("Updated notes")
                .build();

        mockMvc.perform(put(BASE_URL + "/" + promotion.getId()).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(promotion.getId()))
                .andExpect(jsonPath("$.code").value("SUMMER10"))
                .andExpect(jsonPath("$.name").value("Updated Summer Sale"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.scope").value("LINE_ITEM"))
                .andExpect(jsonPath("$.priority").value(8))
                .andExpect(jsonPath("$.stackable").value(true))
                .andExpect(jsonPath("$.maxUses").value(2000))
                .andExpect(jsonPath("$.notes").value("Updated notes"));
    }

    // ---- DELETE /{id} ----

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + promotion.getId()).with(fullAuth()))
                .andExpect(status().isNoContent());

        // Verify promotion no longer exists
        mockMvc.perform(get(BASE_URL + "/" + promotion.getId()).with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- POST /{id}/activate ----

    @Test
    void activate_returnsActivated() throws Exception {
        // First deactivate via the service to have an inactive promotion
        mockMvc.perform(post(BASE_URL + "/" + promotion.getId() + "/deactivate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Now activate
        mockMvc.perform(post(BASE_URL + "/" + promotion.getId() + "/activate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.code").value("SUMMER10"));
    }

    // ---- POST /{id}/deactivate ----

    @Test
    void deactivate_returnsDeactivated() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + promotion.getId() + "/deactivate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.code").value("SUMMER10"));
    }

    // ---- POST /{promotionId}/conditions ----

    @Test
    void addCondition_returnsUpdatedPromotion() throws Exception {
        CreatePromotionConditionRequest request = CreatePromotionConditionRequest.builder()
                .conditionType(PromotionConditionType.MINIMUM_PURCHASE)
                .operator("GTE")
                .thresholdAmount(new BigDecimal("50"))
                .required(true)
                .notes("Minimum purchase of 50")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + promotion.getId() + "/conditions").with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUMMER10"))
                .andExpect(jsonPath("$.conditions").isArray())
                .andExpect(jsonPath("$.conditions.length()").value(1))
                .andExpect(jsonPath("$.conditions[0].conditionType").value("MINIMUM_PURCHASE"))
                .andExpect(jsonPath("$.conditions[0].operator").value("GTE"))
                .andExpect(jsonPath("$.conditions[0].required").value(true))
                .andExpect(jsonPath("$.conditions[0].notes").value("Minimum purchase of 50"));
    }

    // ---- POST /{promotionId}/actions ----

    @Test
    void addAction_returnsUpdatedPromotion() throws Exception {
        CreatePromotionActionRequest request = CreatePromotionActionRequest.builder()
                .actionType("PERCENTAGE_OFF")
                .discountPercent(new BigDecimal("10.00"))
                .maxDiscount(new BigDecimal("25"))
                .applyTo("ALL")
                .sortOrder(0)
                .notes("10% off action")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + promotion.getId() + "/actions").with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUMMER10"))
                .andExpect(jsonPath("$.actions").isArray())
                .andExpect(jsonPath("$.actions.length()").value(1))
                .andExpect(jsonPath("$.actions[0].actionType").value("PERCENTAGE_OFF"))
                .andExpect(jsonPath("$.actions[0].notes").value("10% off action"));
    }

    // ---- DELETE /{promotionId}/conditions/{conditionId} ----

    @Test
    void removeCondition_returnsUpdatedPromotion() throws Exception {
        // First add a condition
        Promotion promo = promotionRepository.findByIdAndTenantId(promotion.getId(), tenant.getId()).orElseThrow();
        PromotionCondition condition = PromotionCondition.builder()
                .promotion(promo)
                .conditionType(PromotionConditionType.MINIMUM_PURCHASE)
                .operator("GTE")
                .thresholdAmount(new BigDecimal("100"))
                .required(true)
                .notes("To be removed")
                .build();
        promo.addCondition(condition);
        promo = promotionRepository.saveAndFlush(promo);
        Long conditionId = promo.getConditions().get(0).getId();
        entityManager.clear();

        mockMvc.perform(delete(BASE_URL + "/" + promotion.getId() + "/conditions/" + conditionId).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUMMER10"))
                .andExpect(jsonPath("$.conditions").isArray())
                .andExpect(jsonPath("$.conditions.length()").value(0));
    }

    // ---- DELETE /{promotionId}/actions/{actionId} ----

    @Test
    void removeAction_returnsUpdatedPromotion() throws Exception {
        // First add an action
        Promotion promo = promotionRepository.findByIdAndTenantId(promotion.getId(), tenant.getId()).orElseThrow();
        PromotionAction action = PromotionAction.builder()
                .promotion(promo)
                .actionType("FIXED_AMOUNT_OFF")
                .discountAmount(new BigDecimal("5.0000"))
                .sortOrder(0)
                .notes("To be removed")
                .build();
        promo.addAction(action);
        promo = promotionRepository.saveAndFlush(promo);
        Long actionId = promo.getActions().get(0).getId();
        entityManager.clear();

        mockMvc.perform(delete(BASE_URL + "/" + promotion.getId() + "/actions/" + actionId).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUMMER10"))
                .andExpect(jsonPath("$.actions").isArray())
                .andExpect(jsonPath("$.actions.length()").value(0));
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        // GET /
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /search
        mockMvc.perform(get(BASE_URL + "/search").param("query", "test").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /active
        mockMvc.perform(get(BASE_URL + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /{id}
        mockMvc.perform(get(BASE_URL + "/" + promotion.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /code/{code}
        mockMvc.perform(get(BASE_URL + "/code/SUMMER10").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /
        CreatePromotionRequest createReq = CreatePromotionRequest.builder()
                .code("NOPERM").name("No Permission").type(PromotionType.PERCENTAGE_OFF).build();
        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // PUT /{id}
        mockMvc.perform(put(BASE_URL + "/" + promotion.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // DELETE /{id}
        mockMvc.perform(delete(BASE_URL + "/" + promotion.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /{id}/activate
        mockMvc.perform(post(BASE_URL + "/" + promotion.getId() + "/activate").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /{id}/deactivate
        mockMvc.perform(post(BASE_URL + "/" + promotion.getId() + "/deactivate").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /{promotionId}/conditions
        CreatePromotionConditionRequest condReq = CreatePromotionConditionRequest.builder()
                .conditionType(PromotionConditionType.MINIMUM_PURCHASE).build();
        mockMvc.perform(post(BASE_URL + "/" + promotion.getId() + "/conditions").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(condReq)))
                .andExpect(status().isForbidden());

        // DELETE /{promotionId}/conditions/{conditionId}
        mockMvc.perform(delete(BASE_URL + "/" + promotion.getId() + "/conditions/1").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /{promotionId}/actions
        CreatePromotionActionRequest actReq = CreatePromotionActionRequest.builder()
                .actionType("PERCENTAGE_OFF").build();
        mockMvc.perform(post(BASE_URL + "/" + promotion.getId() + "/actions").with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actReq)))
                .andExpect(status().isForbidden());

        // DELETE /{promotionId}/actions/{actionId}
        mockMvc.perform(delete(BASE_URL + "/" + promotion.getId() + "/actions/1").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
