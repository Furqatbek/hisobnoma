package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.mapper.ProductMapper;
import com.hisobnoma.platform.inventory.mapper.ProductVariantMapper;
import com.hisobnoma.platform.inventory.mapper.ProductAttributeMapper;
import com.hisobnoma.platform.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductAttributeRepository attributeRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UnitOfMeasureRepository uomRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper variantMapper;
    private final ProductAttributeMapper attributeMapper;
    private final SkuGeneratorService skuGeneratorService;
    private final BarcodeService barcodeService;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> getProducts(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Product> page = productRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(productMapper::toDtoSimple));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> getActiveProducts(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Product> page = productRepository.findAllActiveByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(productMapper::toDtoSimple));
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(Long id) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Product", id));

        ProductDto dto = productMapper.toDto(product);

        // Load variants, images, and attributes
        dto.setVariants(variantMapper.toDtoList(variantRepository.findByProductIdOrderBySortOrder(id)));
        dto.setImages(imageRepository.findByProductIdOrderBySortOrder(id).stream()
                .map(img -> ProductImageDto.builder()
                        .id(img.getId())
                        .productId(product.getId())
                        .imageUrl(img.getImageUrl())
                        .thumbnailUrl(img.getThumbnailUrl())
                        .altText(img.getAltText())
                        .title(img.getTitle())
                        .sortOrder(img.getSortOrder())
                        .primary(img.isPrimary())
                        .active(img.isActive())
                        .build())
                .toList());
        dto.setAttributes(attributeMapper.toDtoList(attributeRepository.findByProductIdOrderBySortOrder(id)));

        return dto;
    }

    @Transactional(readOnly = true)
    public ProductDto getProductBySku(String sku) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Product product = productRepository.findBySkuAndTenantId(sku, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", "SKU", sku));
        return getProduct(product.getId());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductByBarcode(String barcode) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Product product = productRepository.findByBarcodeAndTenantId(barcode, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", "barcode", barcode));
        return getProduct(product.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> searchProducts(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Product> page = productRepository.searchByTenantId(tenantId, search, pageable);
        return PageResponse.of(page.map(productMapper::toDtoSimple));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> getProductsByCategory(Long categoryId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Product> page = productRepository.findByCategoryIdAndTenantId(categoryId, tenantId, pageable);
        return PageResponse.of(page.map(productMapper::toDtoSimple));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> getProductsByBrand(Long brandId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Product> page = productRepository.findByBrandIdAndTenantId(brandId, tenantId, pageable);
        return PageResponse.of(page.map(productMapper::toDtoSimple));
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Generate SKU if not provided
        String sku = request.getSku();
        if (sku == null || sku.isEmpty()) {
            sku = skuGeneratorService.generateSkuFromName(request.getName());
        } else {
            // Validate SKU doesn't exist
            if (productRepository.existsBySkuAndTenantId(sku, tenantId) ||
                (tenantId == null && productRepository.existsBySkuAndTenantIdIsNull(sku))) {
                throw new DuplicateResourceException("Product", "SKU", sku);
            }
        }

        // Validate barcode if provided
        if (request.getBarcode() != null && !request.getBarcode().isEmpty()) {
            if (productRepository.existsByBarcodeAndTenantId(request.getBarcode(), tenantId) ||
                (tenantId == null && productRepository.existsByBarcodeAndTenantIdIsNull(request.getBarcode()))) {
                throw new DuplicateResourceException("Product", "barcode", request.getBarcode());
            }
        }

        Product product = productMapper.toEntity(request);
        product.setSku(sku);
        product.setTenantId(tenantId);

        // Set category
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category", request.getCategoryId()));
            product.setCategory(category);
        }

        // Set brand
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new NotFoundException("Brand", request.getBrandId()));
            product.setBrand(brand);
        }

        // Set base UOM (required)
        UnitOfMeasure baseUom = uomRepository.findById(request.getBaseUomId())
                .orElseThrow(() -> new NotFoundException("Unit of Measure", request.getBaseUomId()));
        product.setBaseUom(baseUom);

        // Save product first
        product = productRepository.save(product);

        // Create variants if provided
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            product.setHasVariants(true);
            for (CreateProductRequest.CreateVariantRequest variantRequest : request.getVariants()) {
                ProductVariant variant = variantMapper.toEntity(variantRequest);
                variant.setProduct(product);
                variant.setTenantId(tenantId);

                // Generate variant SKU if not provided
                if (variant.getSku() == null || variant.getSku().isEmpty()) {
                    variant.setSku(skuGeneratorService.generateVariantSku(
                            product.getSku(),
                            variantRequest.getOption1Value(),
                            variantRequest.getOption2Value(),
                            variantRequest.getOption3Value()
                    ));
                }

                variantRepository.save(variant);
            }
            product = productRepository.save(product);
        }

        // Create attributes if provided
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            for (CreateProductRequest.CreateAttributeRequest attrRequest : request.getAttributes()) {
                ProductAttribute attribute = attributeMapper.toEntity(attrRequest);
                attribute.setProduct(product);
                attribute.setTenantId(tenantId);
                attributeRepository.save(attribute);
            }
        }

        log.info("Created product: {} ({})", product.getName(), product.getSku());
        return getProduct(product.getId());
    }

    @Transactional
    public ProductDto updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product", id));

        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Validate barcode uniqueness if changing
        if (request.getBarcode() != null && !request.getBarcode().equals(product.getBarcode())) {
            if (productRepository.existsByBarcodeAndTenantId(request.getBarcode(), tenantId)) {
                throw new DuplicateResourceException("Product", "barcode", request.getBarcode());
            }
            product.setBarcode(request.getBarcode());
        }

        if (request.getName() != null) {
            product.setName(request.getName());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getShortDescription() != null) {
            product.setShortDescription(request.getShortDescription());
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category", request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new NotFoundException("Brand", request.getBrandId()));
            product.setBrand(brand);
        }

        if (request.getBaseUomId() != null) {
            UnitOfMeasure baseUom = uomRepository.findById(request.getBaseUomId())
                    .orElseThrow(() -> new NotFoundException("Unit of Measure", request.getBaseUomId()));
            product.setBaseUom(baseUom);
        }

        if (request.getCostPrice() != null) product.setCostPrice(request.getCostPrice());
        if (request.getSellingPrice() != null) product.setSellingPrice(request.getSellingPrice());
        if (request.getMinSellingPrice() != null) product.setMinSellingPrice(request.getMinSellingPrice());
        if (request.getWholesalePrice() != null) product.setWholesalePrice(request.getWholesalePrice());

        if (request.getTrackInventory() != null) product.setTrackInventory(request.getTrackInventory());
        if (request.getAllowNegativeStock() != null) product.setAllowNegativeStock(request.getAllowNegativeStock());
        if (request.getMinStockLevel() != null) product.setMinStockLevel(request.getMinStockLevel());
        if (request.getReorderPoint() != null) product.setReorderPoint(request.getReorderPoint());
        if (request.getReorderQuantity() != null) product.setReorderQuantity(request.getReorderQuantity());
        if (request.getMaxStockLevel() != null) product.setMaxStockLevel(request.getMaxStockLevel());

        if (request.getActive() != null) product.setActive(request.getActive());
        if (request.getService() != null) product.setService(request.getService());
        if (request.getSellable() != null) product.setSellable(request.getSellable());
        if (request.getPurchasable() != null) product.setPurchasable(request.getPurchasable());
        if (request.getTrackBatch() != null) product.setTrackBatch(request.getTrackBatch());
        if (request.getTrackSerial() != null) product.setTrackSerial(request.getTrackSerial());

        if (request.getWeight() != null) product.setWeight(request.getWeight());
        if (request.getWeightUnit() != null) product.setWeightUnit(request.getWeightUnit());
        if (request.getLength() != null) product.setLength(request.getLength());
        if (request.getWidth() != null) product.setWidth(request.getWidth());
        if (request.getHeight() != null) product.setHeight(request.getHeight());
        if (request.getDimensionUnit() != null) product.setDimensionUnit(request.getDimensionUnit());

        if (request.getPrimaryImageUrl() != null) product.setPrimaryImageUrl(request.getPrimaryImageUrl());
        if (request.getManufacturer() != null) product.setManufacturer(request.getManufacturer());
        if (request.getManufacturerPartNumber() != null) product.setManufacturerPartNumber(request.getManufacturerPartNumber());
        if (request.getTaxCode() != null) product.setTaxCode(request.getTaxCode());
        if (request.getNotes() != null) product.setNotes(request.getNotes());
        if (request.getTags() != null) product.setTags(request.getTags());

        product = productRepository.save(product);
        log.info("Updated product: {}", product.getId());
        return getProduct(product.getId());
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product", id));

        productRepository.delete(product);
        log.info("Deleted product: {} ({})", product.getName(), product.getSku());
    }

    @Transactional(readOnly = true)
    public long getProductCount() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return productRepository.countByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public long getActiveProductCount() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return productRepository.countActiveByTenantId(tenantId);
    }
}
