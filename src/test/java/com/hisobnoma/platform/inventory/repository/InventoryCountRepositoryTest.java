package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class InventoryCountRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private InventoryCountRepository inventoryCountRepository;

    private Location location;

    @BeforeEach
    void setUp() {
        location = Location.builder()
                .code("IC-WH1").name("Count Warehouse")
                .locationType(LocationType.WAREHOUSE).active(true).build();
        location.setTenantId(1L);
        location = em.persistAndFlush(location);
    }

    private InventoryCount persistCount(String countNumber, CountStatus status,
                                        LocalDate countDate) {
        InventoryCount ic = InventoryCount.builder()
                .countNumber(countNumber)
                .location(location)
                .status(status)
                .countType(CountType.PARTIAL)
                .countDate(countDate)
                .build();
        ic.setTenantId(1L);
        return em.persistAndFlush(ic);
    }

    @Test
    void findByIdAndTenantId_existingCount_returnsCount() {
        InventoryCount ic = persistCount("IC-001", CountStatus.DRAFT, LocalDate.of(2024, 1, 1));

        Optional<InventoryCount> result = inventoryCountRepository.findByIdAndTenantId(ic.getId(), 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        InventoryCount ic = persistCount("IC-002", CountStatus.DRAFT, LocalDate.of(2024, 1, 1));

        Optional<InventoryCount> result = inventoryCountRepository.findByIdAndTenantId(ic.getId(), 99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllByTenantId_returnsAllForTenant() {
        persistCount("IC-A1", CountStatus.DRAFT, LocalDate.of(2024, 1, 1));
        persistCount("IC-A2", CountStatus.COMPLETED, LocalDate.of(2024, 1, 2));

        Page<InventoryCount> result = inventoryCountRepository.findAllByTenantId(1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByCountNumberAndTenantId_existingNumber_returnsCount() {
        persistCount("IC-FIND", CountStatus.DRAFT, LocalDate.of(2024, 1, 1));

        Optional<InventoryCount> result = inventoryCountRepository
                .findByCountNumberAndTenantId("IC-FIND", 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByStatusAndTenantId_filtersByStatus() {
        persistCount("IC-D1", CountStatus.DRAFT, LocalDate.of(2024, 1, 1));
        persistCount("IC-C1", CountStatus.COMPLETED, LocalDate.of(2024, 1, 2));

        Page<InventoryCount> result = inventoryCountRepository.findByStatusAndTenantId(
                CountStatus.DRAFT, 1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByLocationIdAndTenantId_filtersByLocation() {
        persistCount("IC-LOC1", CountStatus.DRAFT, LocalDate.of(2024, 1, 1));

        Location location2 = Location.builder()
                .code("IC-WH2").name("Other Warehouse")
                .locationType(LocationType.WAREHOUSE).active(true).build();
        location2.setTenantId(1L);
        location2 = em.persistAndFlush(location2);

        InventoryCount ic2 = InventoryCount.builder()
                .countNumber("IC-LOC2").location(location2)
                .status(CountStatus.DRAFT).countType(CountType.FULL)
                .countDate(LocalDate.of(2024, 1, 2))
                .build();
        ic2.setTenantId(1L);
        em.persistAndFlush(ic2);

        Page<InventoryCount> result = inventoryCountRepository.findByLocationIdAndTenantId(
                location.getId(), 1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByStatusInAndTenantId_matchesMultipleStatuses() {
        persistCount("IC-M1", CountStatus.DRAFT, LocalDate.of(2024, 1, 1));
        persistCount("IC-M2", CountStatus.IN_PROGRESS, LocalDate.of(2024, 1, 2));
        persistCount("IC-M3", CountStatus.APPROVED, LocalDate.of(2024, 1, 3));

        Page<InventoryCount> result = inventoryCountRepository.findByStatusInAndTenantId(
                List.of(CountStatus.DRAFT, CountStatus.IN_PROGRESS), 1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByDateRangeAndTenantId_filtersCorrectly() {
        persistCount("IC-DR1", CountStatus.DRAFT, LocalDate.of(2024, 1, 15));
        persistCount("IC-DR2", CountStatus.DRAFT, LocalDate.of(2024, 3, 15));
        persistCount("IC-DR3", CountStatus.DRAFT, LocalDate.of(2024, 5, 15));

        Page<InventoryCount> result = inventoryCountRepository.findByDateRangeAndTenantId(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31), 1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void searchByTenantId_matchesMultipleFields() {
        InventoryCount ic = InventoryCount.builder()
                .countNumber("SRCH-IC-001")
                .location(location)
                .status(CountStatus.DRAFT)
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.of(2024, 1, 1))
                .description("Annual stocktaking")
                .build();
        ic.setTenantId(1L);
        em.persistAndFlush(ic);

        Page<InventoryCount> byNumber = inventoryCountRepository.searchByTenantId(
                1L, "SRCH-IC", PageRequest.of(0, 10));
        assertEquals(1, byNumber.getTotalElements());

        Page<InventoryCount> byDesc = inventoryCountRepository.searchByTenantId(
                1L, "stocktaking", PageRequest.of(0, 10));
        assertEquals(1, byDesc.getTotalElements());
    }

    @Test
    void findActiveCountsForLocation_returnsOnlyDraftAndInProgress() {
        persistCount("IC-ACT1", CountStatus.DRAFT, LocalDate.of(2024, 1, 1));
        persistCount("IC-ACT2", CountStatus.IN_PROGRESS, LocalDate.of(2024, 1, 2));
        persistCount("IC-ACT3", CountStatus.COMPLETED, LocalDate.of(2024, 1, 3));
        persistCount("IC-ACT4", CountStatus.APPROVED, LocalDate.of(2024, 1, 4));

        List<InventoryCount> result = inventoryCountRepository.findActiveCountsForLocation(
                location.getId(), 1L);

        assertEquals(2, result.size());
    }

    @Test
    void findMaxCountNumberByTenantId_returnsMaxNumber() {
        persistCount("IC-001", CountStatus.DRAFT, LocalDate.of(2024, 1, 1));
        persistCount("IC-002", CountStatus.DRAFT, LocalDate.of(2024, 1, 2));

        String maxNumber = inventoryCountRepository.findMaxCountNumberByTenantId(1L);

        assertEquals("IC-002", maxNumber);
    }

    @Test
    void existsByCountNumber_existingNumber_returnsTrue() {
        persistCount("EX-IC", CountStatus.DRAFT, LocalDate.of(2024, 1, 1));

        assertTrue(inventoryCountRepository.existsByCountNumber("EX-IC"));
        assertFalse(inventoryCountRepository.existsByCountNumber("NONEXISTENT"));
    }
}
