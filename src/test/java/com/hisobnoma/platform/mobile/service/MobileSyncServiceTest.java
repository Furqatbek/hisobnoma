package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.mobile.dto.MobileSyncDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileSyncServiceTest {

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private MobileSyncService mobileSyncService;

    private Product product;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        product = mock(Product.class);
    }

    // ====== getProductsForSync ======

    @Test
    void getProductsForSync_fullSync_returnsAllActiveProducts() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(product.getId()).thenReturn(1L);
        when(product.getSku()).thenReturn("SKU-001");
        when(product.getBarcode()).thenReturn("1234567890");
        when(product.getName()).thenReturn("Test Product");
        when(product.getCategory()).thenReturn(null);
        when(product.getSellingPrice()).thenReturn(new BigDecimal("15000.00"));
        when(product.getCostPrice()).thenReturn(new BigDecimal("10000.00"));
        when(product.getBaseUom()).thenReturn(null);
        when(product.isTrackInventory()).thenReturn(true);
        when(product.isActive()).thenReturn(true);
        when(product.getUpdatedAt()).thenReturn(Instant.now());
        when(productRepository.findByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(List.of(product));

        // When
        MobileSyncDataDTO result = mobileSyncService.getProductsForSync(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isFullSyncRequired());
        assertEquals("1.0", result.getSyncVersion());
        assertNotNull(result.getProducts());
        assertEquals(1, result.getProducts().size());
        assertEquals("SKU-001", result.getProducts().get(0).getSku());
        verify(productRepository).findByTenantIdAndActiveTrue(TENANT_ID);
    }

    @Test
    void getProductsForSync_incrementalSync_returnsUpdatedProducts() {
        // Given
        Instant lastSync = Instant.now().minusSeconds(3600);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(product.getId()).thenReturn(1L);
        when(product.getSku()).thenReturn("SKU-001");
        when(product.getBarcode()).thenReturn("1234567890");
        when(product.getName()).thenReturn("Test Product");
        when(product.getCategory()).thenReturn(null);
        when(product.getSellingPrice()).thenReturn(new BigDecimal("15000.00"));
        when(product.getCostPrice()).thenReturn(new BigDecimal("10000.00"));
        when(product.getBaseUom()).thenReturn(null);
        when(product.isTrackInventory()).thenReturn(true);
        when(product.isActive()).thenReturn(true);
        when(product.getUpdatedAt()).thenReturn(Instant.now());
        when(productRepository.findByTenantIdAndUpdatedAtAfter(TENANT_ID, lastSync))
                .thenReturn(List.of(product));

        // When
        MobileSyncDataDTO result = mobileSyncService.getProductsForSync(lastSync);

        // Then
        assertNotNull(result);
        assertFalse(result.isFullSyncRequired());
        assertNotNull(result.getProducts());
        assertEquals(1, result.getProducts().size());
        verify(productRepository).findByTenantIdAndUpdatedAtAfter(TENANT_ID, lastSync);
        verify(productRepository, never()).findByTenantIdAndActiveTrue(any());
    }

    // ====== getCustomersForSync ======

    @Test
    void getCustomersForSync_fullSync_returnsAllActiveCustomers() {
        // Given
        var customer = mock(com.hisobnoma.platform.finance.entity.Customer.class);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(customer.getId()).thenReturn(1L);
        when(customer.getCode()).thenReturn("CUST-001");
        when(customer.getName()).thenReturn("Test Customer");
        when(customer.getPhone()).thenReturn("+998901234567");
        when(customer.getEmail()).thenReturn("test@example.com");
        when(customer.getCreditLimit()).thenReturn(new BigDecimal("1000000"));
        when(customer.getCurrentBalance()).thenReturn(new BigDecimal("50000"));
        when(customer.isActive()).thenReturn(true);
        when(customer.getUpdatedAt()).thenReturn(Instant.now());
        when(customerRepository.findByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(List.of(customer));

        // When
        MobileSyncDataDTO result = mobileSyncService.getCustomersForSync(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isFullSyncRequired());
        assertNotNull(result.getCustomers());
        assertEquals(1, result.getCustomers().size());
        assertEquals("CUST-001", result.getCustomers().get(0).getCode());
    }

    @Test
    void getCustomersForSync_incrementalSync_returnsUpdatedCustomers() {
        // Given
        Instant lastSync = Instant.now().minusSeconds(3600);
        var customer = mock(com.hisobnoma.platform.finance.entity.Customer.class);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(customer.getId()).thenReturn(1L);
        when(customer.getCode()).thenReturn("CUST-001");
        when(customer.getName()).thenReturn("Test Customer");
        when(customer.getPhone()).thenReturn("+998901234567");
        when(customer.getEmail()).thenReturn("test@example.com");
        when(customer.getCreditLimit()).thenReturn(new BigDecimal("1000000"));
        when(customer.getCurrentBalance()).thenReturn(new BigDecimal("50000"));
        when(customer.isActive()).thenReturn(true);
        when(customer.getUpdatedAt()).thenReturn(Instant.now());
        when(customerRepository.findByTenantIdAndUpdatedAtAfter(TENANT_ID, lastSync))
                .thenReturn(List.of(customer));

        // When
        MobileSyncDataDTO result = mobileSyncService.getCustomersForSync(lastSync);

        // Then
        assertNotNull(result);
        assertFalse(result.isFullSyncRequired());
        assertNotNull(result.getCustomers());
        assertEquals(1, result.getCustomers().size());
        verify(customerRepository).findByTenantIdAndUpdatedAtAfter(TENANT_ID, lastSync);
    }

    // ====== getCategoriesForSync ======

    @Test
    void getCategoriesForSync_returnsCategorySyncData() {
        // Given
        var category = mock(Category.class);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(category.getId()).thenReturn(1L);
        when(category.getName()).thenReturn("Electronics");
        when(category.getParent()).thenReturn(null);
        when(category.getSortOrder()).thenReturn(1);
        when(category.isActive()).thenReturn(true);
        when(category.getUpdatedAt()).thenReturn(Instant.now());
        when(categoryRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(category));

        // When
        MobileSyncDataDTO result = mobileSyncService.getCategoriesForSync();

        // Then
        assertNotNull(result);
        assertEquals("1.0", result.getSyncVersion());
        assertNotNull(result.getCategories());
        assertEquals(1, result.getCategories().size());
        assertEquals("Electronics", result.getCategories().get(0).getName());
        assertNull(result.getCategories().get(0).getParentId());
    }

    // ====== getLastUpdatedTimestamp ======

    @Test
    void getLastUpdatedTimestamp_returnsLatestTimestamp() {
        // Given
        Instant productUpdate = Instant.parse("2025-01-15T10:00:00Z");
        Instant customerUpdate = Instant.parse("2025-01-15T12:00:00Z");
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findMaxUpdatedAt(TENANT_ID)).thenReturn(productUpdate);
        when(customerRepository.findMaxUpdatedAt(TENANT_ID)).thenReturn(customerUpdate);

        // When
        Instant result = mobileSyncService.getLastUpdatedTimestamp();

        // Then
        assertEquals(customerUpdate, result);
    }

    @Test
    void getLastUpdatedTimestamp_nullProductUpdate_returnsCustomerUpdate() {
        // Given
        Instant customerUpdate = Instant.parse("2025-01-15T12:00:00Z");
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findMaxUpdatedAt(TENANT_ID)).thenReturn(null);
        when(customerRepository.findMaxUpdatedAt(TENANT_ID)).thenReturn(customerUpdate);

        // When
        Instant result = mobileSyncService.getLastUpdatedTimestamp();

        // Then
        assertEquals(customerUpdate, result);
    }

    @Test
    void getLastUpdatedTimestamp_nullCustomerUpdate_returnsProductUpdate() {
        // Given
        Instant productUpdate = Instant.parse("2025-01-15T10:00:00Z");
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findMaxUpdatedAt(TENANT_ID)).thenReturn(productUpdate);
        when(customerRepository.findMaxUpdatedAt(TENANT_ID)).thenReturn(null);

        // When
        Instant result = mobileSyncService.getLastUpdatedTimestamp();

        // Then
        assertEquals(productUpdate, result);
    }

    @Test
    void getLastUpdatedTimestamp_bothNull_returnsNull() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findMaxUpdatedAt(TENANT_ID)).thenReturn(null);
        when(customerRepository.findMaxUpdatedAt(TENANT_ID)).thenReturn(null);

        // When
        Instant result = mobileSyncService.getLastUpdatedTimestamp();

        // Then
        assertNull(result);
    }
}
