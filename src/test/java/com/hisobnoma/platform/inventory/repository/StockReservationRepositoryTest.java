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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class StockReservationRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    private Product product;
    private Location location;

    @BeforeEach
    void setUp() {
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .code("PCS").name("Pieces").active(true).isBaseUnit(true).build();
        uom = em.persistAndFlush(uom);

        product = Product.builder()
                .sku("SR-P1").name("Reserved Product")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom).build();
        product.setTenantId(1L);
        product = em.persistAndFlush(product);

        location = Location.builder()
                .code("SR-WH1").name("Reservation Warehouse")
                .locationType(LocationType.WAREHOUSE).active(true).build();
        location.setTenantId(1L);
        location = em.persistAndFlush(location);
    }

    private StockReservation persistReservation(BigDecimal qty,
                                                MovementReferenceType refType, Long refId,
                                                StockReservation.ReservationStatus status,
                                                LocalDateTime expiresAt) {
        StockReservation r = StockReservation.builder()
                .product(product)
                .location(location)
                .quantity(qty)
                .referenceType(refType)
                .referenceId(refId)
                .status(status)
                .reservedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();
        r.setTenantId(1L);
        return em.persistAndFlush(r);
    }

    @Test
    void findByProductIdAndLocationIdAndTenantIdAndStatus_returnsMatching() {
        persistReservation(new BigDecimal("10"), MovementReferenceType.POS_TRANSACTION, 1L,
                StockReservation.ReservationStatus.ACTIVE, null);
        persistReservation(new BigDecimal("5"), MovementReferenceType.POS_TRANSACTION, 2L,
                StockReservation.ReservationStatus.COMPLETED, null);

        List<StockReservation> result = stockReservationRepository
                .findByProductIdAndLocationIdAndTenantIdAndStatus(
                        product.getId(), location.getId(), 1L,
                        StockReservation.ReservationStatus.ACTIVE);

        assertEquals(1, result.size());
    }

    @Test
    void findByReferenceTypeAndReferenceIdAndTenantId_returnsMatching() {
        persistReservation(new BigDecimal("10"), MovementReferenceType.POS_TRANSACTION, 100L,
                StockReservation.ReservationStatus.ACTIVE, null);

        List<StockReservation> result = stockReservationRepository
                .findByReferenceTypeAndReferenceIdAndTenantId(
                        MovementReferenceType.POS_TRANSACTION, 100L, 1L);

        assertEquals(1, result.size());
    }

    @Test
    void findByProductIdAndLocationIdAndReferenceTypeAndReferenceIdAndTenantId_returnsExact() {
        persistReservation(new BigDecimal("10"), MovementReferenceType.WEB_ORDER, 200L,
                StockReservation.ReservationStatus.ACTIVE, null);

        Optional<StockReservation> result = stockReservationRepository
                .findByProductIdAndLocationIdAndReferenceTypeAndReferenceIdAndTenantId(
                        product.getId(), location.getId(),
                        MovementReferenceType.WEB_ORDER, 200L, 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void sumActiveReservations_sumsCorrectly() {
        persistReservation(new BigDecimal("10"), MovementReferenceType.POS_TRANSACTION, 1L,
                StockReservation.ReservationStatus.ACTIVE, null);
        persistReservation(new BigDecimal("5"), MovementReferenceType.POS_TRANSACTION, 2L,
                StockReservation.ReservationStatus.ACTIVE, null);
        persistReservation(new BigDecimal("20"), MovementReferenceType.POS_TRANSACTION, 3L,
                StockReservation.ReservationStatus.COMPLETED, null);

        BigDecimal sum = stockReservationRepository.sumActiveReservations(
                product.getId(), location.getId(), 1L);

        assertEquals(0, new BigDecimal("15").compareTo(sum));
    }

    @Test
    void findExpiredReservations_returnsActiveExpiredOnes() {
        persistReservation(new BigDecimal("10"), MovementReferenceType.POS_TRANSACTION, 1L,
                StockReservation.ReservationStatus.ACTIVE,
                LocalDateTime.now().minusHours(1));  // expired
        persistReservation(new BigDecimal("5"), MovementReferenceType.POS_TRANSACTION, 2L,
                StockReservation.ReservationStatus.ACTIVE,
                LocalDateTime.now().plusHours(1));    // not expired

        List<StockReservation> result = stockReservationRepository
                .findExpiredReservations(1L, LocalDateTime.now());

        assertEquals(1, result.size());
    }

    @Test
    void findByTenantIdAndStatus_paged_filtersByStatus() {
        persistReservation(new BigDecimal("10"), MovementReferenceType.POS_TRANSACTION, 1L,
                StockReservation.ReservationStatus.ACTIVE, null);
        persistReservation(new BigDecimal("5"), MovementReferenceType.POS_TRANSACTION, 2L,
                StockReservation.ReservationStatus.CANCELLED, null);

        Page<StockReservation> result = stockReservationRepository.findByTenantIdAndStatus(
                1L, StockReservation.ReservationStatus.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findActiveByProduct_returnsActiveSortedByReservedAt() {
        persistReservation(new BigDecimal("10"), MovementReferenceType.POS_TRANSACTION, 1L,
                StockReservation.ReservationStatus.ACTIVE, null);
        persistReservation(new BigDecimal("5"), MovementReferenceType.POS_TRANSACTION, 2L,
                StockReservation.ReservationStatus.COMPLETED, null);

        List<StockReservation> result = stockReservationRepository.findActiveByProduct(
                1L, product.getId());

        assertEquals(1, result.size());
    }
}
