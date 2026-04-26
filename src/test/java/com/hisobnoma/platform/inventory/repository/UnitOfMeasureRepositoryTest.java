package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class UnitOfMeasureRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UnitOfMeasureRepository uomRepository;

    private UnitOfMeasure persistUom(String code, String name, Long tenantId,
                                     boolean active, boolean isBaseUnit) {
        UnitOfMeasure u = UnitOfMeasure.builder()
                .code(code)
                .name(name)
                .active(active)
                .isBaseUnit(isBaseUnit)
                .build();
        u.setTenantId(tenantId);
        return em.persistAndFlush(u);
    }

    @Test
    void findAllByTenantId_list_includesNullTenant() {
        persistUom("PCS", "Pieces", 1L, true, true);
        persistUom("KG", "Kilogram", null, true, true);
        persistUom("LTR", "Liters", 2L, true, true);

        List<UnitOfMeasure> result = uomRepository.findAllByTenantId(1L);

        assertEquals(2, result.size());
    }

    @Test
    void findAllByTenantId_paged_includesNullTenant() {
        persistUom("PCS2", "Pieces", 1L, true, true);
        persistUom("KG2", "Kilogram", null, true, true);

        Page<UnitOfMeasure> result = uomRepository.findAllByTenantId(1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByCodeAndTenantId_existingCode_returnsUom() {
        persistUom("FIND-U", "Findable", 1L, true, true);

        Optional<UnitOfMeasure> result = uomRepository.findByCodeAndTenantId("FIND-U", 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByCode_nullTenant_returnsUom() {
        persistUom("GLB-U", "Global UOM", null, true, true);

        Optional<UnitOfMeasure> result = uomRepository.findByCode("GLB-U");

        assertTrue(result.isPresent());
    }

    @Test
    void findAllActiveByTenantId_returnsOnlyActive() {
        persistUom("ACT-U", "Active", 1L, true, true);
        persistUom("INACT-U", "Inactive", 1L, false, true);

        List<UnitOfMeasure> result = uomRepository.findAllActiveByTenantId(1L);

        assertEquals(1, result.size());
        assertEquals("ACT-U", result.get(0).getCode());
    }

    @Test
    void findAllBaseUnitsByTenantId_returnsOnlyBaseUnits() {
        persistUom("BASE-U", "Base Unit", 1L, true, true);
        persistUom("DERIV-U", "Derived Unit", 1L, true, false);

        List<UnitOfMeasure> result = uomRepository.findAllBaseUnitsByTenantId(1L);

        assertEquals(1, result.size());
        assertEquals("BASE-U", result.get(0).getCode());
    }

    @Test
    void searchByTenantId_matchesNameAndCode() {
        persistUom("SRCH-U", "Searchable Unit", 1L, true, true);
        persistUom("OTHER-U", "Other", 1L, true, true);

        Page<UnitOfMeasure> byName = uomRepository.searchByTenantId(1L, "Searchable", PageRequest.of(0, 10));
        assertEquals(1, byName.getTotalElements());

        Page<UnitOfMeasure> byCode = uomRepository.searchByTenantId(1L, "SRCH", PageRequest.of(0, 10));
        assertEquals(1, byCode.getTotalElements());
    }

    @Test
    void existsByCodeAndTenantId_existingCode_returnsTrue() {
        persistUom("EX-U", "Exists", 1L, true, true);

        assertTrue(uomRepository.existsByCodeAndTenantId("EX-U", 1L));
        assertFalse(uomRepository.existsByCodeAndTenantId("EX-U", 99L));
    }

    @Test
    void existsByCodeAndTenantIdIsNull_globalUom_returnsTrue() {
        persistUom("GL-U", "Global", null, true, true);

        assertTrue(uomRepository.existsByCodeAndTenantIdIsNull("GL-U"));
        assertFalse(uomRepository.existsByCodeAndTenantIdIsNull("NONEXISTENT"));
    }

    @Test
    void findByBaseUomId_returnsRelatedUnits() {
        UnitOfMeasure base = persistUom("BASE-R", "Base", 1L, true, true);

        UnitOfMeasure derived = UnitOfMeasure.builder()
                .code("DERIV-R")
                .name("Derived")
                .isBaseUnit(false)
                .baseUom(base)
                .conversionFactor(new BigDecimal("12"))
                .active(true)
                .build();
        derived.setTenantId(1L);
        em.persistAndFlush(derived);

        List<UnitOfMeasure> result = uomRepository.findByBaseUomId(base.getId());

        assertEquals(1, result.size());
        assertEquals("DERIV-R", result.get(0).getCode());
    }

    @Test
    void findByIdAndTenantId_existingUom_returnsUom() {
        UnitOfMeasure u = persistUom("ID-U", "By ID", 1L, true, true);

        Optional<UnitOfMeasure> result = uomRepository.findByIdAndTenantId(u.getId(), 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        UnitOfMeasure u = persistUom("ID-U2", "By ID 2", 1L, true, true);

        Optional<UnitOfMeasure> result = uomRepository.findByIdAndTenantId(u.getId(), 99L);

        assertTrue(result.isEmpty());
    }
}
