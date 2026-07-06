package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.dto.*;
import com.hisobnoma.platform.distribution.entity.VanLoadout;
import com.hisobnoma.platform.distribution.entity.VanLoadoutLine;
import com.hisobnoma.platform.distribution.entity.VanLoadoutStatus;
import com.hisobnoma.platform.distribution.mapper.VanLoadoutMapper;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.distribution.repository.VanLoadoutRepository;
import com.hisobnoma.platform.inventory.dto.StockTransferRequest;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Van-sales loadout lifecycle: load stock onto a vehicle, sell in the field, then
 * reconcile returns / damages / cash at end of day.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VanLoadoutService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final VanLoadoutRepository loadoutRepository;
    private final VanLoadoutMapper loadoutMapper;
    private final DistributionAgentRepository agentRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final SecurityContextHelper securityContextHelper;
    private final StockService stockService;

    // ---- reads ----

    @Transactional(readOnly = true)
    public PageResponse<VanLoadoutDto> getLoadouts(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<VanLoadout> page = loadoutRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(loadoutMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<VanLoadoutDto> getLoadoutsByStatus(VanLoadoutStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return PageResponse.of(loadoutRepository.findByTenantIdAndStatus(tenantId, status, pageable).map(loadoutMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<VanLoadoutDto> getLoadoutsByAgent(Long agentId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return PageResponse.of(loadoutRepository.findByTenantIdAndAgentId(tenantId, agentId, pageable).map(loadoutMapper::toDto));
    }

    @Transactional(readOnly = true)
    public VanLoadoutDto getLoadout(Long id) {
        return loadoutMapper.toDto(loadObj(id));
    }

    // ---- create / delete ----

    @Transactional
    public VanLoadoutDto createLoadout(CreateVanLoadoutRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        if (agentRepository.findByIdAndTenantId(request.getAgentId(), tenantId).isEmpty()) {
            throw new NotFoundException("DistributionAgent", request.getAgentId());
        }
        Location vehicle = requireLocation(request.getVehicleLocationId(), tenantId);
        if (vehicle.getLocationType() != LocationType.VEHICLE) {
            throw new BusinessException("Loadout target must be a VEHICLE location", "NOT_A_VEHICLE_LOCATION");
        }
        requireLocation(request.getSourceLocationId(), tenantId);

        VanLoadout loadout = VanLoadout.builder()
                .status(VanLoadoutStatus.DRAFT)
                .agentId(request.getAgentId())
                .vehicleLocationId(request.getVehicleLocationId())
                .sourceLocationId(request.getSourceLocationId())
                .loadoutDate(request.getLoadoutDate() != null ? request.getLoadoutDate() : LocalDate.now(ZoneOffset.UTC))
                .notes(request.getNotes())
                .tenantId(tenantId)
                .build();

        for (CreateVanLoadoutRequest.LineRequest lr : request.getLines()) {
            Product product = productRepository.findByIdAndTenantId(lr.getProductId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Product", lr.getProductId()));
            VanLoadoutLine line = VanLoadoutLine.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .unitName(product.getBaseUom() != null ? product.getBaseUom().getName() : null)
                    .quantityLoaded(lr.getQuantityLoaded())
                    .unitCost(nz(product.getCostPrice()))
                    .unitPrice(nz(product.getSellingPrice()))
                    .tenantId(tenantId)
                    .build();
            loadout.addLine(line);
        }
        loadout.setTotalLoadedValue(sumValue(loadout.getLines(), VanLoadoutLine::getQuantityLoaded));
        loadout.setLoadoutNumber(generateNumber(tenantId));
        loadout = loadoutRepository.save(loadout);
        log.info("Created van loadout {} for agent {}", loadout.getLoadoutNumber(), request.getAgentId());
        return loadoutMapper.toDto(loadout);
    }

    @Transactional
    public void deleteLoadout(Long id) {
        VanLoadout loadout = loadObj(id);
        if (loadout.getStatus() != VanLoadoutStatus.DRAFT) {
            throw new BusinessException("Only draft loadouts can be deleted", "LOADOUT_NOT_DELETABLE");
        }
        loadoutRepository.delete(loadout);
    }

    // ---- lifecycle ----

    @Transactional
    public VanLoadoutDto load(Long id) {
        VanLoadout loadout = loadObj(id);
        requireTransition(loadout, VanLoadoutStatus.LOADED);

        // Atomic: warehouse -> vehicle for every line. If any line lacks stock the whole
        // load fails and the status stays DRAFT (you can't load what you don't have).
        List<StockTransferRequest.TransferItem> items = new ArrayList<>();
        for (VanLoadoutLine line : loadout.getLines()) {
            items.add(StockTransferRequest.TransferItem.builder()
                    .productId(line.getProductId())
                    .quantity(line.getQuantityLoaded())
                    .build());
        }
        stockService.transferStock(StockTransferRequest.builder()
                .fromLocationId(loadout.getSourceLocationId())
                .toLocationId(loadout.getVehicleLocationId())
                .referenceNumber(loadout.getLoadoutNumber())
                .reason("Van loadout")
                .items(items)
                .build());

        loadout.setStatus(VanLoadoutStatus.LOADED);
        loadout.setTotalLoadedValue(sumValue(loadout.getLines(), VanLoadoutLine::getQuantityLoaded));
        return loadoutMapper.toDto(loadoutRepository.save(loadout));
    }

    @Transactional
    public VanLoadoutDto reconcile(Long id, ReconcileVanLoadoutRequest request) {
        VanLoadout loadout = loadObj(id);
        requireTransition(loadout, VanLoadoutStatus.RECONCILED);

        // Apply per-line returned/damaged, derive sold, and move stock. All stock moves run
        // in THIS transaction (REQUIRED) together with the status change, so the whole
        // reconciliation is atomic: if any move or the final save fails, nothing is deducted
        // and the loadout stays LOADED — a safe retry, and concurrent reconciles are serialized
        // by the @Version optimistic lock (the loser rolls back its moves too).
        List<StockTransferRequest.TransferItem> returnItems = new ArrayList<>();
        for (VanLoadoutLine line : loadout.getLines()) {
            BigDecimal returned = BigDecimal.ZERO;
            BigDecimal damaged = BigDecimal.ZERO;
            if (request.getLines() != null) {
                var match = request.getLines().stream().filter(l -> l.getLineId().equals(line.getId())).findFirst();
                if (match.isPresent()) {
                    returned = nz(match.get().getQuantityReturned());
                    damaged = nz(match.get().getQuantityDamaged());
                }
            }
            BigDecimal accounted = returned.add(damaged);
            if (accounted.compareTo(line.getQuantityLoaded()) > 0) {
                throw new BusinessException(
                        "Returned + damaged exceeds loaded for product " + line.getProductId(), "RECONCILE_OVER_LOADED");
            }
            BigDecimal sold = line.getQuantityLoaded().subtract(accounted);
            line.setQuantityReturned(returned);
            line.setQuantityDamaged(damaged);
            line.setQuantitySold(sold);

            if (sold.signum() > 0) {
                stockService.deductStockForSale(line.getProductId(), loadout.getVehicleLocationId(), sold,
                        MovementReferenceType.VAN_LOADOUT, loadout.getId(), loadout.getLoadoutNumber());
            }
            if (damaged.signum() > 0) {
                stockService.deductStockForSale(line.getProductId(), loadout.getVehicleLocationId(), damaged,
                        MovementReferenceType.VAN_LOADOUT, loadout.getId(), loadout.getLoadoutNumber());
            }
            if (returned.signum() > 0) {
                returnItems.add(StockTransferRequest.TransferItem.builder()
                        .productId(line.getProductId()).quantity(returned).build());
            }
        }
        if (!returnItems.isEmpty()) {
            stockService.transferStock(StockTransferRequest.builder()
                    .fromLocationId(loadout.getVehicleLocationId())
                    .toLocationId(loadout.getSourceLocationId())
                    .referenceNumber(loadout.getLoadoutNumber())
                    .reason("Van return")
                    .items(returnItems)
                    .build());
        }

        loadout.setTotalSoldValue(sumValue(loadout.getLines(), VanLoadoutLine::getQuantitySold));
        loadout.setTotalReturnedValue(sumValue(loadout.getLines(), VanLoadoutLine::getQuantityReturned));
        loadout.setTotalDamagedValue(sumValue(loadout.getLines(), VanLoadoutLine::getQuantityDamaged));
        loadout.setExpectedCash(loadout.getLines().stream()
                .map(l -> l.getQuantitySold().multiply(nz(l.getUnitPrice())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        loadout.setActualCash(nz(request.getActualCash()));
        loadout.setCashDifference(loadout.getActualCash().subtract(loadout.getExpectedCash()));
        if (request.getNotes() != null) {
            loadout.setNotes(request.getNotes());
        }
        loadout.setStatus(VanLoadoutStatus.RECONCILED);
        loadout.setReconciledAt(Instant.now());
        loadout.setReconciledBy(securityContextHelper.getCurrentUserId());
        log.info("Reconciled van loadout {}: sold {}, cash diff {}",
                loadout.getLoadoutNumber(), loadout.getTotalSoldValue(), loadout.getCashDifference());
        return loadoutMapper.toDto(loadoutRepository.save(loadout));
    }

    @Transactional
    public VanLoadoutDto cancel(Long id) {
        VanLoadout loadout = loadObj(id);
        if (!loadout.getStatus().canTransitionTo(VanLoadoutStatus.CANCELLED)) {
            throw new BusinessException(
                    "Loadout in status " + loadout.getStatus() + " cannot be cancelled", "INVALID_STATUS_TRANSITION");
        }
        boolean wasLoaded = loadout.getStatus() == VanLoadoutStatus.LOADED;
        loadout.setStatus(VanLoadoutStatus.CANCELLED);
        if (wasLoaded) {
            // Nothing was sold — send the whole load back to the warehouse, atomically with
            // the status change (if the return fails, the cancel aborts and stays LOADED).
            List<StockTransferRequest.TransferItem> items = loadout.getLines().stream()
                    .map(l -> StockTransferRequest.TransferItem.builder()
                            .productId(l.getProductId()).quantity(l.getQuantityLoaded()).build())
                    .toList();
            stockService.transferStock(StockTransferRequest.builder()
                    .fromLocationId(loadout.getVehicleLocationId())
                    .toLocationId(loadout.getSourceLocationId())
                    .referenceNumber(loadout.getLoadoutNumber())
                    .reason("Van loadout cancelled")
                    .items(items)
                    .build());
        }
        return loadoutMapper.toDto(loadoutRepository.save(loadout));
    }

    // ---- helpers ----

    private VanLoadout loadObj(Long id) {
        return loadoutRepository.findByIdAndTenantId(id, securityContextHelper.getCurrentTenantId())
                .orElseThrow(() -> new NotFoundException("VanLoadout", id));
    }

    private void requireTransition(VanLoadout loadout, VanLoadoutStatus target) {
        if (!loadout.getStatus().canTransitionTo(target)) {
            throw new BusinessException(
                    "Cannot move loadout from " + loadout.getStatus() + " to " + target, "INVALID_STATUS_TRANSITION");
        }
    }

    private Location requireLocation(Long locationId, Long tenantId) {
        return locationRepository.findByIdAndTenantId(locationId, tenantId)
                .orElseThrow(() -> new NotFoundException("Location", locationId));
    }

    private String generateNumber(Long tenantId) {
        String prefix = "LD" + LocalDate.now(ZoneOffset.UTC).format(DATE) + "-";
        String max = loadoutRepository.findMaxLoadoutNumberByPrefix(tenantId, prefix);
        int next = 1;
        if (max != null) {
            next = Integer.parseInt(max.substring(max.lastIndexOf('-') + 1)) + 1;
        }
        String number = String.format("%s%05d", prefix, next);
        while (loadoutRepository.existsByTenantIdAndLoadoutNumber(tenantId, number)) {
            number = String.format("%s%05d", prefix, ++next);
        }
        return number;
    }

    private interface QtyGetter {
        BigDecimal get(VanLoadoutLine line);
    }

    private BigDecimal sumValue(List<VanLoadoutLine> lines, QtyGetter qty) {
        return lines.stream()
                .map(l -> nz(qty.get(l)).multiply(nz(l.getUnitCost())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
