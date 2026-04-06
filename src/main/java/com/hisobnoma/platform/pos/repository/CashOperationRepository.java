package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.CashOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashOperationRepository extends JpaRepository<CashOperation, Long> {

    @Query("SELECT c FROM CashOperation c WHERE c.shift.id = :shiftId AND c.tenantId = :tenantId ORDER BY c.createdAt DESC")
    List<CashOperation> findByShiftIdAndTenantId(@Param("shiftId") Long shiftId, @Param("tenantId") Long tenantId);

    @Query("SELECT c FROM CashOperation c WHERE c.shift.id = :shiftId AND c.tenantId = :tenantId ORDER BY c.createdAt DESC")
    Page<CashOperation> findByShiftIdAndTenantId(@Param("shiftId") Long shiftId, @Param("tenantId") Long tenantId, Pageable pageable);
}
