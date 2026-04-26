package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.pos.entity.POSTerminal;
import com.hisobnoma.platform.pos.entity.Shift;
import com.hisobnoma.platform.pos.entity.ShiftStatus;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class ShiftRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ShiftRepository shiftRepository;

    private static final Long TENANT_ID = 1L;

    private POSTerminal terminal;

    @BeforeEach
    void setUp() {
        Location location = Location.builder()
                .code("LOC1").name("Store 1").locationType(LocationType.STORE).build();
        location.setTenantId(TENANT_ID);
        location = em.persistAndFlush(location);

        terminal = POSTerminal.builder()
                .terminalCode("T001").name("Terminal 1").location(location).build();
        terminal.setTenantId(TENANT_ID);
        terminal = em.persistAndFlush(terminal);
    }

    private Shift persistShift(String number, Long cashierId, ShiftStatus status, Instant openedAt) {
        Shift s = Shift.builder()
                .shiftNumber(number).terminal(terminal).cashierId(cashierId)
                .cashierName("Cashier").status(status)
                .openedAt(openedAt).openingCash(BigDecimal.ZERO).build();
        s.setTenantId(TENANT_ID);
        return em.persistAndFlush(s);
    }

    @Test
    void findByTenantId_returnsPagedResults() {
        persistShift("SH001", 100L, ShiftStatus.OPEN, Instant.now());
        persistShift("SH002", 101L, ShiftStatus.CLOSED, Instant.now().minus(1, ChronoUnit.DAYS));

        Page<Shift> result = shiftRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByIdAndTenantId_existing_returnsShift() {
        Shift s = persistShift("SH001", 100L, ShiftStatus.OPEN, Instant.now());

        Optional<Shift> result = shiftRepository.findByIdAndTenantId(s.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("SH001", result.get().getShiftNumber());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        Shift s = persistShift("SH001", 100L, ShiftStatus.OPEN, Instant.now());

        Optional<Shift> result = shiftRepository.findByIdAndTenantId(s.getId(), 999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByTerminalIdAndStatusAndTenantId_returnsMatchingShift() {
        persistShift("SH001", 100L, ShiftStatus.OPEN, Instant.now());
        persistShift("SH002", 101L, ShiftStatus.CLOSED, Instant.now().minus(1, ChronoUnit.DAYS));

        Optional<Shift> result = shiftRepository.findByTerminalIdAndStatusAndTenantId(
                terminal.getId(), ShiftStatus.OPEN, TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("SH001", result.get().getShiftNumber());
    }

    @Test
    void findByTerminalIdAndTenantId_returnsPagedResultsSorted() {
        persistShift("SH001", 100L, ShiftStatus.CLOSED, Instant.now().minus(1, ChronoUnit.DAYS));
        persistShift("SH002", 100L, ShiftStatus.OPEN, Instant.now());

        Page<Shift> result = shiftRepository.findByTerminalIdAndTenantId(terminal.getId(), TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByCashierIdAndTenantId_returnsShiftsForCashier() {
        persistShift("SH001", 100L, ShiftStatus.OPEN, Instant.now());
        persistShift("SH002", 101L, ShiftStatus.OPEN, Instant.now());

        Page<Shift> result = shiftRepository.findByCashierIdAndTenantId(100L, TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByCashierIdAndStatusAndTenantId_returnsMatchingShift() {
        persistShift("SH001", 100L, ShiftStatus.OPEN, Instant.now());
        persistShift("SH002", 100L, ShiftStatus.CLOSED, Instant.now().minus(1, ChronoUnit.DAYS));

        Optional<Shift> result = shiftRepository.findByCashierIdAndStatusAndTenantId(100L, ShiftStatus.OPEN, TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("SH001", result.get().getShiftNumber());
    }

    @Test
    void findByDateRangeAndTenantId_filtersCorrectly() {
        Instant now = Instant.now();
        persistShift("SH001", 100L, ShiftStatus.OPEN, now);
        persistShift("SH002", 101L, ShiftStatus.CLOSED, now.minus(5, ChronoUnit.DAYS));

        List<Shift> result = shiftRepository.findByDateRangeAndTenantId(
                now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("SH001", result.get(0).getShiftNumber());
    }

    @Test
    void findOpenShiftsByTenantId_returnsOnlyOpen() {
        persistShift("SH001", 100L, ShiftStatus.OPEN, Instant.now());
        persistShift("SH002", 101L, ShiftStatus.CLOSED, Instant.now().minus(1, ChronoUnit.DAYS));

        List<Shift> result = shiftRepository.findOpenShiftsByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals(ShiftStatus.OPEN, result.get(0).getStatus());
    }

    @Test
    void findByShiftNumberAndTenantId_existing_returnsShift() {
        persistShift("SH-FIND", 100L, ShiftStatus.OPEN, Instant.now());

        Optional<Shift> result = shiftRepository.findByShiftNumberAndTenantId("SH-FIND", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("SH-FIND", result.get().getShiftNumber());
    }

    @Test
    void countByDateRangeAndTenantId_returnsCount() {
        Instant now = Instant.now();
        persistShift("SH001", 100L, ShiftStatus.OPEN, now);
        persistShift("SH002", 101L, ShiftStatus.OPEN, now);
        persistShift("SH003", 102L, ShiftStatus.CLOSED, now.minus(5, ChronoUnit.DAYS));

        Long result = shiftRepository.countByDateRangeAndTenantId(
                now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), TENANT_ID);

        assertEquals(2L, result);
    }

    @Test
    void findMaxShiftNumberByPrefixAndTenantId_returnsMaxNumber() {
        persistShift("SH-2024-001", 100L, ShiftStatus.CLOSED, Instant.now());
        persistShift("SH-2024-002", 100L, ShiftStatus.OPEN, Instant.now());

        String result = shiftRepository.findMaxShiftNumberByPrefixAndTenantId("SH-2024-", TENANT_ID);

        assertEquals("SH-2024-002", result);
    }
}
