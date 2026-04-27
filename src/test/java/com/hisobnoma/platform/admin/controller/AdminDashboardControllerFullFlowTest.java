package com.hisobnoma.platform.admin.controller;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
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

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDashboardControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User adminUser;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Dashboard Tenant").code("FULLFLOW_DASH").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("dashboardadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());
    }

    private RequestPostProcessor dashAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("ADMIN_DASHBOARD_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/admin/dashboard/stats ----

    @Test
    void getDashboardStats_returnsStatsStructure() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/stats").with(dashAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.activeUsers").isNumber())
                .andExpect(jsonPath("$.data.totalProducts").isNumber())
                .andExpect(jsonPath("$.data.totalSalesToday").isNumber())
                .andExpect(jsonPath("$.data.totalReceivables").isNumber())
                .andExpect(jsonPath("$.data.totalPayables").isNumber());
    }

    @Test
    void getDashboardStats_withUsers_countsCorrectly() throws Exception {
        userRepository.saveAndFlush(User.builder()
                .username("dashuser1")
                .passwordHash(passwordEncoder.encode("pass"))
                .tenantId(tenant.getId()).enabled(true).build());

        userRepository.saveAndFlush(User.builder()
                .username("dashuser2")
                .passwordHash(passwordEncoder.encode("pass"))
                .tenantId(tenant.getId()).enabled(false).build());

        mockMvc.perform(get("/api/v1/admin/dashboard/stats").with(dashAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(3))
                .andExpect(jsonPath("$.data.activeUsers").value(2));
    }

    @Test
    void getDashboardStats_noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboardStats_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/stats"))
                .andExpect(status().isForbidden());
    }
}
