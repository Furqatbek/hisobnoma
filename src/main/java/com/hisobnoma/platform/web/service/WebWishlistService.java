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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebWishlistService {

    private final WebWishlistItemRepository wishlistRepository;
    private final WebCatalogItemRepository catalogRepository;
    private final StockRepository stockRepository;
    private final WebPromotionBadgeService badgeService;

    @Transactional
    public boolean toggle(Long tenantId, Long webCustomerId, Long catalogItemId) {
        var existing = wishlistRepository.findByTenantIdAndWebCustomerIdAndCatalogItemId(
                tenantId, webCustomerId, catalogItemId);

        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false;
        }

        catalogRepository.findByIdAndTenantId(catalogItemId, tenantId)
                .orElseThrow(() -> new ValidationException("Catalog item not found"));

        boolean available = isAvailable(catalogItemId, tenantId);

        wishlistRepository.save(WebWishlistItem.builder()
                .tenantId(tenantId)
                .webCustomerId(webCustomerId)
                .catalogItemId(catalogItemId)
                .lastKnownAvailable(available)
                .build());
        return true;
    }

    @Transactional
    public void like(Long tenantId, Long webCustomerId, Long catalogItemId) {
        if (wishlistRepository.findByTenantIdAndWebCustomerIdAndCatalogItemId(
                tenantId, webCustomerId, catalogItemId).isPresent()) {
            return;
        }

        catalogRepository.findByIdAndTenantId(catalogItemId, tenantId)
                .orElseThrow(() -> new ValidationException("Catalog item not found"));

        boolean available = isAvailable(catalogItemId, tenantId);

        wishlistRepository.save(WebWishlistItem.builder()
                .tenantId(tenantId)
                .webCustomerId(webCustomerId)
                .catalogItemId(catalogItemId)
                .lastKnownAvailable(available)
                .build());
    }

    @Transactional
    public void unlike(Long tenantId, Long webCustomerId, Long catalogItemId) {
        wishlistRepository.deleteByTenantIdAndWebCustomerIdAndCatalogItemId(
                tenantId, webCustomerId, catalogItemId);
    }

    @Transactional(readOnly = true)
    public Page<WishlistItemDto> getWishlist(Long tenantId, Long webCustomerId, Pageable pageable) {
        Page<WebWishlistItem> items = wishlistRepository
                .findByTenantIdAndWebCustomerIdOrderByCreatedAtDesc(tenantId, webCustomerId, pageable);

        Map<Long, BigDecimal> stockMap = loadStockMap(tenantId);

        return items.map(wi -> {
            WebCatalogItem catalogItem = catalogRepository.findByIdAndTenantId(wi.getCatalogItemId(), tenantId)
                    .orElse(null);
            if (catalogItem == null) {
                return WishlistItemDto.builder()
                        .catalogItemId(wi.getCatalogItemId())
                        .name("(ўчирилган)")
                        .available(false)
                        .inStock(false)
                        .addedAt(wi.getCreatedAt())
                        .build();
            }
            return toWishlistDto(catalogItem, wi, stockMap);
        });
    }

    @Transactional(readOnly = true)
    public List<Long> getWishlistIds(Long tenantId, Long webCustomerId) {
        return wishlistRepository.findCatalogItemIdsByTenantIdAndWebCustomerId(tenantId, webCustomerId);
    }

    @Transactional(readOnly = true)
    public long likeCount(Long tenantId, Long catalogItemId) {
        return wishlistRepository.countByTenantIdAndCatalogItemId(tenantId, catalogItemId);
    }

    @Transactional(readOnly = true)
    public long wishlistCount(Long tenantId, Long webCustomerId) {
        return wishlistRepository.countByTenantIdAndWebCustomerId(tenantId, webCustomerId);
    }

    @Transactional(readOnly = true)
    public List<MostWishedItemDto> getMostWished(Long tenantId, int limit) {
        List<Object[]> rows = wishlistRepository.findMostWished(tenantId, PageRequest.of(0, limit));
        List<MostWishedItemDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long catalogItemId = (Long) row[0];
            long count = (Long) row[1];
            catalogRepository.findByIdAndTenantId(catalogItemId, tenantId).ifPresent(item -> {
                WebPromotionBadgeService.Badge badge = badgeService
                        .badgeFor(tenantId, item.getId(), item.getProduct().getId(), item.getEffectivePrice())
                        .orElse(null);
                result.add(MostWishedItemDto.builder()
                        .catalogItemId(catalogItemId)
                        .productName(item.getEffectiveName())
                        .likeCount(count)
                        .effectivePrice(item.getEffectivePrice())
                        .salePrice(badge != null ? badge.salePrice() : null)
                        .status(item.getStatus())
                        .build());
            });
        }
        return result;
    }

    boolean isAvailable(Long catalogItemId, Long tenantId) {
        return catalogRepository.findByIdAndTenantId(catalogItemId, tenantId)
                .filter(i -> i.getStatus() == WebCatalogStatus.LIVE)
                .filter(i -> i.getProduct().isActive() && i.getProduct().isSellable())
                .map(i -> !i.getProduct().isTrackInventory() || hasStock(i.getProduct().getId(), tenantId))
                .orElse(false);
    }

    private boolean hasStock(Long productId, Long tenantId) {
        BigDecimal qty = stockRepository.getTotalQuantityByProduct(productId, tenantId);
        return qty != null && qty.compareTo(BigDecimal.ZERO) > 0;
    }

    private Map<Long, BigDecimal> loadStockMap(Long tenantId) {
        Map<Long, BigDecimal> stockByProduct = new HashMap<>();
        try {
            for (Object[] row : stockRepository.getTotalQuantitiesByTenant(tenantId)) {
                stockByProduct.put((Long) row[0], (BigDecimal) row[1]);
            }
        } catch (Exception e) {
            log.warn("Failed to load stock map: {}", e.getMessage());
        }
        return stockByProduct;
    }

    private boolean isInStock(Product product, Map<Long, BigDecimal> stockMap) {
        if (!product.isTrackInventory()) return true;
        BigDecimal qty = stockMap.get(product.getId());
        return qty != null && qty.compareTo(BigDecimal.ZERO) > 0;
    }

    private WishlistItemDto toWishlistDto(WebCatalogItem item, WebWishlistItem wi,
                                          Map<Long, BigDecimal> stockMap) {
        Product p = item.getProduct();
        boolean inStock = isInStock(p, stockMap);
        boolean available = item.getStatus() == WebCatalogStatus.LIVE
                && p.isActive() && p.isSellable() && inStock;

        WebPromotionBadgeService.Badge badge = badgeService
                .badgeFor(item.getTenantId(), item.getId(), p.getId(), item.getEffectivePrice())
                .orElse(null);

        boolean priceDrop = badge != null && wi.getLastNotifiedSalePrice() != null
                && badge.salePrice().compareTo(wi.getLastNotifiedSalePrice()) < 0;

        String imageUrl = p.getPrimaryImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = p.getImages().stream()
                    .filter(ProductImage::isActive)
                    .map(ProductImage::getImageUrl)
                    .findFirst().orElse(null);
        }

        return WishlistItemDto.builder()
                .catalogItemId(item.getId())
                .name(item.getEffectiveName())
                .price(item.getEffectivePrice())
                .salePrice(badge != null ? badge.salePrice() : null)
                .promotionLabel(badge != null ? badge.label() : null)
                .inStock(inStock)
                .available(available)
                .imageUrl(imageUrl)
                .addedAt(wi.getCreatedAt())
                .priceDrop(priceDrop || (badge != null && wi.getLastNotifiedSalePrice() == null))
                .build();
    }
}
