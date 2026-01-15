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
import com.hisobnoma.platform.mobile.dto.QuickSaleRequest;
import com.hisobnoma.platform.mobile.dto.QuickStockCountRequest;
import com.hisobnoma.platform.pos.dto.POSTransactionDTO;
import com.hisobnoma.platform.pos.entity.POSTransaction;
import com.hisobnoma.platform.pos.service.POSTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for mobile quick actions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileQuickActionService {

    private final SecurityContextHelper securityContextHelper;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final CustomerRepository customerRepository;
    private final POSTransactionService posTransactionService;

    /**
     * Lookup product by barcode.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> lookupByBarcode(String barcode) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        Product product = productRepository.findByBarcodeAndTenantId(barcode, tenantId)
                .orElseThrow(() -> new NotFoundException("Product not found with barcode: " + barcode));

        List<Stock> stockLevels = stockRepository.findByProductIdAndTenantId(product.getId(), tenantId);

        BigDecimal totalStock = stockLevels.stream()
                .map(Stock::getQuantityAvailable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", product.getId());
        result.put("sku", product.getSku());
        result.put("barcode", product.getBarcode());
        result.put("name", product.getName());
        result.put("sellingPrice", product.getSellingPrice());
        result.put("costPrice", product.getCostPrice());
        result.put("totalStock", totalStock);
        result.put("category", product.getCategory() != null ? product.getCategory().getName() : null);
        result.put("uom", product.getBaseUom() != null ? product.getBaseUom().getName() : null);
        result.put("trackInventory", product.isTrackInventory());

        // Add stock by location
        List<Map<String, Object>> stockByLocation = stockLevels.stream()
                .map(s -> {
                    Map<String, Object> locationStock = new HashMap<>();
                    locationStock.put("locationId", s.getLocation().getId());
                    locationStock.put("locationName", s.getLocation().getName());
                    locationStock.put("quantityOnHand", s.getQuantityOnHand());
                    locationStock.put("quantityReserved", s.getQuantityReserved());
                    locationStock.put("quantityAvailable", s.getQuantityAvailable());
                    return locationStock;
                })
                .toList();
        result.put("stockByLocation", stockByLocation);

        return result;
    }

    /**
     * Perform quick stock count.
     */
    @Transactional
    public Map<String, Object> performQuickStockCount(QuickStockCountRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        Product product = productRepository.findByIdAndTenantId(request.getProductId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        Stock stock = stockRepository.findByProductIdAndLocationIdAndTenantId(
                        request.getProductId(), request.getLocationId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Stock record not found for this location"));

        BigDecimal systemQuantity = stock.getQuantityOnHand();
        BigDecimal countedQuantity = request.getCountedQuantity();
        BigDecimal variance = countedQuantity.subtract(systemQuantity);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", product.getId());
        result.put("productName", product.getName());
        result.put("sku", product.getSku());
        result.put("locationId", request.getLocationId());
        result.put("systemQuantity", systemQuantity);
        result.put("countedQuantity", countedQuantity);
        result.put("variance", variance);
        result.put("variancePercent", systemQuantity.compareTo(BigDecimal.ZERO) != 0
                ? variance.divide(systemQuantity, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO);

        // Note: This is a quick count - actual adjustment would require approval workflow
        // In a real implementation, you might create a pending count record here
        log.info("Quick stock count for product {} at location {}: system={}, counted={}, variance={}",
                product.getSku(), request.getLocationId(), systemQuantity, countedQuantity, variance);

        return result;
    }

    /**
     * Perform quick sale (simplified POS transaction).
     */
    @Transactional
    public POSTransactionDTO performQuickSale(QuickSaleRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        // Validate items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidationException("At least one item is required for a sale");
        }

        // Validate products exist
        for (QuickSaleRequest.QuickSaleItem item : request.getItems()) {
            productRepository.findByIdAndTenantId(item.getProductId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Product not found: " + item.getProductId()));
        }

        // Create transaction using existing POS service
        // This delegates to the full POS transaction flow
        POSTransactionDTO transaction = posTransactionService.createTransaction(
                request.getTerminalId(),
                POSTransaction.TransactionType.SALE,
                request.getCustomerId(),
                request.getNotes()
        );

        // Add items
        for (QuickSaleRequest.QuickSaleItem item : request.getItems()) {
            posTransactionService.addLineItem(
                    transaction.getId(),
                    item.getProductId(),
                    item.getVariantId(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getDiscountAmount(),
                    null,
                    null,
                    null
            );
        }

        // Add payment
        POSTransaction.PaymentType paymentType;
        try {
            paymentType = POSTransaction.PaymentType.valueOf(request.getPaymentType().toUpperCase());
        } catch (IllegalArgumentException e) {
            paymentType = POSTransaction.PaymentType.CASH;
        }

        // Get updated transaction for total
        transaction = posTransactionService.getTransaction(transaction.getId());

        posTransactionService.addPayment(
                transaction.getId(),
                paymentType,
                transaction.getTotalAmount(),
                request.getTenderedAmount(),
                null, null, null, null, null, null
        );

        // Complete transaction
        return posTransactionService.completeTransaction(transaction.getId());
    }

    /**
     * Search customers for mobile.
     */
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> searchCustomers(String query, int page, int size) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        return customerRepository.searchByNameOrCodeOrPhone(tenantId, query, PageRequest.of(page, size))
                .map(c -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", c.getId());
                    result.put("code", c.getCode());
                    result.put("name", c.getName());
                    result.put("phone", c.getPhone());
                    result.put("email", c.getEmail());
                    result.put("creditLimit", c.getCreditLimit());
                    result.put("currentBalance", c.getCurrentBalance());
                    return result;
                });
    }

    /**
     * Search products for mobile.
     */
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> searchProducts(String query, int page, int size) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        return productRepository.searchByNameOrSkuOrBarcode(tenantId, query, PageRequest.of(page, size))
                .map(p -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", p.getId());
                    result.put("sku", p.getSku());
                    result.put("barcode", p.getBarcode());
                    result.put("name", p.getName());
                    result.put("sellingPrice", p.getSellingPrice());
                    result.put("category", p.getCategory() != null ? p.getCategory().getName() : null);
                    return result;
                });
    }
}
