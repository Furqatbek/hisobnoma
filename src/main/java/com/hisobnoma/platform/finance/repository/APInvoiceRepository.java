package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.APInvoice;
import com.hisobnoma.platform.finance.entity.APInvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface APInvoiceRepository extends JpaRepository<APInvoice, Long> {

    Optional<APInvoice> findByIdAndTenantId(Long id, Long tenantId);

    Optional<APInvoice> findByInvoiceNumberAndTenantId(String invoiceNumber, Long tenantId);

    Page<APInvoice> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<APInvoice> findByTenantIdAndVendorId(Long tenantId, Long vendorId, Pageable pageable);

    Page<APInvoice> findByTenantIdAndStatus(Long tenantId, APInvoiceStatus status, Pageable pageable);

    List<APInvoice> findByTenantIdAndVendorIdAndStatusIn(Long tenantId, Long vendorId, List<APInvoiceStatus> statuses);

    @Query("SELECT i FROM APInvoice i WHERE i.tenantId = :tenantId AND i.status IN :statuses " +
           "AND i.balanceDue > 0 ORDER BY i.dueDate ASC")
    List<APInvoice> findUnpaidInvoices(@Param("tenantId") Long tenantId,
                                        @Param("statuses") List<APInvoiceStatus> statuses);

    @Query("SELECT i FROM APInvoice i WHERE i.tenantId = :tenantId AND i.vendorId = :vendorId " +
           "AND i.status IN :statuses AND i.balanceDue > 0 ORDER BY i.dueDate ASC")
    List<APInvoice> findUnpaidInvoicesByVendor(@Param("tenantId") Long tenantId,
                                                @Param("vendorId") Long vendorId,
                                                @Param("statuses") List<APInvoiceStatus> statuses);

    @Query("SELECT i FROM APInvoice i WHERE i.tenantId = :tenantId AND i.dueDate < :date " +
           "AND i.status NOT IN :excludedStatuses AND i.balanceDue > 0")
    List<APInvoice> findOverdueInvoices(@Param("tenantId") Long tenantId,
                                        @Param("date") LocalDate date,
                                        @Param("excludedStatuses") List<APInvoiceStatus> excludedStatuses);

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM APInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.vendorId = :vendorId " +
           "AND i.status NOT IN :excludedStatuses")
    BigDecimal sumBalanceDueByVendor(@Param("tenantId") Long tenantId,
                                     @Param("vendorId") Long vendorId,
                                     @Param("excludedStatuses") List<APInvoiceStatus> excludedStatuses);

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM APInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.status NOT IN :excludedStatuses")
    BigDecimal sumTotalBalanceDue(@Param("tenantId") Long tenantId,
                                   @Param("excludedStatuses") List<APInvoiceStatus> excludedStatuses);

    // AP Aging queries
    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM APInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.dueDate >= :startDate AND i.dueDate < :endDate " +
           "AND i.status NOT IN :excludedStatuses AND i.balanceDue > 0")
    BigDecimal sumBalanceDueByDateRange(@Param("tenantId") Long tenantId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("excludedStatuses") List<APInvoiceStatus> excludedStatuses);

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM APInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.dueDate < :date " +
           "AND i.status NOT IN :excludedStatuses AND i.balanceDue > 0")
    BigDecimal sumOverdueBalance(@Param("tenantId") Long tenantId,
                                 @Param("date") LocalDate date,
                                 @Param("excludedStatuses") List<APInvoiceStatus> excludedStatuses);

    boolean existsByInvoiceNumberAndTenantId(String invoiceNumber, Long tenantId);

    boolean existsByPurchaseOrderIdAndTenantId(Long purchaseOrderId, Long tenantId);

    boolean existsByReceivingOrderIdAndTenantId(Long receivingOrderId, Long tenantId);

    Optional<APInvoice> findByReceivingOrderIdAndTenantId(Long receivingOrderId, Long tenantId);

    @Query("SELECT MAX(CAST(SUBSTRING(i.invoiceNumber, 4) AS integer)) FROM APInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.invoiceNumber LIKE 'AP-%'")
    Integer findMaxInvoiceNumber(@Param("tenantId") Long tenantId);
}
