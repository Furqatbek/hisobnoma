package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.POStatus;
import com.hisobnoma.platform.inventory.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("SELECT po FROM PurchaseOrder po WHERE po.id = :id AND (po.tenantId = :tenantId OR po.tenantId IS NULL)")
    Optional<PurchaseOrder> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.lines WHERE po.id = :id AND (po.tenantId = :tenantId OR po.tenantId IS NULL)")
    Optional<PurchaseOrder> findByIdWithLinesAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenantId = :tenantId OR po.tenantId IS NULL")
    Page<PurchaseOrder> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.poNumber = :poNumber AND (po.tenantId = :tenantId OR po.tenantId IS NULL)")
    Optional<PurchaseOrder> findByPoNumberAndTenantId(@Param("poNumber") String poNumber, @Param("tenantId") Long tenantId);

    @Query("SELECT po FROM PurchaseOrder po WHERE (po.tenantId = :tenantId OR po.tenantId IS NULL) AND po.status = :status")
    Page<PurchaseOrder> findByStatusAndTenantId(@Param("status") POStatus status, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE (po.tenantId = :tenantId OR po.tenantId IS NULL) AND po.vendor.id = :vendorId")
    Page<PurchaseOrder> findByVendorIdAndTenantId(@Param("vendorId") Long vendorId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE (po.tenantId = :tenantId OR po.tenantId IS NULL) AND po.status IN :statuses")
    Page<PurchaseOrder> findByStatusInAndTenantId(@Param("statuses") List<POStatus> statuses, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE (po.tenantId = :tenantId OR po.tenantId IS NULL) " +
           "AND po.orderDate BETWEEN :startDate AND :endDate")
    Page<PurchaseOrder> findByDateRangeAndTenantId(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("tenantId") Long tenantId,
            Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE (po.tenantId = :tenantId OR po.tenantId IS NULL) " +
           "AND po.status IN ('APPROVED', 'PARTIAL') " +
           "AND po.expectedDate <= :date")
    List<PurchaseOrder> findOverdueByTenantId(@Param("date") LocalDate date, @Param("tenantId") Long tenantId);

    @Query("SELECT po FROM PurchaseOrder po WHERE (po.tenantId = :tenantId OR po.tenantId IS NULL) AND " +
           "(LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(po.vendor.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(po.vendorReference) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PurchaseOrder> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT MAX(po.poNumber) FROM PurchaseOrder po WHERE po.tenantId = :tenantId OR po.tenantId IS NULL")
    String findMaxPoNumberByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByPoNumber(String poNumber);
}
