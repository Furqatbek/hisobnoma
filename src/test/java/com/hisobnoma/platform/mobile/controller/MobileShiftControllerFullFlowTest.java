package com.hisobnoma.platform.mobile.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import com.hisobnoma.platform.pos.dto.CashOperationRequest;
import com.hisobnoma.platform.pos.dto.CloseShiftRequest;
import com.hisobnoma.platform.pos.dto.OpenShiftRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MobileShiftControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/mobile/shifts";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @Autowired private POSTerminalRepository posTerminalRepository;
    @Autowired private LocationRepository locationRepository;

    private Tenant tenant;
    private User user;
    private Location location;
    private POSTerminal terminal;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Mobile Shift Tenant").code("FULLFLOW_MOBSHIFT").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("mobshiftuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        location = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-SHIFT-001").name("Shift Store")
                .locationType(LocationType.STORE).active(true)
                .allowNegativeStock(true)
                .tenantId(tenant.getId()).build());

        terminal = posTerminalRepository.saveAndFlush(POSTerminal.builder()
                .terminalCode("T-SHIFT-001").name("Shift Terminal")
                .location(location).active(true)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    // ---- Auth helpers ----

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("POS_SHIFT_READ"),
                    new SimpleGrantedAuthority("POS_SHIFT_OPEN"),
                    new SimpleGrantedAuthority("POS_SHIFT_CLOSE"),
                    new SimpleGrantedAuthority("POS_SHIFT_CASH_OPERATION"),
                    new SimpleGrantedAuthority("MOBILE_SYNC_ACCESS")));
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

    // ---- Helper: open a shift via API and return the shift id ----

    private Long openShiftViaApi() throws Exception {
        OpenShiftRequest request = OpenShiftRequest.builder()
                .terminalId(terminal.getId())
                .openingCash(new BigDecimal("1000"))
                .notes("Test shift")
                .build();

        MvcResult result = mockMvc.perform(post(BASE_URL + "/open")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    // ==================== GET /current ====================

    @Test
    void getCurrentShift_noOpenShift_returnsNull() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ==================== GET /open ====================

    @Test
    void getOpenShifts_noShifts_returnsEmptyList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/open").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ==================== POST /open ====================

    @Test
    void openShift_validRequest_returns201() throws Exception {
        OpenShiftRequest request = OpenShiftRequest.builder()
                .terminalId(terminal.getId())
                .openingCash(new BigDecimal("1000"))
                .notes("Opening shift")
                .build();

        mockMvc.perform(post(BASE_URL + "/open")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.openingCash").value(1000))
                .andExpect(jsonPath("$.data.terminalId").value(terminal.getId()))
                .andExpect(jsonPath("$.data.cashierId").value(user.getId()));
    }

    // ==================== Open then GET /current ====================

    @Test
    void openShift_thenGetCurrent_returnsShift() throws Exception {
        Long shiftId = openShiftViaApi();

        mockMvc.perform(get(BASE_URL + "/current").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(shiftId))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    // ==================== Open then GET /open ====================

    @Test
    void openShift_thenGetOpen_includesShift() throws Exception {
        Long shiftId = openShiftViaApi();

        mockMvc.perform(get(BASE_URL + "/open").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(shiftId))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"));
    }

    // ==================== POST /{id}/close ====================

    @Test
    void closeShift_openShift_closesSuccessfully() throws Exception {
        Long shiftId = openShiftViaApi();

        CloseShiftRequest closeRequest = CloseShiftRequest.builder()
                .closingCash(new BigDecimal("1500"))
                .closingNotes("End of day")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + shiftId + "/close")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(shiftId))
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closingCash").value(1500));
    }

    // ==================== POST /{id}/cash-operation (CASH_IN) ====================

    @Test
    void cashOperation_cashIn_updatesShift() throws Exception {
        Long shiftId = openShiftViaApi();

        CashOperationRequest cashInRequest = CashOperationRequest.builder()
                .operationType(CashOperationRequest.OperationType.CASH_IN)
                .amount(new BigDecimal("200"))
                .reason("Petty cash deposit")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + shiftId + "/cash-operation")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cashInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cash in recorded"))
                .andExpect(jsonPath("$.data.id").value(shiftId));
    }

    // ==================== POST /{id}/cash-operation (CASH_OUT) ====================

    @Test
    void cashOperation_cashOut_updatesShift() throws Exception {
        Long shiftId = openShiftViaApi();

        CashOperationRequest cashOutRequest = CashOperationRequest.builder()
                .operationType(CashOperationRequest.OperationType.CASH_OUT)
                .amount(new BigDecimal("100"))
                .reason("Change withdrawal")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + shiftId + "/cash-operation")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cashOutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cash out recorded"))
                .andExpect(jsonPath("$.data.id").value(shiftId));
    }

    // ==================== Forbidden ====================

    @Test
    void openShift_forbidden_returns403() throws Exception {
        OpenShiftRequest request = OpenShiftRequest.builder()
                .terminalId(terminal.getId())
                .openingCash(new BigDecimal("1000"))
                .build();

        mockMvc.perform(post(BASE_URL + "/open")
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
