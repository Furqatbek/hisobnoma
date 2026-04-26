package com.hisobnoma.platform.hr.repository;

import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.entity.SalaryRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
class SalaryRecordRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SalaryRecordRepository salaryRecordRepository;

    private static final Long TENANT_ID = 1L;

    private Employee employee1;
    private Employee employee2;

    @BeforeEach
    void setUp() {
        Department dept = Department.builder().code("ENG").name("Engineering").active(true).build();
        dept.setTenantId(TENANT_ID);
        dept = em.persistAndFlush(dept);

        employee1 = Employee.builder()
                .employeeCode("E001").firstName("John").lastName("Doe")
                .hireDate(LocalDate.of(2024, 1, 1))
                .department(dept).active(true)
                .status(Employee.EmployeeStatus.ACTIVE).build();
        employee1.setTenantId(TENANT_ID);
        employee1 = em.persistAndFlush(employee1);

        employee2 = Employee.builder()
                .employeeCode("E002").firstName("Jane").lastName("Smith")
                .hireDate(LocalDate.of(2024, 1, 1))
                .department(dept).active(true)
                .status(Employee.EmployeeStatus.ACTIVE).build();
        employee2.setTenantId(TENANT_ID);
        employee2 = em.persistAndFlush(employee2);
    }

    private SalaryRecord persistSalaryRecord(Employee employee, int year, int month,
                                              BigDecimal base, BigDecimal net,
                                              SalaryRecord.SalaryStatus status) {
        SalaryRecord sr = SalaryRecord.builder()
                .employee(employee).periodYear(year).periodMonth(month)
                .baseAmount(base).netAmount(net)
                .status(status).build();
        sr.setTenantId(TENANT_ID);
        return em.persistAndFlush(sr);
    }

    @Test
    void findByTenantId_returnsPagedResults() {
        persistSalaryRecord(employee1, 2024, 6, new BigDecimal("5000000.00"), new BigDecimal("4500000.00"), SalaryRecord.SalaryStatus.PAID);
        persistSalaryRecord(employee2, 2024, 6, new BigDecimal("6000000.00"), new BigDecimal("5400000.00"), SalaryRecord.SalaryStatus.PENDING);

        Page<SalaryRecord> result = salaryRecordRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByIdAndTenantId_existing_returnsRecord() {
        SalaryRecord sr = persistSalaryRecord(employee1, 2024, 6, new BigDecimal("5000000.00"), new BigDecimal("4500000.00"), SalaryRecord.SalaryStatus.PAID);

        Optional<SalaryRecord> result = salaryRecordRepository.findByIdAndTenantId(sr.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals(2024, result.get().getPeriodYear());
    }

    @Test
    void findByTenantIdAndEmployeeIdOrderByPeriodYearDescPeriodMonthDesc_returnsOrdered() {
        persistSalaryRecord(employee1, 2024, 3, new BigDecimal("5000000.00"), new BigDecimal("4500000.00"), SalaryRecord.SalaryStatus.PAID);
        persistSalaryRecord(employee1, 2024, 6, new BigDecimal("5500000.00"), new BigDecimal("5000000.00"), SalaryRecord.SalaryStatus.PAID);
        persistSalaryRecord(employee1, 2023, 12, new BigDecimal("4500000.00"), new BigDecimal("4000000.00"), SalaryRecord.SalaryStatus.PAID);

        List<SalaryRecord> result = salaryRecordRepository.findByTenantIdAndEmployeeIdOrderByPeriodYearDescPeriodMonthDesc(
                TENANT_ID, employee1.getId());

        assertEquals(3, result.size());
        assertEquals(2024, result.get(0).getPeriodYear());
        assertEquals(6, result.get(0).getPeriodMonth());
    }

    @Test
    void findByTenantIdAndPeriodYearAndPeriodMonth_filtersCorrectly() {
        persistSalaryRecord(employee1, 2024, 6, new BigDecimal("5000000.00"), new BigDecimal("4500000.00"), SalaryRecord.SalaryStatus.PAID);
        persistSalaryRecord(employee2, 2024, 6, new BigDecimal("6000000.00"), new BigDecimal("5400000.00"), SalaryRecord.SalaryStatus.PAID);
        persistSalaryRecord(employee1, 2024, 7, new BigDecimal("5000000.00"), new BigDecimal("4500000.00"), SalaryRecord.SalaryStatus.PENDING);

        Page<SalaryRecord> result = salaryRecordRepository.findByTenantIdAndPeriodYearAndPeriodMonth(
                TENANT_ID, 2024, 6, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void existsByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonth_existing_returnsTrue() {
        persistSalaryRecord(employee1, 2024, 6, new BigDecimal("5000000.00"), new BigDecimal("4500000.00"), SalaryRecord.SalaryStatus.PAID);

        assertTrue(salaryRecordRepository.existsByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
                TENANT_ID, employee1.getId(), 2024, 6));
        assertFalse(salaryRecordRepository.existsByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
                TENANT_ID, employee1.getId(), 2024, 7));
    }

    @Test
    void sumPaidByPeriod_returnsTotalPaidAmount() {
        persistSalaryRecord(employee1, 2024, 6, new BigDecimal("5000000.00"), new BigDecimal("4500000.00"), SalaryRecord.SalaryStatus.PAID);
        persistSalaryRecord(employee2, 2024, 6, new BigDecimal("6000000.00"), new BigDecimal("5400000.00"), SalaryRecord.SalaryStatus.PAID);
        persistSalaryRecord(employee1, 2024, 6, new BigDecimal("3000000.00"), new BigDecimal("2700000.00"), SalaryRecord.SalaryStatus.PENDING);

        BigDecimal result = salaryRecordRepository.sumPaidByPeriod(TENANT_ID, 2024, 6);

        assertEquals(0, new BigDecimal("9900000.00").compareTo(result));
    }

    @Test
    void findByPeriodWithEmployee_returnsRecordsWithJoinedEmployee() {
        persistSalaryRecord(employee1, 2024, 6, new BigDecimal("5000000.00"), new BigDecimal("4500000.00"), SalaryRecord.SalaryStatus.PAID);
        persistSalaryRecord(employee2, 2024, 6, new BigDecimal("6000000.00"), new BigDecimal("5400000.00"), SalaryRecord.SalaryStatus.PAID);

        List<SalaryRecord> result = salaryRecordRepository.findByPeriodWithEmployee(TENANT_ID, 2024, 6);

        assertEquals(2, result.size());
        // Ordered by lastName, firstName: Doe comes before Smith
        assertEquals("Doe", result.get(0).getEmployee().getLastName());
        assertEquals("Smith", result.get(1).getEmployee().getLastName());
    }
}
