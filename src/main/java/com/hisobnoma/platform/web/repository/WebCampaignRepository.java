package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebCampaignRepository extends JpaRepository<WebCampaign, Long> {

    Optional<WebCampaign> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT c FROM WebCampaign c WHERE c.tenantId = :tenantId ORDER BY c.createdAt DESC")
    Page<WebCampaign> findAllByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT c FROM WebCampaign c WHERE c.tenantId = :tenantId ORDER BY c.createdAt DESC")
    List<WebCampaign> findRecentByTenant(@Param("tenantId") Long tenantId, Pageable pageable);
}
