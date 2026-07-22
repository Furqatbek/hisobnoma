package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {

    List<ProductAttribute> findByProductIdOrderBySortOrder(Long productId);

    List<ProductAttribute> findByProductIdAndVisibleTrueOrderBySortOrder(Long productId);

    void deleteByProductId(Long productId);

    @Query("SELECT COUNT(a) FROM ProductAttribute a WHERE a.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);
}
