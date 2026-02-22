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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final StockRepository stockRepository;
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
        return enrichWithStockQuantities(PageResponse.of(page.map(productMapper::toDtoSimple)), tenantId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> getActiveProducts(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Product> page = productRepository.findAllActiveByTenantId(tenantId, pageable);
        return enrichWithStockQuantities(PageResponse.of(page.map(productMapper::toDtoSimple)), tenantId);
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

        // Enrich with stock quantity
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BigDecimal totalQty = stockRepository.getTotalQuantityByProduct(id, tenantId);
        dto.setStockQuantity(totalQty != null ? totalQty : BigDecimal.ZERO);

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
        return enrichWithStockQuantities(PageResponse.of(page.map(productMapper::toDtoSimple)), tenantId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> getProductsByCategory(Long categoryId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Product> page = productRepository.findByCategoryIdAndTenantId(categoryId, tenantId, pageable);
        return enrichWithStockQuantities(PageResponse.of(page.map(productMapper::toDtoSimple)), tenantId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> getProductsByBrand(Long brandId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Product> page = productRepository.findByBrandIdAndTenantId(brandId, tenantId, pageable);
        return enrichWithStockQuantities(PageResponse.of(page.map(productMapper::toDtoSimple)), tenantId);
    }

    private PageResponse<ProductDto> enrichWithStockQuantities(PageResponse<ProductDto> response, Long tenantId) {
        if (response.getContent() == null || response.getContent().isEmpty()) {
            return response;
        }
        Map<Long, BigDecimal> stockMap = stockRepository.getTotalQuantitiesByTenant(tenantId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO
                ));
        response.getContent().forEach(dto ->
                dto.setStockQuantity(stockMap.getOrDefault(dto.getId(), BigDecimal.ZERO))
        );
        return response;
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

    // ==================== Variant Management ====================

    @Transactional(readOnly = true)
    public List<ProductVariantDto> getVariants(Long productId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        return variantMapper.toDtoList(variantRepository.findByProductIdOrderBySortOrder(productId));
    }

    @Transactional
    public ProductVariantDto addVariant(Long productId, CreateVariantRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        // Generate SKU if not provided
        String sku = request.getSku();
        if (sku == null || sku.isEmpty()) {
            sku = skuGeneratorService.generateVariantSku(
                    product.getSku(),
                    request.getOption1Value(),
                    request.getOption2Value(),
                    request.getOption3Value()
            );
        } else {
            // Validate SKU uniqueness
            if (variantRepository.existsBySkuAndTenantId(sku, tenantId)) {
                throw new DuplicateResourceException("ProductVariant", "SKU", sku);
            }
        }

        // Validate barcode uniqueness if provided
        if (request.getBarcode() != null && !request.getBarcode().isEmpty()) {
            if (variantRepository.existsByBarcodeAndTenantId(request.getBarcode(), tenantId)) {
                throw new DuplicateResourceException("ProductVariant", "barcode", request.getBarcode());
            }
        }

        // Build variant name if not provided
        String name = request.getName();
        if (name == null || name.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (request.getOption1Value() != null) sb.append(request.getOption1Value());
            if (request.getOption2Value() != null) sb.append(" / ").append(request.getOption2Value());
            if (request.getOption3Value() != null) sb.append(" / ").append(request.getOption3Value());
            name = sb.toString();
        }

        // Get next sort order
        long variantCount = variantRepository.countByProductId(productId);

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .tenantId(tenantId)
                .sku(sku)
                .barcode(request.getBarcode())
                .name(name)
                .option1Name(request.getOption1Name())
                .option1Value(request.getOption1Value())
                .option2Name(request.getOption2Name())
                .option2Value(request.getOption2Value())
                .option3Name(request.getOption3Name())
                .option3Value(request.getOption3Value())
                .costPrice(request.getCostPrice())
                .sellingPrice(request.getSellingPrice())
                .priceDifference(request.getPriceDifference())
                .weight(request.getWeight())
                .imageUrl(request.getImageUrl())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : (int) variantCount)
                .active(request.isActive())
                .build();

        variant = variantRepository.save(variant);

        // Update product to have variants
        if (!product.isHasVariants()) {
            product.setHasVariants(true);
            productRepository.save(product);
        }

        log.info("Added variant {} to product {}", variant.getSku(), productId);
        return variantMapper.toDto(variant);
    }

    @Transactional
    public ProductVariantDto updateVariant(Long productId, Long variantId, UpdateVariantRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("ProductVariant", variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new NotFoundException("ProductVariant", variantId);
        }

        // Validate barcode uniqueness if changing
        if (request.getBarcode() != null && !request.getBarcode().equals(variant.getBarcode())) {
            if (variantRepository.existsByBarcodeAndTenantId(request.getBarcode(), tenantId)) {
                throw new DuplicateResourceException("ProductVariant", "barcode", request.getBarcode());
            }
            variant.setBarcode(request.getBarcode());
        }

        if (request.getName() != null) variant.setName(request.getName());
        if (request.getOption1Name() != null) variant.setOption1Name(request.getOption1Name());
        if (request.getOption1Value() != null) variant.setOption1Value(request.getOption1Value());
        if (request.getOption2Name() != null) variant.setOption2Name(request.getOption2Name());
        if (request.getOption2Value() != null) variant.setOption2Value(request.getOption2Value());
        if (request.getOption3Name() != null) variant.setOption3Name(request.getOption3Name());
        if (request.getOption3Value() != null) variant.setOption3Value(request.getOption3Value());
        if (request.getCostPrice() != null) variant.setCostPrice(request.getCostPrice());
        if (request.getSellingPrice() != null) variant.setSellingPrice(request.getSellingPrice());
        if (request.getPriceDifference() != null) variant.setPriceDifference(request.getPriceDifference());
        if (request.getWeight() != null) variant.setWeight(request.getWeight());
        if (request.getImageUrl() != null) variant.setImageUrl(request.getImageUrl());
        if (request.getSortOrder() != null) variant.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) variant.setActive(request.getActive());

        variant = variantRepository.save(variant);
        log.info("Updated variant {} for product {}", variantId, productId);
        return variantMapper.toDto(variant);
    }

    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("ProductVariant", variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new NotFoundException("ProductVariant", variantId);
        }

        variantRepository.delete(variant);

        // Check if product still has variants
        long remainingVariants = variantRepository.countByProductId(productId);
        if (remainingVariants == 0) {
            product.setHasVariants(false);
            productRepository.save(product);
        }

        log.info("Deleted variant {} from product {}", variantId, productId);
    }

    // ==================== Export Methods ====================

    private static final String[] EXPORT_HEADERS = {
            "SKU", "Barcode", "Name", "Description", "Short Description",
            "Category", "Brand", "UOM Code", "Cost Price", "Selling Price",
            "Min Selling Price", "Wholesale Price", "Track Inventory", "Allow Negative Stock",
            "Min Stock Level", "Reorder Point", "Reorder Quantity", "Max Stock Level",
            "Active", "Service", "Sellable", "Purchasable", "Track Batch", "Track Serial",
            "Weight", "Weight Unit", "Manufacturer", "Manufacturer Part Number", "Tax Code", "Tags"
    };

    @Transactional(readOnly = true)
    public byte[] exportToCsv(Long categoryId, Long brandId, Boolean active) {
        List<Product> products = getProductsForExport(categoryId, brandId, active);

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", EXPORT_HEADERS)).append("\n");

        for (Product p : products) {
            sb.append(escapeCsv(p.getSku())).append(",");
            sb.append(escapeCsv(p.getBarcode())).append(",");
            sb.append(escapeCsv(p.getName())).append(",");
            sb.append(escapeCsv(p.getDescription())).append(",");
            sb.append(escapeCsv(p.getShortDescription())).append(",");
            sb.append(escapeCsv(p.getCategory() != null ? p.getCategory().getName() : "")).append(",");
            sb.append(escapeCsv(p.getBrand() != null ? p.getBrand().getName() : "")).append(",");
            sb.append(escapeCsv(p.getBaseUom() != null ? p.getBaseUom().getCode() : "")).append(",");
            sb.append(p.getCostPrice() != null ? p.getCostPrice().toString() : "").append(",");
            sb.append(p.getSellingPrice() != null ? p.getSellingPrice().toString() : "").append(",");
            sb.append(p.getMinSellingPrice() != null ? p.getMinSellingPrice().toString() : "").append(",");
            sb.append(p.getWholesalePrice() != null ? p.getWholesalePrice().toString() : "").append(",");
            sb.append(p.isTrackInventory()).append(",");
            sb.append(p.isAllowNegativeStock()).append(",");
            sb.append(p.getMinStockLevel() != null ? p.getMinStockLevel().toString() : "").append(",");
            sb.append(p.getReorderPoint() != null ? p.getReorderPoint().toString() : "").append(",");
            sb.append(p.getReorderQuantity() != null ? p.getReorderQuantity().toString() : "").append(",");
            sb.append(p.getMaxStockLevel() != null ? p.getMaxStockLevel().toString() : "").append(",");
            sb.append(p.isActive()).append(",");
            sb.append(p.isService()).append(",");
            sb.append(p.isSellable()).append(",");
            sb.append(p.isPurchasable()).append(",");
            sb.append(p.isTrackBatch()).append(",");
            sb.append(p.isTrackSerial()).append(",");
            sb.append(p.getWeight() != null ? p.getWeight().toString() : "").append(",");
            sb.append(escapeCsv(p.getWeightUnit())).append(",");
            sb.append(escapeCsv(p.getManufacturer())).append(",");
            sb.append(escapeCsv(p.getManufacturerPartNumber())).append(",");
            sb.append(escapeCsv(p.getTaxCode())).append(",");
            sb.append(escapeCsv(p.getTags())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportToExcel(Long categoryId, Long brandId, Boolean active) throws IOException {
        List<Product> products = getProductsForExport(categoryId, brandId, active);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;
                row.createCell(col++).setCellValue(p.getSku() != null ? p.getSku() : "");
                row.createCell(col++).setCellValue(p.getBarcode() != null ? p.getBarcode() : "");
                row.createCell(col++).setCellValue(p.getName() != null ? p.getName() : "");
                row.createCell(col++).setCellValue(p.getDescription() != null ? p.getDescription() : "");
                row.createCell(col++).setCellValue(p.getShortDescription() != null ? p.getShortDescription() : "");
                row.createCell(col++).setCellValue(p.getCategory() != null ? p.getCategory().getName() : "");
                row.createCell(col++).setCellValue(p.getBrand() != null ? p.getBrand().getName() : "");
                row.createCell(col++).setCellValue(p.getBaseUom() != null ? p.getBaseUom().getCode() : "");
                row.createCell(col++).setCellValue(p.getCostPrice() != null ? p.getCostPrice().doubleValue() : 0);
                row.createCell(col++).setCellValue(p.getSellingPrice() != null ? p.getSellingPrice().doubleValue() : 0);
                row.createCell(col++).setCellValue(p.getMinSellingPrice() != null ? p.getMinSellingPrice().doubleValue() : 0);
                row.createCell(col++).setCellValue(p.getWholesalePrice() != null ? p.getWholesalePrice().doubleValue() : 0);
                row.createCell(col++).setCellValue(p.isTrackInventory());
                row.createCell(col++).setCellValue(p.isAllowNegativeStock());
                row.createCell(col++).setCellValue(p.getMinStockLevel() != null ? p.getMinStockLevel().doubleValue() : 0);
                row.createCell(col++).setCellValue(p.getReorderPoint() != null ? p.getReorderPoint().doubleValue() : 0);
                row.createCell(col++).setCellValue(p.getReorderQuantity() != null ? p.getReorderQuantity().doubleValue() : 0);
                row.createCell(col++).setCellValue(p.getMaxStockLevel() != null ? p.getMaxStockLevel().doubleValue() : 0);
                row.createCell(col++).setCellValue(p.isActive());
                row.createCell(col++).setCellValue(p.isService());
                row.createCell(col++).setCellValue(p.isSellable());
                row.createCell(col++).setCellValue(p.isPurchasable());
                row.createCell(col++).setCellValue(p.isTrackBatch());
                row.createCell(col++).setCellValue(p.isTrackSerial());
                row.createCell(col++).setCellValue(p.getWeight() != null ? p.getWeight().doubleValue() : 0);
                row.createCell(col++).setCellValue(p.getWeightUnit() != null ? p.getWeightUnit() : "");
                row.createCell(col++).setCellValue(p.getManufacturer() != null ? p.getManufacturer() : "");
                row.createCell(col++).setCellValue(p.getManufacturerPartNumber() != null ? p.getManufacturerPartNumber() : "");
                row.createCell(col++).setCellValue(p.getTaxCode() != null ? p.getTaxCode() : "");
                row.createCell(col).setCellValue(p.getTags() != null ? p.getTags() : "");
            }

            // Auto-size columns
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private List<Product> getProductsForExport(Long categoryId, Long brandId, Boolean active) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        List<Product> products;
        if (active != null && active) {
            products = productRepository.findAllSellableByTenantId(tenantId);
        } else {
            products = productRepository.findAllByTenantId(tenantId, Pageable.unpaged()).getContent();
        }

        // Apply filters
        if (categoryId != null) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(categoryId))
                    .collect(Collectors.toList());
        }

        if (brandId != null) {
            products = products.stream()
                    .filter(p -> p.getBrand() != null && p.getBrand().getId().equals(brandId))
                    .collect(Collectors.toList());
        }

        return products;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
