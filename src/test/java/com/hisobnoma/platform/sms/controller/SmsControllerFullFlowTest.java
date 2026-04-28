package com.hisobnoma.platform.sms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.sms.dto.SmsTemplateRequest;
import com.hisobnoma.platform.sms.entity.SmsTemplate;
import com.hisobnoma.platform.sms.repository.SmsTemplateRepository;
import jakarta.persistence.EntityManager;
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
class SmsControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SmsTemplateRepository templateRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private SmsTemplate existingTemplate;

    private static final String BASE_URL = "/api/v1/sms";

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("SMS FullFlow Tenant").code("FULLFLOW_SMS").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("smstestuser")
                .passwordHash(passwordEncoder.encode("test123"))
                .tenantId(tenant.getId()).enabled(true).build());

        existingTemplate = templateRepository.saveAndFlush(SmsTemplate.builder()
                .code("WELCOME")
                .name("Welcome SMS")
                .template("Salom {name}, xush kelibsiz!")
                .active(true)
                .tenantId(tenant.getId())
                .build());

        entityManager.clear();
    }

    // ── Auth helpers ──────────────────────────────────────────────────

    private RequestPostProcessor smsManageAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "test123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("SMS_TEMPLATES_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor smsSendOnlyAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "test123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("SMS_SEND")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "test123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ── 1. GET /templates — list all ──────────────────────────────────

    @Test
    void getTemplates_returnsAll() throws Exception {
        mockMvc.perform(get(BASE_URL + "/templates")
                        .with(smsManageAuth())
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].code").value("WELCOME"))
                .andExpect(jsonPath("$.data[0].name").value("Welcome SMS"))
                .andExpect(jsonPath("$.data[0].template").value("Salom {name}, xush kelibsiz!"))
                .andExpect(jsonPath("$.data[0].active").value(true))
                .andExpect(jsonPath("$.data[0].id").isNumber());
    }

    // ── 2. GET /templates?activeOnly=true — filters inactive ─────────

    @Test
    void getTemplates_activeOnly_filtersInactive() throws Exception {
        templateRepository.saveAndFlush(SmsTemplate.builder()
                .code("INACTIVE_TPL")
                .name("Inactive Template")
                .template("Bu shablon faol emas")
                .active(false)
                .tenantId(tenant.getId())
                .build());
        entityManager.clear();

        // activeOnly=true should exclude the inactive template
        mockMvc.perform(get(BASE_URL + "/templates")
                        .param("activeOnly", "true")
                        .with(smsManageAuth())
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].code", not(hasItem("INACTIVE_TPL"))))
                .andExpect(jsonPath("$.data[*].code", hasItem("WELCOME")));

        // activeOnly=false should include both
        mockMvc.perform(get(BASE_URL + "/templates")
                        .param("activeOnly", "false")
                        .with(smsManageAuth())
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].code", hasItems("WELCOME", "INACTIVE_TPL")));
    }

    // ── 3. GET /templates/{id} — single template with variables ──────

    @Test
    void getTemplate_returnsById() throws Exception {
        mockMvc.perform(get(BASE_URL + "/templates/{id}", existingTemplate.getId())
                        .with(smsManageAuth())
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(existingTemplate.getId()))
                .andExpect(jsonPath("$.data.code").value("WELCOME"))
                .andExpect(jsonPath("$.data.name").value("Welcome SMS"))
                .andExpect(jsonPath("$.data.template").value("Salom {name}, xush kelibsiz!"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.variables").isArray())
                .andExpect(jsonPath("$.data.variables", hasItem("name")))
                .andExpect(jsonPath("$.data.variables.length()").value(1));
    }

    // ── 4. GET /templates/{id} — not found ───────────────────────────

    @Test
    void getTemplate_notFound_returnsError() throws Exception {
        mockMvc.perform(get(BASE_URL + "/templates/{id}", 999999L)
                        .with(smsManageAuth())
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().is5xxServerError());
    }

    // ── 5. POST /templates — create valid ────────────────────────────

    @Test
    void createTemplate_validRequest_returnsCreated() throws Exception {
        SmsTemplateRequest request = new SmsTemplateRequest();
        request.setCode("ORDER_CONFIRM");
        request.setName("Order Confirmation");
        request.setTemplate("Buyurtma #{orderId} tasdiqlandi. Summa: {amount} so'm.");
        request.setActive(true);

        mockMvc.perform(post(BASE_URL + "/templates")
                        .with(smsManageAuth())
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("ORDER_CONFIRM"))
                .andExpect(jsonPath("$.data.name").value("Order Confirmation"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.variables", hasItems("orderId", "amount")));
    }

    // ── 6. POST /templates — duplicate code ──────────────────────────

    @Test
    void createTemplate_duplicateCode_returnsBadRequest() throws Exception {
        SmsTemplateRequest request = new SmsTemplateRequest();
        request.setCode("WELCOME");
        request.setName("Duplicate Welcome");
        request.setTemplate("Duplicate template text");
        request.setActive(true);

        mockMvc.perform(post(BASE_URL + "/templates")
                        .with(smsManageAuth())
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    // ── 7. POST /templates — invalid code pattern ────────────────────

    @Test
    void createTemplate_invalidCodePattern_returnsBadRequest() throws Exception {
        SmsTemplateRequest request = new SmsTemplateRequest();
        request.setCode("lowercase");
        request.setName("Invalid Code Template");
        request.setTemplate("Some text");
        request.setActive(true);

        mockMvc.perform(post(BASE_URL + "/templates")
                        .with(smsManageAuth())
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── 8. PUT /templates/{id} — update valid ────────────────────────

    @Test
    void updateTemplate_validRequest_returnsUpdated() throws Exception {
        SmsTemplateRequest request = new SmsTemplateRequest();
        request.setCode("WELCOME_V2");
        request.setName("Welcome SMS v2");
        request.setTemplate("Salom {name}, {company} ga xush kelibsiz!");
        request.setActive(true);

        mockMvc.perform(put(BASE_URL + "/templates/{id}", existingTemplate.getId())
                        .with(smsManageAuth())
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("WELCOME_V2"))
                .andExpect(jsonPath("$.data.name").value("Welcome SMS v2"))
                .andExpect(jsonPath("$.data.template").value("Salom {name}, {company} ga xush kelibsiz!"))
                .andExpect(jsonPath("$.data.variables", hasItems("name", "company")))
                .andExpect(jsonPath("$.data.variables.length()").value(2));
    }

    // ── 9. DELETE /templates/{id} — delete and verify ────────────────

    @Test
    void deleteTemplate_returnsSuccess() throws Exception {
        // Delete the template
        mockMvc.perform(delete(BASE_URL + "/templates/{id}", existingTemplate.getId())
                        .with(smsManageAuth())
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify GET now returns error (template no longer exists)
        mockMvc.perform(get(BASE_URL + "/templates/{id}", existingTemplate.getId())
                        .with(smsManageAuth())
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().is5xxServerError());
    }

    // ── 10. Permission check — wrong permission returns 403 ──────────

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        // GET /templates is allowed with SMS_SEND, but not with OTHER_PERM
        mockMvc.perform(get(BASE_URL + "/templates")
                        .with(noPermAuth())
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isForbidden());

        // GET /templates/{id} requires SMS_TEMPLATES_MANAGE or ADMIN_SETTINGS_MANAGE
        mockMvc.perform(get(BASE_URL + "/templates/{id}", existingTemplate.getId())
                        .with(noPermAuth())
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isForbidden());

        // POST /templates requires SMS_TEMPLATES_MANAGE or ADMIN_SETTINGS_MANAGE
        SmsTemplateRequest request = new SmsTemplateRequest();
        request.setCode("FORBIDDEN_TPL");
        request.setName("Forbidden Template");
        request.setTemplate("Should not be created");

        mockMvc.perform(post(BASE_URL + "/templates")
                        .with(noPermAuth())
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // PUT /templates/{id} requires SMS_TEMPLATES_MANAGE or ADMIN_SETTINGS_MANAGE
        mockMvc.perform(put(BASE_URL + "/templates/{id}", existingTemplate.getId())
                        .with(noPermAuth())
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // DELETE /templates/{id} requires SMS_TEMPLATES_MANAGE or ADMIN_SETTINGS_MANAGE
        mockMvc.perform(delete(BASE_URL + "/templates/{id}", existingTemplate.getId())
                        .with(noPermAuth())
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isForbidden());

        // SMS_SEND only should NOT access single template, create, update, or delete
        mockMvc.perform(get(BASE_URL + "/templates/{id}", existingTemplate.getId())
                        .with(smsSendOnlyAuth())
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE_URL + "/templates")
                        .with(smsSendOnlyAuth())
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
