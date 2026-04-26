package com.hisobnoma.platform.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.delivery.dto.DeliveryRegionDTO;
import com.hisobnoma.platform.delivery.service.DeliveryRegionService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryRegionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryRegionService regionService;

    // ==================== GET /api/v1/delivery/regions ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_REGION_READ")
    void getAll_authenticated_returns200() throws Exception {
        DeliveryRegionDTO dto = DeliveryRegionDTO.builder().id(1L).name("Tashkent").build();
        when(regionService.findAll(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/delivery/regions"))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/regions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAll_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/regions"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/delivery/regions/active ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_REGION_READ")
    void getActive_authenticated_returns200() throws Exception {
        when(regionService.findActive()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/delivery/regions/active"))
                .andExpect(status().isOk());
    }

    @Test
    void getActive_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/regions/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/delivery/regions/{id} ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_REGION_READ")
    void getById_authenticated_returns200() throws Exception {
        DeliveryRegionDTO dto = DeliveryRegionDTO.builder().id(1L).name("Tashkent").build();
        when(regionService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/delivery/regions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Tashkent"));
    }

    @Test
    void getById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/regions/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/regions/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/delivery/regions ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_REGION_CREATE")
    void create_authenticated_returns201() throws Exception {
        DeliveryRegionDTO dto = DeliveryRegionDTO.builder().id(1L).name("Tashkent").build();
        when(regionService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/delivery/regions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Tashkent"));
    }

    @Test
    void create_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/delivery/regions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void create_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/delivery/regions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/delivery/regions/{id} ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_REGION_UPDATE")
    void update_authenticated_returns200() throws Exception {
        DeliveryRegionDTO dto = DeliveryRegionDTO.builder().id(1L).name("Updated Region").build();
        when(regionService.update(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/delivery/regions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Region"));
    }

    @Test
    void update_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/delivery/regions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void update_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/delivery/regions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/delivery/regions/{id} ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_REGION_DELETE")
    void delete_authenticated_returns200() throws Exception {
        doNothing().when(regionService).delete(1L);

        mockMvc.perform(delete("/api/v1/delivery/regions/1"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/delivery/regions/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void delete_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/delivery/regions/1"))
                .andExpect(status().isForbidden());
    }
}
