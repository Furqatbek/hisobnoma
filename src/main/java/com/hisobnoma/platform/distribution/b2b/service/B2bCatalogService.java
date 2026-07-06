package com.hisobnoma.platform.distribution.b2b.service;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.b2b.dto.B2bCatalogProductDto;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.pos.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Public B2B catalog: active sellable products priced against the buyer's price list.
 * The buyer is resolved and tenant-scoped by the controller before these are called.
 */
@Service
@RequiredArgsConstructor
public class B2bCatalogService {

    private final ProductRepository productRepository;
    private final PricingService pricingService;

    @Transactional(readOnly = true)
    public PageResponse<B2bCatalogProductDto> getCatalog(Customer buyer, String search, Long categoryId, Pageable pageable) {
        Long tenantId = buyer.getTenantId();
        String s = (search != null && !search.isBlank()) ? search.trim() : null;
        return PageResponse.of(productRepository.findSellablePageByTenantId(tenantId, s, categoryId, pageable)
                .map(p -> toDto(p, buyer, tenantId)));
    }

    @Transactional(readOnly = true)
    public B2bCatalogProductDto getProduct(Customer buyer, Long productId) {
        Long tenantId = buyer.getTenantId();
        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));
        if (!product.isActive() || !product.isSellable()) {
            throw new NotFoundException("Product", productId);
        }
        return toDto(product, buyer, tenantId);
    }

    private B2bCatalogProductDto toDto(Product product, Customer buyer, Long tenantId) {
        BigDecimal price = pricingService.getProductPrice(
                product.getId(), null, BigDecimal.ONE, buyer.getId(), null, tenantId);
        return B2bCatalogProductDto.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .unitName(product.getBaseUom() != null ? product.getBaseUom().getName() : null)
                .price(price)
                .currency(buyer.getDefaultCurrency() != null ? buyer.getDefaultCurrency() : "UZS")
                .build();
    }
}
