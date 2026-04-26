package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.pos.dto.CouponDto;
import com.hisobnoma.platform.pos.dto.CreateCouponRequest;
import com.hisobnoma.platform.pos.service.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CouponService couponService;

    private RequestPostProcessor userWithPermission(String... permissions) {
        UserPrincipal principal = new UserPrincipal(
                1L, "admin", "pw", 1L, true, true,
                Arrays.stream(permissions)
                        .map(SimpleGrantedAuthority::new)
                        .toList());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ==================== GET /api/v1/pos/coupons ====================

    @Test
    void getAllCoupons_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.findAllCoupons(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/pos/coupons").with(userWithPermission("POS_COUPON_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getAllCoupons_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllCoupons_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/coupons/{id} ====================

    @Test
    void getCouponById_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.findCouponById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/coupons/1").with(userWithPermission("POS_COUPON_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COUP-001"));
    }

    @Test
    void getCouponById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCouponById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons/1").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/coupons/code/{code} ====================

    @Test
    void getCouponByCode_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.findCouponByCode("COUP-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/coupons/code/COUP-001").with(userWithPermission("POS_COUPON_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COUP-001"));
    }

    @Test
    void getCouponByCode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons/code/COUP-001"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/coupons ====================

    @Test
    void createCoupon_authenticated_returns201() throws Exception {
        CreateCouponRequest request = new CreateCouponRequest();
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.createCoupon(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/coupons")
                        .with(userWithPermission("POS_COUPON_CREATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COUP-001"));
    }

    @Test
    void createCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCoupon_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/pos/coupons/{id} ====================

    @Test
    void updateCoupon_authenticated_returns200() throws Exception {
        CreateCouponRequest request = new CreateCouponRequest();
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.updateCoupon(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/pos/coupons/1")
                        .with(userWithPermission("POS_COUPON_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/coupons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCoupon_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/coupons/1")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/pos/coupons/{id} ====================

    @Test
    void deleteCoupon_authenticated_returns204() throws Exception {
        doNothing().when(couponService).deleteCoupon(1L);

        mockMvc.perform(delete("/api/v1/pos/coupons/1").with(userWithPermission("POS_COUPON_DELETE")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/coupons/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCoupon_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/coupons/1").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/coupons/{id}/activate ====================

    @Test
    void activateCoupon_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.activateCoupon(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/coupons/1/activate").with(userWithPermission("POS_COUPON_UPDATE")))
                .andExpect(status().isOk());
    }

    @Test
    void activateCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons/1/activate"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/coupons/{id}/deactivate ====================

    @Test
    void deactivateCoupon_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.deactivateCoupon(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/coupons/1/deactivate").with(userWithPermission("POS_COUPON_UPDATE")))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons/1/deactivate"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/coupons/{id}/cancel ====================

    @Test
    void cancelCoupon_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.cancelCoupon(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/coupons/1/cancel").with(userWithPermission("POS_COUPON_UPDATE")))
                .andExpect(status().isOk());
    }

    @Test
    void cancelCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons/1/cancel"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/coupons/{id}/redemptions ====================

    @Test
    void getCouponRedemptions_authenticated_returns200() throws Exception {
        when(couponService.getCouponRedemptions(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/coupons/1/redemptions").with(userWithPermission("POS_COUPON_REDEMPTIONS_VIEW")))
                .andExpect(status().isOk());
    }

    @Test
    void getCouponRedemptions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons/1/redemptions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCouponRedemptions_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons/1/redemptions").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/coupons/update-expired ====================

    @Test
    void updateExpiredCoupons_authenticated_returns200() throws Exception {
        doNothing().when(couponService).updateExpiredCoupons();

        mockMvc.perform(post("/api/v1/pos/coupons/update-expired").with(userWithPermission("POS_COUPON_UPDATE")))
                .andExpect(status().isOk());
    }

    @Test
    void updateExpiredCoupons_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons/update-expired"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/coupons/generate/{promotionId} ====================

    @Test
    void generateCoupons_authenticated_returns201() throws Exception {
        CreateCouponRequest request = new CreateCouponRequest();
        CouponDto dto = CouponDto.builder().id(1L).code("GEN-001").build();
        when(couponService.generateCoupons(eq(1L), eq(5), any())).thenReturn(List.of(dto));

        mockMvc.perform(post("/api/v1/pos/coupons/generate/1")
                        .with(userWithPermission("POS_COUPON_GENERATE"))
                        .param("count", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void generateCoupons_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons/generate/1")
                        .param("count", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void generateCoupons_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons/generate/1")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .param("count", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
