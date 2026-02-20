package com.hisobnoma.platform.hr.repository;

import com.hisobnoma.platform.hr.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByTenantIdOrderByName(Long tenantId);
    List<Department> findByTenantIdAndActiveTrue(Long tenantId);
    Optional<Department> findByIdAndTenantId(Long id, Long tenantId);
    Optional<Department> findByTenantIdAndCode(Long tenantId, String code);
    boolean existsByTenantIdAndCode(Long tenantId, String code);
}
