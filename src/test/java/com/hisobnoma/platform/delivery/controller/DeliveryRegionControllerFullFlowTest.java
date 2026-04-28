package com.hisobnoma.platform.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.delivery.dto.DeliveryRegionDTO;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import com.hisobnoma.platform.delivery.repository.DeliveryRegionRepository;
import com.hisobnoma.platform.delivery.repository.DeliveryVillageRepository;
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
class DeliveryRegionControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DeliveryRegionRepository regionRepository;
    @Autowired private DeliveryVillageRepository villageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/delivery/regions";

    private Tenant tenant;
    private User user;
    private DeliveryRegion region1;
    private DeliveryRegion region2;
    private DeliveryRegion inactiveRegion;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("DeliveryRegion Test Tenant").code("FULLFLOW_DELREG").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("delregtestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        region1 = regionRepository.saveAndFlush(DeliveryRegion.builder()
                .name("North Region").code("NORTH").description("Northern delivery zone")
                .active(true).sortOrder(1).tenantId(tenant.getId()).build());

        region2 = regionRepository.saveAndFlush(DeliveryRegion.builder()
                .name("South Region").code("SOUTH").description("Southern delivery zone")
                .active(true).sortOrder(2).tenantId(tenant.getId()).build());

        inactiveRegion = regionRepository.saveAndFlush(DeliveryRegion.builder()
                .name("Old Region").code("OLD").description("Decommissioned zone")
                .active(false).sortOrder(99).tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("DELIVERY_REGION_READ"),
                        new SimpleGrantedAuthority("DELIVERY_REGION_CREATE"),
                        new SimpleGrantedAuthority("DELIVERY_REGION_UPDATE"),
                        new SimpleGrantedAuthority("DELIVERY_REGION_DELETE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("SOME_OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET / ----

    @Test
    void getAll_returnsPaginatedRegions() throws Exception {
        mockMvc.perform(get(BASE_URL).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(3)));
    }

    @Test
    void getAll_withSearch_filtersResults() throws Exception {
        mockMvc.perform(get(BASE_URL).param("search", "North").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("North Region"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    // ---- GET /active ----

    @Test
    void getActive_returnsActiveRegions() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data[?(@.code == 'NORTH')]").exists())
                .andExpect(jsonPath("$.data[?(@.code == 'SOUTH')]").exists())
                .andExpect(jsonPath("$.data[?(@.code == 'OLD')]").doesNotExist());
    }

    // ---- GET /{id} ----

    @Test
    void getById_returnsRegion() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + region1.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(region1.getId()))
                .andExpect(jsonPath("$.data.name").value("North Region"))
                .andExpect(jsonPath("$.data.code").value("NORTH"))
                .andExpect(jsonPath("$.data.description").value("Northern delivery zone"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.sortOrder").value(1))
                .andExpect(jsonPath("$.data.villageCount").value(0));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- POST / ----

    @Test
    void create_validRequest_returns201() throws Exception {
        DeliveryRegionDTO dto = DeliveryRegionDTO.builder()
                .name("East Region").code("EAST")
                .description("Eastern delivery zone")
                .active(true).sortOrder(3).build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("East Region"))
                .andExpect(jsonPath("$.data.code").value("EAST"))
                .andExpect(jsonPath("$.data.description").value("Eastern delivery zone"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.sortOrder").value(3));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        DeliveryRegionDTO dto = DeliveryRegionDTO.builder()
                .name("Duplicate North").code("NORTH")
                .description("Should conflict").active(true).sortOrder(5).build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    // ---- PUT /{id} ----

    @Test
    void update_validRequest_returnsUpdated() throws Exception {
        DeliveryRegionDTO dto = DeliveryRegionDTO.builder()
                .name("North Region Updated").code("NORTH")
                .description("Updated northern zone")
                .active(true).sortOrder(10).build();

        mockMvc.perform(put(BASE_URL + "/" + region1.getId()).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(region1.getId()))
                .andExpect(jsonPath("$.data.name").value("North Region Updated"))
                .andExpect(jsonPath("$.data.description").value("Updated northern zone"))
                .andExpect(jsonPath("$.data.sortOrder").value(10));
    }

    // ---- DELETE /{id} ----

    @Test
    void delete_returnsSuccess() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + region2.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Region deleted successfully"));

        // Verify GET returns 404
        mockMvc.perform(get(BASE_URL + "/" + region2.getId()).with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_regionWithVillages_fails() throws Exception {
        // Create a village attached to region1
        villageRepository.saveAndFlush(DeliveryVillage.builder()
                .name("Test Village").code("TV01")
                .active(true).sortOrder(1)
                .region(region1).tenantId(tenant.getId()).build());
        entityManager.clear();

        mockMvc.perform(delete(BASE_URL + "/" + region1.getId()).with(fullAuth()))
                .andExpect(status().isInternalServerError());
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + region1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        DeliveryRegionDTO dto = DeliveryRegionDTO.builder()
                .name("No Perm").code("NOPERM").build();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + region1.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + region1.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
