package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreateReceivingLineRequest;
import com.hisobnoma.platform.inventory.dto.CreateReceivingRequest;
import com.hisobnoma.platform.inventory.dto.ReceivingOrderDto;
import com.hisobnoma.platform.inventory.entity.ReceivingStatus;
import com.hisobnoma.platform.inventory.service.ReceivingService;
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
class ReceivingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReceivingService receivingService;

    // ==================== getReceivingOrders ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_RECEIVING_VIEW")
    void getReceivingOrders_authenticated_returns200() throws Exception {
        ReceivingOrderDto dto = ReceivingOrderDto.builder()
                .id(1L)
                .receivingNumber("RCV-001")
                .vendorName("Test Vendor")
                .status(ReceivingStatus.DRAFT)
                .totalAmount(new BigDecimal("3000.00"))
                .build();
        PageResponse<ReceivingOrderDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(receivingService.getReceivingOrders(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/receiving"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].receivingNumber").value("RCV-001"));
    }

    @Test
    void getReceivingOrders_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/receiving"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getReceivingOrders_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/receiving"))
                .andExpect(status().isForbidden());
    }

    // ==================== getReceivingOrder ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_RECEIVING_VIEW")
    void getReceivingOrder_found_returns200() throws Exception {
        ReceivingOrderDto dto = ReceivingOrderDto.builder()
                .id(1L)
                .receivingNumber("RCV-001")
                .status(ReceivingStatus.DRAFT)
                .build();
        when(receivingService.getReceivingOrder(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/receiving/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivingNumber").value("RCV-001"));
    }

    @Test
    void getReceivingOrder_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/receiving/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getReceivingOrder_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/receiving/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getReceivingOrdersByStatus ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_RECEIVING_VIEW")
    void getReceivingOrdersByStatus_authenticated_returns200() throws Exception {
        ReceivingOrderDto dto = ReceivingOrderDto.builder()
                .id(1L)
                .receivingNumber("RCV-001")
                .status(ReceivingStatus.COMPLETED)
                .build();
        PageResponse<ReceivingOrderDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(receivingService.getReceivingOrdersByStatus(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/receiving/status/COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
    }

    @Test
    void getReceivingOrdersByStatus_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/receiving/status/COMPLETED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getReceivingOrdersByStatus_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/receiving/status/COMPLETED"))
                .andExpect(status().isForbidden());
    }

    // ==================== getReceivingOrdersForPO ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_RECEIVING_VIEW")
    void getReceivingOrdersForPO_authenticated_returns200() throws Exception {
        ReceivingOrderDto dto = ReceivingOrderDto.builder()
                .id(1L)
                .receivingNumber("RCV-001")
                .purchaseOrderId(1L)
                .poNumber("PO-001")
                .build();
        when(receivingService.getReceivingOrdersForPO(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/inventory/receiving/purchase-order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].poNumber").value("PO-001"));
    }

    @Test
    void getReceivingOrdersForPO_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/receiving/purchase-order/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getReceivingOrdersForPO_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/receiving/purchase-order/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== searchReceivingOrders ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_RECEIVING_VIEW")
    void searchReceivingOrders_authenticated_returns200() throws Exception {
        ReceivingOrderDto dto = ReceivingOrderDto.builder()
                .id(1L)
                .receivingNumber("RCV-001")
                .build();
        PageResponse<ReceivingOrderDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(receivingService.searchReceivingOrders(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/receiving/search").param("q", "RCV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].receivingNumber").value("RCV-001"));
    }

    @Test
    void searchReceivingOrders_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/receiving/search").param("q", "RCV"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchReceivingOrders_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/receiving/search").param("q", "RCV"))
                .andExpect(status().isForbidden());
    }

    // ==================== createReceivingOrder ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_RECEIVING_CREATE")
    void createReceivingOrder_valid_returns201() throws Exception {
        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .vendorId(1L)
                .locationId(1L)
                .receivingDate(LocalDate.now())
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .productId(1L)
                        .receivedQuantity(new BigDecimal("10"))
                        .uomId(1L)
                        .unitCost(new BigDecimal("50.00"))
                        .build()))
                .build();

        ReceivingOrderDto dto = ReceivingOrderDto.builder()
                .id(1L)
                .receivingNumber("RCV-001")
                .status(ReceivingStatus.DRAFT)
                .build();
        when(receivingService.createReceivingOrder(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/inventory/receiving")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receivingNumber").value("RCV-001"));
    }

    @Test
    void createReceivingOrder_unauthenticated_returns403() throws Exception {
        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .vendorId(1L)
                .locationId(1L)
                .receivingDate(LocalDate.now())
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .productId(1L)
                        .receivedQuantity(new BigDecimal("10"))
                        .uomId(1L)
                        .unitCost(new BigDecimal("50.00"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/receiving")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createReceivingOrder_noPermission_returns403() throws Exception {
        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .vendorId(1L)
                .locationId(1L)
                .receivingDate(LocalDate.now())
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .productId(1L)
                        .receivedQuantity(new BigDecimal("10"))
                        .uomId(1L)
                        .unitCost(new BigDecimal("50.00"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/inventory/receiving")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== confirmReceiving ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_RECEIVING_CONFIRM")
    void confirmReceiving_returns200() throws Exception {
        ReceivingOrderDto dto = ReceivingOrderDto.builder()
                .id(1L)
                .receivingNumber("RCV-001")
                .status(ReceivingStatus.COMPLETED)
                .build();
        when(receivingService.confirmReceiving(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/receiving/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void confirmReceiving_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/receiving/1/confirm"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void confirmReceiving_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/receiving/1/confirm"))
                .andExpect(status().isForbidden());
    }

    // ==================== cancelReceiving ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_RECEIVING_CONFIRM")
    void cancelReceiving_returns200() throws Exception {
        ReceivingOrderDto dto = ReceivingOrderDto.builder()
                .id(1L)
                .receivingNumber("RCV-001")
                .status(ReceivingStatus.CANCELLED)
                .cancellationReason("Wrong items")
                .build();
        when(receivingService.cancelReceiving(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/receiving/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Wrong items"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelReceiving_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/receiving/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Wrong items"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void cancelReceiving_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/receiving/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Wrong items"))))
                .andExpect(status().isForbidden());
    }
}
