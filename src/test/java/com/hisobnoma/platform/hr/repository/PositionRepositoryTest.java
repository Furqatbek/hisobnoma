package com.hisobnoma.platform.hr.repository;

import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class PositionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PositionRepository positionRepository;

    private static final Long TENANT_ID = 1L;

    private Department department1;
    private Department department2;

    @BeforeEach
    void setUp() {
        department1 = Department.builder().code("ENG").name("Engineering").active(true).build();
        department1.setTenantId(TENANT_ID);
        department1 = em.persistAndFlush(department1);

        department2 = Department.builder().code("FIN").name("Finance").active(true).build();
        department2.setTenantId(TENANT_ID);
        department2 = em.persistAndFlush(department2);
    }

    private Position persistPosition(String code, String name, Department dept, boolean active) {
        Position p = Position.builder()
                .code(code).name(name).department(dept).active(active)
                .baseSalary(new BigDecimal("5000000.00")).build();
        p.setTenantId(TENANT_ID);
        return em.persistAndFlush(p);
    }

    @Test
    void findByTenantIdOrderByName_returnsOrderedList() {
        persistPosition("SE", "Software Engineer", department1, true);
        persistPosition("ACCT", "Accountant", department2, true);
        persistPosition("PM", "Project Manager", department1, true);

        List<Position> result = positionRepository.findByTenantIdOrderByName(TENANT_ID);

        assertEquals(3, result.size());
        assertEquals("Accountant", result.get(0).getName());
        assertEquals("Project Manager", result.get(1).getName());
        assertEquals("Software Engineer", result.get(2).getName());
    }

    @Test
    void findByTenantIdAndActiveTrue_returnsOnlyActive() {
        persistPosition("SE", "Software Engineer", department1, true);
        persistPosition("OLD", "Deprecated Role", department1, false);

        List<Position> result = positionRepository.findByTenantIdAndActiveTrue(TENANT_ID);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void findByTenantIdAndDepartmentId_returnsPositionsInDept() {
        persistPosition("SE", "Software Engineer", department1, true);
        persistPosition("ACCT", "Accountant", department2, true);
        persistPosition("PM", "Project Manager", department1, true);

        List<Position> result = positionRepository.findByTenantIdAndDepartmentId(TENANT_ID, department1.getId());

        assertEquals(2, result.size());
    }

    @Test
    void findByIdAndTenantId_existing_returnsPosition() {
        Position p = persistPosition("SE", "Software Engineer", department1, true);

        Optional<Position> result = positionRepository.findByIdAndTenantId(p.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("SE", result.get().getCode());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        Position p = persistPosition("SE", "Software Engineer", department1, true);

        Optional<Position> result = positionRepository.findByIdAndTenantId(p.getId(), 999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void existsByTenantIdAndCode_existing_returnsTrue() {
        persistPosition("SE", "Software Engineer", department1, true);

        assertTrue(positionRepository.existsByTenantIdAndCode(TENANT_ID, "SE"));
        assertFalse(positionRepository.existsByTenantIdAndCode(TENANT_ID, "NOPE"));
    }
}
