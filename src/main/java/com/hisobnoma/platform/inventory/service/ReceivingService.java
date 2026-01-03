package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.mapper.ReceivingOrderMapper;
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
public class ReceivingService {

    private final ReceivingOrderRepository receivingOrderRepository;
    private final ReceivingLineRepository receivingLineRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final VendorRepository vendorRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final ReceivingOrderMapper receivingOrderMapper;
    private final StockService stockService;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<ReceivingOrderDto> getReceivingOrders(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<ReceivingOrder> page = receivingOrderRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(receivingOrderMapper::toDto));
    }

    @Transactional(readOnly = true)
    public ReceivingOrderDto getReceivingOrder(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        ReceivingOrder ro = receivingOrderRepository.findByIdWithLinesAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Receiving Order not found with id: " + id));
        ReceivingOrderDto dto = receivingOrderMapper.toDto(ro);
        dto.setLines(receivingOrderMapper.toLineDtoList(ro.getLines()));
        return dto;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReceivingOrderDto> getReceivingOrdersByStatus(ReceivingStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<ReceivingOrder> page = receivingOrderRepository.findByStatusAndTenantId(status, tenantId, pageable);
        return PageResponse.of(page.map(receivingOrderMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<ReceivingOrderDto> getReceivingOrdersForPO(Long poId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ReceivingOrder> orders = receivingOrderRepository.findByPurchaseOrderIdAndTenantId(poId, tenantId);
        return receivingOrderMapper.toDtoList(orders);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReceivingOrderDto> searchReceivingOrders(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<ReceivingOrder> page = receivingOrderRepository.searchByTenantId(tenantId, search, pageable);
        return PageResponse.of(page.map(receivingOrderMapper::toDto));
    }

    @Transactional
    public ReceivingOrderDto createReceivingOrder(CreateReceivingRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Vendor vendor = vendorRepository.findByIdAndTenantId(request.getVendorId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Vendor not found with id: " + request.getVendorId()));

        Location location = locationRepository.findByIdAndTenantId(request.getLocationId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Location not found with id: " + request.getLocationId()));

        PurchaseOrder po = null;
        if (request.getPurchaseOrderId() != null) {
            po = purchaseOrderRepository.findByIdAndTenantId(request.getPurchaseOrderId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Purchase Order not found with id: " + request.getPurchaseOrderId()));

            if (po.getStatus() != POStatus.APPROVED && po.getStatus() != POStatus.PARTIAL) {
                throw new BusinessException("Cannot receive against a purchase order with status: " + po.getStatus());
            }
        }

        ReceivingOrder ro = ReceivingOrder.builder()
                .receivingNumber(generateReceivingNumber(tenantId))
                .purchaseOrder(po)
                .vendor(vendor)
                .location(location)
                .status(ReceivingStatus.DRAFT)
                .receivingDate(request.getReceivingDate())
                .vendorDeliveryNote(request.getVendorDeliveryNote())
                .vendorInvoiceNumber(request.getVendorInvoiceNumber())
                .vendorInvoiceDate(request.getVendorInvoiceDate())
                .discountAmount(request.getDiscountAmount())
                .shippingAmount(request.getShippingAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "UZS")
                .notes(request.getNotes())
                .internalNotes(request.getInternalNotes())
                .tenantId(tenantId)
                .build();

        int lineNumber = 1;
        for (CreateReceivingLineRequest lineRequest : request.getLines()) {
            ReceivingLine line = createReceivingLine(lineRequest, lineNumber++, tenantId);
            ro.addLine(line);
        }

        ro.recalculateTotals();
        ro = receivingOrderRepository.save(ro);

        return getReceivingOrder(ro.getId());
    }

    private ReceivingLine createReceivingLine(CreateReceivingLineRequest request, int lineNumber, Long tenantId) {
        Product product = productRepository.findByIdAndTenantId(request.getProductId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProductId()));

        UnitOfMeasure uom = unitOfMeasureRepository.findByIdAndTenantId(request.getUomId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Unit of Measure not found with id: " + request.getUomId()));

        PurchaseOrderLine poLine = null;
        if (request.getPoLineId() != null) {
            poLine = purchaseOrderLineRepository.findByIdAndTenantId(request.getPoLineId(), tenantId)
                    .orElse(null);
        }

        Location targetLocation = null;
        if (request.getTargetLocationId() != null) {
            targetLocation = locationRepository.findByIdAndTenantId(request.getTargetLocationId(), tenantId)
                    .orElse(null);
        }

        BigDecimal acceptedQty = request.getReceivedQuantity()
                .subtract(request.getRejectedQuantity() != null ? request.getRejectedQuantity() : BigDecimal.ZERO);

        return ReceivingLine.builder()
                .poLine(poLine)
                .product(product)
                .lineNumber(lineNumber)
                .description(request.getDescription())
                .expectedQuantity(request.getExpectedQuantity())
                .receivedQuantity(request.getReceivedQuantity())
                .acceptedQuantity(acceptedQty)
                .rejectedQuantity(request.getRejectedQuantity())
                .rejectionReason(request.getRejectionReason())
                .uom(uom)
                .unitCost(request.getUnitCost())
                .taxPercent(request.getTaxPercent())
                .batchNumber(request.getBatchNumber())
                .serialNumbers(request.getSerialNumbers())
                .manufactureDate(request.getManufactureDate())
                .expiryDate(request.getExpiryDate())
                .targetLocation(targetLocation)
                .notes(request.getNotes())
                .tenantId(tenantId)
                .build();
    }

    @Transactional
    public ReceivingOrderDto confirmReceiving(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        ReceivingOrder ro = receivingOrderRepository.findByIdWithLinesAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Receiving Order not found with id: " + id));

        if (ro.getStatus() != ReceivingStatus.DRAFT && ro.getStatus() != ReceivingStatus.PENDING) {
            throw new BusinessException("Only DRAFT or PENDING receiving orders can be confirmed");
        }

        // Update stock for each line
        for (ReceivingLine line : ro.getLines()) {
            if (line.getAcceptedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                Location targetLocation = line.getTargetLocation() != null ? line.getTargetLocation() : ro.getLocation();

                // Receive stock
                StockReceiveRequest stockRequest = StockReceiveRequest.builder()
                        .locationId(targetLocation.getId())
                        .referenceType(MovementReferenceType.RECEIVING_ORDER)
                        .referenceId(ro.getId())
                        .referenceNumber(ro.getReceivingNumber())
                        .notes("Received from " + ro.getVendor().getName())
                        .items(List.of(StockReceiveRequest.ReceiveItem.builder()
                                .productId(line.getProduct().getId())
                                .quantity(line.getAcceptedQuantity())
                                .unitCost(line.getUnitCost())
                                .batchNumber(line.getBatchNumber())
                                .expiryDate(line.getExpiryDate())
                                .build()))
                        .build();

                List<StockMovementDto> movements = stockService.receiveStock(stockRequest);
                if (!movements.isEmpty()) {
                    line.setStockMovementId(movements.get(0).getId());
                }

                // Update PO line if linked
                if (line.getPoLine() != null) {
                    PurchaseOrderLine poLine = line.getPoLine();
                    poLine.setReceivedQuantity(poLine.getReceivedQuantity().add(line.getAcceptedQuantity()));
                    purchaseOrderLineRepository.save(poLine);
                }
            }
        }

        ro.setStatus(ReceivingStatus.COMPLETED);
        ro.setReceivedBy(userId);
        ro.setReceivedAt(Instant.now());
        ro.setStockUpdated(true);

        // Update PO status if linked
        if (ro.getPurchaseOrder() != null) {
            updatePurchaseOrderStatus(ro.getPurchaseOrder());
        }

        ro = receivingOrderRepository.save(ro);
        return receivingOrderMapper.toDto(ro);
    }

    private void updatePurchaseOrderStatus(PurchaseOrder po) {
        if (po.isFullyReceived()) {
            po.setStatus(POStatus.RECEIVED);
            po.setReceivedDate(LocalDate.now());
        } else if (po.isPartiallyReceived()) {
            po.setStatus(POStatus.PARTIAL);
        }
        purchaseOrderRepository.save(po);
    }

    @Transactional
    public ReceivingOrderDto cancelReceiving(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        ReceivingOrder ro = receivingOrderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Receiving Order not found with id: " + id));

        if (ro.getStatus() == ReceivingStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel a completed receiving order");
        }

        ro.setStatus(ReceivingStatus.CANCELLED);
        ro.setCancelledBy(userId);
        ro.setCancelledAt(Instant.now());
        ro.setCancellationReason(reason);

        ro = receivingOrderRepository.save(ro);
        return receivingOrderMapper.toDto(ro);
    }

    private String generateReceivingNumber(Long tenantId) {
        String prefix = "RCV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        String maxNumber = receivingOrderRepository.findMaxReceivingNumberByTenantId(tenantId);

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
