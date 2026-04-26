package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.finance.dto.BankAccountDto;
import com.hisobnoma.platform.finance.dto.CreateBankAccountRequest;
import com.hisobnoma.platform.finance.entity.BankAccountType;
import com.hisobnoma.platform.finance.service.BankAccountService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BankAccountService bankAccountService;

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_VIEW")
    void getBankAccounts_authenticated_returns200() throws Exception {
        // Given
        BankAccountDto dto = BankAccountDto.builder()
                .id(1L)
                .accountCode("BANK-001")
                .accountName("Main Account")
                .accountType(BankAccountType.CHECKING)
                .currentBalance(new BigDecimal("10000.00"))
                .active(true)
                .build();

        PageResponse<BankAccountDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(bankAccountService.getBankAccounts(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/bank-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountCode").value("BANK-001"));
    }

    @Test
    void getBankAccounts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getBankAccounts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-accounts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_VIEW")
    void getBankAccount_found_returns200() throws Exception {
        // Given
        BankAccountDto dto = BankAccountDto.builder()
                .id(1L)
                .accountCode("BANK-001")
                .accountName("Main Account")
                .accountType(BankAccountType.CHECKING)
                .build();
        when(bankAccountService.getBankAccount(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/bank-accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountCode").value("BANK-001"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_MANAGE")
    void createBankAccount_valid_returns201() throws Exception {
        // Given
        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountCode("BANK-002")
                .accountName("New Account")
                .accountType(BankAccountType.SAVINGS)
                .build();

        BankAccountDto dto = BankAccountDto.builder()
                .id(2L)
                .accountCode("BANK-002")
                .accountName("New Account")
                .accountType(BankAccountType.SAVINGS)
                .build();
        when(bankAccountService.createBankAccount(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountCode").value("BANK-002"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_MANAGE")
    void updateBankAccount_valid_returns200() throws Exception {
        // Given
        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountCode("BANK-001")
                .accountName("Updated Account")
                .accountType(BankAccountType.CHECKING)
                .build();

        BankAccountDto dto = BankAccountDto.builder()
                .id(1L)
                .accountCode("BANK-001")
                .accountName("Updated Account")
                .build();
        when(bankAccountService.updateBankAccount(any(), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/bank-accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountName").value("Updated Account"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_VIEW")
    void getActiveBankAccounts_returns200() throws Exception {
        // Given
        BankAccountDto dto = BankAccountDto.builder()
                .id(1L).accountCode("BANK-001").active(true).build();
        when(bankAccountService.getActiveBankAccounts()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/bank-accounts/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_BANK_MANAGE")
    void deactivateBankAccount_returns200() throws Exception {
        // Given
        BankAccountDto dto = BankAccountDto.builder()
                .id(1L).accountCode("BANK-001").active(false).build();
        when(bankAccountService.deactivateBankAccount(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/bank-accounts/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
