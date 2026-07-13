package com.hisobnoma.platform.mobile.repository;

import com.hisobnoma.platform.mobile.entity.MobileQuickSaleIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MobileQuickSaleIdempotencyRepository extends JpaRepository<MobileQuickSaleIdempotency, Long> {

    Optional<MobileQuickSaleIdempotency> findByTenantIdAndClientRequestId(Long tenantId, String clientRequestId);
}
