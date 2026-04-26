package com.hisobnoma.platform.telegram.controller;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.telegram.config.TelegramProperties;
import com.hisobnoma.platform.telegram.service.TelegramBotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TelegramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelegramBotService botService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private SecurityContextHelper securityContextHelper;

    @MockBean
    private TelegramProperties properties;

    // ==================== GET /api/v1/telegram/status ====================

    @Test
    @WithMockUser
    void getStatus_authenticated_returns200() throws Exception {
        when(securityContextHelper.getCurrentUserId()).thenReturn(1L);
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(properties.getBotUsername()).thenReturn("test_bot");

        mockMvc.perform(get("/api/v1/telegram/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(false));
    }

    @Test
    void getStatus_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/status"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/telegram/link-code ====================

    @Test
    @WithMockUser
    void generateLinkCode_authenticated_returns200() throws Exception {
        when(securityContextHelper.getCurrentUserId()).thenReturn(1L);
        when(botService.generateLinkCode(1L)).thenReturn("123456");
        when(properties.getBotUsername()).thenReturn("test_bot");

        mockMvc.perform(post("/api/v1/telegram/link-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("123456"));
    }

    @Test
    void generateLinkCode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/link-code"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/telegram/unlink ====================

    @Test
    @WithMockUser
    void unlink_authenticated_returns200() throws Exception {
        when(securityContextHelper.getCurrentUserId()).thenReturn(1L);
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/api/v1/telegram/unlink"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("unlinked"));
    }

    @Test
    void unlink_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/telegram/unlink"))
                .andExpect(status().isForbidden());
    }
}
