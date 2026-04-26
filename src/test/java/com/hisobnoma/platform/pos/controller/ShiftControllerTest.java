package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.ShiftService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShiftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShiftService shiftService;

    // ==================== GET /api/v1/pos/shifts ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_READ")
    void getAll_authenticated_returns200() throws Exception {
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.findAll(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/pos/shifts"))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAll_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/shifts/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_READ")
    void getById_authenticated_returns200() throws Exception {
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/shifts/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/shifts/current ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_READ")
    void getCurrentShift_authenticated_returns200() throws Exception {
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.getCurrentShiftForUser()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/shifts/current"))
                .andExpect(status().isOk());
    }

    @Test
    void getCurrentShift_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts/current"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/shifts/open ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_READ")
    void getOpenShifts_authenticated_returns200() throws Exception {
        when(shiftService.findOpenShifts()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/shifts/open"))
                .andExpect(status().isOk());
    }

    @Test
    void getOpenShifts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts/open"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/shifts/open ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_OPEN")
    void openShift_authenticated_returns201() throws Exception {
        OpenShiftRequest request = new OpenShiftRequest();
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.openShift(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/shifts/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void openShift_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void openShift_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/shifts/{id}/close ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_CLOSE")
    void closeShift_authenticated_returns200() throws Exception {
        CloseShiftRequest request = new CloseShiftRequest();
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.closeShift(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/shifts/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void closeShift_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void closeShift_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/shifts/{id}/cash-operation ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_CASH_OPERATION")
    void cashOperation_authenticated_returns200() throws Exception {
        CashOperationRequest request = new CashOperationRequest();
        request.setOperationType(CashOperationRequest.OperationType.CASH_IN);
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.cashOperation(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/shifts/1/cash-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void cashOperation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/1/cash-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void cashOperation_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/1/cash-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/shifts/{id}/cash-operations ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_READ")
    void getCashOperations_authenticated_returns200() throws Exception {
        when(shiftService.findCashOperationsByShift(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/shifts/1/cash-operations"))
                .andExpect(status().isOk());
    }

    @Test
    void getCashOperations_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts/1/cash-operations"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/shifts/{id}/reconcile ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_CLOSE")
    void reconcileShift_authenticated_returns200() throws Exception {
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.reconcileShift(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/shifts/1/reconcile"))
                .andExpect(status().isOk());
    }

    @Test
    void reconcileShift_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/1/reconcile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void reconcileShift_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/1/reconcile"))
                .andExpect(status().isForbidden());
    }
}
