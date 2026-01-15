package com.hisobnoma.platform.auth.repository;

import com.hisobnoma.platform.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId")
    Page<User> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "u.phone LIKE CONCAT('%', :search, '%'))")
    Page<User> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.username = :username")
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.phone = :phone")
    Optional<User> findByPhoneWithRolesAndPermissions(@Param("phone") String phone);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.id = :id")
    Optional<User> findByIdWithRolesAndPermissions(@Param("id") Long id);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndActiveTrue(Long tenantId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.createdAt >= :since")
    long countByTenantIdAndCreatedAtAfter(@Param("tenantId") Long tenantId, @Param("since") Instant since);
}
