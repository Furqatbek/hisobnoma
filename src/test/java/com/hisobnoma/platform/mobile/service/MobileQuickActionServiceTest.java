package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.Stock;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.mobile.dto.ProductLookupDto;
import com.hisobnoma.platform.mobile.dto.QuickSaleRequest;
import com.hisobnoma.platform.mobile.dto.QuickStockCountRequest;
import com.hisobnoma.platform.mobile.entity.MobileQuickSaleIdempotency;
import com.hisobnoma.platform.mobile.repository.MobileQuickSaleIdempotencyRepository;
import com.hisobnoma.platform.pos.dto.POSTransactionDto;
import com.hisobnoma.platform.pos.service.POSPaymentService;
import com.hisobnoma.platform.pos.service.POSTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileQuickActionServiceTest {

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private POSTransactionService posTransactionService;

    @Mock
    private POSPaymentService posPaymentService;

    @Mock
    private MobileQuickSaleIdempotencyRepository idempotencyRepository;

    @InjectMocks
    private MobileQuickActionService mobileQuickActionService;

    private Product product;
    private Stock stock;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        product = mock(Product.class);
        stock = mock(Stock.class);
    }

    // ====== lookupByBarcode ======

    @Test
    void lookupByBarcode_found_returnsProductDetails() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByBarcodeAndTenantId("1234567890", TENANT_ID))
                .thenReturn(Optional.of(product));
        when(product.getId()).thenReturn(100L);
        when(product.getSku()).thenReturn("SKU-001");
        when(product.getBarcode()).thenReturn("1234567890");
        when(product.getName()).thenReturn("Test Product");
        when(product.getSellingPrice()).thenReturn(new BigDecimal("15000.00"));
        when(product.getCostPrice()).thenReturn(new BigDecimal("10000.00"));
        when(product.getCategory()).thenReturn(null);
        when(product.getBaseUom()).thenReturn(null);
        when(product.isTrackInventory()).thenReturn(true);
        when(stockRepository.findByProductIdAndTenantId(100L, TENANT_ID)).thenReturn(List.of());

        // When
        Map<String, Object> result = mobileQuickActionService.lookupByBarcode("1234567890");

        // Then
        assertNotNull(result);
        assertEquals(100L, result.get("productId"));
        assertEquals("SKU-001", result.get("sku"));
        assertEquals("1234567890", result.get("barcode"));
        assertEquals("Test Product", result.get("name"));
        assertEquals(new BigDecimal("15000.00"), result.get("sellingPrice"));
        assertEquals(BigDecimal.ZERO, result.get("totalStock"));
    }

    @Test
    void lookupByBarcode_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByBarcodeAndTenantId("9999999999", TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> mobileQuickActionService.lookupByBarcode("9999999999"));
    }

    // ====== performQuickStockCount ======

    @Test
    void performQuickStockCount_success() {
        // Given
        QuickStockCountRequest request = QuickStockCountRequest.builder()
                .productId(100L)
                .locationId(1L)
                .countedQuantity(new BigDecimal("45"))
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(product));
        when(product.getId()).thenReturn(100L);
        when(product.getName()).thenReturn("Test Product");
        when(product.getSku()).thenReturn("SKU-001");
        when(stockRepository.findByProductIdAndLocationIdAndTenantId(100L, 1L, TENANT_ID))
                .thenReturn(Optional.of(stock));
        when(stock.getQuantityOnHand()).thenReturn(new BigDecimal("50"));

        // When
        Map<String, Object> result = mobileQuickActionService.performQuickStockCount(request);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.get("productId"));
        assertEquals("Test Product", result.get("productName"));
        assertEquals(new BigDecimal("50"), result.get("systemQuantity"));
        assertEquals(new BigDecimal("45"), result.get("countedQuantity"));
        assertEquals(new BigDecimal("-5"), result.get("variance"));
    }

    @Test
    void performQuickStockCount_productNotFound_throwsNotFoundException() {
        // Given
        QuickStockCountRequest request = QuickStockCountRequest.builder()
                .productId(999L)
                .locationId(1L)
                .countedQuantity(new BigDecimal("10"))
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> mobileQuickActionService.performQuickStockCount(request));
    }

    @Test
    void performQuickStockCount_stockNotFound_throwsNotFoundException() {
        // Given
        QuickStockCountRequest request = QuickStockCountRequest.builder()
                .productId(100L)
                .locationId(999L)
                .countedQuantity(new BigDecimal("10"))
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndLocationIdAndTenantId(100L, 999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> mobileQuickActionService.performQuickStockCount(request));
    }

    // ====== performQuickSale ======

    @Test
    void performQuickSale_emptyItems_throwsValidationException() {
        // Given
        QuickSaleRequest request = QuickSaleRequest.builder()
                .terminalId(1L)
                .items(List.of())
                .paymentType("CASH")
                .build();
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);

        // When/Then
        assertThrows(ValidationException.class, () -> mobileQuickActionService.performQuickSale(request));
    }

    @Test
    void performQuickSale_nullItems_throwsValidationException() {
        // Given
        QuickSaleRequest request = QuickSaleRequest.builder()
                .terminalId(1L)
                .items(null)
                .paymentType("CASH")
                .build();
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);

        // When/Then
        assertThrows(ValidationException.class, () -> mobileQuickActionService.performQuickSale(request));
    }

    @Test
    void performQuickSale_productNotFound_throwsNotFoundException() {
        // Given
        QuickSaleRequest.QuickSaleItem item = QuickSaleRequest.QuickSaleItem.builder()
                .productId(999L)
                .quantity(new BigDecimal("1"))
                .build();
        QuickSaleRequest request = QuickSaleRequest.builder()
                .terminalId(1L)
                .items(List.of(item))
                .paymentType("CASH")
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> mobileQuickActionService.performQuickSale(request));
    }

    @Test
    void performQuickSale_success() {
        // Given
        QuickSaleRequest.QuickSaleItem item = QuickSaleRequest.QuickSaleItem.builder()
                .productId(100L)
                .quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("15000.00"))
                .build();
        QuickSaleRequest request = QuickSaleRequest.builder()
                .terminalId(1L)
                .items(List.of(item))
                .paymentType("CASH")
                .tenderedAmount(new BigDecimal("30000.00"))
                .build();

        POSTransactionDto transactionDto = POSTransactionDto.builder()
                .id(1L)
                .transactionNumber("TXN-001")
                .totalAmount(new BigDecimal("30000.00"))
                .build();

        POSTransactionDto completedDto = POSTransactionDto.builder()
                .id(1L)
                .transactionNumber("TXN-001")
                .totalAmount(new BigDecimal("30000.00"))
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(product));
        when(posTransactionService.createTransaction(any())).thenReturn(transactionDto);
        when(posTransactionService.findById(1L)).thenReturn(transactionDto);
        when(posTransactionService.completeTransaction(1L)).thenReturn(completedDto);

        // When
        POSTransactionDto result = mobileQuickActionService.performQuickSale(request);

        // Then
        assertNotNull(result);
        assertEquals("TXN-001", result.getTransactionNumber());
        verify(posTransactionService).addLine(eq(1L), any());
        verify(posPaymentService).addPayment(eq(1L), any());
        verify(posTransactionService).completeTransaction(1L);
    }

    // ====== searchCustomers ======

    @Test
    void searchCustomers_returnsPagedResults() {
        // Given
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(1L);
        when(customer.getCode()).thenReturn("CUST-001");
        when(customer.getName()).thenReturn("Test Customer");
        when(customer.getPhone()).thenReturn("+998901234567");
        when(customer.getEmail()).thenReturn("test@example.com");
        when(customer.getCreditLimit()).thenReturn(new BigDecimal("1000000"));
        when(customer.getCurrentBalance()).thenReturn(new BigDecimal("50000"));

        Page<Customer> page = new PageImpl<>(List.of(customer), PageRequest.of(0, 10), 1);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.searchByNameOrCodeOrPhone(eq(TENANT_ID), eq("Test"), any()))
                .thenReturn(page);

        // When
        Page<Map<String, Object>> result = mobileQuickActionService.searchCustomers("Test", 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Customer", result.getContent().get(0).get("name"));
    }

    // ====== searchProducts ======

    @Test
    void searchProducts_returnsEnrichedLookupWithStockPriceFloorAndUom() {
        // Given — full cart-relevant field set
        when(product.getId()).thenReturn(100L);
        when(product.getSku()).thenReturn("SKU-001");
        when(product.getBarcode()).thenReturn("1234567890");
        when(product.getName()).thenReturn("Test Product");
        when(product.getSellingPrice()).thenReturn(new BigDecimal("15000.00"));
        when(product.getMinSellingPrice()).thenReturn(new BigDecimal("12000.00"));
        when(product.isActive()).thenReturn(true);
        when(product.isTrackInventory()).thenReturn(true);
        when(product.getCategory()).thenReturn(null);
        when(product.getBaseUom()).thenReturn(UnitOfMeasure.builder().code("PCS").name("Pieces").build());

        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.searchByNameOrSkuOrBarcode(eq(TENANT_ID), eq("Test"), any()))
                .thenReturn(page);
        when(stockRepository.getTotalQuantitiesByTenant(TENANT_ID))
                .thenReturn(List.<Object[]>of(new Object[]{100L, new BigDecimal("42")}));

        // When
        Page<ProductLookupDto> result = mobileQuickActionService.searchProducts("Test", 0, 10);

        // Then
        assertEquals(1, result.getContent().size());
        ProductLookupDto dto = result.getContent().get(0);
        assertEquals(100L, dto.getId());
        assertEquals("Test Product", dto.getName());
        assertEquals("SKU-001", dto.getSku());
        assertEquals("1234567890", dto.getBarcode());
        assertEquals(0, new BigDecimal("15000.00").compareTo(dto.getSellingPrice()));
        assertEquals(0, new BigDecimal("12000.00").compareTo(dto.getMinSellingPrice())); // price floor, missing before
        assertEquals(0, new BigDecimal("42").compareTo(dto.getStockQuantity()));
        assertTrue(dto.isActive());
        assertTrue(dto.isTrackInventory());
        assertEquals("Pieces", dto.getBaseUomName());
        assertEquals("PCS", dto.getBaseUomCode());
    }

    // ====== idempotency ======

    @Test
    void performQuickSale_knownClientRequestId_replaysOriginalWithoutNewSale() {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        MobileQuickSaleIdempotency record = MobileQuickSaleIdempotency.builder()
                .clientRequestId("uuid-1").transactionId(500L).tenantId(TENANT_ID).build();
        when(idempotencyRepository.findByTenantIdAndClientRequestId(TENANT_ID, "uuid-1"))
                .thenReturn(Optional.of(record));
        POSTransactionDto original = new POSTransactionDto();
        original.setId(500L);
        when(posTransactionService.findById(500L)).thenReturn(original);

        POSTransactionDto result = mobileQuickActionService.performQuickSale(
                QuickSaleRequest.builder().terminalId(1L).paymentType("CASH").clientRequestId("uuid-1").build());

        assertEquals(500L, result.getId());
        verify(posTransactionService).findById(500L);
        verify(posTransactionService, never()).createTransaction(any());
        verify(idempotencyRepository, never()).save(any());
    }
}
