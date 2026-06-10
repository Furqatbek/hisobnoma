package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.enums.PromotionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Promotion> findByCodeAndTenantId(String code, Long tenantId);

    Page<Promotion> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE p.tenantId = :tenantId " +
           "AND (LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Promotion> searchByTenantId(@Param("query") String query, @Param("tenantId") Long tenantId, Pageable pageable);

    List<Promotion> findByTenantIdAndActive(Long tenantId, boolean active);

    List<Promotion> findByTenantIdAndType(Long tenantId, PromotionType type);

    @Query("SELECT p FROM Promotion p WHERE p.tenantId = :tenantId " +
           "AND p.active = true " +
           "AND p.requiresCoupon = false " +
           "AND (p.startDate IS NULL OR p.startDate <= :date) " +
           "AND (p.endDate IS NULL OR p.endDate >= :date) " +
           "AND (p.maxUses IS NULL OR p.currentUses < p.maxUses) " +
           "AND (p.locationId IS NULL OR p.locationId = :locationId) " +
           "ORDER BY p.priority DESC")
    List<Promotion> findActivePromotions(
            @Param("tenantId") Long tenantId,
            @Param("date") LocalDate date,
            @Param("locationId") Long locationId);

    /**
     * Same as {@link #findActivePromotions} but limited to the given sales
     * channels (callers pass the requested channel plus ALL).
     */
    @Query("SELECT p FROM Promotion p WHERE p.tenantId = :tenantId " +
           "AND p.active = true " +
           "AND p.requiresCoupon = false " +
           "AND p.channel IN :channels " +
           "AND (p.startDate IS NULL OR p.startDate <= :date) " +
           "AND (p.endDate IS NULL OR p.endDate >= :date) " +
           "AND (p.maxUses IS NULL OR p.currentUses < p.maxUses) " +
           "AND (p.locationId IS NULL OR p.locationId = :locationId) " +
           "ORDER BY p.priority DESC")
    List<Promotion> findActivePromotionsForChannels(
            @Param("tenantId") Long tenantId,
            @Param("date") LocalDate date,
            @Param("locationId") Long locationId,
            @Param("channels") List<PromotionChannel> channels);

    @Query("SELECT p FROM Promotion p WHERE p.tenantId = :tenantId " +
           "AND p.active = true " +
           "AND (p.startDate IS NULL OR p.startDate <= :date) " +
           "AND (p.endDate IS NULL OR p.endDate >= :date) " +
           "AND (p.maxUses IS NULL OR p.currentUses < p.maxUses) " +
           "ORDER BY p.priority DESC")
    List<Promotion> findAllActivePromotions(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.tenantId = :tenantId AND p.active = true")
    long countActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Promotion p WHERE p.tenantId = :tenantId " +
           "AND p.endDate < :date AND p.active = true")
    List<Promotion> findExpiredPromotions(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);
}
