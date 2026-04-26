package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.finance.dto.CreateCurrencyRequest;
import com.hisobnoma.platform.finance.dto.CurrencyDto;
import com.hisobnoma.platform.finance.service.CurrencyService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CurrencyService currencyService;

    // ==================== GET /api/v1/finance/currencies ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getCurrencies_authenticated_returns200() throws Exception {
        // Given
        CurrencyDto dto = CurrencyDto.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .symbol("$")
                .active(true)
                .build();

        PageResponse<CurrencyDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(currencyService.getCurrencies(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("USD"));
    }

    @Test
    void getCurrencies_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/currencies"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCurrencies_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/currencies"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/currencies/all ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getAllCurrencies_returns200() throws Exception {
        // Given
        CurrencyDto dto = CurrencyDto.builder()
                .id(1L).code("USD").name("US Dollar").build();
        when(currencyService.getAllCurrencies()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/currencies/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("USD"));
    }

    // ==================== GET /api/v1/finance/currencies/active ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getActiveCurrencies_returns200() throws Exception {
        // Given
        CurrencyDto dto = CurrencyDto.builder()
                .id(1L).code("USD").active(true).build();
        when(currencyService.getActiveCurrencies()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/currencies/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    // ==================== GET /api/v1/finance/currencies/base ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getBaseCurrency_returns200() throws Exception {
        // Given
        CurrencyDto dto = CurrencyDto.builder()
                .id(1L).code("UZS").name("Uzbekistani Sum").baseCurrency(true).build();
        when(currencyService.getBaseCurrency()).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/currencies/base"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value(true));
    }

    // ==================== GET /api/v1/finance/currencies/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getCurrency_found_returns200() throws Exception {
        // Given
        CurrencyDto dto = CurrencyDto.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .build();
        when(currencyService.getCurrency(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/currencies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USD"));
    }

    @Test
    void getCurrency_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/currencies/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCurrency_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/currencies/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/currencies/code/{code} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getCurrencyByCode_returns200() throws Exception {
        // Given
        CurrencyDto dto = CurrencyDto.builder()
                .id(1L).code("USD").name("US Dollar").build();
        when(currencyService.getCurrencyByCode("USD")).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/currencies/code/USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USD"));
    }

    // ==================== POST /api/v1/finance/currencies ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_MANAGE")
    void createCurrency_valid_returns201() throws Exception {
        // Given
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("EUR")
                .name("Euro")
                .symbol("€")
                .decimalPlaces(2)
                .build();

        CurrencyDto dto = CurrencyDto.builder()
                .id(2L)
                .code("EUR")
                .name("Euro")
                .symbol("€")
                .active(true)
                .build();
        when(currencyService.createCurrency(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("EUR"));
    }

    @Test
    void createCurrency_unauthenticated_returns403() throws Exception {
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("EUR").name("Euro").build();

        mockMvc.perform(post("/api/v1/finance/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createCurrency_noPermission_returns403() throws Exception {
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("EUR").name("Euro").build();

        mockMvc.perform(post("/api/v1/finance/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/finance/currencies/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_MANAGE")
    void updateCurrency_valid_returns200() throws Exception {
        // Given
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("USD")
                .name("United States Dollar")
                .symbol("$")
                .build();

        CurrencyDto dto = CurrencyDto.builder()
                .id(1L)
                .code("USD")
                .name("United States Dollar")
                .build();
        when(currencyService.updateCurrency(any(), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/currencies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("United States Dollar"));
    }

    // ==================== PUT /api/v1/finance/currencies/{id}/activate ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_MANAGE")
    void activateCurrency_returns200() throws Exception {
        // Given
        CurrencyDto dto = CurrencyDto.builder()
                .id(1L).code("USD").active(true).build();
        when(currencyService.activateCurrency(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/currencies/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ==================== PUT /api/v1/finance/currencies/{id}/deactivate ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_MANAGE")
    void deactivateCurrency_returns200() throws Exception {
        // Given
        CurrencyDto dto = CurrencyDto.builder()
                .id(1L).code("USD").active(false).build();
        when(currencyService.deactivateCurrency(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/currencies/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ==================== PUT /api/v1/finance/currencies/{id}/set-base ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_MANAGE")
    void setBaseCurrency_returns200() throws Exception {
        // Given
        CurrencyDto dto = CurrencyDto.builder()
                .id(1L).code("UZS").baseCurrency(true).build();
        when(currencyService.setBaseCurrency(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/currencies/1/set-base"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value(true));
    }

    @Test
    void setBaseCurrency_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/finance/currencies/1/set-base"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void setBaseCurrency_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/finance/currencies/1/set-base"))
                .andExpect(status().isForbidden());
    }
}
