package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for generating unique SKUs (Stock Keeping Units).
 * Supports multiple patterns and ensures uniqueness.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuGeneratorService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 100000);

    /**
     * Generate a unique SKU with default pattern (PREFIX-YYYYMMDD-XXXXX).
     */
    public String generateSku(String prefix) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomPart = generateRandomAlphanumeric(5);
        String sku = String.format("%s-%s-%s",
                prefix != null ? prefix.toUpperCase() : "PRD",
                datePart,
                randomPart);

        // Ensure uniqueness
        while (skuExists(sku)) {
            randomPart = generateRandomAlphanumeric(5);
            sku = String.format("%s-%s-%s",
                    prefix != null ? prefix.toUpperCase() : "PRD",
                    datePart,
                    randomPart);
        }

        return sku;
    }

    /**
     * Generate a unique SKU with sequential numbering.
     */
    public String generateSequentialSku(String prefix) {
        long sequence = SEQUENCE.incrementAndGet();
        String sku = String.format("%s-%06d",
                prefix != null ? prefix.toUpperCase() : "PRD",
                sequence);

        while (skuExists(sku)) {
            sequence = SEQUENCE.incrementAndGet();
            sku = String.format("%s-%06d",
                    prefix != null ? prefix.toUpperCase() : "PRD",
                    sequence);
        }

        return sku;
    }

    /**
     * Generate a unique variant SKU based on parent SKU.
     */
    public String generateVariantSku(String parentSku, String... options) {
        StringBuilder variantPart = new StringBuilder();
        for (String option : options) {
            if (option != null && !option.isEmpty()) {
                variantPart.append("-").append(abbreviate(option));
            }
        }

        String sku = parentSku + variantPart;

        // Ensure uniqueness
        int counter = 1;
        String originalSku = sku;
        while (skuExists(sku) || variantSkuExists(sku)) {
            sku = originalSku + "-" + counter++;
        }

        return sku;
    }

    /**
     * Generate SKU from product name.
     */
    public String generateSkuFromName(String name) {
        if (name == null || name.isEmpty()) {
            return generateSku(null);
        }

        String normalized = name.toUpperCase()
                .replaceAll("[^A-Z0-9\\s]", "")
                .trim();

        String[] words = normalized.split("\\s+");
        StringBuilder skuPart = new StringBuilder();

        for (int i = 0; i < Math.min(3, words.length); i++) {
            String word = words[i];
            skuPart.append(word.length() >= 3 ? word.substring(0, 3) : word);
        }

        if (skuPart.length() < 3) {
            skuPart.append(generateRandomAlphanumeric(3 - skuPart.length()));
        }

        String sku = skuPart + "-" + generateRandomAlphanumeric(4);

        while (skuExists(sku)) {
            sku = skuPart + "-" + generateRandomAlphanumeric(4);
        }

        return sku;
    }

    /**
     * Check if a SKU already exists.
     */
    public boolean skuExists(String sku) {
        return productRepository.findBySku(sku).isPresent();
    }

    /**
     * Check if a variant SKU already exists.
     */
    public boolean variantSkuExists(String sku) {
        return variantRepository.findBySku(sku).isPresent();
    }

    /**
     * Validate SKU format.
     */
    public boolean isValidSku(String sku) {
        if (sku == null || sku.isEmpty()) {
            return false;
        }
        // SKU should be alphanumeric with dashes, 3-50 characters
        return sku.matches("^[A-Za-z0-9\\-]{3,50}$");
    }

    private String generateRandomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    private String abbreviate(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String upper = text.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return upper.length() >= 3 ? upper.substring(0, 3) : upper;
    }
}
