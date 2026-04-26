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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class StockBatchRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StockBatchRepository stockBatchRepository;

    private Product product;
    private Location location;

    @BeforeEach
    void setUp() {
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .code("PCS").name("Pieces").active(true).isBaseUnit(true).build();
        uom = em.persistAndFlush(uom);

        product = Product.builder()
                .sku("SB-P1").name("Batch Product")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom).build();
        product.setTenantId(1L);
        product = em.persistAndFlush(product);

        location = Location.builder()
                .code("SB-WH1").name("Batch Warehouse")
                .locationType(LocationType.WAREHOUSE).active(true).build();
        location.setTenantId(1L);
        location = em.persistAndFlush(location);
    }

    private StockBatch persistBatch(String batchNumber, BigDecimal qty, BigDecimal reserved,
                                    LocalDate expiryDate, StockBatch.BatchStatus status) {
        StockBatch b = StockBatch.builder()
                .product(product)
                .location(location)
                .batchNumber(batchNumber)
                .quantity(qty)
                .quantityReserved(reserved)
                .expiryDate(expiryDate)
                .status(status)
                .build();
        b.setTenantId(1L);
        return em.persistAndFlush(b);
    }

    @Test
    void findByProductIdAndLocationIdAndBatchNumberAndTenantId_existingBatch_returnsBatch() {
        persistBatch("BATCH-001", new BigDecimal("100"), BigDecimal.ZERO, null, StockBatch.BatchStatus.ACTIVE);

        Optional<StockBatch> result = stockBatchRepository
                .findByProductIdAndLocationIdAndBatchNumberAndTenantId(
                        product.getId(), location.getId(), "BATCH-001", 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByProductIdAndTenantIdOrderByExpiryDateAsc_returnsSorted() {
        persistBatch("B-LATE", new BigDecimal("10"), BigDecimal.ZERO,
                LocalDate.of(2025, 6, 1), StockBatch.BatchStatus.ACTIVE);
        persistBatch("B-EARLY", new BigDecimal("20"), BigDecimal.ZERO,
                LocalDate.of(2025, 1, 1), StockBatch.BatchStatus.ACTIVE);

        List<StockBatch> result = stockBatchRepository
                .findByProductIdAndTenantIdOrderByExpiryDateAsc(product.getId(), 1L);

        assertEquals(2, result.size());
        assertEquals("B-EARLY", result.get(0).getBatchNumber());
    }

    @Test
    void findByProductIdAndLocationIdAndTenantIdOrderByExpiryDateAsc_returnsSorted() {
        persistBatch("BL-LATE", new BigDecimal("10"), BigDecimal.ZERO,
                LocalDate.of(2025, 12, 1), StockBatch.BatchStatus.ACTIVE);
        persistBatch("BL-EARLY", new BigDecimal("20"), BigDecimal.ZERO,
                LocalDate.of(2025, 3, 1), StockBatch.BatchStatus.ACTIVE);

        List<StockBatch> result = stockBatchRepository
                .findByProductIdAndLocationIdAndTenantIdOrderByExpiryDateAsc(
                        product.getId(), location.getId(), 1L);

        assertEquals(2, result.size());
        assertEquals("BL-EARLY", result.get(0).getBatchNumber());
    }

    @Test
    void findAvailableBatchesFIFO_returnsActiveWithAvailableQtyOrderedByExpiry() {
        persistBatch("FIFO-1", new BigDecimal("100"), new BigDecimal("50"),
                LocalDate.of(2025, 6, 1), StockBatch.BatchStatus.ACTIVE);
        persistBatch("FIFO-2", new BigDecimal("50"), new BigDecimal("50"),
                LocalDate.of(2025, 3, 1), StockBatch.BatchStatus.ACTIVE);
        persistBatch("FIFO-3", new BigDecimal("200"), BigDecimal.ZERO,
                LocalDate.of(2025, 1, 1), StockBatch.BatchStatus.ACTIVE);
        // depleted batch should be excluded
        persistBatch("FIFO-DEP", new BigDecimal("0"), BigDecimal.ZERO,
                LocalDate.of(2025, 2, 1), StockBatch.BatchStatus.DEPLETED);

        List<StockBatch> result = stockBatchRepository.findAvailableBatchesFIFO(
                1L, product.getId(), location.getId());

        assertEquals(2, result.size());
        assertEquals("FIFO-3", result.get(0).getBatchNumber());
    }

    @Test
    void findAvailableBatchesLIFO_returnsActiveOrderedByExpiryDesc() {
        persistBatch("LIFO-1", new BigDecimal("100"), BigDecimal.ZERO,
                LocalDate.of(2025, 1, 1), StockBatch.BatchStatus.ACTIVE);
        persistBatch("LIFO-2", new BigDecimal("100"), BigDecimal.ZERO,
                LocalDate.of(2025, 12, 1), StockBatch.BatchStatus.ACTIVE);

        List<StockBatch> result = stockBatchRepository.findAvailableBatchesLIFO(
                1L, product.getId(), location.getId());

        assertEquals(2, result.size());
        assertEquals("LIFO-2", result.get(0).getBatchNumber());
    }

    @Test
    void findExpiringBefore_returnsActiveBatchesExpiringBeforeDate() {
        persistBatch("EXP-1", new BigDecimal("100"), BigDecimal.ZERO,
                LocalDate.of(2025, 3, 1), StockBatch.BatchStatus.ACTIVE);
        persistBatch("EXP-2", new BigDecimal("100"), BigDecimal.ZERO,
                LocalDate.of(2025, 12, 1), StockBatch.BatchStatus.ACTIVE);

        Page<StockBatch> result = stockBatchRepository.findExpiringBefore(
                1L, LocalDate.of(2025, 6, 1), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("EXP-1", result.getContent().get(0).getBatchNumber());
    }

    @Test
    void findByTenantIdAndStatus_filtersByStatus() {
        persistBatch("ST-ACT", new BigDecimal("100"), BigDecimal.ZERO, null, StockBatch.BatchStatus.ACTIVE);
        persistBatch("ST-QRT", new BigDecimal("50"), BigDecimal.ZERO, null, StockBatch.BatchStatus.QUARANTINE);

        Page<StockBatch> result = stockBatchRepository.findByTenantIdAndStatus(
                1L, StockBatch.BatchStatus.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchBatches_matchesBatchNumberAndProductFields() {
        persistBatch("SEARCH-B", new BigDecimal("100"), BigDecimal.ZERO, null, StockBatch.BatchStatus.ACTIVE);

        Page<StockBatch> byBatch = stockBatchRepository.searchBatches(
                1L, "SEARCH", PageRequest.of(0, 10));
        assertEquals(1, byBatch.getTotalElements());

        Page<StockBatch> byProduct = stockBatchRepository.searchBatches(
                1L, "Batch Product", PageRequest.of(0, 10));
        assertEquals(1, byProduct.getTotalElements());
    }
}
