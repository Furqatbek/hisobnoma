package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductVariantRepository;
import com.hisobnoma.platform.pos.dto.PriceCalculationRequest;
import com.hisobnoma.platform.pos.dto.PriceCalculationResult;
import com.hisobnoma.platform.pos.entity.PriceList;
import com.hisobnoma.platform.pos.entity.PriceListItem;
import com.hisobnoma.platform.pos.enums.PriceListType;
import com.hisobnoma.platform.pos.repository.CustomerPriceListRepository;
import com.hisobnoma.platform.pos.repository.PriceListItemRepository;
import com.hisobnoma.platform.pos.repository.PriceListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PriceListRepository priceListRepository;
    @Mock
    private PriceListItemRepository priceListItemRepository;
    @Mock
    private CustomerPriceListRepository customerPriceListRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private PromotionService promotionService;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private PricingService pricingService;

    private Product product;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .sku("PROD-001")
                .sellingPrice(new BigDecimal("50000"))
                .costPrice(new BigDecimal("30000"))
                .build();
    }

    // ==================== calculatePrices ====================

    @Test
    void calculatePrices_basePrice_returned() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(priceListRepository.findEffectivePriceLists(eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        PriceCalculationRequest.PriceCalculationItem item = PriceCalculationRequest.PriceCalculationItem.builder()
                .productId(1L)
                .quantity(BigDecimal.ONE)
                .build();
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .items(List.of(item))
                .applyPromotions(false)
                .build();

        // When
        PriceCalculationResult result = pricingService.calculatePrices(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(new BigDecimal("50000"), result.getItems().get(0).getBasePrice());
    }

    @Test
    void calculatePrices_withPriceListOverride() {
        // Given
        PriceList priceList = PriceList.builder()
                .id(1L)
                .code("VIP")
                .name("VIP Prices")
                .type(PriceListType.STANDARD)
                .priority(10)
                .active(true)
                .items(new ArrayList<>())
                .customerAssignments(new ArrayList<>())
                .build();

        PriceListItem priceListItem = PriceListItem.builder()
                .id(1L)
                .priceList(priceList)
                .product(product)
                .price(new BigDecimal("45000"))
                .minQuantity(BigDecimal.ONE)
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(priceListRepository.findEffectivePriceLists(eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(List.of(priceList));
        when(priceListItemRepository.findEffectiveItems(eq(1L), eq(1L), isNull(), any(), any(LocalDate.class)))
                .thenReturn(List.of(priceListItem));

        PriceCalculationRequest.PriceCalculationItem item = PriceCalculationRequest.PriceCalculationItem.builder()
                .productId(1L)
                .quantity(BigDecimal.ONE)
                .build();
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .items(List.of(item))
                .applyPromotions(false)
                .build();

        // When
        PriceCalculationResult result = pricingService.calculatePrices(request);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("45000"), result.getItems().get(0).getUnitPrice());
    }

    @Test
    void calculatePrices_productNotFound_returnsZeroPrice() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());
        when(priceListRepository.findEffectivePriceLists(eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        PriceCalculationRequest.PriceCalculationItem item = PriceCalculationRequest.PriceCalculationItem.builder()
                .productId(999L)
                .quantity(BigDecimal.ONE)
                .build();
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .items(List.of(item))
                .applyPromotions(false)
                .build();

        // When
        PriceCalculationResult result = pricingService.calculatePrices(request);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getItems().get(0).getUnitPrice());
    }

    // ==================== getProductPrice ====================

    @Test
    void getProductPrice_returnsBasePrice_whenNoPriceList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(priceListRepository.findEffectivePriceLists(eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        BigDecimal result = pricingService.getProductPrice(1L, null, BigDecimal.ONE, null, null);

        // Then
        assertEquals(new BigDecimal("50000"), result);
    }

    @Test
    void getProductPrice_productNotFound_returnsZero() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When
        BigDecimal result = pricingService.getProductPrice(999L, null, BigDecimal.ONE, null, null);

        // Then
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculatePrices_overridePriceUsed_whenProvided() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(priceListRepository.findEffectivePriceLists(eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        PriceCalculationRequest.PriceCalculationItem item = PriceCalculationRequest.PriceCalculationItem.builder()
                .productId(1L)
                .quantity(new BigDecimal("2"))
                .overridePrice(new BigDecimal("40000"))
                .build();
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .items(List.of(item))
                .applyPromotions(false)
                .build();

        // When
        PriceCalculationResult result = pricingService.calculatePrices(request);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("40000"), result.getItems().get(0).getUnitPrice());
        assertEquals("MANUAL", result.getItems().get(0).getPriceListCode());
    }

    @Test
    void calculatePrices_grandTotalNeverNegative() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(priceListRepository.findEffectivePriceLists(eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        PriceCalculationRequest.PriceCalculationItem item = PriceCalculationRequest.PriceCalculationItem.builder()
                .productId(1L)
                .quantity(BigDecimal.ONE)
                .build();
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .items(List.of(item))
                .applyPromotions(false)
                .build();

        // When
        PriceCalculationResult result = pricingService.calculatePrices(request);

        // Then
        assertTrue(result.getGrandTotal().compareTo(BigDecimal.ZERO) >= 0);
    }
}
