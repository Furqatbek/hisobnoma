package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.mapper.PurchaseOrderMapper;
import com.hisobnoma.platform.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final VendorRepository vendorRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderDto> getPurchaseOrders(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<PurchaseOrder> page = purchaseOrderRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(purchaseOrderMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrder(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLinesAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Purchase Order not found with id: " + id));
        PurchaseOrderDto dto = purchaseOrderMapper.toDto(po);
        dto.setLines(purchaseOrderMapper.toLineDtoList(po.getLines()));
        return dto;
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderDto> getPurchaseOrdersByStatus(POStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<PurchaseOrder> page = purchaseOrderRepository.findByStatusAndTenantId(status, tenantId, pageable);
        return PageResponse.of(page.map(purchaseOrderMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderDto> getPurchaseOrdersByVendor(Long vendorId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<PurchaseOrder> page = purchaseOrderRepository.findByVendorIdAndTenantId(vendorId, tenantId, pageable);
        return PageResponse.of(page.map(purchaseOrderMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderDto> searchPurchaseOrders(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<PurchaseOrder> page = purchaseOrderRepository.searchByTenantId(tenantId, search, pageable);
        return PageResponse.of(page.map(purchaseOrderMapper::toDto));
    }

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(CreatePurchaseOrderRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Vendor vendor = vendorRepository.findByIdAndTenantId(request.getVendorId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Vendor not found with id: " + request.getVendorId()));

        Location location = locationRepository.findByIdAndTenantId(request.getLocationId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Location not found with id: " + request.getLocationId()));

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(generatePoNumber(tenantId))
                .vendor(vendor)
                .location(location)
                .status(POStatus.DRAFT)
                .orderDate(request.getOrderDate())
                .expectedDate(request.getExpectedDate())
                .discountPercent(request.getDiscountPercent())
                .discountAmount(request.getDiscountAmount())
                .shippingAmount(request.getShippingAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "UZS")
                .paymentTerms(request.getPaymentTerms())
                .shippingMethod(request.getShippingMethod())
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .internalNotes(request.getInternalNotes())
                .vendorReference(request.getVendorReference())
                .tenantId(tenantId)
                .build();

        int lineNumber = 1;
        for (CreatePurchaseOrderLineRequest lineRequest : request.getLines()) {
            PurchaseOrderLine line = createPoLine(lineRequest, lineNumber++, tenantId);
            po.addLine(line);
        }

        po.recalculateTotals();
        po = purchaseOrderRepository.save(po);

        return getPurchaseOrder(po.getId());
    }

    private PurchaseOrderLine createPoLine(CreatePurchaseOrderLineRequest request, int lineNumber, Long tenantId) {
        Product product = productRepository.findByIdAndTenantId(request.getProductId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProductId()));

        UnitOfMeasure uom = unitOfMeasureRepository.findByIdAndTenantId(request.getUomId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Unit of Measure not found with id: " + request.getUomId()));

        PurchaseOrderLine line = PurchaseOrderLine.builder()
                .product(product)
                .lineNumber(lineNumber)
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .uom(uom)
                .unitPrice(request.getUnitPrice())
                .discountPercent(request.getDiscountPercent())
                .taxPercent(request.getTaxPercent())
                .notes(request.getNotes())
                .expectedDate(request.getExpectedDate())
                .tenantId(tenantId)
                .build();

        line.calculateLineTotal();
        return line;
    }

    @Transactional
    public PurchaseOrderDto approvePurchaseOrder(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        PurchaseOrder po = purchaseOrderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Purchase Order not found with id: " + id));

        if (po.getStatus() != POStatus.DRAFT && po.getStatus() != POStatus.PENDING) {
            throw new BusinessException("Only DRAFT or PENDING purchase orders can be approved");
        }

        po.setStatus(POStatus.APPROVED);
        po.setApprovedBy(userId);
        po.setApprovedAt(Instant.now());

        po = purchaseOrderRepository.save(po);
        return purchaseOrderMapper.toDto(po);
    }

    @Transactional
    public PurchaseOrderDto cancelPurchaseOrder(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        PurchaseOrder po = purchaseOrderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Purchase Order not found with id: " + id));

        if (po.getStatus() == POStatus.RECEIVED || po.getStatus() == POStatus.CLOSED) {
            throw new BusinessException("Cannot cancel a received or closed purchase order");
        }

        po.setStatus(POStatus.CANCELLED);
        po.setCancelledBy(userId);
        po.setCancelledAt(Instant.now());
        po.setCancellationReason(reason);

        po = purchaseOrderRepository.save(po);
        return purchaseOrderMapper.toDto(po);
    }

    @Transactional
    public PurchaseOrderDto submitForApproval(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        PurchaseOrder po = purchaseOrderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Purchase Order not found with id: " + id));

        if (po.getStatus() != POStatus.DRAFT) {
            throw new BusinessException("Only DRAFT purchase orders can be submitted for approval");
        }

        po.setStatus(POStatus.PENDING);
        po = purchaseOrderRepository.save(po);
        return purchaseOrderMapper.toDto(po);
    }

    private String generatePoNumber(Long tenantId) {
        String prefix = "PO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        String maxPoNumber = purchaseOrderRepository.findMaxPoNumberByTenantId(tenantId);

        int sequence = 1;
        if (maxPoNumber != null && maxPoNumber.startsWith(prefix)) {
            try {
                sequence = Integer.parseInt(maxPoNumber.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                // Use default sequence
            }
        }

        return prefix + String.format("%04d", sequence);
    }
}
