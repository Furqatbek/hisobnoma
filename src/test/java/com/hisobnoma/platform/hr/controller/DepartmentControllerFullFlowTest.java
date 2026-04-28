package com.hisobnoma.platform.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.hr.dto.DepartmentDto;
import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.repository.DepartmentRepository;
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
class DepartmentControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/hr/departments";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Department department;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("HR Dept Test Tenant").code("HR_DEPT_TEST").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("hrdeptuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        department = departmentRepository.saveAndFlush(Department.builder()
                .code("DEP-001").name("Engineering")
                .description("Engineering department")
                .active(true)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    private RequestPostProcessor readWriteAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("HR_DEPARTMENT_READ"),
                        new SimpleGrantedAuthority("HR_DEPARTMENT_WRITE")));
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

    // ---- GET /api/v1/hr/departments ----

    @Test
    void getAll_returnsAllDepartments() throws Exception {
        Department dept2 = departmentRepository.saveAndFlush(Department.builder()
                .code("DEP-002").name("Marketing")
                .description("Marketing department")
                .active(true)
                .tenantId(tenant.getId()).build());
        entityManager.clear();

        mockMvc.perform(get(BASE_URL).with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[*].code", hasItems("DEP-001", "DEP-002")));
    }

    // ---- GET /api/v1/hr/departments/{id} ----

    @Test
    void getById_returnsDepartment() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + department.getId()).with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DEP-001"))
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.description").value("Engineering department"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(readWriteAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/hr/departments ----

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        DepartmentDto dto = DepartmentDto.builder()
                .code("DEP-NEW")
                .name("New Department")
                .description("A newly created department")
                .active(true)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(readWriteAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DEP-NEW"))
                .andExpect(jsonPath("$.name").value("New Department"));
    }

    @Test
    void create_duplicateCode_returnsConflictOrBadRequest() throws Exception {
        DepartmentDto dto = DepartmentDto.builder()
                .code("DEP-001")
                .name("Duplicate Department")
                .description("Should fail")
                .active(true)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(readWriteAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().is(anyOf(equalTo(400), equalTo(409))));
    }

    // ---- PUT /api/v1/hr/departments/{id} ----

    @Test
    void update_validRequest_returnsUpdated() throws Exception {
        DepartmentDto dto = DepartmentDto.builder()
                .code("DEP-001")
                .name("Engineering Updated")
                .description("Updated description")
                .active(true)
                .build();

        mockMvc.perform(put(BASE_URL + "/" + department.getId())
                        .with(readWriteAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Engineering Updated"));
    }

    // ---- DELETE /api/v1/hr/departments/{id} ----

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + department.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + department.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/" + department.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        DepartmentDto dto = DepartmentDto.builder()
                .code("DEP-NOPERM").name("No Perm").active(true).build();

        mockMvc.perform(post(BASE_URL)
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/" + department.getId())
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + department.getId())
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
