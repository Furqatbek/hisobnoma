package com.hisobnoma.platform.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.delivery.dto.DeliveryVillageDTO;
import com.hisobnoma.platform.delivery.service.DeliveryVillageService;
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
class DeliveryVillageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryVillageService villageService;

    // ==================== GET /api/v1/delivery/villages ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_VILLAGE_READ")
    void getAll_authenticated_returns200() throws Exception {
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder().id(1L).name("Village-1").build();
        when(villageService.findAll(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/delivery/villages"))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAll_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/delivery/villages/active ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_VILLAGE_READ")
    void getActive_authenticated_returns200() throws Exception {
        when(villageService.findActive()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/delivery/villages/active"))
                .andExpect(status().isOk());
    }

    @Test
    void getActive_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/delivery/villages/region/{regionId} ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_VILLAGE_READ")
    void getByRegion_authenticated_returns200() throws Exception {
        when(villageService.findByRegion(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/delivery/villages/region/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getByRegion_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages/region/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getByRegion_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages/region/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/delivery/villages/{id} ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_VILLAGE_READ")
    void getById_authenticated_returns200() throws Exception {
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder().id(1L).name("Village-1").build();
        when(villageService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/delivery/villages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Village-1"));
    }

    @Test
    void getById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/delivery/villages ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_VILLAGE_CREATE")
    void create_authenticated_returns201() throws Exception {
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder().id(1L).name("Village-1").build();
        when(villageService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/delivery/villages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Village-1"));
    }

    @Test
    void create_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/delivery/villages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void create_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/delivery/villages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/delivery/villages/{id} ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_VILLAGE_UPDATE")
    void update_authenticated_returns200() throws Exception {
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder().id(1L).name("Updated Village").build();
        when(villageService.update(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/delivery/villages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Village"));
    }

    @Test
    void update_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/delivery/villages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void update_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/delivery/villages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/delivery/villages/{id} ====================

    @Test
    @WithMockUser(authorities = "DELIVERY_VILLAGE_DELETE")
    void delete_authenticated_returns200() throws Exception {
        doNothing().when(villageService).delete(1L);

        mockMvc.perform(delete("/api/v1/delivery/villages/1"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/delivery/villages/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void delete_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/delivery/villages/1"))
                .andExpect(status().isForbidden());
    }
}
