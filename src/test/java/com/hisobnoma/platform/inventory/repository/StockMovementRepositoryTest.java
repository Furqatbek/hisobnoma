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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class StockMovementRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private Product product;
    private Location location;
    private Location location2;

    @BeforeEach
    void setUp() {
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .code("PCS").name("Pieces").active(true).isBaseUnit(true).build();
        uom = em.persistAndFlush(uom);

        product = Product.builder()
                .sku("SM-P1").name("Movement Product")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom).build();
        product.setTenantId(1L);
        product = em.persistAndFlush(product);

        location = Location.builder()
                .code("SM-WH1").name("Warehouse 1")
                .locationType(LocationType.WAREHOUSE).active(true).build();
        location.setTenantId(1L);
        location = em.persistAndFlush(location);

        location2 = Location.builder()
                .code("SM-WH2").name("Warehouse 2")
                .locationType(LocationType.WAREHOUSE).active(true).build();
        location2.setTenantId(1L);
        location2 = em.persistAndFlush(location2);
    }

    private StockMovement persistMovement(MovementType type, BigDecimal qty,
                                          Location from, Location to,
                                          LocalDateTime date) {
        StockMovement sm = StockMovement.builder()
                .product(product)
                .movementType(type)
                .quantity(qty)
                .fromLocation(from)
                .toLocation(to)
                .movementDate(date)
                .build();
        sm.setTenantId(1L);
        return em.persistAndFlush(sm);
    }

    @Test
    void findByTenantIdOrderByMovementDateDesc_returnsSortedMovements() {
        persistMovement(MovementType.STOCK_IN, new BigDecimal("10"), null, location,
                LocalDateTime.of(2024, 1, 1, 10, 0));
        persistMovement(MovementType.STOCK_IN, new BigDecimal("20"), null, location,
                LocalDateTime.of(2024, 1, 2, 10, 0));

        Page<StockMovement> result = stockMovementRepository.findByTenantIdOrderByMovementDateDesc(
                1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().get(0).getMovementDate()
                .isAfter(result.getContent().get(1).getMovementDate()));
    }

    @Test
    void findByProductIdAndTenantIdOrderByMovementDateDesc_filtersForProduct() {
        persistMovement(MovementType.STOCK_IN, new BigDecimal("10"), null, location,
                LocalDateTime.of(2024, 1, 1, 10, 0));

        Page<StockMovement> result = stockMovementRepository
                .findByProductIdAndTenantIdOrderByMovementDateDesc(product.getId(), 1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByLocationId_matchesFromOrToLocation() {
        persistMovement(MovementType.STOCK_IN, new BigDecimal("10"), null, location,
                LocalDateTime.of(2024, 1, 1, 10, 0));
        persistMovement(MovementType.STOCK_OUT, new BigDecimal("5"), location, null,
                LocalDateTime.of(2024, 1, 2, 10, 0));
        persistMovement(MovementType.STOCK_IN, new BigDecimal("15"), null, location2,
                LocalDateTime.of(2024, 1, 3, 10, 0));

        Page<StockMovement> result = stockMovementRepository.findByLocationId(1L, location.getId(),
                PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByMovementTypeAndTenantIdOrderByMovementDateDesc_filtersByType() {
        persistMovement(MovementType.STOCK_IN, new BigDecimal("10"), null, location,
                LocalDateTime.of(2024, 1, 1, 10, 0));
        persistMovement(MovementType.STOCK_OUT, new BigDecimal("5"), location, null,
                LocalDateTime.of(2024, 1, 2, 10, 0));

        Page<StockMovement> result = stockMovementRepository
                .findByMovementTypeAndTenantIdOrderByMovementDateDesc(
                        MovementType.STOCK_IN, 1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByReferenceTypeAndReferenceIdAndTenantId_returnsMatching() {
        StockMovement sm = StockMovement.builder()
                .product(product)
                .movementType(MovementType.STOCK_IN)
                .quantity(new BigDecimal("10"))
                .toLocation(location)
                .movementDate(LocalDateTime.now())
                .referenceType(MovementReferenceType.PURCHASE_ORDER)
                .referenceId(100L)
                .build();
        sm.setTenantId(1L);
        em.persistAndFlush(sm);

        List<StockMovement> result = stockMovementRepository
                .findByReferenceTypeAndReferenceIdAndTenantId(
                        MovementReferenceType.PURCHASE_ORDER, 100L, 1L);

        assertEquals(1, result.size());
    }

    @Test
    void findByDateRange_returnsMovementsInRange() {
        persistMovement(MovementType.STOCK_IN, new BigDecimal("10"), null, location,
                LocalDateTime.of(2024, 1, 15, 10, 0));
        persistMovement(MovementType.STOCK_IN, new BigDecimal("20"), null, location,
                LocalDateTime.of(2024, 2, 15, 10, 0));
        persistMovement(MovementType.STOCK_IN, new BigDecimal("30"), null, location,
                LocalDateTime.of(2024, 3, 15, 10, 0));

        Page<StockMovement> result = stockMovementRepository.findByDateRange(1L,
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 2, 28, 23, 59),
                PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByProductAndDateRange_returnsMovementsForProductInRange() {
        persistMovement(MovementType.STOCK_IN, new BigDecimal("10"), null, location,
                LocalDateTime.of(2024, 1, 15, 10, 0));
        persistMovement(MovementType.STOCK_IN, new BigDecimal("20"), null, location,
                LocalDateTime.of(2024, 3, 15, 10, 0));

        List<StockMovement> result = stockMovementRepository.findByProductAndDateRange(1L,
                product.getId(),
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 2, 28, 23, 59));

        assertEquals(1, result.size());
    }

    @Test
    void findRecentUnreversed_excludesReversedMovements() {
        StockMovement normal = persistMovement(MovementType.STOCK_IN, new BigDecimal("10"),
                null, location, LocalDateTime.of(2024, 6, 1, 10, 0));

        StockMovement reversed = StockMovement.builder()
                .product(product)
                .movementType(MovementType.STOCK_IN)
                .quantity(new BigDecimal("5"))
                .toLocation(location)
                .movementDate(LocalDateTime.of(2024, 6, 2, 10, 0))
                .reversed(true)
                .reversalMovementId(999L)
                .build();
        reversed.setTenantId(1L);
        em.persistAndFlush(reversed);

        List<StockMovement> result = stockMovementRepository.findRecentUnreversed(
                1L, LocalDateTime.of(2024, 5, 1, 0, 0));

        assertEquals(1, result.size());
        assertEquals(normal.getId(), result.get(0).getId());
    }
}
