package com.hisobnoma.platform.hr.repository;

import com.hisobnoma.platform.hr.entity.SalaryAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, Long> {

    Optional<SalaryAdvance> findByIdAndTenantId(Long id, Long tenantId);

    List<SalaryAdvance> findByTenantIdAndPeriodYearAndPeriodMonthOrderByAdvanceDateDesc(
            Long tenantId, Integer year, Integer month);

    List<SalaryAdvance> findByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonthAndStatus(
            Long tenantId, Long employeeId, Integer year, Integer month, SalaryAdvance.AdvanceStatus status);

    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM SalaryAdvance a " +
           "WHERE a.tenantId = :tenantId AND a.employee.id = :employeeId " +
           "AND a.periodYear = :year AND a.periodMonth = :month AND a.status = 'GIVEN'")
    BigDecimal sumGivenByEmployeeAndPeriod(Long tenantId, Long employeeId, Integer year, Integer month);

    List<SalaryAdvance> findByTenantIdAndEmployeeIdOrderByAdvanceDateDesc(Long tenantId, Long employeeId);
}
