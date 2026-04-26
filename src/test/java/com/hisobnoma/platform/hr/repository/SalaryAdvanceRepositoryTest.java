package com.hisobnoma.platform.hr.repository;

import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.entity.SalaryAdvance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class SalaryAdvanceRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SalaryAdvanceRepository salaryAdvanceRepository;

    private static final Long TENANT_ID = 1L;

    private Employee employee;

    @BeforeEach
    void setUp() {
        Department dept = Department.builder().code("ENG").name("Engineering").active(true).build();
        dept.setTenantId(TENANT_ID);
        dept = em.persistAndFlush(dept);

        employee = Employee.builder()
                .employeeCode("E001").firstName("John").lastName("Doe")
                .hireDate(LocalDate.of(2024, 1, 1))
                .department(dept).active(true)
                .status(Employee.EmployeeStatus.ACTIVE).build();
        employee.setTenantId(TENANT_ID);
        employee = em.persistAndFlush(employee);
    }

    private SalaryAdvance persistAdvance(BigDecimal amount, LocalDate advanceDate,
                                          int year, int month, SalaryAdvance.AdvanceStatus status) {
        SalaryAdvance a = SalaryAdvance.builder()
                .employee(employee).amount(amount)
                .advanceDate(advanceDate)
                .periodYear(year).periodMonth(month)
                .status(status).build();
        a.setTenantId(TENANT_ID);
        return em.persistAndFlush(a);
    }

    @Test
    void findByIdAndTenantId_existing_returnsAdvance() {
        SalaryAdvance a = persistAdvance(new BigDecimal("1000000.00"), LocalDate.of(2024, 6, 15), 2024, 6, SalaryAdvance.AdvanceStatus.GIVEN);

        Optional<SalaryAdvance> result = salaryAdvanceRepository.findByIdAndTenantId(a.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("1000000.00").compareTo(result.get().getAmount()));
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        SalaryAdvance a = persistAdvance(new BigDecimal("1000000.00"), LocalDate.of(2024, 6, 15), 2024, 6, SalaryAdvance.AdvanceStatus.GIVEN);

        Optional<SalaryAdvance> result = salaryAdvanceRepository.findByIdAndTenantId(a.getId(), 999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByTenantIdAndPeriodYearAndPeriodMonthOrderByAdvanceDateDesc_returnsOrdered() {
        persistAdvance(new BigDecimal("500000.00"), LocalDate.of(2024, 6, 5), 2024, 6, SalaryAdvance.AdvanceStatus.GIVEN);
        persistAdvance(new BigDecimal("700000.00"), LocalDate.of(2024, 6, 15), 2024, 6, SalaryAdvance.AdvanceStatus.GIVEN);
        persistAdvance(new BigDecimal("300000.00"), LocalDate.of(2024, 7, 10), 2024, 7, SalaryAdvance.AdvanceStatus.GIVEN);

        List<SalaryAdvance> result = salaryAdvanceRepository
                .findByTenantIdAndPeriodYearAndPeriodMonthOrderByAdvanceDateDesc(TENANT_ID, 2024, 6);

        assertEquals(2, result.size());
        // Most recent date first
        assertTrue(result.get(0).getAdvanceDate().isAfter(result.get(1).getAdvanceDate())
                   || result.get(0).getAdvanceDate().isEqual(result.get(1).getAdvanceDate()));
    }

    @Test
    void findByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonthAndStatus_filtersCorrectly() {
        persistAdvance(new BigDecimal("500000.00"), LocalDate.of(2024, 6, 5), 2024, 6, SalaryAdvance.AdvanceStatus.GIVEN);
        persistAdvance(new BigDecimal("300000.00"), LocalDate.of(2024, 6, 15), 2024, 6, SalaryAdvance.AdvanceStatus.DEDUCTED);

        List<SalaryAdvance> result = salaryAdvanceRepository
                .findByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonthAndStatus(
                        TENANT_ID, employee.getId(), 2024, 6, SalaryAdvance.AdvanceStatus.GIVEN);

        assertEquals(1, result.size());
        assertEquals(SalaryAdvance.AdvanceStatus.GIVEN, result.get(0).getStatus());
    }

    @Test
    void sumGivenByEmployeeAndPeriod_returnsTotalGivenAmount() {
        persistAdvance(new BigDecimal("500000.00"), LocalDate.of(2024, 6, 5), 2024, 6, SalaryAdvance.AdvanceStatus.GIVEN);
        persistAdvance(new BigDecimal("700000.00"), LocalDate.of(2024, 6, 15), 2024, 6, SalaryAdvance.AdvanceStatus.GIVEN);
        persistAdvance(new BigDecimal("200000.00"), LocalDate.of(2024, 6, 20), 2024, 6, SalaryAdvance.AdvanceStatus.CANCELLED);

        BigDecimal result = salaryAdvanceRepository.sumGivenByEmployeeAndPeriod(
                TENANT_ID, employee.getId(), 2024, 6);

        assertEquals(0, new BigDecimal("1200000.00").compareTo(result));
    }

    @Test
    void sumGivenByEmployeeAndPeriod_noRecords_returnsZero() {
        BigDecimal result = salaryAdvanceRepository.sumGivenByEmployeeAndPeriod(
                TENANT_ID, employee.getId(), 2024, 12);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void findByTenantIdAndEmployeeIdOrderByAdvanceDateDesc_returnsAllForEmployee() {
        persistAdvance(new BigDecimal("500000.00"), LocalDate.of(2024, 5, 15), 2024, 5, SalaryAdvance.AdvanceStatus.DEDUCTED);
        persistAdvance(new BigDecimal("700000.00"), LocalDate.of(2024, 6, 15), 2024, 6, SalaryAdvance.AdvanceStatus.GIVEN);

        List<SalaryAdvance> result = salaryAdvanceRepository
                .findByTenantIdAndEmployeeIdOrderByAdvanceDateDesc(TENANT_ID, employee.getId());

        assertEquals(2, result.size());
        assertEquals(6, result.get(0).getPeriodMonth());
        assertEquals(5, result.get(1).getPeriodMonth());
    }
}
