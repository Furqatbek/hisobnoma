package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.ProductImageDto;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductImage;
import com.hisobnoma.platform.inventory.repository.ProductImageRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductImageRepository imageRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ProductImageService productImageService;

    private static final Long TENANT_ID = 1L;
    private Product product;
    private ProductImage image;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productImageService, "storagePath", "/tmp/test-uploads");
        ReflectionTestUtils.setField(productImageService, "baseUrl", "/uploads");
        ReflectionTestUtils.setField(productImageService, "maxImagesPerProduct", 10);

        product = Product.builder()
                .id(1L)
                .sku("PRD-001")
                .name("Test Product")
                .tenantId(TENANT_ID)
                .build();

        image = ProductImage.builder()
                .id(1L)
                .product(product)
                .tenantId(TENANT_ID)
                .imageUrl("/uploads/tenant-1/products/1/test.jpg")
                .altText("Test image")
                .title("Test")
                .sortOrder(0)
                .primary(true)
                .active(true)
                .build();
    }

    @Test
    void getImages_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(List.of(image));

        // When
        List<ProductImageDto> result = productImageService.getImages(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/uploads/tenant-1/products/1/test.jpg", result.get(0).getImageUrl());
        assertTrue(result.get(0).isPrimary());
    }

    @Test
    void getImages_productNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productImageService.getImages(999L));
    }

    @Test
    void uploadImage_success_firstImage() throws IOException {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.countByProductId(1L)).thenReturn(0L);
        when(imageRepository.save(any(ProductImage.class))).thenReturn(image);

        // When
        ProductImageDto result = productImageService.uploadImage(1L, file, "Alt text", "Title");

        // Then
        assertNotNull(result);
        verify(imageRepository).save(any(ProductImage.class));
        verify(productRepository).save(any(Product.class)); // Sets primary image URL
    }

    @Test
    void uploadImage_productNotFound_throwsNotFoundException() {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () ->
                productImageService.uploadImage(999L, file, "Alt", "Title"));
    }

    @Test
    void uploadImage_maxImagesReached_throwsBusinessException() {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.countByProductId(1L)).thenReturn(10L);

        // When/Then
        assertThrows(BusinessException.class, () ->
                productImageService.uploadImage(1L, file, "Alt", "Title"));
        verify(imageRepository, never()).save(any());
    }

    @Test
    void uploadImage_emptyFile_throwsBusinessException() {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.countByProductId(1L)).thenReturn(0L);

        // When/Then
        assertThrows(BusinessException.class, () ->
                productImageService.uploadImage(1L, file, "Alt", "Title"));
    }

    @Test
    void uploadImage_nonImageFile_throwsBusinessException() {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.countByProductId(1L)).thenReturn(0L);

        // When/Then
        assertThrows(BusinessException.class, () ->
                productImageService.uploadImage(1L, file, "Alt", "Title"));
    }

    @Test
    void uploadImage_fileTooLarge_throwsBusinessException() {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(11L * 1024 * 1024); // 11MB

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.countByProductId(1L)).thenReturn(0L);

        // When/Then
        assertThrows(BusinessException.class, () ->
                productImageService.uploadImage(1L, file, "Alt", "Title"));
    }

    @Test
    void deleteImage_success_wasPrimary_setsNewPrimary() {
        // Given
        ProductImage remainingImage = ProductImage.builder()
                .id(2L)
                .product(product)
                .tenantId(TENANT_ID)
                .imageUrl("/uploads/tenant-1/products/1/other.jpg")
                .sortOrder(1)
                .primary(false)
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(imageRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(List.of(remainingImage));

        // When
        productImageService.deleteImage(1L, 1L);

        // Then
        verify(imageRepository).delete(image);
        assertTrue(remainingImage.isPrimary());
        verify(imageRepository).save(remainingImage);
        verify(productRepository).save(product);
    }

    @Test
    void deleteImage_success_wasPrimary_noRemainingImages() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(imageRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(List.of());

        // When
        productImageService.deleteImage(1L, 1L);

        // Then
        verify(imageRepository).delete(image);
        assertNull(product.getPrimaryImageUrl());
        verify(productRepository).save(product);
    }

    @Test
    void deleteImage_productNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productImageService.deleteImage(999L, 1L));
    }

    @Test
    void deleteImage_imageNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productImageService.deleteImage(1L, 999L));
    }

    @Test
    void deleteImage_wrongProduct_throwsBusinessException() {
        // Given
        Product otherProduct = Product.builder()
                .id(2L)
                .sku("PRD-002")
                .name("Other Product")
                .tenantId(TENANT_ID)
                .build();

        ProductImage otherImage = ProductImage.builder()
                .id(5L)
                .product(otherProduct)
                .tenantId(TENANT_ID)
                .imageUrl("/uploads/other.jpg")
                .primary(false)
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.findById(5L)).thenReturn(Optional.of(otherImage));

        // When/Then
        assertThrows(BusinessException.class, () -> productImageService.deleteImage(1L, 5L));
    }

    @Test
    void setPrimaryImage_success() {
        // Given
        ProductImage nonPrimaryImage = ProductImage.builder()
                .id(2L)
                .product(product)
                .tenantId(TENANT_ID)
                .imageUrl("/uploads/tenant-1/products/1/second.jpg")
                .sortOrder(1)
                .primary(false)
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.findById(2L)).thenReturn(Optional.of(nonPrimaryImage));
        when(imageRepository.save(any(ProductImage.class))).thenReturn(nonPrimaryImage);

        // When
        ProductImageDto result = productImageService.setPrimaryImage(1L, 2L);

        // Then
        assertNotNull(result);
        verify(imageRepository).clearPrimaryForProduct(1L);
        assertTrue(nonPrimaryImage.isPrimary());
        verify(productRepository).save(product);
    }

    @Test
    void setPrimaryImage_productNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productImageService.setPrimaryImage(999L, 1L));
    }

    @Test
    void setPrimaryImage_imageNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productImageService.setPrimaryImage(1L, 999L));
    }

    @Test
    void setPrimaryImage_wrongProduct_throwsBusinessException() {
        // Given
        Product otherProduct = Product.builder()
                .id(2L)
                .sku("PRD-002")
                .name("Other")
                .tenantId(TENANT_ID)
                .build();

        ProductImage otherImage = ProductImage.builder()
                .id(5L)
                .product(otherProduct)
                .tenantId(TENANT_ID)
                .imageUrl("/uploads/other.jpg")
                .primary(false)
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.findById(5L)).thenReturn(Optional.of(otherImage));

        // When/Then
        assertThrows(BusinessException.class, () -> productImageService.setPrimaryImage(1L, 5L));
    }

    @Test
    void reorderImages_success() {
        // Given
        ProductImage image2 = ProductImage.builder()
                .id(2L)
                .product(product)
                .tenantId(TENANT_ID)
                .imageUrl("/uploads/second.jpg")
                .sortOrder(1)
                .primary(false)
                .active(true)
                .build();

        List<Long> newOrder = List.of(2L, 1L);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.findByProductIdOrderBySortOrder(1L))
                .thenReturn(List.of(image, image2))  // first call for validation
                .thenReturn(List.of(image2, image));  // second call for returning result
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<ProductImageDto> result = productImageService.reorderImages(1L, newOrder);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(imageRepository, times(2)).save(any(ProductImage.class));
    }

    @Test
    void reorderImages_productNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () ->
                productImageService.reorderImages(999L, List.of(1L, 2L)));
    }

    @Test
    void reorderImages_invalidIds_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(imageRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(List.of(image));

        // When/Then
        assertThrows(BusinessException.class, () ->
                productImageService.reorderImages(1L, List.of(1L, 999L)));
    }
}
