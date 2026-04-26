package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.inventory.dto.ImportResultDto;
import com.hisobnoma.platform.inventory.entity.Brand;
import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImportServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private UnitOfMeasureRepository uomRepository;
    @Mock
    private SkuGeneratorService skuGeneratorService;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ProductImportService productImportService;

    private static final Long TENANT_ID = 1L;
    private Category category;
    private Brand brand;
    private UnitOfMeasure uom;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .code("ELEC")
                .name("Electronics")
                .tenantId(TENANT_ID)
                .build();

        brand = Brand.builder()
                .id(1L)
                .code("SAM")
                .name("Samsung")
                .tenantId(TENANT_ID)
                .build();

        uom = UnitOfMeasure.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .tenantId(TENANT_ID)
                .build();
    }

    @Test
    void importFromCsv_success_createsNewProduct() throws IOException {
        // Given
        String csvContent = "SKU,Barcode,Name,Description,Short Description,Category,Brand,UOM Code,Cost Price,Selling Price,Min Selling Price,Wholesale Price,Track Inventory,Allow Negative Stock,Min Stock Level,Reorder Point,Reorder Quantity,Max Stock Level,Active,Service,Sellable,Purchasable,Track Batch,Track Serial,Weight,Weight Unit,Manufacturer,Manufacturer Part Number,Tax Code,Tags\n"
                + "PROD001,1234567890123,Test Product,Description,Short desc,Electronics,Samsung,PCS,100.00,150.00,120.00,130.00,true,false,10,20,50,100,true,false,true,true,false,false,1.5,kg,Samsung,SMP-001,VAT20,\"electronics,sample\"\n";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(category));
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));
        when(productRepository.existsByBarcodeAndTenantId("1234567890123", TENANT_ID)).thenReturn(false);
        when(productRepository.findBySkuAndTenantId("PROD001", TENANT_ID)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ImportResultDto result = productImportService.importFromCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void importFromCsv_success_updatesExistingProduct() throws IOException {
        // Given
        String csvContent = "SKU,Barcode,Name,Description,Short Description,Category,Brand,UOM Code,Cost Price,Selling Price,Min Selling Price,Wholesale Price,Track Inventory,Allow Negative Stock,Min Stock Level,Reorder Point,Reorder Quantity,Max Stock Level,Active,Service,Sellable,Purchasable,Track Batch,Track Serial,Weight,Weight Unit,Manufacturer,Manufacturer Part Number,Tax Code,Tags\n"
                + "PROD001,,Updated Product,,,,Samsung,PCS,,200.00,,,,,,,,,,,,,,,,,,,,\n";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        Product existingProduct = Product.builder()
                .id(1L)
                .sku("PROD001")
                .name("Old Name")
                .sellingPrice(new BigDecimal("150.00"))
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(category));
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));
        when(productRepository.findBySkuAndTenantId("PROD001", TENANT_ID)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        // When
        ImportResultDto result = productImportService.importFromCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
        assertEquals("Updated Product", existingProduct.getName());
    }

    @Test
    void importFromCsv_validationError_missingName() throws IOException {
        // Given
        String csvContent = "SKU,Barcode,Name,Description,Short Description,Category,Brand,UOM Code,Cost Price,Selling Price,Min Selling Price,Wholesale Price,Track Inventory,Allow Negative Stock,Min Stock Level,Reorder Point,Reorder Quantity,Max Stock Level,Active,Service,Sellable,Purchasable,Track Batch,Track Serial,Weight,Weight Unit,Manufacturer,Manufacturer Part Number,Tax Code,Tags\n"
                + "PROD001,,,,,,,PCS,,150.00,,,,,,,,,,,,,,,,,,,,\n";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(category));
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));

        // When
        ImportResultDto result = productImportService.importFromCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getErrorCount());
        assertEquals(0, result.getSuccessCount());
    }

    @Test
    void importFromCsv_validationError_missingSellingPrice() throws IOException {
        // Given
        String csvContent = "SKU,Barcode,Name,Description,Short Description,Category,Brand,UOM Code,Cost Price,Selling Price,Min Selling Price,Wholesale Price,Track Inventory,Allow Negative Stock,Min Stock Level,Reorder Point,Reorder Quantity,Max Stock Level,Active,Service,Sellable,Purchasable,Track Batch,Track Serial,Weight,Weight Unit,Manufacturer,Manufacturer Part Number,Tax Code,Tags\n"
                + "PROD001,,Test Product,,,,,PCS,,,,,,,,,,,,,,,,,,,,,,\n";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(category));
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));

        // When
        ImportResultDto result = productImportService.importFromCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getErrorCount());
        assertEquals(0, result.getSuccessCount());
    }

    @Test
    void importFromCsv_validationError_invalidCategory() throws IOException {
        // Given
        String csvContent = "SKU,Barcode,Name,Description,Short Description,Category,Brand,UOM Code,Cost Price,Selling Price,Min Selling Price,Wholesale Price,Track Inventory,Allow Negative Stock,Min Stock Level,Reorder Point,Reorder Quantity,Max Stock Level,Active,Service,Sellable,Purchasable,Track Batch,Track Serial,Weight,Weight Unit,Manufacturer,Manufacturer Part Number,Tax Code,Tags\n"
                + "PROD001,,Test Product,,,NonExistentCategory,,PCS,,150.00,,,,,,,,,,,,,,,,,,,,\n";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(category));
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));

        // When
        ImportResultDto result = productImportService.importFromCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getErrorCount());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void importFromCsv_validationError_invalidBrand() throws IOException {
        // Given
        String csvContent = "SKU,Barcode,Name,Description,Short Description,Category,Brand,UOM Code,Cost Price,Selling Price,Min Selling Price,Wholesale Price,Track Inventory,Allow Negative Stock,Min Stock Level,Reorder Point,Reorder Quantity,Max Stock Level,Active,Service,Sellable,Purchasable,Track Batch,Track Serial,Weight,Weight Unit,Manufacturer,Manufacturer Part Number,Tax Code,Tags\n"
                + "PROD001,,Test Product,,,,NonExistentBrand,PCS,,150.00,,,,,,,,,,,,,,,,,,,,\n";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(category));
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));

        // When
        ImportResultDto result = productImportService.importFromCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getErrorCount());
    }

    @Test
    void importFromCsv_validationError_invalidUomCode() throws IOException {
        // Given
        String csvContent = "SKU,Barcode,Name,Description,Short Description,Category,Brand,UOM Code,Cost Price,Selling Price,Min Selling Price,Wholesale Price,Track Inventory,Allow Negative Stock,Min Stock Level,Reorder Point,Reorder Quantity,Max Stock Level,Active,Service,Sellable,Purchasable,Track Batch,Track Serial,Weight,Weight Unit,Manufacturer,Manufacturer Part Number,Tax Code,Tags\n"
                + "PROD001,,Test Product,,,,,INVALID,,150.00,,,,,,,,,,,,,,,,,,,,\n";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(category));
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));

        // When
        ImportResultDto result = productImportService.importFromCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getErrorCount());
    }

    @Test
    void importFromCsv_validationError_duplicateBarcode() throws IOException {
        // Given
        String csvContent = "SKU,Barcode,Name,Description,Short Description,Category,Brand,UOM Code,Cost Price,Selling Price,Min Selling Price,Wholesale Price,Track Inventory,Allow Negative Stock,Min Stock Level,Reorder Point,Reorder Quantity,Max Stock Level,Active,Service,Sellable,Purchasable,Track Batch,Track Serial,Weight,Weight Unit,Manufacturer,Manufacturer Part Number,Tax Code,Tags\n"
                + "PROD001,1234567890123,Test Product,,,,,PCS,,150.00,,,,,,,,,,,,,,,,,,,,\n";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(category));
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));
        when(productRepository.existsByBarcodeAndTenantId("1234567890123", TENANT_ID)).thenReturn(true);

        // When
        ImportResultDto result = productImportService.importFromCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getErrorCount());
    }

    @Test
    void importFromCsv_generatesSkuWhenNotProvided() throws IOException {
        // Given
        String csvContent = "SKU,Barcode,Name,Description,Short Description,Category,Brand,UOM Code,Cost Price,Selling Price,Min Selling Price,Wholesale Price,Track Inventory,Allow Negative Stock,Min Stock Level,Reorder Point,Reorder Quantity,Max Stock Level,Active,Service,Sellable,Purchasable,Track Batch,Track Serial,Weight,Weight Unit,Manufacturer,Manufacturer Part Number,Tax Code,Tags\n"
                + ",,Auto SKU Product,,,,,PCS,,150.00,,,,,,,,,,,,,,,,,,,,\n";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(category));
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));
        when(skuGeneratorService.generateSkuFromName("Auto SKU Product")).thenReturn("AUT-001");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ImportResultDto result = productImportService.importFromCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
        verify(skuGeneratorService).generateSkuFromName("Auto SKU Product");
    }

    @Test
    void importFromCsv_ioException_throwsBusinessException() throws IOException {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("File read error"));

        // When/Then
        assertThrows(BusinessException.class, () -> productImportService.importFromCsv(file));
    }

    @Test
    void importFromExcel_ioException_throwsBusinessException() throws IOException {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("File read error"));

        // When/Then
        assertThrows(BusinessException.class, () -> productImportService.importFromExcel(file));
    }

    @Test
    void generateCsvTemplate_returnsValidTemplate() {
        // When
        byte[] template = productImportService.generateCsvTemplate();

        // Then
        assertNotNull(template);
        String content = new String(template, StandardCharsets.UTF_8);
        assertTrue(content.contains("SKU"));
        assertTrue(content.contains("Name"));
        assertTrue(content.contains("Selling Price"));
        assertTrue(content.contains("PROD001"));
    }

    @Test
    void generateExcelTemplate_returnsValidTemplate() throws IOException {
        // When
        byte[] template = productImportService.generateExcelTemplate();

        // Then
        assertNotNull(template);
        assertTrue(template.length > 0);
    }

    @Test
    void importFromCsv_multipleRows_mixedResults() throws IOException {
        // Given
        String csvContent = "SKU,Barcode,Name,Description,Short Description,Category,Brand,UOM Code,Cost Price,Selling Price,Min Selling Price,Wholesale Price,Track Inventory,Allow Negative Stock,Min Stock Level,Reorder Point,Reorder Quantity,Max Stock Level,Active,Service,Sellable,Purchasable,Track Batch,Track Serial,Weight,Weight Unit,Manufacturer,Manufacturer Part Number,Tax Code,Tags\n"
                + "PROD001,,Valid Product,,,,,PCS,,150.00,,,,,,,,,,,,,,,,,,,,\n"
                + "PROD002,,,,,,,PCS,,150.00,,,,,,,,,,,,,,,,,,,,\n";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(category));
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));
        when(productRepository.findBySkuAndTenantId("PROD001", TENANT_ID)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ImportResultDto result = productImportService.importFromCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalRows());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getErrorCount());
    }

    @Test
    void importFromCsv_emptyFile_returnsZeroResults() throws IOException {
        // Given
        String csvContent = "SKU,Barcode,Name,Description,Short Description,Category,Brand,UOM Code,Cost Price,Selling Price,Min Selling Price,Wholesale Price,Track Inventory,Allow Negative Stock,Min Stock Level,Reorder Point,Reorder Quantity,Max Stock Level,Active,Service,Sellable,Purchasable,Track Batch,Track Serial,Weight,Weight Unit,Manufacturer,Manufacturer Part Number,Tax Code,Tags\n";

        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(categoryRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(category));
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));

        // When
        ImportResultDto result = productImportService.importFromCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalRows());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
    }
}
