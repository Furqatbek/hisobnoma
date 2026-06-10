package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class WebCatalogItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WebCatalogItemRepository repository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private UnitOfMeasure uom;
    private Category category1;
    private Category category2;
    private Product liveProduct;
    private Product draftProduct;
    private Product inactiveProduct;
    private Product otherTenantProduct;

    private WebCatalogItem liveItem;
    private WebCatalogItem draftItem;
    private WebCatalogItem inactiveProductItem;
    private WebCatalogItem otherTenantItem;

    @BeforeEach
    void setUp() {
        uom = entityManager.persistAndFlush(UnitOfMeasure.builder()
                .code("PCS").name("Pieces").tenantId(TENANT_ID).active(true).isBaseUnit(true).build());

        category1 = entityManager.persistAndFlush(Category.builder()
                .code("CAT1").name("Drinks").tenantId(TENANT_ID).active(true).build());
        category2 = entityManager.persistAndFlush(Category.builder()
                .code("CAT2").name("Snacks").tenantId(TENANT_ID).active(true).build());

        liveProduct = persistProduct("SKU-001", "Cola Bottle", category1, true, true, TENANT_ID);
        draftProduct = persistProduct("SKU-002", "Orange Juice", category1, true, true, TENANT_ID);
        inactiveProduct = persistProduct("SKU-003", "Old Chips", category2, false, true, TENANT_ID);
        otherTenantProduct = persistProduct("SKU-004", "Foreign Cola", category1, true, true, OTHER_TENANT_ID);

        liveItem = persistItem(liveProduct, WebCatalogStatus.LIVE, 1, TENANT_ID);
        draftItem = persistItem(draftProduct, WebCatalogStatus.DRAFT, 2, TENANT_ID);
        inactiveProductItem = persistItem(inactiveProduct, WebCatalogStatus.LIVE, 3, TENANT_ID);
        otherTenantItem = persistItem(otherTenantProduct, WebCatalogStatus.LIVE, 1, OTHER_TENANT_ID);
    }

    private Product persistProduct(String sku, String name, Category category,
                                   boolean active, boolean sellable, Long tenantId) {
        return entityManager.persistAndFlush(Product.builder()
                .sku(sku).name(name)
                .category(category)
                .baseUom(uom)
                .sellingPrice(new BigDecimal("10000.0000"))
                .costPrice(new BigDecimal("7000.0000"))
                .active(active).sellable(sellable)
                .tenantId(tenantId)
                .build());
    }

    private WebCatalogItem persistItem(Product product, WebCatalogStatus status, int sortOrder, Long tenantId) {
        return entityManager.persistAndFlush(WebCatalogItem.builder()
                .product(product).status(status).sortOrder(sortOrder).tenantId(tenantId)
                .build());
    }

    @Test
    void findVisible_returnsOnlyLiveItemsWithActiveSellableProductsOfTenant() {
        Page<WebCatalogItem> page = repository.findVisible(TENANT_ID, WebCatalogStatus.LIVE, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(liveItem.getId(), page.getContent().get(0).getId());
    }

    @Test
    void findVisible_excludesDraftItems() {
        Page<WebCatalogItem> page = repository.findVisible(TENANT_ID, WebCatalogStatus.LIVE, PageRequest.of(0, 10));

        assertTrue(page.getContent().stream().noneMatch(i -> i.getId().equals(draftItem.getId())));
    }

    @Test
    void findVisible_excludesItemsOfInactiveProducts() {
        Page<WebCatalogItem> page = repository.findVisible(TENANT_ID, WebCatalogStatus.LIVE, PageRequest.of(0, 10));

        assertTrue(page.getContent().stream().noneMatch(i -> i.getId().equals(inactiveProductItem.getId())));
    }

    @Test
    void findVisible_excludesOtherTenantItems() {
        Page<WebCatalogItem> page = repository.findVisible(TENANT_ID, WebCatalogStatus.LIVE, PageRequest.of(0, 10));

        assertTrue(page.getContent().stream().noneMatch(i -> i.getId().equals(otherTenantItem.getId())));
    }

    @Test
    void findVisible_ordersBySortOrder() {
        Product anotherProduct = persistProduct("SKU-010", "Apple Juice", category1, true, true, TENANT_ID);
        WebCatalogItem first = persistItem(anotherProduct, WebCatalogStatus.LIVE, 0, TENANT_ID);

        Page<WebCatalogItem> page = repository.findVisible(TENANT_ID, WebCatalogStatus.LIVE, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertEquals(first.getId(), page.getContent().get(0).getId());
        assertEquals(liveItem.getId(), page.getContent().get(1).getId());
    }

    @Test
    void searchVisible_matchesProductNameCaseInsensitive() {
        Page<WebCatalogItem> page = repository.searchVisible(
                TENANT_ID, WebCatalogStatus.LIVE, "%cola%", PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(liveItem.getId(), page.getContent().get(0).getId());
    }

    @Test
    void searchVisible_matchesDisplayNameOverride() {
        liveItem.setDisplayName("Special Drink");
        entityManager.persistAndFlush(liveItem);

        Page<WebCatalogItem> page = repository.searchVisible(
                TENANT_ID, WebCatalogStatus.LIVE, "%special%", PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findVisibleByCategory_filtersByProductCategory() {
        Page<WebCatalogItem> drinks = repository.findVisibleByCategory(
                TENANT_ID, WebCatalogStatus.LIVE, category1.getId(), PageRequest.of(0, 10));
        Page<WebCatalogItem> snacks = repository.findVisibleByCategory(
                TENANT_ID, WebCatalogStatus.LIVE, category2.getId(), PageRequest.of(0, 10));

        assertEquals(1, drinks.getTotalElements());
        assertEquals(0, snacks.getTotalElements());
    }

    @Test
    void findVisibleCategories_returnsDistinctCategoriesOfLiveItems() {
        List<Object[]> categories = repository.findVisibleCategories(TENANT_ID, WebCatalogStatus.LIVE);

        assertEquals(1, categories.size());
        assertEquals(category1.getId(), categories.get(0)[0]);
        assertEquals("Drinks", categories.get(0)[1]);
    }

    @Test
    void countByTenantIdAndStatus_countsPerStatus() {
        assertEquals(2, repository.countByTenantIdAndStatus(TENANT_ID, WebCatalogStatus.LIVE));
        assertEquals(1, repository.countByTenantIdAndStatus(TENANT_ID, WebCatalogStatus.DRAFT));
        assertEquals(1, repository.countByTenantIdAndStatus(OTHER_TENANT_ID, WebCatalogStatus.LIVE));
    }

    @Test
    void existsByTenantIdAndProductId_detectsExistingProduct() {
        assertTrue(repository.existsByTenantIdAndProductId(TENANT_ID, liveProduct.getId()));
        assertFalse(repository.existsByTenantIdAndProductId(TENANT_ID, otherTenantProduct.getId()));
    }

    @Test
    void findByIdAndTenantId_scopesByTenant() {
        assertTrue(repository.findByIdAndTenantId(liveItem.getId(), TENANT_ID).isPresent());
        assertFalse(repository.findByIdAndTenantId(otherTenantItem.getId(), TENANT_ID).isPresent());
    }

    @Test
    void neighborQueries_findAdjacentSortOrders() {
        Optional<WebCatalogItem> above = repository
                .findFirstByTenantIdAndSortOrderLessThanOrderBySortOrderDesc(TENANT_ID, draftItem.getSortOrder());
        Optional<WebCatalogItem> below = repository
                .findFirstByTenantIdAndSortOrderGreaterThanOrderBySortOrderAsc(TENANT_ID, draftItem.getSortOrder());

        assertTrue(above.isPresent());
        assertEquals(liveItem.getId(), above.get().getId());
        assertTrue(below.isPresent());
        assertEquals(inactiveProductItem.getId(), below.get().getId());
    }

    @Test
    void neighborQueries_emptyAtEdges() {
        assertTrue(repository
                .findFirstByTenantIdAndSortOrderLessThanOrderBySortOrderDesc(TENANT_ID, liveItem.getSortOrder())
                .isEmpty());
        assertTrue(repository
                .findFirstByTenantIdAndSortOrderGreaterThanOrderBySortOrderAsc(TENANT_ID, inactiveProductItem.getSortOrder())
                .isEmpty());
    }

    @Test
    void findMaxSortOrder_returnsMaxOrZeroWhenEmpty() {
        assertEquals(3, repository.findMaxSortOrder(TENANT_ID));
        assertEquals(0, repository.findMaxSortOrder(99L));
    }

    @Test
    void findAllByTenant_returnsAllItemsOfTenantOrdered() {
        Page<WebCatalogItem> page = repository.findAllByTenant(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(3, page.getTotalElements());
        assertEquals(liveItem.getId(), page.getContent().get(0).getId());
    }

    @Test
    void searchByTenant_matchesSku() {
        Page<WebCatalogItem> page = repository.searchByTenant(TENANT_ID, "%sku-002%", PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(draftItem.getId(), page.getContent().get(0).getId());
    }
}
