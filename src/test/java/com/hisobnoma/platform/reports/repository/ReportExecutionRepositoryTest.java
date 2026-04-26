package com.hisobnoma.platform.reports.repository;

import com.hisobnoma.platform.reports.entity.ReportDefinition;
import com.hisobnoma.platform.reports.entity.ReportExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class ReportExecutionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReportExecutionRepository reportExecutionRepository;

    private static final Long TENANT_ID = 1L;

    private ReportDefinition reportDef;

    @BeforeEach
    void setUp() {
        reportDef = ReportDefinition.builder()
                .code("RPT-001").name("Sales Report")
                .category(ReportDefinition.ReportCategory.SALES)
                .active(true).sortOrder(1).build();
        reportDef.setTenantId(TENANT_ID);
        reportDef = em.persistAndFlush(reportDef);
    }

    private ReportExecution persistExecution(ReportExecution.ExecutionStatus status,
                                              ReportExecution.ExecutionSource source,
                                              Long userId, Instant startedAt) {
        ReportExecution exec = ReportExecution.builder()
                .reportDefinition(reportDef)
                .source(source).status(status)
                .executedByUserId(userId)
                .startedAt(startedAt)
                .build();
        exec.setTenantId(TENANT_ID);
        return em.persistAndFlush(exec);
    }

    @Test
    void findByIdAndTenantId_existing_returnsExecution() {
        ReportExecution exec = persistExecution(ReportExecution.ExecutionStatus.COMPLETED,
                ReportExecution.ExecutionSource.MANUAL, 100L, Instant.now());

        Optional<ReportExecution> result = reportExecutionRepository.findByIdAndTenantId(exec.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals(ReportExecution.ExecutionStatus.COMPLETED, result.get().getStatus());
    }

    @Test
    void findByTenantIdOrderByStartedAtDesc_returnsPagedResults() {
        Instant now = Instant.now();
        persistExecution(ReportExecution.ExecutionStatus.COMPLETED, ReportExecution.ExecutionSource.MANUAL,
                100L, now.minus(1, ChronoUnit.HOURS));
        persistExecution(ReportExecution.ExecutionStatus.RUNNING, ReportExecution.ExecutionSource.SCHEDULED,
                100L, now);

        Page<ReportExecution> result = reportExecutionRepository.findByTenantIdOrderByStartedAtDesc(
                TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByReportDefinitionIdAndTenantId_filtersCorrectly() {
        ReportDefinition otherDef = ReportDefinition.builder()
                .code("RPT-002").name("Inventory Report")
                .category(ReportDefinition.ReportCategory.INVENTORY)
                .active(true).sortOrder(2).build();
        otherDef.setTenantId(TENANT_ID);
        otherDef = em.persistAndFlush(otherDef);

        persistExecution(ReportExecution.ExecutionStatus.COMPLETED, ReportExecution.ExecutionSource.MANUAL,
                100L, Instant.now());

        ReportExecution otherExec = ReportExecution.builder()
                .reportDefinition(otherDef)
                .source(ReportExecution.ExecutionSource.MANUAL)
                .status(ReportExecution.ExecutionStatus.COMPLETED)
                .executedByUserId(100L).startedAt(Instant.now())
                .build();
        otherExec.setTenantId(TENANT_ID);
        em.persistAndFlush(otherExec);

        Page<ReportExecution> result = reportExecutionRepository.findByReportDefinitionIdAndTenantId(
                reportDef.getId(), TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByExecutedByUserIdAndTenantId_filtersCorrectly() {
        persistExecution(ReportExecution.ExecutionStatus.COMPLETED, ReportExecution.ExecutionSource.MANUAL,
                100L, Instant.now());
        persistExecution(ReportExecution.ExecutionStatus.COMPLETED, ReportExecution.ExecutionSource.MANUAL,
                200L, Instant.now());

        Page<ReportExecution> result = reportExecutionRepository.findByExecutedByUserIdAndTenantId(
                100L, TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByStatusAndTenantId_filtersCorrectly() {
        persistExecution(ReportExecution.ExecutionStatus.COMPLETED, ReportExecution.ExecutionSource.MANUAL,
                100L, Instant.now());
        persistExecution(ReportExecution.ExecutionStatus.FAILED, ReportExecution.ExecutionSource.SCHEDULED,
                100L, Instant.now());
        persistExecution(ReportExecution.ExecutionStatus.COMPLETED, ReportExecution.ExecutionSource.MANUAL,
                200L, Instant.now());

        List<ReportExecution> result = reportExecutionRepository.findByStatusAndTenantId(
                ReportExecution.ExecutionStatus.COMPLETED, TENANT_ID);

        assertEquals(2, result.size());
    }

    @Test
    void findStaleRunningExecutions_returnsStaleOnes() {
        Instant threshold = Instant.now().minus(30, ChronoUnit.MINUTES);

        persistExecution(ReportExecution.ExecutionStatus.RUNNING, ReportExecution.ExecutionSource.MANUAL,
                100L, Instant.now().minus(1, ChronoUnit.HOURS));
        persistExecution(ReportExecution.ExecutionStatus.RUNNING, ReportExecution.ExecutionSource.MANUAL,
                100L, Instant.now());

        List<ReportExecution> result = reportExecutionRepository.findStaleRunningExecutions(threshold);

        assertEquals(1, result.size());
    }

    @Test
    void countRecentExecutions_returnsCount() {
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);

        persistExecution(ReportExecution.ExecutionStatus.COMPLETED, ReportExecution.ExecutionSource.MANUAL,
                100L, Instant.now());
        persistExecution(ReportExecution.ExecutionStatus.COMPLETED, ReportExecution.ExecutionSource.MANUAL,
                100L, Instant.now().minus(2, ChronoUnit.HOURS));

        long result = reportExecutionRepository.countRecentExecutions(TENANT_ID, reportDef.getId(), since);

        assertEquals(1L, result);
    }
}
