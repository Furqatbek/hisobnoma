package com.hisobnoma.platform.sms.repository;

import com.hisobnoma.platform.sms.entity.SmsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, Long> {

    List<SmsTemplate> findByTenantIdOrderByNameAsc(Long tenantId);

    List<SmsTemplate> findByTenantIdAndActiveTrueOrderByNameAsc(Long tenantId);

    Optional<SmsTemplate> findByTenantIdAndId(Long tenantId, Long id);

    Optional<SmsTemplate> findByTenantIdAndCode(Long tenantId, String code);

    boolean existsByTenantIdAndCode(Long tenantId, String code);
}
