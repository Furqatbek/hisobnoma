package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.JournalLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    @Query("SELECT jl FROM JournalLine jl WHERE jl.journalEntry.id = :entryId ORDER BY jl.lineNumber")
    List<JournalLine> findByJournalEntryId(@Param("entryId") Long entryId);

    @Query("SELECT jl FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.tenantId = :tenantId ORDER BY jl.journalEntry.entryDate DESC")
    Page<JournalLine> findByAccountIdAndTenantId(@Param("accountId") Long accountId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT jl FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.tenantId = :tenantId " +
           "AND jl.journalEntry.entryDate BETWEEN :startDate AND :endDate AND jl.journalEntry.status = 'POSTED' " +
           "ORDER BY jl.journalEntry.entryDate, jl.journalEntry.entryNumber")
    List<JournalLine> findByAccountAndDateRangePosted(@Param("accountId") Long accountId, @Param("tenantId") Long tenantId,
                                                       @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(jl.debitAmount) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.tenantId = :tenantId AND jl.journalEntry.status = 'POSTED'")
    BigDecimal sumDebitByAccountId(@Param("accountId") Long accountId, @Param("tenantId") Long tenantId);

    @Query("SELECT SUM(jl.creditAmount) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.tenantId = :tenantId AND jl.journalEntry.status = 'POSTED'")
    BigDecimal sumCreditByAccountId(@Param("accountId") Long accountId, @Param("tenantId") Long tenantId);

    @Query("SELECT SUM(jl.debitAmount) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.tenantId = :tenantId " +
           "AND jl.journalEntry.status = 'POSTED' AND jl.journalEntry.fiscalPeriod.id = :periodId")
    BigDecimal sumDebitByAccountAndPeriod(@Param("accountId") Long accountId, @Param("tenantId") Long tenantId, @Param("periodId") Long periodId);

    @Query("SELECT SUM(jl.creditAmount) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.tenantId = :tenantId " +
           "AND jl.journalEntry.status = 'POSTED' AND jl.journalEntry.fiscalPeriod.id = :periodId")
    BigDecimal sumCreditByAccountAndPeriod(@Param("accountId") Long accountId, @Param("tenantId") Long tenantId, @Param("periodId") Long periodId);

    @Query("SELECT jl.account.id, SUM(jl.debitAmount), SUM(jl.creditAmount) FROM JournalLine jl " +
           "WHERE jl.tenantId = :tenantId AND jl.journalEntry.status = 'POSTED' " +
           "AND jl.journalEntry.fiscalPeriod.fiscalYear.id = :fiscalYearId " +
           "GROUP BY jl.account.id")
    List<Object[]> sumByAccountForFiscalYear(@Param("tenantId") Long tenantId, @Param("fiscalYearId") Long fiscalYearId);

    @Query("SELECT COALESCE(SUM(jl.baseDebitAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.tenantId = :tenantId " +
           "AND jl.journalEntry.status = 'POSTED' AND jl.journalEntry.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal sumDebitByAccountAndDateRange(@Param("accountId") Long accountId, @Param("tenantId") Long tenantId,
                                              @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(jl.baseCreditAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.tenantId = :tenantId " +
           "AND jl.journalEntry.status = 'POSTED' AND jl.journalEntry.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal sumCreditByAccountAndDateRange(@Param("accountId") Long accountId, @Param("tenantId") Long tenantId,
                                               @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(jl.baseDebitAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.tenantId = :tenantId " +
           "AND jl.journalEntry.status = 'POSTED' AND jl.journalEntry.entryDate <= :asOfDate")
    BigDecimal sumDebitByAccountAsOf(@Param("accountId") Long accountId, @Param("tenantId") Long tenantId,
                                      @Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(jl.baseCreditAmount), 0) FROM JournalLine jl WHERE jl.account.id = :accountId AND jl.tenantId = :tenantId " +
           "AND jl.journalEntry.status = 'POSTED' AND jl.journalEntry.entryDate <= :asOfDate")
    BigDecimal sumCreditByAccountAsOf(@Param("accountId") Long accountId, @Param("tenantId") Long tenantId,
                                       @Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT jl FROM JournalLine jl WHERE jl.tenantId = :tenantId AND jl.costCenter = :costCenter AND jl.journalEntry.status = 'POSTED'")
    List<JournalLine> findByCostCenterAndTenantId(@Param("tenantId") Long tenantId, @Param("costCenter") String costCenter);

    @Query("SELECT jl FROM JournalLine jl WHERE jl.tenantId = :tenantId AND jl.projectCode = :projectCode AND jl.journalEntry.status = 'POSTED'")
    List<JournalLine> findByProjectCodeAndTenantId(@Param("tenantId") Long tenantId, @Param("projectCode") String projectCode);

    @Query("SELECT jl FROM JournalLine jl WHERE jl.taxCode = :taxCode AND jl.account.id = :accountId " +
           "AND jl.journalEntry.entryDate BETWEEN :startDate AND :endDate " +
           "AND jl.journalEntry.status = 'POSTED' AND jl.tenantId = :tenantId")
    List<JournalLine> findByTaxCodeAndAccountIdAndDateRange(@Param("taxCode") String taxCode, @Param("accountId") Long accountId,
                                                             @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                                                             @Param("tenantId") Long tenantId);
}
