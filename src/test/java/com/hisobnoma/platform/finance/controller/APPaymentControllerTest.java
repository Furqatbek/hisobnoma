package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.APPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateAPPaymentAllocationRequest;
import com.hisobnoma.platform.finance.dto.CreateAPPaymentRequest;
import com.hisobnoma.platform.finance.entity.APPaymentMethod;
import com.hisobnoma.platform.finance.entity.APPaymentStatus;
import com.hisobnoma.platform.finance.service.APPaymentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class APPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private APPaymentService apPaymentService;

    private static final String BASE_URL = "/api/v1/ap/payments";

    // ========== GET /api/v1/ap/payments ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_VIEW")
    void getPayments_authenticatedWithPermission_returns200() throws Exception {
        // Given
        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .vendorName("Test Vendor")
                .status(APPaymentStatus.DRAFT)
                .paymentAmount(new BigDecimal("500.00"))
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .build();
        PageResponse<APPaymentDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(apPaymentService.getPayments(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentNumber").value("PAY-000001"))
                .andExpect(jsonPath("$.content[0].vendorName").value("Test Vendor"));
    }

    @Test
    void getPayments_unauthenticated_returns403() throws Exception {
        // When/Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getPayments_noPermission_returns403() throws Exception {
        // When/Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    // ========== GET /api/v1/ap/payments/{id} ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_VIEW")
    void getPayment_found_returns200() throws Exception {
        // Given
        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .vendorName("Test Vendor")
                .status(APPaymentStatus.COMPLETED)
                .paymentAmount(new BigDecimal("500.00"))
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .fullyAllocated(true)
                .availableAmount(BigDecimal.ZERO)
                .build();
        when(apPaymentService.getPayment(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentNumber").value("PAY-000001"))
                .andExpect(jsonPath("$.data.vendorName").value("Test Vendor"))
                .andExpect(jsonPath("$.data.paymentAmount").value(500.00))
                .andExpect(jsonPath("$.data.fullyAllocated").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_VIEW")
    void getPayment_notFound_returns404() throws Exception {
        // Given
        when(apPaymentService.getPayment(999L))
                .thenThrow(new NotFoundException("AP Payment not found with id: 999"));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/v1/ap/payments/vendor/{vendorId} ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_VIEW")
    void getPaymentsByVendor_returns200() throws Exception {
        // Given
        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .vendorId(1L)
                .status(APPaymentStatus.COMPLETED)
                .paymentAmount(new BigDecimal("500.00"))
                .build();
        PageResponse<APPaymentDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(apPaymentService.getPaymentsByVendor(eq(1L), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/vendor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vendorId").value(1));
    }

    // ========== GET /api/v1/ap/payments/status/{status} ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_VIEW")
    void getPaymentsByStatus_returns200() throws Exception {
        // Given
        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .status(APPaymentStatus.COMPLETED)
                .paymentAmount(new BigDecimal("500.00"))
                .build();
        PageResponse<APPaymentDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(apPaymentService.getPaymentsByStatus(eq(APPaymentStatus.COMPLETED), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/status/COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
    }

    // ========== GET /api/v1/ap/payments/date-range ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_VIEW")
    void getPaymentsByDateRange_returns200() throws Exception {
        // Given
        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .paymentDate(LocalDate.of(2025, 3, 15))
                .status(APPaymentStatus.COMPLETED)
                .paymentAmount(new BigDecimal("500.00"))
                .build();
        when(apPaymentService.getPaymentsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/date-range")
                        .param("startDate", "2025-03-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].paymentNumber").value("PAY-000001"));
    }

    // ========== GET /api/v1/ap/payments/unreconciled ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_VIEW")
    void getUnreconciledPayments_returns200() throws Exception {
        // Given
        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .status(APPaymentStatus.COMPLETED)
                .reconciled(false)
                .build();
        when(apPaymentService.getUnreconciledPayments()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/unreconciled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].reconciled").value(false));
    }

    // ========== POST /api/v1/ap/payments ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY")
    void createPayment_validRequest_returns201() throws Exception {
        // Given
        CreateAPPaymentAllocationRequest allocationRequest = CreateAPPaymentAllocationRequest.builder()
                .apInvoiceId(1L)
                .allocatedAmount(new BigDecimal("500.0000"))
                .build();

        CreateAPPaymentRequest request = CreateAPPaymentRequest.builder()
                .vendorId(1L)
                .paymentDate(LocalDate.now())
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("500.0000"))
                .bankAccountId(1L)
                .allocations(List.of(allocationRequest))
                .build();

        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .vendorName("Test Vendor")
                .status(APPaymentStatus.DRAFT)
                .paymentAmount(new BigDecimal("500.0000"))
                .build();
        when(apPaymentService.createPayment(any(CreateAPPaymentRequest.class))).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentNumber").value("PAY-000001"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY")
    void createPayment_missingRequiredFields_returns400() throws Exception {
        // Given - missing paymentDate, paymentMethod, paymentAmount, allocations
        CreateAPPaymentRequest request = CreateAPPaymentRequest.builder()
                .vendorId(1L)
                .build();

        // When/Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_unauthenticated_returns403() throws Exception {
        // Given
        CreateAPPaymentAllocationRequest allocationRequest = CreateAPPaymentAllocationRequest.builder()
                .apInvoiceId(1L)
                .allocatedAmount(new BigDecimal("500.0000"))
                .build();

        CreateAPPaymentRequest request = CreateAPPaymentRequest.builder()
                .vendorId(1L)
                .paymentDate(LocalDate.now())
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("500.0000"))
                .allocations(List.of(allocationRequest))
                .build();

        // When/Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ========== POST /api/v1/ap/payments/{id}/submit ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY")
    void submitForApproval_success_returns200() throws Exception {
        // Given
        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .status(APPaymentStatus.PENDING_APPROVAL)
                .build();
        when(apPaymentService.submitForApproval(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));
    }

    // ========== POST /api/v1/ap/payments/{id}/approve ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_APPROVE")
    void approvePayment_success_returns200() throws Exception {
        // Given
        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .status(APPaymentStatus.APPROVED)
                .build();
        when(apPaymentService.approvePayment(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_APPROVE")
    void approvePayment_invalidStatus_returns400() throws Exception {
        // Given
        when(apPaymentService.approvePayment(1L))
                .thenThrow(new BusinessException("Payment cannot be approved in current status: COMPLETED"));

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/approve"))
                .andExpect(status().isBadRequest());
    }

    // ========== POST /api/v1/ap/payments/{id}/process ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY")
    void processPayment_success_returns200() throws Exception {
        // Given
        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .status(APPaymentStatus.COMPLETED)
                .glPosted(true)
                .build();
        when(apPaymentService.processPayment(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.glPosted").value(true));
    }

    // ========== POST /api/v1/ap/payments/{id}/void ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_VOID")
    void voidPayment_success_returns200() throws Exception {
        // Given
        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .status(APPaymentStatus.VOIDED)
                .voidReason("Duplicate payment")
                .build();
        when(apPaymentService.voidPayment(eq(1L), eq("Duplicate payment"))).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/void")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Duplicate payment\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("VOIDED"))
                .andExpect(jsonPath("$.data.voidReason").value("Duplicate payment"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_VOID")
    void voidPayment_alreadyVoided_returns400() throws Exception {
        // Given
        when(apPaymentService.voidPayment(eq(1L), any()))
                .thenThrow(new BusinessException("Payment is already voided"));

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/void")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Test\"}"))
                .andExpect(status().isBadRequest());
    }

    // ========== POST /api/v1/ap/payments/{id}/reconcile ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY")
    void reconcilePayment_success_returns200() throws Exception {
        // Given
        APPaymentDto dto = APPaymentDto.builder()
                .id(1L)
                .paymentNumber("PAY-000001")
                .status(APPaymentStatus.COMPLETED)
                .reconciled(true)
                .build();
        when(apPaymentService.reconcilePayment(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reconciled").value(true));
    }

    // ========== Summary endpoints ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_VIEW")
    void getTotalPaymentsByVendor_returns200() throws Exception {
        // Given
        when(apPaymentService.getTotalPaymentsByVendor(1L)).thenReturn(new BigDecimal("5000.00"));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/summary/vendor/1/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(5000.00));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AP_PAY_VIEW")
    void getTotalPaymentsByDateRange_returns200() throws Exception {
        // Given
        when(apPaymentService.getTotalPaymentsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("15000.00"));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/summary/date-range/total")
                        .param("startDate", "2025-03-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(15000.00));
    }
}
