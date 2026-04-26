package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.PricingService;
import com.hisobnoma.platform.pos.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PricingService pricingService;

    @MockBean
    private PromotionService promotionService;

    // ==================== POST /api/v1/pos/pricing/calculate ====================

    @Test
    @WithMockUser(authorities = "POS_PRICING_CALCULATE")
    void calculatePrices_authenticated_returns200() throws Exception {
        PriceCalculationRequest request = new PriceCalculationRequest();
        PriceCalculationResult result = new PriceCalculationResult();
        when(pricingService.calculatePrices(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/pos/pricing/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void calculatePrices_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/pricing/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void calculatePrices_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/pricing/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/pricing/product/{productId} ====================

    @Test
    @WithMockUser(authorities = "POS_PRICING_CALCULATE")
    void getProductPrice_authenticated_returns200() throws Exception {
        when(pricingService.getProductPrice(any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("100.00"));

        mockMvc.perform(get("/api/v1/pos/pricing/product/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getProductPrice_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/pricing/product/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getProductPrice_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/pricing/product/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/pricing/apply-coupon ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_APPLY")
    void applyCoupon_authenticated_returns200() throws Exception {
        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .couponCode("SAVE10")
                .orderTotal(new BigDecimal("100.00"))
                .build();

        PriceCalculationResult.CouponApplication couponResult = PriceCalculationResult.CouponApplication.builder()
                .valid(true)
                .discountAmount(new BigDecimal("10.00"))
                .discountDescription("10% off")
                .build();
        when(promotionService.applyCoupon(any(), any(), any())).thenReturn(couponResult);

        mockMvc.perform(post("/api/v1/pos/pricing/apply-coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void applyCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/pricing/apply-coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void applyCoupon_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/pricing/apply-coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/pricing/validate-coupon ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_APPLY")
    void validateCoupon_authenticated_returns200() throws Exception {
        PriceCalculationResult.CouponApplication couponResult = PriceCalculationResult.CouponApplication.builder()
                .valid(true)
                .discountDescription("10% off")
                .build();
        when(promotionService.applyCoupon(any(), any(), any())).thenReturn(couponResult);

        mockMvc.perform(post("/api/v1/pos/pricing/validate-coupon")
                        .param("couponCode", "SAVE10"))
                .andExpect(status().isOk());
    }

    @Test
    void validateCoupon_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/pricing/validate-coupon")
                        .param("couponCode", "SAVE10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void validateCoupon_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/pricing/validate-coupon")
                        .param("couponCode", "SAVE10"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/pricing/record-coupon-redemption ====================

    @Test
    @WithMockUser(authorities = "POS_COUPON_REDEEM")
    void recordCouponRedemption_authenticated_returns200() throws Exception {
        doNothing().when(promotionService).recordCouponRedemption(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/pos/pricing/record-coupon-redemption")
                        .param("couponCode", "SAVE10")
                        .param("customerId", "1")
                        .param("orderId", "100")
                        .param("discountApplied", "10.00"))
                .andExpect(status().isOk());
    }

    @Test
    void recordCouponRedemption_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/pricing/record-coupon-redemption")
                        .param("couponCode", "SAVE10")
                        .param("customerId", "1")
                        .param("orderId", "100")
                        .param("discountApplied", "10.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void recordCouponRedemption_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/pricing/record-coupon-redemption")
                        .param("couponCode", "SAVE10")
                        .param("customerId", "1")
                        .param("orderId", "100")
                        .param("discountApplied", "10.00"))
                .andExpect(status().isForbidden());
    }
}
