package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.entity.Position;
import com.hisobnoma.platform.hr.entity.SalaryRecord;
import com.hisobnoma.platform.hr.repository.SalaryAdvanceRepository;
import com.hisobnoma.platform.hr.repository.SalaryRecordRepository;
import com.hisobnoma.platform.reports.dto.SalaryReportDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryReportServiceTest {

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @Mock
    private SalaryAdvanceRepository advanceRepository;

    @InjectMocks
    private SalaryReportService salaryReportService;

    private static final Long TENANT_ID = 1L;

    private Department department;
    private Position position;
    private Employee employee;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .name("Engineering")
                .build();

        position = Position.builder()
                .id(1L)
                .name("Developer")
                .build();

        employee = Employee.builder()
                .id(1L)
                .employeeCode("EMP-001")
                .firstName("Ali")
                .lastName("Valiyev")
                .department(department)
                .position(position)
                .hireDate(LocalDate.of(2023, 1, 1))
                .tenantId(TENANT_ID)
                .build();
    }

    // ====== generateSalaryReport ======

    @Test
    void generateSalaryReport_withPaidRecord_returnsReport() {
        // Given
        SalaryRecord record = SalaryRecord.builder()
                .id(1L)
                .employee(employee)
                .periodYear(2025)
                .periodMonth(3)
                .baseAmount(new BigDecimal("5000000"))
                .bonusAmount(new BigDecimal("500000"))
                .deductionAmount(new BigDecimal("600000"))
                .netAmount(new BigDecimal("4900000"))
                .advanceAmount(new BigDecimal("1000000"))
                .payAmount(new BigDecimal("3900000"))
                .status(SalaryRecord.SalaryStatus.PAID)
                .paidDate(LocalDate.of(2025, 3, 25))
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByPeriodWithEmployee(TENANT_ID, 2025, 3))
                .thenReturn(List.of(record));

        // When
        SalaryReportDTO result = salaryReportService.generateSalaryReport(2025, 3);

        // Then
        assertNotNull(result);
        assertEquals("Ish haqi hisoboti", result.getMetadata().getReportName());
        assertEquals(2025, result.getMetadata().getPeriodYear());
        assertEquals(3, result.getMetadata().getPeriodMonth());
        assertEquals(1, result.getSummary().getTotalEmployees());
        assertEquals(1, result.getSummary().getPaidCount());
        assertEquals(0, result.getSummary().getPendingCount());
        assertEquals(new BigDecimal("5000000"), result.getSummary().getTotalBase());
        assertEquals(new BigDecimal("4900000"), result.getSummary().getTotalNet());
        assertEquals(1, result.getEmployees().size());
        assertEquals("EMP-001", result.getEmployees().get(0).getEmployeeCode());
    }

    @Test
    void generateSalaryReport_withPendingRecord_calculatesAdvanceDynamically() {
        // Given
        SalaryRecord record = SalaryRecord.builder()
                .id(2L)
                .employee(employee)
                .periodYear(2025)
                .periodMonth(4)
                .baseAmount(new BigDecimal("5000000"))
                .bonusAmount(BigDecimal.ZERO)
                .deductionAmount(BigDecimal.ZERO)
                .netAmount(new BigDecimal("5000000"))
                .status(SalaryRecord.SalaryStatus.PENDING)
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByPeriodWithEmployee(TENANT_ID, 2025, 4))
                .thenReturn(List.of(record));
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2025, 4))
                .thenReturn(new BigDecimal("2000000"));

        // When
        SalaryReportDTO result = salaryReportService.generateSalaryReport(2025, 4);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSummary().getPendingCount());
        assertEquals(new BigDecimal("2000000"), result.getSummary().getTotalAdvances());
        assertEquals(new BigDecimal("3000000"), result.getSummary().getTotalCashPaid());
    }

    @Test
    void generateSalaryReport_noRecords_returnsEmptyReport() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByPeriodWithEmployee(TENANT_ID, 2025, 1))
                .thenReturn(Collections.emptyList());

        // When
        SalaryReportDTO result = salaryReportService.generateSalaryReport(2025, 1);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getSummary().getTotalEmployees());
        assertTrue(result.getEmployees().isEmpty());
        assertTrue(result.getByDepartment().isEmpty());
    }

    @Test
    void generateSalaryReport_multipleDepartments_buildsDepartmentBreakdown() {
        // Given
        Department dept2 = Department.builder().id(2L).name("Sales").build();
        Employee emp2 = Employee.builder()
                .id(2L)
                .employeeCode("EMP-002")
                .firstName("Sali")
                .lastName("Karimov")
                .department(dept2)
                .position(position)
                .hireDate(LocalDate.of(2023, 6, 1))
                .tenantId(TENANT_ID)
                .build();

        SalaryRecord record1 = SalaryRecord.builder()
                .id(1L)
                .employee(employee)
                .periodYear(2025)
                .periodMonth(3)
                .baseAmount(new BigDecimal("5000000"))
                .bonusAmount(BigDecimal.ZERO)
                .deductionAmount(BigDecimal.ZERO)
                .netAmount(new BigDecimal("5000000"))
                .advanceAmount(BigDecimal.ZERO)
                .payAmount(new BigDecimal("5000000"))
                .status(SalaryRecord.SalaryStatus.PAID)
                .tenantId(TENANT_ID)
                .build();

        SalaryRecord record2 = SalaryRecord.builder()
                .id(2L)
                .employee(emp2)
                .periodYear(2025)
                .periodMonth(3)
                .baseAmount(new BigDecimal("4000000"))
                .bonusAmount(BigDecimal.ZERO)
                .deductionAmount(BigDecimal.ZERO)
                .netAmount(new BigDecimal("4000000"))
                .advanceAmount(BigDecimal.ZERO)
                .payAmount(new BigDecimal("4000000"))
                .status(SalaryRecord.SalaryStatus.PAID)
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByPeriodWithEmployee(TENANT_ID, 2025, 3))
                .thenReturn(List.of(record1, record2));

        // When
        SalaryReportDTO result = salaryReportService.generateSalaryReport(2025, 3);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getSummary().getTotalEmployees());
        assertEquals(2, result.getByDepartment().size());
        assertEquals(new BigDecimal("9000000"), result.getSummary().getTotalNet());
    }

    @Test
    void generateSalaryReport_noDepartment_usesDefault() {
        // Given
        Employee empNoDept = Employee.builder()
                .id(3L)
                .employeeCode("EMP-003")
                .firstName("Test")
                .lastName("User")
                .department(null)
                .position(null)
                .hireDate(LocalDate.of(2024, 1, 1))
                .tenantId(TENANT_ID)
                .build();

        SalaryRecord record = SalaryRecord.builder()
                .id(3L)
                .employee(empNoDept)
                .periodYear(2025)
                .periodMonth(3)
                .baseAmount(new BigDecimal("3000000"))
                .bonusAmount(BigDecimal.ZERO)
                .deductionAmount(BigDecimal.ZERO)
                .netAmount(new BigDecimal("3000000"))
                .advanceAmount(BigDecimal.ZERO)
                .payAmount(new BigDecimal("3000000"))
                .status(SalaryRecord.SalaryStatus.PAID)
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByPeriodWithEmployee(TENANT_ID, 2025, 3))
                .thenReturn(List.of(record));

        // When
        SalaryReportDTO result = salaryReportService.generateSalaryReport(2025, 3);

        // Then
        assertNotNull(result);
        assertEquals("Belgilanmagan", result.getEmployees().get(0).getDepartmentName());
        assertNull(result.getEmployees().get(0).getPositionName());
    }
}
