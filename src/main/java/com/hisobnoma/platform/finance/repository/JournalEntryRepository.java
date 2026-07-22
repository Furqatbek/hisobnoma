package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.JournalEntry;
import com.hisobnoma.platform.finance.entity.JournalSource;
import com.hisobnoma.platform.finance.entity.JournalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    @Query("SELECT je FROM JournalEntry je WHERE je.tenantId = :tenantId ORDER BY je.entryDate DESC, je.entryNumber DESC")
    Page<JournalEntry> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT je FROM JournalEntry je WHERE je.id = :id AND je.tenantId = :tenantId")
    Optional<JournalEntry> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT je FROM JournalEntry je WHERE je.entryNumber = :entryNumber AND je.tenantId = :tenantId")
    Optional<JournalEntry> findByEntryNumberAndTenantId(@Param("entryNumber") String entryNumber, @Param("tenantId") Long tenantId);

    @Query("SELECT je FROM JournalEntry je WHERE je.tenantId = :tenantId AND je.status = :status ORDER BY je.entryDate DESC")
    Page<JournalEntry> findByStatusAndTenantId(@Param("tenantId") Long tenantId, @Param("status") JournalStatus status, Pageable pageable);

    @Query("SELECT je FROM JournalEntry je WHERE je.tenantId = :tenantId AND je.source = :source ORDER BY je.entryDate DESC")
    Page<JournalEntry> findBySourceAndTenantId(@Param("tenantId") Long tenantId, @Param("source") JournalSource source, Pageable pageable);

    @Query("SELECT je FROM JournalEntry je WHERE je.tenantId = :tenantId AND je.entryDate BETWEEN :startDate AND :endDate ORDER BY je.entryDate DESC")
    List<JournalEntry> findByDateRangeAndTenantId(@Param("tenantId") Long tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT je FROM JournalEntry je WHERE je.tenantId = :tenantId AND je.fiscalPeriod.id = :periodId ORDER BY je.entryDate DESC")
    List<JournalEntry> findByPeriodAndTenantId(@Param("tenantId") Long tenantId, @Param("periodId") Long periodId);

    @Query("SELECT je FROM JournalEntry je WHERE je.tenantId = :tenantId AND je.referenceType = :referenceType AND je.referenceId = :referenceId")
    List<JournalEntry> findByReferenceAndTenantId(@Param("tenantId") Long tenantId, @Param("referenceType") String referenceType, @Param("referenceId") Long referenceId);

    @Query("SELECT je FROM JournalEntry je LEFT JOIN FETCH je.lines "
            + "WHERE je.id = :id AND je.tenantId = :tenantId")
    Optional<JournalEntry> findByIdWithLines(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT je FROM JournalEntry je WHERE je.tenantId = :tenantId AND " +
           "(LOWER(je.description) LIKE LOWER(CONCAT('%', :search, '%')) OR je.entryNumber LIKE CONCAT('%', :search, '%'))")
    Page<JournalEntry> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT je FROM JournalEntry je WHERE je.tenantId = :tenantId AND je.reversing = true AND je.reversalDate <= :date AND je.reversingEntryId IS NULL")
    List<JournalEntry> findPendingReversalsForDate(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(je.entryNumber, LENGTH(:prefix) + 1) AS integer)), 0) FROM JournalEntry je WHERE je.entryNumber LIKE CONCAT(:prefix, '%') AND je.tenantId = :tenantId")
    Integer findMaxEntryNumberForPrefix(@Param("prefix") String prefix, @Param("tenantId") Long tenantId);

    boolean existsByEntryNumberAndTenantId(String entryNumber, Long tenantId);
}
