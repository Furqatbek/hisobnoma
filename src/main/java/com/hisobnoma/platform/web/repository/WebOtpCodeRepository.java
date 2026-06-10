package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebOtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface WebOtpCodeRepository extends JpaRepository<WebOtpCode, Long> {

    Optional<WebOtpCode> findTopByTenantIdAndPhoneOrderByCreatedAtDesc(Long tenantId, String phone);

    Optional<WebOtpCode> findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(Long tenantId, String phone);

    long countByTenantIdAndPhoneAndCreatedAtAfter(Long tenantId, String phone, Instant since);
}
