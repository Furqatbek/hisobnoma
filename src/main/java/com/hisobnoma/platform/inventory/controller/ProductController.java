package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.service.BarcodeService;
import com.hisobnoma.platform.inventory.service.ProductImageService;
import com.hisobnoma.platform.inventory.service.ProductImportService;
import com.hisobnoma.platform.inventory.service.ProductService;
import com.hisobnoma.platform.inventory.service.ProductVendorService;
import com.hisobnoma.platform.inventory.service.SkuGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final SkuGeneratorService skuGeneratorService;
    private final BarcodeService barcodeService;
    private final ProductImageService productImageService;
    private final ProductImportService productImportService;
    private final ProductVendorService productVendorService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<PageResponse<ProductDto>> getProducts(
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getProducts(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<PageResponse<ProductDto>> getActiveProducts(
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getActiveProducts(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<ProductDto> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    @GetMapping("/barcode/{barcode}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<ProductDto> getProductByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.getProductByBarcode(barcode));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<PageResponse<ProductDto>> searchProducts(
            @RequestParam String q,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.searchProducts(q, pageable));
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<PageResponse<ProductDto>> getProductsByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId, pageable));
    }

    @GetMapping("/brand/{brandId}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<PageResponse<ProductDto>> getProductsByBrand(
            @PathVariable Long brandId,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsByBrand(brandId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_CREATE')")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_DELETE')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<Map<String, Long>> getProductCount() {
        return ResponseEntity.ok(Map.of(
                "total", productService.getProductCount(),
                "active", productService.getActiveProductCount()
        ));
    }

    // SKU Generation endpoints
    @GetMapping("/generate-sku")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_CREATE')")
    public ResponseEntity<Map<String, String>> generateSku(
            @RequestParam(required = false) String prefix) {
        String sku = skuGeneratorService.generateSku(prefix);
        return ResponseEntity.ok(Map.of("sku", sku));
    }

    @GetMapping("/generate-sku-from-name")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_CREATE')")
    public ResponseEntity<Map<String, String>> generateSkuFromName(
            @RequestParam String name) {
        String sku = skuGeneratorService.generateSkuFromName(name);
        return ResponseEntity.ok(Map.of("sku", sku));
    }

    @GetMapping("/validate-sku/{sku}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<Map<String, Object>> validateSku(@PathVariable String sku) {
        boolean valid = skuGeneratorService.isValidSku(sku);
        boolean exists = skuGeneratorService.skuExists(sku);
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "exists", exists,
                "available", valid && !exists
        ));
    }

    // Barcode endpoints
    @GetMapping("/generate-barcode")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_CREATE')")
    public ResponseEntity<Map<String, String>> generateBarcode(
            @RequestParam(required = false) String prefix) {
        String barcode = barcodeService.generateInternalBarcode(prefix);
        return ResponseEntity.ok(Map.of("barcode", barcode));
    }

    @GetMapping("/generate-ean13")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_CREATE')")
    public ResponseEntity<Map<String, String>> generateEan13(
            @RequestParam(defaultValue = "200") String countryCode,
            @RequestParam(required = false) String companyCode) {
        String barcode = barcodeService.generateEan13(countryCode, companyCode);
        return ResponseEntity.ok(Map.of("barcode", barcode));
    }

    @GetMapping("/validate-barcode/{barcode}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<Map<String, Object>> validateBarcode(@PathVariable String barcode) {
        boolean valid = barcodeService.isValidBarcode(barcode);
        boolean exists = barcodeService.barcodeExists(barcode);
        String type = barcodeService.getBarcodeType(barcode);
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "exists", exists,
                "available", valid && !exists,
                "type", type
        ));
    }

    // ==================== Image Endpoints ====================

    @GetMapping("/{id}/images")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<List<ProductImageDto>> getProductImages(@PathVariable Long id) {
        return ResponseEntity.ok(productImageService.getImages(id));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<ProductImageDto> uploadProductImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String altText,
            @RequestParam(required = false) String title) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productImageService.uploadImage(id, file, altText, title));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<Void> deleteProductImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        productImageService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<ProductImageDto> setProductPrimaryImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(productImageService.setPrimaryImage(id, imageId));
    }

    @PutMapping("/{id}/images/reorder")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<List<ProductImageDto>> reorderProductImages(
            @PathVariable Long id,
            @RequestBody List<Long> imageIds) {
        return ResponseEntity.ok(productImageService.reorderImages(id, imageIds));
    }

    // ==================== Variant Endpoints ====================

    @GetMapping("/{id}/variants")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<List<ProductVariantDto>> getProductVariants(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getVariants(id));
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<ProductVariantDto> addProductVariant(
            @PathVariable Long id,
            @Valid @RequestBody CreateVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addVariant(id, request));
    }

    @PutMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<ProductVariantDto> updateProductVariant(
            @PathVariable Long id,
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateVariantRequest request) {
        return ResponseEntity.ok(productService.updateVariant(id, variantId, request));
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<Void> deleteProductVariant(
            @PathVariable Long id,
            @PathVariable Long variantId) {
        productService.deleteVariant(id, variantId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Vendor Endpoints ====================

    @GetMapping("/{id}/vendors")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<List<ProductVendorDto>> getProductVendors(@PathVariable Long id) {
        return ResponseEntity.ok(productVendorService.getVendorsForProduct(id));
    }

    @PostMapping("/{id}/vendors")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<ProductVendorDto> addProductVendor(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductVendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productVendorService.addVendorToProduct(id, request));
    }

    @PutMapping("/{id}/vendors/{vendorLinkId}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<ProductVendorDto> updateProductVendor(
            @PathVariable Long id,
            @PathVariable Long vendorLinkId,
            @Valid @RequestBody CreateProductVendorRequest request) {
        return ResponseEntity.ok(productVendorService.updateProductVendor(id, vendorLinkId, request));
    }

    @DeleteMapping("/{id}/vendors/{vendorLinkId}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<Void> removeProductVendor(
            @PathVariable Long id,
            @PathVariable Long vendorLinkId) {
        productVendorService.removeVendorFromProduct(id, vendorLinkId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Import/Export Endpoints ====================

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_CREATE')")
    public ResponseEntity<ImportResultDto> importProducts(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "csv") String format) {
        ImportResultDto result;
        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            result = productImportService.importFromExcel(file);
        } else {
            result = productImportService.importFromCsv(file);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/import/template")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<byte[]> getImportTemplate(
            @RequestParam(defaultValue = "csv") String format) {
        try {
            byte[] content;
            String filename;
            String contentType;

            if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
                content = productImportService.generateExcelTemplate();
                filename = "product_import_template.xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else {
                content = productImportService.generateCsvTemplate();
                filename = "product_import_template.csv";
                contentType = "text/csv";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate template: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean active) {
        try {
            byte[] content;
            String filename;
            String contentType;

            if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
                content = productService.exportToExcel(categoryId, brandId, active);
                filename = "products_export.xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else {
                content = productService.exportToCsv(categoryId, brandId, active);
                filename = "products_export.csv";
                contentType = "text/csv";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export products: " + e.getMessage());
        }
    }
}
