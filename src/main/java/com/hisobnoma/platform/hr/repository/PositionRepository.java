package com.hisobnoma.platform.hr.repository;

import com.hisobnoma.platform.hr.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByTenantIdOrderByName(Long tenantId);
    List<Position> findByTenantIdAndActiveTrue(Long tenantId);
    List<Position> findByTenantIdAndDepartmentId(Long tenantId, Long departmentId);
    Optional<Position> findByIdAndTenantId(Long id, Long tenantId);
    boolean existsByTenantIdAndCode(Long tenantId, String code);
}
