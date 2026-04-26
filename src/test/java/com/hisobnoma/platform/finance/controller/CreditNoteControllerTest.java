package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.finance.dto.CreateCreditNoteRequest;
import com.hisobnoma.platform.finance.dto.CreditNoteDto;
import com.hisobnoma.platform.finance.entity.CreditNoteStatus;
import com.hisobnoma.platform.finance.service.CreditNoteService;
import com.hisobnoma.platform.common.dto.PageResponse;
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
class CreditNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreditNoteService creditNoteService;

    // ==================== GET /api/v1/finance/credit-notes ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCreditNotes_authenticated_returns200() throws Exception {
        // Given
        CreditNoteDto dto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-001")
                .customerName("Test Customer")
                .status(CreditNoteStatus.DRAFT)
                .totalAmount(new BigDecimal("500.00"))
                .build();

        PageResponse<CreditNoteDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(creditNoteService.getCreditNotes(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/credit-notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].creditNoteNumber").value("CN-001"));
    }

    @Test
    void getCreditNotes_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/credit-notes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCreditNotes_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/credit-notes"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/credit-notes/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCreditNote_found_returns200() throws Exception {
        // Given
        CreditNoteDto dto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-001")
                .customerName("Test Customer")
                .status(CreditNoteStatus.DRAFT)
                .build();
        when(creditNoteService.getCreditNote(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/credit-notes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditNoteNumber").value("CN-001"));
    }

    @Test
    void getCreditNote_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/credit-notes/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCreditNote_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/credit-notes/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/credit-notes/customer/{customerId} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCreditNotesByCustomer_returns200() throws Exception {
        // Given
        CreditNoteDto dto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-001")
                .customerId(10L)
                .build();

        PageResponse<CreditNoteDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(creditNoteService.getCreditNotesByCustomer(eq(10L), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/credit-notes/customer/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].creditNoteNumber").value("CN-001"));
    }

    // ==================== GET /api/v1/finance/credit-notes/status/{status} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCreditNotesByStatus_returns200() throws Exception {
        // Given
        CreditNoteDto dto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-001")
                .status(CreditNoteStatus.APPROVED)
                .build();

        PageResponse<CreditNoteDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(creditNoteService.getCreditNotesByStatus(eq(CreditNoteStatus.APPROVED), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/credit-notes/status/APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
    }

    // ==================== GET /api/v1/finance/credit-notes/number/{creditNoteNumber} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCreditNoteByNumber_returns200() throws Exception {
        // Given
        CreditNoteDto dto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-001")
                .build();
        when(creditNoteService.getCreditNoteByNumber("CN-001")).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/credit-notes/number/CN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditNoteNumber").value("CN-001"));
    }

    // ==================== GET /api/v1/finance/credit-notes/customer/{customerId}/available ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getAvailableCreditNotes_returns200() throws Exception {
        // Given
        CreditNoteDto dto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-001")
                .status(CreditNoteStatus.APPROVED)
                .build();
        when(creditNoteService.getAvailableCreditNotes(10L)).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/credit-notes/customer/10/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].creditNoteNumber").value("CN-001"));
    }

    // ==================== GET /api/v1/finance/credit-notes/customer/{customerId}/available-credit ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getAvailableCredit_returns200() throws Exception {
        // Given
        when(creditNoteService.getAvailableCredit(10L)).thenReturn(new BigDecimal("1500.00"));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/credit-notes/customer/10/available-credit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1500.00));
    }

    // ==================== POST /api/v1/finance/credit-notes ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void createCreditNote_valid_returns201() throws Exception {
        // Given
        CreateCreditNoteRequest request = CreateCreditNoteRequest.builder()
                .customerId(10L)
                .creditNoteDate(LocalDate.of(2026, 1, 15))
                .creditAmount(new BigDecimal("500.00"))
                .reason("Defective goods")
                .build();

        CreditNoteDto dto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-001")
                .customerId(10L)
                .status(CreditNoteStatus.DRAFT)
                .totalAmount(new BigDecimal("500.00"))
                .build();
        when(creditNoteService.createCreditNote(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/credit-notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditNoteNumber").value("CN-001"));
    }

    @Test
    void createCreditNote_unauthenticated_returns403() throws Exception {
        CreateCreditNoteRequest request = CreateCreditNoteRequest.builder()
                .customerId(10L)
                .creditNoteDate(LocalDate.of(2026, 1, 15))
                .creditAmount(new BigDecimal("500.00"))
                .build();

        mockMvc.perform(post("/api/v1/finance/credit-notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createCreditNote_noPermission_returns403() throws Exception {
        CreateCreditNoteRequest request = CreateCreditNoteRequest.builder()
                .customerId(10L)
                .creditNoteDate(LocalDate.of(2026, 1, 15))
                .creditAmount(new BigDecimal("500.00"))
                .build();

        mockMvc.perform(post("/api/v1/finance/credit-notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/finance/credit-notes/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void updateCreditNote_valid_returns200() throws Exception {
        // Given
        CreateCreditNoteRequest request = CreateCreditNoteRequest.builder()
                .customerId(10L)
                .creditNoteDate(LocalDate.of(2026, 1, 15))
                .creditAmount(new BigDecimal("600.00"))
                .reason("Updated reason")
                .build();

        CreditNoteDto dto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-001")
                .totalAmount(new BigDecimal("600.00"))
                .reason("Updated reason")
                .build();
        when(creditNoteService.updateCreditNote(any(), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/credit-notes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Updated reason"));
    }

    // ==================== POST /api/v1/finance/credit-notes/{id}/approve ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_APPROVE")
    void approveCreditNote_returns200() throws Exception {
        // Given
        CreditNoteDto dto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-001")
                .status(CreditNoteStatus.APPROVED)
                .build();
        when(creditNoteService.approveCreditNote(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/credit-notes/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approveCreditNote_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/finance/credit-notes/1/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void approveCreditNote_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/finance/credit-notes/1/approve"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/finance/credit-notes/{id}/apply-to-invoice ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_WRITE")
    void applyToInvoice_returns200() throws Exception {
        // Given
        CreditNoteDto dto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-001")
                .status(CreditNoteStatus.APPLIED)
                .appliedAmount(new BigDecimal("300.00"))
                .build();
        when(creditNoteService.applyToInvoice(eq(1L), eq(5L), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/credit-notes/1/apply-to-invoice")
                        .param("invoiceId", "5")
                        .param("amount", "300.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    // ==================== POST /api/v1/finance/credit-notes/{id}/cancel ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_APPROVE")
    void cancelCreditNote_returns200() throws Exception {
        // Given
        CreditNoteDto dto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-001")
                .status(CreditNoteStatus.CANCELLED)
                .build();
        when(creditNoteService.cancelCreditNote(eq(1L), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/credit-notes/1/cancel")
                        .param("reason", "No longer needed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelCreditNote_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/finance/credit-notes/1/cancel")
                        .param("reason", "No longer needed"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void cancelCreditNote_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/finance/credit-notes/1/cancel")
                        .param("reason", "No longer needed"))
                .andExpect(status().isForbidden());
    }
}
