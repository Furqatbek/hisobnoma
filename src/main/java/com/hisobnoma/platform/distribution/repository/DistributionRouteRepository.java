package com.hisobnoma.platform.distribution.repository;

import com.hisobnoma.platform.distribution.entity.DistributionRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributionRouteRepository extends JpaRepository<DistributionRoute, Long> {

    @Query("SELECT r FROM DistributionRoute r WHERE r.id = :id AND r.tenantId = :tenantId")
    Optional<DistributionRoute> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT r FROM DistributionRoute r WHERE r.tenantId = :tenantId")
    Page<DistributionRoute> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT r FROM DistributionRoute r WHERE r.tenantId = :tenantId AND r.agentId = :agentId ORDER BY r.name")
    List<DistributionRoute> findByTenantIdAndAgentId(@Param("tenantId") Long tenantId, @Param("agentId") Long agentId);

    @Query("SELECT r FROM DistributionRoute r WHERE r.tenantId = :tenantId AND " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<DistributionRoute> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    boolean existsByCodeAndTenantId(String code, Long tenantId);
}
