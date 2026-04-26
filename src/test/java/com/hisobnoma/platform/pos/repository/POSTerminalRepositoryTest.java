package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.pos.entity.POSTerminal;
import org.junit.jupiter.api.BeforeEach;
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
class POSTerminalRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private POSTerminalRepository posTerminalRepository;

    private static final Long TENANT_ID = 1L;

    private Location location1;
    private Location location2;

    @BeforeEach
    void setUp() {
        location1 = Location.builder()
                .code("LOC1").name("Store 1").locationType(LocationType.STORE).build();
        location1.setTenantId(TENANT_ID);
        location1 = em.persistAndFlush(location1);

        location2 = Location.builder()
                .code("LOC2").name("Store 2").locationType(LocationType.STORE).build();
        location2.setTenantId(TENANT_ID);
        location2 = em.persistAndFlush(location2);
    }

    private POSTerminal persistTerminal(String code, String name, Location loc, boolean active) {
        POSTerminal t = POSTerminal.builder()
                .terminalCode(code).name(name).location(loc).active(active).build();
        t.setTenantId(TENANT_ID);
        return em.persistAndFlush(t);
    }

    @Test
    void findByTenantId_returnsPagedResults() {
        persistTerminal("T001", "Terminal 1", location1, true);
        persistTerminal("T002", "Terminal 2", location1, true);

        Page<POSTerminal> result = posTerminalRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByIdAndTenantId_existing_returnsTerminal() {
        POSTerminal t = persistTerminal("T001", "Terminal 1", location1, true);

        Optional<POSTerminal> result = posTerminalRepository.findByIdAndTenantId(t.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("T001", result.get().getTerminalCode());
    }

    @Test
    void findByTerminalCodeAndTenantId_existing_returnsTerminal() {
        persistTerminal("T-FIND", "Terminal Find", location1, true);

        Optional<POSTerminal> result = posTerminalRepository.findByTerminalCodeAndTenantId("T-FIND", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Terminal Find", result.get().getName());
    }

    @Test
    void findByLocationIdAndTenantId_returnsTerminalsAtLocation() {
        persistTerminal("T001", "Terminal 1", location1, true);
        persistTerminal("T002", "Terminal 2", location1, true);
        persistTerminal("T003", "Terminal 3", location2, true);

        List<POSTerminal> result = posTerminalRepository.findByLocationIdAndTenantId(location1.getId(), TENANT_ID);

        assertEquals(2, result.size());
    }

    @Test
    void findActiveByTenantId_returnsOnlyActive() {
        persistTerminal("T001", "Terminal 1", location1, true);
        persistTerminal("T002", "Terminal 2", location1, false);

        List<POSTerminal> result = posTerminalRepository.findActiveByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void existsByTerminalCodeAndTenantId_existing_returnsTrue() {
        persistTerminal("T-EXISTS", "Terminal", location1, true);

        assertTrue(posTerminalRepository.existsByTerminalCodeAndTenantId("T-EXISTS", TENANT_ID, null));
        assertFalse(posTerminalRepository.existsByTerminalCodeAndTenantId("T-NOPE", TENANT_ID, null));
    }

    @Test
    void existsByTerminalCodeAndTenantId_excludesOwnId() {
        POSTerminal t = persistTerminal("T-EXISTS", "Terminal", location1, true);

        // Excluding own ID should return false (code is unique only when belonging to another record)
        assertFalse(posTerminalRepository.existsByTerminalCodeAndTenantId("T-EXISTS", TENANT_ID, t.getId()));
    }

    @Test
    void searchByTenantId_matchesByCode() {
        persistTerminal("CASH-01", "Cash Register", location1, true);
        persistTerminal("POS-01", "POS Terminal", location1, true);

        Page<POSTerminal> result = posTerminalRepository.searchByTenantId("CASH", TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("CASH-01", result.getContent().get(0).getTerminalCode());
    }

    @Test
    void searchByTenantId_matchesByName() {
        persistTerminal("T001", "Main Register", location1, true);
        persistTerminal("T002", "Backup Terminal", location1, true);

        Page<POSTerminal> result = posTerminalRepository.searchByTenantId("register", TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Main Register", result.getContent().get(0).getName());
    }
}
