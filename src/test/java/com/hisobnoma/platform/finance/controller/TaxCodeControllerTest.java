package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.finance.dto.*;
import com.hisobnoma.platform.finance.entity.TaxType;
import com.hisobnoma.platform.finance.service.TaxCalculationService;
import com.hisobnoma.platform.finance.service.TaxCodeService;
import com.hisobnoma.platform.finance.service.TaxReportService;
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
class TaxCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaxCodeService taxCodeService;

    @MockBean
    private TaxCalculationService taxCalculationService;

    @MockBean
    private TaxReportService taxReportService;

    // ==================== GET /api/v1/finance/tax-codes ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_VIEW")
    void getTaxCodes_authenticated_returns200() throws Exception {
        // Given
        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L)
                .code("VAT-20")
                .name("VAT 20%")
                .taxType(TaxType.VAT)
                .active(true)
                .build();

        PageResponse<TaxCodeDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(taxCodeService.getTaxCodes(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("VAT-20"));
    }

    @Test
    void getTaxCodes_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getTaxCodes_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/tax-codes/all ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_VIEW")
    void getAllTaxCodes_returns200() throws Exception {
        // Given
        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L).code("VAT-20").name("VAT 20%").build();
        when(taxCodeService.getAllTaxCodes()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("VAT-20"));
    }

    // ==================== GET /api/v1/finance/tax-codes/active ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_VIEW")
    void getActiveTaxCodes_returns200() throws Exception {
        // Given
        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L).code("VAT-20").active(true).build();
        when(taxCodeService.getActiveTaxCodes()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    // ==================== GET /api/v1/finance/tax-codes/by-type/{type} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_VIEW")
    void getTaxCodesByType_returns200() throws Exception {
        // Given
        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L).code("VAT-20").taxType(TaxType.VAT).build();
        when(taxCodeService.getTaxCodesByType(TaxType.VAT)).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes/by-type/VAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taxType").value("VAT"));
    }

    // ==================== GET /api/v1/finance/tax-codes/sales ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_VIEW")
    void getSalesTaxCodes_returns200() throws Exception {
        // Given
        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L).code("VAT-20").appliesToSales(true).build();
        when(taxCodeService.getSalesTaxCodes()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].appliesToSales").value(true));
    }

    // ==================== GET /api/v1/finance/tax-codes/purchases ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_VIEW")
    void getPurchaseTaxCodes_returns200() throws Exception {
        // Given
        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L).code("VAT-20").appliesToPurchases(true).build();
        when(taxCodeService.getPurchaseTaxCodes()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes/purchases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].appliesToPurchases").value(true));
    }

    // ==================== GET /api/v1/finance/tax-codes/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_VIEW")
    void getTaxCode_found_returns200() throws Exception {
        // Given
        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L)
                .code("VAT-20")
                .name("VAT 20%")
                .taxType(TaxType.VAT)
                .build();
        when(taxCodeService.getTaxCode(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("VAT-20"));
    }

    @Test
    void getTaxCode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getTaxCode_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/tax-codes/code/{code} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_VIEW")
    void getTaxCodeByCode_returns200() throws Exception {
        // Given
        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L).code("VAT-20").name("VAT 20%").build();
        when(taxCodeService.getTaxCodeByCode("VAT-20")).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes/code/VAT-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("VAT-20"));
    }

    // ==================== GET /api/v1/finance/tax-codes/search ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_VIEW")
    void searchTaxCodes_returns200() throws Exception {
        // Given
        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L).code("VAT-20").name("VAT 20%").build();

        PageResponse<TaxCodeDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(taxCodeService.searchTaxCodes(eq("VAT"), any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes/search")
                        .param("query", "VAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("VAT-20"));
    }

    // ==================== POST /api/v1/finance/tax-codes ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_MANAGE")
    void createTaxCode_valid_returns201() throws Exception {
        // Given
        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .code("GST-10")
                .name("GST 10%")
                .taxType(TaxType.GST)
                .appliesToSales(true)
                .appliesToPurchases(true)
                .build();

        TaxCodeDto dto = TaxCodeDto.builder()
                .id(2L)
                .code("GST-10")
                .name("GST 10%")
                .taxType(TaxType.GST)
                .active(true)
                .build();
        when(taxCodeService.createTaxCode(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/tax-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("GST-10"));
    }

    @Test
    void createTaxCode_unauthenticated_returns403() throws Exception {
        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .code("GST-10").name("GST 10%").taxType(TaxType.GST).build();

        mockMvc.perform(post("/api/v1/finance/tax-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createTaxCode_noPermission_returns403() throws Exception {
        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .code("GST-10").name("GST 10%").taxType(TaxType.GST).build();

        mockMvc.perform(post("/api/v1/finance/tax-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/finance/tax-codes/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_MANAGE")
    void updateTaxCode_valid_returns200() throws Exception {
        // Given
        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .code("VAT-20")
                .name("VAT 20% Updated")
                .taxType(TaxType.VAT)
                .build();

        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L)
                .code("VAT-20")
                .name("VAT 20% Updated")
                .build();
        when(taxCodeService.updateTaxCode(any(), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/tax-codes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("VAT 20% Updated"));
    }

    // ==================== PUT /api/v1/finance/tax-codes/{id}/activate ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_MANAGE")
    void activateTaxCode_returns200() throws Exception {
        // Given
        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L).code("VAT-20").active(true).build();
        when(taxCodeService.activateTaxCode(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/tax-codes/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ==================== PUT /api/v1/finance/tax-codes/{id}/deactivate ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_MANAGE")
    void deactivateTaxCode_returns200() throws Exception {
        // Given
        TaxCodeDto dto = TaxCodeDto.builder()
                .id(1L).code("VAT-20").active(false).build();
        when(taxCodeService.deactivateTaxCode(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/tax-codes/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ==================== GET /api/v1/finance/tax-codes/{taxCodeId}/rates ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_VIEW")
    void getTaxRates_returns200() throws Exception {
        // Given
        TaxRateDto dto = TaxRateDto.builder()
                .id(1L).taxCodeId(1L).name("Standard Rate")
                .rate(new BigDecimal("20.00")).active(true).build();
        when(taxCodeService.getTaxRates(1L)).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes/1/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Standard Rate"));
    }

    // ==================== POST /api/v1/finance/tax-codes/rates ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_MANAGE")
    void addTaxRate_valid_returns201() throws Exception {
        // Given
        CreateTaxRateRequest request = CreateTaxRateRequest.builder()
                .taxCodeId(1L)
                .name("Reduced Rate")
                .rate(new BigDecimal("10.00"))
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();

        TaxRateDto dto = TaxRateDto.builder()
                .id(2L).taxCodeId(1L).name("Reduced Rate")
                .rate(new BigDecimal("10.00")).active(true).build();
        when(taxCodeService.addTaxRate(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/tax-codes/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Reduced Rate"));
    }

    // ==================== PUT /api/v1/finance/tax-codes/rates/{rateId} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_MANAGE")
    void updateTaxRate_valid_returns200() throws Exception {
        // Given
        CreateTaxRateRequest request = CreateTaxRateRequest.builder()
                .taxCodeId(1L)
                .name("Updated Rate")
                .rate(new BigDecimal("15.00"))
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();

        TaxRateDto dto = TaxRateDto.builder()
                .id(1L).taxCodeId(1L).name("Updated Rate")
                .rate(new BigDecimal("15.00")).build();
        when(taxCodeService.updateTaxRate(any(), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/tax-codes/rates/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Rate"));
    }

    // ==================== DELETE /api/v1/finance/tax-codes/rates/{rateId} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_MANAGE")
    void deleteTaxRate_returns204() throws Exception {
        // Given
        doNothing().when(taxCodeService).deleteTaxRate(1L);

        // When/Then
        mockMvc.perform(delete("/api/v1/finance/tax-codes/rates/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTaxRate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/finance/tax-codes/rates/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteTaxRate_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/finance/tax-codes/rates/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/finance/tax-codes/calculate ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_VIEW")
    void calculateTax_returns200() throws Exception {
        // Given
        TaxCalculationRequest request = TaxCalculationRequest.builder()
                .taxCode("VAT-20")
                .amount(new BigDecimal("1000.00"))
                .transactionDate(LocalDate.of(2026, 1, 15))
                .build();

        TaxCalculationResult result = TaxCalculationResult.builder()
                .taxCode("VAT-20")
                .taxName("VAT 20%")
                .rate(new BigDecimal("20.00"))
                .baseAmount(new BigDecimal("1000.00"))
                .taxAmount(new BigDecimal("200.00"))
                .totalAmount(new BigDecimal("1200.00"))
                .inclusive(false)
                .build();
        when(taxCalculationService.calculateTax(any())).thenReturn(result);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/tax-codes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxAmount").value(200.00));
    }

    @Test
    void calculateTax_unauthenticated_returns403() throws Exception {
        TaxCalculationRequest request = TaxCalculationRequest.builder()
                .taxCode("VAT-20").amount(new BigDecimal("1000.00")).build();

        mockMvc.perform(post("/api/v1/finance/tax-codes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/tax-codes/reports/vat-summary ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_REPORTS")
    void getVatSummaryReport_returns200() throws Exception {
        // Given
        TaxReportDto report = TaxReportDto.builder()
                .periodStart(LocalDate.of(2026, 1, 1))
                .periodEnd(LocalDate.of(2026, 3, 31))
                .totalSalesTax(new BigDecimal("5000.00"))
                .totalPurchaseTax(new BigDecimal("3000.00"))
                .netTaxPayable(new BigDecimal("2000.00"))
                .build();
        when(taxReportService.generateVatSummaryReport(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes/reports/vat-summary")
                        .param("periodStart", "2026-01-01")
                        .param("periodEnd", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netTaxPayable").value(2000.00));
    }

    @Test
    void getVatSummaryReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/reports/vat-summary")
                        .param("periodStart", "2026-01-01")
                        .param("periodEnd", "2026-03-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getVatSummaryReport_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/reports/vat-summary")
                        .param("periodStart", "2026-01-01")
                        .param("periodEnd", "2026-03-31"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/tax-codes/reports/summary-by-type ====================

    @Test
    @WithMockUser(authorities = "FINANCE_TAX_REPORTS")
    void getTaxSummaryByType_returns200() throws Exception {
        // Given
        TaxReportDto report = TaxReportDto.builder()
                .taxType("VAT")
                .totalSalesTax(new BigDecimal("5000.00"))
                .build();
        when(taxReportService.generateTaxSummaryByType(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(report));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/tax-codes/reports/summary-by-type")
                        .param("periodStart", "2026-01-01")
                        .param("periodEnd", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taxType").value("VAT"));
    }
}
