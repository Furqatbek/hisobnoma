package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductImage;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.web.dto.AddCatalogItemsRequest;
import com.hisobnoma.platform.web.dto.MostWishedItemDto;
import com.hisobnoma.platform.web.dto.UpdateCatalogItemRequest;
import com.hisobnoma.platform.web.dto.WebCatalogCountsDto;
import com.hisobnoma.platform.web.dto.WebCatalogItemDto;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebWishlistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Staff-facing management of the online shop catalog (draft/live item list).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebCatalogService {

    private final WebCatalogItemRepository catalogRepository;
    private final ProductRepository productRepository;
    private final SecurityContextHelper securityContextHelper;
    private final WebPromotionBadgeService badgeService;
    private final WebWishlistItemRepository wishlistRepository;

    @Transactional(readOnly = true)
    public Page<WebCatalogItemDto> getItems(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<WebCatalogItem> page = (search == null || search.isBlank())
                ? catalogRepository.findAllByTenant(tenantId, pageable)
                : catalogRepository.searchByTenant(tenantId, likePattern(search), pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public WebCatalogCountsDto getCounts() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        long live = catalogRepository.countByTenantIdAndStatus(tenantId, WebCatalogStatus.LIVE);
        long draft = catalogRepository.countByTenantIdAndStatus(tenantId, WebCatalogStatus.DRAFT);
        return WebCatalogCountsDto.builder().live(live).draft(draft).total(live + draft).build();
    }

    @Transactional
    public List<WebCatalogItemDto> addProducts(AddCatalogItemsRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        int nextSort = catalogRepository.findMaxSortOrder(tenantId) + 1;
        List<WebCatalogItemDto> added = new ArrayList<>();

        for (Long productId : request.getProductIds()) {
            if (catalogRepository.existsByTenantIdAndProductId(tenantId, productId)) {
                continue;
            }
            Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                    .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
            if (!product.isActive() || !product.isSellable()) {
                throw new ValidationException("Product is not active or not sellable: " + product.getName());
            }

            WebCatalogItem item = WebCatalogItem.builder()
                    .product(product)
                    .sortOrder(nextSort++)
                    .status(WebCatalogStatus.DRAFT)
                    .tenantId(tenantId)
                    .build();
            added.add(toDto(catalogRepository.save(item)));
        }

        log.info("Added {} products to web catalog for tenant {}", added.size(), tenantId);
        return added;
    }

    @Transactional
    public WebCatalogItemDto updateItem(Long id, UpdateCatalogItemRequest request) {
        WebCatalogItem item = getItem(id);
        item.setDisplayName(request.getDisplayName());
        item.setPriceOverride(request.getPriceOverride());
        return toDto(catalogRepository.save(item));
    }

    @Transactional
    public WebCatalogItemDto publish(Long id) {
        WebCatalogItem item = getItem(id);
        Product product = item.getProduct();
        if (!product.isActive() || !product.isSellable()) {
            throw new ValidationException("Cannot publish: product is not active or not sellable: " + product.getName());
        }
        item.setStatus(WebCatalogStatus.LIVE);
        log.info("Published web catalog item {} ({})", id, product.getName());
        return toDto(catalogRepository.save(item));
    }

    @Transactional
    public WebCatalogItemDto unpublish(Long id) {
        WebCatalogItem item = getItem(id);
        item.setStatus(WebCatalogStatus.DRAFT);
        return toDto(catalogRepository.save(item));
    }

    /**
     * Publishes every draft item whose product is still active and sellable.
     * Returns the number of items published.
     */
    @Transactional
    public int publishAll() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        int published = 0;
        for (WebCatalogItem item : catalogRepository.findByTenantIdOrderBySortOrderAsc(tenantId)) {
            if (item.getStatus() == WebCatalogStatus.DRAFT
                    && item.getProduct().isActive() && item.getProduct().isSellable()) {
                item.setStatus(WebCatalogStatus.LIVE);
                catalogRepository.save(item);
                published++;
            }
        }
        log.info("Published {} web catalog items for tenant {}", published, tenantId);
        return published;
    }

    @Transactional
    public int unpublishAll() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        int unpublished = 0;
        for (WebCatalogItem item : catalogRepository.findByTenantIdOrderBySortOrderAsc(tenantId)) {
            if (item.getStatus() == WebCatalogStatus.LIVE) {
                item.setStatus(WebCatalogStatus.DRAFT);
                catalogRepository.save(item);
                unpublished++;
            }
        }
        return unpublished;
    }

    @Transactional
    public void moveUp(Long id) {
        WebCatalogItem item = getItem(id);
        catalogRepository.findFirstByTenantIdAndSortOrderLessThanOrderBySortOrderDesc(
                        item.getTenantId(), item.getSortOrder())
                .ifPresent(neighbor -> swapSortOrders(item, neighbor));
    }

    @Transactional
    public void moveDown(Long id) {
        WebCatalogItem item = getItem(id);
        catalogRepository.findFirstByTenantIdAndSortOrderGreaterThanOrderBySortOrderAsc(
                        item.getTenantId(), item.getSortOrder())
                .ifPresent(neighbor -> swapSortOrders(item, neighbor));
    }

    @Transactional
    public void removeItem(Long id) {
        WebCatalogItem item = getItem(id);
        catalogRepository.delete(item);
        log.info("Removed web catalog item {}", id);
    }

    @Transactional(readOnly = true)
    public List<MostWishedItemDto> getMostWished(int limit) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Object[]> rows = wishlistRepository.findMostWished(tenantId,
                org.springframework.data.domain.PageRequest.of(0, limit));
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

    private void swapSortOrders(WebCatalogItem a, WebCatalogItem b) {
        Integer tmp = a.getSortOrder();
        a.setSortOrder(b.getSortOrder());
        b.setSortOrder(tmp);
        catalogRepository.save(a);
        catalogRepository.save(b);
    }

    private WebCatalogItem getItem(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return catalogRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Web catalog item not found: " + id));
    }

    private String likePattern(String search) {
        return "%" + search.toLowerCase().trim() + "%";
    }

    private WebCatalogItemDto toDto(WebCatalogItem item) {
        Product p = item.getProduct();
        // Staff sees the same sale price the customer would see in the app
        WebPromotionBadgeService.Badge badge = badgeService
                .badgeFor(item.getTenantId(), item.getId(), p.getId(), item.getEffectivePrice())
                .orElse(null);
        return WebCatalogItemDto.builder()
                .id(item.getId())
                .productId(p.getId())
                .sku(p.getSku())
                .productName(p.getName())
                .displayName(item.getDisplayName())
                .basePrice(p.getSellingPrice())
                .priceOverride(item.getPriceOverride())
                .effectivePrice(item.getEffectivePrice())
                .salePrice(badge != null ? badge.salePrice() : null)
                .promotionLabel(badge != null ? badge.label() : null)
                .sortOrder(item.getSortOrder())
                .status(item.getStatus())
                .imageUrl(resolvePrimaryImage(p))
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .productActive(p.isActive())
                .productSellable(p.isSellable())
                .likeCount(wishlistRepository.countByTenantIdAndCatalogItemId(item.getTenantId(), item.getId()))
                .build();
    }

    private String resolvePrimaryImage(Product p) {
        if (p.getPrimaryImageUrl() != null && !p.getPrimaryImageUrl().isBlank()) {
            return p.getPrimaryImageUrl();
        }
        return p.getImages().stream()
                .filter(ProductImage::isActive)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }
}
