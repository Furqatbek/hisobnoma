package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {

    @Query("SELECT pol FROM PurchaseOrderLine pol WHERE pol.id = :id AND (pol.tenantId = :tenantId OR pol.tenantId IS NULL)")
    Optional<PurchaseOrderLine> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT pol FROM PurchaseOrderLine pol WHERE pol.purchaseOrder.id = :poId AND (pol.tenantId = :tenantId OR pol.tenantId IS NULL) ORDER BY pol.lineNumber")
    List<PurchaseOrderLine> findByPurchaseOrderIdAndTenantId(@Param("poId") Long poId, @Param("tenantId") Long tenantId);

    @Query("SELECT pol FROM PurchaseOrderLine pol WHERE pol.purchaseOrder.id = :poId AND pol.product.id = :productId AND (pol.tenantId = :tenantId OR pol.tenantId IS NULL)")
    Optional<PurchaseOrderLine> findByPurchaseOrderIdAndProductIdAndTenantId(
            @Param("poId") Long poId,
            @Param("productId") Long productId,
            @Param("tenantId") Long tenantId);

    @Query("SELECT pol FROM PurchaseOrderLine pol WHERE pol.purchaseOrder.id = :poId " +
           "AND pol.receivedQuantity < pol.quantity " +
           "AND (pol.tenantId = :tenantId OR pol.tenantId IS NULL)")
    List<PurchaseOrderLine> findPendingLinesByPurchaseOrderId(@Param("poId") Long poId, @Param("tenantId") Long tenantId);

    @Query("SELECT MAX(pol.lineNumber) FROM PurchaseOrderLine pol WHERE pol.purchaseOrder.id = :poId")
    Integer findMaxLineNumberByPurchaseOrderId(@Param("poId") Long poId);
}
