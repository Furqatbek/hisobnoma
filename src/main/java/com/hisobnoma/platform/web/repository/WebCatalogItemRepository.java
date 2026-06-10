package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebCatalogItemRepository extends JpaRepository<WebCatalogItem, Long> {

    Optional<WebCatalogItem> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndProductId(Long tenantId, Long productId);

    long countByTenantIdAndStatus(Long tenantId, WebCatalogStatus status);

    List<WebCatalogItem> findByTenantIdOrderBySortOrderAsc(Long tenantId);

    @Query("SELECT i FROM WebCatalogItem i WHERE i.tenantId = :tenantId ORDER BY i.sortOrder ASC")
    Page<WebCatalogItem> findAllByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT i FROM WebCatalogItem i JOIN i.product p WHERE i.tenantId = :tenantId " +
           "AND (LOWER(p.name) LIKE :pattern OR LOWER(p.sku) LIKE :pattern OR LOWER(i.displayName) LIKE :pattern) " +
           "ORDER BY i.sortOrder ASC")
    Page<WebCatalogItem> searchByTenant(@Param("tenantId") Long tenantId,
                                        @Param("pattern") String pattern,
                                        Pageable pageable);

    /**
     * Items visible in the online shop: given status with an active, sellable product.
     */
    @Query("SELECT i FROM WebCatalogItem i JOIN i.product p WHERE i.tenantId = :tenantId " +
           "AND i.status = :status AND p.active = true AND p.sellable = true ORDER BY i.sortOrder ASC")
    Page<WebCatalogItem> findVisible(@Param("tenantId") Long tenantId,
                                     @Param("status") WebCatalogStatus status,
                                     Pageable pageable);

    @Query("SELECT i FROM WebCatalogItem i JOIN i.product p WHERE i.tenantId = :tenantId " +
           "AND i.status = :status AND p.active = true AND p.sellable = true " +
           "AND p.category.id = :categoryId ORDER BY i.sortOrder ASC")
    Page<WebCatalogItem> findVisibleByCategory(@Param("tenantId") Long tenantId,
                                               @Param("status") WebCatalogStatus status,
                                               @Param("categoryId") Long categoryId,
                                               Pageable pageable);

    @Query("SELECT i FROM WebCatalogItem i JOIN i.product p WHERE i.tenantId = :tenantId " +
           "AND i.status = :status AND p.active = true AND p.sellable = true " +
           "AND (LOWER(p.name) LIKE :pattern OR LOWER(i.displayName) LIKE :pattern) " +
           "ORDER BY i.sortOrder ASC")
    Page<WebCatalogItem> searchVisible(@Param("tenantId") Long tenantId,
                                       @Param("status") WebCatalogStatus status,
                                       @Param("pattern") String pattern,
                                       Pageable pageable);

    @Query("SELECT DISTINCT c.id, c.name FROM WebCatalogItem i JOIN i.product p JOIN p.category c " +
           "WHERE i.tenantId = :tenantId AND i.status = :status AND p.active = true AND p.sellable = true " +
           "ORDER BY c.name ASC")
    List<Object[]> findVisibleCategories(@Param("tenantId") Long tenantId,
                                         @Param("status") WebCatalogStatus status);

    Optional<WebCatalogItem> findFirstByTenantIdAndSortOrderLessThanOrderBySortOrderDesc(Long tenantId, Integer sortOrder);

    Optional<WebCatalogItem> findFirstByTenantIdAndSortOrderGreaterThanOrderBySortOrderAsc(Long tenantId, Integer sortOrder);

    @Query("SELECT COALESCE(MAX(i.sortOrder), 0) FROM WebCatalogItem i WHERE i.tenantId = :tenantId")
    int findMaxSortOrder(@Param("tenantId") Long tenantId);
}
