package com.hisobnoma.platform.distribution.repository;

import com.hisobnoma.platform.distribution.entity.DistributionAgentOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface DistributionAgentOtpRepository extends JpaRepository<DistributionAgentOtp, Long> {

    Optional<DistributionAgentOtp> findTopByTenantIdAndPhoneOrderByCreatedAtDesc(Long tenantId, String phone);

    Optional<DistributionAgentOtp> findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(Long tenantId, String phone);

    long countByTenantIdAndPhoneAndCreatedAtAfter(Long tenantId, String phone, Instant since);
}
