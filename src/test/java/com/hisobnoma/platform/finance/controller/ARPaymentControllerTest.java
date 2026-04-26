package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.ARPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateARPaymentAllocationRequest;
import com.hisobnoma.platform.finance.dto.CreateARPaymentRequest;
import com.hisobnoma.platform.finance.entity.ARPaymentMethod;
import com.hisobnoma.platform.finance.entity.ARPaymentStatus;
import com.hisobnoma.platform.finance.service.ARPaymentService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ARPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ARPaymentService arPaymentService;

    private static final String BASE_URL = "/api/v1/finance/ar-payments";

    private ARPaymentDto createSamplePaymentDto() {
        return ARPaymentDto.builder()
                .id(1L)
                .paymentNumber("REC-000001")
                .customerId(1L)
                .customerName("Test Customer")
                .paymentDate(LocalDate.of(2025, 3, 15))
                .status(ARPaymentStatus.PENDING)
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("1000.00"))
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(new BigDecimal("1000.00"))
                .currency("UZS")
                .build();
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getPayments_authenticated_returns200() throws Exception {
        // Given
        ARPaymentDto dto = createSamplePaymentDto();
        PageResponse<ARPaymentDto> pageResponse = PageResponse.of(List.of(dto), 0, 10, 1);
        when(arPaymentService.getPayments(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentNumber").value("REC-000001"));
    }

    @Test
    void getPayments_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getPayments_wrongPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getPayment_authenticated_returns200() throws Exception {
        // Given
        ARPaymentDto dto = createSamplePaymentDto();
        when(arPaymentService.getPayment(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentNumber").value("REC-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getPaymentByNumber_authenticated_returns200() throws Exception {
        // Given
        ARPaymentDto dto = createSamplePaymentDto();
        when(arPaymentService.getPaymentByNumber("REC-000001")).thenReturn(dto);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/number/REC-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentNumber").value("REC-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getPaymentsByCustomer_authenticated_returns200() throws Exception {
        // Given
        ARPaymentDto dto = createSamplePaymentDto();
        PageResponse<ARPaymentDto> pageResponse = PageResponse.of(List.of(dto), 0, 10, 1);
        when(arPaymentService.getPaymentsByCustomer(eq(1L), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentNumber").value("REC-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getPaymentsByStatus_authenticated_returns200() throws Exception {
        // Given
        ARPaymentDto dto = createSamplePaymentDto();
        PageResponse<ARPaymentDto> pageResponse = PageResponse.of(List.of(dto), 0, 10, 1);
        when(arPaymentService.getPaymentsByStatus(eq(ARPaymentStatus.PENDING), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentNumber").value("REC-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getPaymentsByDateRange_authenticated_returns200() throws Exception {
        // Given
        ARPaymentDto dto = createSamplePaymentDto();
        when(arPaymentService.getPaymentsByDateRange(any(), any())).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/date-range")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentNumber").value("REC-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void createPayment_authenticated_returns201() throws Exception {
        // Given
        CreateARPaymentAllocationRequest allocRequest = CreateARPaymentAllocationRequest.builder()
                .arInvoiceId(1L)
                .allocatedAmount(new BigDecimal("1000.00"))
                .build();

        CreateARPaymentRequest request = CreateARPaymentRequest.builder()
                .customerId(1L)
                .paymentDate(LocalDate.of(2025, 3, 15))
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("1000.00"))
                .allocations(List.of(allocRequest))
                .build();

        ARPaymentDto dto = createSamplePaymentDto();
        when(arPaymentService.createPayment(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentNumber").value("REC-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_APPROVE")
    void completePayment_authenticated_returns200() throws Exception {
        // Given
        ARPaymentDto dto = createSamplePaymentDto();
        dto.setStatus(ARPaymentStatus.COMPLETED);
        when(arPaymentService.completePayment(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void createAndCompletePayment_authenticated_returns201() throws Exception {
        // Given
        CreateARPaymentAllocationRequest allocRequest = CreateARPaymentAllocationRequest.builder()
                .arInvoiceId(1L)
                .allocatedAmount(new BigDecimal("1000.00"))
                .build();

        CreateARPaymentRequest request = CreateARPaymentRequest.builder()
                .customerId(1L)
                .paymentDate(LocalDate.of(2025, 3, 15))
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("1000.00"))
                .allocations(List.of(allocRequest))
                .build();

        ARPaymentDto dto = createSamplePaymentDto();
        dto.setStatus(ARPaymentStatus.COMPLETED);
        when(arPaymentService.createAndCompletePayment(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void addAllocation_authenticated_returns200() throws Exception {
        // Given
        CreateARPaymentAllocationRequest allocRequest = CreateARPaymentAllocationRequest.builder()
                .arInvoiceId(1L)
                .allocatedAmount(new BigDecimal("500.00"))
                .build();

        ARPaymentDto dto = createSamplePaymentDto();
        when(arPaymentService.addAllocation(eq(1L), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentNumber").value("REC-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void removeAllocation_authenticated_returns200() throws Exception {
        // Given
        ARPaymentDto dto = createSamplePaymentDto();
        when(arPaymentService.removeAllocation(1L, 10L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(delete(BASE_URL + "/1/allocations/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentNumber").value("REC-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_APPROVE")
    void cancelPayment_authenticated_returns200() throws Exception {
        // Given
        ARPaymentDto dto = createSamplePaymentDto();
        dto.setStatus(ARPaymentStatus.CANCELLED);
        when(arPaymentService.cancelPayment(eq(1L), eq("Duplicate"))).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/cancel")
                        .param("reason", "Duplicate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void markAsDeposited_authenticated_returns200() throws Exception {
        // Given
        ARPaymentDto dto = createSamplePaymentDto();
        dto.setDeposited(true);
        when(arPaymentService.markAsDeposited(eq(1L), eq("DEP-001"))).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/deposit")
                        .param("bankReference", "DEP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deposited").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getUndepositedPayments_authenticated_returns200() throws Exception {
        // Given
        ARPaymentDto dto = createSamplePaymentDto();
        when(arPaymentService.getUndepositedPayments()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/undeposited"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentNumber").value("REC-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getTotalPaymentsByDateRange_authenticated_returns200() throws Exception {
        // Given
        when(arPaymentService.getTotalPaymentsByDateRange(any(), any())).thenReturn(new BigDecimal("5000.00"));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/total-by-date-range")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void completePayment_withReadOnlyPermission_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL + "/1/complete"))
                .andExpect(status().isForbidden());
    }
}
