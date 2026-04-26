package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.AccountDto;
import com.hisobnoma.platform.finance.dto.CreateAccountRequest;
import com.hisobnoma.platform.finance.entity.AccountType;
import com.hisobnoma.platform.finance.entity.NormalBalance;
import com.hisobnoma.platform.finance.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private static final String BASE_URL = "/api/v1/finance/accounts";

    private AccountDto buildAccountDto() {
        return AccountDto.builder()
                .id(1L)
                .code("1110")
                .name("Cash")
                .description("Cash account")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .accountLevel(1)
                .active(true)
                .allowsDirectPosting(true)
                .currentBalance(new BigDecimal("10000.00"))
                .currency("UZS")
                .build();
    }

    // ==================== GET /api/v1/finance/accounts ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_VIEW")
    void getAccounts_returns200() throws Exception {
        // Given
        AccountDto dto = buildAccountDto();
        PageResponse<AccountDto> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(dto))
        );
        when(accountService.getAccounts(any())).thenReturn(pageResponse);

        // When / Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("1110"))
                .andExpect(jsonPath("$.content[0].name").value("Cash"))
                .andExpect(jsonPath("$.content[0].accountType").value("ASSET"));
    }

    @Test
    void getAccounts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAccounts_noPermission_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/accounts/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_VIEW")
    void getAccount_returns200() throws Exception {
        // Given
        AccountDto dto = buildAccountDto();
        when(accountService.getAccount(1L)).thenReturn(dto);

        // When / Then
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("1110"))
                .andExpect(jsonPath("$.name").value("Cash"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_GL_VIEW")
    void getAccount_notFound_returns404() throws Exception {
        // Given
        when(accountService.getAccount(999L)).thenThrow(new NotFoundException("Account not found with id: 999"));

        // When / Then
        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    // ==================== POST /api/v1/finance/accounts ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_MANAGE")
    void createAccount_returns201() throws Exception {
        // Given
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .build();

        AccountDto dto = buildAccountDto();
        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(dto);

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("1110"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_GL_MANAGE")
    void createAccount_duplicateCode_returns400() throws Exception {
        // Given
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .build();

        when(accountService.createAccount(any(CreateAccountRequest.class)))
                .thenThrow(new BusinessException("Account with code '1110' already exists"));

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_GL_MANAGE")
    void createAccount_invalidRequest_returns400() throws Exception {
        // Given - missing required fields
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("")
                .name("")
                .build();

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createAccount_noPermission_returns403() throws Exception {
        // Given
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .build();

        // When / Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/finance/accounts/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_MANAGE")
    void updateAccount_returns200() throws Exception {
        // Given
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1110")
                .name("Cash Updated")
                .accountType(AccountType.ASSET)
                .build();

        AccountDto updatedDto = AccountDto.builder()
                .id(1L)
                .code("1110")
                .name("Cash Updated")
                .accountType(AccountType.ASSET)
                .build();

        when(accountService.updateAccount(eq(1L), any(CreateAccountRequest.class))).thenReturn(updatedDto);

        // When / Then
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cash Updated"));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_GL_MANAGE")
    void updateAccount_notFound_returns404() throws Exception {
        // Given
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .build();

        when(accountService.updateAccount(eq(999L), any(CreateAccountRequest.class)))
                .thenThrow(new NotFoundException("Account not found with id: 999"));

        // When / Then
        mockMvc.perform(put(BASE_URL + "/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE /api/v1/finance/accounts/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_MANAGE")
    void deleteAccount_returns204() throws Exception {
        // Given
        doNothing().when(accountService).deleteAccount(1L);

        // When / Then
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());

        verify(accountService).deleteAccount(1L);
    }

    @Test
    @WithMockUser(authorities = "FINANCE_GL_MANAGE")
    void deleteAccount_hasEntries_returns400() throws Exception {
        // Given
        doThrow(new BusinessException("Cannot delete account with existing transactions. Deactivate instead."))
                .when(accountService).deleteAccount(1L);

        // When / Then
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "FINANCE_GL_MANAGE")
    void deleteAccount_notFound_returns404() throws Exception {
        // Given
        doThrow(new NotFoundException("Account not found with id: 999"))
                .when(accountService).deleteAccount(999L);

        // When / Then
        mockMvc.perform(delete(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /api/v1/finance/accounts/type/{accountType} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_VIEW")
    void getAccountsByType_returns200() throws Exception {
        // Given
        AccountDto dto = buildAccountDto();
        when(accountService.getAccountsByType(AccountType.ASSET)).thenReturn(List.of(dto));

        // When / Then
        mockMvc.perform(get(BASE_URL + "/type/ASSET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountType").value("ASSET"));
    }

    // ==================== GET /api/v1/finance/accounts/code/{code} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_VIEW")
    void getAccountByCode_returns200() throws Exception {
        // Given
        AccountDto dto = buildAccountDto();
        when(accountService.getAccountByCode("1110")).thenReturn(dto);

        // When / Then
        mockMvc.perform(get(BASE_URL + "/code/1110"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1110"));
    }

    // ==================== PUT /api/v1/finance/accounts/{id}/activate ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_MANAGE")
    void activateAccount_returns200() throws Exception {
        // Given
        AccountDto dto = buildAccountDto();
        when(accountService.activateAccount(1L)).thenReturn(dto);

        // When / Then
        mockMvc.perform(put(BASE_URL + "/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ==================== PUT /api/v1/finance/accounts/{id}/deactivate ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_MANAGE")
    void deactivateAccount_returns200() throws Exception {
        // Given
        AccountDto dto = AccountDto.builder()
                .id(1L)
                .code("1110")
                .name("Cash")
                .active(false)
                .build();
        when(accountService.deactivateAccount(1L)).thenReturn(dto);

        // When / Then
        mockMvc.perform(put(BASE_URL + "/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ==================== GET /api/v1/finance/accounts/search ====================

    @Test
    @WithMockUser(authorities = "FINANCE_GL_VIEW")
    void searchAccounts_returns200() throws Exception {
        // Given
        AccountDto dto = buildAccountDto();
        PageResponse<AccountDto> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(dto))
        );
        when(accountService.searchAccounts(eq("cash"), any())).thenReturn(pageResponse);

        // When / Then
        mockMvc.perform(get(BASE_URL + "/search")
                        .param("q", "cash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Cash"));
    }
}
