package com.hisobnoma.platform.hr.service;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.hr.dto.CreateEmployeeRequest;
import com.hisobnoma.platform.hr.dto.EmployeeDto;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.repository.DepartmentRepository;
import com.hisobnoma.platform.hr.repository.EmployeeRepository;
import com.hisobnoma.platform.hr.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDto> getAll(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<EmployeeDto> page = employeeRepository.findByTenantId(tenantId, pageable).map(this::toDto);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> getActive() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return employeeRepository.findByTenantIdAndActiveTrue(tenantId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeDto getById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return toDto(employeeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDto> search(String query, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<EmployeeDto> page = employeeRepository.search(tenantId, query, pageable).map(this::toDto);
        return PageResponse.from(page);
    }

    @Transactional
    public EmployeeDto create(CreateEmployeeRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        if (employeeRepository.existsByTenantIdAndEmployeeCode(tenantId, request.getEmployeeCode())) {
            throw new BusinessException("Employee code already exists: " + request.getEmployeeCode());
        }

        Employee employee = Employee.builder()
                .tenantId(tenantId)
                .employeeCode(request.getEmployeeCode())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .hireDate(request.getHireDate())
                .currentSalary(request.getCurrentSalary())
                .notes(request.getNotes())
                .status(Employee.EmployeeStatus.ACTIVE)
                .active(true)
                .build();

        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentRepository.findByIdAndTenantId(request.getDepartmentId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Department", request.getDepartmentId())));
        }
        if (request.getPositionId() != null) {
            employee.setPosition(positionRepository.findByIdAndTenantId(request.getPositionId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Position", request.getPositionId())));
        }
        if (request.getUserId() != null) {
            employee.setUser(userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new NotFoundException("User", request.getUserId())));
        }

        return toDto(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeDto update(Long id, CreateEmployeeRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Employee employee = employeeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", id));

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setMiddleName(request.getMiddleName());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setGender(request.getGender());
        employee.setAddress(request.getAddress());
        employee.setHireDate(request.getHireDate());
        employee.setCurrentSalary(request.getCurrentSalary());
        employee.setNotes(request.getNotes());

        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentRepository.findByIdAndTenantId(request.getDepartmentId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Department", request.getDepartmentId())));
        } else {
            employee.setDepartment(null);
        }
        if (request.getPositionId() != null) {
            employee.setPosition(positionRepository.findByIdAndTenantId(request.getPositionId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Position", request.getPositionId())));
        } else {
            employee.setPosition(null);
        }
        if (request.getUserId() != null) {
            employee.setUser(userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new NotFoundException("User", request.getUserId())));
        } else {
            employee.setUser(null);
        }

        return toDto(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeDto terminate(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Employee employee = employeeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", id));

        employee.setStatus(Employee.EmployeeStatus.TERMINATED);
        employee.setTerminationDate(java.time.LocalDate.now());
        employee.setActive(false);

        return toDto(employeeRepository.save(employee));
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Employee employee = employeeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", id));
        employeeRepository.delete(employee);
    }

    private EmployeeDto toDto(Employee e) {
        return EmployeeDto.builder()
                .id(e.getId())
                .employeeCode(e.getEmployeeCode())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .middleName(e.getMiddleName())
                .fullName(e.getFullName())
                .phone(e.getPhone())
                .email(e.getEmail())
                .dateOfBirth(e.getDateOfBirth())
                .gender(e.getGender())
                .address(e.getAddress())
                .hireDate(e.getHireDate())
                .terminationDate(e.getTerminationDate())
                .departmentId(e.getDepartment() != null ? e.getDepartment().getId() : null)
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .positionId(e.getPosition() != null ? e.getPosition().getId() : null)
                .positionName(e.getPosition() != null ? e.getPosition().getName() : null)
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .username(e.getUser() != null ? e.getUser().getUsername() : null)
                .currentSalary(e.getCurrentSalary())
                .status(e.getStatus().name())
                .notes(e.getNotes())
                .active(e.isActive())
                .build();
    }
}
