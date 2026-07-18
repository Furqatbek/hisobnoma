package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.web.dto.CreatePaymentRequest;
import com.hisobnoma.platform.web.dto.PaymentDto;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.entity.WebPayment;
import com.hisobnoma.platform.web.entity.WebPaymentMethod;
import com.hisobnoma.platform.web.entity.WebPaymentProviderType;
import com.hisobnoma.platform.web.entity.WebPaymentStatus;
import com.hisobnoma.platform.web.exception.PaymentNotConfiguredException;
import com.hisobnoma.platform.web.payment.PaymentProvider;
import com.hisobnoma.platform.web.payment.PaymentProviderRegistry;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import com.hisobnoma.platform.web.repository.WebPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Online card payments for shop orders. Creates a provider checkout session,
 * lets the app poll the opaque payment id, and applies the provider's
 * confirmation (via webhook or staff action). Decoupled from the fulfilment
 * lifecycle: a paid order stays in its current {@link WebOrderStatus} and is
 * still confirmed by staff in their inbox — only {@code paymentStatus} changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebPaymentService {

    private final WebPaymentRepository paymentRepository;
    private final WebOrderRepository orderRepository;
    private final PaymentProviderRegistry providerRegistry;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public PaymentDto createPayment(String orderNumber, CreatePaymentRequest request) {
        Long tenantId = resolveTenantId();
        WebOrder order = orderRepository.findByTenantIdAndOrderNumber(tenantId, orderNumber)
                .filter(o -> WebOrderPublicService.phoneMatches(o.getPhone(), request.getPhone()))
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderNumber));

        if (order.getStatus() == WebOrderStatus.CANCELLED) {
            throw new ValidationException("Cancelled orders cannot be paid");
        }
        if (order.getPaymentStatus() == WebPaymentStatus.PAID) {
            throw new ValidationException("Order is already paid");
        }

        WebPaymentProviderType providerType = WebPaymentProviderType.valueOf(request.getProvider());
        PaymentProvider provider = providerRegistry.get(providerType);
        if (provider == null || !provider.isConfigured()) {
            throw new PaymentNotConfiguredException(
                    "Online payment via " + providerType + " is not available");
        }

        String publicId = "pay_" + HexFormat.of().formatHex(randomBytes(16));
        String returnUrl = httpsOrNull(request.getReturnUrl());

        WebPayment payment = WebPayment.builder()
                .tenantId(tenantId)
                .webOrderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .publicId(publicId)
                .provider(providerType)
                .status(WebPaymentStatus.PENDING)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .build();

        PaymentProvider.Session session = provider.createSession(new PaymentProvider.Context(
                tenantId, order.getOrderNumber(), publicId,
                order.getTotalAmount(), order.getCurrency(), returnUrl));
        payment.setPaymentUrl(session.paymentUrl());
        payment.setProviderReference(session.providerReference());

        // Reflect "awaiting card payment" on the order.
        order.setPaymentMethod(WebPaymentMethod.CARD);
        if (order.getPaymentStatus() != WebPaymentStatus.PENDING) {
            order.setPaymentStatus(WebPaymentStatus.PENDING);
        }
        orderRepository.save(order);

        WebPayment saved = paymentRepository.save(payment);
        log.info("Created {} payment {} for order {} (tenant {})",
                providerType, publicId, order.getOrderNumber(), tenantId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public PaymentDto getStatus(String publicId) {
        Long tenantId = resolveTenantId();
        WebPayment payment = paymentRepository.findByPublicIdAndTenantId(publicId, tenantId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        return toDto(payment);
    }

    /**
     * Marks a payment (and its order) PAID. Called by a provider webhook once
     * it has verified the callback, or by staff confirming a payment. Idempotent.
     */
    @Transactional
    public void markPaid(WebPaymentProviderType provider, String providerReference) {
        paymentRepository.findByProviderAndProviderReference(provider, providerReference)
                .ifPresent(this::applyPaid);
    }

    /**
     * Marks a payment FAILED/CANCELLED. Idempotent; never downgrades a PAID
     * payment.
     */
    @Transactional
    public void markFailed(WebPaymentProviderType provider, String providerReference,
                           boolean cancelled, String reason) {
        paymentRepository.findByProviderAndProviderReference(provider, providerReference)
                .filter(p -> p.getStatus() == WebPaymentStatus.PENDING)
                .ifPresent(p -> {
                    p.setStatus(cancelled ? WebPaymentStatus.CANCELLED : WebPaymentStatus.FAILED);
                    p.setFailureReason(reason);
                    paymentRepository.save(p);
                    orderRepository.findByIdAndTenantId(p.getWebOrderId(), p.getTenantId())
                            .filter(o -> o.getPaymentStatus() == WebPaymentStatus.PENDING)
                            .ifPresent(o -> {
                                o.setPaymentStatus(cancelled
                                        ? WebPaymentStatus.CANCELLED : WebPaymentStatus.FAILED);
                                orderRepository.save(o);
                            });
                });
    }

    private void applyPaid(WebPayment payment) {
        if (payment.getStatus() == WebPaymentStatus.PAID) {
            return;
        }
        payment.setStatus(WebPaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        orderRepository.findByIdAndTenantId(payment.getWebOrderId(), payment.getTenantId())
                .ifPresent(o -> {
                    o.setPaymentStatus(WebPaymentStatus.PAID);
                    orderRepository.save(o);
                });
        log.info("Payment {} marked PAID (order {})", payment.getPublicId(), payment.getOrderNumber());
    }

    // ---- internals ----

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            // Fail closed: never silently attribute a payment to tenant 1.
            throw new BusinessException("X-Tenant-ID header is required", "TENANT_REQUIRED");
        }
        return tenantId;
    }

    /** Only HTTPS return URLs are honoured; anything else is dropped. */
    private static String httpsOrNull(String url) {
        return url != null && url.startsWith("https://") ? url : null;
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    private PaymentDto toDto(WebPayment payment) {
        return PaymentDto.builder()
                .id(payment.getPublicId())
                .paymentUrl(payment.getPaymentUrl())
                .status(payment.getStatus().name())
                .provider(payment.getProvider().name())
                .amount(payment.getAmount())
                .build();
    }
}
