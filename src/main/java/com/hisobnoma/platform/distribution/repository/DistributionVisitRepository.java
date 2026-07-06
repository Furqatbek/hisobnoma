package com.hisobnoma.platform.distribution.repository;

import com.hisobnoma.platform.distribution.entity.DistributionVisit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface DistributionVisitRepository extends JpaRepository<DistributionVisit, Long> {

    @Query("SELECT v FROM DistributionVisit v WHERE v.id = :id AND v.tenantId = :tenantId")
    Optional<DistributionVisit> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT v FROM DistributionVisit v WHERE v.tenantId = :tenantId")
    Page<DistributionVisit> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT v FROM DistributionVisit v WHERE v.tenantId = :tenantId AND v.agentId = :agentId")
    Page<DistributionVisit> findByTenantIdAndAgentId(@Param("tenantId") Long tenantId,
                                                     @Param("agentId") Long agentId, Pageable pageable);

    @Query("SELECT v FROM DistributionVisit v WHERE v.tenantId = :tenantId AND v.routeId = :routeId")
    Page<DistributionVisit> findByTenantIdAndRouteId(@Param("tenantId") Long tenantId,
                                                     @Param("routeId") Long routeId, Pageable pageable);

    @Query("SELECT v FROM DistributionVisit v WHERE v.tenantId = :tenantId " +
           "AND v.checkInAt >= :from AND v.checkInAt < :to")
    Page<DistributionVisit> findByTenantIdAndCheckInBetween(@Param("tenantId") Long tenantId,
                                                            @Param("from") Instant from,
                                                            @Param("to") Instant to, Pageable pageable);
}
