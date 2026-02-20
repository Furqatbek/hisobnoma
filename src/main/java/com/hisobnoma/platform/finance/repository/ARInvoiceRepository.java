package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
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
public interface ARInvoiceRepository extends JpaRepository<ARInvoice, Long> {

    Optional<ARInvoice> findByIdAndTenantId(Long id, Long tenantId);

    Optional<ARInvoice> findByInvoiceNumberAndTenantId(String invoiceNumber, Long tenantId);

    Page<ARInvoice> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<ARInvoice> findByTenantIdAndCustomer_Id(Long tenantId, Long customerId, Pageable pageable);

    Page<ARInvoice> findByTenantIdAndStatus(Long tenantId, ARInvoiceStatus status, Pageable pageable);

    List<ARInvoice> findByTenantIdAndCustomer_IdAndStatusIn(Long tenantId, Long customerId, List<ARInvoiceStatus> statuses);

    @Query("SELECT i FROM ARInvoice i WHERE i.tenantId = :tenantId AND i.status IN :statuses " +
           "AND i.balanceDue > 0 ORDER BY i.dueDate ASC")
    List<ARInvoice> findUnpaidInvoices(@Param("tenantId") Long tenantId,
                                        @Param("statuses") List<ARInvoiceStatus> statuses);

    @Query("SELECT i FROM ARInvoice i WHERE i.tenantId = :tenantId AND i.customer.id = :customerId " +
           "AND i.status IN :statuses AND i.balanceDue > 0 ORDER BY i.dueDate ASC")
    List<ARInvoice> findUnpaidByCustomer(@Param("tenantId") Long tenantId,
                                         @Param("customerId") Long customerId,
                                         @Param("statuses") List<ARInvoiceStatus> statuses);

    @Query("SELECT i FROM ARInvoice i WHERE i.tenantId = :tenantId AND i.dueDate < :date " +
           "AND i.status IN :statuses AND i.balanceDue > 0")
    List<ARInvoice> findOverdueInvoices(@Param("tenantId") Long tenantId,
                                        @Param("date") LocalDate date,
                                        @Param("statuses") List<ARInvoiceStatus> statuses);

    List<ARInvoice> findByTenantIdAndStatusIn(Long tenantId, List<ARInvoiceStatus> statuses);

    List<ARInvoice> findByTenantIdAndDueDateBeforeAndStatusIn(Long tenantId, LocalDate dueDate, List<ARInvoiceStatus> statuses);

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM ARInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.customer.id = :customerId " +
           "AND i.status IN :statuses")
    BigDecimal sumBalanceDueByCustomer(@Param("tenantId") Long tenantId,
                                       @Param("customerId") Long customerId,
                                       @Param("statuses") List<ARInvoiceStatus> statuses);

    @Query("SELECT MAX(i.invoiceDate) FROM ARInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.customer.id = :customerId")
    LocalDate findLatestInvoiceDateByCustomer(@Param("tenantId") Long tenantId,
                                              @Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM ARInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.status NOT IN :excludedStatuses")
    BigDecimal sumTotalBalanceDue(@Param("tenantId") Long tenantId,
                                   @Param("excludedStatuses") List<ARInvoiceStatus> excludedStatuses);

    // AR Aging queries
    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM ARInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.dueDate >= :startDate AND i.dueDate < :endDate " +
           "AND i.status NOT IN :excludedStatuses AND i.balanceDue > 0")
    BigDecimal sumBalanceDueByDateRange(@Param("tenantId") Long tenantId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("excludedStatuses") List<ARInvoiceStatus> excludedStatuses);

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM ARInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.dueDate < :date " +
           "AND i.status NOT IN :excludedStatuses AND i.balanceDue > 0")
    BigDecimal sumOverdueBalance(@Param("tenantId") Long tenantId,
                                 @Param("date") LocalDate date,
                                 @Param("excludedStatuses") List<ARInvoiceStatus> excludedStatuses);

    boolean existsByInvoiceNumberAndTenantId(String invoiceNumber, Long tenantId);

    boolean existsByPosTransactionIdAndTenantId(Long posTransactionId, Long tenantId);

    Optional<ARInvoice> findByPosTransactionIdAndTenantId(Long posTransactionId, Long tenantId);

    @Query("SELECT MAX(CAST(SUBSTRING(i.invoiceNumber, 5) AS integer)) FROM ARInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.invoiceNumber LIKE 'INV-%'")
    Integer findMaxInvoiceNumber(@Param("tenantId") Long tenantId);

    // Mobile module method
    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM ARInvoice i " +
           "WHERE i.tenantId = :tenantId AND i.status NOT IN :excludedStatuses")
    BigDecimal sumOutstandingBalance(@Param("tenantId") Long tenantId,
                                     @Param("excludedStatuses") List<ARInvoiceStatus> excludedStatuses);
}
