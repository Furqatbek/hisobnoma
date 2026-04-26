package com.hisobnoma.platform.mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.ShiftService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
class MobileShiftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShiftService shiftService;

    // ==================== GET /api/v1/mobile/shifts/current ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_READ")
    void getCurrentShift_authenticated_returns200() throws Exception {
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.getCurrentShiftForUser()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/mobile/shifts/current"))
                .andExpect(status().isOk());
    }

    @Test
    void getCurrentShift_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/shifts/current"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCurrentShift_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/shifts/current"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/shifts/open ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_READ")
    void getOpenShifts_authenticated_returns200() throws Exception {
        when(shiftService.findOpenShifts()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/mobile/shifts/open"))
                .andExpect(status().isOk());
    }

    @Test
    void getOpenShifts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/shifts/open"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getOpenShifts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/shifts/open"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/mobile/shifts/open ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_OPEN")
    void openShift_authenticated_returns201() throws Exception {
        OpenShiftRequest request = OpenShiftRequest.builder()
                .terminalId(1L)
                .openingCash(new java.math.BigDecimal("100000"))
                .build();
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.openShift(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/mobile/shifts/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void openShift_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/shifts/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void openShift_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/shifts/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/mobile/shifts/{id}/close ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_CLOSE")
    void closeShift_authenticated_returns200() throws Exception {
        CloseShiftRequest request = CloseShiftRequest.builder()
                .closingCash(new java.math.BigDecimal("150000"))
                .build();
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.closeShift(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/mobile/shifts/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void closeShift_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/shifts/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void closeShift_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/shifts/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/mobile/shifts/{id}/cash-operation ====================

    @Test
    @WithMockUser(authorities = "POS_SHIFT_CASH_OPERATION")
    void cashOperation_authenticated_returns200() throws Exception {
        CashOperationRequest request = CashOperationRequest.builder()
                .operationType(CashOperationRequest.OperationType.CASH_IN)
                .amount(new java.math.BigDecimal("50000"))
                .build();
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.cashOperation(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/mobile/shifts/1/cash-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void cashOperation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/shifts/1/cash-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void cashOperation_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/shifts/1/cash-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
