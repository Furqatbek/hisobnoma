package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreatePurchaseOrderLineRequest;
import com.hisobnoma.platform.inventory.dto.CreatePurchaseOrderRequest;
import com.hisobnoma.platform.inventory.dto.PurchaseOrderDto;
import com.hisobnoma.platform.inventory.entity.POStatus;
import com.hisobnoma.platform.inventory.service.PurchaseOrderService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PurchaseOrderService purchaseOrderService;

    // ==================== getPurchaseOrders ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PO_VIEW")
    void getPurchaseOrders_authenticated_returns200() throws Exception {
        PurchaseOrderDto dto = PurchaseOrderDto.builder()
                .id(1L)
                .poNumber("PO-001")
                .vendorName("Test Vendor")
                .status(POStatus.DRAFT)
                .totalAmount(new BigDecimal("5000.00"))
                .build();
        PageResponse<PurchaseOrderDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(purchaseOrderService.getPurchaseOrders(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/purchase-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].poNumber").value("PO-001"));
    }

    @Test
    void getPurchaseOrders_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/purchase-orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getPurchaseOrders_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/purchase-orders"))
                .andExpect(status().isForbidden());
    }

    // ==================== getPurchaseOrder ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PO_VIEW")
    void getPurchaseOrder_found_returns200() throws Exception {
        PurchaseOrderDto dto = PurchaseOrderDto.builder()
                .id(1L)
                .poNumber("PO-001")
                .status(POStatus.DRAFT)
                .build();
        when(purchaseOrderService.getPurchaseOrder(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/purchase-orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poNumber").value("PO-001"));
    }

    @Test
    void getPurchaseOrder_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/purchase-orders/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getPurchaseOrder_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/purchase-orders/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getPurchaseOrdersByStatus ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PO_VIEW")
    void getPurchaseOrdersByStatus_authenticated_returns200() throws Exception {
        PurchaseOrderDto dto = PurchaseOrderDto.builder()
                .id(1L)
                .poNumber("PO-001")
                .status(POStatus.APPROVED)
                .build();
        PageResponse<PurchaseOrderDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(purchaseOrderService.getPurchaseOrdersByStatus(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/purchase-orders/status/APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
    }

    @Test
    void getPurchaseOrdersByStatus_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/purchase-orders/status/APPROVED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getPurchaseOrdersByStatus_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/purchase-orders/status/APPROVED"))
                .andExpect(status().isForbidden());
    }

    // ==================== getPurchaseOrdersByVendor ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PO_VIEW")
    void getPurchaseOrdersByVendor_authenticated_returns200() throws Exception {
        PurchaseOrderDto dto = PurchaseOrderDto.builder()
                .id(1L)
                .poNumber("PO-001")
                .vendorId(1L)
                .vendorName("Test Vendor")
                .build();
        PageResponse<PurchaseOrderDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(purchaseOrderService.getPurchaseOrdersByVendor(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/purchase-orders/vendor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vendorName").value("Test Vendor"));
    }

    @Test
    void getPurchaseOrdersByVendor_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/purchase-orders/vendor/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getPurchaseOrdersByVendor_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/purchase-orders/vendor/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== searchPurchaseOrders ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PO_VIEW")
    void searchPurchaseOrders_authenticated_returns200() throws Exception {
        PurchaseOrderDto dto = PurchaseOrderDto.builder()
                .id(1L)
                .poNumber("PO-001")
                .build();
        PageResponse<PurchaseOrderDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(purchaseOrderService.searchPurchaseOrders(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/purchase-orders/search").param("q", "PO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].poNumber").value("PO-001"));
    }

    @Test
    void searchPurchaseOrders_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/purchase-orders/search").param("q", "PO"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchPurchaseOrders_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/purchase-orders/search").param("q", "PO"))
                .andExpect(status().isForbidden());
    }

    // ==================== createPurchaseOrder ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PO_CREATE")
    void createPurchaseOrder_valid_returns201() throws Exception {
        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .vendorId(1L)
                .locationId(1L)
                .orderDate(LocalDate.now())
                .lines(List.of(CreatePurchaseOrderLineRequest.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("10"))
                        .uomId(1L)
                        .unitPrice(new BigDecimal("50.00"))
                        .build()))
                .build();

        PurchaseOrderDto dto = PurchaseOrderDto.builder()
                .id(1L)
                .poNumber("PO-001")
                .status(POStatus.DRAFT)
                .totalAmount(new BigDecimal("500.00"))
                .build();
        when(purchaseOrderService.createPurchaseOrder(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/inventory/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.poNumber").value("PO-001"));
    }

    @Test
    void createPurchaseOrder_unauthenticated_returns403() throws Exception {
        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .vendorId(1L)
                .locationId(1L)
                .orderDate(LocalDate.now())
                .lines(List.of(CreatePurchaseOrderLineRequest.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("10"))
                        .uomId(1L)
                        .unitPrice(new BigDecimal("50.00"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createPurchaseOrder_noPermission_returns403() throws Exception {
        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .vendorId(1L)
                .locationId(1L)
                .orderDate(LocalDate.now())
                .lines(List.of(CreatePurchaseOrderLineRequest.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("10"))
                        .uomId(1L)
                        .unitPrice(new BigDecimal("50.00"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== submitForApproval ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PO_UPDATE")
    void submitForApproval_returns200() throws Exception {
        PurchaseOrderDto dto = PurchaseOrderDto.builder()
                .id(1L)
                .poNumber("PO-001")
                .status(POStatus.PENDING)
                .build();
        when(purchaseOrderService.submitForApproval(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void submitForApproval_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/submit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void submitForApproval_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/submit"))
                .andExpect(status().isForbidden());
    }

    // ==================== approvePurchaseOrder ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PO_APPROVE")
    void approvePurchaseOrder_returns200() throws Exception {
        PurchaseOrderDto dto = PurchaseOrderDto.builder()
                .id(1L)
                .poNumber("PO-001")
                .status(POStatus.APPROVED)
                .build();
        when(purchaseOrderService.approvePurchaseOrder(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approvePurchaseOrder_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void approvePurchaseOrder_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/approve"))
                .andExpect(status().isForbidden());
    }

    // ==================== receivePurchaseOrder ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PO_UPDATE")
    void receivePurchaseOrder_returns200() throws Exception {
        PurchaseOrderDto dto = PurchaseOrderDto.builder()
                .id(1L)
                .poNumber("PO-001")
                .status(POStatus.RECEIVED)
                .build();
        when(purchaseOrderService.receivePurchaseOrder(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/receive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void receivePurchaseOrder_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/receive"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void receivePurchaseOrder_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/receive"))
                .andExpect(status().isForbidden());
    }

    // ==================== cancelPurchaseOrder ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PO_CANCEL")
    void cancelPurchaseOrder_returns200() throws Exception {
        PurchaseOrderDto dto = PurchaseOrderDto.builder()
                .id(1L)
                .poNumber("PO-001")
                .status(POStatus.CANCELLED)
                .cancellationReason("No longer needed")
                .build();
        when(purchaseOrderService.cancelPurchaseOrder(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "No longer needed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelPurchaseOrder_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "No longer needed"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void cancelPurchaseOrder_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/purchase-orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "No longer needed"))))
                .andExpect(status().isForbidden());
    }
}
