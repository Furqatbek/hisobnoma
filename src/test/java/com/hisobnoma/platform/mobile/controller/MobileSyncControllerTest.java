package com.hisobnoma.platform.mobile.controller;

import com.hisobnoma.platform.mobile.dto.MobileSyncDataDTO;
import com.hisobnoma.platform.mobile.service.MobileSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobileSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MobileSyncService syncService;

    // ==================== GET /api/v1/mobile/sync/products ====================

    @Test
    @WithMockUser(authorities = "MOBILE_SYNC_ACCESS")
    void syncProducts_authenticated_returns200() throws Exception {
        MobileSyncDataDTO dto = new MobileSyncDataDTO();
        when(syncService.getProductsForSync(any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/mobile/sync/products"))
                .andExpect(status().isOk());
    }

    @Test
    void syncProducts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void syncProducts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/products"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/sync/customers ====================

    @Test
    @WithMockUser(authorities = "MOBILE_SYNC_ACCESS")
    void syncCustomers_authenticated_returns200() throws Exception {
        MobileSyncDataDTO dto = new MobileSyncDataDTO();
        when(syncService.getCustomersForSync(any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/mobile/sync/customers"))
                .andExpect(status().isOk());
    }

    @Test
    void syncCustomers_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void syncCustomers_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/customers"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/sync/categories ====================

    @Test
    @WithMockUser(authorities = "MOBILE_SYNC_ACCESS")
    void syncCategories_authenticated_returns200() throws Exception {
        MobileSyncDataDTO dto = new MobileSyncDataDTO();
        when(syncService.getCategoriesForSync()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/mobile/sync/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void syncCategories_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void syncCategories_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/categories"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/sync/last-updated ====================

    @Test
    @WithMockUser(authorities = "MOBILE_SYNC_ACCESS")
    void getLastUpdated_authenticated_returns200() throws Exception {
        when(syncService.getLastUpdatedTimestamp()).thenReturn(Instant.now());

        mockMvc.perform(get("/api/v1/mobile/sync/last-updated"))
                .andExpect(status().isOk());
    }

    @Test
    void getLastUpdated_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/last-updated"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getLastUpdated_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/sync/last-updated"))
                .andExpect(status().isForbidden());
    }
}
