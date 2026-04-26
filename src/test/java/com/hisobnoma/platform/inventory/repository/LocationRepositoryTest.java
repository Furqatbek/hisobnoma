package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
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
class LocationRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private LocationRepository locationRepository;

    private Location persistLocation(String code, String name, Long tenantId,
                                     LocationType type, boolean active, int sortOrder,
                                     boolean isDefault, Location parent) {
        Location l = Location.builder()
                .code(code)
                .name(name)
                .locationType(type)
                .active(active)
                .sortOrder(sortOrder)
                .isDefault(isDefault)
                .parentLocation(parent)
                .build();
        l.setTenantId(tenantId);
        return em.persistAndFlush(l);
    }

    private Location persistLocation(String code, String name, Long tenantId, LocationType type) {
        return persistLocation(code, name, tenantId, type, true, 0, false, null);
    }

    @Test
    void findByIdAndTenantId_existingLocation_returnsLocation() {
        Location l = persistLocation("LOC-1", "Location 1", 1L, LocationType.WAREHOUSE);

        Optional<Location> result = locationRepository.findByIdAndTenantId(l.getId(), 1L);

        assertTrue(result.isPresent());
        assertEquals(l.getId(), result.get().getId());
    }

    @Test
    void findByCodeAndTenantId_existingCode_returnsLocation() {
        persistLocation("FIND-LOC", "Findable", 1L, LocationType.WAREHOUSE);

        Optional<Location> result = locationRepository.findByCodeAndTenantId("FIND-LOC", 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByTenantIdOrderBySortOrderAsc_returnsSorted() {
        persistLocation("L-B", "Loc B", 1L, LocationType.WAREHOUSE, true, 2, false, null);
        persistLocation("L-A", "Loc A", 1L, LocationType.WAREHOUSE, true, 1, false, null);

        List<Location> result = locationRepository.findByTenantIdOrderBySortOrderAsc(1L);

        assertEquals(2, result.size());
        assertEquals("L-A", result.get(0).getCode());
    }

    @Test
    void findByTenantIdAndActiveOrderBySortOrderAsc_returnsActiveOnly() {
        persistLocation("ACTIVE-L", "Active", 1L, LocationType.WAREHOUSE, true, 1, false, null);
        persistLocation("INACTIVE-L", "Inactive", 1L, LocationType.WAREHOUSE, false, 2, false, null);

        List<Location> result = locationRepository.findByTenantIdAndActiveOrderBySortOrderAsc(1L, true);

        assertEquals(1, result.size());
        assertEquals("ACTIVE-L", result.get(0).getCode());
    }

    @Test
    void findByTenantIdAndLocationTypeOrderBySortOrderAsc_filtersByType() {
        persistLocation("WH-1", "Warehouse", 1L, LocationType.WAREHOUSE);
        persistLocation("ST-1", "Store", 1L, LocationType.STORE);

        List<Location> result = locationRepository.findByTenantIdAndLocationTypeOrderBySortOrderAsc(1L, LocationType.WAREHOUSE);

        assertEquals(1, result.size());
        assertEquals("WH-1", result.get(0).getCode());
    }

    @Test
    void findByParentLocationIdAndTenantIdOrderBySortOrderAsc_returnsChildren() {
        Location parent = persistLocation("PARENT", "Parent", 1L, LocationType.WAREHOUSE);
        persistLocation("CHILD-1", "Child 1", 1L, LocationType.ZONE, true, 1, false, parent);
        persistLocation("CHILD-2", "Child 2", 1L, LocationType.ZONE, true, 2, false, parent);

        List<Location> result = locationRepository.findByParentLocationIdAndTenantIdOrderBySortOrderAsc(parent.getId(), 1L);

        assertEquals(2, result.size());
    }

    @Test
    void findRootLocationsByTenantId_returnsOnlyRoots() {
        Location root = persistLocation("ROOT-L", "Root", 1L, LocationType.WAREHOUSE);
        persistLocation("CHILD-L", "Child", 1L, LocationType.ZONE, true, 0, false, root);

        List<Location> result = locationRepository.findRootLocationsByTenantId(1L);

        assertEquals(1, result.size());
        assertEquals("ROOT-L", result.get(0).getCode());
    }

    @Test
    void searchByNameOrCode_matchesNameAndCode() {
        persistLocation("SRCH-L", "Searchable Location", 1L, LocationType.WAREHOUSE);
        persistLocation("OTHER-L", "Other", 1L, LocationType.STORE);

        Page<Location> byName = locationRepository.searchByNameOrCode(1L, "Searchable", PageRequest.of(0, 10));
        assertEquals(1, byName.getTotalElements());

        Page<Location> byCode = locationRepository.searchByNameOrCode(1L, "SRCH", PageRequest.of(0, 10));
        assertEquals(1, byCode.getTotalElements());
    }

    @Test
    void existsByCodeAndTenantId_existingCode_returnsTrue() {
        persistLocation("EX-L", "Exists", 1L, LocationType.WAREHOUSE);

        assertTrue(locationRepository.existsByCodeAndTenantId("EX-L", 1L));
        assertFalse(locationRepository.existsByCodeAndTenantId("EX-L", 99L));
    }

    @Test
    void existsByCodeAndTenantIdAndIdNot_excludesSelf() {
        Location l = persistLocation("UNQ-L", "Unique", 1L, LocationType.WAREHOUSE);

        assertFalse(locationRepository.existsByCodeAndTenantIdAndIdNot("UNQ-L", 1L, l.getId()));
        assertTrue(locationRepository.existsByCodeAndTenantIdAndIdNot("UNQ-L", 1L, 999L));
    }

    @Test
    void findByTenantIdAndIsDefaultTrue_returnsDefaultLocation() {
        persistLocation("DEF-L", "Default", 1L, LocationType.WAREHOUSE, true, 0, true, null);
        persistLocation("OTHER-L2", "Other", 1L, LocationType.STORE, true, 0, false, null);

        Optional<Location> result = locationRepository.findByTenantIdAndIsDefaultTrue(1L);

        assertTrue(result.isPresent());
        assertEquals("DEF-L", result.get().getCode());
    }

    @Test
    void hasChildren_parentWithChildren_returnsTrue() {
        Location parent = persistLocation("HC-PARENT", "Parent", 1L, LocationType.WAREHOUSE);
        persistLocation("HC-CHILD", "Child", 1L, LocationType.ZONE, true, 0, false, parent);

        assertTrue(locationRepository.hasChildren(parent.getId(), 1L));
    }

    @Test
    void hasChildren_parentWithoutChildren_returnsFalse() {
        Location leaf = persistLocation("HC-LEAF", "Leaf", 1L, LocationType.WAREHOUSE);

        assertFalse(locationRepository.hasChildren(leaf.getId(), 1L));
    }

    @Test
    void findActiveByTypes_returnsActiveOfGivenTypes() {
        persistLocation("WH-ACT", "Active WH", 1L, LocationType.WAREHOUSE, true, 0, false, null);
        persistLocation("ST-ACT", "Active Store", 1L, LocationType.STORE, true, 0, false, null);
        persistLocation("WH-INACT", "Inactive WH", 1L, LocationType.WAREHOUSE, false, 0, false, null);
        persistLocation("VR-ACT", "Active Virtual", 1L, LocationType.VIRTUAL, true, 0, false, null);

        List<Location> result = locationRepository.findActiveByTypes(1L,
                List.of(LocationType.WAREHOUSE, LocationType.STORE));

        assertEquals(2, result.size());
    }

    @Test
    void findByTenantId_paged_returnsAllForTenant() {
        persistLocation("P1", "Page 1", 1L, LocationType.WAREHOUSE);
        persistLocation("P2", "Page 2", 1L, LocationType.STORE);
        persistLocation("P3", "Page 3", 2L, LocationType.WAREHOUSE);

        Page<Location> result = locationRepository.findByTenantId(1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }
}
