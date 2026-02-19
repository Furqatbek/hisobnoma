package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.POSTransaction;
import com.hisobnoma.platform.pos.entity.TransactionStatus;
import com.hisobnoma.platform.pos.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface POSTransactionRepository extends JpaRepository<POSTransaction, Long> {

    @Query("SELECT t FROM POSTransaction t WHERE t.tenantId = :tenantId ORDER BY t.createdAt DESC")
    Page<POSTransaction> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"terminal", "terminal.location", "shift", "customer"})
    @Query("SELECT t FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.id = :id")
    Optional<POSTransaction> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @EntityGraph(attributePaths = {"terminal", "terminal.location", "shift", "customer"})
    @Query("SELECT t FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.transactionNumber = :number")
    Optional<POSTransaction> findByTransactionNumberAndTenantId(@Param("number") String number, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.shift.id = :shiftId ORDER BY t.createdAt DESC")
    List<POSTransaction> findByShiftIdAndTenantId(@Param("shiftId") Long shiftId, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.terminal.id = :terminalId ORDER BY t.createdAt DESC")
    Page<POSTransaction> findByTerminalIdAndTenantId(@Param("terminalId") Long terminalId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT t FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.status = :status ORDER BY t.createdAt DESC")
    List<POSTransaction> findByStatusAndTenantId(@Param("status") TransactionStatus status, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.shift.id = :shiftId AND t.status = :status")
    List<POSTransaction> findByShiftIdAndStatusAndTenantId(
            @Param("shiftId") Long shiftId,
            @Param("status") TransactionStatus status,
            @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.customer.id = :customerId ORDER BY t.createdAt DESC")
    Page<POSTransaction> findByCustomerIdAndTenantId(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT t FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.completedAt >= :startDate AND t.completedAt < :endDate")
    List<POSTransaction> findByDateRangeAndTenantId(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("tenantId") Long tenantId);

    @Query("SELECT SUM(t.totalAmount) FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.shift.id = :shiftId AND t.status = 'COMPLETED' AND t.transactionType = 'SALE'")
    BigDecimal sumSalesByShiftId(@Param("shiftId") Long shiftId, @Param("tenantId") Long tenantId);

    @Query("SELECT SUM(t.totalAmount) FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.shift.id = :shiftId AND t.status = 'COMPLETED' AND t.transactionType = 'RETURN'")
    BigDecimal sumReturnsByShiftId(@Param("shiftId") Long shiftId, @Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(t) FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.shift.id = :shiftId AND t.status = 'COMPLETED'")
    Long countCompletedByShiftId(@Param("shiftId") Long shiftId, @Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(t) FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.shift.id = :shiftId AND t.status = 'VOIDED'")
    Long countVoidedByShiftId(@Param("shiftId") Long shiftId, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM POSTransaction t WHERE t.tenantId = :tenantId AND " +
           "(LOWER(t.transactionNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.customerPhone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.createdAt DESC")
    Page<POSTransaction> searchByTenantId(@Param("search") String search, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT t FROM POSTransaction t WHERE t.tenantId = :tenantId AND t.transactionType = :type " +
           "AND t.completedAt >= :startDate AND t.completedAt < :endDate AND t.status = 'COMPLETED'")
    List<POSTransaction> findByTypeAndDateRangeAndTenantId(
            @Param("type") TransactionType type,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("tenantId") Long tenantId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM POSTransaction t " +
           "WHERE t.tenantId = :tenantId AND t.transactionNumber = :number")
    boolean existsByTransactionNumberAndTenantId(@Param("number") String number, @Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(t) FROM POSTransaction t WHERE t.customer.id = :customerId AND t.tenantId = :tenantId AND t.status = :status")
    long countByCustomerIdAndTenantIdAndStatus(
            @Param("customerId") Long customerId,
            @Param("tenantId") Long tenantId,
            @Param("status") TransactionStatus status);

    // Mobile dashboard methods
    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM POSTransaction t WHERE t.tenantId = :tenantId " +
           "AND t.status = 'COMPLETED' AND t.transactionType = 'SALE' " +
           "AND t.completedAt >= :startDate AND t.completedAt < :endDate")
    BigDecimal sumCompletedSalesByDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(t) FROM POSTransaction t WHERE t.tenantId = :tenantId " +
           "AND t.status = 'COMPLETED' AND t.transactionType = 'SALE' " +
           "AND t.completedAt >= :startDate AND t.completedAt < :endDate")
    Integer countCompletedSalesByDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
