package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.inventory.dto.AbcAnalysisDto;
import com.hisobnoma.platform.inventory.dto.LowStockDto;
import com.hisobnoma.platform.inventory.dto.ReorderSuggestionDto;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.Stock;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.inventory.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for inventory planning and analysis.
 * Provides reorder suggestions, ABC analysis, slow-moving, and dead stock detection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryPlanningService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Get reorder suggestions for products below reorder point.
     */
    @Transactional(readOnly = true)
    public List<ReorderSuggestionDto> getReorderSuggestions(Long locationId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        List<Stock> stocks;
        if (locationId != null) {
            stocks = stockRepository.findByLocationIdAndTenantId(locationId, tenantId);
        } else {
            stocks = stockRepository.findByTenantId(tenantId);
        }

        List<ReorderSuggestionDto> suggestions = new ArrayList<>();

        for (Stock stock : stocks) {
            Product product = stock.getProduct();
            if (product == null || !product.isTrackInventory()) continue;

            BigDecimal reorderPoint = product.getReorderPoint();
            if (reorderPoint == null || reorderPoint.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal currentStock = stock.getQuantityAvailable();
            if (currentStock.compareTo(reorderPoint) < 0) {
                BigDecimal suggestedQuantity = product.getReorderQuantity();
                if (suggestedQuantity == null || suggestedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    // Default: order enough to reach max stock level or 2x reorder point
                    if (product.getMaxStockLevel() != null) {
                        suggestedQuantity = product.getMaxStockLevel().subtract(currentStock);
                    } else {
                        suggestedQuantity = reorderPoint.multiply(BigDecimal.valueOf(2)).subtract(currentStock);
                    }
                }

                suggestions.add(ReorderSuggestionDto.builder()
                        .productId(product.getId())
                        .productSku(product.getSku())
                        .productName(product.getName())
                        .locationId(stock.getLocation().getId())
                        .locationName(stock.getLocation().getName())
                        .currentStock(currentStock)
                        .reorderPoint(reorderPoint)
                        .suggestedQuantity(suggestedQuantity.max(BigDecimal.ZERO))
                        .estimatedCost(product.getCostPrice() != null ?
                                product.getCostPrice().multiply(suggestedQuantity) : null)
                        .priority(calculatePriority(currentStock, reorderPoint, product.getMinStockLevel()))
                        .build());
            }
        }

        // Sort by priority (highest first)
        suggestions.sort((a, b) -> b.getPriority().compareTo(a.getPriority()));

        return suggestions;
    }

    /**
     * Perform ABC analysis on products based on value/movement.
     */
    @Transactional(readOnly = true)
    public List<AbcAnalysisDto> performAbcAnalysis(int days) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Instant startDate = Instant.now().minus(days, ChronoUnit.DAYS);

        // Get products and their movement values
        List<Product> products = productRepository.findAllByTenantId(tenantId, Pageable.unpaged()).getContent();
        List<AbcAnalysisDto> analysis = new ArrayList<>();

        BigDecimal totalValue = BigDecimal.ZERO;

        for (Product product : products) {
            if (!product.isActive() || !product.isTrackInventory()) continue;

            // Calculate movement value (quantity sold * unit cost)
            BigDecimal movementValue = movementRepository.sumMovementValueByProductAndDateRange(
                    product.getId(), tenantId, startDate, Instant.now());

            if (movementValue == null) movementValue = BigDecimal.ZERO;

            // Get current stock value
            BigDecimal stockValue = calculateStockValue(product, tenantId);

            BigDecimal itemValue = movementValue.add(stockValue);
            totalValue = totalValue.add(itemValue);

            analysis.add(AbcAnalysisDto.builder()
                    .productId(product.getId())
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .movementValue(movementValue)
                    .stockValue(stockValue)
                    .totalValue(itemValue)
                    .build());
        }

        // Sort by value descending
        analysis.sort((a, b) -> b.getTotalValue().compareTo(a.getTotalValue()));

        // Assign ABC classification
        BigDecimal cumulativeValue = BigDecimal.ZERO;
        for (AbcAnalysisDto item : analysis) {
            cumulativeValue = cumulativeValue.add(item.getTotalValue());
            BigDecimal cumulativePercent = totalValue.compareTo(BigDecimal.ZERO) > 0 ?
                    cumulativeValue.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                    BigDecimal.ZERO;

            item.setCumulativePercent(cumulativePercent);

            if (cumulativePercent.compareTo(BigDecimal.valueOf(80)) <= 0) {
                item.setClassification("A");
            } else if (cumulativePercent.compareTo(BigDecimal.valueOf(95)) <= 0) {
                item.setClassification("B");
            } else {
                item.setClassification("C");
            }

            // Calculate percent of total
            item.setPercentOfTotal(totalValue.compareTo(BigDecimal.ZERO) > 0 ?
                    item.getTotalValue().divide(totalValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                    BigDecimal.ZERO);
        }

        return analysis;
    }

    /**
     * Get slow-moving products (not sold in X days).
     */
    @Transactional(readOnly = true)
    public List<LowStockDto> getSlowMovingProducts(int days, Long locationId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Instant cutoffDate = Instant.now().minus(days, ChronoUnit.DAYS);

        List<Product> products = productRepository.findAllByTenantId(tenantId, Pageable.unpaged()).getContent();
        List<LowStockDto> slowMoving = new ArrayList<>();

        for (Product product : products) {
            if (!product.isActive() || !product.isTrackInventory()) continue;

            // Check last movement date
            Instant lastMovement = movementRepository.findLastMovementDateByProduct(product.getId(), tenantId);

            if (lastMovement == null || lastMovement.isBefore(cutoffDate)) {
                // Get current stock
                List<Stock> stocks = locationId != null ?
                        stockRepository.findByProductIdAndLocationId(product.getId(), locationId) :
                        stockRepository.findByProductId(product.getId());

                BigDecimal totalStock = stocks.stream()
                        .map(Stock::getQuantityOnHand)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalStock.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal stockValue = totalStock.multiply(
                            product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);

                    int daysSinceLastSale = lastMovement != null ?
                            (int) ChronoUnit.DAYS.between(lastMovement, Instant.now()) : days;

                    slowMoving.add(LowStockDto.builder()
                            .productId(product.getId())
                            .productSku(product.getSku())
                            .productName(product.getName())
                            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                            .currentStock(totalStock)
                            .stockValue(stockValue)
                            .daysSinceLastSale(daysSinceLastSale)
                            .lastSaleDate(lastMovement)
                            .recommendation("DISCOUNT")
                            .build());
                }
            }
        }

        // Sort by days since last sale descending
        slowMoving.sort((a, b) -> b.getDaysSinceLastSale().compareTo(a.getDaysSinceLastSale()));

        return slowMoving;
    }

    /**
     * Get dead stock (zero movement products).
     */
    @Transactional(readOnly = true)
    public List<LowStockDto> getDeadStock(int days, Long locationId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Instant cutoffDate = Instant.now().minus(days, ChronoUnit.DAYS);

        List<Product> products = productRepository.findAllByTenantId(tenantId, Pageable.unpaged()).getContent();
        List<LowStockDto> deadStock = new ArrayList<>();

        for (Product product : products) {
            if (!product.isActive() || !product.isTrackInventory()) continue;

            // Check if there's any movement at all
            boolean hasMovement = movementRepository.existsByProductIdAndTenantIdAndCreatedAtAfter(
                    product.getId(), tenantId, cutoffDate);

            if (!hasMovement) {
                // Get current stock
                List<Stock> stocks = locationId != null ?
                        stockRepository.findByProductIdAndLocationId(product.getId(), locationId) :
                        stockRepository.findByProductId(product.getId());

                BigDecimal totalStock = stocks.stream()
                        .map(Stock::getQuantityOnHand)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalStock.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal stockValue = totalStock.multiply(
                            product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);

                    Instant lastMovement = movementRepository.findLastMovementDateByProduct(product.getId(), tenantId);
                    int daysSinceLastMovement = lastMovement != null ?
                            (int) ChronoUnit.DAYS.between(lastMovement, Instant.now()) : days;

                    deadStock.add(LowStockDto.builder()
                            .productId(product.getId())
                            .productSku(product.getSku())
                            .productName(product.getName())
                            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                            .currentStock(totalStock)
                            .stockValue(stockValue)
                            .daysSinceLastSale(daysSinceLastMovement)
                            .lastSaleDate(lastMovement)
                            .recommendation("WRITE_OFF")
                            .build());
                }
            }
        }

        // Sort by stock value descending
        deadStock.sort((a, b) -> b.getStockValue().compareTo(a.getStockValue()));

        return deadStock;
    }

    private String calculatePriority(BigDecimal current, BigDecimal reorderPoint, BigDecimal minStock) {
        if (minStock != null && current.compareTo(minStock) <= 0) {
            return "CRITICAL";
        }
        BigDecimal ratio = current.divide(reorderPoint, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(BigDecimal.valueOf(0.25)) <= 0) {
            return "HIGH";
        } else if (ratio.compareTo(BigDecimal.valueOf(0.5)) <= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private BigDecimal calculateStockValue(Product product, Long tenantId) {
        List<Stock> stocks = stockRepository.findByProductId(product.getId());
        BigDecimal totalQuantity = stocks.stream()
                .map(Stock::getQuantityOnHand)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalQuantity.multiply(product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);
    }
}
