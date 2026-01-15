package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.mobile.dto.MobileSyncDataDTO;
import com.hisobnoma.platform.pos.repository.PriceListItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for mobile offline sync data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MobileSyncService {

    private final SecurityContextHelper securityContextHelper;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;
    private final PriceListItemRepository priceListItemRepository;

    private static final String SYNC_VERSION = "1.0";

    /**
     * Get product catalog for offline sync.
     */
    public MobileSyncDataDTO getProductsForSync(Instant lastSyncAt) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        List<MobileSyncDataDTO.ProductSync> products;
        if (lastSyncAt == null) {
            // Full sync - get all active products
            products = productRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                    .map(p -> MobileSyncDataDTO.ProductSync.builder()
                            .id(p.getId())
                            .sku(p.getSku())
                            .barcode(p.getBarcode())
                            .name(p.getName())
                            .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                            .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                            .sellingPrice(p.getSellingPrice())
                            .costPrice(p.getCostPrice())
                            .unitOfMeasure(p.getBaseUom() != null ? p.getBaseUom().getName() : null)
                            .trackInventory(p.isTrackInventory())
                            .active(p.isActive())
                            .updatedAt(p.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());
        } else {
            // Incremental sync - get products updated since lastSyncAt
            products = productRepository.findByTenantIdAndUpdatedAtAfter(tenantId, lastSyncAt).stream()
                    .map(p -> MobileSyncDataDTO.ProductSync.builder()
                            .id(p.getId())
                            .sku(p.getSku())
                            .barcode(p.getBarcode())
                            .name(p.getName())
                            .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                            .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                            .sellingPrice(p.getSellingPrice())
                            .costPrice(p.getCostPrice())
                            .unitOfMeasure(p.getBaseUom() != null ? p.getBaseUom().getName() : null)
                            .trackInventory(p.isTrackInventory())
                            .active(p.isActive())
                            .updatedAt(p.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());
        }

        return MobileSyncDataDTO.builder()
                .lastSyncAt(Instant.now())
                .syncVersion(SYNC_VERSION)
                .fullSyncRequired(lastSyncAt == null)
                .products(products)
                .build();
    }

    /**
     * Get customer list for offline sync.
     */
    public MobileSyncDataDTO getCustomersForSync(Instant lastSyncAt) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        List<MobileSyncDataDTO.CustomerSync> customers;
        if (lastSyncAt == null) {
            customers = customerRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                    .map(c -> MobileSyncDataDTO.CustomerSync.builder()
                            .id(c.getId())
                            .code(c.getCode())
                            .name(c.getName())
                            .phone(c.getPhone())
                            .email(c.getEmail())
                            .priceListId(c.getPriceList() != null ? c.getPriceList().getId() : null)
                            .creditLimit(c.getCreditLimit())
                            .currentBalance(c.getCurrentBalance())
                            .active(c.isActive())
                            .updatedAt(c.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());
        } else {
            customers = customerRepository.findByTenantIdAndUpdatedAtAfter(tenantId, lastSyncAt).stream()
                    .map(c -> MobileSyncDataDTO.CustomerSync.builder()
                            .id(c.getId())
                            .code(c.getCode())
                            .name(c.getName())
                            .phone(c.getPhone())
                            .email(c.getEmail())
                            .priceListId(c.getPriceList() != null ? c.getPriceList().getId() : null)
                            .creditLimit(c.getCreditLimit())
                            .currentBalance(c.getCurrentBalance())
                            .active(c.isActive())
                            .updatedAt(c.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());
        }

        return MobileSyncDataDTO.builder()
                .lastSyncAt(Instant.now())
                .syncVersion(SYNC_VERSION)
                .fullSyncRequired(lastSyncAt == null)
                .customers(customers)
                .build();
    }

    /**
     * Get categories for offline sync.
     */
    public MobileSyncDataDTO getCategoriesForSync() {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        List<MobileSyncDataDTO.CategorySync> categories = categoryRepository.findAllActiveByTenantId(tenantId).stream()
                .map(c -> MobileSyncDataDTO.CategorySync.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .parentId(c.getParent() != null ? c.getParent().getId() : null)
                        .sortOrder(c.getSortOrder())
                        .active(c.isActive())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return MobileSyncDataDTO.builder()
                .lastSyncAt(Instant.now())
                .syncVersion(SYNC_VERSION)
                .categories(categories)
                .build();
    }

    /**
     * Check when data was last updated for sync decision.
     */
    public Instant getLastUpdatedTimestamp() {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        Instant productLastUpdate = productRepository.findMaxUpdatedAt(tenantId);
        Instant customerLastUpdate = customerRepository.findMaxUpdatedAt(tenantId);

        if (productLastUpdate == null) return customerLastUpdate;
        if (customerLastUpdate == null) return productLastUpdate;
        return productLastUpdate.isAfter(customerLastUpdate) ? productLastUpdate : customerLastUpdate;
    }
}
