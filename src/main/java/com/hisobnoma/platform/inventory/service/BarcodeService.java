package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Service for generating and validating barcodes.
 * Supports EAN-13, UPC-A, and internal barcodes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate an internal barcode (not standard format).
     */
    public String generateInternalBarcode(String prefix) {
        String prefixPart = prefix != null ? prefix : "INT";
        String randomPart = String.format("%09d", RANDOM.nextInt(1_000_000_000));
        String barcode = prefixPart + randomPart;

        while (barcodeExists(barcode)) {
            randomPart = String.format("%09d", RANDOM.nextInt(1_000_000_000));
            barcode = prefixPart + randomPart;
        }

        return barcode;
    }

    /**
     * Generate an EAN-13 barcode with check digit.
     * @param countryCode 3-digit country code (e.g., "860" for Uzbekistan)
     * @param companyCode 4-5 digit company code
     */
    public String generateEan13(String countryCode, String companyCode) {
        if (countryCode == null || countryCode.length() != 3) {
            countryCode = "200"; // Internal use prefix
        }
        if (companyCode == null || companyCode.length() < 4) {
            companyCode = String.format("%05d", RANDOM.nextInt(100000));
        }

        String productCode = String.format("%04d", RANDOM.nextInt(10000));
        String base = countryCode + companyCode.substring(0, 5) + productCode;

        if (base.length() != 12) {
            base = String.format("%012d", Long.parseLong(base.replaceAll("[^0-9]", "")));
            if (base.length() > 12) {
                base = base.substring(0, 12);
            }
        }

        String barcode = base + calculateEan13CheckDigit(base);

        while (barcodeExists(barcode)) {
            productCode = String.format("%04d", RANDOM.nextInt(10000));
            base = countryCode + companyCode.substring(0, 5) + productCode;
            barcode = base + calculateEan13CheckDigit(base);
        }

        return barcode;
    }

    /**
     * Validate EAN-13 barcode format and check digit.
     */
    public boolean isValidEan13(String barcode) {
        if (barcode == null || barcode.length() != 13 || !barcode.matches("\\d+")) {
            return false;
        }

        String base = barcode.substring(0, 12);
        char expectedCheck = calculateEan13CheckDigit(base);
        return barcode.charAt(12) == expectedCheck;
    }

    /**
     * Validate UPC-A barcode (12 digits).
     */
    public boolean isValidUpcA(String barcode) {
        if (barcode == null || barcode.length() != 12 || !barcode.matches("\\d+")) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 11; i++) {
            int digit = Character.getNumericValue(barcode.charAt(i));
            sum += (i % 2 == 0) ? digit * 3 : digit;
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        return Character.getNumericValue(barcode.charAt(11)) == checkDigit;
    }

    /**
     * Check if barcode exists.
     */
    public boolean barcodeExists(String barcode) {
        return productRepository.findByBarcode(barcode).isPresent() ||
               variantRepository.findByBarcode(barcode).isPresent();
    }

    /**
     * Validate any barcode format.
     */
    public boolean isValidBarcode(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            return false;
        }

        // Check common formats
        if (barcode.length() == 13) {
            return isValidEan13(barcode);
        } else if (barcode.length() == 12) {
            return isValidUpcA(barcode);
        } else if (barcode.length() >= 8 && barcode.length() <= 50) {
            // Accept internal barcodes
            return barcode.matches("^[A-Za-z0-9\\-]+$");
        }

        return false;
    }

    /**
     * Get barcode type.
     */
    public String getBarcodeType(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            return "UNKNOWN";
        }

        if (barcode.length() == 13 && barcode.matches("\\d+")) {
            return "EAN-13";
        } else if (barcode.length() == 12 && barcode.matches("\\d+")) {
            return "UPC-A";
        } else if (barcode.length() == 8 && barcode.matches("\\d+")) {
            return "EAN-8";
        } else {
            return "INTERNAL";
        }
    }

    private char calculateEan13CheckDigit(String base) {
        if (base.length() != 12) {
            throw new IllegalArgumentException("Base must be 12 digits");
        }

        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(base.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        return Character.forDigit(checkDigit, 10);
    }
}
