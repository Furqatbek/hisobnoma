package com.hisobnoma.platform.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.dto.RequestOtpRequest;
import com.hisobnoma.platform.web.dto.VerifyOtpRequest;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderLine;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebAuthFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private WebOrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;

    @MockBean
    private SmsService smsService;

    private static final String TENANT_HEADER = "X-Tenant-ID";

    /**
     * Per-IP OTP limiter is an app-scoped singleton — unique phones AND
     * X-Forwarded-For per test keep tests independent.
     */
    private static final AtomicLong SEQ = new AtomicLong(5000000);

    private Tenant tenant;
    private User adminUser;
    private long seq;

    @BeforeEach
    void setUp() {
        seq = SEQ.incrementAndGet();
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Web Auth Tenant").code("FULLFLOW_WEBAUTH").active(true)
                .maxUsers(100).maxLocations(10).build());
        adminUser = userRepository.saveAndFlush(User.builder()
                .username("webauthadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());
    }

    private String uniquePhone() {
        return "+99890" + SEQ.incrementAndGet();
    }

    private String uniqueIp() {
        return "10.9." + (seq % 250) + "." + (SEQ.incrementAndGet() % 250);
    }

    private String requestOtpAndCaptureCode(String phone, String ip) throws Exception {
        mockMvc.perform(post("/api/v1/web/auth/request-otp")
                        .header(TENANT_HEADER, tenant.getId())
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestOtpRequest(phone))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService, org.mockito.Mockito.atLeastOnce())
                .sendSmsAsync(anyString(), messageCaptor.capture());
        return messageCaptor.getValue().replaceAll("[^0-9]", "");
    }

    private String loginAndGetToken(String phone, String name) throws Exception {
        String code = requestOtpAndCaptureCode(phone, uniqueIp());
        MvcResult result = mockMvc.perform(post("/api/v1/web/auth/verify")
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                VerifyOtpRequest.builder().phone(phone).code(code).name(name).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", not(emptyString())))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/token").asText();
    }

    private WebOrder persistOrder(String phone, String number) {
        WebOrder order = WebOrder.builder()
                .orderNumber(number).status(WebOrderStatus.NEW)
                .customerName("Ali").phone(phone)
                .phoneNormalized(phone.replaceAll("[^0-9]", ""))
                .totalAmount(new BigDecimal("12000")).tenantId(tenant.getId())
                .build();
        order.addLine(WebOrderLine.builder()
                .productId(1L).productName("Cola")
                .quantity(BigDecimal.ONE).unitPrice(new BigDecimal("12000"))
                .lineTotal(new BigDecimal("12000")).tenantId(tenant.getId())
                .build());
        return orderRepository.saveAndFlush(order);
    }

    // ---- OTP flow ----

    @Test
    void fullLoginFlow_otpSmsVerifyToken() throws Exception {
        String phone = uniquePhone();
        String token = loginAndGetToken(phone, "Ali Valiyev");

        mockMvc.perform(get("/api/v1/web/me")
                        .header(TENANT_HEADER, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Ali Valiyev")));
    }

    @Test
    void wrongCode_rejectedAndEventuallyLocked() throws Exception {
        String phone = uniquePhone();
        String code = requestOtpAndCaptureCode(phone, uniqueIp());
        String wrongCode = code.equals("000000") ? "000001" : "000000";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/web/auth/verify")
                            .header(TENANT_HEADER, tenant.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    VerifyOtpRequest.builder().phone(phone).code(wrongCode).build())))
                    .andExpect(status().isBadRequest());
        }

        // After 5 wrong attempts even the correct code is rejected
        mockMvc.perform(post("/api/v1/web/auth/verify")
                        .header(TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                VerifyOtpRequest.builder().phone(phone).code(code).build())))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void immediateResend_blockedByCooldown() throws Exception {
        String phone = uniquePhone();
        String ip = uniqueIp();
        requestOtpAndCaptureCode(phone, ip);

        mockMvc.perform(post("/api/v1/web/auth/request-otp")
                        .header(TENANT_HEADER, tenant.getId())
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestOtpRequest(phone))))
                .andExpect(status().isTooManyRequests());
    }

    // ---- /me/orders scoping ----

    @Test
    void myOrders_returnsOnlyOwnOrders() throws Exception {
        String phone = uniquePhone();
        persistOrder(phone, "WO-AUTH-01");
        persistOrder(uniquePhone(), "WO-AUTH-02"); // someone else's order

        String token = loginAndGetToken(phone, null);

        mockMvc.perform(get("/api/v1/web/me/orders")
                        .header(TENANT_HEADER, tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderNumber", is("WO-AUTH-01")));
    }

    @Test
    void myOrders_withoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/web/me/orders")
                        .header(TENANT_HEADER, tenant.getId())
                        .header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    // ---- token separation ----

    @Test
    void webCustomerToken_rejectedOnStaffEndpoints() throws Exception {
        String token = loginAndGetToken(uniquePhone(), null);

        mockMvc.perform(get("/api/v1/web-orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffJwt_rejectedOnWebMeEndpoints() throws Exception {
        // A staff-style bearer (not signed with the web-customer key) must not work on /me
        mockMvc.perform(get("/api/v1/web/me/orders")
                        .header(TENANT_HEADER, tenant.getId())
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.staff.token"))
                .andExpect(status().isUnauthorized());
    }

    // ---- staff management of web customers ----

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

    @Test
    void staffList_requiresPermissionAndShowsCustomers() throws Exception {
        String phone = uniquePhone();
        loginAndGetToken(phone, "Ali Valiyev");

        mockMvc.perform(get("/api/v1/web-customers"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/web-customers").with(staffAuth("WEB_CUSTOMER_VIEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Ali Valiyev")));
    }

    @Test
    void staffLinksWebCustomerToArCustomer() throws Exception {
        String phone = uniquePhone();
        loginAndGetToken(phone, "Ali");

        Customer arCustomer = customerRepository.saveAndFlush(Customer.builder()
                .code("CUST-LNK-01").name("Ali Valiyev").active(true)
                .currentBalance(BigDecimal.ZERO).totalInvoiced(BigDecimal.ZERO)
                .totalReceived(BigDecimal.ZERO).creditHold(false)
                .tenantId(tenant.getId()).build());

        MvcResult listResult = mockMvc.perform(get("/api/v1/web-customers")
                        .with(staffAuth("WEB_CUSTOMER_VIEW")))
                .andExpect(status().isOk())
                .andReturn();
        long webCustomerId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .at("/content/0/id").asLong();

        mockMvc.perform(post("/api/v1/web-customers/" + webCustomerId + "/link-customer")
                        .with(staffAuth("WEB_CUSTOMER_VIEW", "WEB_CUSTOMER_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\": " + arCustomer.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId", is(arCustomer.getId().intValue())))
                .andExpect(jsonPath("$.data.customerCode", is("CUST-LNK-01")));

        mockMvc.perform(post("/api/v1/web-customers/" + webCustomerId + "/unlink-customer")
                        .with(staffAuth("WEB_CUSTOMER_VIEW", "WEB_CUSTOMER_MANAGE")))
                .andExpect(status().isOk())
                // nulls are omitted from JSON, so the field must be absent
                .andExpect(jsonPath("$.data.customerId").doesNotExist());
    }

    @Test
    void linkAction_viewOnlyPermissionGets403() throws Exception {
        loginAndGetToken(uniquePhone(), null);

        MvcResult listResult = mockMvc.perform(get("/api/v1/web-customers")
                        .with(staffAuth("WEB_CUSTOMER_VIEW")))
                .andReturn();
        long webCustomerId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .at("/content/0/id").asLong();

        mockMvc.perform(post("/api/v1/web-customers/" + webCustomerId + "/link-customer")
                        .with(staffAuth("WEB_CUSTOMER_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\": 1}"))
                .andExpect(status().isForbidden());
    }
}
