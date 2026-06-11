package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductImage;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.web.dto.MostWishedItemDto;
import com.hisobnoma.platform.web.dto.WishlistItemDto;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.entity.WebWishlistItem;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebWishlistItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebWishlistServiceTest {

    static final Long TENANT = 1L;
    static final Long CUSTOMER = 10L;
    static final Long ITEM_ID = 100L;

    @Mock private WebWishlistItemRepository wishlistRepository;
    @Mock private WebCatalogItemRepository catalogRepository;
    @Mock private StockRepository stockRepository;
    @Mock private WebPromotionBadgeService badgeService;

    @InjectMocks
    private WebWishlistService service;

    @Test
    void toggle_likesNewItem() {
        when(wishlistRepository.findByTenantIdAndWebCustomerIdAndCatalogItemId(TENANT, CUSTOMER, ITEM_ID))
                .thenReturn(Optional.empty());
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT))
                .thenReturn(Optional.of(buildCatalogItem()));
        when(wishlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean liked = service.toggle(TENANT, CUSTOMER, ITEM_ID);

        assertTrue(liked);
        verify(wishlistRepository).save(argThat(w ->
                w.getTenantId().equals(TENANT) && w.getWebCustomerId().equals(CUSTOMER)
                        && w.getCatalogItemId().equals(ITEM_ID)));
    }

    @Test
    void toggle_unlikesExistingItem() {
        WebWishlistItem existing = WebWishlistItem.builder()
                .tenantId(TENANT).webCustomerId(CUSTOMER).catalogItemId(ITEM_ID).build();
        when(wishlistRepository.findByTenantIdAndWebCustomerIdAndCatalogItemId(TENANT, CUSTOMER, ITEM_ID))
                .thenReturn(Optional.of(existing));

        boolean liked = service.toggle(TENANT, CUSTOMER, ITEM_ID);

        assertFalse(liked);
        verify(wishlistRepository).delete(existing);
    }

    @Test
    void toggle_rejectsUnknownCatalogItem() {
        when(wishlistRepository.findByTenantIdAndWebCustomerIdAndCatalogItemId(TENANT, CUSTOMER, 999L))
                .thenReturn(Optional.empty());
        when(catalogRepository.findByIdAndTenantId(999L, TENANT))
                .thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.toggle(TENANT, CUSTOMER, 999L));
    }

    @Test
    void like_idempotent() {
        WebWishlistItem existing = WebWishlistItem.builder().build();
        when(wishlistRepository.findByTenantIdAndWebCustomerIdAndCatalogItemId(TENANT, CUSTOMER, ITEM_ID))
                .thenReturn(Optional.of(existing));

        service.like(TENANT, CUSTOMER, ITEM_ID);

        verify(wishlistRepository, never()).save(any());
    }

    @Test
    void unlike_deletesEntry() {
        service.unlike(TENANT, CUSTOMER, ITEM_ID);
        verify(wishlistRepository).deleteByTenantIdAndWebCustomerIdAndCatalogItemId(TENANT, CUSTOMER, ITEM_ID);
    }

    @Test
    void getWishlist_returnsItemsWithBadges() {
        WebWishlistItem wi = WebWishlistItem.builder()
                .tenantId(TENANT).webCustomerId(CUSTOMER).catalogItemId(ITEM_ID)
                .lastKnownAvailable(true).build();
        wi.setCreatedAt(Instant.now());

        WebCatalogItem catalogItem = buildCatalogItem();
        when(wishlistRepository.findByTenantIdAndWebCustomerIdOrderByCreatedAtDesc(eq(TENANT), eq(CUSTOMER), any()))
                .thenReturn(new PageImpl<>(List.of(wi)));
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT))
                .thenReturn(Optional.of(catalogItem));
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any()))
                .thenReturn(Optional.of(new WebPromotionBadgeService.Badge(BigDecimal.valueOf(8500), "-15%")));
        @SuppressWarnings("unchecked")
        List<Object[]> stockData = java.util.Collections.singletonList(
                new Object[]{catalogItem.getProduct().getId(), BigDecimal.TEN});
        when(stockRepository.getTotalQuantitiesByTenant(TENANT)).thenReturn(stockData);

        Page<WishlistItemDto> page = service.getWishlist(TENANT, CUSTOMER, PageRequest.of(0, 20));

        assertEquals(1, page.getContent().size());
        WishlistItemDto dto = page.getContent().get(0);
        assertEquals("Test Product", dto.getName());
        assertEquals(BigDecimal.valueOf(8500), dto.getSalePrice());
        assertEquals("-15%", dto.getPromotionLabel());
        assertTrue(dto.isInStock());
        assertTrue(dto.isAvailable());
    }

    @Test
    void getWishlist_handlesDeletedCatalogItem() {
        WebWishlistItem wi = WebWishlistItem.builder()
                .tenantId(TENANT).webCustomerId(CUSTOMER).catalogItemId(999L)
                .build();
        wi.setCreatedAt(Instant.now());

        when(wishlistRepository.findByTenantIdAndWebCustomerIdOrderByCreatedAtDesc(eq(TENANT), eq(CUSTOMER), any()))
                .thenReturn(new PageImpl<>(List.of(wi)));
        when(catalogRepository.findByIdAndTenantId(999L, TENANT))
                .thenReturn(Optional.empty());
        when(stockRepository.getTotalQuantitiesByTenant(TENANT)).thenReturn(List.of());

        Page<WishlistItemDto> page = service.getWishlist(TENANT, CUSTOMER, PageRequest.of(0, 20));

        assertEquals(1, page.getContent().size());
        assertFalse(page.getContent().get(0).isAvailable());
        assertEquals("(ўчирилган)", page.getContent().get(0).getName());
    }

    @Test
    void getWishlistIds_returnsIdList() {
        when(wishlistRepository.findCatalogItemIdsByTenantIdAndWebCustomerId(TENANT, CUSTOMER))
                .thenReturn(List.of(100L, 200L));

        List<Long> ids = service.getWishlistIds(TENANT, CUSTOMER);

        assertEquals(2, ids.size());
        assertTrue(ids.contains(100L));
        assertTrue(ids.contains(200L));
    }

    @Test
    void likeCount_returnsCount() {
        when(wishlistRepository.countByTenantIdAndCatalogItemId(TENANT, ITEM_ID)).thenReturn(5L);
        assertEquals(5L, service.likeCount(TENANT, ITEM_ID));
    }

    @Test
    void getMostWished_returnsRankedList() {
        @SuppressWarnings("unchecked")
        List<Object[]> mostWished = java.util.Collections.singletonList(new Object[]{ITEM_ID, 7L});
        when(wishlistRepository.findMostWished(eq(TENANT), any())).thenReturn(mostWished);
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT))
                .thenReturn(Optional.of(buildCatalogItem()));
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any()))
                .thenReturn(Optional.empty());

        List<MostWishedItemDto> result = service.getMostWished(TENANT, 10);

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getLikeCount());
        assertEquals("Test Product", result.get(0).getProductName());
    }

    @Test
    void wishlistCount_returnsCount() {
        when(wishlistRepository.countByTenantIdAndWebCustomerId(TENANT, CUSTOMER)).thenReturn(3L);
        assertEquals(3L, service.wishlistCount(TENANT, CUSTOMER));
    }

    private WebCatalogItem buildCatalogItem() {
        Product product = Product.builder()
                .name("Test Product")
                .sellingPrice(BigDecimal.valueOf(10000))
                .active(true)
                .sellable(true)
                .trackInventory(false)
                .build();
        product.setId(50L);
        product.setImages(List.of());

        WebCatalogItem item = WebCatalogItem.builder()
                .product(product)
                .tenantId(TENANT)
                .status(WebCatalogStatus.LIVE)
                .sortOrder(0)
                .build();
        item.setId(ITEM_ID);
        return item;
    }
}
