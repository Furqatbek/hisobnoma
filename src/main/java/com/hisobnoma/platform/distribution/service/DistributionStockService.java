package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
import com.hisobnoma.platform.inventory.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Runs distribution-order stock mutations in their own {@code REQUIRES_NEW} transactions so
 * a stock failure stays isolated and cannot mark the caller's (order-lifecycle) transaction
 * rollback-only.
 *
 * <p>{@link StockService#deductStockForSale} is {@code REQUIRED}: called directly from the
 * delivery transaction, a failure there would poison it and roll back the status change even
 * though the caller swallows the exception. Wrapping it in a separate proxied bean with
 * {@code REQUIRES_NEW} gives the same best-effort isolation that {@code reserveStock} /
 * {@code releaseReservation} already have.
 */
@Service
@RequiredArgsConstructor
public class DistributionStockService {

    private final StockService stockService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deduct(Long productId, Long locationId, BigDecimal quantity, Long orderId, String orderNumber) {
        stockService.deductStockForSale(productId, locationId, quantity,
                MovementReferenceType.DISTRIBUTION_ORDER, orderId, orderNumber);
    }
}
