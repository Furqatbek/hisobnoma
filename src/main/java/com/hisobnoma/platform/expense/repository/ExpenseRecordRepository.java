package com.hisobnoma.platform.expense.repository;

import com.hisobnoma.platform.expense.entity.ExpenseRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ExpenseRecordRepository extends JpaRepository<ExpenseRecord, Long> {

    Page<ExpenseRecord> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<ExpenseRecord> findByTenantIdAndCategory(Long tenantId, String category, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM ExpenseRecord e WHERE e.tenantId = :tenantId")
    BigDecimal sumTotalByTenantId(@Param("tenantId") Long tenantId);
}
