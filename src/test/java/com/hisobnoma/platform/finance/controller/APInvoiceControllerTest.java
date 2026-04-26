package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.APInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceLineRequest;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceRequest;
import com.hisobnoma.platform.finance.entity.APInvoiceStatus;
import com.hisobnoma.platform.finance.service.APInvoiceService;
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
class APInvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private APInvoiceService apInvoiceService;

    private static final String BASE_URL = "/api/v1/ap/invoices";

    // ========== GET /api/v1/ap/invoices ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_VIEW")
    void getInvoices_authenticatedWithPermission_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .vendorName("Test Vendor")
                .status(APInvoiceStatus.DRAFT)
                .totalAmount(new BigDecimal("250.00"))
                .build();
        PageResponse<APInvoiceDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(apInvoiceService.getInvoices(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("AP-000001"))
                .andExpect(jsonPath("$.content[0].vendorName").value("Test Vendor"));
    }

    @Test
    void getInvoices_unauthenticated_returns403() throws Exception {
        // When/Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getInvoices_noPermission_returns403() throws Exception {
        // When/Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    // ========== GET /api/v1/ap/invoices/{id} ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_VIEW")
    void getInvoice_found_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .vendorName("Test Vendor")
                .status(APInvoiceStatus.DRAFT)
                .totalAmount(new BigDecimal("250.00"))
                .balanceDue(new BigDecimal("250.00"))
                .build();
        when(apInvoiceService.getInvoice(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber").value("AP-000001"))
                .andExpect(jsonPath("$.data.vendorName").value("Test Vendor"))
                .andExpect(jsonPath("$.data.totalAmount").value(250.00));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AP_VIEW")
    void getInvoice_notFound_returns404() throws Exception {
        // Given
        when(apInvoiceService.getInvoice(999L))
                .thenThrow(new NotFoundException("AP Invoice not found with id: 999"));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/v1/ap/invoices/vendor/{vendorId} ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_VIEW")
    void getInvoicesByVendor_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .vendorId(1L)
                .status(APInvoiceStatus.DRAFT)
                .build();
        PageResponse<APInvoiceDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(apInvoiceService.getInvoicesByVendor(eq(1L), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/vendor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vendorId").value(1));
    }

    // ========== GET /api/v1/ap/invoices/status/{status} ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_VIEW")
    void getInvoicesByStatus_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.APPROVED)
                .build();
        PageResponse<APInvoiceDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(apInvoiceService.getInvoicesByStatus(eq(APInvoiceStatus.APPROVED), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get(BASE_URL + "/status/APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
    }

    // ========== GET /api/v1/ap/invoices/vendor/{vendorId}/unpaid ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_VIEW")
    void getUnpaidInvoicesByVendor_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .vendorId(1L)
                .status(APInvoiceStatus.APPROVED)
                .balanceDue(new BigDecimal("500.00"))
                .build();
        when(apInvoiceService.getUnpaidInvoicesByVendor(1L)).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/vendor/1/unpaid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].invoiceNumber").value("AP-000001"))
                .andExpect(jsonPath("$.data[0].balanceDue").value(500.00));
    }

    // ========== GET /api/v1/ap/invoices/overdue ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_VIEW")
    void getOverdueInvoices_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.APPROVED)
                .overdue(true)
                .daysOverdue(10)
                .build();
        when(apInvoiceService.getOverdueInvoices()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].overdue").value(true))
                .andExpect(jsonPath("$.data[0].daysOverdue").value(10));
    }

    // ========== POST /api/v1/ap/invoices ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_CREATE")
    void createInvoice_validRequest_returns201() throws Exception {
        // Given
        CreateAPInvoiceLineRequest lineRequest = CreateAPInvoiceLineRequest.builder()
                .description("Office Supplies")
                .quantity(new BigDecimal("10.0000"))
                .unitPrice(new BigDecimal("25.0000"))
                .build();

        CreateAPInvoiceRequest request = CreateAPInvoiceRequest.builder()
                .vendorId(1L)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .description("Test invoice")
                .lines(List.of(lineRequest))
                .build();

        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .vendorName("Test Vendor")
                .status(APInvoiceStatus.DRAFT)
                .totalAmount(new BigDecimal("250.0000"))
                .build();
        when(apInvoiceService.createInvoice(any(CreateAPInvoiceRequest.class))).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber").value("AP-000001"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AP_CREATE")
    void createInvoice_missingRequiredFields_returns400() throws Exception {
        // Given - missing invoiceDate, dueDate, and lines
        CreateAPInvoiceRequest request = CreateAPInvoiceRequest.builder()
                .vendorId(1L)
                .description("Test invoice")
                .build();

        // When/Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createInvoice_unauthenticated_returns403() throws Exception {
        // Given
        CreateAPInvoiceLineRequest lineRequest = CreateAPInvoiceLineRequest.builder()
                .description("Office Supplies")
                .quantity(new BigDecimal("10.0000"))
                .unitPrice(new BigDecimal("25.0000"))
                .build();

        CreateAPInvoiceRequest request = CreateAPInvoiceRequest.builder()
                .vendorId(1L)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .lines(List.of(lineRequest))
                .build();

        // When/Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ========== POST /api/v1/ap/invoices/{id}/approve ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_APPROVE")
    void approveInvoice_success_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.APPROVED)
                .glPosted(true)
                .build();
        when(apInvoiceService.approveInvoice(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.glPosted").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AP_APPROVE")
    void approveInvoice_invalidStatus_returns400() throws Exception {
        // Given
        when(apInvoiceService.approveInvoice(1L))
                .thenThrow(new BusinessException("Invoice cannot be approved in current status: PAID"));

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/approve"))
                .andExpect(status().isBadRequest());
    }

    // ========== POST /api/v1/ap/invoices/{id}/cancel ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_CANCEL")
    void cancelInvoice_success_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.CANCELLED)
                .build();
        when(apInvoiceService.cancelInvoice(eq(1L), eq("No longer needed"))).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"No longer needed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AP_CANCEL")
    void cancelInvoice_hasPayments_returns400() throws Exception {
        // Given
        when(apInvoiceService.cancelInvoice(eq(1L), any()))
                .thenThrow(new BusinessException("Invoice has payments applied. Cannot cancel."));

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Test\"}"))
                .andExpect(status().isBadRequest());
    }

    // ========== POST /api/v1/ap/invoices/{id}/submit ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_CREATE")
    void submitForApproval_success_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.PENDING_APPROVAL)
                .build();
        when(apInvoiceService.submitForApproval(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));
    }

    // ========== POST /api/v1/ap/invoices/{id}/reject ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_APPROVE")
    void rejectInvoice_success_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.DRAFT)
                .rejectionReason("Incorrect amounts")
                .build();
        when(apInvoiceService.rejectInvoice(eq(1L), eq("Incorrect amounts"))).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Incorrect amounts\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    // ========== POST /api/v1/ap/invoices/{id}/hold ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_UPDATE")
    void holdInvoice_success_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.ON_HOLD)
                .build();
        when(apInvoiceService.holdInvoice(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/hold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ON_HOLD"));
    }

    // ========== POST /api/v1/ap/invoices/{id}/release-hold ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_UPDATE")
    void releaseHold_success_returns200() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.DRAFT)
                .build();
        when(apInvoiceService.releaseHold(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/1/release-hold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    // ========== POST /api/v1/ap/invoices/from-receiving/{receivingOrderId} ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_CREATE")
    void createFromReceiving_success_returns201() throws Exception {
        // Given
        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.DRAFT)
                .receivingOrderId(1L)
                .receivingOrderNumber("RCV-000001")
                .build();
        when(apInvoiceService.createFromReceiving(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post(BASE_URL + "/from-receiving/1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber").value("AP-000001"))
                .andExpect(jsonPath("$.data.receivingOrderId").value(1));
    }

    // ========== PUT /api/v1/ap/invoices/{id} ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_UPDATE")
    void updateInvoice_success_returns200() throws Exception {
        // Given
        CreateAPInvoiceLineRequest lineRequest = CreateAPInvoiceLineRequest.builder()
                .description("Updated Supplies")
                .quantity(new BigDecimal("15.0000"))
                .unitPrice(new BigDecimal("30.0000"))
                .build();

        CreateAPInvoiceRequest request = CreateAPInvoiceRequest.builder()
                .vendorId(1L)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .description("Updated invoice")
                .lines(List.of(lineRequest))
                .build();

        APInvoiceDto dto = APInvoiceDto.builder()
                .id(1L)
                .invoiceNumber("AP-000001")
                .status(APInvoiceStatus.DRAFT)
                .totalAmount(new BigDecimal("450.0000"))
                .build();
        when(apInvoiceService.updateInvoice(eq(1L), any(CreateAPInvoiceRequest.class))).thenReturn(dto);

        // When/Then
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAmount").value(450.0000));
    }

    // ========== Summary endpoints ==========

    @Test
    @WithMockUser(authorities = "FINANCE_AP_VIEW")
    void getTotalPayable_returns200() throws Exception {
        // Given
        when(apInvoiceService.getTotalPayable()).thenReturn(new BigDecimal("10000.00"));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/summary/total-payable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(10000.00));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AP_VIEW")
    void getVendorBalance_returns200() throws Exception {
        // Given
        when(apInvoiceService.getVendorBalance(1L)).thenReturn(new BigDecimal("5000.00"));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/summary/vendor/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(5000.00));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AP_VIEW")
    void getOverdueBalance_returns200() throws Exception {
        // Given
        when(apInvoiceService.getOverdueBalance()).thenReturn(new BigDecimal("2000.00"));

        // When/Then
        mockMvc.perform(get(BASE_URL + "/summary/overdue-balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(2000.00));
    }
}
