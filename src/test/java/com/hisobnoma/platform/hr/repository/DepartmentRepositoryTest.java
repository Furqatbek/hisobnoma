package com.hisobnoma.platform.hr.repository;

import com.hisobnoma.platform.hr.entity.Department;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class DepartmentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private DepartmentRepository departmentRepository;

    private static final Long TENANT_ID = 1L;

    private Department persistDepartment(String code, String name, boolean active) {
        Department d = Department.builder()
                .code(code).name(name).active(active).build();
        d.setTenantId(TENANT_ID);
        return em.persistAndFlush(d);
    }

    @Test
    void findByTenantIdOrderByName_returnsOrderedList() {
        persistDepartment("FIN", "Finance", true);
        persistDepartment("ENG", "Engineering", true);
        persistDepartment("HR", "Human Resources", true);

        List<Department> result = departmentRepository.findByTenantIdOrderByName(TENANT_ID);

        assertEquals(3, result.size());
        assertEquals("Engineering", result.get(0).getName());
        assertEquals("Finance", result.get(1).getName());
        assertEquals("Human Resources", result.get(2).getName());
    }

    @Test
    void findByTenantIdAndActiveTrue_returnsOnlyActive() {
        persistDepartment("FIN", "Finance", true);
        persistDepartment("OLD", "Deprecated", false);

        List<Department> result = departmentRepository.findByTenantIdAndActiveTrue(TENANT_ID);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void findByIdAndTenantId_existing_returnsDepartment() {
        Department d = persistDepartment("FIN", "Finance", true);

        Optional<Department> result = departmentRepository.findByIdAndTenantId(d.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("FIN", result.get().getCode());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        Department d = persistDepartment("FIN", "Finance", true);

        Optional<Department> result = departmentRepository.findByIdAndTenantId(d.getId(), 999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByTenantIdAndCode_existing_returnsDepartment() {
        persistDepartment("ENG", "Engineering", true);

        Optional<Department> result = departmentRepository.findByTenantIdAndCode(TENANT_ID, "ENG");

        assertTrue(result.isPresent());
        assertEquals("Engineering", result.get().getName());
    }

    @Test
    void existsByTenantIdAndCode_existing_returnsTrue() {
        persistDepartment("FIN", "Finance", true);

        assertTrue(departmentRepository.existsByTenantIdAndCode(TENANT_ID, "FIN"));
        assertFalse(departmentRepository.existsByTenantIdAndCode(TENANT_ID, "NOPE"));
    }

    @Test
    void findByTenantIdAndActiveTrue_multiTenant_isolatesData() {
        persistDepartment("FIN", "Finance T1", true);

        Department t2Dept = Department.builder().code("FIN").name("Finance T2").active(true).build();
        t2Dept.setTenantId(2L);
        em.persistAndFlush(t2Dept);

        List<Department> t1 = departmentRepository.findByTenantIdAndActiveTrue(TENANT_ID);
        List<Department> t2 = departmentRepository.findByTenantIdAndActiveTrue(2L);

        assertEquals(1, t1.size());
        assertEquals("Finance T1", t1.get(0).getName());
        assertEquals(1, t2.size());
        assertEquals("Finance T2", t2.get(0).getName());
    }
}
