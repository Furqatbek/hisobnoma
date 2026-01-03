package com.hisobnoma.platform.auth.repository;

import com.hisobnoma.platform.auth.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    Optional<Role> findByCodeAndTenantId(String code, Long tenantId);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId OR r.tenantId IS NULL")
    Page<Role> findAllByTenantIdOrSystem(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT r FROM Role r WHERE r.tenantId IS NULL AND r.systemRole = true")
    List<Role> findAllSystemRoles();

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(@Param("id") Long id);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.code = :code")
    Optional<Role> findByCodeWithPermissions(@Param("code") String code);
}
