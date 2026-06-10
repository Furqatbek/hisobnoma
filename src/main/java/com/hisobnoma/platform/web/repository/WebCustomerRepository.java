package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebCustomer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WebCustomerRepository extends JpaRepository<WebCustomer, Long> {

    Optional<WebCustomer> findByIdAndTenantId(Long id, Long tenantId);

    Optional<WebCustomer> findByTenantIdAndPhone(Long tenantId, String phone);

    long countByTenantId(Long tenantId);

    @Query("SELECT c FROM WebCustomer c WHERE c.tenantId = :tenantId ORDER BY c.lastLoginAt DESC NULLS LAST")
    Page<WebCustomer> findAllByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT c FROM WebCustomer c WHERE c.tenantId = :tenantId " +
           "AND (c.phone LIKE :pattern OR LOWER(c.name) LIKE :pattern) ORDER BY c.lastLoginAt DESC NULLS LAST")
    Page<WebCustomer> searchByTenant(@Param("tenantId") Long tenantId,
                                     @Param("pattern") String pattern,
                                     Pageable pageable);
}
