package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductImage;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.web.dto.PublicCatalogProductDto;
import com.hisobnoma.platform.web.dto.PublicCategoryDto;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anonymous (customer-facing) read access to the live web catalog.
 * Tenant is resolved from the X-Tenant-ID header via TenantContext,
 * defaulting to tenant 1 (same pattern as other public endpoints).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebCatalogPublicService {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final String CURRENCY = "UZS";

    private final WebCatalogItemRepository catalogRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public Page<PublicCatalogProductDto> getProducts(String search, Long categoryId, Pageable pageable) {
        Long tenantId = resolveTenantId();
        Page<WebCatalogItem> page;
        if (search != null && !search.isBlank()) {
            page = catalogRepository.searchVisible(tenantId, WebCatalogStatus.LIVE,
                    "%" + search.toLowerCase().trim() + "%", pageable);
        } else if (categoryId != null) {
            page = catalogRepository.findVisibleByCategory(tenantId, WebCatalogStatus.LIVE, categoryId, pageable);
        } else {
            page = catalogRepository.findVisible(tenantId, WebCatalogStatus.LIVE, pageable);
        }

        Map<Long, BigDecimal> stockByProduct = loadStockMap(tenantId);
        return page.map(item -> toPublicDto(item, isInStock(item.getProduct(), stockByProduct)));
    }

    @Transactional(readOnly = true)
    public PublicCatalogProductDto getProduct(Long id) {
        Long tenantId = resolveTenantId();
        WebCatalogItem item = catalogRepository.findByIdAndTenantId(id, tenantId)
                .filter(i -> i.getStatus() == WebCatalogStatus.LIVE)
                .filter(i -> i.getProduct().isActive() && i.getProduct().isSellable())
                .orElseThrow(() -> new NotFoundException("Catalog item not found: " + id));

        Product product = item.getProduct();
        boolean inStock = !product.isTrackInventory() || hasStock(product.getId(), tenantId);
        return toPublicDto(item, inStock);
    }

    @Transactional(readOnly = true)
    public List<PublicCategoryDto> getCategories() {
        Long tenantId = resolveTenantId();
        return catalogRepository.findVisibleCategories(tenantId, WebCatalogStatus.LIVE).stream()
                .map(row -> PublicCategoryDto.builder()
                        .id((Long) row[0])
                        .name((String) row[1])
                        .build())
                .toList();
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
    }

    private Map<Long, BigDecimal> loadStockMap(Long tenantId) {
        Map<Long, BigDecimal> stockByProduct = new HashMap<>();
        try {
            for (Object[] row : stockRepository.getTotalQuantitiesByTenant(tenantId)) {
                stockByProduct.put((Long) row[0], (BigDecimal) row[1]);
            }
        } catch (Exception e) {
            log.warn("Failed to load stock quantities for tenant {}: {}", tenantId, e.getMessage());
        }
        return stockByProduct;
    }

    private boolean isInStock(Product product, Map<Long, BigDecimal> stockByProduct) {
        if (!product.isTrackInventory()) {
            return true;
        }
        BigDecimal qty = stockByProduct.get(product.getId());
        return qty != null && qty.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasStock(Long productId, Long tenantId) {
        BigDecimal qty = stockRepository.getTotalQuantityByProduct(productId, tenantId);
        return qty != null && qty.compareTo(BigDecimal.ZERO) > 0;
    }

    private PublicCatalogProductDto toPublicDto(WebCatalogItem item, boolean inStock) {
        Product p = item.getProduct();
        List<String> images = p.getImages().stream()
                .filter(ProductImage::isActive)
                .map(ProductImage::getImageUrl)
                .toList();
        String primary = p.getPrimaryImageUrl() != null && !p.getPrimaryImageUrl().isBlank()
                ? p.getPrimaryImageUrl()
                : (images.isEmpty() ? null : images.get(0));

        return PublicCatalogProductDto.builder()
                .id(item.getId())
                .name(item.getEffectiveName())
                .shortDescription(p.getShortDescription())
                .description(p.getDescription())
                .price(item.getEffectivePrice())
                .currency(CURRENCY)
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                .unitName(p.getBaseUom() != null ? p.getBaseUom().getName() : null)
                .inStock(inStock)
                .imageUrl(primary)
                .images(images)
                .build();
    }
}
