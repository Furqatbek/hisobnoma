package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.ShiftService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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

    // ==================== GET /api/v1/pos/shifts ====================

    @Test
    void getAll_authenticated_returns200() throws Exception {
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.findAll(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/pos/shifts").with(userWithPermission("POS_SHIFT_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/shifts/{id} ====================

    @Test
    void getById_authenticated_returns200() throws Exception {
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/shifts/1").with(userWithPermission("POS_SHIFT_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts/1").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/shifts/current ====================

    @Test
    void getCurrentShift_authenticated_returns200() throws Exception {
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.getCurrentShiftForUser()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/shifts/current").with(userWithPermission("POS_SHIFT_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getCurrentShift_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts/current"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/shifts/open ====================

    @Test
    void getOpenShifts_authenticated_returns200() throws Exception {
        when(shiftService.findOpenShifts()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/shifts/open").with(userWithPermission("POS_SHIFT_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getOpenShifts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts/open"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/shifts/open ====================

    @Test
    void openShift_authenticated_returns201() throws Exception {
        OpenShiftRequest request = new OpenShiftRequest();
        request.setTerminalId(1L);
        request.setOpeningCash(java.math.BigDecimal.ZERO);
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.openShift(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/shifts/open")
                        .with(userWithPermission("POS_SHIFT_OPEN"))
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
    void openShift_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/open")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminalId\":1,\"openingCash\":0}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/shifts/{id}/close ====================

    @Test
    void closeShift_authenticated_returns200() throws Exception {
        CloseShiftRequest request = new CloseShiftRequest();
        request.setClosingCash(java.math.BigDecimal.ZERO);
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.closeShift(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/shifts/1/close")
                        .with(userWithPermission("POS_SHIFT_CLOSE"))
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
    void closeShift_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/1/close")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingCash\":0}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/shifts/{id}/cash-operation ====================

    @Test
    void cashOperation_authenticated_returns200() throws Exception {
        CashOperationRequest request = new CashOperationRequest();
        request.setOperationType(CashOperationRequest.OperationType.CASH_IN);
        request.setAmount(new java.math.BigDecimal("100.00"));
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.cashOperation(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/shifts/1/cash-operation")
                        .with(userWithPermission("POS_SHIFT_CASH_OPERATION"))
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
    void cashOperation_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/1/cash-operation")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operationType\":\"CASH_IN\",\"amount\":100.00}"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/shifts/{id}/cash-operations ====================

    @Test
    void getCashOperations_authenticated_returns200() throws Exception {
        when(shiftService.findCashOperationsByShift(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/shifts/1/cash-operations").with(userWithPermission("POS_SHIFT_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getCashOperations_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/shifts/1/cash-operations"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/shifts/{id}/reconcile ====================

    @Test
    void reconcileShift_authenticated_returns200() throws Exception {
        ShiftDto dto = ShiftDto.builder().id(1L).build();
        when(shiftService.reconcileShift(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/shifts/1/reconcile").with(userWithPermission("POS_SHIFT_CLOSE")))
                .andExpect(status().isOk());
    }

    @Test
    void reconcileShift_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/1/reconcile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reconcileShift_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/shifts/1/reconcile").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }
}
