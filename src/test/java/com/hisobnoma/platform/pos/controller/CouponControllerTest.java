package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
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

    // ==================== GET /api/v1/pos/coupons ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_READ")
    void getAllCoupons_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.findAllCoupons(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/pos/coupons"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllCoupons_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAllCoupons_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/coupons/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_READ")
    void getCouponById_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.findCouponById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/coupons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COUP-001"));
    }

    @Test
    void getCouponById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCouponById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/coupons/code/{code} ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_READ")
    void getCouponByCode_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.findCouponByCode("COUP-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/coupons/code/COUP-001"))
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
    @WithMockUser(authorities = "POS_COUPON_CREATE")
    void createCoupon_authenticated_returns201() throws Exception {
        CreateCouponRequest request = new CreateCouponRequest();
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.createCoupon(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/coupons")
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
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createCoupon_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/pos/coupons/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_UPDATE")
    void updateCoupon_authenticated_returns200() throws Exception {
        CreateCouponRequest request = new CreateCouponRequest();
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.updateCoupon(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/pos/coupons/1")
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
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void updateCoupon_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/coupons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/pos/coupons/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_DELETE")
    void deleteCoupon_authenticated_returns204() throws Exception {
        doNothing().when(couponService).deleteCoupon(1L);

        mockMvc.perform(delete("/api/v1/pos/coupons/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/coupons/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteCoupon_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/coupons/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/coupons/{id}/activate ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_UPDATE")
    void activateCoupon_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.activateCoupon(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/coupons/1/activate"))
                .andExpect(status().isOk());
    }

    @Test
    void activateCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons/1/activate"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/coupons/{id}/deactivate ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_UPDATE")
    void deactivateCoupon_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.deactivateCoupon(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/coupons/1/deactivate"))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons/1/deactivate"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/coupons/{id}/cancel ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_UPDATE")
    void cancelCoupon_authenticated_returns200() throws Exception {
        CouponDto dto = CouponDto.builder().id(1L).code("COUP-001").build();
        when(couponService.cancelCoupon(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/coupons/1/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    void cancelCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons/1/cancel"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/coupons/{id}/redemptions ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_REDEMPTIONS_VIEW")
    void getCouponRedemptions_authenticated_returns200() throws Exception {
        when(couponService.getCouponRedemptions(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/coupons/1/redemptions"))
                .andExpect(status().isOk());
    }

    @Test
    void getCouponRedemptions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons/1/redemptions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCouponRedemptions_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/coupons/1/redemptions"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/coupons/update-expired ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_UPDATE")
    void updateExpiredCoupons_authenticated_returns200() throws Exception {
        doNothing().when(couponService).updateExpiredCoupons();

        mockMvc.perform(post("/api/v1/pos/coupons/update-expired"))
                .andExpect(status().isOk());
    }

    @Test
    void updateExpiredCoupons_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons/update-expired"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/coupons/generate/{promotionId} ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_GENERATE")
    void generateCoupons_authenticated_returns201() throws Exception {
        CreateCouponRequest request = new CreateCouponRequest();
        CouponDto dto = CouponDto.builder().id(1L).code("GEN-001").build();
        when(couponService.generateCoupons(eq(1L), eq(5), any())).thenReturn(List.of(dto));

        mockMvc.perform(post("/api/v1/pos/coupons/generate/1")
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
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void generateCoupons_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/coupons/generate/1")
                        .param("count", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
