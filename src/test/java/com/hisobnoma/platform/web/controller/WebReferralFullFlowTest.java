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
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.dto.RegisterDeviceTokenRequest;
import com.hisobnoma.platform.web.dto.RequestOtpRequest;
import com.hisobnoma.platform.web.dto.VerifyOtpRequest;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderLine;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebDeviceTokenRepository;
import com.hisobnoma.platform.web.repository.WebLoyaltyTransactionRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebReferralFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private WebCustomerRepository customerRepository;
    @Autowired private WebOrderRepository orderRepository;
    @Autowired private WebLoyaltyTransactionRepository loyaltyRepository;
    @Autowired private WebDeviceTokenRepository deviceTokenRepository;
    @Autowired private TenantSettingRepository tenantSettingRepository;

    @MockBean private SmsService smsService;

    private static final AtomicLong SEQ = new AtomicLong(7000000);
    private static final String H = "X-Tenant-ID";

    private Tenant tenant;
    private User adminUser;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Referral Tenant").code("FULLFLOW_REF").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("refadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        setSetting("loyalty.enabled", "true");
        setSetting("loyalty.earn_percent", "5");
        setSetting("loyalty.expiry_days", "180");
        setSetting("loyalty.min_redeem", "5000");
        setSetting("loyalty.max_redeem_percent_of_order", "50");
        setSetting("referral.enabled", "true");
        setSetting("referral.reward_referrer", "10000");
        setSetting("referral.reward_referred", "5000");
    }

    private void setSetting(String key, String value) {
        tenantSettingRepository.saveAndFlush(TenantSetting.builder()
                .tenantId(tenant.getId())
                .settingKey(key)
                .settingValue(value)
                .valueType(SystemSetting.SettingValueType.STRING)
                .build());
    }

    private String uniquePhone() { return "+99890" + SEQ.incrementAndGet(); }
    private String uniqueIp() { return "10.8." + (SEQ.incrementAndGet() % 250) + "." + (SEQ.incrementAndGet() % 250); }

    private String loginAndGetToken(String phone, String name, String referralCode) throws Exception {
        mockMvc.perform(post("/api/v1/web/auth/request-otp")
                        .header(H, tenant.getId())
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestOtpRequest(phone))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(smsService, atLeastOnce()).sendSmsAsync(anyString(), cap.capture());
        String code = cap.getValue().replaceAll("[^0-9]", "");

        VerifyOtpRequest req = VerifyOtpRequest.builder()
                .phone(phone).code(code).name(name).referralCode(referralCode).build();

        MvcResult result = mockMvc.perform(post("/api/v1/web/auth/verify")
                        .header(H, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/token").asText();
    }

    private RequestPostProcessor staffAuth(String... permissions) {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true,
                java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new)
                        .map(a -> (org.springframework.security.core.GrantedAuthority) a).toList());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- device token endpoints ----

    @Test
    void deviceToken_registerAndUnregister() throws Exception {
        String phone = uniquePhone();
        String token = loginAndGetToken(phone, "Ali", null);

        mockMvc.perform(post("/api/v1/web/me/device-token")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RegisterDeviceTokenRequest.builder()
                                        .token("fcm-test-token-001").platform("ANDROID").build())))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/web/me/device-token")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("token", "fcm-test-token-001"))
                .andExpect(status().isOk());
    }

    @Test
    void deviceToken_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/web/me/device-token")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RegisterDeviceTokenRequest.builder()
                                        .token("fcm-xxx").platform("IOS").build())))
                .andExpect(status().isUnauthorized());
    }

    // ---- referral code ----

    @Test
    void referralCode_generatedAndReturnedOnGet() throws Exception {
        String phone = uniquePhone();
        String token = loginAndGetToken(phone, "Referrer", null);

        mockMvc.perform(get("/api/v1/web/me/referral-code")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code", not(emptyString())));
    }

    // ---- full referral flow ----

    @Test
    void fullReferralFlow_referAndRewardOnFirstCompletion() throws Exception {
        // Customer A gets a referral code
        String phoneA = uniquePhone();
        String tokenA = loginAndGetToken(phoneA, "Referrer A", null);

        MvcResult codeResult = mockMvc.perform(get("/api/v1/web/me/referral-code")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        String refCode = objectMapper.readTree(codeResult.getResponse().getContentAsString())
                .at("/data/code").asText();

        // Customer B registers with A's referral code
        String phoneB = uniquePhone();
        loginAndGetToken(phoneB, "Referred B", refCode);

        // B places and completes an order
        String normalizedB = phoneB.replaceAll("[^0-9]", "");
        WebOrder order = WebOrder.builder()
                .orderNumber("WO-REF-01").status(WebOrderStatus.CONFIRMED)
                .customerName("Referred B").phone(phoneB).phoneNormalized(normalizedB)
                .totalAmount(new BigDecimal("50000")).tenantId(tenant.getId())
                .build();
        order.addLine(WebOrderLine.builder()
                .productId(1L).productName("Test Item")
                .quantity(BigDecimal.ONE).unitPrice(new BigDecimal("50000"))
                .lineTotal(new BigDecimal("50000")).tenantId(tenant.getId())
                .build());
        order = orderRepository.saveAndFlush(order);

        // Staff completes the order
        mockMvc.perform(post("/api/v1/web-orders/" + order.getId() + "/status")
                        .with(staffAuth("WEB_ORDER_VIEW", "WEB_ORDER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());

        // Check loyalty ledger for customer A (referrer) — should have ADJUST
        String normalizedA = phoneA.replaceAll("[^0-9]", "");
        WebCustomer customerA = customerRepository.findByTenantIdAndPhone(tenant.getId(), normalizedA).orElseThrow();
        BigDecimal balanceA = loyaltyRepository.balanceByCustomer(tenant.getId(), customerA.getId());
        assertTrue(balanceA.compareTo(BigDecimal.ZERO) > 0, "Referrer should have loyalty points");

        // Check loyalty ledger for customer B (referred)
        WebCustomer customerB = customerRepository.findByTenantIdAndPhone(tenant.getId(), normalizedB).orElseThrow();
        BigDecimal balanceB = loyaltyRepository.balanceByCustomer(tenant.getId(), customerB.getId());
        assertTrue(balanceB.compareTo(BigDecimal.ZERO) > 0, "Referred should have loyalty points");

        // B's second order should NOT add more referral rewards
        WebOrder order2 = WebOrder.builder()
                .orderNumber("WO-REF-02").status(WebOrderStatus.CONFIRMED)
                .customerName("Referred B").phone(phoneB).phoneNormalized(normalizedB)
                .totalAmount(new BigDecimal("30000")).tenantId(tenant.getId())
                .build();
        order2.addLine(WebOrderLine.builder()
                .productId(1L).productName("Test Item 2")
                .quantity(BigDecimal.ONE).unitPrice(new BigDecimal("30000"))
                .lineTotal(new BigDecimal("30000")).tenantId(tenant.getId())
                .build());
        order2 = orderRepository.saveAndFlush(order2);

        mockMvc.perform(post("/api/v1/web-orders/" + order2.getId() + "/status")
                        .with(staffAuth("WEB_ORDER_VIEW", "WEB_ORDER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());

        BigDecimal balanceA2 = loyaltyRepository.balanceByCustomer(tenant.getId(), customerA.getId());
        assertEquals(0, balanceA.compareTo(balanceA2), "Referrer balance should not change on second order");
    }

    // ---- referral stats + signup-bonus interplay ----

    @Test
    void referralStats_reflectInviteAndEarnings_withSignupBonus() throws Exception {
        setSetting("loyalty.signup_bonus", "7000");

        // Customer A gets a referral code
        String phoneA = uniquePhone();
        String tokenA = loginAndGetToken(phoneA, "Referrer A", null);

        MvcResult codeResult = mockMvc.perform(get("/api/v1/web/me/referral-code")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        String refCode = objectMapper.readTree(codeResult.getResponse().getContentAsString())
                .at("/data/code").asText();

        // Customer B signs up with A's code — signup bonus lands immediately
        String phoneB = uniquePhone();
        loginAndGetToken(phoneB, "Referred B", refCode);

        String normalizedB = phoneB.replaceAll("[^0-9]", "");
        WebCustomer customerB = customerRepository
                .findByTenantIdAndPhone(tenant.getId(), normalizedB).orElseThrow();
        BigDecimal afterSignup = loyaltyRepository.balanceByCustomer(tenant.getId(), customerB.getId());
        assertEquals(0, new BigDecimal("7000").compareTo(afterSignup),
                "Referred customer should hold exactly the signup bonus after first sign-in");

        // B's first completed order: 5% earn on 50000 + referred reward, referrer reward for A
        WebOrder order = WebOrder.builder()
                .orderNumber("WO-REF-03").status(WebOrderStatus.CONFIRMED)
                .customerName("Referred B").phone(phoneB).phoneNormalized(normalizedB)
                .totalAmount(new BigDecimal("50000")).tenantId(tenant.getId())
                .build();
        order.addLine(WebOrderLine.builder()
                .productId(1L).productName("Test Item")
                .quantity(BigDecimal.ONE).unitPrice(new BigDecimal("50000"))
                .lineTotal(new BigDecimal("50000")).tenantId(tenant.getId())
                .build());
        order = orderRepository.saveAndFlush(order);

        mockMvc.perform(post("/api/v1/web-orders/" + order.getId() + "/status")
                        .with(staffAuth("WEB_ORDER_VIEW", "WEB_ORDER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());

        BigDecimal balanceB = loyaltyRepository.balanceByCustomer(tenant.getId(), customerB.getId());
        assertEquals(0, new BigDecimal("14500").compareTo(balanceB),
                "Referred: 7000 signup + 5000 referral + 2500 earn");

        String normalizedA = phoneA.replaceAll("[^0-9]", "");
        WebCustomer customerA = customerRepository
                .findByTenantIdAndPhone(tenant.getId(), normalizedA).orElseThrow();
        BigDecimal balanceA = loyaltyRepository.balanceByCustomer(tenant.getId(), customerA.getId());
        assertEquals(0, new BigDecimal("17000").compareTo(balanceA),
                "Referrer: 7000 own signup bonus + 10000 referrer reward");

        // Stats endpoint for A reflects the invite and the earnings
        MvcResult statsResult = mockMvc.perform(get("/api/v1/web/me/referral-stats")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code", is(refCode)))
                .andExpect(jsonPath("$.data.enabled", is(true)))
                .andExpect(jsonPath("$.data.invitedCount", is(1)))
                .andReturn();
        BigDecimal pointsEarned = new BigDecimal(objectMapper
                .readTree(statsResult.getResponse().getContentAsString())
                .at("/data/pointsEarned").asText());
        assertEquals(0, new BigDecimal("10000").compareTo(pointsEarned),
                "Stats pointsEarned should equal the referrer reward");
    }

    @Test
    void referralStats_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/web/me/referral-stats")
                        .header(H, tenant.getId())
                        .header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    // ---- staff customer view with referral info ----

    @Test
    void staffCustomerView_showsReferralInfo() throws Exception {
        String phoneA = uniquePhone();
        loginAndGetToken(phoneA, "Referrer", null);

        // Generate referral code
        String normalizedA = phoneA.replaceAll("[^0-9]", "");
        WebCustomer customerA = customerRepository.findByTenantIdAndPhone(tenant.getId(), normalizedA).orElseThrow();
        customerA.setReferralCode("TESTREF1");
        customerRepository.saveAndFlush(customerA);

        mockMvc.perform(get("/api/v1/web-customers/" + customerA.getId())
                        .with(staffAuth("WEB_CUSTOMER_VIEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referralCode", is("TESTREF1")));
    }
}
