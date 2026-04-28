package com.hisobnoma.platform.mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.mobile.dto.AlertPreferenceDTO;
import com.hisobnoma.platform.mobile.entity.AlertPreference;
import com.hisobnoma.platform.mobile.entity.MobileAlert;
import com.hisobnoma.platform.mobile.repository.AlertPreferenceRepository;
import com.hisobnoma.platform.mobile.repository.MobileAlertRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MobileAlertControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @Autowired private MobileAlertRepository alertRepository;
    @Autowired private AlertPreferenceRepository preferenceRepository;

    private Tenant tenant;
    private User user;
    private MobileAlert alert1;
    private MobileAlert alert2;
    private MobileAlert alert3;
    private AlertPreference preference;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Mobile Alert Tenant").code("FULLFLOW_MOBALERT").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("mobalertuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        // Alert 1: unread LOW_STOCK
        alert1 = MobileAlert.builder()
                .userId(user.getId())
                .alertType(MobileAlert.AlertType.LOW_STOCK)
                .title("Low Stock Alert")
                .message("Product XYZ is low")
                .priority(MobileAlert.AlertPriority.HIGH)
                .read(false)
                .sentViaPush(false)
                .build();
        alert1.setTenantId(tenant.getId());
        alert1 = alertRepository.saveAndFlush(alert1);

        // Alert 2: unread PAYMENT_RECEIVED
        alert2 = MobileAlert.builder()
                .userId(user.getId())
                .alertType(MobileAlert.AlertType.PAYMENT_RECEIVED)
                .title("Payment Received")
                .message("Payment of $500 received")
                .priority(MobileAlert.AlertPriority.NORMAL)
                .read(false)
                .sentViaPush(false)
                .build();
        alert2.setTenantId(tenant.getId());
        alert2 = alertRepository.saveAndFlush(alert2);

        // Alert 3: read SYSTEM_ALERT
        alert3 = MobileAlert.builder()
                .userId(user.getId())
                .alertType(MobileAlert.AlertType.SYSTEM_ALERT)
                .title("System Maintenance")
                .message("Scheduled maintenance tonight")
                .priority(MobileAlert.AlertPriority.LOW)
                .read(true)
                .sentViaPush(false)
                .build();
        alert3.setTenantId(tenant.getId());
        alert3 = alertRepository.saveAndFlush(alert3);

        // Preference for LOW_STOCK with pushEnabled=true
        preference = AlertPreference.builder()
                .userId(user.getId())
                .alertType(MobileAlert.AlertType.LOW_STOCK)
                .pushEnabled(true)
                .inAppEnabled(true)
                .emailEnabled(false)
                .smsEnabled(false)
                .telegramEnabled(false)
                .build();
        preference.setTenantId(tenant.getId());
        preference = preferenceRepository.saveAndFlush(preference);

        entityManager.flush();
        entityManager.clear();
    }

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("MOBILE_ALERTS_VIEW"),
                    new SimpleGrantedAuthority("MOBILE_ALERTS_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("SOME_OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ==================== GET /api/v1/mobile/alerts ====================

    @Test
    void getAlerts_returnsPaginatedAlerts() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.page.totalElements").value(3));
    }

    @Test
    void getAlerts_forbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/alerts/unread ====================

    @Test
    void getUnreadAlerts_returnsOnlyUnread() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts/unread").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(2));
    }

    // ==================== GET /api/v1/mobile/alerts/type/{alertType} ====================

    @Test
    void getAlertsByType_lowStock_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts/type/LOW_STOCK").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].alertType").value("LOW_STOCK"))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    void getAlertsByType_nonExistingType_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts/type/CUSTOM").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page.totalElements").value(0));
    }

    // ==================== GET /api/v1/mobile/alerts/count ====================

    @Test
    void getUnreadCount_returns2() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts/count").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(2));
    }

    // ==================== PUT /api/v1/mobile/alerts/{id}/read ====================

    @Test
    void markAsRead_existingAlert_succeeds() throws Exception {
        mockMvc.perform(put("/api/v1/mobile/alerts/" + alert1.getId() + "/read").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Alert marked as read"));

        mockMvc.perform(get("/api/v1/mobile/alerts/count").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    void markAsRead_nonExistentAlert_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/mobile/alerts/999999/read").with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ==================== PUT /api/v1/mobile/alerts/read-all ====================

    @Test
    void markAllAsRead_setsAllRead() throws Exception {
        mockMvc.perform(put("/api/v1/mobile/alerts/read-all").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All alerts marked as read"));

        mockMvc.perform(get("/api/v1/mobile/alerts/count").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    // ==================== GET /api/v1/mobile/alerts/settings ====================

    @Test
    void getSettings_returnsList() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts/settings").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].alertType").value("LOW_STOCK"))
                .andExpect(jsonPath("$.data[0].pushEnabled").value(true));
    }

    // ==================== PUT /api/v1/mobile/alerts/settings/{alertType} ====================

    @Test
    void updateSettings_updatesPreference() throws Exception {
        AlertPreferenceDTO updateDto = AlertPreferenceDTO.builder()
                .pushEnabled(false)
                .inAppEnabled(true)
                .build();

        mockMvc.perform(put("/api/v1/mobile/alerts/settings/LOW_STOCK")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alertType").value("LOW_STOCK"))
                .andExpect(jsonPath("$.data.pushEnabled").value(false))
                .andExpect(jsonPath("$.data.inAppEnabled").value(true));
    }

    @Test
    void updateSettings_createsNewPreference() throws Exception {
        AlertPreferenceDTO newDto = AlertPreferenceDTO.builder()
                .pushEnabled(true)
                .build();

        mockMvc.perform(put("/api/v1/mobile/alerts/settings/PAYMENT_RECEIVED")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alertType").value("PAYMENT_RECEIVED"))
                .andExpect(jsonPath("$.data.pushEnabled").value(true));
    }
}
