package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrder(Long productId);

    List<ProductImage> findByProductIdAndActiveTrueOrderBySortOrder(Long productId);

    Optional<ProductImage> findByProductIdAndPrimaryTrue(Long productId);

    @Modifying
    @Query("UPDATE ProductImage i SET i.primary = false WHERE i.product.id = :productId")
    void clearPrimaryForProduct(@Param("productId") Long productId);

    @Query("SELECT COUNT(i) FROM ProductImage i WHERE i.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);

    @Query("SELECT i.product.id, i.imageUrl FROM ProductImage i WHERE i.primary = true AND i.product.id IN :productIds")
    List<Object[]> findPrimaryImageUrlsByProductIds(@Param("productIds") List<Long> productIds);

    void deleteByProductId(Long productId);
}
