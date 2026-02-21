package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.CreateProductUomRequest;
import com.hisobnoma.platform.inventory.dto.ProductUomDto;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductUom;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.mapper.ProductUomMapper;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductUomRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductUomService {

    private final ProductUomRepository productUomRepository;
    private final ProductRepository productRepository;
    private final UnitOfMeasureRepository uomRepository;
    private final ProductUomMapper productUomMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<ProductUomDto> getByProduct(Long productId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ProductUom> uoms = productUomRepository.findByProductIdAndTenantIdOrderBySortOrderAsc(productId, tenantId);
        return productUomMapper.toDtoList(uoms);
    }

    @Transactional(readOnly = true)
    public List<ProductUomDto> getActiveByProduct(Long productId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ProductUom> uoms = productUomRepository.findByProductIdAndActiveTrueAndTenantIdOrderBySortOrderAsc(productId, tenantId);
        return productUomMapper.toDtoList(uoms);
    }

    @Transactional
    public ProductUomDto create(Long productId, CreateProductUomRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        UnitOfMeasure uom = uomRepository.findByIdAndTenantId(request.getUomId(), tenantId)
                .orElseThrow(() -> new NotFoundException("UOM not found: " + request.getUomId()));

        // Prevent adding the base UOM as an alternate (it's implicit)
        if (product.getBaseUom() != null && product.getBaseUom().getId().equals(uom.getId())) {
            throw new BusinessException("Cannot add the product's base UOM as an alternate UOM");
        }

        if (productUomRepository.existsByProductIdAndUomIdAndTenantId(productId, request.getUomId(), tenantId)) {
            throw new BusinessException("This UOM is already configured for this product");
        }

        // If this is set as default, unset others
        if (request.isDefaultSale()) {
            clearDefaultSaleUom(productId, tenantId);
        }

        ProductUom productUom = ProductUom.builder()
                .product(product)
                .uom(uom)
                .conversionFactor(request.getConversionFactor())
                .sellingPrice(request.getSellingPrice())
                .defaultSale(request.isDefaultSale())
                .active(request.isActive())
                .sortOrder(request.getSortOrder())
                .tenantId(tenantId)
                .build();

        productUom = productUomRepository.save(productUom);
        log.info("Created product UOM: product={}, uom={}, factor={}", productId, uom.getCode(), request.getConversionFactor());

        return productUomMapper.toDto(productUom);
    }

    @Transactional
    public ProductUomDto update(Long productId, Long id, CreateProductUomRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ProductUom productUom = productUomRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Product UOM not found: " + id));

        if (!productUom.getProduct().getId().equals(productId)) {
            throw new BusinessException("Product UOM does not belong to the specified product");
        }

        // If changing UOM, check no duplicate
        if (!productUom.getUom().getId().equals(request.getUomId())) {
            if (productUomRepository.existsByProductIdAndUomIdAndTenantId(productId, request.getUomId(), tenantId)) {
                throw new BusinessException("This UOM is already configured for this product");
            }
            UnitOfMeasure uom = uomRepository.findByIdAndTenantId(request.getUomId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("UOM not found: " + request.getUomId()));
            productUom.setUom(uom);
        }

        if (request.isDefaultSale() && !productUom.isDefaultSale()) {
            clearDefaultSaleUom(productId, tenantId);
        }

        productUom.setConversionFactor(request.getConversionFactor());
        productUom.setSellingPrice(request.getSellingPrice());
        productUom.setDefaultSale(request.isDefaultSale());
        productUom.setActive(request.isActive());
        productUom.setSortOrder(request.getSortOrder());

        productUom = productUomRepository.save(productUom);
        return productUomMapper.toDto(productUom);
    }

    @Transactional
    public void delete(Long productId, Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ProductUom productUom = productUomRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Product UOM not found: " + id));

        if (!productUom.getProduct().getId().equals(productId)) {
            throw new BusinessException("Product UOM does not belong to the specified product");
        }

        productUomRepository.delete(productUom);
        log.info("Deleted product UOM: id={}", id);
    }

    /**
     * Convert a sale quantity in an alternate UOM to the product's base UOM quantity.
     * If productUomId is null, returns the quantity as-is (already in base UOM).
     */
    @Transactional(readOnly = true)
    public BigDecimal convertToBaseQuantity(Long productUomId, BigDecimal saleQuantity) {
        if (productUomId == null) {
            return saleQuantity;
        }
        Long tenantId = securityContextHelper.getCurrentTenantId();
        ProductUom productUom = productUomRepository.findByIdAndTenantId(productUomId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product UOM not found: " + productUomId));
        return productUom.toBaseQuantity(saleQuantity);
    }

    private void clearDefaultSaleUom(Long productId, Long tenantId) {
        productUomRepository.findDefaultSaleUom(productId, tenantId)
                .ifPresent(existing -> {
                    existing.setDefaultSale(false);
                    productUomRepository.save(existing);
                });
    }
}
