package com.hisobnoma.platform.telegram.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TelegramControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Telegram FullFlow Tenant").code("FULLFLOW_TG").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("tgtestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        entityManager.clear();
    }

    private RequestPostProcessor auth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("USER")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(authentication);
    }

    // ---- GET /api/v1/telegram/status ----

    @Test
    void getStatus_notLinked_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/status").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(false))
                .andExpect(jsonPath("$.botUsername").isNotEmpty())
                .andExpect(jsonPath("$.linkedAt").value(""));
    }

    @Test
    void getStatus_linked_returnsTrue() throws Exception {
        user.setTelegramChatId(123456789L);
        user.setTelegramLinkedAt(Instant.now());
        userRepository.saveAndFlush(user);
        entityManager.clear();

        mockMvc.perform(get("/api/v1/telegram/status").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(true))
                .andExpect(jsonPath("$.botUsername").isNotEmpty())
                .andExpect(jsonPath("$.linkedAt").isNotEmpty());
    }

    // ---- POST /api/v1/telegram/link-code ----

    @Test
    void generateLinkCode_returnsCode() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/link-code").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.botUsername").isNotEmpty())
                .andExpect(jsonPath("$.expiresInSeconds").value("600"));
    }

    // ---- DELETE /api/v1/telegram/unlink ----

    @Test
    void unlink_whenLinked_clearsFields() throws Exception {
        user.setTelegramChatId(123456789L);
        user.setTelegramLinkedAt(Instant.now());
        userRepository.saveAndFlush(user);
        entityManager.clear();

        mockMvc.perform(delete("/api/v1/telegram/unlink").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("unlinked"));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertNull(updatedUser.getTelegramChatId());
        assertNull(updatedUser.getTelegramLinkedAt());
    }

    @Test
    void unlink_whenNotLinked_returnsSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/telegram/unlink").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("unlinked"));
    }
}
