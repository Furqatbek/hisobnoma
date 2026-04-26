package com.hisobnoma.platform.hr.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.service.GLIntegrationService;
import com.hisobnoma.platform.hr.dto.CreateSalaryRecordRequest;
import com.hisobnoma.platform.hr.dto.SalaryRecordDto;
import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.entity.SalaryAdvance;
import com.hisobnoma.platform.hr.entity.SalaryRecord;
import com.hisobnoma.platform.hr.repository.EmployeeRepository;
import com.hisobnoma.platform.hr.repository.SalaryAdvanceRepository;
import com.hisobnoma.platform.hr.repository.SalaryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalaryServiceTest {

    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalaryAdvanceRepository advanceRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private GLIntegrationService glIntegrationService;

    @InjectMocks
    private SalaryService salaryService;

    private Employee employee;
    private Department department;
    private SalaryRecord salaryRecord;
    private static final Long TENANT_ID = 1L;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        department = Department.builder()
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

        salaryRecord = SalaryRecord.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .employee(employee)
                .periodYear(2024)
                .periodMonth(1)
                .baseAmount(new BigDecimal("5000000"))
                .bonusAmount(new BigDecimal("500000"))
                .deductionAmount(new BigDecimal("200000"))
                .netAmount(new BigDecimal("5300000"))
                .status(SalaryRecord.SalaryStatus.PENDING)
                .build();
    }

    @Test
    void getAll_returnsPage() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<SalaryRecord> page = new PageImpl<>(List.of(salaryRecord), pageable, 1);
        when(salaryRecordRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2024, 1))
                .thenReturn(BigDecimal.ZERO);

        PageResponse<SalaryRecordDto> result = salaryService.getAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("PENDING", result.getContent().get(0).getStatus());
    }

    @Test
    void getAll_returnsEmptyPage() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<SalaryRecord> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(salaryRecordRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

        PageResponse<SalaryRecordDto> result = salaryService.getAll(pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getByPeriod_returnsPage() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<SalaryRecord> page = new PageImpl<>(List.of(salaryRecord), pageable, 1);
        when(salaryRecordRepository.findByTenantIdAndPeriodYearAndPeriodMonth(TENANT_ID, 2024, 1, pageable))
                .thenReturn(page);
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2024, 1))
                .thenReturn(BigDecimal.ZERO);

        PageResponse<SalaryRecordDto> result = salaryService.getByPeriod(2024, 1, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(2024, result.getContent().get(0).getPeriodYear());
    }

    @Test
    void getByEmployee_returnsRecords() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.findByTenantIdAndEmployeeIdOrderByPeriodYearDescPeriodMonthDesc(TENANT_ID, 1L))
                .thenReturn(List.of(salaryRecord));
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2024, 1))
                .thenReturn(BigDecimal.ZERO);

        List<SalaryRecordDto> result = salaryService.getByEmployee(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getEmployeeId());
    }

    @Test
    void getByEmployee_employeeNotFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> salaryService.getByEmployee(999L));
    }

    @Test
    void getById_found() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(salaryRecord));
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2024, 1))
                .thenReturn(BigDecimal.ZERO);

        SalaryRecordDto result = salaryService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> salaryService.getById(999L));
    }

    @Test
    void create_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.existsByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
                TENANT_ID, 1L, 2024, 1)).thenReturn(false);
        when(salaryRecordRepository.save(any())).thenReturn(salaryRecord);
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2024, 1))
                .thenReturn(BigDecimal.ZERO);

        CreateSalaryRecordRequest request = new CreateSalaryRecordRequest();
        request.setEmployeeId(1L);
        request.setPeriodYear(2024);
        request.setPeriodMonth(1);
        request.setBaseAmount(new BigDecimal("5000000"));
        request.setBonusAmount(new BigDecimal("500000"));
        request.setDeductionAmount(new BigDecimal("200000"));

        SalaryRecordDto result = salaryService.create(request);

        assertNotNull(result);
        verify(salaryRecordRepository).save(any());
    }

    @Test
    void create_duplicatePeriod_throwsBusinessException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.existsByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
                TENANT_ID, 1L, 2024, 1)).thenReturn(true);

        CreateSalaryRecordRequest request = new CreateSalaryRecordRequest();
        request.setEmployeeId(1L);
        request.setPeriodYear(2024);
        request.setPeriodMonth(1);
        request.setBaseAmount(new BigDecimal("5000000"));

        assertThrows(BusinessException.class, () -> salaryService.create(request));
        verify(salaryRecordRepository, never()).save(any());
    }

    @Test
    void create_employeeNotFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        CreateSalaryRecordRequest request = new CreateSalaryRecordRequest();
        request.setEmployeeId(999L);
        request.setPeriodYear(2024);
        request.setPeriodMonth(1);
        request.setBaseAmount(new BigDecimal("5000000"));

        assertThrows(NotFoundException.class, () -> salaryService.create(request));
    }

    @Test
    void markPaid_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(salaryRecord));
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2024, 1))
                .thenReturn(new BigDecimal("1000000"));
        when(glIntegrationService.postSalaryPayment(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(100L);
        when(advanceRepository.findByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonthAndStatus(
                TENANT_ID, 1L, 2024, 1, SalaryAdvance.AdvanceStatus.GIVEN))
                .thenReturn(Collections.emptyList());
        when(salaryRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SalaryRecordDto result = salaryService.markPaid(1L);

        assertNotNull(result);
        assertEquals("PAID", result.getStatus());
        assertNotNull(result.getPaidDate());
        verify(glIntegrationService).postSalaryPayment(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void markPaid_notPending_throwsBusinessException() {
        SalaryRecord paidRecord = SalaryRecord.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .employee(employee)
                .periodYear(2024)
                .periodMonth(1)
                .baseAmount(new BigDecimal("5000000"))
                .netAmount(new BigDecimal("5000000"))
                .status(SalaryRecord.SalaryStatus.PAID)
                .paidDate(LocalDate.now())
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(paidRecord));

        assertThrows(BusinessException.class, () -> salaryService.markPaid(2L));
    }

    @Test
    void markPaid_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> salaryService.markPaid(999L));
    }

    @Test
    void markPaid_glFails_throwsBusinessException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(salaryRecord));
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2024, 1))
                .thenReturn(BigDecimal.ZERO);
        when(glIntegrationService.postSalaryPayment(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("GL error"));

        assertThrows(BusinessException.class, () -> salaryService.markPaid(1L));
    }

    @Test
    void cancel_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(salaryRecord));
        when(salaryRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2024, 1))
                .thenReturn(BigDecimal.ZERO);

        SalaryRecordDto result = salaryService.cancel(1L);

        assertNotNull(result);
        assertEquals("CANCELLED", result.getStatus());
    }

    @Test
    void cancel_alreadyPaid_throwsBusinessException() {
        SalaryRecord paidRecord = SalaryRecord.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .employee(employee)
                .periodYear(2024)
                .periodMonth(1)
                .baseAmount(new BigDecimal("5000000"))
                .netAmount(new BigDecimal("5000000"))
                .status(SalaryRecord.SalaryStatus.PAID)
                .paidDate(LocalDate.now())
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(paidRecord));

        assertThrows(BusinessException.class, () -> salaryService.cancel(2L));
    }

    @Test
    void cancel_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> salaryService.cancel(999L));
    }

    @Test
    void markPaid_deductsAdvancesAndSetsPayAmount() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(salaryRecordRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(salaryRecord));
        when(advanceRepository.sumGivenByEmployeeAndPeriod(TENANT_ID, 1L, 2024, 1))
                .thenReturn(new BigDecimal("2000000"));

        SalaryAdvance advance = SalaryAdvance.builder()
                .id(10L)
                .tenantId(TENANT_ID)
                .employee(employee)
                .amount(new BigDecimal("2000000"))
                .periodYear(2024)
                .periodMonth(1)
                .status(SalaryAdvance.AdvanceStatus.GIVEN)
                .build();

        when(advanceRepository.findByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonthAndStatus(
                TENANT_ID, 1L, 2024, 1, SalaryAdvance.AdvanceStatus.GIVEN))
                .thenReturn(List.of(advance));
        when(glIntegrationService.postSalaryPayment(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(100L);
        when(salaryRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SalaryRecordDto result = salaryService.markPaid(1L);

        assertNotNull(result);
        assertEquals("PAID", result.getStatus());
        assertEquals(new BigDecimal("2000000"), result.getAdvanceAmount());
        assertEquals(new BigDecimal("3300000"), result.getPayAmount());
        verify(advanceRepository).saveAll(any());
    }
}
