package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Category;
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
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category persistCategory(String code, String name, Long tenantId,
                                     boolean active, int sortOrder, Category parent) {
        Category c = Category.builder()
                .code(code)
                .name(name)
                .active(active)
                .sortOrder(sortOrder)
                .parent(parent)
                .level(parent == null ? 0 : 1)
                .build();
        c.setTenantId(tenantId);
        return em.persistAndFlush(c);
    }

    private Category persistCategory(String code, String name, Long tenantId) {
        return persistCategory(code, name, tenantId, true, 0, null);
    }

    @Test
    void findAllByTenantId_returnsSortedByOrder() {
        persistCategory("C-B", "Cat B", 1L, true, 2, null);
        persistCategory("C-A", "Cat A", 1L, true, 1, null);
        persistCategory("C-OTHER", "Other", 2L, true, 1, null);

        List<Category> result = categoryRepository.findAllByTenantId(1L);

        assertEquals(2, result.size());
        assertEquals("C-A", result.get(0).getCode());
    }

    @Test
    void findRootCategoriesByTenantId_returnsOnlyRoots() {
        Category root = persistCategory("ROOT", "Root", 1L, true, 1, null);
        persistCategory("CHILD", "Child", 1L, true, 1, root);

        List<Category> result = categoryRepository.findRootCategoriesByTenantId(1L);

        assertEquals(1, result.size());
        assertEquals("ROOT", result.get(0).getCode());
    }

    @Test
    void findChildrenByParentId_returnsChildrenOfParent() {
        Category root = persistCategory("ROOT2", "Root 2", 1L, true, 0, null);
        persistCategory("CH1", "Child 1", 1L, true, 1, root);
        persistCategory("CH2", "Child 2", 1L, true, 2, root);

        List<Category> result = categoryRepository.findChildrenByParentId(1L, root.getId());

        assertEquals(2, result.size());
    }

    @Test
    void findByCodeAndTenantId_existingCode_returnsCategory() {
        persistCategory("FIND-CAT", "Findable", 1L);

        Optional<Category> result = categoryRepository.findByCodeAndTenantId("FIND-CAT", 1L);

        assertTrue(result.isPresent());
        assertEquals("FIND-CAT", result.get().getCode());
    }

    @Test
    void findAllActiveByTenantId_returnsOnlyActive() {
        persistCategory("ACT-C", "Active", 1L, true, 0, null);
        persistCategory("INACT-C", "Inactive", 1L, false, 0, null);

        List<Category> result = categoryRepository.findAllActiveByTenantId(1L);

        assertEquals(1, result.size());
        assertEquals("ACT-C", result.get(0).getCode());
    }

    @Test
    void searchByTenantId_matchesNameAndCode() {
        persistCategory("SRCH-C", "Searchable Category", 1L);
        persistCategory("OTHER-C", "Other", 1L);

        Page<Category> byName = categoryRepository.searchByTenantId(1L, "Searchable", PageRequest.of(0, 10));
        assertEquals(1, byName.getTotalElements());

        Page<Category> byCode = categoryRepository.searchByTenantId(1L, "SRCH", PageRequest.of(0, 10));
        assertEquals(1, byCode.getTotalElements());
    }

    @Test
    void existsByCodeAndTenantId_existingCode_returnsTrue() {
        persistCategory("EX-C", "Exists", 1L);

        assertTrue(categoryRepository.existsByCodeAndTenantId("EX-C", 1L));
        assertFalse(categoryRepository.existsByCodeAndTenantId("EX-C", 99L));
    }

    @Test
    void existsByCodeAndTenantIdIsNull_globalCategory_returnsTrue() {
        persistCategory("GL-C", "Global", null);

        assertTrue(categoryRepository.existsByCodeAndTenantIdIsNull("GL-C"));
        assertFalse(categoryRepository.existsByCodeAndTenantIdIsNull("NONEXISTENT"));
    }

    @Test
    void findByIdWithChildren_loadsChildren() {
        Category root = persistCategory("ROOT-WC", "Root With Children", 1L, true, 0, null);
        persistCategory("CHILD-WC", "Child", 1L, true, 0, root);
        em.clear();

        Optional<Category> result = categoryRepository.findByIdWithChildren(root.getId(), 1L);

        assertTrue(result.isPresent());
        assertFalse(result.get().getChildren().isEmpty());
        assertEquals(1, result.get().getChildren().size());
        assertTrue(categoryRepository.findByIdWithChildren(root.getId(), 2L).isEmpty(),
                "Another tenant must not load this category by id");
    }

    @Test
    void findByIdAndTenantId_existingCategory_returnsCategory() {
        Category c = persistCategory("ID-C", "Find by ID", 1L);

        Optional<Category> result = categoryRepository.findByIdAndTenantId(c.getId(), 1L);

        assertTrue(result.isPresent());
        assertEquals(c.getId(), result.get().getId());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        Category c = persistCategory("ID-C2", "Find by ID 2", 1L);

        Optional<Category> result = categoryRepository.findByIdAndTenantId(c.getId(), 99L);

        assertTrue(result.isEmpty());
    }
}
