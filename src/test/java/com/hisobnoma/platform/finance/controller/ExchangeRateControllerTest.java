package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.finance.dto.CreateExchangeRateRequest;
import com.hisobnoma.platform.finance.dto.ExchangeRateDto;
import com.hisobnoma.platform.finance.service.ExchangeRateService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExchangeRateService exchangeRateService;

    // ==================== GET /api/v1/finance/exchange-rates ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getExchangeRates_authenticated_returns200() throws Exception {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .id(1L)
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12800.00"))
                .active(true)
                .build();

        PageResponse<ExchangeRateDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(exchangeRateService.getExchangeRates(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fromCurrency").value("USD"));
    }

    @Test
    void getExchangeRates_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getExchangeRates_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/exchange-rates/all ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getAllExchangeRates_returns200() throws Exception {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .id(1L).fromCurrency("USD").toCurrency("UZS").build();
        when(exchangeRateService.getAllExchangeRates()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/exchange-rates/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromCurrency").value("USD"));
    }

    // ==================== GET /api/v1/finance/exchange-rates/current ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getCurrentActiveRates_returns200() throws Exception {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .id(1L).fromCurrency("USD").toCurrency("UZS").active(true).build();
        when(exchangeRateService.getCurrentActiveRates()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/exchange-rates/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    // ==================== GET /api/v1/finance/exchange-rates/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getExchangeRate_found_returns200() throws Exception {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .id(1L)
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12800.00"))
                .build();
        when(exchangeRateService.getExchangeRate(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/exchange-rates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCurrency").value("USD"));
    }

    @Test
    void getExchangeRate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getExchangeRate_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/exchange-rates/effective ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getEffectiveRate_returns200() throws Exception {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .id(1L).fromCurrency("USD").toCurrency("UZS")
                .rate(new BigDecimal("12800.00")).build();
        when(exchangeRateService.getEffectiveRate(eq("USD"), eq("UZS"), any(LocalDate.class))).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/exchange-rates/effective")
                        .param("from", "USD")
                        .param("to", "UZS")
                        .param("date", "2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCurrency").value("USD"));
    }

    // ==================== GET /api/v1/finance/exchange-rates/pair ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getRatesByCurrencyPair_returns200() throws Exception {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .id(1L).fromCurrency("USD").toCurrency("UZS").build();
        when(exchangeRateService.getRatesByCurrencyPair("USD", "UZS")).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/exchange-rates/pair")
                        .param("from", "USD")
                        .param("to", "UZS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromCurrency").value("USD"));
    }

    // ==================== GET /api/v1/finance/exchange-rates/by-date ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void getRatesByDate_returns200() throws Exception {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .id(1L).fromCurrency("USD").toCurrency("UZS").build();
        when(exchangeRateService.getRatesByDate(any(LocalDate.class))).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/exchange-rates/by-date")
                        .param("date", "2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromCurrency").value("USD"));
    }

    // ==================== GET /api/v1/finance/exchange-rates/convert ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_VIEW")
    void convert_returns200() throws Exception {
        // Given
        when(exchangeRateService.convert(any(), eq("USD"), eq("UZS"), any(LocalDate.class)))
                .thenReturn(new BigDecimal("12800000.00"));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/exchange-rates/convert")
                        .param("amount", "1000")
                        .param("from", "USD")
                        .param("to", "UZS")
                        .param("date", "2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(12800000.00));
    }

    // ==================== POST /api/v1/finance/exchange-rates ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_MANAGE")
    void createExchangeRate_valid_returns201() throws Exception {
        // Given
        CreateExchangeRateRequest request = CreateExchangeRateRequest.builder()
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12800.00"))
                .effectiveDate(LocalDate.of(2026, 1, 15))
                .source("Central Bank")
                .build();

        ExchangeRateDto dto = ExchangeRateDto.builder()
                .id(1L)
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12800.00"))
                .active(true)
                .build();
        when(exchangeRateService.createExchangeRate(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromCurrency").value("USD"));
    }

    @Test
    void createExchangeRate_unauthenticated_returns403() throws Exception {
        CreateExchangeRateRequest request = CreateExchangeRateRequest.builder()
                .fromCurrency("USD").toCurrency("UZS")
                .rate(new BigDecimal("12800.00"))
                .effectiveDate(LocalDate.of(2026, 1, 15)).build();

        mockMvc.perform(post("/api/v1/finance/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createExchangeRate_noPermission_returns403() throws Exception {
        CreateExchangeRateRequest request = CreateExchangeRateRequest.builder()
                .fromCurrency("USD").toCurrency("UZS")
                .rate(new BigDecimal("12800.00"))
                .effectiveDate(LocalDate.of(2026, 1, 15)).build();

        mockMvc.perform(post("/api/v1/finance/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/finance/exchange-rates/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_MANAGE")
    void updateExchangeRate_valid_returns200() throws Exception {
        // Given
        CreateExchangeRateRequest request = CreateExchangeRateRequest.builder()
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12900.00"))
                .effectiveDate(LocalDate.of(2026, 1, 16))
                .build();

        ExchangeRateDto dto = ExchangeRateDto.builder()
                .id(1L)
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12900.00"))
                .build();
        when(exchangeRateService.updateExchangeRate(any(), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/exchange-rates/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(12900.00));
    }

    // ==================== PUT /api/v1/finance/exchange-rates/{id}/activate ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_MANAGE")
    void activateExchangeRate_returns200() throws Exception {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .id(1L).fromCurrency("USD").toCurrency("UZS").active(true).build();
        when(exchangeRateService.activateExchangeRate(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/exchange-rates/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ==================== PUT /api/v1/finance/exchange-rates/{id}/deactivate ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_MANAGE")
    void deactivateExchangeRate_returns200() throws Exception {
        // Given
        ExchangeRateDto dto = ExchangeRateDto.builder()
                .id(1L).fromCurrency("USD").toCurrency("UZS").active(false).build();
        when(exchangeRateService.deactivateExchangeRate(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/exchange-rates/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ==================== DELETE /api/v1/finance/exchange-rates/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_CURRENCY_MANAGE")
    void deleteExchangeRate_returns204() throws Exception {
        // Given
        doNothing().when(exchangeRateService).deleteExchangeRate(1L);

        // When/Then
        mockMvc.perform(delete("/api/v1/finance/exchange-rates/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteExchangeRate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/finance/exchange-rates/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteExchangeRate_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/finance/exchange-rates/1"))
                .andExpect(status().isForbidden());
    }
}
