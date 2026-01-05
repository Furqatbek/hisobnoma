package com.hisobnoma.platform.inventory.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for inventory-related domain events.
 * Handles logging, notifications, and potential integrations with other systems.
 *
 * Events are processed asynchronously to avoid blocking the main transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    // Future: Inject notification service, external integration service, etc.
    // private final NotificationService notificationService;
    // private final AuditLogService auditLogService;

    /**
     * Handles StockReceivedEvent.
     * Logs the event and can trigger notifications or integrations.
     */
    @Async
    @EventListener
    public void handleStockReceived(StockReceivedEvent event) {
        log.info("[INVENTORY_EVENT] {} | Tenant: {} | Product: {} ({}) | Location: {} | Qty: {} | Ref: {}",
                event.getEventType(),
                event.getTenantId(),
                event.getProductName(),
                event.getProductSku(),
                event.getLocationCode(),
                event.getQuantity(),
                event.getReferenceNumber());

        // Future implementations:
        // - Send webhook notifications to external systems
        // - Update dashboard real-time metrics
        // - Trigger inventory audit logging
        // auditLogService.logInventoryEvent(event);
    }

    /**
     * Handles StockIssuedEvent.
     * Logs the event and can trigger notifications or integrations.
     */
    @Async
    @EventListener
    public void handleStockIssued(StockIssuedEvent event) {
        log.info("[INVENTORY_EVENT] {} | Tenant: {} | Product: {} ({}) | Location: {} | Qty: {} | Reason: {} | Ref: {}",
                event.getEventType(),
                event.getTenantId(),
                event.getProductName(),
                event.getProductSku(),
                event.getLocationCode(),
                event.getQuantity(),
                event.getReason(),
                event.getReferenceNumber());

        // Future implementations:
        // - Update sales analytics
        // - Trigger reorder suggestions
        // - Notify warehouse staff
    }

    /**
     * Handles StockTransferredEvent.
     * Logs the event and can trigger notifications for receiving location.
     */
    @Async
    @EventListener
    public void handleStockTransferred(StockTransferredEvent event) {
        log.info("[INVENTORY_EVENT] {} | Tenant: {} | Product: {} ({}) | From: {} -> To: {} | Qty: {} | Ref: {}",
                event.getEventType(),
                event.getTenantId(),
                event.getProductName(),
                event.getProductSku(),
                event.getFromLocationCode(),
                event.getToLocationCode(),
                event.getQuantity(),
                event.getReferenceNumber());

        // Future implementations:
        // - Notify receiving location staff
        // - Update transfer tracking dashboard
        // - Log for compliance/audit
    }

    /**
     * Handles StockAdjustedEvent.
     * Logs the event with adjustment details for audit purposes.
     */
    @Async
    @EventListener
    public void handleStockAdjusted(StockAdjustedEvent event) {
        String direction = event.isPositiveAdjustment() ? "+" : "";
        log.info("[INVENTORY_EVENT] {} | Tenant: {} | Product: {} ({}) | Location: {} | Adjustment: {}{} | Type: {} | Reason: {} | Ref: {}",
                event.getEventType(),
                event.getTenantId(),
                event.getProductName(),
                event.getProductSku(),
                event.getLocationCode(),
                direction,
                event.getAdjustmentQuantity(),
                event.getAdjustmentType(),
                event.getReason(),
                event.getReferenceNumber());

        // Future implementations:
        // - Log to compliance audit trail
        // - Alert managers for large adjustments
        // - Update inventory accuracy metrics
    }

    /**
     * Handles LowStockAlertEvent.
     * This is critical for triggering reorder notifications and alerts.
     */
    @Async
    @EventListener
    public void handleLowStockAlert(LowStockAlertEvent event) {
        // Log with appropriate level based on severity
        switch (event.getSeverity()) {
            case CRITICAL:
                log.warn("[LOW_STOCK_ALERT] CRITICAL | Tenant: {} | Product: {} ({}) | Location: {} | " +
                                "Current: {} | Reorder Point: {} | OUT OF STOCK!",
                        event.getTenantId(),
                        event.getProductName(),
                        event.getProductSku(),
                        event.getLocationCode(),
                        event.getCurrentQuantity(),
                        event.getReorderPoint());
                break;
            case LOW:
                log.warn("[LOW_STOCK_ALERT] LOW | Tenant: {} | Product: {} ({}) | Location: {} | " +
                                "Current: {} | Min Level: {} | Below minimum!",
                        event.getTenantId(),
                        event.getProductName(),
                        event.getProductSku(),
                        event.getLocationCode(),
                        event.getCurrentQuantity(),
                        event.getMinimumStockLevel());
                break;
            case WARNING:
                log.info("[LOW_STOCK_ALERT] WARNING | Tenant: {} | Product: {} ({}) | Location: {} | " +
                                "Current: {} | Reorder Point: {} | Suggested Order: {}",
                        event.getTenantId(),
                        event.getProductName(),
                        event.getProductSku(),
                        event.getLocationCode(),
                        event.getCurrentQuantity(),
                        event.getReorderPoint(),
                        event.getSuggestedOrderQuantity());
                break;
        }

        // Future implementations:
        // - Send email/SMS notifications to purchasing team
        // - Create automatic purchase order suggestions
        // - Update low stock dashboard
        // - Integrate with vendor portals for automatic reordering

        // Example: Send notification based on severity
        // if (event.getSeverity() == LowStockAlertEvent.AlertSeverity.CRITICAL) {
        //     notificationService.sendUrgentAlert(
        //         event.getTenantId(),
        //         "OUT OF STOCK: " + event.getProductName(),
        //         event.getDescription()
        //     );
        // }
    }

    /**
     * Generic handler for any InventoryEvent not specifically handled above.
     * Useful for logging and auditing purposes.
     */
    @Async
    @EventListener(condition = "#event.eventType != 'STOCK_RECEIVED' && #event.eventType != 'STOCK_ISSUED' " +
            "&& #event.eventType != 'STOCK_TRANSFERRED' && #event.eventType != 'STOCK_ADJUSTED' " +
            "&& #event.eventType != 'LOW_STOCK_ALERT'")
    public void handleGenericInventoryEvent(InventoryEvent event) {
        log.info("[INVENTORY_EVENT] {} | Tenant: {} | Product: {} ({}) | {}",
                event.getEventType(),
                event.getTenantId(),
                event.getProductName(),
                event.getProductSku(),
                event.getDescription());
    }
}
