package com.hisobnoma.platform.inventory.event;

import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Publisher for inventory-related domain events.
 * Provides convenient methods for publishing events and checking low stock conditions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publishes a StockReceivedEvent.
     */
    public void publishStockReceived(Long tenantId, Product product, Location location,
                                      BigDecimal quantity, BigDecimal unitCost,
                                      BigDecimal quantityBefore, BigDecimal quantityAfter,
                                      String referenceType, Long referenceId, String referenceNumber,
                                      String batchNumber) {
        StockReceivedEvent event = StockReceivedEvent.builder(this)
                .tenantId(tenantId)
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .locationId(location.getId())
                .locationCode(location.getCode())
                .locationName(location.getName())
                .quantity(quantity)
                .unitCost(unitCost)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .batchNumber(batchNumber)
                .build();

        eventPublisher.publishEvent(event);
        log.debug("Published StockReceivedEvent: {}", event.getDescription());
    }

    /**
     * Publishes a StockIssuedEvent.
     */
    public void publishStockIssued(Long tenantId, Product product, Location location,
                                    BigDecimal quantity, BigDecimal unitCost,
                                    BigDecimal quantityBefore, BigDecimal quantityAfter,
                                    String referenceType, Long referenceId, String referenceNumber,
                                    String reason, String batchNumber) {
        StockIssuedEvent event = StockIssuedEvent.builder(this)
                .tenantId(tenantId)
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .locationId(location.getId())
                .locationCode(location.getCode())
                .locationName(location.getName())
                .quantity(quantity)
                .unitCost(unitCost)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .reason(reason)
                .batchNumber(batchNumber)
                .build();

        eventPublisher.publishEvent(event);
        log.debug("Published StockIssuedEvent: {}", event.getDescription());
    }

    /**
     * Publishes a StockTransferredEvent.
     */
    public void publishStockTransferred(Long tenantId, Product product,
                                         Location fromLocation, Location toLocation,
                                         BigDecimal quantity, BigDecimal unitCost,
                                         BigDecimal sourceQuantityBefore, BigDecimal sourceQuantityAfter,
                                         BigDecimal destQuantityBefore, BigDecimal destQuantityAfter,
                                         String referenceNumber, String reason, String batchNumber) {
        StockTransferredEvent event = StockTransferredEvent.builder(this)
                .tenantId(tenantId)
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .fromLocationId(fromLocation.getId())
                .fromLocationCode(fromLocation.getCode())
                .fromLocationName(fromLocation.getName())
                .toLocationId(toLocation.getId())
                .toLocationCode(toLocation.getCode())
                .toLocationName(toLocation.getName())
                .quantity(quantity)
                .unitCost(unitCost)
                .sourceQuantityBefore(sourceQuantityBefore)
                .sourceQuantityAfter(sourceQuantityAfter)
                .destQuantityBefore(destQuantityBefore)
                .destQuantityAfter(destQuantityAfter)
                .referenceNumber(referenceNumber)
                .reason(reason)
                .batchNumber(batchNumber)
                .build();

        eventPublisher.publishEvent(event);
        log.debug("Published StockTransferredEvent: {}", event.getDescription());
    }

    /**
     * Publishes a StockAdjustedEvent.
     */
    public void publishStockAdjusted(Long tenantId, Product product, Location location,
                                      BigDecimal adjustmentQuantity,
                                      BigDecimal quantityBefore, BigDecimal quantityAfter,
                                      String referenceType, Long referenceId, String referenceNumber,
                                      String reason, StockAdjustedEvent.AdjustmentType adjustmentType) {
        StockAdjustedEvent event = StockAdjustedEvent.builder(this)
                .tenantId(tenantId)
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .locationId(location.getId())
                .locationCode(location.getCode())
                .locationName(location.getName())
                .adjustmentQuantity(adjustmentQuantity)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .reason(reason)
                .adjustmentType(adjustmentType)
                .build();

        eventPublisher.publishEvent(event);
        log.debug("Published StockAdjustedEvent: {}", event.getDescription());
    }

    /**
     * Publishes a LowStockAlertEvent if stock is below reorder point.
     * Should be called after any stock-decreasing operation.
     */
    public void publishLowStockAlertIfNeeded(Long tenantId, Stock stock) {
        if (stock == null || stock.getProduct() == null) {
            return;
        }

        Product product = stock.getProduct();
        Location location = stock.getLocation();
        BigDecimal currentQuantity = stock.getQuantityAvailable();

        // Get effective reorder point and minimum level
        BigDecimal reorderPoint = stock.getReorderPoint() != null ?
                stock.getReorderPoint() : (product.getReorderPoint() != null ? product.getReorderPoint() : BigDecimal.ZERO);
        BigDecimal minStockLevel = stock.getMinStockLevel() != null ?
                stock.getMinStockLevel() : (product.getMinStockLevel() != null ? product.getMinStockLevel() : BigDecimal.ZERO);
        BigDecimal reorderQuantity = stock.getReorderQuantity() != null ?
                stock.getReorderQuantity() : product.getReorderQuantity();

        // Determine severity
        LowStockAlertEvent.AlertSeverity severity;
        if (currentQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            severity = LowStockAlertEvent.AlertSeverity.CRITICAL;
        } else if (currentQuantity.compareTo(minStockLevel) < 0) {
            severity = LowStockAlertEvent.AlertSeverity.LOW;
        } else if (currentQuantity.compareTo(reorderPoint) < 0) {
            severity = LowStockAlertEvent.AlertSeverity.WARNING;
        } else {
            // Stock is above reorder point, no alert needed
            return;
        }

        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
        String brandName = product.getBrand() != null ? product.getBrand().getName() : null;

        LowStockAlertEvent event = LowStockAlertEvent.builder(this)
                .tenantId(tenantId)
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .locationId(location.getId())
                .locationCode(location.getCode())
                .locationName(location.getName())
                .currentQuantity(currentQuantity)
                .reorderPoint(reorderPoint)
                .minimumStockLevel(minStockLevel)
                .reorderQuantity(reorderQuantity)
                .severity(severity)
                .categoryName(categoryName)
                .brandName(brandName)
                .build();

        eventPublisher.publishEvent(event);
        log.info("Published LowStockAlertEvent: {}", event.getDescription());
    }

    /**
     * Generic method to publish any InventoryEvent.
     */
    public void publish(InventoryEvent event) {
        eventPublisher.publishEvent(event);
        log.debug("Published {}: {}", event.getEventType(), event.getDescription());
    }
}
