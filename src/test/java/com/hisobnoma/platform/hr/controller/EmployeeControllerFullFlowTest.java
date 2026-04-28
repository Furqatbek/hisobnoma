package com.hisobnoma.platform.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.hr.dto.CreateEmployeeRequest;
import com.hisobnoma.platform.hr.entity.*;
import com.hisobnoma.platform.hr.repository.*;
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
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/hr/employees";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;
    private Department department;
    private Position position;
    private Employee employee;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("HR Emp Test Tenant").code("HR_EMP_TEST").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("hrempuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        department = departmentRepository.saveAndFlush(Department.builder()
                .code("DEP-EMP-001").name("Engineering")
                .active(true).tenantId(tenant.getId()).build());

        position = positionRepository.saveAndFlush(Position.builder()
                .code("POS-EMP-001").name("Developer")
                .department(department)
                .baseSalary(new BigDecimal("5000000"))
                .active(true).tenantId(tenant.getId()).build());

        employee = employeeRepository.saveAndFlush(Employee.builder()
                .employeeCode("EMP-001")
                .firstName("John").lastName("Doe").middleName("M")
                .phone("+998901234567").email("john@test.com")
                .hireDate(LocalDate.of(2024, 1, 15))
                .department(department).position(position)
                .currentSalary(new BigDecimal("5000000"))
                .status(Employee.EmployeeStatus.ACTIVE).active(true)
                .tenantId(tenant.getId()).build());

        entityManager.clear();
    }

    private RequestPostProcessor readWriteAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("HR_EMPLOYEE_READ"),
                        new SimpleGrantedAuthority("HR_EMPLOYEE_WRITE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/hr/employees (paginated) ----

    @Test
    void getAll_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].employeeCode").value("EMP-001"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- GET /api/v1/hr/employees/active ----

    @Test
    void getActive_returnsActiveEmployees() throws Exception {
        mockMvc.perform(get(BASE_URL + "/active").with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[*].status", everyItem(equalTo("ACTIVE"))));
    }

    // ---- GET /api/v1/hr/employees/{id} ----

    @Test
    void getById_returnsEmployee() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", employee.getId()).with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employee.getId().intValue()))
                .andExpect(jsonPath("$.employeeCode").value("EMP-001"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.middleName").value("M"))
                .andExpect(jsonPath("$.fullName").value("Doe John M"))
                .andExpect(jsonPath("$.phone").value("+998901234567"))
                .andExpect(jsonPath("$.email").value("john@test.com"))
                .andExpect(jsonPath("$.hireDate").value("2024-01-15"))
                .andExpect(jsonPath("$.departmentId").value(department.getId().intValue()))
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.positionId").value(position.getId().intValue()))
                .andExpect(jsonPath("$.positionName").value("Developer"))
                .andExpect(jsonPath("$.currentSalary").value(5000000))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 999999L).with(readWriteAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/hr/employees/search ----

    @Test
    void search_byName_returnsResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search")
                        .param("q", "John")
                        .with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("John"))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ---- POST /api/v1/hr/employees ----

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeCode("EMP-002");
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setMiddleName("A");
        request.setPhone("+998901234568");
        request.setEmail("jane@test.com");
        request.setDateOfBirth(LocalDate.of(1990, 5, 20));
        request.setGender("FEMALE");
        request.setAddress("123 Test Street");
        request.setHireDate(LocalDate.of(2024, 3, 1));
        request.setDepartmentId(department.getId());
        request.setPositionId(position.getId());
        request.setCurrentSalary(new BigDecimal("6000000"));
        request.setNotes("New hire");

        mockMvc.perform(post(BASE_URL)
                        .with(readWriteAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeCode").value("EMP-002"))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.middleName").value("A"))
                .andExpect(jsonPath("$.fullName").value("Smith Jane A"))
                .andExpect(jsonPath("$.phone").value("+998901234568"))
                .andExpect(jsonPath("$.email").value("jane@test.com"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-05-20"))
                .andExpect(jsonPath("$.gender").value("FEMALE"))
                .andExpect(jsonPath("$.address").value("123 Test Street"))
                .andExpect(jsonPath("$.hireDate").value("2024-03-01"))
                .andExpect(jsonPath("$.departmentId").value(department.getId().intValue()))
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.positionId").value(position.getId().intValue()))
                .andExpect(jsonPath("$.positionName").value("Developer"))
                .andExpect(jsonPath("$.currentSalary").value(6000000))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.notes").value("New hire"));
    }

    @Test
    void create_duplicateCode_returnsConflictOrBadRequest() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeCode("EMP-001");
        request.setFirstName("Duplicate");
        request.setLastName("Employee");
        request.setHireDate(LocalDate.of(2024, 6, 1));

        mockMvc.perform(post(BASE_URL)
                        .with(readWriteAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(anyOf(equalTo(400), equalTo(409))));
    }

    // ---- PUT /api/v1/hr/employees/{id} ----

    @Test
    void update_validRequest_returnsUpdated() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeCode("EMP-001");
        request.setFirstName("Johnny");
        request.setLastName("Updated");
        request.setMiddleName("B");
        request.setPhone("+998901111111");
        request.setEmail("johnny@test.com");
        request.setHireDate(LocalDate.of(2024, 1, 15));
        request.setDepartmentId(department.getId());
        request.setPositionId(position.getId());
        request.setCurrentSalary(new BigDecimal("7000000"));
        request.setNotes("Updated employee");

        mockMvc.perform(put(BASE_URL + "/{id}", employee.getId())
                        .with(readWriteAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.lastName").value("Updated"))
                .andExpect(jsonPath("$.middleName").value("B"))
                .andExpect(jsonPath("$.fullName").value("Updated Johnny B"))
                .andExpect(jsonPath("$.phone").value("+998901111111"))
                .andExpect(jsonPath("$.email").value("johnny@test.com"))
                .andExpect(jsonPath("$.currentSalary").value(7000000))
                .andExpect(jsonPath("$.notes").value("Updated employee"));
    }

    // ---- PUT /api/v1/hr/employees/{id}/terminate ----

    @Test
    void terminate_setsTerminatedStatus() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}/terminate", employee.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.terminationDate").isNotEmpty())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ---- DELETE /api/v1/hr/employees/{id} ----

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", employee.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/{id}", employee.getId())
                        .with(readWriteAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/{id}", employee.getId()).with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/active").with(noPermAuth()))
                .andExpect(status().isForbidden());

        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeCode("EMP-NOPERM");
        request.setFirstName("No");
        request.setLastName("Perm");
        request.setHireDate(LocalDate.of(2024, 6, 1));

        mockMvc.perform(post(BASE_URL)
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/{id}", employee.getId())
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(BASE_URL + "/{id}/terminate", employee.getId())
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/{id}", employee.getId())
                        .with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
