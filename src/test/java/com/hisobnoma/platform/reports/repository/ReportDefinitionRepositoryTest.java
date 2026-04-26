package com.hisobnoma.platform.reports.repository;

import com.hisobnoma.platform.reports.entity.ReportDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class ReportDefinitionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReportDefinitionRepository reportDefinitionRepository;

    private static final Long TENANT_ID = 1L;

    private ReportDefinition persistReport(String code, String name,
                                            ReportDefinition.ReportCategory category,
                                            boolean active, boolean systemReport,
                                            int sortOrder, Long tenantId) {
        ReportDefinition rd = ReportDefinition.builder()
                .code(code).name(name).category(category)
                .active(active).systemReport(systemReport).sortOrder(sortOrder)
                .build();
        rd.setTenantId(tenantId);
        return em.persistAndFlush(rd);
    }

    @Test
    void findByCodeAndTenantId_existing_returnsDefinition() {
        persistReport("RPT-001", "Sales Report", ReportDefinition.ReportCategory.SALES,
                true, false, 1, TENANT_ID);

        Optional<ReportDefinition> result = reportDefinitionRepository.findByCodeAndTenantId("RPT-001", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Sales Report", result.get().getName());
    }

    @Test
    void findByIdAndTenantId_existing_returnsDefinition() {
        ReportDefinition rd = persistReport("RPT-001", "Sales Report", ReportDefinition.ReportCategory.SALES,
                true, false, 1, TENANT_ID);

        Optional<ReportDefinition> result = reportDefinitionRepository.findByIdAndTenantId(rd.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("RPT-001", result.get().getCode());
    }

    @Test
    void findAllActiveByTenantId_includesSystemReports() {
        persistReport("SYS-001", "System Sales", ReportDefinition.ReportCategory.SALES,
                true, true, 1, null);
        persistReport("RPT-001", "Custom Report", ReportDefinition.ReportCategory.CUSTOM,
                true, false, 2, TENANT_ID);
        persistReport("RPT-002", "Inactive Report", ReportDefinition.ReportCategory.INVENTORY,
                false, false, 3, TENANT_ID);

        List<ReportDefinition> result = reportDefinitionRepository.findAllActiveByTenantId(TENANT_ID);

        assertEquals(2, result.size());
    }

    @Test
    void findAllByTenantId_includesSystemAndTenantReports() {
        persistReport("SYS-001", "System Report", ReportDefinition.ReportCategory.SALES,
                true, true, 1, null);
        persistReport("RPT-001", "Tenant Report", ReportDefinition.ReportCategory.CUSTOM,
                true, false, 2, TENANT_ID);
        persistReport("RPT-T2", "Other Tenant", ReportDefinition.ReportCategory.SALES,
                true, false, 1, 2L);

        Page<ReportDefinition> result = reportDefinitionRepository.findAllByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByCategoryAndTenantId_filtersCorrectly() {
        persistReport("RPT-S1", "Sales 1", ReportDefinition.ReportCategory.SALES, true, false, 1, TENANT_ID);
        persistReport("RPT-I1", "Inventory 1", ReportDefinition.ReportCategory.INVENTORY, true, false, 1, TENANT_ID);
        persistReport("SYS-S1", "System Sales", ReportDefinition.ReportCategory.SALES, true, true, 0, null);

        List<ReportDefinition> result = reportDefinitionRepository.findByCategoryAndTenantId(
                ReportDefinition.ReportCategory.SALES, TENANT_ID);

        assertEquals(2, result.size());
    }

    @Test
    void searchByTenantId_matchesByNameOrCode() {
        persistReport("SALES-RPT", "Monthly Sales Report", ReportDefinition.ReportCategory.SALES,
                true, false, 1, TENANT_ID);
        persistReport("INV-RPT", "Inventory Status", ReportDefinition.ReportCategory.INVENTORY,
                true, false, 1, TENANT_ID);

        Page<ReportDefinition> result = reportDefinitionRepository.searchByTenantId(
                "sales", TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void existsByCodeAndTenantId_existing_returnsTrue() {
        persistReport("RPT-001", "Sales Report", ReportDefinition.ReportCategory.SALES,
                true, false, 1, TENANT_ID);

        assertTrue(reportDefinitionRepository.existsByCodeAndTenantId("RPT-001", TENANT_ID));
        assertFalse(reportDefinitionRepository.existsByCodeAndTenantId("RPT-NOPE", TENANT_ID));
    }

    @Test
    void findAllSystemReports_returnsOnlySystemReports() {
        persistReport("SYS-001", "System Sales", ReportDefinition.ReportCategory.SALES,
                true, true, 1, null);
        persistReport("SYS-002", "System Inventory", ReportDefinition.ReportCategory.INVENTORY,
                true, true, 2, null);
        persistReport("SYS-003", "Inactive System", ReportDefinition.ReportCategory.FINANCIAL,
                false, true, 3, null);
        persistReport("RPT-001", "Tenant Report", ReportDefinition.ReportCategory.CUSTOM,
                true, false, 1, TENANT_ID);

        List<ReportDefinition> result = reportDefinitionRepository.findAllSystemReports();

        assertEquals(2, result.size());
        result.forEach(r -> assertTrue(r.isSystemReport()));
    }
}
