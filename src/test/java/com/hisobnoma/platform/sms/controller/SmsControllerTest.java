package com.hisobnoma.platform.sms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.sms.dto.SmsBulkSendRequest;
import com.hisobnoma.platform.sms.dto.SmsSendRequest;
import com.hisobnoma.platform.sms.dto.SmsSettingsRequest;
import com.hisobnoma.platform.sms.dto.SmsTemplateDto;
import com.hisobnoma.platform.sms.dto.SmsTemplateRequest;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.sms.service.SmsTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SmsService smsService;

    @MockBean
    private SmsTemplateService templateService;

    // ==================== POST /api/v1/sms/send ====================

    @Test
    @WithMockUser(authorities = "SMS_SEND")
    void sendSms_authenticated_returns200() throws Exception {
        SmsSendRequest request = new SmsSendRequest();
        request.setPhone("+998901234567");
        request.setTemplateId(1L);
        when(templateService.resolveTemplate(any(), any())).thenReturn("Hello");
        when(smsService.sendSms(any(), any(), any())).thenReturn(Map.of("status", "sent"));

        mockMvc.perform(post("/api/v1/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void sendSms_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void sendSms_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/sms/send-bulk ====================

    @Test
    @WithMockUser(authorities = "SMS_SEND")
    void sendBulkSms_authenticated_returns200() throws Exception {
        SmsBulkSendRequest request = new SmsBulkSendRequest();
        request.setTemplateId(1L);
        SmsBulkSendRequest.Recipient recipient = new SmsBulkSendRequest.Recipient();
        recipient.setPhone("+998901234567");
        request.setRecipients(List.of(recipient));
        when(smsService.sendBulk(any(), any(), any())).thenReturn(Map.of("status", "sent"));

        mockMvc.perform(post("/api/v1/sms/send-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void sendBulkSms_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/sms/send-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void sendBulkSms_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/sms/send-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/sms/templates ====================

    @Test
    @WithMockUser(authorities = "SMS_SEND")
    void getTemplates_authenticated_returns200() throws Exception {
        when(templateService.getAllTemplates()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/sms/templates"))
                .andExpect(status().isOk());
    }

    @Test
    void getTemplates_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/sms/templates"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getTemplates_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/sms/templates"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/sms/templates/{id} ====================

    @Test
    @WithMockUser(authorities = "SMS_TEMPLATES_MANAGE")
    void getTemplate_authenticated_returns200() throws Exception {
        SmsTemplateDto dto = SmsTemplateDto.builder().id(1L).code("WELCOME").name("Welcome").template("Hello {name}").active(true).variables(List.of("name")).build();
        when(templateService.getTemplate(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/sms/templates/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getTemplate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/sms/templates/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getTemplate_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/sms/templates/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/sms/templates ====================

    @Test
    @WithMockUser(authorities = "SMS_TEMPLATES_MANAGE")
    void createTemplate_authenticated_returns200() throws Exception {
        SmsTemplateRequest request = new SmsTemplateRequest();
        request.setCode("NEW_TEMPLATE");
        request.setName("New Template");
        request.setTemplate("Hello {name}");
        SmsTemplateDto dto = SmsTemplateDto.builder().id(1L).code("NEW").name("New").template("Hello").active(true).variables(List.of()).build();
        when(templateService.createTemplate(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/sms/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void createTemplate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/sms/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createTemplate_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/sms/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/sms/templates/{id} ====================

    @Test
    @WithMockUser(authorities = "SMS_TEMPLATES_MANAGE")
    void updateTemplate_authenticated_returns200() throws Exception {
        SmsTemplateRequest request = new SmsTemplateRequest();
        request.setCode("UPD_TEMPLATE");
        request.setName("Updated Template");
        request.setTemplate("Hi {name}");
        SmsTemplateDto dto = SmsTemplateDto.builder().id(1L).code("UPD").name("Updated").template("Hi").active(true).variables(List.of()).build();
        when(templateService.updateTemplate(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/sms/templates/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateTemplate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/sms/templates/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/sms/templates/{id} ====================

    @Test
    @WithMockUser(authorities = "SMS_TEMPLATES_MANAGE")
    void deleteTemplate_authenticated_returns200() throws Exception {
        doNothing().when(templateService).deleteTemplate(1L);

        mockMvc.perform(delete("/api/v1/sms/templates/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTemplate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/sms/templates/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteTemplate_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/sms/templates/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/sms/history ====================

    @Test
    @WithMockUser(authorities = "SMS_VIEW")
    void getHistory_authenticated_returns200() throws Exception {
        when(smsService.getHistory(anyInt(), anyInt(), any())).thenReturn(Map.of());

        mockMvc.perform(get("/api/v1/sms/history"))
                .andExpect(status().isOk());
    }

    @Test
    void getHistory_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/sms/history"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getHistory_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/sms/history"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/sms/balance ====================

    @Test
    @WithMockUser(authorities = "SMS_VIEW")
    void getBalance_authenticated_returns200() throws Exception {
        when(smsService.getBalance()).thenReturn(Map.of("balance", 1000));

        mockMvc.perform(get("/api/v1/sms/balance"))
                .andExpect(status().isOk());
    }

    @Test
    void getBalance_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/sms/balance"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getBalance_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/sms/balance"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/sms/settings ====================

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void getSettings_authenticated_returns200() throws Exception {
        when(smsService.getSettings()).thenReturn(Map.of("enabled", true));

        mockMvc.perform(get("/api/v1/sms/settings"))
                .andExpect(status().isOk());
    }

    @Test
    void getSettings_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/sms/settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getSettings_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/sms/settings"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/sms/settings ====================

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void saveSettings_authenticated_returns200() throws Exception {
        SmsSettingsRequest request = new SmsSettingsRequest();
        doNothing().when(smsService).updateSettings(anyBoolean(), any(), any());
        when(smsService.getBalance()).thenReturn(Map.of("success", true));
        when(smsService.getSettings()).thenReturn(Map.of("enabled", true));

        mockMvc.perform(post("/api/v1/sms/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void saveSettings_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/sms/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void saveSettings_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/sms/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
