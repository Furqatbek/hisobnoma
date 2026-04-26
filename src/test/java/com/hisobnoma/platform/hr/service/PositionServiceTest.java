package com.hisobnoma.platform.hr.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.hr.dto.PositionDto;
import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Position;
import com.hisobnoma.platform.hr.repository.DepartmentRepository;
import com.hisobnoma.platform.hr.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private PositionService positionService;

    private Position position;
    private Department department;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
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
                .description("Software Developer")
                .department(department)
                .baseSalary(new BigDecimal("5000000"))
                .active(true)
                .build();
    }

    @Test
    void getAll_returnsPositionList() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(positionRepository.findByTenantIdOrderByName(TENANT_ID)).thenReturn(List.of(position));

        List<PositionDto> result = positionService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Developer", result.get(0).getName());
    }

    @Test
    void getAll_returnsEmptyList() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(positionRepository.findByTenantIdOrderByName(TENANT_ID)).thenReturn(Collections.emptyList());

        List<PositionDto> result = positionService.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getById_found() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(positionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(position));

        PositionDto result = positionService.getById(1L);

        assertNotNull(result);
        assertEquals("Developer", result.getName());
        assertEquals("IT Department", result.getDepartmentName());
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(positionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> positionService.getById(999L));
    }

    @Test
    void create_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(positionRepository.existsByTenantIdAndCode(TENANT_ID, "DEV")).thenReturn(false);
        when(departmentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(department));
        when(positionRepository.save(any())).thenReturn(position);

        PositionDto request = PositionDto.builder()
                .code("DEV")
                .name("Developer")
                .description("Software Developer")
                .departmentId(1L)
                .baseSalary(new BigDecimal("5000000"))
                .build();

        PositionDto result = positionService.create(request);

        assertNotNull(result);
        assertEquals("Developer", result.getName());
        verify(positionRepository).save(any());
    }

    @Test
    void create_duplicateCode_throwsBusinessException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(positionRepository.existsByTenantIdAndCode(TENANT_ID, "DEV")).thenReturn(true);

        PositionDto request = PositionDto.builder()
                .code("DEV")
                .name("Developer")
                .build();

        assertThrows(BusinessException.class, () -> positionService.create(request));
        verify(positionRepository, never()).save(any());
    }

    @Test
    void update_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(positionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(position));
        when(positionRepository.save(any())).thenReturn(position);

        PositionDto request = PositionDto.builder()
                .name("Senior Developer")
                .description("Senior Software Developer")
                .baseSalary(new BigDecimal("7000000"))
                .active(true)
                .build();

        PositionDto result = positionService.update(1L, request);

        assertNotNull(result);
        verify(positionRepository).save(any());
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(positionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        PositionDto request = PositionDto.builder().name("Updated").build();

        assertThrows(NotFoundException.class, () -> positionService.update(999L, request));
    }

    @Test
    void delete_success() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(positionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(position));

        positionService.delete(1L);

        verify(positionRepository).delete(position);
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(positionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> positionService.delete(999L));
    }
}
