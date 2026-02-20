package com.hisobnoma.platform.hr.repository;

import com.hisobnoma.platform.hr.entity.SalaryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {
    Page<SalaryRecord> findByTenantId(Long tenantId, Pageable pageable);
    Optional<SalaryRecord> findByIdAndTenantId(Long id, Long tenantId);
    List<SalaryRecord> findByTenantIdAndEmployeeIdOrderByPeriodYearDescPeriodMonthDesc(Long tenantId, Long employeeId);

    Page<SalaryRecord> findByTenantIdAndPeriodYearAndPeriodMonth(
            Long tenantId, Integer year, Integer month, Pageable pageable);

    boolean existsByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
            Long tenantId, Long employeeId, Integer year, Integer month);

    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM SalaryRecord s " +
           "WHERE s.tenantId = :tenantId AND s.periodYear = :year AND s.periodMonth = :month AND s.status = 'PAID'")
    BigDecimal sumPaidByPeriod(Long tenantId, Integer year, Integer month);
}
