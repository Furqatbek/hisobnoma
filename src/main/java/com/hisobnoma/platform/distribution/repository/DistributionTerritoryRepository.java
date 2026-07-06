package com.hisobnoma.platform.distribution.repository;

import com.hisobnoma.platform.distribution.entity.DistributionTerritory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistributionTerritoryRepository extends JpaRepository<DistributionTerritory, Long> {

    @Query("SELECT t FROM DistributionTerritory t WHERE t.tenantId = :tenantId AND t.regionId = :regionId AND t.active = true")
    List<DistributionTerritory> findActiveByRegion(@Param("tenantId") Long tenantId,
                                                   @Param("regionId") Long regionId);

    @Query("SELECT t FROM DistributionTerritory t WHERE t.tenantId = :tenantId AND t.agent.id = :agentId")
    List<DistributionTerritory> findByAgentId(@Param("tenantId") Long tenantId,
                                              @Param("agentId") Long agentId);
}
