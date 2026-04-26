package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.inventory.dto.AbcAnalysisDto;
import com.hisobnoma.platform.inventory.dto.LowStockDto;
import com.hisobnoma.platform.inventory.dto.ReorderSuggestionDto;
import com.hisobnoma.platform.inventory.service.InventoryPlanningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryPlanningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryPlanningService inventoryPlanningService;

    // ==================== getReorderSuggestions ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getReorderSuggestions_authenticated_returns200() throws Exception {
        ReorderSuggestionDto dto = ReorderSuggestionDto.builder()
                .productId(1L)
                .productSku("SKU-001")
                .productName("Test Product")
                .currentStock(new BigDecimal("5"))
                .reorderPoint(new BigDecimal("10"))
                .suggestedQuantity(new BigDecimal("50"))
                .priority("CRITICAL")
                .build();
        when(inventoryPlanningService.getReorderSuggestions(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/planning/reorder-suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productSku").value("SKU-001"))
                .andExpect(jsonPath("$[0].priority").value("CRITICAL"));
    }

    @Test
    void getReorderSuggestions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/planning/reorder-suggestions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getReorderSuggestions_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/planning/reorder-suggestions"))
                .andExpect(status().isForbidden());
    }

    // ==================== getAbcAnalysis ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getAbcAnalysis_authenticated_returns200() throws Exception {
        AbcAnalysisDto dto = AbcAnalysisDto.builder()
                .productId(1L)
                .productSku("SKU-001")
                .productName("Test Product")
                .classification("A")
                .annualValue(new BigDecimal("50000"))
                .build();
        when(inventoryPlanningService.performAbcAnalysis(anyInt())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/planning/abc-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].classification").value("A"));
    }

    @Test
    void getAbcAnalysis_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/planning/abc-analysis"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAbcAnalysis_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/planning/abc-analysis"))
                .andExpect(status().isForbidden());
    }

    // ==================== getSlowMovingProducts ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getSlowMovingProducts_authenticated_returns200() throws Exception {
        LowStockDto dto = LowStockDto.builder()
                .productId(1L)
                .productSku("SKU-001")
                .productName("Slow Product")
                .daysSinceLastSale(120)
                .recommendation("DISCOUNT")
                .build();
        when(inventoryPlanningService.getSlowMovingProducts(anyInt(), any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/planning/slow-moving"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productSku").value("SKU-001"))
                .andExpect(jsonPath("$[0].recommendation").value("DISCOUNT"));
    }

    @Test
    void getSlowMovingProducts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/planning/slow-moving"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getSlowMovingProducts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/planning/slow-moving"))
                .andExpect(status().isForbidden());
    }

    // ==================== getDeadStock ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getDeadStock_authenticated_returns200() throws Exception {
        LowStockDto dto = LowStockDto.builder()
                .productId(1L)
                .productSku("SKU-001")
                .productName("Dead Stock Product")
                .daysSinceLastSale(200)
                .recommendation("WRITE_OFF")
                .build();
        when(inventoryPlanningService.getDeadStock(anyInt(), any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/planning/dead-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recommendation").value("WRITE_OFF"));
    }

    @Test
    void getDeadStock_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/planning/dead-stock"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getDeadStock_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/planning/dead-stock"))
                .andExpect(status().isForbidden());
    }
}
