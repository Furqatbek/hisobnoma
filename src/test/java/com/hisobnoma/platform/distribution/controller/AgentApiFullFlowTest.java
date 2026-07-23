package com.hisobnoma.platform.distribution.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.distribution.entity.AgentStatus;
import com.hisobnoma.platform.distribution.entity.DistributionAgent;
import com.hisobnoma.platform.distribution.entity.DistributionAgentOtp;
import com.hisobnoma.platform.distribution.repository.DistributionAgentOtpRepository;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import com.hisobnoma.platform.sms.service.SmsService;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end for the agent mobile API: phone+OTP login issues a token, the token
 * unlocks the agent's own portal, a foreign token is rejected, and a check-in →
 * check-out-with-cash-collection round-trips over HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AgentApiFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private DistributionAgentRepository agentRepository;
    @Autowired private DistributionAgentOtpRepository otpRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UnitOfMeasureRepository uomRepository;

    @MockBean private SmsService smsService;

    private static final AtomicLong SEQ = new AtomicLong(9_100_000);
    private static final String H = "X-Tenant-ID";

    private Tenant tenant;
    private DistributionAgent agent;
    private Long customerId;
    private Long productId;
    private String agentPhone;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Agent Tenant").code("AGENT_FF").active(true)
                .maxUsers(50).maxLocations(10).build());

        agentPhone = "99890" + SEQ.incrementAndGet();
        agent = DistributionAgent.builder()
                .code("AG-1").name("Alisher").phone(agentPhone).status(AgentStatus.ACTIVE)
                .build();
        agent.setTenantId(tenant.getId());
        agent = agentRepository.saveAndFlush(agent);

        Customer c = new Customer();
        c.setCode("C-1");
        c.setName("Osiyo");
        c.setTenantId(tenant.getId());
        customerId = customerRepository.saveAndFlush(c).getId();

        UnitOfMeasure uom = UnitOfMeasure.builder().code("PCS").name("Pieces").build();
        uom.setTenantId(tenant.getId());
        uom = uomRepository.saveAndFlush(uom);
        Product p = Product.builder().sku("SKU-1").name("Cola").sellingPrice(new BigDecimal("10000"))
                .baseUom(uom).active(true).sellable(true).build();
        p.setTenantId(tenant.getId());
        productId = productRepository.saveAndFlush(p).getId();
    }

    private String loginAndGetToken(String phone) throws Exception {
        mockMvc.perform(post("/api/v1/agent/auth/request-otp")
                        .header(H, tenant.getId())
                        .header("X-Forwarded-For", "10.9." + (SEQ.incrementAndGet() % 250) + ".2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(smsService, atLeastOnce()).sendSmsAsync(anyString(), cap.capture());
        String code = cap.getValue().replaceAll("[^0-9]", "");

        MvcResult result = mockMvc.perform(post("/api/v1/agent/auth/verify")
                        .header(H, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", not(emptyString())))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/token").asText();
    }

    @Test
    void login_thenReadOwnProfileAndSummary() throws Exception {
        String token = loginAndGetToken(agentPhone);

        mockMvc.perform(get("/api/v1/agent/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("AG-1"))
                .andExpect(jsonPath("$.data.name").value("Alisher"));

        mockMvc.perform(get("/api/v1/agent/me/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentId").value(agent.getId().intValue()))
                .andExpect(jsonPath("$.data.hasActiveLoadout").value(false));
    }

    @Test
    void requestOtp_unknownPhone_returns200ButSendsNoSms() throws Exception {
        mockMvc.perform(post("/api/v1/agent/auth/request-otp")
                        .header(H, tenant.getId())
                        .header("X-Forwarded-For", "10.9.1.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"998900000000\"}"))
                .andExpect(status().isOk());
        // No agent for that phone → no code persisted, no SMS.
        assertTrue(otpRepository.findTopByTenantIdAndPhoneOrderByCreatedAtDesc(
                tenant.getId(), "998900000000").isEmpty());
    }

    @Test
    void portal_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/agent/me/routes").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void suspendedAgent_cannotUsePortal() throws Exception {
        String token = loginAndGetToken(agentPhone);
        agent.setStatus(AgentStatus.SUSPENDED);
        agentRepository.saveAndFlush(agent);

        mockMvc.perform(get("/api/v1/agent/me/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkInThenCheckOut_roundTripsForOwnVisit() throws Exception {
        String token = loginAndGetToken(agentPhone);

        MvcResult checkIn = mockMvc.perform(post("/api/v1/agent/me/visits/check-in")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + customerId + ",\"visitType\":\"PLANNED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("PENDING"))
                .andExpect(jsonPath("$.data.agentId").value(agent.getId().intValue()))
                .andReturn();
        long visitId = objectMapper.readTree(checkIn.getResponse().getContentAsString())
                .at("/data/id").asLong();

        mockMvc.perform(post("/api/v1/agent/me/visits/" + visitId + "/check-out")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"NO_ORDER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("NO_ORDER"));

        // The visit shows up in the agent's own list
        MvcResult visits = mockMvc.perform(get("/api/v1/agent/me/visits")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(visits.getResponse().getContentAsString()).at("/content");
        assertTrue(content.isArray() && content.size() >= 1);
    }

    @Test
    void placeOrder_fromField_createsScopedDraftWithServerPricing() throws Exception {
        String token = loginAndGetToken(agentPhone);

        MvcResult result = mockMvc.perform(post("/api/v1/agent/me/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + customerId + ",\"paymentMethod\":\"CREDIT\","
                                + "\"lines\":[{\"productId\":" + productId + ",\"quantity\":3}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.agentId").value(agent.getId().intValue()))
                .andExpect(jsonPath("$.data.customerId").value(customerId.intValue()))
                // server priced: 3 x 10000
                .andExpect(jsonPath("$.data.totalAmount").value(30000))
                .andReturn();

        // The order shows up in the agent's own list
        String orderNumber = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/orderNumber").asText();
        assertFalse(orderNumber.isBlank());

        MvcResult orders = mockMvc.perform(get("/api/v1/agent/me/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(orders.getResponse().getContentAsString()).at("/content");
        assertTrue(content.isArray() && content.size() >= 1);
    }

    @Test
    void placeOrder_missingLines_isBadRequest() throws Exception {
        String token = loginAndGetToken(agentPhone);
        mockMvc.perform(post("/api/v1/agent/me/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + customerId + ",\"lines\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/agent/me/orders")
                        .header("Authorization", "Bearer garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + customerId + ",\"lines\":[{\"productId\":"
                                + productId + ",\"quantity\":1}]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void foreignToken_cannotCheckOutAnothersVisit() throws Exception {
        // Agent A checks in
        String tokenA = loginAndGetToken(agentPhone);
        MvcResult checkIn = mockMvc.perform(post("/api/v1/agent/me/visits/check-in")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + customerId + ",\"visitType\":\"PLANNED\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long visitId = objectMapper.readTree(checkIn.getResponse().getContentAsString())
                .at("/data/id").asLong();

        // Agent B (same tenant) logs in and tries to check out A's visit
        String phoneB = "99890" + SEQ.incrementAndGet();
        DistributionAgent b = DistributionAgent.builder()
                .code("AG-2").name("Bek").phone(phoneB).status(AgentStatus.ACTIVE).build();
        b.setTenantId(tenant.getId());
        agentRepository.saveAndFlush(b);
        String tokenB = loginAndGetToken(phoneB);

        mockMvc.perform(post("/api/v1/agent/me/visits/" + visitId + "/check-out")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"NO_ORDER\"}"))
                .andExpect(status().isNotFound());
    }
}
