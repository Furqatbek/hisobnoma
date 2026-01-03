package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.mapper.InventoryCountMapper;
import com.hisobnoma.platform.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryCountService {

    private final InventoryCountRepository inventoryCountRepository;
    private final InventoryCountLineRepository inventoryCountLineRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockRepository stockRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final InventoryCountMapper inventoryCountMapper;
    private final StockService stockService;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<InventoryCountDto> getInventoryCounts(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<InventoryCount> page = inventoryCountRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(inventoryCountMapper::toDto));
    }

    @Transactional(readOnly = true)
    public InventoryCountDto getInventoryCount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        InventoryCount ic = inventoryCountRepository.findByIdWithLinesAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Inventory Count not found with id: " + id));
        InventoryCountDto dto = inventoryCountMapper.toDto(ic);
        dto.setLines(inventoryCountMapper.toLineDtoList(ic.getLines()));
        return dto;
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryCountDto> getInventoryCountsByStatus(CountStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<InventoryCount> page = inventoryCountRepository.findByStatusAndTenantId(status, tenantId, pageable);
        return PageResponse.of(page.map(inventoryCountMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryCountDto> searchInventoryCounts(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<InventoryCount> page = inventoryCountRepository.searchByTenantId(tenantId, search, pageable);
        return PageResponse.of(page.map(inventoryCountMapper::toDto));
    }

    @Transactional
    public InventoryCountDto createInventoryCount(CreateInventoryCountRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Location location = locationRepository.findByIdAndTenantId(request.getLocationId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Location not found with id: " + request.getLocationId()));

        // Check if there's an active count for this location
        List<InventoryCount> activeCounts = inventoryCountRepository.findActiveCountsForLocation(
                request.getLocationId(), tenantId);
        if (!activeCounts.isEmpty()) {
            throw new BusinessException("There is already an active inventory count for this location");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdAndTenantId(request.getCategoryId(), tenantId)
                    .orElse(null);
        }

        InventoryCount ic = InventoryCount.builder()
                .countNumber(generateCountNumber(tenantId))
                .location(location)
                .status(CountStatus.DRAFT)
                .countType(request.getCountType())
                .countDate(request.getCountDate())
                .description(request.getDescription())
                .notes(request.getNotes())
                .freezeStock(request.isFreezeStock())
                .category(category)
                .tenantId(tenantId)
                .build();

        ic = inventoryCountRepository.save(ic);

        // Add products to count
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            addProductsToCount(ic, request.getProductIds(), tenantId);
        } else if (request.getCountType() == CountType.FULL) {
            // For full count, add all products at the location
            addAllProductsAtLocation(ic, location, tenantId);
        } else if (category != null) {
            // Add products from the specified category
            addProductsFromCategory(ic, category, location, tenantId);
        }

        ic.updateSummary();
        ic = inventoryCountRepository.save(ic);

        return getInventoryCount(ic.getId());
    }

    private void addProductsToCount(InventoryCount ic, List<Long> productIds, Long tenantId) {
        int lineNumber = 1;
        for (Long productId : productIds) {
            Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                    .orElse(null);
            if (product != null) {
                addCountLine(ic, product, lineNumber++, tenantId);
            }
        }
    }

    private void addAllProductsAtLocation(InventoryCount ic, Location location, Long tenantId) {
        List<Stock> stocks = stockRepository.findByLocationIdAndTenantId(location.getId(), tenantId);
        int lineNumber = 1;
        for (Stock stock : stocks) {
            addCountLine(ic, stock.getProduct(), lineNumber++, tenantId);
        }
    }

    private void addProductsFromCategory(InventoryCount ic, Category category, Location location, Long tenantId) {
        List<Stock> stocks = stockRepository.findByLocationIdAndTenantId(location.getId(), tenantId);
        int lineNumber = 1;
        for (Stock stock : stocks) {
            if (stock.getProduct().getCategory() != null &&
                stock.getProduct().getCategory().getId().equals(category.getId())) {
                addCountLine(ic, stock.getProduct(), lineNumber++, tenantId);
            }
        }
    }

    private void addCountLine(InventoryCount ic, Product product, int lineNumber, Long tenantId) {
        Stock stock = stockRepository.findByProductIdAndLocationIdAndTenantId(
                product.getId(), ic.getLocation().getId(), tenantId).orElse(null);

        BigDecimal systemQuantity = stock != null ? stock.getQuantityOnHand() : BigDecimal.ZERO;
        BigDecimal unitCost = stock != null ? stock.getAverageCost() : product.getCostPrice();

        InventoryCountLine line = InventoryCountLine.builder()
                .inventoryCount(ic)
                .product(product)
                .lineNumber(lineNumber)
                .systemQuantity(systemQuantity)
                .unitCost(unitCost)
                .uom(product.getBaseUom())
                .tenantId(tenantId)
                .build();

        ic.addLine(line);
    }

    @Transactional
    public InventoryCountDto startCount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        InventoryCount ic = inventoryCountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Inventory Count not found with id: " + id));

        if (ic.getStatus() != CountStatus.DRAFT) {
            throw new BusinessException("Only DRAFT counts can be started");
        }

        ic.setStatus(CountStatus.IN_PROGRESS);
        ic.setStartedAt(Instant.now());

        ic = inventoryCountRepository.save(ic);
        return inventoryCountMapper.toDto(ic);
    }

    @Transactional
    public InventoryCountLineDto recordCount(Long countId, Long lineId, RecordCountRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        InventoryCount ic = inventoryCountRepository.findByIdAndTenantId(countId, tenantId)
                .orElseThrow(() -> new NotFoundException("Inventory Count not found with id: " + countId));

        if (ic.getStatus() != CountStatus.IN_PROGRESS) {
            throw new BusinessException("Counts can only be recorded on IN_PROGRESS inventory counts");
        }

        InventoryCountLine line = inventoryCountLineRepository.findByIdAndTenantId(lineId, tenantId)
                .orElseThrow(() -> new NotFoundException("Count line not found with id: " + lineId));

        if (!line.getInventoryCount().getId().equals(countId)) {
            throw new BusinessException("Line does not belong to the specified count");
        }

        line.recordCount(request.getCountedQuantity(), userId);
        line.setNotes(request.getNotes());
        line.setBatchNumber(request.getBatchNumber());
        line.setSerialNumber(request.getSerialNumber());

        line = inventoryCountLineRepository.save(line);

        // Update count summary
        ic.updateSummary();
        inventoryCountRepository.save(ic);

        return inventoryCountMapper.toLineDto(line);
    }

    @Transactional
    public InventoryCountDto completeCount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        InventoryCount ic = inventoryCountRepository.findByIdWithLinesAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Inventory Count not found with id: " + id));

        if (ic.getStatus() != CountStatus.IN_PROGRESS) {
            throw new BusinessException("Only IN_PROGRESS counts can be completed");
        }

        // Check if all lines are counted
        long uncounted = inventoryCountLineRepository.findUncountedLinesByCountId(id, tenantId).size();
        if (uncounted > 0) {
            throw new BusinessException("Cannot complete count - " + uncounted + " items are not counted yet");
        }

        ic.setStatus(CountStatus.COMPLETED);
        ic.setCompletedAt(Instant.now());
        ic.updateSummary();

        ic = inventoryCountRepository.save(ic);
        return inventoryCountMapper.toDto(ic);
    }

    @Transactional
    public InventoryCountDto approveCount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        InventoryCount ic = inventoryCountRepository.findByIdWithLinesAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Inventory Count not found with id: " + id));

        if (ic.getStatus() != CountStatus.COMPLETED) {
            throw new BusinessException("Only COMPLETED counts can be approved");
        }

        // Post adjustments for variance lines
        for (InventoryCountLine line : ic.getLines()) {
            if (line.hasVariance()) {
                StockAdjustRequest adjustRequest = StockAdjustRequest.builder()
                        .locationId(ic.getLocation().getId())
                        .referenceType(MovementReferenceType.COUNT)
                        .referenceId(ic.getId())
                        .referenceNumber(ic.getCountNumber())
                        .reason("Inventory count adjustment")
                        .items(List.of(StockAdjustRequest.AdjustItem.builder()
                                .productId(line.getProduct().getId())
                                .adjustmentQuantity(line.getVarianceQuantity())
                                .reason(line.getAdjustmentReason() != null ?
                                        line.getAdjustmentReason() : "Count variance")
                                .build()))
                        .build();

                List<StockMovementDto> movements = stockService.adjustStock(adjustRequest);
                if (!movements.isEmpty()) {
                    line.setStockMovementId(movements.get(0).getId());
                }
                line.setAdjustmentApproved(true);
            }
        }

        ic.setStatus(CountStatus.APPROVED);
        ic.setApprovedBy(userId);
        ic.setApprovedAt(Instant.now());
        ic.setAdjustmentsPosted(true);

        ic = inventoryCountRepository.save(ic);
        return inventoryCountMapper.toDto(ic);
    }

    @Transactional
    public InventoryCountDto cancelCount(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        InventoryCount ic = inventoryCountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Inventory Count not found with id: " + id));

        if (ic.getStatus() == CountStatus.APPROVED) {
            throw new BusinessException("Cannot cancel an approved count");
        }

        ic.setStatus(CountStatus.CANCELLED);
        ic.setCancelledBy(userId);
        ic.setCancelledAt(Instant.now());
        ic.setCancellationReason(reason);

        ic = inventoryCountRepository.save(ic);
        return inventoryCountMapper.toDto(ic);
    }

    @Transactional
    public void markForRecount(Long countId, Long lineId, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        InventoryCountLine line = inventoryCountLineRepository.findByIdAndTenantId(lineId, tenantId)
                .orElseThrow(() -> new NotFoundException("Count line not found with id: " + lineId));

        if (!line.getInventoryCount().getId().equals(countId)) {
            throw new BusinessException("Line does not belong to the specified count");
        }

        line.setRecountRequired(true);
        line.setNotes(reason);
        inventoryCountLineRepository.save(line);
    }

    private String generateCountNumber(Long tenantId) {
        String prefix = "CNT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        String maxNumber = inventoryCountRepository.findMaxCountNumberByTenantId(tenantId);

        int sequence = 1;
        if (maxNumber != null && maxNumber.startsWith(prefix)) {
            try {
                sequence = Integer.parseInt(maxNumber.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                // Use default sequence
            }
        }

        return prefix + String.format("%04d", sequence);
    }
}
