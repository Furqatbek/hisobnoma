package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.PromotionService;
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
class PromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PromotionService promotionService;

    // ==================== GET /api/v1/pos/promotions ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_READ")
    void getAllPromotions_authenticated_returns200() throws Exception {
        PromotionDto dto = PromotionDto.builder().id(1L).code("PROMO-001").name("Summer Sale").build();
        when(promotionService.findAll(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/pos/promotions"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllPromotions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/promotions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAllPromotions_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/promotions"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/promotions/search ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_READ")
    void searchPromotions_authenticated_returns200() throws Exception {
        when(promotionService.search(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/pos/promotions/search").param("query", "summer"))
                .andExpect(status().isOk());
    }

    @Test
    void searchPromotions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/promotions/search").param("query", "summer"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/promotions/active ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_READ")
    void getActivePromotions_authenticated_returns200() throws Exception {
        when(promotionService.findActivePromotions(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/promotions/active"))
                .andExpect(status().isOk());
    }

    @Test
    void getActivePromotions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/promotions/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/promotions/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_READ")
    void getPromotionById_authenticated_returns200() throws Exception {
        PromotionDto dto = PromotionDto.builder().id(1L).code("PROMO-001").name("Summer Sale").build();
        when(promotionService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/promotions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROMO-001"));
    }

    @Test
    void getPromotionById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/promotions/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getPromotionById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/promotions/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/promotions/code/{code} ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_READ")
    void getPromotionByCode_authenticated_returns200() throws Exception {
        PromotionDto dto = PromotionDto.builder().id(1L).code("PROMO-001").name("Summer Sale").build();
        when(promotionService.findByCode("PROMO-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/promotions/code/PROMO-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROMO-001"));
    }

    @Test
    void getPromotionByCode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/promotions/code/PROMO-001"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/promotions ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_CREATE")
    void createPromotion_authenticated_returns201() throws Exception {
        CreatePromotionRequest request = new CreatePromotionRequest();
        PromotionDto dto = PromotionDto.builder().id(1L).code("PROMO-001").name("Summer Sale").build();
        when(promotionService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PROMO-001"));
    }

    @Test
    void createPromotion_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createPromotion_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/pos/promotions/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_UPDATE")
    void updatePromotion_authenticated_returns200() throws Exception {
        CreatePromotionRequest request = new CreatePromotionRequest();
        PromotionDto dto = PromotionDto.builder().id(1L).code("PROMO-001").name("Updated").build();
        when(promotionService.update(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/pos/promotions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updatePromotion_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/promotions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void updatePromotion_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/promotions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/pos/promotions/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_DELETE")
    void deletePromotion_authenticated_returns204() throws Exception {
        doNothing().when(promotionService).delete(1L);

        mockMvc.perform(delete("/api/v1/pos/promotions/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePromotion_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/promotions/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deletePromotion_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/promotions/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/promotions/{id}/activate ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_UPDATE")
    void activatePromotion_authenticated_returns200() throws Exception {
        PromotionDto dto = PromotionDto.builder().id(1L).code("PROMO-001").build();
        when(promotionService.activate(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/promotions/1/activate"))
                .andExpect(status().isOk());
    }

    @Test
    void activatePromotion_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/promotions/1/activate"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/promotions/{id}/deactivate ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_UPDATE")
    void deactivatePromotion_authenticated_returns200() throws Exception {
        PromotionDto dto = PromotionDto.builder().id(1L).code("PROMO-001").build();
        when(promotionService.deactivate(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/promotions/1/deactivate"))
                .andExpect(status().isOk());
    }

    @Test
    void deactivatePromotion_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/promotions/1/deactivate"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/promotions/{promotionId}/conditions ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_CONDITIONS_MANAGE")
    void addCondition_authenticated_returns201() throws Exception {
        CreatePromotionConditionRequest request = new CreatePromotionConditionRequest();
        PromotionDto dto = PromotionDto.builder().id(1L).code("PROMO-001").build();
        when(promotionService.addCondition(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/promotions/1/conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void addCondition_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/promotions/1/conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void addCondition_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/promotions/1/conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/pos/promotions/{promotionId}/conditions/{conditionId} ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_CONDITIONS_MANAGE")
    void removeCondition_authenticated_returns200() throws Exception {
        PromotionDto dto = PromotionDto.builder().id(1L).code("PROMO-001").build();
        when(promotionService.removeCondition(1L, 2L)).thenReturn(dto);

        mockMvc.perform(delete("/api/v1/pos/promotions/1/conditions/2"))
                .andExpect(status().isOk());
    }

    @Test
    void removeCondition_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/promotions/1/conditions/2"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/promotions/{promotionId}/actions ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_ACTIONS_MANAGE")
    void addAction_authenticated_returns201() throws Exception {
        CreatePromotionActionRequest request = new CreatePromotionActionRequest();
        PromotionDto dto = PromotionDto.builder().id(1L).code("PROMO-001").build();
        when(promotionService.addAction(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/promotions/1/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void addAction_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/promotions/1/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void addAction_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/promotions/1/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/pos/promotions/{promotionId}/actions/{actionId} ====================

    @Test
    @WithMockUser(authorities = "POS_PROMOTION_ACTIONS_MANAGE")
    void removeAction_authenticated_returns200() throws Exception {
        PromotionDto dto = PromotionDto.builder().id(1L).code("PROMO-001").build();
        when(promotionService.removeAction(1L, 2L)).thenReturn(dto);

        mockMvc.perform(delete("/api/v1/pos/promotions/1/actions/2"))
                .andExpect(status().isOk());
    }

    @Test
    void removeAction_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/promotions/1/actions/2"))
                .andExpect(status().isForbidden());
    }
}
