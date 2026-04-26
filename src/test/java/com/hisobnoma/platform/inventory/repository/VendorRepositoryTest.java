package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Vendor;
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
class VendorRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private VendorRepository vendorRepository;

    private Vendor persistVendor(String code, String name, Long tenantId,
                                 boolean active, boolean preferred) {
        Vendor v = Vendor.builder()
                .code(code)
                .name(name)
                .active(active)
                .preferred(preferred)
                .build();
        v.setTenantId(tenantId);
        return em.persistAndFlush(v);
    }

    private Vendor persistVendor(String code, String name, Long tenantId) {
        return persistVendor(code, name, tenantId, true, false);
    }

    @Test
    void findByIdAndTenantId_existingVendor_returnsVendor() {
        Vendor v = persistVendor("V-1", "Vendor 1", 1L);

        Optional<Vendor> result = vendorRepository.findByIdAndTenantId(v.getId(), 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByIdAndTenantId_includesNullTenant() {
        Vendor v = persistVendor("V-G", "Global Vendor", null);

        Optional<Vendor> result = vendorRepository.findByIdAndTenantId(v.getId(), 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findAllByTenantId_paged_includesNullTenant() {
        persistVendor("V-1", "Vendor 1", 1L);
        persistVendor("V-G", "Global", null);
        persistVendor("V-2", "Vendor 2", 2L);

        Page<Vendor> result = vendorRepository.findAllByTenantId(1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findAllByTenantId_list_includesNullTenant() {
        persistVendor("V-L1", "Vendor L1", 1L);
        persistVendor("V-LG", "Global", null);

        List<Vendor> result = vendorRepository.findAllByTenantId(1L);

        assertEquals(2, result.size());
    }

    @Test
    void findAllActiveByTenantId_returnsOnlyActive() {
        persistVendor("ACT-V", "Active", 1L, true, false);
        persistVendor("INACT-V", "Inactive", 1L, false, false);

        List<Vendor> result = vendorRepository.findAllActiveByTenantId(1L);

        assertEquals(1, result.size());
        assertEquals("ACT-V", result.get(0).getCode());
    }

    @Test
    void findByCodeAndTenantId_existingCode_returnsVendor() {
        persistVendor("FIND-V", "Findable", 1L);

        Optional<Vendor> result = vendorRepository.findByCodeAndTenantId("FIND-V", 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void searchByTenantId_matchesMultipleFields() {
        Vendor v = Vendor.builder()
                .code("SRCH-V")
                .name("Searchable Vendor")
                .email("search@vendor.com")
                .phone("123456")
                .build();
        v.setTenantId(1L);
        em.persistAndFlush(v);

        Page<Vendor> byName = vendorRepository.searchByTenantId(1L, "Searchable", PageRequest.of(0, 10));
        assertEquals(1, byName.getTotalElements());

        Page<Vendor> byCode = vendorRepository.searchByTenantId(1L, "SRCH", PageRequest.of(0, 10));
        assertEquals(1, byCode.getTotalElements());

        Page<Vendor> byEmail = vendorRepository.searchByTenantId(1L, "search@vendor", PageRequest.of(0, 10));
        assertEquals(1, byEmail.getTotalElements());

        Page<Vendor> byPhone = vendorRepository.searchByTenantId(1L, "123456", PageRequest.of(0, 10));
        assertEquals(1, byPhone.getTotalElements());
    }

    @Test
    void findPreferredVendorsByTenantId_returnsOnlyPreferred() {
        persistVendor("PREF-V", "Preferred", 1L, true, true);
        persistVendor("NORM-V", "Normal", 1L, true, false);

        List<Vendor> result = vendorRepository.findPreferredVendorsByTenantId(1L);

        assertEquals(1, result.size());
        assertEquals("PREF-V", result.get(0).getCode());
    }

    @Test
    void existsByCodeAndTenantId_existingCode_returnsTrue() {
        persistVendor("EX-V", "Exists", 1L);

        assertTrue(vendorRepository.existsByCodeAndTenantId("EX-V", 1L));
        assertFalse(vendorRepository.existsByCodeAndTenantId("EX-V", 99L));
    }

    @Test
    void existsByCodeAndTenantIdIsNull_globalVendor_returnsTrue() {
        persistVendor("GL-V", "Global", null);

        assertTrue(vendorRepository.existsByCodeAndTenantIdIsNull("GL-V"));
        assertFalse(vendorRepository.existsByCodeAndTenantIdIsNull("NONEXISTENT"));
    }
}
