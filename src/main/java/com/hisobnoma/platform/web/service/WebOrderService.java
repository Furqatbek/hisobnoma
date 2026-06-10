package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceLineRequest;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.dto.CreateCustomerRequest;
import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.dto.CustomerDto;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
import com.hisobnoma.platform.finance.service.CustomerService;
import com.hisobnoma.platform.web.dto.UpdateOrderStatusRequest;
import com.hisobnoma.platform.web.dto.WebOrderDto;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderLine;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Staff management of online orders: inbox, status transitions and
 * conversion to AR invoices (debt) via the existing finance services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebOrderService {

    private final WebOrderRepository orderRepository;
    private final SecurityContextHelper securityContextHelper;
    private final CustomerService customerService;
    private final ARInvoiceService arInvoiceService;

    @Transactional(readOnly = true)
    public Page<WebOrderDto> getOrders(WebOrderStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<WebOrder> page = status != null
                ? orderRepository.findByTenantAndStatus(tenantId, status, pageable)
                : orderRepository.findAllByTenant(tenantId, pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public WebOrderDto getOrder(Long id) {
        return toDto(getOrderEntity(id));
    }

    @Transactional(readOnly = true)
    public long getNewOrderCount() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return orderRepository.countByTenantIdAndStatus(tenantId, WebOrderStatus.NEW);
    }

    @Transactional
    public WebOrderDto updateStatus(Long id, UpdateOrderStatusRequest request) {
        WebOrder order = getOrderEntity(id);
        WebOrderStatus target = request.getStatus();

        if (!order.getStatus().canTransitionTo(target)) {
            throw new ValidationException(String.format(
                    "Cannot change order %s from %s to %s",
                    order.getOrderNumber(), order.getStatus(), target));
        }
        if (target == WebOrderStatus.CANCELLED) {
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw new ValidationException("Cancellation reason is required");
            }
            order.setCancellationReason(request.getReason().trim());
        }

        order.setStatus(target);
        log.info("Web order {} status changed to {}", order.getOrderNumber(), target);
        return toDto(orderRepository.save(order));
    }

    /**
     * Converts the order into an AR invoice (debt). Creates an AR customer
     * from the order's name/phone when the order isn't linked to one yet.
     */
    @Transactional
    public WebOrderDto convertToInvoice(Long id) {
        WebOrder order = getOrderEntity(id);

        if (order.getStatus() == WebOrderStatus.CANCELLED) {
            throw new ValidationException("Cancelled orders cannot be converted to an invoice");
        }
        if (order.getArInvoiceId() != null) {
            throw new ValidationException(
                    "Order is already converted to invoice " + order.getArInvoiceNumber());
        }

        Long customerId = order.getCustomerId();
        if (customerId == null) {
            CustomerDto customer = customerService.createCustomer(CreateCustomerRequest.builder()
                    .name(order.getCustomerName())
                    .phone(order.getPhone())
                    .address(buildAddress(order))
                    .build());
            customerId = customer.getId();
            order.setCustomerId(customerId);
            log.info("Created customer {} from web order {}", customer.getCode(), order.getOrderNumber());
        }

        List<CreateARInvoiceLineRequest> lines = order.getLines().stream()
                .map(this::toInvoiceLine)
                .toList();

        ARInvoiceDto invoice = arInvoiceService.createInvoice(CreateARInvoiceRequest.builder()
                .customerId(customerId)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .description("Онлайн буюртма " + order.getOrderNumber())
                .lines(lines)
                .build());

        order.setArInvoiceId(invoice.getId());
        order.setArInvoiceNumber(invoice.getInvoiceNumber());
        log.info("Web order {} converted to invoice {}", order.getOrderNumber(), invoice.getInvoiceNumber());
        return toDto(orderRepository.save(order));
    }

    // ---- internals ----

    private WebOrder getOrderEntity(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Web order not found: " + id));
    }

    private CreateARInvoiceLineRequest toInvoiceLine(WebOrderLine line) {
        return CreateARInvoiceLineRequest.builder()
                .productId(line.getProductId())
                .productName(line.getProductName())
                .description(line.getProductName())
                .quantity(line.getQuantity())
                .unitOfMeasure(line.getUnitName())
                .unitPrice(line.getUnitPrice())
                .build();
    }

    private String buildAddress(WebOrder order) {
        StringBuilder sb = new StringBuilder();
        if (order.getDeliveryRegionName() != null) {
            sb.append(order.getDeliveryRegionName());
        }
        if (order.getDeliveryVillageName() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(order.getDeliveryVillageName());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private WebOrderDto toDto(WebOrder order) {
        return WebOrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .customerName(order.getCustomerName())
                .phone(order.getPhone())
                .deliveryRegionId(order.getDeliveryRegionId())
                .deliveryRegionName(order.getDeliveryRegionName())
                .deliveryVillageId(order.getDeliveryVillageId())
                .deliveryVillageName(order.getDeliveryVillageName())
                .customerNote(order.getCustomerNote())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .customerId(order.getCustomerId())
                .arInvoiceId(order.getArInvoiceId())
                .arInvoiceNumber(order.getArInvoiceNumber())
                .cancellationReason(order.getCancellationReason())
                .createdAt(order.getCreatedAt())
                .lines(order.getLines().stream()
                        .map(l -> WebOrderDto.WebOrderLineDto.builder()
                                .id(l.getId())
                                .productId(l.getProductId())
                                .productName(l.getProductName())
                                .unitName(l.getUnitName())
                                .quantity(l.getQuantity())
                                .unitPrice(l.getUnitPrice())
                                .lineTotal(l.getLineTotal())
                                .build())
                        .toList())
                .build();
    }
}
