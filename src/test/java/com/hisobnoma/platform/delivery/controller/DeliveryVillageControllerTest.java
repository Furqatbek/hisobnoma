package com.hisobnoma.platform.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.delivery.dto.DeliveryVillageDTO;
import com.hisobnoma.platform.delivery.service.DeliveryVillageService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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

    // ==================== GET /api/v1/delivery/villages ====================

    @Test
    void getAll_authenticated_returns200() throws Exception {
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder().id(1L).name("Village-1").build();
        when(villageService.findAll(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/delivery/villages").with(userWithPermission("DELIVERY_VILLAGE_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/delivery/villages/active ====================

    @Test
    void getActive_authenticated_returns200() throws Exception {
        when(villageService.findActive()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/delivery/villages/active").with(userWithPermission("DELIVERY_VILLAGE_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getActive_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/delivery/villages/region/{regionId} ====================

    @Test
    void getByRegion_authenticated_returns200() throws Exception {
        when(villageService.findByRegion(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/delivery/villages/region/1").with(userWithPermission("DELIVERY_VILLAGE_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getByRegion_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages/region/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByRegion_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages/region/1").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/delivery/villages/{id} ====================

    @Test
    void getById_authenticated_returns200() throws Exception {
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder().id(1L).name("Village-1").build();
        when(villageService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/delivery/villages/1").with(userWithPermission("DELIVERY_VILLAGE_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Village-1"));
    }

    @Test
    void getById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/villages/1").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/delivery/villages ====================

    @Test
    void create_authenticated_returns201() throws Exception {
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder().id(1L).name("Village-1").build();
        when(villageService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/delivery/villages")
                        .with(userWithPermission("DELIVERY_VILLAGE_CREATE"))
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
    void create_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/delivery/villages")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/delivery/villages/{id} ====================

    @Test
    void update_authenticated_returns200() throws Exception {
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder().id(1L).name("Updated Village").build();
        when(villageService.update(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/delivery/villages/1")
                        .with(userWithPermission("DELIVERY_VILLAGE_UPDATE"))
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
    void update_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/delivery/villages/1")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/delivery/villages/{id} ====================

    @Test
    void delete_authenticated_returns200() throws Exception {
        doNothing().when(villageService).delete(1L);

        mockMvc.perform(delete("/api/v1/delivery/villages/1").with(userWithPermission("DELIVERY_VILLAGE_DELETE")))
                .andExpect(status().isOk());
    }

    @Test
    void delete_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/delivery/villages/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/delivery/villages/1").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }
}
