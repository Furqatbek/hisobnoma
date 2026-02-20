package com.hisobnoma.platform.hr.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.hr.dto.DepartmentDto;
import com.hisobnoma.platform.hr.entity.Department;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.repository.DepartmentRepository;
import com.hisobnoma.platform.hr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<DepartmentDto> getAll() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return departmentRepository.findByTenantIdOrderByName(tenantId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentDto getById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return toDto(departmentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Department", id)));
    }

    @Transactional
    public DepartmentDto create(DepartmentDto request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        if (departmentRepository.existsByTenantIdAndCode(tenantId, request.getCode())) {
            throw new BusinessException("Department code already exists: " + request.getCode());
        }

        Department dept = Department.builder()
                .tenantId(tenantId)
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .build();

        if (request.getParentId() != null) {
            dept.setParent(departmentRepository.findByIdAndTenantId(request.getParentId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Parent Department", request.getParentId())));
        }
        if (request.getManagerId() != null) {
            dept.setManager(employeeRepository.findByIdAndTenantId(request.getManagerId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Employee", request.getManagerId())));
        }

        return toDto(departmentRepository.save(dept));
    }

    @Transactional
    public DepartmentDto update(Long id, DepartmentDto request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Department dept = departmentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Department", id));

        dept.setName(request.getName());
        dept.setDescription(request.getDescription());
        dept.setActive(request.isActive());

        if (request.getParentId() != null) {
            dept.setParent(departmentRepository.findByIdAndTenantId(request.getParentId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Parent Department", request.getParentId())));
        } else {
            dept.setParent(null);
        }
        if (request.getManagerId() != null) {
            dept.setManager(employeeRepository.findByIdAndTenantId(request.getManagerId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Employee", request.getManagerId())));
        } else {
            dept.setManager(null);
        }

        return toDto(departmentRepository.save(dept));
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Department dept = departmentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Department", id));
        departmentRepository.delete(dept);
    }

    private DepartmentDto toDto(Department d) {
        return DepartmentDto.builder()
                .id(d.getId())
                .code(d.getCode())
                .name(d.getName())
                .description(d.getDescription())
                .parentId(d.getParent() != null ? d.getParent().getId() : null)
                .parentName(d.getParent() != null ? d.getParent().getName() : null)
                .managerId(d.getManager() != null ? d.getManager().getId() : null)
                .managerName(d.getManager() != null ? d.getManager().getFullName() : null)
                .active(d.isActive())
                .build();
    }
}
