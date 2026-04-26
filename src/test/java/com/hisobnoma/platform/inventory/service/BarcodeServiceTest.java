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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarcodeServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;

    @InjectMocks
    private BarcodeService barcodeService;

    // ==================== generateInternalBarcode ====================

    @Test
    void generateInternalBarcode_withPrefix_returnsBarcode() {
        // Given
        when(productRepository.findByBarcode(anyString())).thenReturn(Optional.empty());
        when(variantRepository.findByBarcode(anyString())).thenReturn(Optional.empty());

        // When
        String barcode = barcodeService.generateInternalBarcode("TST");

        // Then
        assertNotNull(barcode);
        assertTrue(barcode.startsWith("TST"));
        assertEquals(12, barcode.length()); // 3 prefix + 9 random
    }

    @Test
    void generateInternalBarcode_nullPrefix_usesINT() {
        // Given
        when(productRepository.findByBarcode(anyString())).thenReturn(Optional.empty());
        when(variantRepository.findByBarcode(anyString())).thenReturn(Optional.empty());

        // When
        String barcode = barcodeService.generateInternalBarcode(null);

        // Then
        assertNotNull(barcode);
        assertTrue(barcode.startsWith("INT"));
    }

    @Test
    void generateInternalBarcode_collidesOnce_regenerates() {
        // Given
        Product existingProduct = Product.builder().id(1L).barcode("TST000000001").build();

        when(productRepository.findByBarcode(anyString()))
                .thenReturn(Optional.of(existingProduct))
                .thenReturn(Optional.empty());
        when(variantRepository.findByBarcode(anyString()))
                .thenReturn(Optional.empty());

        // When
        String barcode = barcodeService.generateInternalBarcode("TST");

        // Then
        assertNotNull(barcode);
        assertTrue(barcode.startsWith("TST"));
        verify(productRepository, atLeast(2)).findByBarcode(anyString());
    }

    // ==================== generateEan13 ====================

    @Test
    void generateEan13_validParams_returns13DigitBarcode() {
        // Given
        when(productRepository.findByBarcode(anyString())).thenReturn(Optional.empty());
        when(variantRepository.findByBarcode(anyString())).thenReturn(Optional.empty());

        // When
        String barcode = barcodeService.generateEan13("860", "12345");

        // Then
        assertNotNull(barcode);
        assertEquals(13, barcode.length());
        assertTrue(barcode.matches("\\d+"));
        assertTrue(barcode.startsWith("860"));
    }

    @Test
    void generateEan13_nullCountryCode_usesDefault() {
        // Given
        when(productRepository.findByBarcode(anyString())).thenReturn(Optional.empty());
        when(variantRepository.findByBarcode(anyString())).thenReturn(Optional.empty());

        // When
        String barcode = barcodeService.generateEan13(null, "12345");

        // Then
        assertNotNull(barcode);
        assertEquals(13, barcode.length());
        assertTrue(barcode.startsWith("200"));
    }

    @Test
    void generateEan13_shortCompanyCode_generatesRandom() {
        // Given
        when(productRepository.findByBarcode(anyString())).thenReturn(Optional.empty());
        when(variantRepository.findByBarcode(anyString())).thenReturn(Optional.empty());

        // When
        String barcode = barcodeService.generateEan13("860", "12");

        // Then
        assertNotNull(barcode);
        assertEquals(13, barcode.length());
    }

    @Test
    void generateEan13_validCheckDigit() {
        // Given
        when(productRepository.findByBarcode(anyString())).thenReturn(Optional.empty());
        when(variantRepository.findByBarcode(anyString())).thenReturn(Optional.empty());

        // When
        String barcode = barcodeService.generateEan13("860", "12345");

        // Then
        assertTrue(barcodeService.isValidEan13(barcode));
    }

    // ==================== isValidEan13 ====================

    @Test
    void isValidEan13_validBarcode_returnsTrue() {
        // Given - known valid EAN-13: 4006381333931
        // When/Then
        assertTrue(barcodeService.isValidEan13("4006381333931"));
    }

    @Test
    void isValidEan13_invalidCheckDigit_returnsFalse() {
        // Given - corrupted last digit
        // When/Then
        assertFalse(barcodeService.isValidEan13("4006381333932"));
    }

    @Test
    void isValidEan13_nullBarcode_returnsFalse() {
        // When/Then
        assertFalse(barcodeService.isValidEan13(null));
    }

    @Test
    void isValidEan13_wrongLength_returnsFalse() {
        // When/Then
        assertFalse(barcodeService.isValidEan13("123456"));
    }

    @Test
    void isValidEan13_nonNumeric_returnsFalse() {
        // When/Then
        assertFalse(barcodeService.isValidEan13("400638133393A"));
    }

    // ==================== isValidUpcA ====================

    @Test
    void isValidUpcA_validBarcode_returnsTrue() {
        // Given - known valid UPC-A: 036000291452
        // When/Then
        assertTrue(barcodeService.isValidUpcA("036000291452"));
    }

    @Test
    void isValidUpcA_invalidCheckDigit_returnsFalse() {
        // Given - corrupted last digit
        // When/Then
        assertFalse(barcodeService.isValidUpcA("036000291453"));
    }

    @Test
    void isValidUpcA_nullBarcode_returnsFalse() {
        // When/Then
        assertFalse(barcodeService.isValidUpcA(null));
    }

    @Test
    void isValidUpcA_wrongLength_returnsFalse() {
        // When/Then
        assertFalse(barcodeService.isValidUpcA("123456"));
    }

    @Test
    void isValidUpcA_nonNumeric_returnsFalse() {
        // When/Then
        assertFalse(barcodeService.isValidUpcA("03600029145A"));
    }

    // ==================== barcodeExists ====================

    @Test
    void barcodeExists_foundInProducts_returnsTrue() {
        // Given
        Product existingProduct = Product.builder().id(1L).barcode("TEST-BC").build();
        when(productRepository.findByBarcode("TEST-BC")).thenReturn(Optional.of(existingProduct));

        // When/Then
        assertTrue(barcodeService.barcodeExists("TEST-BC"));
    }

    @Test
    void barcodeExists_foundInVariants_returnsTrue() {
        // Given
        ProductVariant variant = ProductVariant.builder().id(1L).barcode("TEST-BC").build();
        when(productRepository.findByBarcode("TEST-BC")).thenReturn(Optional.empty());
        when(variantRepository.findByBarcode("TEST-BC")).thenReturn(Optional.of(variant));

        // When/Then
        assertTrue(barcodeService.barcodeExists("TEST-BC"));
    }

    @Test
    void barcodeExists_notFound_returnsFalse() {
        // Given
        when(productRepository.findByBarcode("MISSING")).thenReturn(Optional.empty());
        when(variantRepository.findByBarcode("MISSING")).thenReturn(Optional.empty());

        // When/Then
        assertFalse(barcodeService.barcodeExists("MISSING"));
    }

    // ==================== isValidBarcode ====================

    @Test
    void isValidBarcode_nullBarcode_returnsFalse() {
        // When/Then
        assertFalse(barcodeService.isValidBarcode(null));
    }

    @Test
    void isValidBarcode_emptyBarcode_returnsFalse() {
        // When/Then
        assertFalse(barcodeService.isValidBarcode(""));
    }

    @Test
    void isValidBarcode_validEan13_returnsTrue() {
        // When/Then
        assertTrue(barcodeService.isValidBarcode("4006381333931"));
    }

    @Test
    void isValidBarcode_validUpcA_returnsTrue() {
        // When/Then
        assertTrue(barcodeService.isValidBarcode("036000291452"));
    }

    @Test
    void isValidBarcode_validInternalBarcode_returnsTrue() {
        // When/Then
        assertTrue(barcodeService.isValidBarcode("INT-12345-AB"));
    }

    @Test
    void isValidBarcode_tooShort_returnsFalse() {
        // When/Then
        assertFalse(barcodeService.isValidBarcode("ABC"));
    }

    @Test
    void isValidBarcode_tooLong_returnsFalse() {
        // When/Then
        String longBarcode = "A".repeat(51);
        assertFalse(barcodeService.isValidBarcode(longBarcode));
    }

    @Test
    void isValidBarcode_internalWithSpecialChars_returnsFalse() {
        // When/Then
        assertFalse(barcodeService.isValidBarcode("INT@#$%^"));
    }

    // ==================== getBarcodeType ====================

    @Test
    void getBarcodeType_nullBarcode_returnsUnknown() {
        // When/Then
        assertEquals("UNKNOWN", barcodeService.getBarcodeType(null));
    }

    @Test
    void getBarcodeType_emptyBarcode_returnsUnknown() {
        // When/Then
        assertEquals("UNKNOWN", barcodeService.getBarcodeType(""));
    }

    @Test
    void getBarcodeType_13Digits_returnsEan13() {
        // When/Then
        assertEquals("EAN-13", barcodeService.getBarcodeType("4006381333931"));
    }

    @Test
    void getBarcodeType_12Digits_returnsUpcA() {
        // When/Then
        assertEquals("UPC-A", barcodeService.getBarcodeType("036000291452"));
    }

    @Test
    void getBarcodeType_8Digits_returnsEan8() {
        // When/Then
        assertEquals("EAN-8", barcodeService.getBarcodeType("12345678"));
    }

    @Test
    void getBarcodeType_alphanumeric_returnsInternal() {
        // When/Then
        assertEquals("INTERNAL", barcodeService.getBarcodeType("INT-12345-ABC"));
    }
}
