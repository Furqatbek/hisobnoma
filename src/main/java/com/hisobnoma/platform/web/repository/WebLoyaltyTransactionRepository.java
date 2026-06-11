package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebLoyaltyTransaction;
import com.hisobnoma.platform.web.entity.WebLoyaltyTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WebLoyaltyTransactionRepository extends JpaRepository<WebLoyaltyTransaction, Long> {

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WebLoyaltyTransaction t " +
           "WHERE t.tenantId = :tenantId AND t.webCustomerId = :customerId " +
           "AND (t.expiresAt IS NULL OR t.expiresAt > :now)")
    BigDecimal balanceByCustomer(@Param("tenantId") Long tenantId,
                                 @Param("customerId") Long customerId,
                                 @Param("now") Instant now);

    @Query("SELECT t FROM WebLoyaltyTransaction t " +
           "WHERE t.tenantId = :tenantId AND t.webCustomerId = :customerId " +
           "ORDER BY t.createdAt DESC")
    Page<WebLoyaltyTransaction> findByCustomer(@Param("tenantId") Long tenantId,
                                                @Param("customerId") Long customerId,
                                                Pageable pageable);

    Optional<WebLoyaltyTransaction> findByTenantIdAndWebOrderIdAndType(
            Long tenantId, Long webOrderId, WebLoyaltyTransactionType type);

    List<WebLoyaltyTransaction> findByTenantIdAndWebOrderId(Long tenantId, Long webOrderId);

    @Query("SELECT t FROM WebLoyaltyTransaction t " +
           "WHERE t.type = 'EARN' AND t.expiresAt IS NOT NULL AND t.expiresAt <= :now " +
           "AND t.amount > 0")
    List<WebLoyaltyTransaction> findExpiredEarns(@Param("now") Instant now);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WebLoyaltyTransaction t " +
           "WHERE t.tenantId = :tenantId " +
           "AND (t.expiresAt IS NULL OR t.expiresAt > :now)")
    BigDecimal totalLiability(@Param("tenantId") Long tenantId,
                              @Param("now") Instant now);
}
