package com.hisobnoma.platform.distribution.repository;

import com.hisobnoma.platform.distribution.entity.DistributionAgentTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DistributionAgentTargetRepository extends JpaRepository<DistributionAgentTarget, Long> {

    @Query("SELECT t FROM DistributionAgentTarget t WHERE t.id = :id AND t.tenantId = :tenantId")
    Optional<DistributionAgentTarget> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM DistributionAgentTarget t WHERE t.tenantId = :tenantId AND t.agentId = :agentId " +
           "ORDER BY t.periodStart DESC")
    List<DistributionAgentTarget> findByTenantIdAndAgentId(@Param("tenantId") Long tenantId, @Param("agentId") Long agentId);

    @Query("SELECT t FROM DistributionAgentTarget t WHERE t.tenantId = :tenantId " +
           "AND t.periodStart = :start AND t.periodEnd = :end")
    List<DistributionAgentTarget> findByTenantIdAndPeriod(@Param("tenantId") Long tenantId,
                                                          @Param("start") LocalDate start, @Param("end") LocalDate end);

    boolean existsByTenantIdAndAgentIdAndPeriodStartAndPeriodEnd(Long tenantId, Long agentId, LocalDate start, LocalDate end);
}
