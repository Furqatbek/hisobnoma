package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.ProductUom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductUomRepository extends JpaRepository<ProductUom, Long> {

    List<ProductUom> findByProductIdAndTenantIdOrderBySortOrderAsc(Long productId, Long tenantId);

    List<ProductUom> findByProductIdAndActiveTrueAndTenantIdOrderBySortOrderAsc(Long productId, Long tenantId);

    Optional<ProductUom> findByIdAndTenantId(Long id, Long tenantId);

    Optional<ProductUom> findByProductIdAndUomIdAndTenantId(Long productId, Long uomId, Long tenantId);

    @Query("SELECT pu FROM ProductUom pu WHERE pu.product.id = :productId AND pu.defaultSale = true AND pu.tenantId = :tenantId")
    Optional<ProductUom> findDefaultSaleUom(Long productId, Long tenantId);

    boolean existsByProductIdAndUomIdAndTenantId(Long productId, Long uomId, Long tenantId);
}
