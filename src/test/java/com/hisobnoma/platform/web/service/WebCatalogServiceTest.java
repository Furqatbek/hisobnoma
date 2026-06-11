package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.web.dto.AddCatalogItemsRequest;
import com.hisobnoma.platform.web.dto.UpdateCatalogItemRequest;
import com.hisobnoma.platform.web.dto.WebCatalogCountsDto;
import com.hisobnoma.platform.web.dto.WebCatalogItemDto;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebWishlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebCatalogServiceTest {

    @Mock
    private WebCatalogItemRepository catalogRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private WebPromotionBadgeService badgeService;

    @Mock
    private WebWishlistItemRepository wishlistRepository;

    @InjectMocks
    private WebCatalogService service;

    private static final Long TENANT_ID = 1L;

    private Product activeProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);

        activeProduct = Product.builder()
                .sku("SKU-001").name("Cola")
                .sellingPrice(new BigDecimal("12000"))
                .active(true).sellable(true)
                .tenantId(TENANT_ID)
                .build();

        inactiveProduct = Product.builder()
                .sku("SKU-002").name("Old Product")
                .sellingPrice(new BigDecimal("5000"))
                .active(false).sellable(true)
                .tenantId(TENANT_ID)
                .build();
    }

    private WebCatalogItem item(Long id, Product product, WebCatalogStatus status, int sortOrder) {
        WebCatalogItem item = WebCatalogItem.builder()
                .product(product).status(status).sortOrder(sortOrder).tenantId(TENANT_ID)
                .build();
        item.setId(id);
        return item;
    }

    // ---- addProducts ----

    @Test
    void addProducts_savesNewDraftItemWithNextSortOrder() {
        when(catalogRepository.findMaxSortOrder(TENANT_ID)).thenReturn(5);
        when(catalogRepository.existsByTenantIdAndProductId(TENANT_ID, 10L)).thenReturn(false);
        when(productRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(activeProduct));
        when(catalogRepository.save(any(WebCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        List<WebCatalogItemDto> added = service.addProducts(new AddCatalogItemsRequest(List.of(10L)));

        assertEquals(1, added.size());
        assertEquals(WebCatalogStatus.DRAFT, added.get(0).getStatus());
        assertEquals(6, added.get(0).getSortOrder());
        assertEquals(new BigDecimal("12000"), added.get(0).getEffectivePrice());
    }

    @Test
    void addProducts_skipsProductsAlreadyInCatalog() {
        when(catalogRepository.findMaxSortOrder(TENANT_ID)).thenReturn(0);
        when(catalogRepository.existsByTenantIdAndProductId(TENANT_ID, 10L)).thenReturn(true);

        List<WebCatalogItemDto> added = service.addProducts(new AddCatalogItemsRequest(List.of(10L)));

        assertTrue(added.isEmpty());
        verify(catalogRepository, never()).save(any());
    }

    @Test
    void addProducts_rejectsInactiveProduct() {
        when(catalogRepository.findMaxSortOrder(TENANT_ID)).thenReturn(0);
        when(catalogRepository.existsByTenantIdAndProductId(TENANT_ID, 20L)).thenReturn(false);
        when(productRepository.findByIdAndTenantId(20L, TENANT_ID)).thenReturn(Optional.of(inactiveProduct));

        assertThrows(ValidationException.class,
                () -> service.addProducts(new AddCatalogItemsRequest(List.of(20L))));
        verify(catalogRepository, never()).save(any());
    }

    @Test
    void addProducts_unknownProductThrowsNotFound() {
        when(catalogRepository.findMaxSortOrder(TENANT_ID)).thenReturn(0);
        when(catalogRepository.existsByTenantIdAndProductId(TENANT_ID, 99L)).thenReturn(false);
        when(productRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.addProducts(new AddCatalogItemsRequest(List.of(99L))));
    }

    // ---- publish / unpublish ----

    @Test
    void publish_setsStatusLive() {
        WebCatalogItem draft = item(1L, activeProduct, WebCatalogStatus.DRAFT, 1);
        when(catalogRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(draft));
        when(catalogRepository.save(any(WebCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        WebCatalogItemDto dto = service.publish(1L);

        assertEquals(WebCatalogStatus.LIVE, dto.getStatus());
    }

    @Test
    void publish_rejectsItemWithInactiveProduct() {
        WebCatalogItem draft = item(1L, inactiveProduct, WebCatalogStatus.DRAFT, 1);
        when(catalogRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(draft));

        assertThrows(ValidationException.class, () -> service.publish(1L));
        verify(catalogRepository, never()).save(any());
    }

    @Test
    void unpublish_setsStatusDraft() {
        WebCatalogItem live = item(1L, activeProduct, WebCatalogStatus.LIVE, 1);
        when(catalogRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(live));
        when(catalogRepository.save(any(WebCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        WebCatalogItemDto dto = service.unpublish(1L);

        assertEquals(WebCatalogStatus.DRAFT, dto.getStatus());
    }

    @Test
    void publishAll_publishesOnlyDraftsWithValidProducts() {
        WebCatalogItem draftValid = item(1L, activeProduct, WebCatalogStatus.DRAFT, 1);
        WebCatalogItem draftInvalid = item(2L, inactiveProduct, WebCatalogStatus.DRAFT, 2);
        WebCatalogItem alreadyLive = item(3L, activeProduct, WebCatalogStatus.LIVE, 3);
        when(catalogRepository.findByTenantIdOrderBySortOrderAsc(TENANT_ID))
                .thenReturn(List.of(draftValid, draftInvalid, alreadyLive));
        when(catalogRepository.save(any(WebCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        int published = service.publishAll();

        assertEquals(1, published);
        assertEquals(WebCatalogStatus.LIVE, draftValid.getStatus());
        assertEquals(WebCatalogStatus.DRAFT, draftInvalid.getStatus());
    }

    // ---- update ----

    @Test
    void updateItem_setsOverridesAndComputesEffectivePrice() {
        WebCatalogItem live = item(1L, activeProduct, WebCatalogStatus.LIVE, 1);
        when(catalogRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(live));
        when(catalogRepository.save(any(WebCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        WebCatalogItemDto dto = service.updateItem(1L, UpdateCatalogItemRequest.builder()
                .displayName("Cola 1L")
                .priceOverride(new BigDecimal("9999"))
                .build());

        assertEquals("Cola 1L", dto.getDisplayName());
        assertEquals(new BigDecimal("9999"), dto.getEffectivePrice());
        assertEquals(new BigDecimal("12000"), dto.getBasePrice());
    }

    @Test
    void updateItem_nullOverridesFallBackToProductValues() {
        WebCatalogItem live = item(1L, activeProduct, WebCatalogStatus.LIVE, 1);
        live.setDisplayName("Old name");
        live.setPriceOverride(new BigDecimal("8000"));
        when(catalogRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(live));
        when(catalogRepository.save(any(WebCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        WebCatalogItemDto dto = service.updateItem(1L, new UpdateCatalogItemRequest());

        assertNull(dto.getDisplayName());
        assertNull(dto.getPriceOverride());
        assertEquals(new BigDecimal("12000"), dto.getEffectivePrice());
    }

    // ---- move up / down ----

    @Test
    void moveUp_swapsSortOrderWithNeighborAbove() {
        WebCatalogItem current = item(2L, activeProduct, WebCatalogStatus.LIVE, 5);
        WebCatalogItem above = item(1L, activeProduct, WebCatalogStatus.LIVE, 3);
        when(catalogRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(current));
        when(catalogRepository.findFirstByTenantIdAndSortOrderLessThanOrderBySortOrderDesc(TENANT_ID, 5))
                .thenReturn(Optional.of(above));

        service.moveUp(2L);

        assertEquals(3, current.getSortOrder());
        assertEquals(5, above.getSortOrder());
        verify(catalogRepository, times(2)).save(any(WebCatalogItem.class));
    }

    @Test
    void moveUp_noopAtTop() {
        WebCatalogItem current = item(1L, activeProduct, WebCatalogStatus.LIVE, 1);
        when(catalogRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(current));
        when(catalogRepository.findFirstByTenantIdAndSortOrderLessThanOrderBySortOrderDesc(TENANT_ID, 1))
                .thenReturn(Optional.empty());

        service.moveUp(1L);

        assertEquals(1, current.getSortOrder());
        verify(catalogRepository, never()).save(any());
    }

    @Test
    void moveDown_swapsSortOrderWithNeighborBelow() {
        WebCatalogItem current = item(1L, activeProduct, WebCatalogStatus.LIVE, 1);
        WebCatalogItem below = item(2L, activeProduct, WebCatalogStatus.LIVE, 2);
        when(catalogRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(current));
        when(catalogRepository.findFirstByTenantIdAndSortOrderGreaterThanOrderBySortOrderAsc(TENANT_ID, 1))
                .thenReturn(Optional.of(below));

        service.moveDown(1L);

        assertEquals(2, current.getSortOrder());
        assertEquals(1, below.getSortOrder());
    }

    // ---- counts / remove ----

    @Test
    void getCounts_aggregatesPerStatus() {
        when(catalogRepository.countByTenantIdAndStatus(TENANT_ID, WebCatalogStatus.LIVE)).thenReturn(7L);
        when(catalogRepository.countByTenantIdAndStatus(TENANT_ID, WebCatalogStatus.DRAFT)).thenReturn(3L);

        WebCatalogCountsDto counts = service.getCounts();

        assertEquals(7, counts.getLive());
        assertEquals(3, counts.getDraft());
        assertEquals(10, counts.getTotal());
    }

    @Test
    void removeItem_deletesExistingItem() {
        WebCatalogItem live = item(1L, activeProduct, WebCatalogStatus.LIVE, 1);
        when(catalogRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(live));

        service.removeItem(1L);

        verify(catalogRepository).delete(live);
    }

    @Test
    void removeItem_unknownIdThrowsNotFound() {
        when(catalogRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.removeItem(99L));
    }
}
