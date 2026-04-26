package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceLineRequest;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
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
class ARInvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ARInvoiceService arInvoiceService;

    private static final String BASE_URL = "/api/v1/finance/ar-invoices";

    private ARInvoiceDto createSampleInvoiceDto() {
        return ARInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("INV-000001")
                .customerId(1L)
                .customerName("Test Customer")
                .invoiceDate(LocalDate.of(2025, 1, 15))
                .dueDate(LocalDate.of(2025, 2, 14))
                .status(ARInvoiceStatus.DRAFT)
                .subtotal(new BigDecimal("1000.00"))
                .totalAmount(new BigDecimal("1000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("1000.00"))
                .currency("UZS")
                .build();
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getInvoices_authenticated_returns200() throws Exception {
        // Given
        ARInvoiceDto dto = createSampleInvoiceDto();
        PageResponse<ARInvoiceDto> pageResponse = PageResponse.of(List.of(dto), 0, 10, 1);
        when(arInvoiceService.getInvoices(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-000001"));
    }

    @Test
    void getInvoices_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getInvoices_wrongPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getInvoice_authenticated_returns200() throws Exception {
        // Given
        ARInvoiceDto dto = createSampleInvoiceDto();
        when(arInvoiceService.getInvoice(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getInvoiceByNumber_authenticated_returns200() throws Exception {
        // Given
        ARInvoiceDto dto = createSampleInvoiceDto();
        when(arInvoiceService.getInvoiceByNumber("INV-000001")).thenReturn(dto);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/number/INV-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getInvoicesByCustomer_authenticated_returns200() throws Exception {
        // Given
        ARInvoiceDto dto = createSampleInvoiceDto();
        PageResponse<ARInvoiceDto> pageResponse = PageResponse.of(List.of(dto), 0, 10, 1);
        when(arInvoiceService.getInvoicesByCustomer(eq(1L), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getInvoicesByStatus_authenticated_returns200() throws Exception {
        // Given
        ARInvoiceDto dto = createSampleInvoiceDto();
        PageResponse<ARInvoiceDto> pageResponse = PageResponse.of(List.of(dto), 0, 10, 1);
        when(arInvoiceService.getInvoicesByStatus(eq(ARInvoiceStatus.DRAFT), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/status/DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getUnpaidInvoicesByCustomer_authenticated_returns200() throws Exception {
        // Given
        ARInvoiceDto dto = createSampleInvoiceDto();
        when(arInvoiceService.getUnpaidInvoicesByCustomer(1L)).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/customer/1/unpaid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getOverdueInvoices_authenticated_returns200() throws Exception {
        // Given
        ARInvoiceDto dto = createSampleInvoiceDto();
        when(arInvoiceService.getOverdueInvoices()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void createInvoice_authenticated_returns201() throws Exception {
        // Given
        CreateARInvoiceLineRequest lineRequest = CreateARInvoiceLineRequest.builder()
                .description("Test Item")
                .quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("500.00"))
                .build();

        CreateARInvoiceRequest request = CreateARInvoiceRequest.builder()
                .customerId(1L)
                .invoiceDate(LocalDate.of(2025, 1, 15))
                .dueDate(LocalDate.of(2025, 2, 14))
                .totalAmount(new BigDecimal("1000.00"))
                .lines(List.of(lineRequest))
                .build();

        ARInvoiceDto dto = createSampleInvoiceDto();
        when(arInvoiceService.createInvoice(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void updateInvoice_authenticated_returns200() throws Exception {
        // Given
        CreateARInvoiceLineRequest lineRequest = CreateARInvoiceLineRequest.builder()
                .description("Updated Item")
                .quantity(new BigDecimal("3"))
                .unitPrice(new BigDecimal("500.00"))
                .build();

        CreateARInvoiceRequest request = CreateARInvoiceRequest.builder()
                .customerId(1L)
                .invoiceDate(LocalDate.of(2025, 1, 15))
                .dueDate(LocalDate.of(2025, 2, 14))
                .totalAmount(new BigDecimal("1500.00"))
                .lines(List.of(lineRequest))
                .build();

        ARInvoiceDto dto = createSampleInvoiceDto();
        when(arInvoiceService.updateInvoice(eq(1L), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_APPROVE")
    void postInvoice_authenticated_returns200() throws Exception {
        // Given
        ARInvoiceDto dto = createSampleInvoiceDto();
        dto.setStatus(ARInvoiceStatus.PENDING);
        when(arInvoiceService.postInvoice(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void sendInvoice_authenticated_returns200() throws Exception {
        // Given
        ARInvoiceDto dto = createSampleInvoiceDto();
        dto.setStatus(ARInvoiceStatus.SENT);
        when(arInvoiceService.sendInvoice(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/send"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_APPROVE")
    void cancelInvoice_authenticated_returns200() throws Exception {
        // Given
        ARInvoiceDto dto = createSampleInvoiceDto();
        dto.setStatus(ARInvoiceStatus.CANCELLED);
        when(arInvoiceService.cancelInvoice(eq(1L), eq("No longer needed"))).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/cancel")
                        .param("reason", "No longer needed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCustomerTotalOutstanding_authenticated_returns200() throws Exception {
        // Given
        when(arInvoiceService.getCustomerTotalOutstanding(1L)).thenReturn(new BigDecimal("5000.00"));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/customer/1/outstanding"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void checkOverdueInvoices_authenticated_returns200() throws Exception {
        // When/Then
        mockMvc.perform(post(BASE_URL + "/check-overdue"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void postInvoice_withReadOnlyPermission_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL + "/1/post"))
                .andExpect(status().isForbidden());
    }
}
