package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.ARPayment;
import com.hisobnoma.platform.finance.entity.ARPaymentStatus;
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
public interface ARPaymentRepository extends JpaRepository<ARPayment, Long> {

    Optional<ARPayment> findByIdAndTenantId(Long id, Long tenantId);

    Optional<ARPayment> findByPaymentNumberAndTenantId(String paymentNumber, Long tenantId);

    Page<ARPayment> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<ARPayment> findByTenantIdAndCustomer_Id(Long tenantId, Long customerId, Pageable pageable);

    Page<ARPayment> findByTenantIdAndStatus(Long tenantId, ARPaymentStatus status, Pageable pageable);

    List<ARPayment> findByTenantIdAndCustomer_IdAndStatusIn(Long tenantId, Long customerId, List<ARPaymentStatus> statuses);

    @Query("SELECT p FROM ARPayment p WHERE p.tenantId = :tenantId " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<ARPayment> findByDateRange(@Param("tenantId") Long tenantId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM ARPayment p " +
           "WHERE p.tenantId = :tenantId AND p.customer.id = :customerId " +
           "AND p.status = :status")
    BigDecimal sumPaymentsByCustomerAndStatus(@Param("tenantId") Long tenantId,
                                              @Param("customerId") Long customerId,
                                              @Param("status") ARPaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM ARPayment p " +
           "WHERE p.tenantId = :tenantId AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "AND p.status = :status")
    BigDecimal sumPaymentsByDateRangeAndStatus(@Param("tenantId") Long tenantId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("status") ARPaymentStatus status);

    List<ARPayment> findByTenantIdAndDepositedFalseAndStatus(Long tenantId, ARPaymentStatus status);

    boolean existsByPaymentNumberAndTenantId(String paymentNumber, Long tenantId);

    @Query("SELECT MAX(CAST(SUBSTRING(p.paymentNumber, 5) AS integer)) FROM ARPayment p " +
           "WHERE p.tenantId = :tenantId AND p.paymentNumber LIKE 'REC-%'")
    Integer findMaxPaymentNumber(@Param("tenantId") Long tenantId);
}
