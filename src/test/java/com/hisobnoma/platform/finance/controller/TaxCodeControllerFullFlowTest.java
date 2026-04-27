package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateTaxCodeRequest;
import com.hisobnoma.platform.finance.dto.CreateTaxRateRequest;
import com.hisobnoma.platform.finance.dto.TaxCalculationRequest;
import com.hisobnoma.platform.finance.entity.TaxCode;
import com.hisobnoma.platform.finance.entity.TaxRate;
import com.hisobnoma.platform.finance.entity.TaxType;
import com.hisobnoma.platform.finance.repository.TaxCodeRepository;
import com.hisobnoma.platform.finance.repository.TaxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaxCodeControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TaxCodeRepository taxCodeRepository;
    @Autowired private TaxRateRepository taxRateRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User adminUser;
    private TaxCode vatCode;
    private TaxCode gstCode;
    private TaxRate vatRate;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Tax Tenant").code("FULLFLOW_TAX").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("taxadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        vatCode = taxCodeRepository.saveAndFlush(TaxCode.builder()
                .code("VAT20").name("VAT 20%").description("Standard VAT rate")
                .taxType(TaxType.VAT).inclusive(false).compound(false)
                .appliesToSales(true).appliesToPurchases(true)
                .taxAuthority("Tax Authority").reportCode("BOX1")
                .active(true).systemCode(false).sortOrder(1)
                .tenantId(tenant.getId()).build());

        gstCode = taxCodeRepository.saveAndFlush(TaxCode.builder()
                .code("GST10").name("GST 10%").description("Goods and Services Tax")
                .taxType(TaxType.GST).inclusive(false).compound(false)
                .appliesToSales(true).appliesToPurchases(false)
                .active(true).systemCode(false).sortOrder(2)
                .tenantId(tenant.getId()).build());

        vatRate = taxRateRepository.saveAndFlush(TaxRate.builder()
                .taxCode(vatCode).name("Standard Rate")
                .rate(new BigDecimal("20.0000"))
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .active(true)
                .tenantId(tenant.getId()).build());
    }

    private RequestPostProcessor manageAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_TAX_VIEW"),
                        new SimpleGrantedAuthority("FINANCE_TAX_MANAGE"),
                        new SimpleGrantedAuthority("FINANCE_TAX_REPORTS")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor viewAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_TAX_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/finance/tax-codes ----

    @Test
    void getTaxCodes_returnsPaginatedList() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes").with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/tax-codes/all ----

    @Test
    void getAllTaxCodes_returnsAllForTenant() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/all").with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/tax-codes/active ----

    @Test
    void getActiveTaxCodes_returnsOnlyActive() throws Exception {
        taxCodeRepository.saveAndFlush(TaxCode.builder()
                .code("INACTIVE1").name("Inactive Tax").taxType(TaxType.OTHER)
                .active(false).sortOrder(99).tenantId(tenant.getId()).build());

        mockMvc.perform(get("/api/v1/finance/tax-codes/active").with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- GET /api/v1/finance/tax-codes/by-type/{type} ----

    @Test
    void getTaxCodesByType_returnsMatchingType() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/by-type/VAT").with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("VAT20"));
    }

    // ---- GET /api/v1/finance/tax-codes/sales ----

    @Test
    void getSalesTaxCodes_returnsCodesApplyingToSales() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/sales").with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/tax-codes/purchases ----

    @Test
    void getPurchaseTaxCodes_returnsCodesApplyingToPurchases() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/purchases").with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].code").value("VAT20"));
    }

    // ---- GET /api/v1/finance/tax-codes/{id} ----

    @Test
    void getTaxCode_existingId_returnsTaxCodeWithRates() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/" + vatCode.getId()).with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("VAT20"))
                .andExpect(jsonPath("$.name").value("VAT 20%"))
                .andExpect(jsonPath("$.taxType").value("VAT"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getTaxCode_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/99999").with(manageAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/tax-codes/code/{code} ----

    @Test
    void getTaxCodeByCode_existingCode_returnsTaxCode() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/code/VAT20").with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("VAT 20%"))
                .andExpect(jsonPath("$.taxType").value("VAT"));
    }

    @Test
    void getTaxCodeByCode_nonExistentCode_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/code/NONEXIST").with(manageAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/tax-codes/search ----

    @Test
    void searchTaxCodes_matchingQuery_returnsResults() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/search")
                        .param("query", "VAT")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].code").value("VAT20"));
    }

    // ---- POST /api/v1/finance/tax-codes ----

    @Test
    void createTaxCode_validRequest_returnsCreated() throws Exception {
        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .code("EXCISE5").name("Excise Tax 5%").description("Excise duty")
                .taxType(TaxType.EXCISE_TAX).inclusive(false).compound(false)
                .appliesToSales(true).appliesToPurchases(false)
                .taxAuthority("Excise Board").reportCode("EX1")
                .sortOrder(5).build();

        mockMvc.perform(post("/api/v1/finance/tax-codes")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("EXCISE5"))
                .andExpect(jsonPath("$.name").value("Excise Tax 5%"))
                .andExpect(jsonPath("$.taxType").value("EXCISE_TAX"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createTaxCode_duplicateCode_returns400() throws Exception {
        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .code("VAT20").name("Duplicate VAT").taxType(TaxType.VAT).build();

        mockMvc.perform(post("/api/v1/finance/tax-codes")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTaxCode_missingRequiredFields_returns400() throws Exception {
        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .name("No Code Tax").build();

        mockMvc.perform(post("/api/v1/finance/tax-codes")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/finance/tax-codes/{id} ----

    @Test
    void updateTaxCode_validRequest_updatesFields() throws Exception {
        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .code("VAT20").name("Updated VAT 20%").description("Updated description")
                .taxType(TaxType.VAT).inclusive(true).compound(false)
                .appliesToSales(true).appliesToPurchases(true)
                .sortOrder(1).build();

        mockMvc.perform(put("/api/v1/finance/tax-codes/" + vatCode.getId())
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated VAT 20%"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.inclusive").value(true));
    }

    @Test
    void updateTaxCode_nonExistentId_returns404() throws Exception {
        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .code("VAT20").name("VAT").taxType(TaxType.VAT).build();

        mockMvc.perform(put("/api/v1/finance/tax-codes/99999")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /api/v1/finance/tax-codes/{id}/activate ----

    @Test
    void activateTaxCode_deactivatedCode_activates() throws Exception {
        TaxCode inactive = taxCodeRepository.saveAndFlush(TaxCode.builder()
                .code("INACT").name("Inactive Tax").taxType(TaxType.OTHER)
                .active(false).sortOrder(10).tenantId(tenant.getId()).build());

        mockMvc.perform(put("/api/v1/finance/tax-codes/" + inactive.getId() + "/activate")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---- PUT /api/v1/finance/tax-codes/{id}/deactivate ----

    @Test
    void deactivateTaxCode_activeCode_deactivates() throws Exception {
        mockMvc.perform(put("/api/v1/finance/tax-codes/" + vatCode.getId() + "/deactivate")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ---- GET /api/v1/finance/tax-codes/{taxCodeId}/rates ----

    @Test
    void getTaxRates_existingTaxCode_returnsRates() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/" + vatCode.getId() + "/rates")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Standard Rate"))
                .andExpect(jsonPath("$[0].rate").value(closeTo(20.0, 0.01)));
    }

    // ---- POST /api/v1/finance/tax-codes/rates ----

    @Test
    void addTaxRate_validRequest_returnsCreated() throws Exception {
        CreateTaxRateRequest request = CreateTaxRateRequest.builder()
                .taxCodeId(gstCode.getId())
                .name("GST Standard Rate")
                .rate(new BigDecimal("10.0000"))
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .notes("Standard GST rate")
                .build();

        mockMvc.perform(post("/api/v1/finance/tax-codes/rates")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("GST Standard Rate"))
                .andExpect(jsonPath("$.rate").value(closeTo(10.0, 0.01)))
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---- PUT /api/v1/finance/tax-codes/rates/{rateId} ----

    @Test
    void updateTaxRate_validRequest_updatesRate() throws Exception {
        CreateTaxRateRequest request = CreateTaxRateRequest.builder()
                .taxCodeId(vatCode.getId())
                .name("Updated Standard Rate")
                .rate(new BigDecimal("25.0000"))
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .effectiveTo(LocalDate.of(2025, 12, 31))
                .build();

        mockMvc.perform(put("/api/v1/finance/tax-codes/rates/" + vatRate.getId())
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Standard Rate"))
                .andExpect(jsonPath("$.rate").value(closeTo(25.0, 0.01)));
    }

    // ---- DELETE /api/v1/finance/tax-codes/rates/{rateId} ----

    @Test
    void deleteTaxRate_existingRate_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/finance/tax-codes/rates/" + vatRate.getId())
                        .with(manageAuth()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/finance/tax-codes/" + vatCode.getId() + "/rates")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- POST /api/v1/finance/tax-codes/calculate ----

    @Test
    void calculateTax_exclusiveTax_returnsCorrectAmounts() throws Exception {
        TaxCalculationRequest request = TaxCalculationRequest.builder()
                .taxCode("VAT20")
                .amount(new BigDecimal("1000.00"))
                .transactionDate(LocalDate.of(2025, 6, 15))
                .inclusive(false)
                .build();

        mockMvc.perform(post("/api/v1/finance/tax-codes/calculate")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxCode").value("VAT20"))
                .andExpect(jsonPath("$.baseAmount").value(closeTo(1000.0, 0.01)))
                .andExpect(jsonPath("$.taxAmount").value(closeTo(200.0, 0.01)))
                .andExpect(jsonPath("$.totalAmount").value(closeTo(1200.0, 0.01)))
                .andExpect(jsonPath("$.inclusive").value(false));
    }

    @Test
    void calculateTax_inclusiveTax_extractsTaxFromAmount() throws Exception {
        TaxCalculationRequest request = TaxCalculationRequest.builder()
                .taxCode("VAT20")
                .amount(new BigDecimal("1200.00"))
                .transactionDate(LocalDate.of(2025, 6, 15))
                .inclusive(true)
                .build();

        mockMvc.perform(post("/api/v1/finance/tax-codes/calculate")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxCode").value("VAT20"))
                .andExpect(jsonPath("$.taxAmount").value(closeTo(200.0, 0.01)))
                .andExpect(jsonPath("$.totalAmount").value(closeTo(1200.0, 0.01)))
                .andExpect(jsonPath("$.inclusive").value(true));
    }

    // ---- GET /api/v1/finance/tax-codes/reports/vat-summary ----

    @Test
    void getVatSummaryReport_validPeriod_returnsReport() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/reports/vat-summary")
                        .param("periodStart", "2025-01-01")
                        .param("periodEnd", "2025-12-31")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodStart").value("2025-01-01"))
                .andExpect(jsonPath("$.periodEnd").value("2025-12-31"))
                .andExpect(jsonPath("$.taxType").value("VAT"));
    }

    // ---- Permission checks ----

    @Test
    void getTaxCodes_noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/finance/tax-codes")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTaxCode_viewOnlyPermission_returns403() throws Exception {
        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .code("NEW1").name("New Tax").taxType(TaxType.OTHER).build();

        mockMvc.perform(post("/api/v1/finance/tax-codes")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getVatReport_viewOnlyWithoutReportsPerm_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/tax-codes/reports/vat-summary")
                        .param("periodStart", "2025-01-01")
                        .param("periodEnd", "2025-12-31")
                        .with(viewAuth()))
                .andExpect(status().isForbidden());
    }

    // ---- Full lifecycle ----

    @Test
    void fullTaxCodeLifecycle_createUpdateRatesDeactivateActivate() throws Exception {
        // 1. Create a new tax code
        CreateTaxCodeRequest createReq = CreateTaxCodeRequest.builder()
                .code("WITHTAX").name("Withholding Tax 15%").description("Withholding tax")
                .taxType(TaxType.WITHHOLDING_TAX).inclusive(false).compound(false)
                .appliesToSales(false).appliesToPurchases(true)
                .taxAuthority("Revenue Service").sortOrder(10).build();

        String createResult = mockMvc.perform(post("/api/v1/finance/tax-codes")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("WITHTAX"))
                .andReturn().getResponse().getContentAsString();

        Long taxCodeId = objectMapper.readTree(createResult).get("id").asLong();

        // 2. Verify retrieval by code
        mockMvc.perform(get("/api/v1/finance/tax-codes/code/WITHTAX").with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Withholding Tax 15%"));

        // 3. Add a tax rate
        CreateTaxRateRequest rateReq = CreateTaxRateRequest.builder()
                .taxCodeId(taxCodeId)
                .name("WH Standard Rate")
                .rate(new BigDecimal("15.0000"))
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .build();

        String rateResult = mockMvc.perform(post("/api/v1/finance/tax-codes/rates")
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rateReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("WH Standard Rate"))
                .andReturn().getResponse().getContentAsString();

        Long rateId = objectMapper.readTree(rateResult).get("id").asLong();

        // 4. Verify rates are listed
        mockMvc.perform(get("/api/v1/finance/tax-codes/" + taxCodeId + "/rates")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 5. Update the tax code
        CreateTaxCodeRequest updateReq = CreateTaxCodeRequest.builder()
                .code("WITHTAX").name("Updated Withholding Tax")
                .description("Updated description")
                .taxType(TaxType.WITHHOLDING_TAX).inclusive(false).compound(false)
                .appliesToSales(false).appliesToPurchases(true)
                .sortOrder(10).build();

        mockMvc.perform(put("/api/v1/finance/tax-codes/" + taxCodeId)
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Withholding Tax"));

        // 6. Update the rate
        CreateTaxRateRequest updateRateReq = CreateTaxRateRequest.builder()
                .taxCodeId(taxCodeId)
                .name("WH Revised Rate")
                .rate(new BigDecimal("18.0000"))
                .effectiveFrom(LocalDate.of(2025, 7, 1))
                .build();

        mockMvc.perform(put("/api/v1/finance/tax-codes/rates/" + rateId)
                        .with(manageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(closeTo(18.0, 0.01)));

        // 7. Deactivate
        mockMvc.perform(put("/api/v1/finance/tax-codes/" + taxCodeId + "/deactivate")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // 8. Reactivate
        mockMvc.perform(put("/api/v1/finance/tax-codes/" + taxCodeId + "/activate")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        // 9. Delete the rate
        mockMvc.perform(delete("/api/v1/finance/tax-codes/rates/" + rateId)
                        .with(manageAuth()))
                .andExpect(status().isNoContent());

        // 10. Verify rate is gone
        mockMvc.perform(get("/api/v1/finance/tax-codes/" + taxCodeId + "/rates")
                        .with(manageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
