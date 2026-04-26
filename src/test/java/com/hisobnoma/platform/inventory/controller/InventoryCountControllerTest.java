package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreateInventoryCountRequest;
import com.hisobnoma.platform.inventory.dto.InventoryCountDto;
import com.hisobnoma.platform.inventory.dto.InventoryCountLineDto;
import com.hisobnoma.platform.inventory.dto.RecordCountRequest;
import com.hisobnoma.platform.inventory.entity.CountStatus;
import com.hisobnoma.platform.inventory.entity.CountType;
import com.hisobnoma.platform.inventory.service.InventoryCountService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryCountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryCountService inventoryCountService;

    // ==================== getInventoryCounts ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_COUNT_VIEW")
    void getInventoryCounts_authenticated_returns200() throws Exception {
        InventoryCountDto dto = InventoryCountDto.builder()
                .id(1L)
                .countNumber("CNT-001")
                .status(CountStatus.DRAFT)
                .countType(CountType.FULL)
                .locationName("Main Warehouse")
                .build();
        PageResponse<InventoryCountDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(inventoryCountService.getInventoryCounts(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].countNumber").value("CNT-001"));
    }

    @Test
    void getInventoryCounts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/counts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getInventoryCounts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/counts"))
                .andExpect(status().isForbidden());
    }

    // ==================== getInventoryCount ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_COUNT_VIEW")
    void getInventoryCount_found_returns200() throws Exception {
        InventoryCountDto dto = InventoryCountDto.builder()
                .id(1L)
                .countNumber("CNT-001")
                .status(CountStatus.DRAFT)
                .build();
        when(inventoryCountService.getInventoryCount(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/counts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countNumber").value("CNT-001"));
    }

    @Test
    void getInventoryCount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/counts/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getInventoryCount_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/counts/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getInventoryCountsByStatus ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_COUNT_VIEW")
    void getInventoryCountsByStatus_authenticated_returns200() throws Exception {
        InventoryCountDto dto = InventoryCountDto.builder()
                .id(1L)
                .countNumber("CNT-001")
                .status(CountStatus.IN_PROGRESS)
                .build();
        PageResponse<InventoryCountDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(inventoryCountService.getInventoryCountsByStatus(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/counts/status/IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"));
    }

    @Test
    void getInventoryCountsByStatus_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/counts/status/IN_PROGRESS"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getInventoryCountsByStatus_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/counts/status/IN_PROGRESS"))
                .andExpect(status().isForbidden());
    }

    // ==================== searchInventoryCounts ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_COUNT_VIEW")
    void searchInventoryCounts_authenticated_returns200() throws Exception {
        InventoryCountDto dto = InventoryCountDto.builder()
                .id(1L)
                .countNumber("CNT-001")
                .build();
        PageResponse<InventoryCountDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(inventoryCountService.searchInventoryCounts(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/counts/search").param("q", "CNT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].countNumber").value("CNT-001"));
    }

    @Test
    void searchInventoryCounts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/counts/search").param("q", "CNT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchInventoryCounts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/counts/search").param("q", "CNT"))
                .andExpect(status().isForbidden());
    }

    // ==================== createInventoryCount ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_COUNT_CREATE")
    void createInventoryCount_valid_returns201() throws Exception {
        CreateInventoryCountRequest request = CreateInventoryCountRequest.builder()
                .locationId(1L)
                .countType(CountType.FULL)
                .countDate(LocalDate.now())
                .description("Annual count")
                .build();

        InventoryCountDto dto = InventoryCountDto.builder()
                .id(1L)
                .countNumber("CNT-001")
                .status(CountStatus.DRAFT)
                .countType(CountType.FULL)
                .build();
        when(inventoryCountService.createInventoryCount(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/inventory/counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countNumber").value("CNT-001"));
    }

    @Test
    void createInventoryCount_unauthenticated_returns403() throws Exception {
        CreateInventoryCountRequest request = CreateInventoryCountRequest.builder()
                .locationId(1L)
                .countType(CountType.FULL)
                .countDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/inventory/counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createInventoryCount_noPermission_returns403() throws Exception {
        CreateInventoryCountRequest request = CreateInventoryCountRequest.builder()
                .locationId(1L)
                .countType(CountType.FULL)
                .countDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/inventory/counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== startCount ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_COUNT_PERFORM")
    void startCount_returns200() throws Exception {
        InventoryCountDto dto = InventoryCountDto.builder()
                .id(1L)
                .countNumber("CNT-001")
                .status(CountStatus.IN_PROGRESS)
                .build();
        when(inventoryCountService.startCount(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/counts/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void startCount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/counts/1/start"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void startCount_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/counts/1/start"))
                .andExpect(status().isForbidden());
    }

    // ==================== recordCount ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_COUNT_PERFORM")
    void recordCount_valid_returns200() throws Exception {
        RecordCountRequest request = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("95"))
                .notes("Slight variance found")
                .build();

        InventoryCountLineDto dto = InventoryCountLineDto.builder()
                .id(1L)
                .inventoryCountId(1L)
                .productSku("SKU-001")
                .countedQuantity(new BigDecimal("95"))
                .counted(true)
                .build();
        when(inventoryCountService.recordCount(any(), any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/counts/1/lines/1/count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counted").value(true));
    }

    @Test
    void recordCount_unauthenticated_returns403() throws Exception {
        RecordCountRequest request = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("95"))
                .build();

        mockMvc.perform(put("/api/v1/inventory/counts/1/lines/1/count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void recordCount_noPermission_returns403() throws Exception {
        RecordCountRequest request = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("95"))
                .build();

        mockMvc.perform(put("/api/v1/inventory/counts/1/lines/1/count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== markForRecount ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_COUNT_PERFORM")
    void markForRecount_returns200() throws Exception {
        doNothing().when(inventoryCountService).markForRecount(any(), any(), any());

        mockMvc.perform(put("/api/v1/inventory/counts/1/lines/1/recount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Variance too high"))))
                .andExpect(status().isOk());
    }

    @Test
    void markForRecount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/counts/1/lines/1/recount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Variance too high"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void markForRecount_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/counts/1/lines/1/recount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Variance too high"))))
                .andExpect(status().isForbidden());
    }

    // ==================== completeCount ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_COUNT_PERFORM")
    void completeCount_returns200() throws Exception {
        InventoryCountDto dto = InventoryCountDto.builder()
                .id(1L)
                .countNumber("CNT-001")
                .status(CountStatus.COMPLETED)
                .build();
        when(inventoryCountService.completeCount(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/counts/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void completeCount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/counts/1/complete"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void completeCount_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/counts/1/complete"))
                .andExpect(status().isForbidden());
    }

    // ==================== approveCount ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_COUNT_APPROVE")
    void approveCount_returns200() throws Exception {
        InventoryCountDto dto = InventoryCountDto.builder()
                .id(1L)
                .countNumber("CNT-001")
                .status(CountStatus.APPROVED)
                .build();
        when(inventoryCountService.approveCount(1L)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/counts/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approveCount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/counts/1/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void approveCount_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/counts/1/approve"))
                .andExpect(status().isForbidden());
    }

    // ==================== cancelCount ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_COUNT_APPROVE")
    void cancelCount_returns200() throws Exception {
        InventoryCountDto dto = InventoryCountDto.builder()
                .id(1L)
                .countNumber("CNT-001")
                .status(CountStatus.CANCELLED)
                .cancellationReason("No longer needed")
                .build();
        when(inventoryCountService.cancelCount(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/counts/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "No longer needed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelCount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/counts/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "No longer needed"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void cancelCount_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/counts/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "No longer needed"))))
                .andExpect(status().isForbidden());
    }
}
