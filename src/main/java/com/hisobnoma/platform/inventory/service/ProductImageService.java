package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.ProductImageDto;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductImage;
import com.hisobnoma.platform.inventory.repository.ProductImageRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing product images.
 * Handles image upload, deletion, and ordering.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final SecurityContextHelper securityContextHelper;

    @Value("${app.upload.storage-path:uploads}")
    private String storagePath;

    @Value("${app.upload.base-url:/uploads}")
    private String baseUrl;

    @Value("${app.upload.max-images-per-product:10}")
    private int maxImagesPerProduct;

    /**
     * Upload an image for a product.
     */
    @Transactional
    public ProductImageDto uploadImage(Long productId, MultipartFile file, String altText, String title) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        // Check max images limit
        long imageCount = imageRepository.countByProductId(productId);
        if (imageCount >= maxImagesPerProduct) {
            throw new BusinessException("Maximum number of images (" + maxImagesPerProduct + ") reached for this product");
        }

        // Validate file
        validateImageFile(file);

        // Save file to storage
        String filename = generateUniqueFilename(file.getOriginalFilename());
        String relativePath = saveFile(file, tenantId, productId, filename);
        String imageUrl = baseUrl + "/" + relativePath;

        // Determine if this should be primary
        boolean isPrimary = imageCount == 0;

        // Create image record
        ProductImage image = ProductImage.builder()
                .product(product)
                .tenantId(tenantId)
                .imageUrl(imageUrl)
                .altText(altText)
                .title(title)
                .sortOrder((int) imageCount)
                .primary(isPrimary)
                .active(true)
                .build();

        image = imageRepository.save(image);

        // Update product primary image URL if this is the first image
        if (isPrimary) {
            product.setPrimaryImageUrl(imageUrl);
            productRepository.save(product);
        }

        log.info("Uploaded image for product {}: {}", productId, image.getId());
        return toDto(image);
    }

    /**
     * Delete an image from a product.
     */
    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("ProductImage", imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new BusinessException("Image does not belong to the specified product");
        }

        boolean wasPrimary = image.isPrimary();
        String imageUrl = image.getImageUrl();

        // Delete from database
        imageRepository.delete(image);

        // Delete file from storage
        deleteFile(imageUrl);

        // If this was the primary image, set a new primary
        if (wasPrimary) {
            List<ProductImage> remainingImages = imageRepository.findByProductIdOrderBySortOrder(productId);
            if (!remainingImages.isEmpty()) {
                ProductImage newPrimary = remainingImages.get(0);
                newPrimary.setPrimary(true);
                imageRepository.save(newPrimary);
                product.setPrimaryImageUrl(newPrimary.getImageUrl());
            } else {
                product.setPrimaryImageUrl(null);
            }
            productRepository.save(product);
        }

        log.info("Deleted image {} from product {}", imageId, productId);
    }

    /**
     * Set an image as the primary image for a product.
     */
    @Transactional
    public ProductImageDto setPrimaryImage(Long productId, Long imageId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("ProductImage", imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new BusinessException("Image does not belong to the specified product");
        }

        // Clear primary flag from all images
        imageRepository.clearPrimaryForProduct(productId);

        // Set new primary
        image.setPrimary(true);
        image = imageRepository.save(image);

        // Update product's primary image URL
        product.setPrimaryImageUrl(image.getImageUrl());
        productRepository.save(product);

        log.info("Set image {} as primary for product {}", imageId, productId);
        return toDto(image);
    }

    /**
     * Reorder images for a product.
     */
    @Transactional
    public List<ProductImageDto> reorderImages(Long productId, List<Long> imageIds) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        List<ProductImage> images = imageRepository.findByProductIdOrderBySortOrder(productId);

        // Validate all IDs belong to this product
        List<Long> existingIds = images.stream()
                .map(ProductImage::getId)
                .collect(Collectors.toList());

        if (!existingIds.containsAll(imageIds) || !imageIds.containsAll(existingIds)) {
            throw new BusinessException("Invalid image IDs provided for reordering");
        }

        // Update sort order
        for (int i = 0; i < imageIds.size(); i++) {
            Long imageId = imageIds.get(i);
            ProductImage image = images.stream()
                    .filter(img -> img.getId().equals(imageId))
                    .findFirst()
                    .orElseThrow();
            image.setSortOrder(i);
            imageRepository.save(image);
        }

        log.info("Reordered images for product {}", productId);
        return imageRepository.findByProductIdOrderBySortOrder(productId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all images for a product.
     */
    @Transactional(readOnly = true)
    public List<ProductImageDto> getImages(Long productId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        return imageRepository.findByProductIdOrderBySortOrder(productId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Only image files are allowed");
        }

        // Check file size (10MB max)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("File size exceeds maximum allowed size (10MB)");
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private String saveFile(MultipartFile file, Long tenantId, Long productId, String filename) {
        try {
            String relativePath = String.format("tenant-%d/products/%d/%s",
                    tenantId != null ? tenantId : 0, productId, filename);
            Path filePath = Paths.get(storagePath, relativePath);

            // Create directories if they don't exist
            Files.createDirectories(filePath.getParent());

            // Copy file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return relativePath;
        } catch (IOException e) {
            log.error("Failed to save file: {}", e.getMessage());
            throw new BusinessException("Failed to save image file");
        }
    }

    private void deleteFile(String imageUrl) {
        try {
            if (imageUrl != null && imageUrl.startsWith(baseUrl)) {
                String relativePath = imageUrl.substring(baseUrl.length() + 1);
                Path filePath = Paths.get(storagePath, relativePath);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", e.getMessage());
        }
    }

    private ProductImageDto toDto(ProductImage image) {
        return ProductImageDto.builder()
                .id(image.getId())
                .productId(image.getProduct().getId())
                .imageUrl(image.getImageUrl())
                .thumbnailUrl(image.getThumbnailUrl())
                .altText(image.getAltText())
                .title(image.getTitle())
                .sortOrder(image.getSortOrder())
                .primary(image.isPrimary())
                .active(image.isActive())
                .build();
    }
}
