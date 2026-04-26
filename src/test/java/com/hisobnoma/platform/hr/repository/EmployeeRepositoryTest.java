package com.hisobnoma.platform.hr.repository;

import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class EmployeeRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private EmployeeRepository employeeRepository;

    private static final Long TENANT_ID = 1L;

    private Department department;

    @BeforeEach
    void setUp() {
        department = Department.builder().code("ENG").name("Engineering").active(true).build();
        department.setTenantId(TENANT_ID);
        department = em.persistAndFlush(department);
    }

    private Employee persistEmployee(String code, String firstName, String lastName,
                                      boolean active, Department dept) {
        Employee e = Employee.builder()
                .employeeCode(code).firstName(firstName).lastName(lastName)
                .hireDate(LocalDate.of(2024, 1, 1))
                .department(dept).active(active)
                .status(Employee.EmployeeStatus.ACTIVE)
                .build();
        e.setTenantId(TENANT_ID);
        return em.persistAndFlush(e);
    }

    @Test
    void findByTenantId_returnsPagedResults() {
        persistEmployee("E001", "John", "Doe", true, department);
        persistEmployee("E002", "Jane", "Smith", true, department);

        Page<Employee> result = employeeRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByTenantIdAndActiveTrue_returnsOnlyActive() {
        persistEmployee("E001", "John", "Doe", true, department);
        persistEmployee("E002", "Jane", "Smith", false, department);

        List<Employee> result = employeeRepository.findByTenantIdAndActiveTrue(TENANT_ID);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void findByIdAndTenantId_existing_returnsEmployee() {
        Employee e = persistEmployee("E001", "John", "Doe", true, department);

        Optional<Employee> result = employeeRepository.findByIdAndTenantId(e.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("E001", result.get().getEmployeeCode());
    }

    @Test
    void findByTenantIdAndEmployeeCode_existing_returnsEmployee() {
        persistEmployee("E-FIND", "John", "Doe", true, department);

        Optional<Employee> result = employeeRepository.findByTenantIdAndEmployeeCode(TENANT_ID, "E-FIND");

        assertTrue(result.isPresent());
        assertEquals("John", result.get().getFirstName());
    }

    @Test
    void existsByTenantIdAndEmployeeCode_existing_returnsTrue() {
        persistEmployee("E001", "John", "Doe", true, department);

        assertTrue(employeeRepository.existsByTenantIdAndEmployeeCode(TENANT_ID, "E001"));
        assertFalse(employeeRepository.existsByTenantIdAndEmployeeCode(TENANT_ID, "E-NOPE"));
    }

    @Test
    void findByTenantIdAndDepartmentId_returnsEmployeesInDept() {
        Department otherDept = Department.builder().code("FIN").name("Finance").active(true).build();
        otherDept.setTenantId(TENANT_ID);
        otherDept = em.persistAndFlush(otherDept);

        persistEmployee("E001", "John", "Doe", true, department);
        persistEmployee("E002", "Jane", "Smith", true, otherDept);

        List<Employee> result = employeeRepository.findByTenantIdAndDepartmentId(TENANT_ID, department.getId());

        assertEquals(1, result.size());
        assertEquals("E001", result.get(0).getEmployeeCode());
    }

    @Test
    void search_matchesByFirstName() {
        persistEmployee("E001", "John", "Doe", true, department);
        persistEmployee("E002", "Jane", "Smith", true, department);

        Page<Employee> result = employeeRepository.search(TENANT_ID, "john", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("John", result.getContent().get(0).getFirstName());
    }

    @Test
    void search_matchesByEmployeeCode() {
        persistEmployee("ENG-001", "John", "Doe", true, department);
        persistEmployee("FIN-001", "Jane", "Smith", true, department);

        Page<Employee> result = employeeRepository.search(TENANT_ID, "ENG", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void countByTenantIdAndActiveTrue_returnsCount() {
        persistEmployee("E001", "John", "Doe", true, department);
        persistEmployee("E002", "Jane", "Smith", true, department);
        persistEmployee("E003", "Bob", "Wilson", false, department);

        long result = employeeRepository.countByTenantIdAndActiveTrue(TENANT_ID);

        assertEquals(2L, result);
    }
}
