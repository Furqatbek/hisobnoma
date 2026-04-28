package com.hisobnoma.platform.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.delivery.dto.DeliveryVillageDTO;
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
class DeliveryVillageControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DeliveryRegionRepository regionRepository;
    @Autowired private DeliveryVillageRepository villageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/delivery/villages";

    private Tenant tenant;
    private User user;
    private DeliveryRegion region;
    private DeliveryRegion region2;
    private DeliveryVillage village;
    private DeliveryVillage village2;
    private DeliveryVillage inactiveVillage;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Delivery Village Test Tenant").code("FULLFLOW_DELVIL").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("delviltestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        region = regionRepository.saveAndFlush(DeliveryRegion.builder()
                .name("Tashkent Region").code("TASH-REG")
                .description("Tashkent delivery region")
                .active(true).sortOrder(1)
                .tenantId(tenant.getId()).build());

        region2 = regionRepository.saveAndFlush(DeliveryRegion.builder()
                .name("Samarkand Region").code("SAM-REG")
                .description("Samarkand delivery region")
                .active(true).sortOrder(2)
                .tenantId(tenant.getId()).build());

        village = villageRepository.saveAndFlush(DeliveryVillage.builder()
                .name("Chirchiq").code("CHR-001")
                .description("Chirchiq village")
                .region(region)
                .active(true).sortOrder(1)
                .tenantId(tenant.getId()).build());

        village2 = villageRepository.saveAndFlush(DeliveryVillage.builder()
                .name("Olmaliq").code("OLM-001")
                .description("Olmaliq village")
                .region(region2)
                .active(true).sortOrder(2)
                .tenantId(tenant.getId()).build());

        inactiveVillage = villageRepository.saveAndFlush(DeliveryVillage.builder()
                .name("Inactive Village").code("INACT-001")
                .description("Inactive village")
                .region(region)
                .active(false).sortOrder(3)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor readAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("DELIVERY_VILLAGE_READ"),
                        new SimpleGrantedAuthority("DELIVERY_VILLAGE_CREATE"),
                        new SimpleGrantedAuthority("DELIVERY_VILLAGE_UPDATE"),
                        new SimpleGrantedAuthority("DELIVERY_VILLAGE_DELETE")));
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
    void getAll_returnsPaginatedVillages() throws Exception {
        mockMvc.perform(get(BASE_URL).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.content[?(@.code == 'CHR-001')]").exists())
                .andExpect(jsonPath("$.content[?(@.code == 'OLM-001')]").exists())
                .andExpect(jsonPath("$.content[?(@.code == 'INACT-001')]").exists());
    }

    // ---- GET /?search= ----

    @Test
    void getAll_withSearch_filtersResults() throws Exception {
        mockMvc.perform(get(BASE_URL).param("search", "Chirchiq").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Chirchiq"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    // ---- GET /active ----

    @Test
    void getActive_returnsActiveVillages() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data[?(@.code == 'CHR-001')]").exists())
                .andExpect(jsonPath("$.data[?(@.code == 'OLM-001')]").exists())
                .andExpect(jsonPath("$.data[?(@.code == 'INACT-001')]").doesNotExist());
    }

    // ---- GET /region/{regionId} ----

    @Test
    void getByRegion_returnsVillagesInRegion() throws Exception {
        mockMvc.perform(get(BASE_URL + "/region/" + region.getId()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[?(@.code == 'CHR-001')]").exists())
                // inactiveVillage belongs to region but is not active, so excluded
                .andExpect(jsonPath("$.data[?(@.code == 'INACT-001')]").doesNotExist())
                // village2 belongs to region2, so excluded
                .andExpect(jsonPath("$.data[?(@.code == 'OLM-001')]").doesNotExist());
    }

    // ---- GET /{id} ----

    @Test
    void getById_returnsVillage() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + village.getId()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(village.getId()))
                .andExpect(jsonPath("$.data.name").value("Chirchiq"))
                .andExpect(jsonPath("$.data.code").value("CHR-001"))
                .andExpect(jsonPath("$.data.description").value("Chirchiq village"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.sortOrder").value(1))
                .andExpect(jsonPath("$.data.regionId").value(region.getId()))
                .andExpect(jsonPath("$.data.regionName").value("Tashkent Region"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(readAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- POST / ----

    @Test
    void create_validRequest_returns201() throws Exception {
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder()
                .name("Angren")
                .code("ANG-001")
                .description("Angren village")
                .regionId(region.getId())
                .active(true)
                .sortOrder(5)
                .build();

        mockMvc.perform(post(BASE_URL).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Angren"))
                .andExpect(jsonPath("$.data.code").value("ANG-001"))
                .andExpect(jsonPath("$.data.description").value("Angren village"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.sortOrder").value(5))
                .andExpect(jsonPath("$.data.regionId").value(region.getId()))
                .andExpect(jsonPath("$.data.regionName").value("Tashkent Region"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder()
                .name("Duplicate Village")
                .code("CHR-001")
                .description("Should fail - duplicate code")
                .regionId(region.getId())
                .active(true)
                .build();

        mockMvc.perform(post(BASE_URL).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    // ---- PUT /{id} ----

    @Test
    void update_validRequest_returnsUpdated() throws Exception {
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder()
                .name("Chirchiq Updated")
                .code("CHR-001")
                .description("Updated description")
                .regionId(region.getId())
                .active(true)
                .sortOrder(10)
                .build();

        mockMvc.perform(put(BASE_URL + "/" + village.getId()).with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(village.getId()))
                .andExpect(jsonPath("$.data.name").value("Chirchiq Updated"))
                .andExpect(jsonPath("$.data.code").value("CHR-001"))
                .andExpect(jsonPath("$.data.description").value("Updated description"))
                .andExpect(jsonPath("$.data.sortOrder").value(10))
                .andExpect(jsonPath("$.data.regionId").value(region.getId()))
                .andExpect(jsonPath("$.data.regionName").value("Tashkent Region"));
    }

    // ---- DELETE /{id} ----

    @Test
    void delete_returnsSuccess() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + village.getId()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Village deleted successfully"));

        // Verify GET returns 404 after deletion
        mockMvc.perform(get(BASE_URL + "/" + village.getId()).with(readAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        // GET /
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /active
        mockMvc.perform(get(BASE_URL + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /region/{regionId}
        mockMvc.perform(get(BASE_URL + "/region/" + region.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /{id}
        mockMvc.perform(get(BASE_URL + "/" + village.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /
        DeliveryVillageDTO createDto = DeliveryVillageDTO.builder()
                .name("No Perm Village").code("NOPERM-001")
                .regionId(region.getId()).active(true).build();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());

        // PUT /{id}
        mockMvc.perform(put(BASE_URL + "/" + village.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());

        // DELETE /{id}
        mockMvc.perform(delete(BASE_URL + "/" + village.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
