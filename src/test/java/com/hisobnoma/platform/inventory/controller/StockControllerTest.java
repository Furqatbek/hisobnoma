package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.MovementType;
import com.hisobnoma.platform.inventory.service.StockService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockService stockService;

    // ==================== getStock ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getStock_authenticated_returns200() throws Exception {
        StockDto dto = StockDto.builder()
                .id(1L)
                .productId(1L)
                .productSku("SKU-001")
                .productName("Test Product")
                .locationId(1L)
                .locationCode("LOC-001")
                .quantityOnHand(new BigDecimal("100"))
                .build();
        PageResponse<StockDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(stockService.getStock(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productSku").value("SKU-001"));
    }

    @Test
    void getStock_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getStock_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock"))
                .andExpect(status().isForbidden());
    }

    // ==================== getStockByProduct ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getStockByProduct_authenticated_returns200() throws Exception {
        StockDto dto = StockDto.builder()
                .id(1L)
                .productId(1L)
                .productSku("SKU-001")
                .quantityOnHand(new BigDecimal("100"))
                .build();
        when(stockService.getStockByProduct(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/stock/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productSku").value("SKU-001"));
    }

    @Test
    void getStockByProduct_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/product/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getStockByProduct_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/product/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getStockByLocation ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getStockByLocation_authenticated_returns200() throws Exception {
        StockDto dto = StockDto.builder()
                .id(1L)
                .productId(1L)
                .productSku("SKU-001")
                .locationId(1L)
                .build();
        PageResponse<StockDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(stockService.getStockByLocation(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/stock/location/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productSku").value("SKU-001"));
    }

    @Test
    void getStockByLocation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/location/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getStockByLocation_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/location/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getStockByProductAndLocation ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getStockByProductAndLocation_authenticated_returns200() throws Exception {
        StockDto dto = StockDto.builder()
                .id(1L)
                .productId(1L)
                .locationId(1L)
                .quantityOnHand(new BigDecimal("50"))
                .build();
        when(stockService.getStockByProductAndLocation(1L, 1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/stock/product/1/location/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(50));
    }

    @Test
    void getStockByProductAndLocation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/product/1/location/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getStockByProductAndLocation_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/product/1/location/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getLowStock ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getLowStock_authenticated_returns200() throws Exception {
        LowStockDto dto = LowStockDto.builder()
                .productId(1L)
                .productSku("SKU-001")
                .productName("Test Product")
                .severity("CRITICAL")
                .build();
        PageResponse<LowStockDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(stockService.getLowStock(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/stock/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].severity").value("CRITICAL"));
    }

    @Test
    void getLowStock_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/low-stock"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getLowStock_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/low-stock"))
                .andExpect(status().isForbidden());
    }

    // ==================== searchStock ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void searchStock_authenticated_returns200() throws Exception {
        StockDto dto = StockDto.builder()
                .id(1L)
                .productSku("SKU-001")
                .build();
        PageResponse<StockDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(stockService.searchStock(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/stock/search").param("q", "SKU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productSku").value("SKU-001"));
    }

    @Test
    void searchStock_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/search").param("q", "SKU"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchStock_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/search").param("q", "SKU"))
                .andExpect(status().isForbidden());
    }

    // ==================== getStockValuation ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getStockValuation_authenticated_returns200() throws Exception {
        StockValuationDto dto = StockValuationDto.builder()
                .totalValue(new BigDecimal("50000.00"))
                .totalQuantity(new BigDecimal("1000"))
                .totalProducts(50L)
                .totalLocations(3L)
                .build();
        when(stockService.getStockValuation()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/stock/valuation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(50));
    }

    @Test
    void getStockValuation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/valuation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getStockValuation_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/valuation"))
                .andExpect(status().isForbidden());
    }

    // ==================== getAvailableQuantity ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getAvailableQuantity_authenticated_returns200() throws Exception {
        when(stockService.getAvailableQuantity(1L)).thenReturn(new BigDecimal("75.00"));

        mockMvc.perform(get("/api/v1/inventory/stock/available/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(75.00));
    }

    @Test
    void getAvailableQuantity_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/available/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAvailableQuantity_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/available/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== checkAvailability ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void checkAvailability_authenticated_returns200() throws Exception {
        when(stockService.isStockAvailable(1L, 1L, new BigDecimal("10"))).thenReturn(true);

        mockMvc.perform(get("/api/v1/inventory/stock/check-availability")
                        .param("productId", "1")
                        .param("locationId", "1")
                        .param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void checkAvailability_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/check-availability")
                        .param("productId", "1")
                        .param("locationId", "1")
                        .param("quantity", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void checkAvailability_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/check-availability")
                        .param("productId", "1")
                        .param("locationId", "1")
                        .param("quantity", "10"))
                .andExpect(status().isForbidden());
    }

    // ==================== getStockMovements ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getStockMovements_authenticated_returns200() throws Exception {
        StockMovementDto dto = StockMovementDto.builder()
                .id(1L)
                .productSku("SKU-001")
                .movementType(MovementType.STOCK_IN)
                .quantity(new BigDecimal("10"))
                .build();
        PageResponse<StockMovementDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(stockService.getStockMovements(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/stock/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productSku").value("SKU-001"));
    }

    @Test
    void getStockMovements_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/movements"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getStockMovements_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/movements"))
                .andExpect(status().isForbidden());
    }

    // ==================== getStockMovementsByProduct ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getStockMovementsByProduct_authenticated_returns200() throws Exception {
        StockMovementDto dto = StockMovementDto.builder()
                .id(1L)
                .productId(1L)
                .productSku("SKU-001")
                .build();
        PageResponse<StockMovementDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(stockService.getStockMovementsByProduct(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/stock/movements/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productSku").value("SKU-001"));
    }

    @Test
    void getStockMovementsByProduct_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/movements/product/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getStockMovementsByProduct_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/movements/product/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getStockMovementsByLocation ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_VIEW")
    void getStockMovementsByLocation_authenticated_returns200() throws Exception {
        StockMovementDto dto = StockMovementDto.builder()
                .id(1L)
                .toLocationId(1L)
                .toLocationName("Main Warehouse")
                .build();
        PageResponse<StockMovementDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(stockService.getStockMovementsByLocation(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/stock/movements/location/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].toLocationName").value("Main Warehouse"));
    }

    @Test
    void getStockMovementsByLocation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/movements/location/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getStockMovementsByLocation_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock/movements/location/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== receiveStock ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_RECEIVE")
    void receiveStock_valid_returns201() throws Exception {
        StockReceiveRequest request = StockReceiveRequest.builder()
                .locationId(1L)
                .items(List.of(StockReceiveRequest.ReceiveItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("10"))
                        .unitCost(new BigDecimal("50.00"))
                        .build()))
                .build();

        StockMovementDto movement = StockMovementDto.builder()
                .id(1L)
                .movementType(MovementType.STOCK_IN)
                .quantity(new BigDecimal("10"))
                .build();
        when(stockService.receiveStock(any())).thenReturn(List.of(movement));

        mockMvc.perform(post("/api/v1/inventory/stock/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].movementType").value("STOCK_IN"));
    }

    @Test
    void receiveStock_unauthenticated_returns403() throws Exception {
        StockReceiveRequest request = StockReceiveRequest.builder()
                .locationId(1L)
                .items(List.of(StockReceiveRequest.ReceiveItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("10"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/stock/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void receiveStock_noPermission_returns403() throws Exception {
        StockReceiveRequest request = StockReceiveRequest.builder()
                .locationId(1L)
                .items(List.of(StockReceiveRequest.ReceiveItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("10"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/stock/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== issueStock ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_ISSUE")
    void issueStock_valid_returns200() throws Exception {
        StockIssueRequest request = StockIssueRequest.builder()
                .locationId(1L)
                .items(List.of(StockIssueRequest.IssueItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("5"))
                        .build()))
                .build();

        StockMovementDto movement = StockMovementDto.builder()
                .id(1L)
                .movementType(MovementType.STOCK_OUT)
                .build();
        when(stockService.issueStock(any())).thenReturn(List.of(movement));

        mockMvc.perform(post("/api/v1/inventory/stock/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType").value("STOCK_OUT"));
    }

    @Test
    void issueStock_unauthenticated_returns403() throws Exception {
        StockIssueRequest request = StockIssueRequest.builder()
                .locationId(1L)
                .items(List.of(StockIssueRequest.IssueItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("5"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/stock/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void issueStock_noPermission_returns403() throws Exception {
        StockIssueRequest request = StockIssueRequest.builder()
                .locationId(1L)
                .items(List.of(StockIssueRequest.IssueItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("5"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/stock/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== transferStock ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_TRANSFER")
    void transferStock_valid_returns200() throws Exception {
        StockTransferRequest request = StockTransferRequest.builder()
                .fromLocationId(1L)
                .toLocationId(2L)
                .items(List.of(StockTransferRequest.TransferItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("5"))
                        .build()))
                .build();

        StockMovementDto movement = StockMovementDto.builder()
                .id(1L)
                .movementType(MovementType.TRANSFER)
                .build();
        when(stockService.transferStock(any())).thenReturn(List.of(movement));

        mockMvc.perform(post("/api/v1/inventory/stock/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType").value("TRANSFER"));
    }

    @Test
    void transferStock_unauthenticated_returns403() throws Exception {
        StockTransferRequest request = StockTransferRequest.builder()
                .fromLocationId(1L)
                .toLocationId(2L)
                .items(List.of(StockTransferRequest.TransferItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("5"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/stock/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void transferStock_noPermission_returns403() throws Exception {
        StockTransferRequest request = StockTransferRequest.builder()
                .fromLocationId(1L)
                .toLocationId(2L)
                .items(List.of(StockTransferRequest.TransferItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("5"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/stock/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== adjustStock ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_STOCK_ADJUST")
    void adjustStock_valid_returns200() throws Exception {
        StockAdjustRequest request = StockAdjustRequest.builder()
                .locationId(1L)
                .reason("Inventory correction")
                .items(List.of(StockAdjustRequest.AdjustItem.builder()
                        .productId(1L)
                        .adjustmentQuantity(new BigDecimal("-2"))
                        .build()))
                .build();

        StockMovementDto movement = StockMovementDto.builder()
                .id(1L)
                .movementType(MovementType.ADJUSTMENT)
                .build();
        when(stockService.adjustStock(any())).thenReturn(List.of(movement));

        mockMvc.perform(post("/api/v1/inventory/stock/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType").value("ADJUSTMENT"));
    }

    @Test
    void adjustStock_unauthenticated_returns403() throws Exception {
        StockAdjustRequest request = StockAdjustRequest.builder()
                .locationId(1L)
                .reason("Inventory correction")
                .items(List.of(StockAdjustRequest.AdjustItem.builder()
                        .productId(1L)
                        .adjustmentQuantity(new BigDecimal("-2"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/stock/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void adjustStock_noPermission_returns403() throws Exception {
        StockAdjustRequest request = StockAdjustRequest.builder()
                .locationId(1L)
                .reason("Inventory correction")
                .items(List.of(StockAdjustRequest.AdjustItem.builder()
                        .productId(1L)
                        .adjustmentQuantity(new BigDecimal("-2"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/stock/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
