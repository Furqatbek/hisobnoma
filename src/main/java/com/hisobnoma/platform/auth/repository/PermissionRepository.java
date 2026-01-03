package com.hisobnoma.platform.auth.repository;

import com.hisobnoma.platform.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    List<Permission> findByModule(Permission.Module module);

    List<Permission> findByAction(Permission.Action action);

    @Query("SELECT p FROM Permission p WHERE p.code IN :codes")
    Set<Permission> findByCodeIn(@Param("codes") Set<String> codes);

    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId")
    Set<Permission> findAllByUserId(@Param("userId") Long userId);

    boolean existsByCode(String code);
}
