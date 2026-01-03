package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.CountStatus;
import com.hisobnoma.platform.inventory.entity.InventoryCount;
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
public interface InventoryCountRepository extends JpaRepository<InventoryCount, Long> {

    @Query("SELECT ic FROM InventoryCount ic WHERE ic.id = :id AND (ic.tenantId = :tenantId OR ic.tenantId IS NULL)")
    Optional<InventoryCount> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT ic FROM InventoryCount ic LEFT JOIN FETCH ic.lines WHERE ic.id = :id AND (ic.tenantId = :tenantId OR ic.tenantId IS NULL)")
    Optional<InventoryCount> findByIdWithLinesAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT ic FROM InventoryCount ic WHERE ic.tenantId = :tenantId OR ic.tenantId IS NULL")
    Page<InventoryCount> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT ic FROM InventoryCount ic WHERE ic.countNumber = :countNumber AND (ic.tenantId = :tenantId OR ic.tenantId IS NULL)")
    Optional<InventoryCount> findByCountNumberAndTenantId(@Param("countNumber") String countNumber, @Param("tenantId") Long tenantId);

    @Query("SELECT ic FROM InventoryCount ic WHERE (ic.tenantId = :tenantId OR ic.tenantId IS NULL) AND ic.status = :status")
    Page<InventoryCount> findByStatusAndTenantId(@Param("status") CountStatus status, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT ic FROM InventoryCount ic WHERE (ic.tenantId = :tenantId OR ic.tenantId IS NULL) AND ic.location.id = :locationId")
    Page<InventoryCount> findByLocationIdAndTenantId(@Param("locationId") Long locationId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT ic FROM InventoryCount ic WHERE (ic.tenantId = :tenantId OR ic.tenantId IS NULL) AND ic.status IN :statuses")
    Page<InventoryCount> findByStatusInAndTenantId(@Param("statuses") List<CountStatus> statuses, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT ic FROM InventoryCount ic WHERE (ic.tenantId = :tenantId OR ic.tenantId IS NULL) " +
           "AND ic.countDate BETWEEN :startDate AND :endDate")
    Page<InventoryCount> findByDateRangeAndTenantId(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("tenantId") Long tenantId,
            Pageable pageable);

    @Query("SELECT ic FROM InventoryCount ic WHERE (ic.tenantId = :tenantId OR ic.tenantId IS NULL) AND " +
           "(LOWER(ic.countNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ic.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ic.location.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<InventoryCount> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT ic FROM InventoryCount ic WHERE (ic.tenantId = :tenantId OR ic.tenantId IS NULL) " +
           "AND ic.location.id = :locationId " +
           "AND ic.status IN ('DRAFT', 'IN_PROGRESS')")
    List<InventoryCount> findActiveCountsForLocation(@Param("locationId") Long locationId, @Param("tenantId") Long tenantId);

    @Query("SELECT MAX(ic.countNumber) FROM InventoryCount ic WHERE ic.tenantId = :tenantId OR ic.tenantId IS NULL")
    String findMaxCountNumberByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByCountNumber(String countNumber);
}
