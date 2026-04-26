package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.finance.dto.BankTransactionDto;
import com.hisobnoma.platform.finance.dto.CashFlowDto;
import com.hisobnoma.platform.finance.dto.CreateBankTransactionRequest;
import com.hisobnoma.platform.finance.entity.BankTransactionStatus;
import com.hisobnoma.platform.finance.entity.BankTransactionType;
import com.hisobnoma.platform.finance.service.BankTransactionService;
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
class BankTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BankTransactionService bankTransactionService;

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_VIEW")
    void getTransactions_authenticated_returns200() throws Exception {
        // Given
        BankTransactionDto dto = BankTransactionDto.builder()
                .id(1L)
                .transactionNumber("BT-000001")
                .transactionType(BankTransactionType.DEPOSIT)
                .status(BankTransactionStatus.PENDING)
                .creditAmount(new BigDecimal("500.00"))
                .build();

        PageResponse<BankTransactionDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(bankTransactionService.getTransactions(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/bank-transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionNumber").value("BT-000001"));
    }

    @Test
    void getTransactions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-transactions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_VIEW")
    void getTransaction_found_returns200() throws Exception {
        // Given
        BankTransactionDto dto = BankTransactionDto.builder()
                .id(1L)
                .transactionNumber("BT-000001")
                .transactionType(BankTransactionType.DEPOSIT)
                .build();
        when(bankTransactionService.getTransaction(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/bank-transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionNumber").value("BT-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_TRANSACT")
    void createTransaction_valid_returns201() throws Exception {
        // Given
        CreateBankTransactionRequest request = CreateBankTransactionRequest.builder()
                .bankAccountId(1L)
                .transactionDate(LocalDate.now())
                .transactionType(BankTransactionType.DEPOSIT)
                .description("Customer payment")
                .amount(new BigDecimal("500.00"))
                .build();

        BankTransactionDto dto = BankTransactionDto.builder()
                .id(1L)
                .transactionNumber("BT-000001")
                .transactionType(BankTransactionType.DEPOSIT)
                .status(BankTransactionStatus.PENDING)
                .creditAmount(new BigDecimal("500.00"))
                .build();
        when(bankTransactionService.createTransaction(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/bank-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionNumber").value("BT-000001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_VIEW")
    void getTransactionsByAccount_returns200() throws Exception {
        // Given
        BankTransactionDto dto = BankTransactionDto.builder()
                .id(1L).bankAccountId(1L).transactionNumber("BT-000001").build();
        PageResponse<BankTransactionDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(bankTransactionService.getTransactionsByBankAccount(eq(1L), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/bank-transactions/by-account/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].bankAccountId").value(1));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_VIEW")
    void getCashFlow_returns200() throws Exception {
        // Given
        CashFlowDto cashFlowDto = CashFlowDto.builder()
                .periodStart(LocalDate.now().minusDays(30))
                .periodEnd(LocalDate.now())
                .openingBalance(new BigDecimal("9000.00"))
                .totalInflows(new BigDecimal("1000.00"))
                .totalOutflows(new BigDecimal("300.00"))
                .netCashFlow(new BigDecimal("700.00"))
                .closingBalance(new BigDecimal("9700.00"))
                .build();

        when(bankTransactionService.getCashFlow(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(cashFlowDto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/bank-transactions/cash-flow/1")
                        .param("startDate", LocalDate.now().minusDays(30).toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netCashFlow").value(700.00));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_MANAGE")
    void voidTransaction_returns200() throws Exception {
        // Given
        BankTransactionDto voidedDto = BankTransactionDto.builder()
                .id(1L)
                .status(BankTransactionStatus.VOIDED)
                .build();
        when(bankTransactionService.voidTransaction(eq(1L), any())).thenReturn(voidedDto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/bank-transactions/1/void")
                        .param("reason", "Test void"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOIDED"));
    }
}
