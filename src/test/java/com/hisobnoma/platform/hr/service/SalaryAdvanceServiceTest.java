package com.hisobnoma.platform.hr.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.service.GLIntegrationService;
import com.hisobnoma.platform.hr.dto.CreateSalaryAdvanceRequest;
import com.hisobnoma.platform.hr.dto.SalaryAdvanceDto;
import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.entity.SalaryAdvance;
import com.hisobnoma.platform.hr.repository.EmployeeRepository;
import com.hisobnoma.platform.hr.repository.SalaryAdvanceRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalaryAdvanceServiceTest {

    @Mock
    private SalaryAdvanceRepository advanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private GLIntegrationService glIntegrationService;

    @InjectMocks
    private SalaryAdvanceService salaryAdvanceService;

    private Employee employee;
    private SalaryAdvance advance;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        Department department = Department.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .code("IT")
                .name("IT Department")
                .active(true)
                .build();

        employee = Employee.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .employeeCode("EMP-001")
                .firstName("Ali")
                .lastName("Valiyev")
                .department(department)
                .status(Employee.EmployeeStatus.ACTIVE)
                .active(true)
                .build();

        advance = SalaryAdvance.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .employee(employee)
                .amount(new BigDecimal("1000000"))
                .advanceDate(LocalDate.of(2024, 1, 10))
                .periodYear(2024)
                .periodMonth(1)
                .status(SalaryAdvance.AdvanceStatus.GIVEN)
                .glJournalEntryId(50L)
                .build();
    }

    @Test
    void getByPeriod_returnsList() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(advanceRepository.findByTenantIdAndPeriodYearAndPeriodMonthOrderByAdvanceDateDesc(TENANT_ID, 2024, 1))
                .thenReturn(List.of(advance));

        List<SalaryAdvanceDto> result = salaryAdvanceService.getByPeriod(2024, 1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2024, result.get(0).getPeriodYear());
        assertEquals(1, result.get(0).getPeriodMonth());
    }

    @Test
    void getByPeriod_returnsEmptyList() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(advanceRepository.findByTenantIdAndPeriodYearAndPeriodMonthOrderByAdvanceDateDesc(TENANT_ID, 2024, 2))
                .thenReturn(Collections.emptyList());

        List<SalaryAdvanceDto> result = salaryAdvanceService.getByPeriod(2024, 2);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getByEmployee_returnsList() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(advanceRepository.findByTenantIdAndEmployeeIdOrderByAdvanceDateDesc(TENANT_ID, 1L))
                .thenReturn(List.of(advance));

        List<SalaryAdvanceDto> result = salaryAdvanceService.getByEmployee(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getEmployeeId());
    }

    @Test
    void getByEmployee_returnsEmptyList() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(advanceRepository.findByTenantIdAndEmployeeIdOrderByAdvanceDateDesc(TENANT_ID, 999L))
                .thenReturn(Collections.emptyList());

        List<SalaryAdvanceDto> result = salaryAdvanceService.getByEmployee(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUndeductedTotal_returnsTotal() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2024, 1))
                .thenReturn(new BigDecimal("2000000"));

        BigDecimal result = salaryAdvanceService.getUndeductedTotal(1L, 2024, 1);

        assertEquals(new BigDecimal("2000000"), result);
    }

    @Test
    void getUndeductedTotal_returnsZero() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2024, 1))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal result = salaryAdvanceService.getUndeductedTotal(1L, 2024, 1);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void create_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(employee));
        when(advanceRepository.save(any())).thenReturn(advance);
        when(glIntegrationService.postSalaryAdvance(any(), any(), any(), any(), any(), any()))
                .thenReturn(50L);

        CreateSalaryAdvanceRequest request = new CreateSalaryAdvanceRequest();
        request.setEmployeeId(1L);
        request.setAmount(new BigDecimal("1000000"));
        request.setPeriodYear(2024);
        request.setPeriodMonth(1);
        request.setAdvanceDate(LocalDate.of(2024, 1, 10));

        SalaryAdvanceDto result = salaryAdvanceService.create(request);

        assertNotNull(result);
        assertEquals("GIVEN", result.getStatus());
        verify(advanceRepository, times(2)).save(any());
        verify(glIntegrationService).postSalaryAdvance(any(), any(), any(), any(), any(), any());
    }

    @Test
    void create_employeeNotFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        CreateSalaryAdvanceRequest request = new CreateSalaryAdvanceRequest();
        request.setEmployeeId(999L);
        request.setAmount(new BigDecimal("1000000"));
        request.setPeriodYear(2024);
        request.setPeriodMonth(1);

        assertThrows(NotFoundException.class, () -> salaryAdvanceService.create(request));
        verify(advanceRepository, never()).save(any());
    }

    @Test
    void create_glFails_throwsBusinessException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(employee));
        when(advanceRepository.save(any())).thenReturn(advance);
        when(glIntegrationService.postSalaryAdvance(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("GL error"));

        CreateSalaryAdvanceRequest request = new CreateSalaryAdvanceRequest();
        request.setEmployeeId(1L);
        request.setAmount(new BigDecimal("1000000"));
        request.setPeriodYear(2024);
        request.setPeriodMonth(1);

        assertThrows(BusinessException.class, () -> salaryAdvanceService.create(request));
    }

    @Test
    void create_noAdvanceDate_usesToday() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(employee));
        when(advanceRepository.save(any())).thenReturn(advance);
        when(glIntegrationService.postSalaryAdvance(any(), any(), any(), any(), any(), any()))
                .thenReturn(50L);

        CreateSalaryAdvanceRequest request = new CreateSalaryAdvanceRequest();
        request.setEmployeeId(1L);
        request.setAmount(new BigDecimal("1000000"));
        request.setPeriodYear(2024);
        request.setPeriodMonth(1);
        // advanceDate is null

        SalaryAdvanceDto result = salaryAdvanceService.create(request);

        assertNotNull(result);
        verify(advanceRepository, times(2)).save(any());
    }

    @Test
    void cancel_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(advanceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(advance));
        when(advanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SalaryAdvanceDto result = salaryAdvanceService.cancel(1L);

        assertNotNull(result);
        assertEquals("CANCELLED", result.getStatus());
    }

    @Test
    void cancel_notGiven_throwsBusinessException() {
        SalaryAdvance deducted = SalaryAdvance.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .employee(employee)
                .amount(new BigDecimal("1000000"))
                .advanceDate(LocalDate.of(2024, 1, 10))
                .periodYear(2024)
                .periodMonth(1)
                .status(SalaryAdvance.AdvanceStatus.DEDUCTED)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(advanceRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(deducted));

        assertThrows(BusinessException.class, () -> salaryAdvanceService.cancel(2L));
    }

    @Test
    void cancel_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(advanceRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> salaryAdvanceService.cancel(999L));
    }
}
