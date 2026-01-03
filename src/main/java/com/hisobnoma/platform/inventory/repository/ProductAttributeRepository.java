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

    @Query("SELECT a FROM ProductAttribute a WHERE a.product.id = :productId AND a.attributeName = :attributeName")
    Optional<ProductAttribute> findByProductIdAndAttributeName(
            @Param("productId") Long productId,
            @Param("attributeName") String attributeName);

    @Query("SELECT a FROM ProductAttribute a WHERE a.product.id = :productId AND a.attributeGroup = :group ORDER BY a.sortOrder")
    List<ProductAttribute> findByProductIdAndAttributeGroup(
            @Param("productId") Long productId,
            @Param("group") String group);

    @Query("SELECT DISTINCT a.attributeGroup FROM ProductAttribute a WHERE a.product.id = :productId AND a.attributeGroup IS NOT NULL")
    List<String> findDistinctGroupsByProductId(@Param("productId") Long productId);

    @Query("SELECT a FROM ProductAttribute a WHERE a.searchable = true AND a.attributeName = :name AND a.attributeValue = :value")
    List<ProductAttribute> findBySearchableAttribute(
            @Param("name") String name,
            @Param("value") String value);

    @Query("SELECT DISTINCT a.attributeName FROM ProductAttribute a WHERE a.filterable = true")
    List<String> findDistinctFilterableAttributeNames();

    @Query("SELECT DISTINCT a.attributeValue FROM ProductAttribute a WHERE a.attributeName = :name AND a.filterable = true")
    List<String> findDistinctFilterableValuesByAttributeName(@Param("name") String name);

    void deleteByProductId(Long productId);

    @Query("SELECT COUNT(a) FROM ProductAttribute a WHERE a.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);
}
