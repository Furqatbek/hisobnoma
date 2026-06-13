package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebPayment;
import com.hisobnoma.platform.web.entity.WebPaymentProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebPaymentRepository extends JpaRepository<WebPayment, Long> {

    Optional<WebPayment> findByPublicIdAndTenantId(String publicId, Long tenantId);

    /** Webhooks are unauthenticated; reference is unique per provider. */
    Optional<WebPayment> findByProviderAndProviderReference(
            WebPaymentProviderType provider, String providerReference);

    Optional<WebPayment> findTopByWebOrderIdAndTenantIdOrderByCreatedAtDesc(
            Long webOrderId, Long tenantId);
}
