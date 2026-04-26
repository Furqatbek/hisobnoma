package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.finance.dto.BankReconciliationDto;
import com.hisobnoma.platform.finance.dto.BankTransactionDto;
import com.hisobnoma.platform.finance.dto.CreateBankReconciliationRequest;
import com.hisobnoma.platform.finance.dto.ReconcileTransactionsRequest;
import com.hisobnoma.platform.finance.entity.ReconciliationStatus;
import com.hisobnoma.platform.finance.service.BankReconciliationService;
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
class BankReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BankReconciliationService reconciliationService;

    // ==================== GET /api/v1/finance/bank-reconciliations ====================

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_RECONCILE")
    void getReconciliations_authenticated_returns200() throws Exception {
        // Given
        BankReconciliationDto dto = BankReconciliationDto.builder()
                .id(1L)
                .reconciliationNumber("REC-001")
                .bankAccountCode("BANK-001")
                .status(ReconciliationStatus.DRAFT)
                .statementDate(LocalDate.of(2026, 1, 31))
                .build();

        PageResponse<BankReconciliationDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(reconciliationService.getReconciliations(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/bank-reconciliations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reconciliationNumber").value("REC-001"));
    }

    @Test
    void getReconciliations_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-reconciliations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getReconciliations_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-reconciliations"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/bank-reconciliations/by-account/{bankAccountId} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_RECONCILE")
    void getReconciliationsByBankAccount_returns200() throws Exception {
        // Given
        BankReconciliationDto dto = BankReconciliationDto.builder()
                .id(1L)
                .reconciliationNumber("REC-001")
                .bankAccountId(5L)
                .build();

        PageResponse<BankReconciliationDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(reconciliationService.getReconciliationsByBankAccount(eq(5L), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/bank-reconciliations/by-account/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reconciliationNumber").value("REC-001"));
    }

    // ==================== GET /api/v1/finance/bank-reconciliations/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_RECONCILE")
    void getReconciliation_found_returns200() throws Exception {
        // Given
        BankReconciliationDto dto = BankReconciliationDto.builder()
                .id(1L)
                .reconciliationNumber("REC-001")
                .status(ReconciliationStatus.DRAFT)
                .build();
        when(reconciliationService.getReconciliation(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/bank-reconciliations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationNumber").value("REC-001"));
    }

    @Test
    void getReconciliation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-reconciliations/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getReconciliation_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-reconciliations/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/bank-reconciliations/unreconciled/{bankAccountId} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_RECONCILE")
    void getUnreconciledTransactions_returns200() throws Exception {
        // Given
        BankTransactionDto dto = BankTransactionDto.builder()
                .id(1L)
                .transactionNumber("TXN-001")
                .bankAccountId(5L)
                .debitAmount(new BigDecimal("1000.00"))
                .build();
        when(reconciliationService.getUnreconciledTransactions(eq(5L), any(LocalDate.class)))
                .thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/bank-reconciliations/unreconciled/5")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionNumber").value("TXN-001"));
    }

    // ==================== POST /api/v1/finance/bank-reconciliations ====================

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_RECONCILE")
    void createReconciliation_valid_returns201() throws Exception {
        // Given
        CreateBankReconciliationRequest request = CreateBankReconciliationRequest.builder()
                .bankAccountId(5L)
                .statementDate(LocalDate.of(2026, 1, 31))
                .statementStartDate(LocalDate.of(2026, 1, 1))
                .statementEndDate(LocalDate.of(2026, 1, 31))
                .openingBalance(new BigDecimal("10000.00"))
                .statementEndingBalance(new BigDecimal("12000.00"))
                .build();

        BankReconciliationDto dto = BankReconciliationDto.builder()
                .id(1L)
                .reconciliationNumber("REC-001")
                .bankAccountId(5L)
                .status(ReconciliationStatus.DRAFT)
                .openingBalance(new BigDecimal("10000.00"))
                .statementEndingBalance(new BigDecimal("12000.00"))
                .build();
        when(reconciliationService.createReconciliation(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/bank-reconciliations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reconciliationNumber").value("REC-001"));
    }

    @Test
    void createReconciliation_unauthenticated_returns403() throws Exception {
        CreateBankReconciliationRequest request = CreateBankReconciliationRequest.builder()
                .bankAccountId(5L)
                .statementDate(LocalDate.of(2026, 1, 31))
                .statementStartDate(LocalDate.of(2026, 1, 1))
                .statementEndDate(LocalDate.of(2026, 1, 31))
                .openingBalance(new BigDecimal("10000.00"))
                .statementEndingBalance(new BigDecimal("12000.00"))
                .build();

        mockMvc.perform(post("/api/v1/finance/bank-reconciliations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createReconciliation_noPermission_returns403() throws Exception {
        CreateBankReconciliationRequest request = CreateBankReconciliationRequest.builder()
                .bankAccountId(5L)
                .statementDate(LocalDate.of(2026, 1, 31))
                .statementStartDate(LocalDate.of(2026, 1, 1))
                .statementEndDate(LocalDate.of(2026, 1, 31))
                .openingBalance(new BigDecimal("10000.00"))
                .statementEndingBalance(new BigDecimal("12000.00"))
                .build();

        mockMvc.perform(post("/api/v1/finance/bank-reconciliations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/finance/bank-reconciliations/{id}/start ====================

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_RECONCILE")
    void startReconciliation_returns200() throws Exception {
        // Given
        BankReconciliationDto dto = BankReconciliationDto.builder()
                .id(1L)
                .reconciliationNumber("REC-001")
                .status(ReconciliationStatus.IN_PROGRESS)
                .build();
        when(reconciliationService.startReconciliation(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/bank-reconciliations/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // ==================== POST /api/v1/finance/bank-reconciliations/reconcile-transactions ====================

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_RECONCILE")
    void reconcileTransactions_returns200() throws Exception {
        // Given
        ReconcileTransactionsRequest request = ReconcileTransactionsRequest.builder()
                .reconciliationId(1L)
                .transactionIds(List.of(10L, 20L, 30L))
                .clear(true)
                .build();

        BankReconciliationDto dto = BankReconciliationDto.builder()
                .id(1L)
                .reconciliationNumber("REC-001")
                .status(ReconciliationStatus.IN_PROGRESS)
                .transactionsCleared(3)
                .build();
        when(reconciliationService.reconcileTransactions(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/bank-reconciliations/reconcile-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionsCleared").value(3));
    }

    // ==================== PUT /api/v1/finance/bank-reconciliations/{id}/complete ====================

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_RECONCILE")
    void completeReconciliation_returns200() throws Exception {
        // Given
        BankReconciliationDto dto = BankReconciliationDto.builder()
                .id(1L)
                .reconciliationNumber("REC-001")
                .status(ReconciliationStatus.COMPLETED)
                .difference(BigDecimal.ZERO)
                .build();
        when(reconciliationService.completeReconciliation(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/bank-reconciliations/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // ==================== PUT /api/v1/finance/bank-reconciliations/{id}/cancel ====================

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_RECONCILE")
    void cancelReconciliation_returns200() throws Exception {
        // Given
        BankReconciliationDto dto = BankReconciliationDto.builder()
                .id(1L)
                .reconciliationNumber("REC-001")
                .status(ReconciliationStatus.CANCELLED)
                .cancellationReason("Data entry error")
                .build();
        when(reconciliationService.cancelReconciliation(eq(1L), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/bank-reconciliations/1/cancel")
                        .param("reason", "Data entry error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelReconciliation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/finance/bank-reconciliations/1/cancel"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void cancelReconciliation_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/finance/bank-reconciliations/1/cancel"))
                .andExpect(status().isForbidden());
    }
}
