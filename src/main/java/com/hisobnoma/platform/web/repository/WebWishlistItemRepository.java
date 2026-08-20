package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebWishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebWishlistItemRepository extends JpaRepository<WebWishlistItem, Long> {

    Optional<WebWishlistItem> findByTenantIdAndWebCustomerIdAndCatalogItemId(
            Long tenantId, Long webCustomerId, Long catalogItemId);

    Page<WebWishlistItem> findByTenantIdAndWebCustomerIdOrderByCreatedAtDesc(
            Long tenantId, Long webCustomerId, Pageable pageable);

    @Query("SELECT w.catalogItemId FROM WebWishlistItem w " +
           "WHERE w.tenantId = :tenantId AND w.webCustomerId = :webCustomerId")
    List<Long> findCatalogItemIdsByTenantIdAndWebCustomerId(
            @Param("tenantId") Long tenantId, @Param("webCustomerId") Long webCustomerId);

    void deleteByTenantIdAndWebCustomerIdAndCatalogItemId(
            Long tenantId, Long webCustomerId, Long catalogItemId);

    @Modifying
    @Query("DELETE FROM WebWishlistItem w WHERE w.tenantId = :tenantId AND w.webCustomerId = :webCustomerId")
    void deleteAllByTenantIdAndWebCustomerId(
            @Param("tenantId") Long tenantId, @Param("webCustomerId") Long webCustomerId);

    long countByTenantIdAndCatalogItemId(Long tenantId, Long catalogItemId);

    long countByTenantIdAndWebCustomerId(Long tenantId, Long webCustomerId);

    @Query("SELECT w.catalogItemId, COUNT(w) FROM WebWishlistItem w " +
           "WHERE w.tenantId = :tenantId " +
           "GROUP BY w.catalogItemId ORDER BY COUNT(w) DESC")
    List<Object[]> findMostWished(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT DISTINCT w.catalogItemId FROM WebWishlistItem w " +
           "WHERE w.tenantId = :tenantId")
    List<Long> findDistinctCatalogItemIdsByTenantId(@Param("tenantId") Long tenantId);

    List<WebWishlistItem> findByTenantIdAndCatalogItemId(Long tenantId, Long catalogItemId);

    @Query("SELECT DISTINCT w.tenantId FROM WebWishlistItem w")
    List<Long> findDistinctTenantIds();
}
