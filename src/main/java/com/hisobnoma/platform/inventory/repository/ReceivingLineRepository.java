package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.ReceivingLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivingLineRepository extends JpaRepository<ReceivingLine, Long> {

    @Query("SELECT rl FROM ReceivingLine rl WHERE rl.id = :id AND (rl.tenantId = :tenantId OR rl.tenantId IS NULL)")
    Optional<ReceivingLine> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT rl FROM ReceivingLine rl WHERE rl.receivingOrder.id = :receivingId AND (rl.tenantId = :tenantId OR rl.tenantId IS NULL) ORDER BY rl.lineNumber")
    List<ReceivingLine> findByReceivingOrderIdAndTenantId(@Param("receivingId") Long receivingId, @Param("tenantId") Long tenantId);

    @Query("SELECT rl FROM ReceivingLine rl WHERE rl.poLine.id = :poLineId AND (rl.tenantId = :tenantId OR rl.tenantId IS NULL)")
    List<ReceivingLine> findByPoLineIdAndTenantId(@Param("poLineId") Long poLineId, @Param("tenantId") Long tenantId);

    @Query("SELECT MAX(rl.lineNumber) FROM ReceivingLine rl WHERE rl.receivingOrder.id = :receivingId")
    Integer findMaxLineNumberByReceivingOrderId(@Param("receivingId") Long receivingId);

    @Query("SELECT SUM(rl.acceptedQuantity) FROM ReceivingLine rl WHERE rl.poLine.id = :poLineId AND rl.receivingOrder.status = 'COMPLETED'")
    java.math.BigDecimal getTotalReceivedQuantityForPoLine(@Param("poLineId") Long poLineId);
}
