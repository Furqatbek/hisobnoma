package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.ReceivingOrder;
import com.hisobnoma.platform.inventory.entity.ReceivingStatus;
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
public interface ReceivingOrderRepository extends JpaRepository<ReceivingOrder, Long> {

    @Query("SELECT ro FROM ReceivingOrder ro WHERE ro.id = :id AND (ro.tenantId = :tenantId OR ro.tenantId IS NULL)")
    Optional<ReceivingOrder> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT ro FROM ReceivingOrder ro LEFT JOIN FETCH ro.lines WHERE ro.id = :id AND (ro.tenantId = :tenantId OR ro.tenantId IS NULL)")
    Optional<ReceivingOrder> findByIdWithLinesAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT ro FROM ReceivingOrder ro WHERE ro.tenantId = :tenantId OR ro.tenantId IS NULL")
    Page<ReceivingOrder> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT ro FROM ReceivingOrder ro WHERE ro.receivingNumber = :receivingNumber AND (ro.tenantId = :tenantId OR ro.tenantId IS NULL)")
    Optional<ReceivingOrder> findByReceivingNumberAndTenantId(@Param("receivingNumber") String receivingNumber, @Param("tenantId") Long tenantId);

    @Query("SELECT ro FROM ReceivingOrder ro WHERE (ro.tenantId = :tenantId OR ro.tenantId IS NULL) AND ro.status = :status")
    Page<ReceivingOrder> findByStatusAndTenantId(@Param("status") ReceivingStatus status, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT ro FROM ReceivingOrder ro WHERE (ro.tenantId = :tenantId OR ro.tenantId IS NULL) AND ro.purchaseOrder.id = :poId")
    List<ReceivingOrder> findByPurchaseOrderIdAndTenantId(@Param("poId") Long poId, @Param("tenantId") Long tenantId);

    @Query("SELECT ro FROM ReceivingOrder ro WHERE (ro.tenantId = :tenantId OR ro.tenantId IS NULL) AND ro.vendor.id = :vendorId")
    Page<ReceivingOrder> findByVendorIdAndTenantId(@Param("vendorId") Long vendorId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT ro FROM ReceivingOrder ro WHERE (ro.tenantId = :tenantId OR ro.tenantId IS NULL) " +
           "AND ro.receivingDate BETWEEN :startDate AND :endDate")
    Page<ReceivingOrder> findByDateRangeAndTenantId(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("tenantId") Long tenantId,
            Pageable pageable);

    @Query("SELECT ro FROM ReceivingOrder ro WHERE (ro.tenantId = :tenantId OR ro.tenantId IS NULL) AND " +
           "(LOWER(ro.receivingNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ro.vendor.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ro.vendorDeliveryNote) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ro.vendorInvoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ReceivingOrder> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT MAX(ro.receivingNumber) FROM ReceivingOrder ro WHERE ro.tenantId = :tenantId OR ro.tenantId IS NULL")
    String findMaxReceivingNumberByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByReceivingNumber(String receivingNumber);
}
