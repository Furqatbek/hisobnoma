package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.dto.CreateLocationRequest;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
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
class LocationControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LocationRepository locationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/inventory/locations";

    private Tenant tenant;
    private User user;
    private Location warehouse;
    private Location zone;
    private Location store;
    private Location inactiveLocation;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Location Test Tenant").code("FULLFLOW_LOC").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("loctestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        warehouse = locationRepository.saveAndFlush(Location.builder()
                .code("WH-001").name("Main Warehouse").description("Primary warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true).isDefault(true).sortOrder(1)
                .tenantId(tenant.getId()).build());

        zone = locationRepository.saveAndFlush(Location.builder()
                .code("ZONE-001").name("Zone A").description("Zone A inside warehouse")
                .locationType(LocationType.ZONE)
                .parentLocation(warehouse)
                .active(true).sortOrder(2)
                .tenantId(tenant.getId()).build());

        store = locationRepository.saveAndFlush(Location.builder()
                .code("STORE-001").name("Retail Store").description("Main retail store")
                .locationType(LocationType.STORE)
                .active(true).sortOrder(3)
                .tenantId(tenant.getId()).build());

        inactiveLocation = locationRepository.saveAndFlush(Location.builder()
                .code("INACTIVE-001").name("Old Virtual").description("Decommissioned")
                .locationType(LocationType.VIRTUAL)
                .active(false).sortOrder(4)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor viewAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("INVENTORY_STOCK_VIEW"),
                        new SimpleGrantedAuthority("INVENTORY_LOCATION_MANAGE")));
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
    void getAllLocations_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$[?(@.code == 'WH-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'ZONE-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'STORE-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'INACTIVE-001')]").exists());
    }

    // ---- GET /paginated ----

    @Test
    void getLocationsPaginated_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/paginated").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(4)));
    }

    // ---- GET /active ----

    @Test
    void getActiveLocations_returnsOnlyActive() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.code == 'WH-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'ZONE-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'STORE-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'INACTIVE-001')]").doesNotExist());
    }

    // ---- GET /tree ----

    @Test
    void getLocationTree_returnsHierarchy() throws Exception {
        mockMvc.perform(get(BASE_URL + "/tree").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                // Tree returns only root locations with children nested
                .andExpect(jsonPath("$[?(@.code == 'WH-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'WH-001')].children").isNotEmpty())
                .andExpect(jsonPath("$[?(@.code == 'WH-001')].children[0].code").value("ZONE-001"))
                // Zone should NOT appear as a root-level element
                .andExpect(jsonPath("$[?(@.code == 'ZONE-001')]").doesNotExist());
    }

    // ---- GET /roots ----

    @Test
    void getRootLocations_returnsOnlyRoots() throws Exception {
        mockMvc.perform(get(BASE_URL + "/roots").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.code == 'WH-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'STORE-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'INACTIVE-001')]").exists())
                // Zone has a parent, so it should NOT be a root
                .andExpect(jsonPath("$[?(@.code == 'ZONE-001')]").doesNotExist());
    }

    // ---- GET /type/{type} ----

    @Test
    void getLocationsByType_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/type/WAREHOUSE").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.code == 'WH-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'STORE-001')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.code == 'ZONE-001')]").doesNotExist());
    }

    // ---- GET /warehouse-store ----

    @Test
    void getWarehouseAndStoreLocations_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/warehouse-store").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.code == 'WH-001')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'STORE-001')]").exists())
                // Zone and inactive virtual should not appear (not WAREHOUSE/STORE or not active)
                .andExpect(jsonPath("$[?(@.code == 'ZONE-001')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.code == 'INACTIVE-001')]").doesNotExist());
    }

    // ---- GET /default ----

    @Test
    void getDefaultLocation_returnsDefault() throws Exception {
        mockMvc.perform(get(BASE_URL + "/default").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WH-001"))
                .andExpect(jsonPath("$.default").value(true));
    }

    // ---- GET /{id} ----

    @Test
    void getLocationById_returnsLocation() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + warehouse.getId()).with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(warehouse.getId()))
                .andExpect(jsonPath("$.code").value("WH-001"))
                .andExpect(jsonPath("$.name").value("Main Warehouse"))
                .andExpect(jsonPath("$.locationType").value("WAREHOUSE"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.default").value(true))
                .andExpect(jsonPath("$.level").value(0))
                .andExpect(jsonPath("$.fullPath").value("Main Warehouse"));
    }

    @Test
    void getLocationById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(viewAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /code/{code} ----

    @Test
    void getLocationByCode_returnsLocation() throws Exception {
        mockMvc.perform(get(BASE_URL + "/code/ZONE-001").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ZONE-001"))
                .andExpect(jsonPath("$.name").value("Zone A"))
                .andExpect(jsonPath("$.locationType").value("ZONE"))
                .andExpect(jsonPath("$.parentLocationId").value(warehouse.getId()))
                .andExpect(jsonPath("$.parentLocationName").value("Main Warehouse"))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.fullPath").value("Main Warehouse > Zone A"));
    }

    // ---- GET /{id}/children ----

    @Test
    void getChildLocations_returnsChildren() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + warehouse.getId() + "/children").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("ZONE-001"));
    }

    // ---- GET /search ----

    @Test
    void searchLocations_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("q", "Warehouse").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].code").value("WH-001"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- POST / ----

    @Test
    void createLocation_validRequest_returnsCreated() throws Exception {
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("BIN-001").name("Storage Bin 1")
                .description("A storage bin")
                .locationType(LocationType.BIN)
                .active(true)
                .sortOrder(10)
                .build();

        mockMvc.perform(post(BASE_URL).with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BIN-001"))
                .andExpect(jsonPath("$.name").value("Storage Bin 1"))
                .andExpect(jsonPath("$.description").value("A storage bin"))
                .andExpect(jsonPath("$.locationType").value("BIN"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.sortOrder").value(10));
    }

    @Test
    void createLocation_duplicateCode_returnsBadRequest() throws Exception {
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("WH-001").name("Duplicate Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true).build();

        mockMvc.perform(post(BASE_URL).with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLocation_withParent_setsHierarchy() throws Exception {
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("BIN-002").name("Bin in Zone A")
                .locationType(LocationType.BIN)
                .parentLocationId(zone.getId())
                .active(true).build();

        mockMvc.perform(post(BASE_URL).with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BIN-002"))
                .andExpect(jsonPath("$.parentLocationId").value(zone.getId()))
                .andExpect(jsonPath("$.parentLocationName").value("Zone A"))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.fullPath").value("Main Warehouse > Zone A > Bin in Zone A"));
    }

    // ---- PUT /{id} ----

    @Test
    void updateLocation_validRequest_returnsUpdated() throws Exception {
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("STORE-001").name("Updated Retail Store")
                .description("Updated description")
                .locationType(LocationType.STORE)
                .city("Tashkent").country("Uzbekistan")
                .active(true).build();

        mockMvc.perform(put(BASE_URL + "/" + store.getId()).with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(store.getId()))
                .andExpect(jsonPath("$.code").value("STORE-001"))
                .andExpect(jsonPath("$.name").value("Updated Retail Store"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.city").value("Tashkent"))
                .andExpect(jsonPath("$.country").value("Uzbekistan"));
    }

    // ---- DELETE /{id} ----

    @Test
    void deleteLocation_noChildren_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + inactiveLocation.getId()).with(viewAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteLocation_withChildren_returnsBadRequest() throws Exception {
        // Warehouse has zone as a child, so deletion should fail
        mockMvc.perform(delete(BASE_URL + "/" + warehouse.getId()).with(viewAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/paginated").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/tree").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/roots").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/type/WAREHOUSE").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/warehouse-store").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/default").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + warehouse.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/code/WH-001").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + warehouse.getId() + "/children").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/search").param("q", "WH").with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("NOPE").name("No Permission").locationType(LocationType.WAREHOUSE).build();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + warehouse.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + warehouse.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
