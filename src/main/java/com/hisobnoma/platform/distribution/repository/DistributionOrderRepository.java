package com.hisobnoma.platform.distribution.repository;

import com.hisobnoma.platform.distribution.entity.DistributionOrder;
import com.hisobnoma.platform.distribution.entity.DistributionOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributionOrderRepository extends JpaRepository<DistributionOrder, Long> {

    @Query("SELECT o FROM DistributionOrder o WHERE o.id = :id AND o.tenantId = :tenantId")
    Optional<DistributionOrder> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT o FROM DistributionOrder o WHERE o.tenantId = :tenantId")
    Page<DistributionOrder> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT o FROM DistributionOrder o WHERE o.tenantId = :tenantId AND o.status = :status")
    Page<DistributionOrder> findByTenantIdAndStatus(@Param("tenantId") Long tenantId,
                                                    @Param("status") DistributionOrderStatus status,
                                                    Pageable pageable);

    @Query("SELECT o FROM DistributionOrder o WHERE o.tenantId = :tenantId AND o.agentId = :agentId")
    Page<DistributionOrder> findByTenantIdAndAgentId(@Param("tenantId") Long tenantId,
                                                     @Param("agentId") Long agentId,
                                                     Pageable pageable);

    @Query("SELECT o FROM DistributionOrder o WHERE o.tenantId = :tenantId AND " +
           "(LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<DistributionOrder> searchByTenantId(@Param("tenantId") Long tenantId,
                                             @Param("search") String search, Pageable pageable);

    @Query("SELECT o FROM DistributionOrder o WHERE o.tenantId = :tenantId AND o.customerId = :customerId")
    Page<DistributionOrder> findByTenantIdAndCustomerId(@Param("tenantId") Long tenantId,
                                                        @Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT o FROM DistributionOrder o WHERE o.tenantId = :tenantId AND o.orderNumber = :orderNumber")
    Optional<DistributionOrder> findByTenantIdAndOrderNumber(@Param("tenantId") Long tenantId,
                                                             @Param("orderNumber") String orderNumber);

    boolean existsByTenantIdAndOrderNumber(Long tenantId, String orderNumber);

    @Query("SELECT MAX(o.orderNumber) FROM DistributionOrder o " +
           "WHERE o.tenantId = :tenantId AND o.orderNumber LIKE CONCAT(:prefix, '%')")
    String findMaxOrderNumberByPrefix(@Param("tenantId") Long tenantId, @Param("prefix") String prefix);

    /**
     * Per-agent order aggregates over a date range (non-cancelled orders only).
     * Each row: [agentId, orderCount, revenue, cashCollected, distinctCustomers].
     */
    @Query("SELECT o.agentId, COUNT(o), COALESCE(SUM(o.totalAmount), 0), " +
           "COALESCE(SUM(o.cashCollected), 0), COUNT(DISTINCT o.customerId) " +
           "FROM DistributionOrder o WHERE o.tenantId = :tenantId AND o.agentId IS NOT NULL " +
           "AND o.status <> :cancelled AND o.orderDate >= :from AND o.orderDate <= :to " +
           "GROUP BY o.agentId")
    List<Object[]> aggregateByAgent(@Param("tenantId") Long tenantId,
                                    @Param("cancelled") DistributionOrderStatus cancelled,
                                    @Param("from") java.time.LocalDate from,
                                    @Param("to") java.time.LocalDate to);

    /** Per-day order counts and revenue. Each row: [orderDate, orderCount, revenue]. */
    @Query("SELECT o.orderDate, COUNT(o), COALESCE(SUM(o.totalAmount), 0) FROM DistributionOrder o " +
           "WHERE o.tenantId = :tenantId AND o.status <> :cancelled " +
           "AND o.orderDate >= :from AND o.orderDate <= :to GROUP BY o.orderDate")
    List<Object[]> aggregateByDate(@Param("tenantId") Long tenantId,
                                   @Param("cancelled") DistributionOrderStatus cancelled,
                                   @Param("from") java.time.LocalDate from,
                                   @Param("to") java.time.LocalDate to);
}
