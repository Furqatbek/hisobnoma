package com.hisobnoma.platform.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreateProductRequest;
import com.hisobnoma.platform.inventory.dto.ProductDto;
import com.hisobnoma.platform.inventory.dto.UpdateProductRequest;
import com.hisobnoma.platform.inventory.service.BarcodeService;
import com.hisobnoma.platform.inventory.service.ProductImageService;
import com.hisobnoma.platform.inventory.service.ProductImportService;
import com.hisobnoma.platform.inventory.service.ProductService;
import com.hisobnoma.platform.inventory.service.ProductVendorService;
import com.hisobnoma.platform.inventory.service.SkuGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private SkuGeneratorService skuGeneratorService;

    @MockBean
    private BarcodeService barcodeService;

    @MockBean
    private ProductImageService productImageService;

    @MockBean
    private ProductImportService productImportService;

    @MockBean
    private ProductVendorService productVendorService;

    // ==================== getProducts ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getProducts_authenticated_returns200() throws Exception {
        ProductDto dto = ProductDto.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .sellingPrice(new BigDecimal("100.00"))
                .active(true)
                .build();
        PageResponse<ProductDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(productService.getProducts(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-001"));
    }

    @Test
    void getProducts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getProducts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products"))
                .andExpect(status().isForbidden());
    }

    // ==================== getActiveProducts ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getActiveProducts_authenticated_returns200() throws Exception {
        ProductDto dto = ProductDto.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .active(true)
                .build();
        PageResponse<ProductDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(productService.getActiveProducts(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/products/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    void getActiveProducts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/active"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getActiveProducts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== getProduct ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getProduct_found_returns200() throws Exception {
        ProductDto dto = ProductDto.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .build();
        when(productService.getProduct(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void getProduct_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getProduct_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getProductBySku ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getProductBySku_found_returns200() throws Exception {
        ProductDto dto = ProductDto.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .build();
        when(productService.getProductBySku("SKU-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/products/sku/SKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void getProductBySku_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/sku/SKU-001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getProductBySku_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/sku/SKU-001"))
                .andExpect(status().isForbidden());
    }

    // ==================== getProductByBarcode ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getProductByBarcode_found_returns200() throws Exception {
        ProductDto dto = ProductDto.builder()
                .id(1L)
                .sku("SKU-001")
                .barcode("1234567890123")
                .name("Test Product")
                .build();
        when(productService.getProductByBarcode("1234567890123")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/products/barcode/1234567890123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("1234567890123"));
    }

    @Test
    void getProductByBarcode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/barcode/1234567890123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getProductByBarcode_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/barcode/1234567890123"))
                .andExpect(status().isForbidden());
    }

    // ==================== searchProducts ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void searchProducts_authenticated_returns200() throws Exception {
        ProductDto dto = ProductDto.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .build();
        PageResponse<ProductDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(productService.searchProducts(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/products/search").param("q", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-001"));
    }

    @Test
    void searchProducts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/search").param("q", "Test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void searchProducts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/search").param("q", "Test"))
                .andExpect(status().isForbidden());
    }

    // ==================== getProductsByCategory ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getProductsByCategory_authenticated_returns200() throws Exception {
        ProductDto dto = ProductDto.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .categoryId(1L)
                .build();
        PageResponse<ProductDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(productService.getProductsByCategory(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/products/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-001"));
    }

    @Test
    void getProductsByCategory_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/category/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getProductsByCategory_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/category/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getProductsByBrand ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getProductsByBrand_authenticated_returns200() throws Exception {
        ProductDto dto = ProductDto.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .brandId(1L)
                .build();
        PageResponse<ProductDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(productService.getProductsByBrand(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/products/brand/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-001"));
    }

    @Test
    void getProductsByBrand_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/brand/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getProductsByBrand_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/brand/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== createProduct ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_CREATE")
    void createProduct_valid_returns201() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .baseUomId(1L)
                .sellingPrice(new BigDecimal("150.00"))
                .build();

        ProductDto dto = ProductDto.builder()
                .id(2L)
                .sku("SKU-002")
                .name("New Product")
                .sellingPrice(new BigDecimal("150.00"))
                .build();
        when(productService.createProduct(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Product"));
    }

    @Test
    void createProduct_unauthenticated_returns403() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .baseUomId(1L)
                .sellingPrice(new BigDecimal("150.00"))
                .build();

        mockMvc.perform(post("/api/v1/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createProduct_noPermission_returns403() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .baseUomId(1L)
                .sellingPrice(new BigDecimal("150.00"))
                .build();

        mockMvc.perform(post("/api/v1/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== updateProduct ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_UPDATE")
    void updateProduct_valid_returns200() throws Exception {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Updated Product")
                .build();

        ProductDto dto = ProductDto.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Updated Product")
                .build();
        when(productService.updateProduct(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/inventory/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"));
    }

    @Test
    void updateProduct_unauthenticated_returns403() throws Exception {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Updated Product")
                .build();

        mockMvc.perform(put("/api/v1/inventory/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void updateProduct_noPermission_returns403() throws Exception {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Updated Product")
                .build();

        mockMvc.perform(put("/api/v1/inventory/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== deleteProduct ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_DELETE")
    void deleteProduct_returns204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/v1/inventory/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProduct_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/products/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteProduct_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/products/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== getProductCount ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getProductCount_authenticated_returns200() throws Exception {
        when(productService.getProductCount()).thenReturn(10L);
        when(productService.getActiveProductCount()).thenReturn(8L);

        mockMvc.perform(get("/api/v1/inventory/products/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.active").value(8));
    }

    @Test
    void getProductCount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/count"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getProductCount_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/count"))
                .andExpect(status().isForbidden());
    }

    // ==================== generateSku ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_CREATE")
    void generateSku_authenticated_returns200() throws Exception {
        when(skuGeneratorService.generateSku(any())).thenReturn("SKU-AUTO-001");

        mockMvc.perform(get("/api/v1/inventory/products/generate-sku"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-AUTO-001"));
    }

    @Test
    void generateSku_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/generate-sku"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void generateSku_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/generate-sku"))
                .andExpect(status().isForbidden());
    }

    // ==================== generateSkuFromName ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_CREATE")
    void generateSkuFromName_authenticated_returns200() throws Exception {
        when(skuGeneratorService.generateSkuFromName("Test Product")).thenReturn("TST-PRD-001");

        mockMvc.perform(get("/api/v1/inventory/products/generate-sku-from-name").param("name", "Test Product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TST-PRD-001"));
    }

    @Test
    void generateSkuFromName_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/generate-sku-from-name").param("name", "Test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void generateSkuFromName_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/generate-sku-from-name").param("name", "Test"))
                .andExpect(status().isForbidden());
    }

    // ==================== validateSku ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void validateSku_authenticated_returns200() throws Exception {
        when(skuGeneratorService.isValidSku("SKU-001")).thenReturn(true);
        when(skuGeneratorService.skuExists("SKU-001")).thenReturn(false);

        mockMvc.perform(get("/api/v1/inventory/products/validate-sku/SKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void validateSku_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/validate-sku/SKU-001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void validateSku_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/validate-sku/SKU-001"))
                .andExpect(status().isForbidden());
    }

    // ==================== generateBarcode ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_CREATE")
    void generateBarcode_authenticated_returns200() throws Exception {
        when(barcodeService.generateInternalBarcode(any())).thenReturn("INT-1234567890");

        mockMvc.perform(get("/api/v1/inventory/products/generate-barcode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("INT-1234567890"));
    }

    @Test
    void generateBarcode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/generate-barcode"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void generateBarcode_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/generate-barcode"))
                .andExpect(status().isForbidden());
    }

    // ==================== validateBarcode ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void validateBarcode_authenticated_returns200() throws Exception {
        when(barcodeService.isValidBarcode("1234567890123")).thenReturn(true);
        when(barcodeService.barcodeExists("1234567890123")).thenReturn(false);
        when(barcodeService.getBarcodeType("1234567890123")).thenReturn("EAN13");

        mockMvc.perform(get("/api/v1/inventory/products/validate-barcode/1234567890123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.type").value("EAN13"));
    }

    @Test
    void validateBarcode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/validate-barcode/1234567890123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void validateBarcode_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/validate-barcode/1234567890123"))
                .andExpect(status().isForbidden());
    }

    // ==================== getProductImages ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getProductImages_authenticated_returns200() throws Exception {
        when(productImageService.getImages(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/inventory/products/1/images"))
                .andExpect(status().isOk());
    }

    @Test
    void getProductImages_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1/images"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getProductImages_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1/images"))
                .andExpect(status().isForbidden());
    }

    // ==================== getProductVariants ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getProductVariants_authenticated_returns200() throws Exception {
        when(productService.getVariants(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/inventory/products/1/variants"))
                .andExpect(status().isOk());
    }

    @Test
    void getProductVariants_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1/variants"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getProductVariants_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1/variants"))
                .andExpect(status().isForbidden());
    }

    // ==================== getProductVendors ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getProductVendors_authenticated_returns200() throws Exception {
        when(productVendorService.getVendorsForProduct(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/inventory/products/1/vendors"))
                .andExpect(status().isOk());
    }

    @Test
    void getProductVendors_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1/vendors"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getProductVendors_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/1/vendors"))
                .andExpect(status().isForbidden());
    }

    // ==================== getImportTemplate ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void getImportTemplate_authenticated_returns200() throws Exception {
        when(productImportService.generateCsvTemplate()).thenReturn("sku,name".getBytes());

        mockMvc.perform(get("/api/v1/inventory/products/import/template"))
                .andExpect(status().isOk());
    }

    @Test
    void getImportTemplate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/import/template"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getImportTemplate_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/import/template"))
                .andExpect(status().isForbidden());
    }

    // ==================== exportProducts ====================

    @Test
    @WithMockUser(authorities = "INVENTORY_PRODUCT_READ")
    void exportProducts_authenticated_returns200() throws Exception {
        when(productService.exportToCsv(any(), any(), any())).thenReturn("data".getBytes());

        mockMvc.perform(get("/api/v1/inventory/products/export"))
                .andExpect(status().isOk());
    }

    @Test
    void exportProducts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void exportProducts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/products/export"))
                .andExpect(status().isForbidden());
    }
}
