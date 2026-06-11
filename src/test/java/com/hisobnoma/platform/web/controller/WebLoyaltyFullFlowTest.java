package com.hisobnoma.platform.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.admin.entity.TenantSetting;
import com.hisobnoma.platform.admin.entity.SystemSetting;
import com.hisobnoma.platform.admin.repository.TenantSettingRepository;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.web.dto.LoyaltyAdjustRequest;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebLoyaltyTransaction;
import com.hisobnoma.platform.web.entity.WebLoyaltyTransactionType;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebLoyaltyTransactionRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebLoyaltyFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private WebCustomerRepository customerRepository;
    @Autowired private WebLoyaltyTransactionRepository loyaltyRepository;
    @Autowired private TenantSettingRepository tenantSettingRepository;

    private Tenant tenant;
    private User adminUser;
    private WebCustomer customer;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Loyalty Tenant").code("FULLFLOW_LOYALTY").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("loyaltyadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        customer = customerRepository.saveAndFlush(WebCustomer.builder()
                .phone("998901234567").name("Test Customer").tenantId(tenant.getId()).build());

        setSetting("loyalty.enabled", "true");
        setSetting("loyalty.earn_percent", "5");
        setSetting("loyalty.expiry_days", "180");
        setSetting("loyalty.min_redeem", "5000");
        setSetting("loyalty.max_redeem_percent_of_order", "50");
    }

    private void setSetting(String key, String value) {
        tenantSettingRepository.saveAndFlush(TenantSetting.builder()
                .tenantId(tenant.getId())
                .settingKey(key)
                .settingValue(value)
                .valueType(SystemSetting.SettingValueType.STRING)
                .build());
    }

    private RequestPostProcessor auth(String... perms) {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true,
                java.util.Arrays.stream(perms)
                        .map(SimpleGrantedAuthority::new)
                        .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                        .toList());
        Authentication a = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(a);
    }

    private RequestPostProcessor manage() {
        return auth("WEB_LOYALTY_VIEW", "WEB_LOYALTY_MANAGE", "WEB_CUSTOMER_VIEW");
    }

    private String customerUrl() {
        return "/api/v1/web-customers/" + customer.getId();
    }

    // ---- permission check ----

    @Test
    void getLoyalty_withoutPermission_returns403() throws Exception {
        mockMvc.perform(get(customerUrl() + "/loyalty")
                        .header("X-Tenant-ID", tenant.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLoyalty_withViewPermission_returnsBalance() throws Exception {
        mockMvc.perform(get(customerUrl() + "/loyalty")
                        .with(auth("WEB_LOYALTY_VIEW", "WEB_CUSTOMER_VIEW"))
                        .header("X-Tenant-ID", tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled", is(true)))
                .andExpect(jsonPath("$.data.balance", is(0)));
    }

    // ---- adjust ----

    @Test
    void adjust_withManagePermission_createsTransaction() throws Exception {
        LoyaltyAdjustRequest request = LoyaltyAdjustRequest.builder()
                .amount(new BigDecimal("15000"))
                .reason("Бонус")
                .build();

        mockMvc.perform(post(customerUrl() + "/loyalty/adjust")
                        .with(manage())
                        .header("X-Tenant-ID", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance", is(15000.0)))
                .andExpect(jsonPath("$.data.entries[0].type", is("ADJUST")))
                .andExpect(jsonPath("$.data.entries[0].note", is("Бонус")));
    }

    @Test
    void adjust_withoutManagePermission_returns403() throws Exception {
        LoyaltyAdjustRequest request = LoyaltyAdjustRequest.builder()
                .amount(new BigDecimal("5000"))
                .reason("Бонус")
                .build();

        mockMvc.perform(post(customerUrl() + "/loyalty/adjust")
                        .with(auth("WEB_LOYALTY_VIEW"))
                        .header("X-Tenant-ID", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adjust_negativeAmount_works() throws Exception {
        loyaltyRepository.saveAndFlush(WebLoyaltyTransaction.builder()
                .tenantId(tenant.getId()).webCustomerId(customer.getId())
                .type(WebLoyaltyTransactionType.EARN).amount(new BigDecimal("20000")).build());

        LoyaltyAdjustRequest request = LoyaltyAdjustRequest.builder()
                .amount(new BigDecimal("-5000"))
                .reason("Тўғрилаш")
                .build();

        mockMvc.perform(post(customerUrl() + "/loyalty/adjust")
                        .with(manage())
                        .header("X-Tenant-ID", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance", is(15000.0)));
    }

    // ---- ledger with earn/spend entries ----

    @Test
    void ledger_showsAllTransactionTypes() throws Exception {
        loyaltyRepository.saveAndFlush(WebLoyaltyTransaction.builder()
                .tenantId(tenant.getId()).webCustomerId(customer.getId())
                .type(WebLoyaltyTransactionType.EARN).amount(new BigDecimal("10000"))
                .note("Буюртма тугалланди").build());
        loyaltyRepository.saveAndFlush(WebLoyaltyTransaction.builder()
                .tenantId(tenant.getId()).webCustomerId(customer.getId())
                .type(WebLoyaltyTransactionType.SPEND).amount(new BigDecimal("-3000"))
                .note("Буюртмага сарфланди").build());

        mockMvc.perform(get(customerUrl() + "/loyalty")
                        .with(manage())
                        .header("X-Tenant-ID", tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance", is(7000.0)))
                .andExpect(jsonPath("$.data.entries", hasSize(2)));
    }

    // ---- tenant isolation ----

    @Test
    void getLoyalty_differentTenantCustomer_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/web-customers/999999/loyalty")
                        .with(manage())
                        .header("X-Tenant-ID", tenant.getId()))
                .andExpect(status().isNotFound());
    }

    // ---- disabled loyalty ----

    @Test
    void getLoyalty_disabled_showsDisabledState() throws Exception {
        tenantSettingRepository.findByTenantIdAndSettingKey(tenant.getId(), "loyalty.enabled")
                .ifPresent(s -> { s.setSettingValue("false"); tenantSettingRepository.saveAndFlush(s); });

        mockMvc.perform(get(customerUrl() + "/loyalty")
                        .with(manage())
                        .header("X-Tenant-ID", tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled", is(false)))
                .andExpect(jsonPath("$.data.balance", is(0)));
    }
}
