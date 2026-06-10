package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.web.dto.PublicCatalogProductDto;
import com.hisobnoma.platform.web.dto.PublicCategoryDto;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebCatalogPublicServiceTest {

    @Mock
    private WebCatalogItemRepository catalogRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private WebCatalogPublicService service;

    // No TenantContext set in unit tests, so the service falls back to the default tenant.
    private static final Long DEFAULT_TENANT_ID = 1L;

    private Product product(Long id, String name, BigDecimal sellingPrice, boolean trackInventory) {
        Product p = Product.builder()
                .sku("SKU-" + id).name(name)
                .sellingPrice(sellingPrice)
                .active(true).sellable(true)
                .trackInventory(trackInventory)
                .tenantId(DEFAULT_TENANT_ID)
                .build();
        p.setId(id);
        return p;
    }

    private WebCatalogItem item(Long id, Product product, WebCatalogStatus status) {
        WebCatalogItem item = WebCatalogItem.builder()
                .product(product).status(status).sortOrder(1).tenantId(DEFAULT_TENANT_ID)
                .build();
        item.setId(id);
        return item;
    }

    // ---- getProducts ----

    @Test
    void getProducts_mapsEffectivePriceWithOverrideFallback() {
        Product p = product(10L, "Cola", new BigDecimal("12000"), false);
        WebCatalogItem withOverride = item(1L, p, WebCatalogStatus.LIVE);
        withOverride.setPriceOverride(new BigDecimal("9999"));
        WebCatalogItem withoutOverride = item(2L, product(11L, "Juice", new BigDecimal("8000"), false), WebCatalogStatus.LIVE);

        when(catalogRepository.findVisible(eq(DEFAULT_TENANT_ID), eq(WebCatalogStatus.LIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(withOverride, withoutOverride)));
        when(stockRepository.getTotalQuantitiesByTenant(DEFAULT_TENANT_ID)).thenReturn(List.of());

        Page<PublicCatalogProductDto> page = service.getProducts(null, null, PageRequest.of(0, 20));

        assertEquals(new BigDecimal("9999"), page.getContent().get(0).getPrice());
        assertEquals(new BigDecimal("8000"), page.getContent().get(1).getPrice());
        assertEquals("UZS", page.getContent().get(0).getCurrency());
    }

    @Test
    void getProducts_inStockTrueWhenInventoryNotTracked() {
        Product untracked = product(10L, "Service Item", new BigDecimal("5000"), false);
        when(catalogRepository.findVisible(eq(DEFAULT_TENANT_ID), eq(WebCatalogStatus.LIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item(1L, untracked, WebCatalogStatus.LIVE))));
        when(stockRepository.getTotalQuantitiesByTenant(DEFAULT_TENANT_ID)).thenReturn(List.of());

        Page<PublicCatalogProductDto> page = service.getProducts(null, null, PageRequest.of(0, 20));

        assertTrue(page.getContent().get(0).isInStock());
    }

    @Test
    void getProducts_inStockReflectsStockQuantities() {
        Product tracked = product(10L, "Cola", new BigDecimal("12000"), true);
        Product trackedEmpty = product(11L, "Juice", new BigDecimal("8000"), true);
        when(catalogRepository.findVisible(eq(DEFAULT_TENANT_ID), eq(WebCatalogStatus.LIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        item(1L, tracked, WebCatalogStatus.LIVE),
                        item(2L, trackedEmpty, WebCatalogStatus.LIVE))));
        when(stockRepository.getTotalQuantitiesByTenant(DEFAULT_TENANT_ID))
                .thenReturn(List.<Object[]>of(new Object[]{10L, new BigDecimal("5")}));

        Page<PublicCatalogProductDto> page = service.getProducts(null, null, PageRequest.of(0, 20));

        assertTrue(page.getContent().get(0).isInStock());
        assertFalse(page.getContent().get(1).isInStock());
    }

    @Test
    void getProducts_searchUsesSearchQuery() {
        when(catalogRepository.searchVisible(eq(DEFAULT_TENANT_ID), eq(WebCatalogStatus.LIVE), eq("%cola%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(stockRepository.getTotalQuantitiesByTenant(DEFAULT_TENANT_ID)).thenReturn(List.of());

        Page<PublicCatalogProductDto> page = service.getProducts("  Cola ", null, PageRequest.of(0, 20));

        assertEquals(0, page.getTotalElements());
    }

    @Test
    void getProducts_categoryFilterUsesCategoryQuery() {
        when(catalogRepository.findVisibleByCategory(eq(DEFAULT_TENANT_ID), eq(WebCatalogStatus.LIVE), eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(stockRepository.getTotalQuantitiesByTenant(DEFAULT_TENANT_ID)).thenReturn(List.of());

        Page<PublicCatalogProductDto> page = service.getProducts(null, 5L, PageRequest.of(0, 20));

        assertEquals(0, page.getTotalElements());
    }

    // ---- getProduct ----

    @Test
    void getProduct_returnsLiveItem() {
        Product p = product(10L, "Cola", new BigDecimal("12000"), false);
        when(catalogRepository.findByIdAndTenantId(1L, DEFAULT_TENANT_ID))
                .thenReturn(Optional.of(item(1L, p, WebCatalogStatus.LIVE)));

        PublicCatalogProductDto dto = service.getProduct(1L);

        assertEquals("Cola", dto.getName());
        assertTrue(dto.isInStock());
    }

    @Test
    void getProduct_draftItemThrowsNotFound() {
        Product p = product(10L, "Cola", new BigDecimal("12000"), false);
        when(catalogRepository.findByIdAndTenantId(1L, DEFAULT_TENANT_ID))
                .thenReturn(Optional.of(item(1L, p, WebCatalogStatus.DRAFT)));

        assertThrows(NotFoundException.class, () -> service.getProduct(1L));
    }

    @Test
    void getProduct_inactiveProductThrowsNotFound() {
        Product p = product(10L, "Cola", new BigDecimal("12000"), false);
        p.setActive(false);
        when(catalogRepository.findByIdAndTenantId(1L, DEFAULT_TENANT_ID))
                .thenReturn(Optional.of(item(1L, p, WebCatalogStatus.LIVE)));

        assertThrows(NotFoundException.class, () -> service.getProduct(1L));
    }

    @Test
    void getProduct_displayNameOverrideUsedAsName() {
        Product p = product(10L, "Cola", new BigDecimal("12000"), false);
        WebCatalogItem live = item(1L, p, WebCatalogStatus.LIVE);
        live.setDisplayName("Cola Special");
        when(catalogRepository.findByIdAndTenantId(1L, DEFAULT_TENANT_ID)).thenReturn(Optional.of(live));

        assertEquals("Cola Special", service.getProduct(1L).getName());
    }

    // ---- getCategories ----

    @Test
    void getCategories_mapsIdAndName() {
        when(catalogRepository.findVisibleCategories(DEFAULT_TENANT_ID, WebCatalogStatus.LIVE))
                .thenReturn(List.<Object[]>of(new Object[]{3L, "Drinks"}, new Object[]{4L, "Snacks"}));

        List<PublicCategoryDto> categories = service.getCategories();

        assertEquals(2, categories.size());
        assertEquals(3L, categories.get(0).getId());
        assertEquals("Drinks", categories.get(0).getName());
    }
}
