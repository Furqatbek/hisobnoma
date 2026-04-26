package com.hisobnoma.platform.mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.mobile.dto.QuickSaleRequest;
import com.hisobnoma.platform.mobile.dto.QuickStockCountRequest;
import com.hisobnoma.platform.mobile.service.MobileQuickActionService;
import com.hisobnoma.platform.pos.dto.POSTransactionDto;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobileQuickActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MobileQuickActionService quickActionService;

    // ==================== GET /api/v1/mobile/inventory/barcode/{barcode} ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void lookupByBarcode_authenticated_returns200() throws Exception {
        when(quickActionService.lookupByBarcode("123456")).thenReturn(Map.of("name", "Product A"));

        mockMvc.perform(get("/api/v1/mobile/inventory/barcode/123456"))
                .andExpect(status().isOk());
    }

    @Test
    void lookupByBarcode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/inventory/barcode/123456"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void lookupByBarcode_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/inventory/barcode/123456"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/mobile/inventory/quick-count ====================

    @Test
    @WithMockUser(authorities = "MOBILE_QUICK_COUNT")
    void quickStockCount_authenticated_returns200() throws Exception {
        QuickStockCountRequest request = QuickStockCountRequest.builder()
                .productId(1L)
                .locationId(1L)
                .countedQuantity(new java.math.BigDecimal("10"))
                .build();
        when(quickActionService.performQuickStockCount(any())).thenReturn(Map.of("status", "counted"));

        mockMvc.perform(post("/api/v1/mobile/inventory/quick-count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void quickStockCount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/inventory/quick-count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void quickStockCount_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/inventory/quick-count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/mobile/pos/quick-sale ====================

    @Test
    @WithMockUser(authorities = "MOBILE_QUICK_SALE")
    void quickSale_authenticated_returns200() throws Exception {
        QuickSaleRequest request = QuickSaleRequest.builder()
                .terminalId(1L)
                .paymentType("CASH")
                .items(List.of(QuickSaleRequest.QuickSaleItem.builder()
                        .productId(1L)
                        .quantity(new java.math.BigDecimal("1"))
                        .build()))
                .build();
        POSTransactionDto dto = POSTransactionDto.builder().id(1L).transactionNumber("QS-001").build();
        when(quickActionService.performQuickSale(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/mobile/pos/quick-sale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void quickSale_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/pos/quick-sale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void quickSale_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/pos/quick-sale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/customers/search ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_CUSTOMER_VIEW")
    void searchCustomers_authenticated_returns200() throws Exception {
        when(quickActionService.searchCustomers(any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/mobile/customers/search").param("query", "john"))
                .andExpect(status().isOk());
    }

    @Test
    void searchCustomers_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/customers/search").param("query", "john"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchCustomers_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/customers/search").param("query", "john"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/inventory/search ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void searchProducts_authenticated_returns200() throws Exception {
        when(quickActionService.searchProducts(any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/mobile/inventory/search").param("query", "widget"))
                .andExpect(status().isOk());
    }

    @Test
    void searchProducts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/inventory/search").param("query", "widget"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchProducts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/inventory/search").param("query", "widget"))
                .andExpect(status().isForbidden());
    }
}
