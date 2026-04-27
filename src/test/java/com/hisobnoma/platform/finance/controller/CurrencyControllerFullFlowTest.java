package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.dto.CreateCurrencyRequest;
import com.hisobnoma.platform.finance.entity.Currency;
import com.hisobnoma.platform.finance.repository.CurrencyRepository;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CurrencyControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User adminUser;
    private Currency usdCurrency;
    private Currency uzsCurrency;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Currency Tenant").code("FULLFLOW_CUR").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("currencyadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        usdCurrency = currencyRepository.saveAndFlush(Currency.builder()
                .code("USD").name("US Dollar").symbol("$").decimalPlaces(2)
                .baseCurrency(false).active(true).sortOrder(1)
                .tenantId(tenant.getId()).build());

        uzsCurrency = currencyRepository.saveAndFlush(Currency.builder()
                .code("UZS").name("Uzbek Sum").symbol("so'm").decimalPlaces(0)
                .baseCurrency(true).active(true).sortOrder(0)
                .tenantId(tenant.getId()).build());
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

    // ---- GET /api/v1/finance/currencies ----

    @Test
    void getCurrencies_returnsPaginatedList() throws Exception {
        mockMvc.perform(get("/api/v1/finance/currencies").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/currencies/all ----

    @Test
    void getAllCurrencies_returnsAllForTenant() throws Exception {
        mockMvc.perform(get("/api/v1/finance/currencies/all").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/finance/currencies/active ----

    @Test
    void getActiveCurrencies_returnsOnlyActive() throws Exception {
        currencyRepository.saveAndFlush(Currency.builder()
                .code("EUR").name("Euro").symbol("E").decimalPlaces(2)
                .active(false).tenantId(tenant.getId()).build());

        mockMvc.perform(get("/api/v1/finance/currencies/active").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- GET /api/v1/finance/currencies/base ----

    @Test
    void getBaseCurrency_returnsBaseCurrency() throws Exception {
        mockMvc.perform(get("/api/v1/finance/currencies/base").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("UZS"))
                .andExpect(jsonPath("$.baseCurrency").value(true));
    }

    // ---- GET /api/v1/finance/currencies/{id} ----

    @Test
    void getCurrency_existingId_returnsCurrency() throws Exception {
        mockMvc.perform(get("/api/v1/finance/currencies/" + usdCurrency.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USD"))
                .andExpect(jsonPath("$.name").value("US Dollar"));
    }

    @Test
    void getCurrency_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/currencies/99999").with(viewAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/finance/currencies/code/{code} ----

    @Test
    void getCurrencyByCode_existingCode_returnsCurrency() throws Exception {
        mockMvc.perform(get("/api/v1/finance/currencies/code/USD").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("US Dollar"));
    }

    @Test
    void getCurrencyByCode_nonExistentCode_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/finance/currencies/code/XYZ").with(viewAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/finance/currencies ----

    @Test
    void createCurrency_validRequest_returnsCreated() throws Exception {
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("EUR").name("Euro").symbol("E").decimalPlaces(2).build();

        mockMvc.perform(post("/api/v1/finance/currencies")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("EUR"))
                .andExpect(jsonPath("$.name").value("Euro"));
    }

    @Test
    void createCurrency_duplicateCode_returns400() throws Exception {
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("USD").name("Another USD").build();

        mockMvc.perform(post("/api/v1/finance/currencies")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCurrency_missingCode_returns400() throws Exception {
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .name("No Code Currency").build();

        mockMvc.perform(post("/api/v1/finance/currencies")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCurrency_invalidCodeLength_returns400() throws Exception {
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("AB").name("Short Code").build();

        mockMvc.perform(post("/api/v1/finance/currencies")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCurrency_asBaseCurrency_unsetsExistingBase() throws Exception {
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("GBP").name("British Pound").symbol("L").decimalPlaces(2)
                .baseCurrency(true).build();

        mockMvc.perform(post("/api/v1/finance/currencies")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baseCurrency").value(true));

        mockMvc.perform(get("/api/v1/finance/currencies/base").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("GBP"));
    }

    // ---- PUT /api/v1/finance/currencies/{id} ----

    @Test
    void updateCurrency_validRequest_updatesFields() throws Exception {
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("USD").name("Updated US Dollar").symbol("$").decimalPlaces(2).build();

        mockMvc.perform(put("/api/v1/finance/currencies/" + usdCurrency.getId())
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated US Dollar"));
    }

    @Test
    void updateCurrency_nonExistentId_returns404() throws Exception {
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("USD").name("US Dollar").build();

        mockMvc.perform(put("/api/v1/finance/currencies/99999")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /api/v1/finance/currencies/{id}/activate ----

    @Test
    void activateCurrency_deactivatedCurrency_activates() throws Exception {
        Currency inactive = currencyRepository.saveAndFlush(Currency.builder()
                .code("JPY").name("Japanese Yen").decimalPlaces(0)
                .active(false).tenantId(tenant.getId()).build());

        mockMvc.perform(put("/api/v1/finance/currencies/" + inactive.getId() + "/activate")
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---- PUT /api/v1/finance/currencies/{id}/deactivate ----

    @Test
    void deactivateCurrency_nonBaseCurrency_deactivates() throws Exception {
        mockMvc.perform(put("/api/v1/finance/currencies/" + usdCurrency.getId() + "/deactivate")
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deactivateCurrency_baseCurrency_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/finance/currencies/" + uzsCurrency.getId() + "/deactivate")
                        .with(viewAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/finance/currencies/{id}/set-base ----

    @Test
    void setBaseCurrency_switchesBase() throws Exception {
        mockMvc.perform(put("/api/v1/finance/currencies/" + usdCurrency.getId() + "/set-base")
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value(true));

        mockMvc.perform(get("/api/v1/finance/currencies/" + uzsCurrency.getId()).with(viewAuth()))
                .andExpect(jsonPath("$.baseCurrency").value(false));
    }

    // ---- Permission checks ----

    @Test
    void getCurrencies_noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/finance/currencies")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCurrency_viewOnlyPermission_returns403() throws Exception {
        UserPrincipal viewOnly = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("FINANCE_CURRENCY_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                viewOnly, null, viewOnly.getAuthorities());

        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("GBP").name("British Pound").build();

        mockMvc.perform(post("/api/v1/finance/currencies")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---- Full CRUD lifecycle ----

    @Test
    void fullCrudLifecycle() throws Exception {
        CreateCurrencyRequest createReq = CreateCurrencyRequest.builder()
                .code("RUB").name("Russian Ruble").symbol("R").decimalPlaces(2)
                .sortOrder(5).build();

        String createResult = mockMvc.perform(post("/api/v1/finance/currencies")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(createResult).get("id").asLong();

        mockMvc.perform(get("/api/v1/finance/currencies/code/RUB").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Russian Ruble"));

        CreateCurrencyRequest updateReq = CreateCurrencyRequest.builder()
                .code("RUB").name("Updated Ruble").symbol("R").decimalPlaces(2).build();

        mockMvc.perform(put("/api/v1/finance/currencies/" + createdId)
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Ruble"));

        mockMvc.perform(put("/api/v1/finance/currencies/" + createdId + "/deactivate")
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(put("/api/v1/finance/currencies/" + createdId + "/activate")
                        .with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }
}
