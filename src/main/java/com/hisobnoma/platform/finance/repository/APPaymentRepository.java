package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.APPayment;
import com.hisobnoma.platform.finance.entity.APPaymentStatus;
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
public interface APPaymentRepository extends JpaRepository<APPayment, Long> {

    Optional<APPayment> findByIdAndTenantId(Long id, Long tenantId);

    Optional<APPayment> findByPaymentNumberAndTenantId(String paymentNumber, Long tenantId);

    Page<APPayment> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<APPayment> findByTenantIdAndVendorId(Long tenantId, Long vendorId, Pageable pageable);

    Page<APPayment> findByTenantIdAndStatus(Long tenantId, APPaymentStatus status, Pageable pageable);

    List<APPayment> findByTenantIdAndVendorIdAndStatusIn(Long tenantId, Long vendorId, List<APPaymentStatus> statuses);

    @Query("SELECT p FROM APPayment p WHERE p.tenantId = :tenantId " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<APPayment> findByDateRange(@Param("tenantId") Long tenantId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM APPayment p " +
           "WHERE p.tenantId = :tenantId AND p.vendorId = :vendorId " +
           "AND p.status = :status")
    BigDecimal sumPaymentsByVendorAndStatus(@Param("tenantId") Long tenantId,
                                            @Param("vendorId") Long vendorId,
                                            @Param("status") APPaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM APPayment p " +
           "WHERE p.tenantId = :tenantId AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "AND p.status = :status")
    BigDecimal sumPaymentsByDateRangeAndStatus(@Param("tenantId") Long tenantId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("status") APPaymentStatus status);

    List<APPayment> findByTenantIdAndReconciledFalseAndStatus(Long tenantId, APPaymentStatus status);

    boolean existsByPaymentNumberAndTenantId(String paymentNumber, Long tenantId);

    @Query("SELECT MAX(CAST(SUBSTRING(p.paymentNumber, 5) AS integer)) FROM APPayment p " +
           "WHERE p.tenantId = :tenantId AND p.paymentNumber LIKE 'PAY-%'")
    Integer findMaxPaymentNumber(@Param("tenantId") Long tenantId);
}
