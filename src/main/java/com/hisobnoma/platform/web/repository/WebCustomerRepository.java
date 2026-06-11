package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebCustomer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WebCustomerRepository extends JpaRepository<WebCustomer, Long> {

    Optional<WebCustomer> findByIdAndTenantId(Long id, Long tenantId);

    Optional<WebCustomer> findByTenantIdAndPhone(Long tenantId, String phone);

    long countByTenantId(Long tenantId);

    @Query("SELECT c FROM WebCustomer c WHERE c.tenantId = :tenantId ORDER BY c.lastLoginAt DESC NULLS LAST")
    Page<WebCustomer> findAllByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT c FROM WebCustomer c WHERE c.tenantId = :tenantId " +
           "AND (c.phone LIKE :pattern OR LOWER(c.name) LIKE :pattern) ORDER BY c.lastLoginAt DESC NULLS LAST")
    Page<WebCustomer> searchByTenant(@Param("tenantId") Long tenantId,
                                     @Param("pattern") String pattern,
                                     Pageable pageable);

    // ---- campaign segments (always exclude opted-out customers) ----

    @Query("SELECT c FROM WebCustomer c WHERE c.tenantId = :tenantId AND c.smsOptOut = false")
    List<WebCustomer> segmentAll(@Param("tenantId") Long tenantId);

    @Query("SELECT c FROM WebCustomer c WHERE c.tenantId = :tenantId AND c.smsOptOut = false " +
           "AND EXISTS (SELECT 1 FROM WebOrder o WHERE o.tenantId = c.tenantId " +
           "            AND o.phoneNormalized = c.phone AND o.createdAt >= :cutoff)")
    List<WebCustomer> segmentOrderedSince(@Param("tenantId") Long tenantId,
                                          @Param("cutoff") Instant cutoff);

    @Query("SELECT c FROM WebCustomer c WHERE c.tenantId = :tenantId AND c.smsOptOut = false " +
           "AND EXISTS (SELECT 1 FROM WebOrder o WHERE o.tenantId = c.tenantId AND o.phoneNormalized = c.phone) " +
           "AND NOT EXISTS (SELECT 1 FROM WebOrder o2 WHERE o2.tenantId = c.tenantId " +
           "                AND o2.phoneNormalized = c.phone AND o2.createdAt >= :cutoff)")
    List<WebCustomer> segmentNoOrderSince(@Param("tenantId") Long tenantId,
                                          @Param("cutoff") Instant cutoff);

    @Query("SELECT c FROM WebCustomer c WHERE c.tenantId = :tenantId AND c.smsOptOut = false " +
           "AND NOT EXISTS (SELECT 1 FROM WebOrder o WHERE o.tenantId = c.tenantId AND o.phoneNormalized = c.phone)")
    List<WebCustomer> segmentNeverOrdered(@Param("tenantId") Long tenantId);

    @Query("SELECT c FROM WebCustomer c WHERE c.tenantId = :tenantId AND c.smsOptOut = false " +
           "AND (SELECT COALESCE(SUM(o.totalAmount), 0) FROM WebOrder o WHERE o.tenantId = c.tenantId " +
           "     AND o.phoneNormalized = c.phone " +
           "     AND o.status = com.hisobnoma.platform.web.entity.WebOrderStatus.COMPLETED) >= :amount")
    List<WebCustomer> segmentMinSpent(@Param("tenantId") Long tenantId,
                                      @Param("amount") BigDecimal amount);

    boolean existsByReferralCode(String referralCode);

    Optional<WebCustomer> findByReferralCode(String referralCode);

    long countByTenantIdAndReferredBy(Long tenantId, Long referredBy);

    long countByTenantIdAndReferredByIsNotNull(Long tenantId);
}
