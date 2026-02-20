package com.hisobnoma.platform.hr.repository;

import com.hisobnoma.platform.hr.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Page<Employee> findByTenantId(Long tenantId, Pageable pageable);
    List<Employee> findByTenantIdAndActiveTrue(Long tenantId);
    Optional<Employee> findByIdAndTenantId(Long id, Long tenantId);
    Optional<Employee> findByTenantIdAndEmployeeCode(Long tenantId, String employeeCode);
    boolean existsByTenantIdAndEmployeeCode(Long tenantId, String employeeCode);
    List<Employee> findByTenantIdAndDepartmentId(Long tenantId, Long departmentId);

    @Query("SELECT e FROM Employee e WHERE e.tenantId = :tenantId " +
           "AND (LOWER(e.firstName) LIKE LOWER(CONCAT('%',:query,'%')) " +
           "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%',:query,'%')) " +
           "OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%',:query,'%')))")
    Page<Employee> search(Long tenantId, String query, Pageable pageable);

    long countByTenantIdAndActiveTrue(Long tenantId);
}
