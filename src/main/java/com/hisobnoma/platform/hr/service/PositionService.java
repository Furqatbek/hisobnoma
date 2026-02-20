package com.hisobnoma.platform.hr.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.hr.dto.PositionDto;
import com.hisobnoma.platform.hr.entity.Position;
import com.hisobnoma.platform.hr.repository.DepartmentRepository;
import com.hisobnoma.platform.hr.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<PositionDto> getAll() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return positionRepository.findByTenantIdOrderByName(tenantId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PositionDto getById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return toDto(positionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Position", id)));
    }

    @Transactional
    public PositionDto create(PositionDto request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        if (positionRepository.existsByTenantIdAndCode(tenantId, request.getCode())) {
            throw new BusinessException("Position code already exists: " + request.getCode());
        }

        Position pos = Position.builder()
                .tenantId(tenantId)
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .baseSalary(request.getBaseSalary())
                .active(true)
                .build();

        if (request.getDepartmentId() != null) {
            pos.setDepartment(departmentRepository.findByIdAndTenantId(request.getDepartmentId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Department", request.getDepartmentId())));
        }

        return toDto(positionRepository.save(pos));
    }

    @Transactional
    public PositionDto update(Long id, PositionDto request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Position pos = positionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Position", id));

        pos.setName(request.getName());
        pos.setDescription(request.getDescription());
        pos.setBaseSalary(request.getBaseSalary());
        pos.setActive(request.isActive());

        if (request.getDepartmentId() != null) {
            pos.setDepartment(departmentRepository.findByIdAndTenantId(request.getDepartmentId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Department", request.getDepartmentId())));
        } else {
            pos.setDepartment(null);
        }

        return toDto(positionRepository.save(pos));
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Position pos = positionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Position", id));
        positionRepository.delete(pos);
    }

    private PositionDto toDto(Position p) {
        return PositionDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .departmentId(p.getDepartment() != null ? p.getDepartment().getId() : null)
                .departmentName(p.getDepartment() != null ? p.getDepartment().getName() : null)
                .baseSalary(p.getBaseSalary())
                .active(p.isActive())
                .build();
    }
}
