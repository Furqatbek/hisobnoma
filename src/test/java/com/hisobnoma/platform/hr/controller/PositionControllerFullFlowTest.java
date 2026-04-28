package com.hisobnoma.platform.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.hr.dto.PositionDto;
import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Position;
import com.hisobnoma.platform.hr.repository.DepartmentRepository;
import com.hisobnoma.platform.hr.repository.PositionRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PositionControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PositionRepository positionRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Department department;
    private Position position;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("HR Pos Test Tenant").code("HR_POS_TEST").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("hrposuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        department = departmentRepository.saveAndFlush(Department.builder()
                .code("DEP-POS-001").name("Engineering")
                .active(true).tenantId(tenant.getId()).build());

        position = positionRepository.saveAndFlush(Position.builder()
                .code("POS-001").name("Senior Developer")
                .description("Senior software developer")
                .department(department)
                .baseSalary(new BigDecimal("5000000"))
                .active(true).tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    private RequestPostProcessor readAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("HR_POSITION_READ"),
                        new SimpleGrantedAuthority("HR_POSITION_WRITE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/hr/positions ----

    @Test
    void getAll_returnsAllPositions() throws Exception {
        mockMvc.perform(get("/api/v1/hr/positions").with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("POS-001"));
    }

    // ---- GET /api/v1/hr/positions/{id} ----

    @Test
    void getById_returnsPosition() throws Exception {
        mockMvc.perform(get("/api/v1/hr/positions/{id}", position.getId()).with(readAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("POS-001"))
                .andExpect(jsonPath("$.name").value("Senior Developer"))
                .andExpect(jsonPath("$.departmentId").value(department.getId().intValue()))
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.baseSalary").value(5000000));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/hr/positions/{id}", 999999L).with(readAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/hr/positions ----

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        PositionDto dto = PositionDto.builder()
                .code("POS-002")
                .name("Junior Developer")
                .description("Junior software developer")
                .departmentId(department.getId())
                .baseSalary(new BigDecimal("3000000"))
                .active(true)
                .build();

        mockMvc.perform(post("/api/v1/hr/positions")
                        .with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("POS-002"))
                .andExpect(jsonPath("$.name").value("Junior Developer"))
                .andExpect(jsonPath("$.departmentId").value(department.getId().intValue()))
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.baseSalary").value(3000000));
    }

    @Test
    void create_duplicateCode_returnsConflictOrBadRequest() throws Exception {
        PositionDto dto = PositionDto.builder()
                .code("POS-001")
                .name("Duplicate Position")
                .active(true)
                .build();

        mockMvc.perform(post("/api/v1/hr/positions")
                        .with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/hr/positions/{id} ----

    @Test
    void update_validRequest_returnsUpdated() throws Exception {
        PositionDto dto = PositionDto.builder()
                .code("POS-001")
                .name("Lead Developer")
                .description("Lead software developer")
                .departmentId(department.getId())
                .baseSalary(new BigDecimal("7000000"))
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/hr/positions/{id}", position.getId())
                        .with(readAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lead Developer"))
                .andExpect(jsonPath("$.baseSalary").value(7000000));
    }

    // ---- DELETE /api/v1/hr/positions/{id} ----

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/hr/positions/{id}", position.getId())
                        .with(readAuth()))
                .andExpect(status().isNoContent());
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/hr/positions")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }
}
