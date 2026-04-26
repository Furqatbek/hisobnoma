package com.hisobnoma.platform.reports.repository;

import com.hisobnoma.platform.reports.entity.ReportDefinition;
import com.hisobnoma.platform.reports.entity.ReportSchedule;
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
class ReportScheduleRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReportScheduleRepository reportScheduleRepository;

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

    private ReportSchedule persistSchedule(String name, ReportSchedule.ScheduleFrequency frequency,
                                            boolean active, Instant nextRunAt, Long createdByUserId) {
        ReportSchedule s = ReportSchedule.builder()
                .reportDefinition(reportDef).name(name)
                .frequency(frequency)
                .exportFormat(ReportDefinition.ExportFormat.PDF)
                .active(active).nextRunAt(nextRunAt)
                .createdByUserId(createdByUserId)
                .build();
        s.setTenantId(TENANT_ID);
        return em.persistAndFlush(s);
    }

    @Test
    void findByIdAndTenantId_existing_returnsSchedule() {
        ReportSchedule s = persistSchedule("Daily Sales", ReportSchedule.ScheduleFrequency.DAILY,
                true, Instant.now(), 100L);

        Optional<ReportSchedule> result = reportScheduleRepository.findByIdAndTenantId(s.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Daily Sales", result.get().getName());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        ReportSchedule s = persistSchedule("Daily Sales", ReportSchedule.ScheduleFrequency.DAILY,
                true, Instant.now(), 100L);

        Optional<ReportSchedule> result = reportScheduleRepository.findByIdAndTenantId(s.getId(), 999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByTenantId_returnsPagedResults() {
        persistSchedule("Daily Sales", ReportSchedule.ScheduleFrequency.DAILY, true, Instant.now(), 100L);
        persistSchedule("Weekly Inventory", ReportSchedule.ScheduleFrequency.WEEKLY, true, Instant.now(), 100L);

        Page<ReportSchedule> result = reportScheduleRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10));

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

        persistSchedule("Sales Daily", ReportSchedule.ScheduleFrequency.DAILY, true, Instant.now(), 100L);

        ReportSchedule otherSchedule = ReportSchedule.builder()
                .reportDefinition(otherDef).name("Inventory Weekly")
                .frequency(ReportSchedule.ScheduleFrequency.WEEKLY)
                .exportFormat(ReportDefinition.ExportFormat.EXCEL)
                .active(true).nextRunAt(Instant.now()).build();
        otherSchedule.setTenantId(TENANT_ID);
        em.persistAndFlush(otherSchedule);

        List<ReportSchedule> result = reportScheduleRepository.findByReportDefinitionIdAndTenantId(
                reportDef.getId(), TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Sales Daily", result.get(0).getName());
    }

    @Test
    void findSchedulesDueForExecution_returnsSchedulesPastDue() {
        Instant now = Instant.now();
        persistSchedule("Overdue", ReportSchedule.ScheduleFrequency.DAILY, true,
                now.minus(1, ChronoUnit.HOURS), 100L);
        persistSchedule("Future", ReportSchedule.ScheduleFrequency.DAILY, true,
                now.plus(1, ChronoUnit.HOURS), 100L);
        persistSchedule("Inactive Overdue", ReportSchedule.ScheduleFrequency.DAILY, false,
                now.minus(1, ChronoUnit.HOURS), 100L);

        List<ReportSchedule> result = reportScheduleRepository.findSchedulesDueForExecution(now);

        assertEquals(1, result.size());
        assertEquals("Overdue", result.get(0).getName());
    }

    @Test
    void findActiveSchedulesByTenantId_returnsOnlyActiveOrderedByNextRun() {
        Instant now = Instant.now();
        persistSchedule("Later", ReportSchedule.ScheduleFrequency.DAILY, true,
                now.plus(2, ChronoUnit.HOURS), 100L);
        persistSchedule("Sooner", ReportSchedule.ScheduleFrequency.WEEKLY, true,
                now.plus(1, ChronoUnit.HOURS), 100L);
        persistSchedule("Inactive", ReportSchedule.ScheduleFrequency.MONTHLY, false,
                now, 100L);

        List<ReportSchedule> result = reportScheduleRepository.findActiveSchedulesByTenantId(TENANT_ID);

        assertEquals(2, result.size());
        assertEquals("Sooner", result.get(0).getName());
        assertEquals("Later", result.get(1).getName());
    }

    @Test
    void findByCreatedByUserId_filtersCorrectly() {
        persistSchedule("User 100 Schedule", ReportSchedule.ScheduleFrequency.DAILY, true, Instant.now(), 100L);
        persistSchedule("User 200 Schedule", ReportSchedule.ScheduleFrequency.WEEKLY, true, Instant.now(), 200L);

        List<ReportSchedule> result = reportScheduleRepository.findByCreatedByUserId(TENANT_ID, 100L);

        assertEquals(1, result.size());
        assertEquals("User 100 Schedule", result.get(0).getName());
    }
}
