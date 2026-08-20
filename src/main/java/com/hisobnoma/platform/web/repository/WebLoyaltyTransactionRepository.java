package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebLoyaltyTransaction;
import com.hisobnoma.platform.web.entity.WebLoyaltyTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WebLoyaltyTransactionRepository extends JpaRepository<WebLoyaltyTransaction, Long> {

    // Balance is a plain SUM of the append-only ledger. Expiry is applied ONLY by the nightly
    // job writing explicit EXPIRE debits (clamped to the unspent remainder) — filtering earns out
    // by expiresAt here double-counted once the job also wrote its debit, and worse, expired
    // points that were ALREADY SPENT, driving balances negative.
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WebLoyaltyTransaction t " +
           "WHERE t.tenantId = :tenantId AND t.webCustomerId = :customerId")
    BigDecimal balanceByCustomer(@Param("tenantId") Long tenantId,
                                 @Param("customerId") Long customerId);

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
           "WHERE t.tenantId = :tenantId")
    BigDecimal totalLiability(@Param("tenantId") Long tenantId);

    boolean existsByTenantIdAndWebCustomerIdAndTypeAndNote(
            Long tenantId, Long webCustomerId, WebLoyaltyTransactionType type, String note);

    long countByTenantIdAndWebCustomerIdAndTypeAndNoteStartingWithAndCreatedAtBetween(
            Long tenantId, Long webCustomerId, WebLoyaltyTransactionType type,
            String notePrefix, Instant start, Instant end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WebLoyaltyTransaction t " +
           "WHERE t.tenantId = :tenantId AND t.webCustomerId = :customerId " +
           "AND t.type = 'ADJUST' AND t.note LIKE 'Реферал%'")
    BigDecimal sumReferralRewards(@Param("tenantId") Long tenantId,
                                  @Param("customerId") Long customerId);

    @Modifying
    @Query("DELETE FROM WebLoyaltyTransaction t WHERE t.tenantId = :tenantId AND t.webCustomerId = :customerId")
    void deleteAllByTenantIdAndWebCustomerId(@Param("tenantId") Long tenantId,
                                             @Param("customerId") Long customerId);
}
