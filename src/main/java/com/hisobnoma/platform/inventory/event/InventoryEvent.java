package com.hisobnoma.platform.inventory.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Base class for all inventory-related domain events.
 * Extends Spring's ApplicationEvent for seamless integration with Spring's event system.
 */
@Getter
public abstract class InventoryEvent extends ApplicationEvent {

    private final Long tenantId;
    private final Long productId;
    private final String productSku;
    private final String productName;
    private final LocalDateTime eventTime;

    protected InventoryEvent(Object source, Long tenantId, Long productId, String productSku, String productName) {
        super(source);
        this.tenantId = tenantId;
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.eventTime = LocalDateTime.now();
    }

    /**
     * Returns the event type name for logging and tracking purposes.
     */
    public abstract String getEventType();

    /**
     * Returns a human-readable description of the event.
     */
    public abstract String getDescription();
}
