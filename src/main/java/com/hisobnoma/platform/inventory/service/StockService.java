package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.mapper.StockMapper;
import com.hisobnoma.platform.inventory.mapper.StockMovementMapper;
import com.hisobnoma.platform.inventory.repository.*;
import com.hisobnoma.platform.inventory.event.InventoryEventPublisher;
import com.hisobnoma.platform.inventory.event.StockAdjustedEvent;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockBatchRepository stockBatchRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final StockMapper stockMapper;
    private final StockMovementMapper stockMovementMapper;
    private final SecurityContextHelper securityContextHelper;
    private final InventoryEventPublisher inventoryEventPublisher;

    // ==================== Stock Query Methods ====================

    @Transactional(readOnly = true)
    public PageResponse<StockDto> getStock(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Stock> page = stockRepository.findByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(stockMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<StockDto> getStockByProduct(Long productId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return stockRepository.findByProductIdAndTenantId(productId, tenantId)
                .stream()
                .map(stockMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<StockDto> getStockByLocation(Long locationId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Stock> page = stockRepository.findByLocationIdAndTenantId(locationId, tenantId, pageable);
        return PageResponse.of(page.map(stockMapper::toDto));
    }

    @Transactional(readOnly = true)
    public StockDto getStockByProductAndLocation(Long productId, Long locationId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Stock stock = stockRepository.findByProductIdAndLocationIdAndTenantId(productId, locationId, tenantId)
                .orElse(null);
        return stock != null ? stockMapper.toDto(stock) : null;
    }

    @Transactional(readOnly = true)
    public PageResponse<LowStockDto> getLowStock(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Stock> page = stockRepository.findBelowReorderPoint(tenantId, pageable);
        return PageResponse.of(page.map(this::mapToLowStockDto));
    }

    private LowStockDto mapToLowStockDto(Stock stock) {
        BigDecimal reorderPoint = stock.getReorderPoint() != null ?
                stock.getReorderPoint() : stock.getProduct().getReorderPoint();
        BigDecimal available = stock.getQuantityAvailable();
        BigDecimal shortfall = reorderPoint.subtract(available);

        String severity;
        if (stock.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
            severity = "CRITICAL";
        } else if (stock.isBelowMinimum()) {
            severity = "LOW";
        } else {
            severity = "WARNING";
        }

        return LowStockDto.builder()
                .productId(stock.getProduct().getId())
                .productSku(stock.getProduct().getSku())
                .productName(stock.getProduct().getName())
                .locationId(stock.getLocation().getId())
                .locationCode(stock.getLocation().getCode())
                .locationName(stock.getLocation().getName())
                .quantityOnHand(stock.getQuantityOnHand())
                .quantityAvailable(available)
                .reorderPoint(reorderPoint)
                .reorderQuantity(stock.getReorderQuantity() != null ?
                        stock.getReorderQuantity() : stock.getProduct().getReorderQuantity())
                .minStockLevel(stock.getMinStockLevel() != null ?
                        stock.getMinStockLevel() : stock.getProduct().getMinStockLevel())
                .shortfallQuantity(shortfall.max(BigDecimal.ZERO))
                .severity(severity)
                .categoryName(stock.getProduct().getCategory() != null ?
                        stock.getProduct().getCategory().getName() : null)
                .brandName(stock.getProduct().getBrand() != null ?
                        stock.getProduct().getBrand().getName() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<StockDto> searchStock(String query, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Stock> page = stockRepository.searchStock(tenantId, query, pageable);
        return PageResponse.of(page.map(stockMapper::toDto));
    }

    @Transactional(readOnly = true)
    public StockValuationDto getStockValuation() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BigDecimal totalValue = stockRepository.getTotalStockValue(tenantId);
        Long totalProducts = stockRepository.countProductsWithStock(tenantId);

        return StockValuationDto.builder()
                .totalValue(totalValue != null ? totalValue : BigDecimal.ZERO)
                .totalProducts(totalProducts)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementDto> getStockMovements(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<StockMovement> page = stockMovementRepository.findByTenantIdOrderByMovementDateDesc(tenantId, pageable);
        return PageResponse.of(page.map(stockMovementMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementDto> getStockMovementsByProduct(Long productId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<StockMovement> page = stockMovementRepository.findByProductIdAndTenantIdOrderByMovementDateDesc(productId, tenantId, pageable);
        return PageResponse.of(page.map(stockMovementMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementDto> getStockMovementsByLocation(Long locationId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<StockMovement> page = stockMovementRepository.findByLocationId(tenantId, locationId, pageable);
        return PageResponse.of(page.map(stockMovementMapper::toDto));
    }

    // ==================== Stock Movement Methods ====================

    @Transactional
    public List<StockMovementDto> receiveStock(StockReceiveRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Location location = getLocation(request.getLocationId(), tenantId);

        List<StockMovement> movements = new ArrayList<>();

        for (StockReceiveRequest.ReceiveItem item : request.getItems()) {
            Product product = getProduct(item.getProductId(), tenantId);

            // Get or create stock record
            Stock stock = stockRepository.findByProductIdAndLocationIdWithLock(
                    item.getProductId(), request.getLocationId(), tenantId)
                    .orElseGet(() -> createNewStock(product, location, tenantId));

            BigDecimal quantityBefore = stock.getQuantityOnHand();

            // Update stock quantity and cost
            if (item.getUnitCost() != null) {
                stock.updateAverageCost(item.getQuantity(), item.getUnitCost());
            }
            stock.setQuantityOnHand(stock.getQuantityOnHand().add(item.getQuantity()));

            stockRepository.save(stock);

            // Create stock movement
            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .toLocation(location)
                    .movementType(MovementType.STOCK_IN)
                    .quantity(item.getQuantity())
                    .unitCost(item.getUnitCost())
                    .referenceType(request.getReferenceType())
                    .referenceId(request.getReferenceId())
                    .referenceNumber(request.getReferenceNumber())
                    .movementDate(LocalDateTime.now())
                    .notes(request.getNotes())
                    .batchNumber(item.getBatchNumber())
                    .quantityBefore(quantityBefore)
                    .quantityAfter(stock.getQuantityOnHand())
                    .tenantId(tenantId)
                    .build();

            movements.add(stockMovementRepository.save(movement));

            // Handle batch tracking
            if (product.isTrackBatch() && item.getBatchNumber() != null) {
                createOrUpdateBatch(product, location, item, tenantId);
            }

            // Handle serial tracking
            if (product.isTrackSerial() && item.getSerialNumbers() != null) {
                createSerialNumbers(product, location, item.getSerialNumbers(), item.getUnitCost(), tenantId);
            }

            log.info("Received {} units of product {} at location {}",
                    item.getQuantity(), product.getSku(), location.getCode());

            // Publish stock received event
            inventoryEventPublisher.publishStockReceived(
                    tenantId, product, location,
                    item.getQuantity(), item.getUnitCost(),
                    quantityBefore, stock.getQuantityOnHand(),
                    request.getReferenceType() != null ? request.getReferenceType().name() : null,
                    request.getReferenceId(), request.getReferenceNumber(),
                    item.getBatchNumber());
        }

        return movements.stream().map(stockMovementMapper::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<StockMovementDto> issueStock(StockIssueRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Location location = getLocation(request.getLocationId(), tenantId);

        List<StockMovement> movements = new ArrayList<>();

        for (StockIssueRequest.IssueItem item : request.getItems()) {
            Product product = getProduct(item.getProductId(), tenantId);

            // Get stock record with lock
            Stock stock = stockRepository.findByProductIdAndLocationIdWithLock(
                    item.getProductId(), request.getLocationId(), tenantId)
                    .orElseThrow(() -> new BusinessException("No stock found for product: " + product.getSku()));

            BigDecimal available = stock.getQuantityAvailable();

            // Check availability
            if (!location.isAllowNegativeStock() && !product.isAllowNegativeStock()) {
                if (available.compareTo(item.getQuantity()) < 0) {
                    throw new BusinessException(String.format(
                            "Insufficient stock for product %s. Available: %s, Requested: %s",
                            product.getSku(), available, item.getQuantity()));
                }
            }

            BigDecimal quantityBefore = stock.getQuantityOnHand();

            // Deduct stock
            stock.setQuantityOnHand(stock.getQuantityOnHand().subtract(item.getQuantity()));
            stockRepository.save(stock);

            // Create stock movement
            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .fromLocation(location)
                    .movementType(MovementType.STOCK_OUT)
                    .quantity(item.getQuantity())
                    .unitCost(stock.getAverageCost())
                    .referenceType(request.getReferenceType())
                    .referenceId(request.getReferenceId())
                    .referenceNumber(request.getReferenceNumber())
                    .movementDate(LocalDateTime.now())
                    .reason(request.getReason())
                    .notes(request.getNotes())
                    .batchNumber(item.getBatchNumber())
                    .quantityBefore(quantityBefore)
                    .quantityAfter(stock.getQuantityOnHand())
                    .tenantId(tenantId)
                    .build();

            movements.add(stockMovementRepository.save(movement));

            // Handle batch deduction
            if (product.isTrackBatch() && item.getBatchNumber() != null) {
                deductFromBatch(product, location, item.getBatchNumber(), item.getQuantity(), tenantId);
            }

            // Handle serial tracking
            if (product.isTrackSerial() && item.getSerialNumbers() != null) {
                issueSerialNumbers(product, item.getSerialNumbers(), tenantId);
            }

            log.info("Issued {} units of product {} from location {}",
                    item.getQuantity(), product.getSku(), location.getCode());

            // Publish stock issued event
            inventoryEventPublisher.publishStockIssued(
                    tenantId, product, location,
                    item.getQuantity(), stock.getAverageCost(),
                    quantityBefore, stock.getQuantityOnHand(),
                    request.getReferenceType() != null ? request.getReferenceType().name() : null,
                    request.getReferenceId(), request.getReferenceNumber(),
                    request.getReason(), item.getBatchNumber());

            // Check and publish low stock alert if needed
            inventoryEventPublisher.publishLowStockAlertIfNeeded(tenantId, stock);
        }

        return movements.stream().map(stockMovementMapper::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<StockMovementDto> transferStock(StockTransferRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        if (request.getFromLocationId().equals(request.getToLocationId())) {
            throw new BusinessException("Source and destination locations must be different");
        }

        Location fromLocation = getLocation(request.getFromLocationId(), tenantId);
        Location toLocation = getLocation(request.getToLocationId(), tenantId);

        List<StockMovement> movements = new ArrayList<>();

        for (StockTransferRequest.TransferItem item : request.getItems()) {
            Product product = getProduct(item.getProductId(), tenantId);

            // Get source stock with lock
            Stock sourceStock = stockRepository.findByProductIdAndLocationIdWithLock(
                    item.getProductId(), request.getFromLocationId(), tenantId)
                    .orElseThrow(() -> new BusinessException("No stock found at source location for: " + product.getSku()));

            BigDecimal available = sourceStock.getQuantityAvailable();

            // Check availability
            if (!fromLocation.isAllowNegativeStock() && available.compareTo(item.getQuantity()) < 0) {
                throw new BusinessException(String.format(
                        "Insufficient stock for transfer. Product: %s, Available: %s, Requested: %s",
                        product.getSku(), available, item.getQuantity()));
            }

            // Get or create destination stock
            Stock destStock = stockRepository.findByProductIdAndLocationIdWithLock(
                    item.getProductId(), request.getToLocationId(), tenantId)
                    .orElseGet(() -> createNewStock(product, toLocation, tenantId));

            BigDecimal sourceQtyBefore = sourceStock.getQuantityOnHand();
            BigDecimal destQtyBefore = destStock.getQuantityOnHand();

            // Update stock quantities
            sourceStock.setQuantityOnHand(sourceStock.getQuantityOnHand().subtract(item.getQuantity()));
            destStock.setQuantityOnHand(destStock.getQuantityOnHand().add(item.getQuantity()));

            // Transfer average cost
            if (destStock.getAverageCost() == null || destStock.getAverageCost().compareTo(BigDecimal.ZERO) == 0) {
                destStock.setAverageCost(sourceStock.getAverageCost());
            } else {
                destStock.updateAverageCost(item.getQuantity(), sourceStock.getAverageCost());
            }

            stockRepository.save(sourceStock);
            stockRepository.save(destStock);

            // Create transfer movement
            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .fromLocation(fromLocation)
                    .toLocation(toLocation)
                    .movementType(MovementType.TRANSFER)
                    .quantity(item.getQuantity())
                    .unitCost(sourceStock.getAverageCost())
                    .referenceType(MovementReferenceType.TRANSFER_ORDER)
                    .referenceNumber(request.getReferenceNumber())
                    .movementDate(LocalDateTime.now())
                    .reason(request.getReason())
                    .notes(request.getNotes())
                    .batchNumber(item.getBatchNumber())
                    .quantityBefore(sourceQtyBefore)
                    .quantityAfter(sourceStock.getQuantityOnHand())
                    .tenantId(tenantId)
                    .build();

            movements.add(stockMovementRepository.save(movement));

            log.info("Transferred {} units of product {} from {} to {}",
                    item.getQuantity(), product.getSku(), fromLocation.getCode(), toLocation.getCode());

            // Publish stock transferred event
            inventoryEventPublisher.publishStockTransferred(
                    tenantId, product, fromLocation, toLocation,
                    item.getQuantity(), sourceStock.getAverageCost(),
                    sourceQtyBefore, sourceStock.getQuantityOnHand(),
                    destQtyBefore, destStock.getQuantityOnHand(),
                    request.getReferenceNumber(), request.getReason(), item.getBatchNumber());

            // Check and publish low stock alert for source location if needed
            inventoryEventPublisher.publishLowStockAlertIfNeeded(tenantId, sourceStock);
        }

        return movements.stream().map(stockMovementMapper::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<StockMovementDto> adjustStock(StockAdjustRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Location location = getLocation(request.getLocationId(), tenantId);

        List<StockMovement> movements = new ArrayList<>();

        for (StockAdjustRequest.AdjustItem item : request.getItems()) {
            Product product = getProduct(item.getProductId(), tenantId);

            // Get or create stock record
            Stock stock = stockRepository.findByProductIdAndLocationIdWithLock(
                    item.getProductId(), request.getLocationId(), tenantId)
                    .orElseGet(() -> createNewStock(product, location, tenantId));

            BigDecimal quantityBefore = stock.getQuantityOnHand();
            BigDecimal adjustmentQuantity;

            // Calculate adjustment
            if (item.getNewQuantity() != null) {
                adjustmentQuantity = item.getNewQuantity().subtract(stock.getQuantityOnHand());
            } else {
                adjustmentQuantity = item.getAdjustmentQuantity();
            }

            // Apply adjustment
            stock.setQuantityOnHand(stock.getQuantityOnHand().add(adjustmentQuantity));
            stockRepository.save(stock);

            // Create movement
            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .fromLocation(adjustmentQuantity.compareTo(BigDecimal.ZERO) < 0 ? location : null)
                    .toLocation(adjustmentQuantity.compareTo(BigDecimal.ZERO) > 0 ? location : null)
                    .movementType(MovementType.ADJUSTMENT)
                    .quantity(adjustmentQuantity.abs())
                    .unitCost(stock.getAverageCost())
                    .referenceType(request.getReferenceType() != null ? request.getReferenceType() : MovementReferenceType.MANUAL_ADJUSTMENT)
                    .referenceId(request.getReferenceId())
                    .referenceNumber(request.getReferenceNumber())
                    .movementDate(LocalDateTime.now())
                    .reason(item.getReason() != null ? item.getReason() : request.getReason())
                    .notes(request.getNotes())
                    .batchNumber(item.getBatchNumber())
                    .quantityBefore(quantityBefore)
                    .quantityAfter(stock.getQuantityOnHand())
                    .tenantId(tenantId)
                    .build();

            movements.add(stockMovementRepository.save(movement));

            log.info("Adjusted product {} by {} units at location {} (reason: {})",
                    product.getSku(), adjustmentQuantity, location.getCode(), request.getReason());

            // Determine adjustment type based on reference type
            StockAdjustedEvent.AdjustmentType adjustmentType = StockAdjustedEvent.AdjustmentType.MANUAL;
            if (request.getReferenceType() != null) {
                switch (request.getReferenceType()) {
                    case INVENTORY_COUNT -> adjustmentType = StockAdjustedEvent.AdjustmentType.INVENTORY_COUNT;
                    case MANUAL_ADJUSTMENT -> adjustmentType = StockAdjustedEvent.AdjustmentType.CORRECTION;
                    default -> adjustmentType = StockAdjustedEvent.AdjustmentType.MANUAL;
                }
            }

            // Publish stock adjusted event
            inventoryEventPublisher.publishStockAdjusted(
                    tenantId, product, location,
                    adjustmentQuantity, quantityBefore, stock.getQuantityOnHand(),
                    request.getReferenceType() != null ? request.getReferenceType().name() : null,
                    request.getReferenceId(), request.getReferenceNumber(),
                    item.getReason() != null ? item.getReason() : request.getReason(),
                    adjustmentType);

            // Check and publish low stock alert if negative adjustment
            if (adjustmentQuantity.compareTo(java.math.BigDecimal.ZERO) < 0) {
                inventoryEventPublisher.publishLowStockAlertIfNeeded(tenantId, stock);
            }
        }

        return movements.stream().map(stockMovementMapper::toDto).collect(Collectors.toList());
    }

    // ==================== Reservation Methods ====================

    @Transactional
    public void reserveStock(Long productId, Long locationId, BigDecimal quantity,
                             MovementReferenceType referenceType, Long referenceId, String referenceNumber) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Stock stock = stockRepository.findByProductIdAndLocationIdWithLock(productId, locationId, tenantId)
                .orElseThrow(() -> new BusinessException("No stock found for reservation"));

        BigDecimal available = stock.getQuantityAvailable();
        if (available.compareTo(quantity) < 0) {
            throw new BusinessException("Insufficient stock for reservation");
        }

        // Update reservation quantity
        stock.setQuantityReserved(stock.getQuantityReserved().add(quantity));
        stockRepository.save(stock);

        // Create reservation record
        StockReservation reservation = StockReservation.builder()
                .product(stock.getProduct())
                .location(stock.getLocation())
                .quantity(quantity)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .tenantId(tenantId)
                .build();

        stockReservationRepository.save(reservation);
        log.info("Reserved {} units of product {} for {} {}", quantity, stock.getProduct().getSku(), referenceType, referenceId);
    }

    @Transactional
    public void releaseReservation(Long productId, Long locationId,
                                   MovementReferenceType referenceType, Long referenceId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        StockReservation reservation = stockReservationRepository.findByProductIdAndLocationIdAndReferenceTypeAndReferenceIdAndTenantId(
                productId, locationId, referenceType, referenceId, tenantId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        Stock stock = stockRepository.findByProductIdAndLocationIdWithLock(productId, locationId, tenantId)
                .orElseThrow(() -> new BusinessException("Stock record not found"));

        // Release reservation
        stock.setQuantityReserved(stock.getQuantityReserved().subtract(reservation.getQuantity()));
        stockRepository.save(stock);

        reservation.cancel();
        stockReservationRepository.save(reservation);

        log.info("Released reservation of {} units for product {}", reservation.getQuantity(), stock.getProduct().getSku());
    }

    // ==================== Helper Methods ====================

    private Product getProduct(Long productId, Long tenantId) {
        return productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
    }

    private Location getLocation(Long locationId, Long tenantId) {
        return locationRepository.findByIdAndTenantId(locationId, tenantId)
                .orElseThrow(() -> new NotFoundException("Location not found: " + locationId));
    }

    private Stock createNewStock(Product product, Location location, Long tenantId) {
        return Stock.builder()
                .product(product)
                .location(location)
                .quantityOnHand(BigDecimal.ZERO)
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(product.getCostPrice())
                .tenantId(tenantId)
                .build();
    }

    private void createOrUpdateBatch(Product product, Location location,
                                      StockReceiveRequest.ReceiveItem item, Long tenantId) {
        StockBatch batch = stockBatchRepository.findByProductIdAndLocationIdAndBatchNumberAndTenantId(
                product.getId(), location.getId(), item.getBatchNumber(), tenantId)
                .orElseGet(() -> StockBatch.builder()
                        .product(product)
                        .location(location)
                        .batchNumber(item.getBatchNumber())
                        .quantity(BigDecimal.ZERO)
                        .tenantId(tenantId)
                        .build());

        batch.setQuantity(batch.getQuantity().add(item.getQuantity()));
        if (batch.getInitialQuantity() == null) {
            batch.setInitialQuantity(item.getQuantity());
        }
        batch.setUnitCost(item.getUnitCost());
        batch.setManufactureDate(item.getManufactureDate());
        batch.setExpiryDate(item.getExpiryDate());
        batch.setSupplierBatchNumber(item.getSupplierBatchNumber());

        stockBatchRepository.save(batch);
    }

    private void deductFromBatch(Product product, Location location,
                                  String batchNumber, BigDecimal quantity, Long tenantId) {
        StockBatch batch = stockBatchRepository.findWithLock(
                product.getId(), location.getId(), batchNumber, tenantId)
                .orElseThrow(() -> new BusinessException("Batch not found: " + batchNumber));

        if (batch.getQuantityAvailable().compareTo(quantity) < 0) {
            throw new BusinessException("Insufficient quantity in batch: " + batchNumber);
        }

        batch.setQuantity(batch.getQuantity().subtract(quantity));
        if (batch.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            batch.setStatus(StockBatch.BatchStatus.DEPLETED);
        }

        stockBatchRepository.save(batch);
    }

    private void createSerialNumbers(Product product, Location location,
                                      List<String> serialNumbers, BigDecimal unitCost, Long tenantId) {
        for (String serial : serialNumbers) {
            if (serialNumberRepository.existsByProductIdAndSerialNumberAndTenantId(product.getId(), serial, tenantId)) {
                throw new BusinessException("Serial number already exists: " + serial);
            }

            SerialNumber serialNumber = SerialNumber.builder()
                    .product(product)
                    .location(location)
                    .serialNumber(serial)
                    .status(SerialNumber.SerialStatus.AVAILABLE)
                    .unitCost(unitCost)
                    .receivedDate(LocalDateTime.now())
                    .tenantId(tenantId)
                    .build();

            serialNumberRepository.save(serialNumber);
        }
    }

    private void issueSerialNumbers(Product product, List<String> serialNumbers, Long tenantId) {
        for (String serial : serialNumbers) {
            SerialNumber serialNumber = serialNumberRepository.findWithLock(product.getId(), serial, tenantId)
                    .orElseThrow(() -> new BusinessException("Serial number not found: " + serial));

            if (serialNumber.getStatus() != SerialNumber.SerialStatus.AVAILABLE) {
                throw new BusinessException("Serial number not available: " + serial);
            }

            serialNumber.setStatus(SerialNumber.SerialStatus.SOLD);
            serialNumber.setSoldDate(LocalDateTime.now());
            serialNumberRepository.save(serialNumber);
        }
    }

    // ==================== Methods for POS/Web Integration ====================

    /**
     * Deduct stock for a sale transaction (called by POS/Web modules)
     */
    @Transactional
    public void deductStockForSale(Long productId, Long locationId, BigDecimal quantity,
                                   MovementReferenceType refType, Long refId, String refNumber) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        StockIssueRequest request = StockIssueRequest.builder()
                .locationId(locationId)
                .referenceType(refType)
                .referenceId(refId)
                .referenceNumber(refNumber)
                .reason("Sale transaction")
                .items(List.of(StockIssueRequest.IssueItem.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .build()))
                .build();

        issueStock(request);
    }

    /**
     * Return stock for a refund (called by POS/Web modules)
     */
    @Transactional
    public void returnStock(Long productId, Long locationId, BigDecimal quantity, BigDecimal unitCost,
                            MovementReferenceType refType, Long refId, String refNumber) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        StockReceiveRequest request = StockReceiveRequest.builder()
                .locationId(locationId)
                .referenceType(refType)
                .referenceId(refId)
                .referenceNumber(refNumber)
                .notes("Return from sale")
                .items(List.of(StockReceiveRequest.ReceiveItem.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .unitCost(unitCost)
                        .build()))
                .build();

        receiveStock(request);
    }

    /**
     * Check if sufficient stock is available
     */
    @Transactional(readOnly = true)
    public boolean isStockAvailable(Long productId, Long locationId, BigDecimal quantity) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BigDecimal available = stockRepository.getAvailableQuantityByProduct(productId, tenantId);
        return available != null && available.compareTo(quantity) >= 0;
    }

    /**
     * Get available quantity for a product across all locations
     */
    @Transactional(readOnly = true)
    public BigDecimal getAvailableQuantity(Long productId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BigDecimal available = stockRepository.getAvailableQuantityByProduct(productId, tenantId);
        return available != null ? available : BigDecimal.ZERO;
    }

    /**
     * Convenience method to deduct stock using string reference type (for POS module)
     */
    @Transactional
    public void deductStock(Long productId, Long locationId, BigDecimal quantity,
                            String referenceType, Long referenceId, String description) {
        MovementReferenceType refType = MovementReferenceType.valueOf(referenceType);
        String refNumber = referenceType + "-" + referenceId;
        deductStockForSale(productId, locationId, quantity, refType, referenceId, refNumber);
    }

    /**
     * Convenience method to add stock using string reference type (for POS void/returns)
     */
    @Transactional
    public void addStock(Long productId, Long locationId, BigDecimal quantity,
                         String referenceType, Long referenceId, String description) {
        MovementReferenceType refType = MovementReferenceType.valueOf(referenceType);
        returnStock(productId, locationId, quantity, null, refType, referenceId, referenceType + "-" + referenceId);
    }
}
