package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId OR c.tenantId IS NULL ORDER BY c.sortOrder")
    List<Category> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Category c WHERE (c.tenantId = :tenantId OR c.tenantId IS NULL) AND c.parent IS NULL ORDER BY c.sortOrder")
    List<Category> findRootCategoriesByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Category c WHERE (c.tenantId = :tenantId OR c.tenantId IS NULL) AND c.parent.id = :parentId ORDER BY c.sortOrder")
    List<Category> findChildrenByParentId(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);

    @Query("SELECT c FROM Category c WHERE (c.tenantId = :tenantId OR c.tenantId IS NULL) AND c.code = :code")
    Optional<Category> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Category c WHERE (c.tenantId = :tenantId OR c.tenantId IS NULL) AND c.active = true ORDER BY c.sortOrder")
    List<Category> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Category c WHERE (c.tenantId = :tenantId OR c.tenantId IS NULL) AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Category> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.code = :code AND c.tenantId IS NULL")
    boolean existsByCodeAndTenantIdIsNull(@Param("code") String code);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.id = :id")
    Optional<Category> findByIdWithChildren(@Param("id") Long id);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.tenantId = :tenantId OR c.tenantId IS NULL)")
    Optional<Category> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
