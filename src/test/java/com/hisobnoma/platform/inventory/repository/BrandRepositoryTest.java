package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Brand;
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
class BrandRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private BrandRepository brandRepository;

    private Brand persistBrand(String code, String name, Long tenantId, boolean active, int sortOrder) {
        Brand b = Brand.builder()
                .code(code)
                .name(name)
                .active(active)
                .sortOrder(sortOrder)
                .build();
        b.setTenantId(tenantId);
        return em.persistAndFlush(b);
    }

    @Test
    void findAllByTenantId_list_returnsSortedByOrder() {
        persistBrand("BR-B", "Brand B", 1L, true, 2);
        persistBrand("BR-A", "Brand A", 1L, true, 1);
        persistBrand("BR-OTHER", "Other Brand", 2L, true, 1);

        List<Brand> result = brandRepository.findAllByTenantId(1L);

        assertEquals(2, result.size());
        assertEquals("BR-A", result.get(0).getCode());
        assertEquals("BR-B", result.get(1).getCode());
    }

    @Test
    void findAllByTenantId_page_includesNullTenant() {
        persistBrand("BR-1", "Brand 1", 1L, true, 0);
        persistBrand("BR-G", "Global Brand", null, true, 0);
        persistBrand("BR-2", "Brand 2", 2L, true, 0);

        Page<Brand> result = brandRepository.findAllByTenantId(1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByCodeAndTenantId_existingCode_returnsBrand() {
        persistBrand("FIND-BR", "Findable", 1L, true, 0);

        Optional<Brand> result = brandRepository.findByCodeAndTenantId("FIND-BR", 1L);

        assertTrue(result.isPresent());
        assertEquals("FIND-BR", result.get().getCode());
    }

    @Test
    void findAllActiveByTenantId_returnsOnlyActive() {
        persistBrand("ACT-BR", "Active", 1L, true, 1);
        persistBrand("INACT-BR", "Inactive", 1L, false, 2);

        List<Brand> result = brandRepository.findAllActiveByTenantId(1L);

        assertEquals(1, result.size());
        assertEquals("ACT-BR", result.get(0).getCode());
    }

    @Test
    void searchByTenantId_matchesNameAndCode() {
        persistBrand("SRCH-BR", "Searchable Brand", 1L, true, 0);
        persistBrand("OTHER-BR", "Other", 1L, true, 0);

        Page<Brand> byName = brandRepository.searchByTenantId(1L, "Searchable", PageRequest.of(0, 10));
        assertEquals(1, byName.getTotalElements());

        Page<Brand> byCode = brandRepository.searchByTenantId(1L, "SRCH", PageRequest.of(0, 10));
        assertEquals(1, byCode.getTotalElements());
    }

    @Test
    void existsByCodeAndTenantId_existingCode_returnsTrue() {
        persistBrand("EX-BR", "Exists", 1L, true, 0);

        assertTrue(brandRepository.existsByCodeAndTenantId("EX-BR", 1L));
        assertFalse(brandRepository.existsByCodeAndTenantId("EX-BR", 99L));
    }

    @Test
    void existsByCodeAndTenantIdIsNull_globalBrand_returnsTrue() {
        persistBrand("GL-BR", "Global", null, true, 0);

        assertTrue(brandRepository.existsByCodeAndTenantIdIsNull("GL-BR"));
        assertFalse(brandRepository.existsByCodeAndTenantIdIsNull("NONEXISTENT"));
    }
}
