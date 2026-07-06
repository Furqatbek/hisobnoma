package com.hisobnoma.platform.distribution.repository;

import com.hisobnoma.platform.distribution.entity.AgentStatus;
import com.hisobnoma.platform.distribution.entity.DistributionAgent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributionAgentRepository extends JpaRepository<DistributionAgent, Long> {

    @Query("SELECT a FROM DistributionAgent a WHERE a.id = :id AND a.tenantId = :tenantId")
    Optional<DistributionAgent> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT a FROM DistributionAgent a WHERE a.tenantId = :tenantId ORDER BY a.name")
    List<DistributionAgent> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT a FROM DistributionAgent a WHERE a.tenantId = :tenantId")
    Page<DistributionAgent> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT a FROM DistributionAgent a WHERE a.tenantId = :tenantId AND a.status = :status ORDER BY a.name")
    List<DistributionAgent> findAllByTenantIdAndStatus(@Param("tenantId") Long tenantId,
                                                       @Param("status") AgentStatus status);

    @Query("SELECT a FROM DistributionAgent a WHERE a.tenantId = :tenantId AND " +
           "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR a.phone LIKE CONCAT('%', :search, '%'))")
    Page<DistributionAgent> searchByTenantId(@Param("tenantId") Long tenantId,
                                             @Param("search") String search, Pageable pageable);

    boolean existsByCodeAndTenantId(String code, Long tenantId);
}
