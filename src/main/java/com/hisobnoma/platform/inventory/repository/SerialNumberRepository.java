package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.SerialNumber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface SerialNumberRepository extends JpaRepository<SerialNumber, Long> {

    Optional<SerialNumber> findByProductIdAndSerialNumberAndTenantId(Long productId, String serialNumber, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SerialNumber s WHERE s.product.id = :productId AND s.serialNumber = :serialNumber AND s.tenantId = :tenantId")
    Optional<SerialNumber> findWithLock(@Param("productId") Long productId,
                                         @Param("serialNumber") String serialNumber,
                                         @Param("tenantId") Long tenantId);

    Optional<SerialNumber> findBySerialNumberAndTenantId(String serialNumber, Long tenantId);

    Optional<SerialNumber> findByImeiAndTenantId(String imei, Long tenantId);

    List<SerialNumber> findByProductIdAndTenantId(Long productId, Long tenantId);

    List<SerialNumber> findByProductIdAndLocationIdAndTenantId(Long productId, Long locationId, Long tenantId);

    List<SerialNumber> findByProductIdAndLocationIdAndStatusAndTenantId(Long productId, Long locationId,
                                                                         SerialNumber.SerialStatus status, Long tenantId);

    @Query("SELECT s FROM SerialNumber s WHERE s.tenantId = :tenantId AND s.product.id = :productId " +
           "AND s.location.id = :locationId AND s.status = 'AVAILABLE' AND s.reserved = false")
    List<SerialNumber> findAvailableSerials(@Param("tenantId") Long tenantId,
                                             @Param("productId") Long productId,
                                             @Param("locationId") Long locationId);

    Page<SerialNumber> findByTenantIdAndStatus(Long tenantId, SerialNumber.SerialStatus status, Pageable pageable);

    Page<SerialNumber> findByLocationIdAndTenantId(Long locationId, Long tenantId, Pageable pageable);

    @Query("SELECT s FROM SerialNumber s WHERE s.tenantId = :tenantId AND " +
           "(LOWER(s.serialNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.imei) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.macAddress) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.product.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<SerialNumber> searchSerials(@Param("tenantId") Long tenantId, @Param("query") String query, Pageable pageable);

    boolean existsByProductIdAndSerialNumberAndTenantId(Long productId, String serialNumber, Long tenantId);

    boolean existsByImeiAndTenantId(String imei, Long tenantId);

    @Query("SELECT COUNT(s) FROM SerialNumber s WHERE s.tenantId = :tenantId AND s.product.id = :productId AND s.status = 'AVAILABLE'")
    Long countAvailableByProduct(@Param("tenantId") Long tenantId, @Param("productId") Long productId);

    List<SerialNumber> findByBatchIdAndTenantId(Long batchId, Long tenantId);

    @Query("SELECT s FROM SerialNumber s WHERE s.tenantId = :tenantId AND s.customerId = :customerId ORDER BY s.soldDate DESC")
    Page<SerialNumber> findByCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId, Pageable pageable);

    List<SerialNumber> findBySerialNumberInAndTenantId(List<String> serialNumbers, Long tenantId);
}
