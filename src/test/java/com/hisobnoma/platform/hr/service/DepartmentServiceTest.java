package com.hisobnoma.platform.hr.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.hr.dto.DepartmentDto;
import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.repository.DepartmentRepository;
import com.hisobnoma.platform.hr.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .code("IT")
                .name("IT Department")
                .description("Information Technology")
                .active(true)
                .build();
    }

    @Test
    void getAll_returnsDepartmentList() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(departmentRepository.findByTenantIdOrderByName(TENANT_ID)).thenReturn(List.of(department));

        List<DepartmentDto> result = departmentService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("IT Department", result.get(0).getName());
        verify(departmentRepository).findByTenantIdOrderByName(TENANT_ID);
    }

    @Test
    void getAll_returnsEmptyList() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(departmentRepository.findByTenantIdOrderByName(TENANT_ID)).thenReturn(Collections.emptyList());

        List<DepartmentDto> result = departmentService.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getById_found() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(departmentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(department));

        DepartmentDto result = departmentService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("IT Department", result.getName());
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(departmentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> departmentService.getById(999L));
    }

    @Test
    void create_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(departmentRepository.existsByTenantIdAndCode(TENANT_ID, "IT")).thenReturn(false);
        when(departmentRepository.save(any())).thenReturn(department);

        DepartmentDto request = DepartmentDto.builder()
                .code("IT")
                .name("IT Department")
                .description("Information Technology")
                .build();

        DepartmentDto result = departmentService.create(request);

        assertNotNull(result);
        assertEquals("IT Department", result.getName());
        verify(departmentRepository).save(any());
    }

    @Test
    void create_duplicateCode_throwsBusinessException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(departmentRepository.existsByTenantIdAndCode(TENANT_ID, "IT")).thenReturn(true);

        DepartmentDto request = DepartmentDto.builder()
                .code("IT")
                .name("IT Department")
                .build();

        assertThrows(BusinessException.class, () -> departmentService.create(request));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void update_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(departmentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(department));
        when(departmentRepository.save(any())).thenReturn(department);

        DepartmentDto request = DepartmentDto.builder()
                .name("Updated IT Department")
                .description("Updated Description")
                .active(true)
                .build();

        DepartmentDto result = departmentService.update(1L, request);

        assertNotNull(result);
        verify(departmentRepository).save(any());
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(departmentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        DepartmentDto request = DepartmentDto.builder().name("Updated").build();

        assertThrows(NotFoundException.class, () -> departmentService.update(999L, request));
    }

    @Test
    void delete_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(departmentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(department));

        departmentService.delete(1L);

        verify(departmentRepository).delete(department);
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(departmentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> departmentService.delete(999L));
    }
}
