package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateExchangeRateRequest;
import com.hisobnoma.platform.finance.entity.ExchangeRate;
import com.hisobnoma.platform.finance.repository.ExchangeRateRepository;
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
class ExchangeRateControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ExchangeRateRepository exchangeRateRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User adminUser;
    private ExchangeRate usdToUzs;
    private ExchangeRate eurToUzs;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("ExRate Tenant").code("FULLFLOW_EXR").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("exrateadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        usdToUzs = exchangeRateRepository.saveAndFlush(ExchangeRate.builder()
                .fromCurrency("USD").toCurrency("UZS")
                .rate(new BigDecimal("12500.00000000"))
                .effectiveDate(LocalDate.now().minusDays(10))
                .source("BANK").active(true)
                .tenantId(tenant.getId()).build());
        usdToUzs.calculateInverseRate();
        usdToUzs = exchangeRateRepository.saveAndFlush(usdToUzs);

        eurToUzs = exchangeRateRepository.saveAndFlush(ExchangeRate.builder()
                .fromCurrency("EUR").toCurrency("UZS")
                .rate(new BigDecimal("13500.00000000"))
                .effectiveDate(LocalDate.now().minusDays(5))
                .expiryDate(LocalDate.now().plusDays(30))
                .source("ECB").active(true)
                .tenantId(tenant.getId()).build());
        eurToUzs.calculateInverseRate();
        eurToUzs = exchangeRateRepository.saveAndFlush(eurToUzs);
    }

    private RequestPostProcessor viewAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("FINANCE_CURRENCY_VIEW"),
                        new SimpleGrantedAuthority("FINANCE_CURRENCY_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/finance/exchange-rates ----

    @Test
    void getExchangeRates_returnsPaginatedList() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/exchange-rates/all ----

    @Test
    void getAllExchangeRates_returnsAll() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates/all").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/exchange-rates/current ----

    @Test
    void getCurrentActiveRates_returnsCurrentlyEffective() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates/current").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ---- GET /api/v1/finance/exchange-rates/{id} ----

    @Test
    void getExchangeRate_existingId_returnsRate() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates/" + usdToUzs.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.toCurrency").value("UZS"));
    }

    @Test
    void getExchangeRate_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates/99999").with(viewAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/exchange-rates/effective ----

    @Test
    void getEffectiveRate_existingPair_returnsRate() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates/effective")
                        .param("from", "USD").param("to", "UZS")
                        .param("date", LocalDate.now().toString())
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCurrency").value("USD"));
    }

    @Test
    void getEffectiveRate_sameCurrency_returns1to1() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates/effective")
                        .param("from", "USD").param("to", "USD")
                        .param("date", LocalDate.now().toString())
                        .with(viewAuth()))
                .andExpect(status().isOk());
    }

    // ---- GET /api/v1/finance/exchange-rates/pair ----

    @Test
    void getRatesByCurrencyPair_returnsMatching() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates/pair")
                        .param("from", "USD").param("to", "UZS")
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    // ---- GET /api/v1/finance/exchange-rates/by-date ----

    @Test
    void getRatesByDate_returnsMatchingDate() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates/by-date")
                        .param("date", usdToUzs.getEffectiveDate().toString())
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    // ---- GET /api/v1/finance/exchange-rates/convert ----

    @Test
    void convert_validAmount_returnsConvertedValue() throws Exception {
        mockMvc.perform(get("/api/v1/finance/exchange-rates/convert")
                        .param("amount", "100")
                        .param("from", "USD").param("to", "UZS")
                        .param("date", LocalDate.now().toString())
                        .with(viewAuth()))
                .andExpect(status().isOk());
    }

    // ---- POST /api/v1/finance/exchange-rates ----

    @Test
    void createExchangeRate_validRequest_returnsCreated() throws Exception {
        CreateExchangeRateRequest request = CreateExchangeRateRequest.builder()
                .fromCurrency("GBP").toCurrency("UZS")
                .rate(new BigDecimal("15000.00"))
                .effectiveDate(LocalDate.now())
                .source("MANUAL").build();

        mockMvc.perform(post("/api/v1/finance/exchange-rates")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromCurrency").value("GBP"))
                .andExpect(jsonPath("$.toCurrency").value("UZS"))
                .andExpect(jsonPath("$.inverseRate").isNotEmpty());
    }

    @Test
    void createExchangeRate_sameCurrencies_returns400() throws Exception {
        CreateExchangeRateRequest request = CreateExchangeRateRequest.builder()
                .fromCurrency("USD").toCurrency("USD")
                .rate(BigDecimal.ONE)
                .effectiveDate(LocalDate.now()).build();

        mockMvc.perform(post("/api/v1/finance/exchange-rates")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExchangeRate_missingRate_returns400() throws Exception {
        CreateExchangeRateRequest request = CreateExchangeRateRequest.builder()
                .fromCurrency("GBP").toCurrency("UZS")
                .effectiveDate(LocalDate.now()).build();

        mockMvc.perform(post("/api/v1/finance/exchange-rates")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExchangeRate_missingDate_returns400() throws Exception {
        CreateExchangeRateRequest request = CreateExchangeRateRequest.builder()
                .fromCurrency("GBP").toCurrency("UZS")
                .rate(new BigDecimal("15000")).build();

        mockMvc.perform(post("/api/v1/finance/exchange-rates")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/finance/exchange-rates/{id} ----

    @Test
    void updateExchangeRate_validRequest_updatesFields() throws Exception {
        CreateExchangeRateRequest request = CreateExchangeRateRequest.builder()
                .fromCurrency("USD").toCurrency("UZS")
                .rate(new BigDecimal("12800.00"))
                .effectiveDate(LocalDate.now())
                .source("UPDATED").build();

        mockMvc.perform(put("/api/v1/finance/exchange-rates/" + usdToUzs.getId())
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("UPDATED"));
    }

    // ---- PUT /api/v1/finance/exchange-rates/{id}/activate ----

    @Test
    void activateExchangeRate_deactivatedRate_activates() throws Exception {
        ExchangeRate inactive = exchangeRateRepository.saveAndFlush(ExchangeRate.builder()
                .fromCurrency("JPY").toCurrency("UZS")
                .rate(new BigDecimal("85.00000000"))
                .effectiveDate(LocalDate.now())
                .active(false).tenantId(tenant.getId()).build());

        mockMvc.perform(put("/api/v1/finance/exchange-rates/" + inactive.getId() + "/activate")
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---- PUT /api/v1/finance/exchange-rates/{id}/deactivate ----

    @Test
    void deactivateExchangeRate_activeRate_deactivates() throws Exception {
        mockMvc.perform(put("/api/v1/finance/exchange-rates/" + usdToUzs.getId() + "/deactivate")
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ---- DELETE /api/v1/finance/exchange-rates/{id} ----

    @Test
    void deleteExchangeRate_existingRate_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/finance/exchange-rates/" + eurToUzs.getId())
                        .with(viewAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteExchangeRate_nonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/finance/exchange-rates/99999")
                        .with(viewAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- Permission checks ----

    @Test
    void getExchangeRates_noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/finance/exchange-rates")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // ---- Full CRUD lifecycle ----

    @Test
    void fullCrudLifecycle() throws Exception {
        CreateExchangeRateRequest createReq = CreateExchangeRateRequest.builder()
                .fromCurrency("CHF").toCurrency("UZS")
                .rate(new BigDecimal("14000.00"))
                .effectiveDate(LocalDate.now())
                .source("LIFECYCLE").build();

        String result = mockMvc.perform(post("/api/v1/finance/exchange-rates")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(result).get("id").asLong();

        mockMvc.perform(get("/api/v1/finance/exchange-rates/" + createdId).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCurrency").value("CHF"));

        CreateExchangeRateRequest updateReq = CreateExchangeRateRequest.builder()
                .fromCurrency("CHF").toCurrency("UZS")
                .rate(new BigDecimal("14200.00"))
                .effectiveDate(LocalDate.now())
                .source("UPDATED").build();

        mockMvc.perform(put("/api/v1/finance/exchange-rates/" + createdId)
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("UPDATED"));

        mockMvc.perform(put("/api/v1/finance/exchange-rates/" + createdId + "/deactivate")
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/v1/finance/exchange-rates/" + createdId)
                        .with(viewAuth()))
                .andExpect(status().isNoContent());
    }
}
