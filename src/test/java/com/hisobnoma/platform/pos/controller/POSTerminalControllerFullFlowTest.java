package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import com.hisobnoma.platform.pos.dto.CreateTerminalRequest;
import com.hisobnoma.platform.pos.entity.POSTerminal;
import com.hisobnoma.platform.pos.repository.POSTerminalRepository;
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
class POSTerminalControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private POSTerminalRepository terminalRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/pos/terminals";

    private Tenant tenant;
    private User user;
    private Location location;
    private POSTerminal activeTerminal;
    private POSTerminal inactiveTerminal;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("POS Test Tenant").code("FULLFLOW_POS").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("postestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        location = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-001").name("Main Store").locationType(LocationType.STORE)
                .active(true).tenantId(tenant.getId()).build());

        activeTerminal = terminalRepository.saveAndFlush(POSTerminal.builder()
                .terminalCode("T-001").name("Terminal One").description("First terminal")
                .location(location).active(true)
                .ipAddress("192.168.1.10").macAddress("AA:BB:CC:DD:EE:01")
                .deviceType("Desktop").printerName("Printer-1")
                .cashDrawerEnabled(true).allowOffline(false)
                .tenantId(tenant.getId()).build());

        inactiveTerminal = terminalRepository.saveAndFlush(POSTerminal.builder()
                .terminalCode("T-002").name("Terminal Two").description("Second terminal")
                .location(location).active(false)
                .ipAddress("192.168.1.11").macAddress("AA:BB:CC:DD:EE:02")
                .deviceType("Tablet").printerName("Printer-2")
                .cashDrawerEnabled(false).allowOffline(true)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("POS_TERMINAL_READ"),
                        new SimpleGrantedAuthority("POS_TERMINAL_CREATE"),
                        new SimpleGrantedAuthority("POS_TERMINAL_UPDATE"),
                        new SimpleGrantedAuthority("POS_TERMINAL_DELETE")));
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
    void getAll_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void getAll_withSearch_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL).param("search", "T-001").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].terminalCode").value("T-001"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    // ---- GET /{id} ----

    @Test
    void getById_returnsTerminal() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + activeTerminal.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(activeTerminal.getId()))
                .andExpect(jsonPath("$.data.terminalCode").value("T-001"))
                .andExpect(jsonPath("$.data.name").value("Terminal One"))
                .andExpect(jsonPath("$.data.description").value("First terminal"))
                .andExpect(jsonPath("$.data.locationId").value(location.getId()))
                .andExpect(jsonPath("$.data.locationName").value("Main Store"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.ipAddress").value("192.168.1.10"))
                .andExpect(jsonPath("$.data.macAddress").value("AA:BB:CC:DD:EE:01"))
                .andExpect(jsonPath("$.data.deviceType").value("Desktop"))
                .andExpect(jsonPath("$.data.printerName").value("Printer-1"))
                .andExpect(jsonPath("$.data.cashDrawerEnabled").value(true))
                .andExpect(jsonPath("$.data.allowOffline").value(false));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /code/{code} ----

    @Test
    void getByCode_returnsTerminal() throws Exception {
        mockMvc.perform(get(BASE_URL + "/code/T-001").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.terminalCode").value("T-001"))
                .andExpect(jsonPath("$.data.name").value("Terminal One"))
                .andExpect(jsonPath("$.data.locationId").value(location.getId()));
    }

    // ---- GET /location/{locationId} ----

    @Test
    void getByLocation_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/location/" + location.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // ---- GET /active ----

    @Test
    void getActiveTerminals_returnsOnlyActive() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].terminalCode").value("T-001"))
                .andExpect(jsonPath("$.data[0].active").value(true));
    }

    // ---- POST / ----

    @Test
    void createTerminal_validRequest_returnsCreated() throws Exception {
        CreateTerminalRequest request = CreateTerminalRequest.builder()
                .terminalCode("T-003").name("Terminal Three")
                .description("Third terminal").locationId(location.getId())
                .ipAddress("192.168.1.12").macAddress("AA:BB:CC:DD:EE:03")
                .deviceType("Mobile").printerName("Printer-3")
                .cashDrawerEnabled(true).allowOffline(false).build();

        mockMvc.perform(post(BASE_URL).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.terminalCode").value("T-003"))
                .andExpect(jsonPath("$.data.name").value("Terminal Three"))
                .andExpect(jsonPath("$.data.description").value("Third terminal"))
                .andExpect(jsonPath("$.data.locationId").value(location.getId()))
                .andExpect(jsonPath("$.data.locationName").value("Main Store"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.ipAddress").value("192.168.1.12"))
                .andExpect(jsonPath("$.data.macAddress").value("AA:BB:CC:DD:EE:03"))
                .andExpect(jsonPath("$.data.deviceType").value("Mobile"))
                .andExpect(jsonPath("$.data.printerName").value("Printer-3"))
                .andExpect(jsonPath("$.data.cashDrawerEnabled").value(true))
                .andExpect(jsonPath("$.data.allowOffline").value(false));
    }

    // ---- PUT /{id} ----

    @Test
    void updateTerminal_validRequest_returnsUpdated() throws Exception {
        CreateTerminalRequest request = CreateTerminalRequest.builder()
                .terminalCode("T-001-UPD").name("Updated Terminal One")
                .description("Updated description").locationId(location.getId())
                .ipAddress("10.0.0.1").macAddress("FF:FF:FF:FF:FF:01")
                .deviceType("Kiosk").printerName("Printer-Updated")
                .cashDrawerEnabled(false).allowOffline(true).build();

        mockMvc.perform(put(BASE_URL + "/" + activeTerminal.getId()).with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(activeTerminal.getId()))
                .andExpect(jsonPath("$.data.terminalCode").value("T-001-UPD"))
                .andExpect(jsonPath("$.data.name").value("Updated Terminal One"))
                .andExpect(jsonPath("$.data.description").value("Updated description"))
                .andExpect(jsonPath("$.data.ipAddress").value("10.0.0.1"))
                .andExpect(jsonPath("$.data.macAddress").value("FF:FF:FF:FF:FF:01"))
                .andExpect(jsonPath("$.data.deviceType").value("Kiosk"))
                .andExpect(jsonPath("$.data.printerName").value("Printer-Updated"))
                .andExpect(jsonPath("$.data.cashDrawerEnabled").value(false))
                .andExpect(jsonPath("$.data.allowOffline").value(true));
    }

    // ---- PUT /{id}/activate ----

    @Test
    void activateTerminal_returns200() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + inactiveTerminal.getId() + "/activate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Terminal activated"));

        // Verify terminal is now active
        mockMvc.perform(get(BASE_URL + "/" + inactiveTerminal.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    // ---- PUT /{id}/deactivate ----

    @Test
    void deactivateTerminal_returns200() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + activeTerminal.getId() + "/deactivate").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Terminal deactivated"));

        // Verify terminal is now inactive
        mockMvc.perform(get(BASE_URL + "/" + activeTerminal.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    // ---- DELETE /{id} ----

    @Test
    void deleteTerminal_returns200() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + activeTerminal.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Terminal deleted"));

        // Verify terminal no longer exists
        mockMvc.perform(get(BASE_URL + "/" + activeTerminal.getId()).with(fullAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- Permission check ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        // GET /
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /{id}
        mockMvc.perform(get(BASE_URL + "/" + activeTerminal.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /code/{code}
        mockMvc.perform(get(BASE_URL + "/code/T-001").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /location/{locationId}
        mockMvc.perform(get(BASE_URL + "/location/" + location.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        // GET /active
        mockMvc.perform(get(BASE_URL + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // POST /
        CreateTerminalRequest createReq = CreateTerminalRequest.builder()
                .terminalCode("NOPE").name("No Permission")
                .locationId(location.getId()).build();

        mockMvc.perform(post(BASE_URL).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // PUT /{id}
        mockMvc.perform(put(BASE_URL + "/" + activeTerminal.getId()).with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // PUT /{id}/activate
        mockMvc.perform(put(BASE_URL + "/" + activeTerminal.getId() + "/activate").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // PUT /{id}/deactivate
        mockMvc.perform(put(BASE_URL + "/" + activeTerminal.getId() + "/deactivate").with(noPermAuth()))
                .andExpect(status().isForbidden());

        // DELETE /{id}
        mockMvc.perform(delete(BASE_URL + "/" + activeTerminal.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
