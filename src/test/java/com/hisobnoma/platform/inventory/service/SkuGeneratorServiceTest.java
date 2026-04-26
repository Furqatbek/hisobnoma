package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkuGeneratorServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;

    @InjectMocks
    private SkuGeneratorService skuGeneratorService;

    // ==================== generateSku ====================

    @Test
    void generateSku_withPrefix_returnsFormattedSku() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        // When
        String sku = skuGeneratorService.generateSku("ELC");

        // Then
        assertNotNull(sku);
        assertTrue(sku.startsWith("ELC-" + datePart + "-"));
        assertTrue(sku.matches("^ELC-\\d{8}-[A-Z0-9]{5}$"));
    }

    @Test
    void generateSku_nullPrefix_usesPRD() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateSku(null);

        // Then
        assertNotNull(sku);
        assertTrue(sku.startsWith("PRD-"));
    }

    @Test
    void generateSku_lowercasePrefix_uppercased() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateSku("abc");

        // Then
        assertNotNull(sku);
        assertTrue(sku.startsWith("ABC-"));
    }

    @Test
    void generateSku_collidesOnce_regenerates() {
        // Given
        Product existingProduct = Product.builder().id(1L).sku("ELC-20260426-AAAAA").build();

        when(productRepository.findBySku(anyString()))
                .thenReturn(Optional.of(existingProduct))
                .thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateSku("ELC");

        // Then
        assertNotNull(sku);
        verify(productRepository, atLeast(2)).findBySku(anyString());
    }

    // ==================== generateSequentialSku ====================

    @Test
    void generateSequentialSku_withPrefix_returnsSequentialSku() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateSequentialSku("ELC");

        // Then
        assertNotNull(sku);
        assertTrue(sku.startsWith("ELC-"));
        assertTrue(sku.matches("^ELC-\\d{6}$"));
    }

    @Test
    void generateSequentialSku_nullPrefix_usesPRD() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateSequentialSku(null);

        // Then
        assertNotNull(sku);
        assertTrue(sku.startsWith("PRD-"));
    }

    @Test
    void generateSequentialSku_multipleCalls_incrementSequence() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku1 = skuGeneratorService.generateSequentialSku("ELC");
        String sku2 = skuGeneratorService.generateSequentialSku("ELC");

        // Then
        assertNotNull(sku1);
        assertNotNull(sku2);
        assertNotEquals(sku1, sku2);
    }

    // ==================== generateVariantSku ====================

    @Test
    void generateVariantSku_withOptions_returnsVariantSku() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());
        when(variantRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateVariantSku("PROD-001", "Red", "Large");

        // Then
        assertNotNull(sku);
        assertTrue(sku.startsWith("PROD-001-"));
        assertTrue(sku.contains("RED"));
        assertTrue(sku.contains("LAR"));
    }

    @Test
    void generateVariantSku_noOptions_returnsParentSku() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());
        when(variantRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateVariantSku("PROD-001");

        // Then
        assertNotNull(sku);
        assertEquals("PROD-001", sku);
    }

    @Test
    void generateVariantSku_collidesOnce_appendsCounter() {
        // Given
        ProductVariant existingVariant = ProductVariant.builder().id(1L).sku("PROD-001-RED").build();

        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());
        when(variantRepository.findBySku("PROD-001-RED")).thenReturn(Optional.of(existingVariant));
        when(variantRepository.findBySku("PROD-001-RED-1")).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateVariantSku("PROD-001", "Red");

        // Then
        assertNotNull(sku);
        assertEquals("PROD-001-RED-1", sku);
    }

    @Test
    void generateVariantSku_nullOption_skipped() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());
        when(variantRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateVariantSku("PROD-001", null, "Large");

        // Then
        assertNotNull(sku);
        assertTrue(sku.contains("LAR"));
        assertFalse(sku.contains("--")); // null option should not add double dash
    }

    @Test
    void generateVariantSku_emptyOption_skipped() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());
        when(variantRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateVariantSku("PROD-001", "", "Large");

        // Then
        assertNotNull(sku);
        assertTrue(sku.contains("LAR"));
    }

    // ==================== generateSkuFromName ====================

    @Test
    void generateSkuFromName_normalName_returnsSkuFromWords() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateSkuFromName("Samsung Galaxy Phone");

        // Then
        assertNotNull(sku);
        assertTrue(sku.startsWith("SAMGALPH"));
        assertTrue(sku.contains("-"));
    }

    @Test
    void generateSkuFromName_singleWord_usesPartialWord() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateSkuFromName("Samsung");

        // Then
        assertNotNull(sku);
        assertTrue(sku.startsWith("SAM-"));
    }

    @Test
    void generateSkuFromName_nullName_generatesDefaultSku() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateSkuFromName(null);

        // Then
        assertNotNull(sku);
        assertTrue(sku.startsWith("PRD-")); // Falls back to generateSku(null)
    }

    @Test
    void generateSkuFromName_emptyName_generatesDefaultSku() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateSkuFromName("");

        // Then
        assertNotNull(sku);
        assertTrue(sku.startsWith("PRD-"));
    }

    @Test
    void generateSkuFromName_specialChars_removedFromSku() {
        // Given
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        // When
        String sku = skuGeneratorService.generateSkuFromName("Product @#$% 123");

        // Then
        assertNotNull(sku);
        assertTrue(sku.matches("^[A-Z0-9\\-]+$"));
    }

    // ==================== skuExists ====================

    @Test
    void skuExists_found_returnsTrue() {
        // Given
        Product product = Product.builder().id(1L).sku("EXISTING-SKU").build();
        when(productRepository.findBySku("EXISTING-SKU")).thenReturn(Optional.of(product));

        // When/Then
        assertTrue(skuGeneratorService.skuExists("EXISTING-SKU"));
    }

    @Test
    void skuExists_notFound_returnsFalse() {
        // Given
        when(productRepository.findBySku("MISSING-SKU")).thenReturn(Optional.empty());

        // When/Then
        assertFalse(skuGeneratorService.skuExists("MISSING-SKU"));
    }

    // ==================== variantSkuExists ====================

    @Test
    void variantSkuExists_found_returnsTrue() {
        // Given
        ProductVariant variant = ProductVariant.builder().id(1L).sku("VAR-SKU").build();
        when(variantRepository.findBySku("VAR-SKU")).thenReturn(Optional.of(variant));

        // When/Then
        assertTrue(skuGeneratorService.variantSkuExists("VAR-SKU"));
    }

    @Test
    void variantSkuExists_notFound_returnsFalse() {
        // Given
        when(variantRepository.findBySku("MISSING-VAR")).thenReturn(Optional.empty());

        // When/Then
        assertFalse(skuGeneratorService.variantSkuExists("MISSING-VAR"));
    }

    // ==================== isValidSku ====================

    @Test
    void isValidSku_validSku_returnsTrue() {
        // When/Then
        assertTrue(skuGeneratorService.isValidSku("PROD-001"));
    }

    @Test
    void isValidSku_alphanumericWithDashes_returnsTrue() {
        // When/Then
        assertTrue(skuGeneratorService.isValidSku("ELC-20260426-AB12C"));
    }

    @Test
    void isValidSku_nullSku_returnsFalse() {
        // When/Then
        assertFalse(skuGeneratorService.isValidSku(null));
    }

    @Test
    void isValidSku_emptySku_returnsFalse() {
        // When/Then
        assertFalse(skuGeneratorService.isValidSku(""));
    }

    @Test
    void isValidSku_tooShort_returnsFalse() {
        // When/Then
        assertFalse(skuGeneratorService.isValidSku("AB"));
    }

    @Test
    void isValidSku_tooLong_returnsFalse() {
        // When/Then
        String longSku = "A".repeat(51);
        assertFalse(skuGeneratorService.isValidSku(longSku));
    }

    @Test
    void isValidSku_specialChars_returnsFalse() {
        // When/Then
        assertFalse(skuGeneratorService.isValidSku("PROD@001"));
    }

    @Test
    void isValidSku_exactlyThreeChars_returnsTrue() {
        // When/Then
        assertTrue(skuGeneratorService.isValidSku("ABC"));
    }

    @Test
    void isValidSku_exactly50Chars_returnsTrue() {
        // When/Then
        String fiftyCharSku = "A".repeat(50);
        assertTrue(skuGeneratorService.isValidSku(fiftyCharSku));
    }
}
