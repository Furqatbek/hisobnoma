package com.hisobnoma.platform.hr.service;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.hr.dto.CreateEmployeeRequest;
import com.hisobnoma.platform.hr.dto.EmployeeDto;
import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.entity.Position;
import com.hisobnoma.platform.hr.repository.DepartmentRepository;
import com.hisobnoma.platform.hr.repository.EmployeeRepository;
import com.hisobnoma.platform.hr.repository.PositionRepository;
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
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee;
    private Department department;
    private Position position;
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

        position = Position.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .code("DEV")
                .name("Developer")
                .active(true)
                .build();

        employee = Employee.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .employeeCode("EMP-001")
                .firstName("Ali")
                .lastName("Valiyev")
                .phone("+998901234567")
                .email("ali@example.com")
                .hireDate(LocalDate.of(2023, 1, 15))
                .department(department)
                .position(position)
                .currentSalary(new BigDecimal("5000000"))
                .status(Employee.EmployeeStatus.ACTIVE)
                .active(true)
                .build();
    }

    @Test
    void getAll_returnsPage() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<Employee> page = new PageImpl<>(List.of(employee), pageable, 1);
        when(employeeRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

        PageResponse<EmployeeDto> result = employeeService.getAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("EMP-001", result.getContent().get(0).getEmployeeCode());
    }

    @Test
    void getAll_returnsEmptyPage() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<Employee> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(employeeRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

        PageResponse<EmployeeDto> result = employeeService.getAll(pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getActive_returnsList() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(List.of(employee));

        List<EmployeeDto> result = employeeService.getActive();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void getById_found() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(employee));

        EmployeeDto result = employeeService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Valiyev Ali", result.getFullName());
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> employeeService.getById(999L));
    }

    @Test
    void create_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.existsByTenantIdAndEmployeeCode(TENANT_ID, "EMP-002")).thenReturn(false);
        when(departmentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(department));
        when(positionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(position));
        when(employeeRepository.save(any())).thenReturn(employee);

        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeCode("EMP-002");
        request.setFirstName("Ali");
        request.setLastName("Valiyev");
        request.setHireDate(LocalDate.of(2023, 1, 15));
        request.setDepartmentId(1L);
        request.setPositionId(1L);

        EmployeeDto result = employeeService.create(request);

        assertNotNull(result);
        verify(employeeRepository).save(any());
    }

    @Test
    void create_duplicateEmployeeCode_throwsBusinessException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.existsByTenantIdAndEmployeeCode(TENANT_ID, "EMP-001")).thenReturn(true);

        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeCode("EMP-001");
        request.setFirstName("Ali");
        request.setLastName("Valiyev");
        request.setHireDate(LocalDate.now());

        assertThrows(BusinessException.class, () -> employeeService.create(request));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void create_invalidDepartment_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.existsByTenantIdAndEmployeeCode(TENANT_ID, "EMP-002")).thenReturn(false);
        when(departmentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeCode("EMP-002");
        request.setFirstName("Ali");
        request.setLastName("Valiyev");
        request.setHireDate(LocalDate.now());
        request.setDepartmentId(999L);

        assertThrows(NotFoundException.class, () -> employeeService.create(request));
    }

    @Test
    void update_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any())).thenReturn(employee);

        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setHireDate(LocalDate.now());

        EmployeeDto result = employeeService.update(1L, request);

        assertNotNull(result);
        verify(employeeRepository).save(any());
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setHireDate(LocalDate.now());

        assertThrows(NotFoundException.class, () -> employeeService.update(999L, request));
    }

    @Test
    void delete_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(employee));

        employeeService.delete(1L);

        verify(employeeRepository).delete(employee);
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> employeeService.delete(999L));
    }

    @Test
    void terminate_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(employeeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeeDto result = employeeService.terminate(1L);

        assertNotNull(result);
        assertEquals("TERMINATED", result.getStatus());
        assertFalse(result.isActive());
    }

    @Test
    void search_returnsResults() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<Employee> page = new PageImpl<>(List.of(employee), pageable, 1);
        when(employeeRepository.search(TENANT_ID, "ali", pageable)).thenReturn(page);

        PageResponse<EmployeeDto> result = employeeService.search("ali", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }
}
