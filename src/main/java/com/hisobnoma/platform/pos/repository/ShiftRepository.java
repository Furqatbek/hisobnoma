package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.Shift;
import com.hisobnoma.platform.pos.entity.ShiftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    @Query("SELECT s FROM Shift s WHERE s.tenantId = :tenantId")
    Page<Shift> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT s FROM Shift s WHERE s.tenantId = :tenantId AND s.id = :id")
    Optional<Shift> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Shift s WHERE s.tenantId = :tenantId AND s.terminal.id = :terminalId AND s.status = :status")
    Optional<Shift> findByTerminalIdAndStatusAndTenantId(
            @Param("terminalId") Long terminalId,
            @Param("status") ShiftStatus status,
            @Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Shift s WHERE s.tenantId = :tenantId AND s.terminal.id = :terminalId ORDER BY s.openedAt DESC")
    Page<Shift> findByTerminalIdAndTenantId(@Param("terminalId") Long terminalId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT s FROM Shift s WHERE s.tenantId = :tenantId AND s.cashierId = :cashierId ORDER BY s.openedAt DESC")
    Page<Shift> findByCashierIdAndTenantId(@Param("cashierId") Long cashierId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT s FROM Shift s WHERE s.tenantId = :tenantId AND s.cashierId = :cashierId AND s.status = :status")
    Optional<Shift> findByCashierIdAndStatusAndTenantId(
            @Param("cashierId") Long cashierId,
            @Param("status") ShiftStatus status,
            @Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Shift s WHERE s.tenantId = :tenantId AND s.openedAt >= :startDate AND s.openedAt < :endDate")
    List<Shift> findByDateRangeAndTenantId(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Shift s WHERE s.tenantId = :tenantId AND s.status = 'OPEN'")
    List<Shift> findOpenShiftsByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Shift s WHERE s.tenantId = :tenantId AND s.shiftNumber = :shiftNumber")
    Optional<Shift> findByShiftNumberAndTenantId(@Param("shiftNumber") String shiftNumber, @Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(s) FROM Shift s WHERE s.tenantId = :tenantId AND s.openedAt >= :startDate AND s.openedAt < :endDate")
    Long countByDateRangeAndTenantId(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("tenantId") Long tenantId);
}
