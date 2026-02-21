package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.CreateProductVendorRequest;
import com.hisobnoma.platform.inventory.dto.ProductVendorDto;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVendor;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductVendorRepository;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductVendorService {

    private final ProductVendorRepository productVendorRepository;
    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<ProductVendorDto> getVendorsForProduct(Long productId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ProductVendor> list = productVendorRepository.findByTenantIdAndProductId(tenantId, productId);
        return list.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductVendorDto> getProductsForVendor(Long vendorId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ProductVendor> list = productVendorRepository.findByTenantIdAndVendorId(tenantId, vendorId);
        return list.stream().map(this::toDto).toList();
    }

    @Transactional
    public ProductVendorDto addVendorToProduct(Long productId, CreateProductVendorRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        Vendor vendor = vendorRepository.findByIdAndTenantId(request.getVendorId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Vendor not found: " + request.getVendorId()));

        if (productVendorRepository.existsByTenantIdAndProductIdAndVendorId(tenantId, productId, request.getVendorId())) {
            throw new BusinessException("This vendor is already linked to this product");
        }

        // If marking as preferred, clear existing preferred
        if (request.isPreferred()) {
            productVendorRepository.clearPreferredForProduct(tenantId, productId);
        }

        ProductVendor pv = ProductVendor.builder()
                .tenantId(tenantId)
                .product(product)
                .vendor(vendor)
                .vendorSku(request.getVendorSku())
                .vendorProductName(request.getVendorProductName())
                .unitCost(request.getUnitCost())
                .minOrderQuantity(request.getMinOrderQuantity())
                .leadTimeDays(request.getLeadTimeDays())
                .preferred(request.isPreferred())
                .active(request.isActive())
                .notes(request.getNotes())
                .build();

        pv = productVendorRepository.save(pv);
        log.info("Added vendor {} to product {}", vendor.getCode(), product.getSku());

        return toDto(pv);
    }

    @Transactional
    public ProductVendorDto updateProductVendor(Long productId, Long id, CreateProductVendorRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ProductVendor pv = productVendorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Product-vendor link not found: " + id));

        if (!pv.getProduct().getId().equals(productId)) {
            throw new BusinessException("Product-vendor link does not belong to this product");
        }

        // If marking as preferred, clear existing preferred
        if (request.isPreferred() && !pv.isPreferred()) {
            productVendorRepository.clearPreferredForProduct(tenantId, productId);
        }

        pv.setVendorSku(request.getVendorSku());
        pv.setVendorProductName(request.getVendorProductName());
        pv.setUnitCost(request.getUnitCost());
        pv.setMinOrderQuantity(request.getMinOrderQuantity());
        pv.setLeadTimeDays(request.getLeadTimeDays());
        pv.setPreferred(request.isPreferred());
        pv.setActive(request.isActive());
        pv.setNotes(request.getNotes());

        pv = productVendorRepository.save(pv);
        log.info("Updated product-vendor link {}", id);

        return toDto(pv);
    }

    @Transactional
    public void removeVendorFromProduct(Long productId, Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ProductVendor pv = productVendorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Product-vendor link not found: " + id));

        if (!pv.getProduct().getId().equals(productId)) {
            throw new BusinessException("Product-vendor link does not belong to this product");
        }

        productVendorRepository.delete(pv);
        log.info("Removed vendor from product, link id: {}", id);
    }

    private ProductVendorDto toDto(ProductVendor pv) {
        return ProductVendorDto.builder()
                .id(pv.getId())
                .productId(pv.getProduct().getId())
                .productName(pv.getProduct().getName())
                .productSku(pv.getProduct().getSku())
                .vendorId(pv.getVendor().getId())
                .vendorName(pv.getVendor().getName())
                .vendorCode(pv.getVendor().getCode())
                .vendorSku(pv.getVendorSku())
                .vendorProductName(pv.getVendorProductName())
                .unitCost(pv.getUnitCost())
                .minOrderQuantity(pv.getMinOrderQuantity())
                .leadTimeDays(pv.getLeadTimeDays())
                .preferred(pv.isPreferred())
                .active(pv.isActive())
                .notes(pv.getNotes())
                .build();
    }
}
