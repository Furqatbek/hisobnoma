package com.hisobnoma.platform.distribution.controller;

import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack check of the staff distribution API's security + serialization, using the agent
 * controller as the representative (all staff distribution controllers share the same
 * {@code @PreAuthorize} pattern): MANAGE can create, VIEW-only is forbidden.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DistributionAgentControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Dist Tenant").code("DIST_FULLFLOW").active(true)
                .maxUsers(50).maxLocations(10).build());
    }

    private RequestPostProcessor auth(String... authorities) {
        UserPrincipal principal = new UserPrincipal(
                1L, "staff", "x", tenant.getId(), true, true,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).map(a -> (org.springframework.security.core.GrantedAuthority) a).toList());
        Authentication a = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(a);
    }

    private static final String NEW_AGENT = "{\"code\":\"AG-1\",\"name\":\"Alisher\"}";

    @Test
    void createAgent_withManage_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/distribution/agents")
                        .with(auth("DISTRIBUTION_AGENT_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_AGENT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("AG-1"))
                .andExpect(jsonPath("$.name").value("Alisher"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createAgent_withViewOnly_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/distribution/agents")
                        .with(auth("DISTRIBUTION_AGENT_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_AGENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAgents_withView_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/distribution/agents").with(auth("DISTRIBUTION_AGENT_VIEW")))
                .andExpect(status().isOk());
    }

    @Test
    void listAgents_unauthenticated_isDenied() throws Exception {
        // No authentication -> access denied (this app answers protected staff routes with 403).
        mockMvc.perform(get("/api/v1/distribution/agents"))
                .andExpect(status().isForbidden());
    }
}
