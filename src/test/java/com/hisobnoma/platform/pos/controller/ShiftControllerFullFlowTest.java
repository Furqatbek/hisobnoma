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
import com.hisobnoma.platform.pos.dto.CashOperationRequest;
import com.hisobnoma.platform.pos.dto.CloseShiftRequest;
import com.hisobnoma.platform.pos.dto.OpenShiftRequest;
import com.hisobnoma.platform.pos.entity.POSTerminal;
import com.hisobnoma.platform.pos.entity.Shift;
import com.hisobnoma.platform.pos.entity.ShiftStatus;
import com.hisobnoma.platform.pos.repository.POSTerminalRepository;
import com.hisobnoma.platform.pos.repository.ShiftRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
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
class ShiftControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private POSTerminalRepository terminalRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private static final String BASE_URL = "/api/v1/pos/shifts";

    private Tenant tenant;
    private User cashierUser;
    private Location location;
    private POSTerminal terminal;
    private POSTerminal terminal2;
    private Shift openShift;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Shift Test Tenant").code("FULLFLOW_SHIFT").active(true)
                .maxUsers(100).maxLocations(10).build());

        cashierUser = userRepository.saveAndFlush(User.builder()
                .username("shiftcashier")
                .passwordHash(passwordEncoder.encode("cashier123"))
                .firstName("Test").lastName("Cashier")
                .tenantId(tenant.getId()).enabled(true).build());

        location = locationRepository.saveAndFlush(Location.builder()
                .code("LOC-SHIFT-01").name("Shift Test Store")
                .locationType(LocationType.STORE).active(true)
                .tenantId(tenant.getId()).build());

        terminal = terminalRepository.saveAndFlush(POSTerminal.builder()
                .terminalCode("T-SHIFT-01").name("Shift Terminal 1")
                .location(location).active(true)
                .tenantId(tenant.getId()).build());

        terminal2 = terminalRepository.saveAndFlush(POSTerminal.builder()
                .terminalCode("T-SHIFT-02").name("Shift Terminal 2")
                .location(location).active(true)
                .tenantId(tenant.getId()).build());

        // Pre-existing OPEN shift on terminal for read/query tests
        openShift = shiftRepository.saveAndFlush(Shift.builder()
                .shiftNumber("SH-TEST-0001")
                .terminal(terminal)
                .cashierId(cashierUser.getId())
                .cashierName("Test Cashier")
                .status(ShiftStatus.OPEN)
                .openedAt(Instant.now())
                .openingCash(new BigDecimal("100000"))
                .tenantId(tenant.getId())
                .build());

        // Update terminal's current shift reference
        terminal.setCurrentShiftId(openShift.getId());
        terminalRepository.saveAndFlush(terminal);

        entityManager.clear();
    }

    // ========================= Auth helpers =========================

    private RequestPostProcessor allPermsAuth() {
        UserPrincipal principal = new UserPrincipal(
                cashierUser.getId(), cashierUser.getUsername(), "cashier123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("POS_SHIFT_READ"),
                        new SimpleGrantedAuthority("POS_SHIFT_OPEN"),
                        new SimpleGrantedAuthority("POS_SHIFT_CLOSE"),
                        new SimpleGrantedAuthority("POS_SHIFT_CASH_OPERATION")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor readOnlyAuth() {
        UserPrincipal principal = new UserPrincipal(
                cashierUser.getId(), cashierUser.getUsername(), "cashier123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("POS_SHIFT_READ")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ========================= GET /api/v1/pos/shifts =========================

    @Test
    void getAll_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ========================= GET /api/v1/pos/shifts/{id} =========================

    @Test
    void getById_returnsShift() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + openShift.getId()).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shiftNumber").value("SH-TEST-0001"))
                .andExpect(jsonPath("$.data.terminalId").value(terminal.getId().intValue()))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.openingCash").value(100000));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(allPermsAuth()))
                .andExpect(status().isNotFound());
    }

    // ========================= GET /api/v1/pos/shifts/terminal/{terminalId} =========================

    @Test
    void getByTerminal_returnsShifts() throws Exception {
        mockMvc.perform(get(BASE_URL + "/terminal/" + terminal.getId()).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].terminalId").value(terminal.getId().intValue()));
    }

    // ========================= GET /api/v1/pos/shifts/cashier/{cashierId} =========================

    @Test
    void getByCashier_returnsShifts() throws Exception {
        mockMvc.perform(get(BASE_URL + "/cashier/" + cashierUser.getId()).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].cashierId").value(cashierUser.getId().intValue()));
    }

    // ========================= GET /api/v1/pos/shifts/open =========================

    @Test
    void getOpenShifts_returnsList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/open").with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"));
    }

    // ========================= GET /api/v1/pos/shifts/current/terminal/{terminalId} =========================

    @Test
    void getCurrentShiftForTerminal_returnsShift() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current/terminal/" + terminal.getId()).with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.terminalId").value(terminal.getId().intValue()))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    // ========================= POST /api/v1/pos/shifts/open =========================

    @Test
    void openShift_validRequest_returnsCreated() throws Exception {
        // Use terminal2 since terminal already has an open shift.
        // Also need a different user since cashierUser already has an open shift.
        User anotherUser = userRepository.saveAndFlush(User.builder()
                .username("shiftcashier2")
                .passwordHash(passwordEncoder.encode("cashier123"))
                .firstName("Another").lastName("Cashier")
                .tenantId(tenant.getId()).enabled(true).build());
        entityManager.clear();

        UserPrincipal anotherPrincipal = new UserPrincipal(
                anotherUser.getId(), anotherUser.getUsername(), "cashier123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("POS_SHIFT_READ"),
                        new SimpleGrantedAuthority("POS_SHIFT_OPEN"),
                        new SimpleGrantedAuthority("POS_SHIFT_CLOSE"),
                        new SimpleGrantedAuthority("POS_SHIFT_CASH_OPERATION")));
        Authentication anotherAuth = new UsernamePasswordAuthenticationToken(
                anotherPrincipal, null, anotherPrincipal.getAuthorities());

        OpenShiftRequest request = OpenShiftRequest.builder()
                .terminalId(terminal2.getId())
                .openingCash(new BigDecimal("200000"))
                .notes("Opening shift for test")
                .build();

        mockMvc.perform(post(BASE_URL + "/open")
                        .with(authentication(anotherAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.terminalId").value(terminal2.getId().intValue()))
                .andExpect(jsonPath("$.data.openingCash").value(200000))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.cashierName").value("Another Cashier"))
                .andExpect(jsonPath("$.data.notes").value("Opening shift for test"));
    }

    // ========================= POST /api/v1/pos/shifts/{id}/close =========================

    @Test
    void closeShift_validRequest_returnsOk() throws Exception {
        CloseShiftRequest request = CloseShiftRequest.builder()
                .closingCash(new BigDecimal("95000"))
                .closingNotes("End of day close")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + openShift.getId() + "/close")
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closingCash").value(95000))
                .andExpect(jsonPath("$.data.closingNotes").value("End of day close"));
    }

    // ========================= POST /api/v1/pos/shifts/{id}/cash-operation =========================

    @Test
    void cashOperation_cashIn_recordsOperation() throws Exception {
        CashOperationRequest request = CashOperationRequest.builder()
                .operationType(CashOperationRequest.OperationType.CASH_IN)
                .amount(new BigDecimal("50000"))
                .reason("Petty cash replenishment")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + openShift.getId() + "/cash-operation")
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cashIn").value(50000))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void cashOperation_cashOut_recordsOperation() throws Exception {
        CashOperationRequest request = CashOperationRequest.builder()
                .operationType(CashOperationRequest.OperationType.CASH_OUT)
                .amount(new BigDecimal("30000"))
                .reason("Vendor payment")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + openShift.getId() + "/cash-operation")
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cashOut").value(30000))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    // ========================= GET /api/v1/pos/shifts/{id}/cash-operations =========================

    @Test
    void getCashOperations_returnsList() throws Exception {
        // First perform a cash-in so there's at least one operation
        CashOperationRequest request = CashOperationRequest.builder()
                .operationType(CashOperationRequest.OperationType.CASH_IN)
                .amount(new BigDecimal("25000"))
                .reason("Test cash in")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + openShift.getId() + "/cash-operation")
                        .with(allPermsAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Now fetch cash operations
        mockMvc.perform(get(BASE_URL + "/" + openShift.getId() + "/cash-operations")
                        .with(allPermsAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].operationType").value("CASH_IN"))
                .andExpect(jsonPath("$.data[0].amount").value(25000));
    }

    // ========================= Full lifecycle test =========================

    @Test
    void fullLifecycle_openCashOpCloseReconcile() throws Exception {
        // Use a different user and terminal to avoid conflicts with setUp shift
        User lifecycleUser = userRepository.saveAndFlush(User.builder()
                .username("lifecyclecashier")
                .passwordHash(passwordEncoder.encode("cashier123"))
                .firstName("Lifecycle").lastName("User")
                .tenantId(tenant.getId()).enabled(true).build());
        entityManager.clear();

        UserPrincipal lifecyclePrincipal = new UserPrincipal(
                lifecycleUser.getId(), lifecycleUser.getUsername(), "cashier123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("POS_SHIFT_READ"),
                        new SimpleGrantedAuthority("POS_SHIFT_OPEN"),
                        new SimpleGrantedAuthority("POS_SHIFT_CLOSE"),
                        new SimpleGrantedAuthority("POS_SHIFT_CASH_OPERATION")));
        Authentication lifecycleAuth = new UsernamePasswordAuthenticationToken(
                lifecyclePrincipal, null, lifecyclePrincipal.getAuthorities());
        RequestPostProcessor lifecycleAuthProcessor = authentication(lifecycleAuth);

        // 1. Open shift
        OpenShiftRequest openRequest = OpenShiftRequest.builder()
                .terminalId(terminal2.getId())
                .openingCash(new BigDecimal("150000"))
                .notes("Lifecycle test")
                .build();

        String openResponse = mockMvc.perform(post(BASE_URL + "/open")
                        .with(lifecycleAuthProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();

        Long shiftId = objectMapper.readTree(openResponse).path("data").path("id").asLong();

        // 2. Cash-in
        CashOperationRequest cashInRequest = CashOperationRequest.builder()
                .operationType(CashOperationRequest.OperationType.CASH_IN)
                .amount(new BigDecimal("20000"))
                .reason("Additional cash")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + shiftId + "/cash-operation")
                        .with(lifecycleAuthProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cashInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cashIn").value(20000));

        // 3. Close shift
        CloseShiftRequest closeRequest = CloseShiftRequest.builder()
                .closingCash(new BigDecimal("170000"))
                .closingNotes("End of lifecycle test")
                .build();

        mockMvc.perform(post(BASE_URL + "/" + shiftId + "/close")
                        .with(lifecycleAuthProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closingCash").value(170000));

        // 4. Reconcile shift
        mockMvc.perform(post(BASE_URL + "/" + shiftId + "/reconcile")
                        .with(lifecycleAuthProcessor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECONCILED"));
    }

    // ========================= Permission check =========================

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                cashierUser.getId(), cashierUser.getUsername(), "cashier123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERMISSION")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get(BASE_URL).with(authentication(auth)))
                .andExpect(status().isForbidden());
    }
}
