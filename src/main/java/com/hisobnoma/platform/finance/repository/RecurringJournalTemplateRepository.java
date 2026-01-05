package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.RecurringJournalTemplate;
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
public interface RecurringJournalTemplateRepository extends JpaRepository<RecurringJournalTemplate, Long> {

    @Query("SELECT t FROM RecurringJournalTemplate t WHERE t.tenantId = :tenantId ORDER BY t.name")
    Page<RecurringJournalTemplate> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT t FROM RecurringJournalTemplate t WHERE t.id = :id AND t.tenantId = :tenantId")
    Optional<RecurringJournalTemplate> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM RecurringJournalTemplate t LEFT JOIN FETCH t.lines WHERE t.id = :id AND t.tenantId = :tenantId")
    Optional<RecurringJournalTemplate> findByIdWithLines(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM RecurringJournalTemplate t WHERE t.tenantId = :tenantId AND t.active = true " +
           "AND t.nextRunDate <= :date AND (t.endDate IS NULL OR t.endDate >= :date)")
    List<RecurringJournalTemplate> findDueTemplates(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    @Query("SELECT t FROM RecurringJournalTemplate t WHERE t.tenantId = :tenantId AND t.active = true ORDER BY t.name")
    List<RecurringJournalTemplate> findActiveByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByNameAndTenantId(String name, Long tenantId);
}
