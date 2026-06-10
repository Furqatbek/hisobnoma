package com.hisobnoma.platform.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.sms.entity.SmsTemplate;
import com.hisobnoma.platform.sms.repository.SmsTemplateRepository;
import com.hisobnoma.platform.web.dto.CreateCampaignRequest;
import com.hisobnoma.platform.web.entity.WebCampaign;
import com.hisobnoma.platform.web.entity.WebCampaignStatus;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebSegmentType;
import com.hisobnoma.platform.web.repository.WebCampaignRepository;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 3 (loyalty plan) end-to-end: create a campaign, preview its cost
 * (opted-out customers excluded), send it once (DRAFT→SENDING), and enforce
 * permissions + the no-double-blast guard.
 *
 * NOTE: final SENT/FAILED counts are produced by the async dispatcher in a
 * separate transaction that can't see this test's uncommitted fixtures, so
 * that finalization is covered by WebCampaignDispatcherTest instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebCampaignFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SmsTemplateRepository templateRepository;
    @Autowired private WebCustomerRepository customerRepository;
    @Autowired private WebCampaignRepository campaignRepository;

    private static final String URL = "/api/v1/web-campaigns";

    private Tenant tenant;
    private User adminUser;
    private SmsTemplate template;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Campaign Tenant").code("FULLFLOW_CAMPAIGN").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("campaignadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        template = templateRepository.saveAndFlush(SmsTemplate.builder()
                .code("PROMO").name("Promo").template("Салом {name}!").active(true)
                .tenantId(tenant.getId()).build());

        // 3 reachable customers + 1 opted out
        customer("998900000001", false);
        customer("998900000002", false);
        customer("998900000003", false);
        customer("998900000004", true);
    }

    private WebCustomer customer(String phone, boolean optOut) {
        return customerRepository.saveAndFlush(WebCustomer.builder()
                .phone(phone).name("C").smsOptOut(optOut).tenantId(tenant.getId()).build());
    }

    private RequestPostProcessor auth(String... perms) {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, java.util.Arrays.stream(perms).map(SimpleGrantedAuthority::new).map(a -> (org.springframework.security.core.GrantedAuthority) a).toList());
        Authentication a = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(a);
    }

    private RequestPostProcessor manage() {
        return auth("WEB_CAMPAIGN_VIEW", "WEB_CAMPAIGN_MANAGE");
    }

    private CreateCampaignRequest request(WebSegmentType type) {
        return CreateCampaignRequest.builder()
                .name("Spring blast").segmentType(type).smsTemplateId(template.getId()).build();
    }

    private Long createDraft(WebSegmentType type) throws Exception {
        MvcResult result = mockMvc.perform(post(URL)
                        .with(manage())
                        .header("X-Tenant-ID", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(type))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asLong();
    }

    // ---- security ----

    @Test
    void anonymous_isRejected() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isForbidden());
    }

    @Test
    void create_withoutManagePermissionGets403() throws Exception {
        mockMvc.perform(post(URL)
                        .with(auth("WEB_CAMPAIGN_VIEW"))
                        .header("X-Tenant-ID", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(WebSegmentType.ALL_CUSTOMERS))))
                .andExpect(status().isForbidden());
    }

    // ---- create / preview / send ----

    @Test
    void create_startsAsDraft() throws Exception {
        mockMvc.perform(post(URL)
                        .with(manage())
                        .header("X-Tenant-ID", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(WebSegmentType.ALL_CUSTOMERS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.smsTemplateName", is("Promo")));
    }

    @Test
    void preview_excludesOptedOutAndComputesCost() throws Exception {
        Long id = createDraft(WebSegmentType.ALL_CUSTOMERS);

        mockMvc.perform(post(URL + "/" + id + "/preview")
                        .with(manage()).header("X-Tenant-ID", tenant.getId()))
                .andExpect(status().isOk())
                // 3 reachable (the opted-out customer is excluded)
                .andExpect(jsonPath("$.data.recipientCount", is(3)))
                .andExpect(jsonPath("$.data.estimatedCost", closeTo(600.0, 0.01)))
                // SMS not configured in tests → balance unknown → treated as sufficient
                .andExpect(jsonPath("$.data.sufficientBalance", is(true)));
    }

    @Test
    void send_transitionsToSendingAndCannotBeSentTwice() throws Exception {
        Long id = createDraft(WebSegmentType.ALL_CUSTOMERS);

        mockMvc.perform(post(URL + "/" + id + "/send")
                        .with(manage()).header("X-Tenant-ID", tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SENDING")))
                .andExpect(jsonPath("$.data.recipientCount", is(3)));

        WebCampaign campaign = campaignRepository.findByIdAndTenantId(id, tenant.getId()).orElseThrow();
        assertEquals(WebCampaignStatus.SENDING, campaign.getStatus());

        // Second send is rejected — no double blast
        mockMvc.perform(post(URL + "/" + id + "/send")
                        .with(manage()).header("X-Tenant-ID", tenant.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void send_emptySegmentIsRejected() throws Exception {
        // No customer has ever ordered, so NEVER_ORDERED excludes the opted-out
        // one but still has 3 — use MIN_TOTAL_SPENT with a huge threshold for empty
        Long id = createDraft(WebSegmentType.ALL_CUSTOMERS);
        // Opt everyone out so the segment is empty
        for (WebCustomer c : customerRepository.findAllByTenant(tenant.getId(),
                org.springframework.data.domain.PageRequest.of(0, 50)).getContent()) {
            c.setSmsOptOut(true);
            customerRepository.saveAndFlush(c);
        }

        mockMvc.perform(post(URL + "/" + id + "/send")
                        .with(manage()).header("X-Tenant-ID", tenant.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void segmentParamRequired_isValidated() throws Exception {
        CreateCampaignRequest request = request(WebSegmentType.NO_ORDER_IN_N_DAYS); // needs param
        mockMvc.perform(post(URL)
                        .with(manage()).header("X-Tenant-ID", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returnsCreatedCampaigns() throws Exception {
        createDraft(WebSegmentType.ALL_CUSTOMERS);
        createDraft(WebSegmentType.NEVER_ORDERED);

        mockMvc.perform(get(URL).with(manage()).header("X-Tenant-ID", tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }
}
