package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.mobile.service.PushNotificationService;
import com.hisobnoma.platform.web.entity.WebDeviceToken;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {

    private final WebDeviceTokenRepository tokenRepository;
    private final WebCustomerRepository customerRepository;
    private final PushNotificationService pushNotificationService;
    private final WebNotificationService notificationService;

    @Transactional
    public void register(Long tenantId, Long webCustomerId, String token, String platform) {
        WebDeviceToken existing = tokenRepository.findByTenantIdAndToken(tenantId, token)
                .orElse(null);

        if (existing != null) {
            existing.setWebCustomerId(webCustomerId);
            existing.setPlatform(platform);
            existing.setLastSeenAt(Instant.now());
            tokenRepository.save(existing);
        } else {
            tokenRepository.save(WebDeviceToken.builder()
                    .tenantId(tenantId)
                    .webCustomerId(webCustomerId)
                    .token(token)
                    .platform(platform)
                    .lastSeenAt(Instant.now())
                    .build());
        }
        log.info("Registered device token for web customer {} (tenant {})", webCustomerId, tenantId);
    }

    @Transactional
    public void unregister(Long tenantId, Long webCustomerId, String token) {
        if (token != null && !token.isBlank()) {
            tokenRepository.deleteByTenantIdAndWebCustomerIdAndToken(tenantId, webCustomerId, token);
        } else {
            tokenRepository.deleteAllByTenantIdAndWebCustomerId(tenantId, webCustomerId);
        }
    }

    public void sendToCustomer(Long tenantId, Long webCustomerId,
                               String title, String body, Map<String, String> data) {
        notificationService.create(tenantId, webCustomerId, title, body,
                data.getOrDefault("type", "GENERAL"),
                null,
                data.get("orderNumber"));

        List<WebDeviceToken> tokens = tokenRepository.findByTenantIdAndWebCustomerId(tenantId, webCustomerId);
        for (WebDeviceToken dt : tokens) {
            try {
                pushNotificationService.sendToDevice(dt.getToken(), title, body, data);
            } catch (Exception e) {
                log.warn("Failed to push to token {} for customer {}: {}",
                        dt.getToken().substring(0, Math.min(20, dt.getToken().length())),
                        webCustomerId, e.getMessage());
            }
        }
    }

    public void notifyOrderStatusChange(WebOrder order, WebOrderStatus newStatus) {
        Long tenantId = order.getTenantId();
        String phone = order.getPhoneNormalized();
        if (phone == null || phone.isBlank()) return;

        customerRepository.findByTenantIdAndPhone(tenantId, phone).ifPresent(customer -> {
            String title;
            String body;
            switch (newStatus) {
                case CONFIRMED -> {
                    title = "Буюртма тасдиқланди";
                    body = "Буюртма " + order.getOrderNumber() + " қабул қилинди";
                }
                case DELIVERING -> {
                    title = "Буюртма йўлда";
                    body = "Буюртма " + order.getOrderNumber() + " етказиб берилмоқда";
                }
                case COMPLETED -> {
                    title = "Буюртма тугалланди";
                    body = "Буюртма " + order.getOrderNumber() + " муваффақиятли тугалланди";
                }
                default -> { return; }
            }

            try {
                sendToCustomer(tenantId, customer.getId(), title, body,
                        Map.of("type", "ORDER_STATUS", "orderNumber", order.getOrderNumber()));
            } catch (Exception e) {
                log.warn("Push notification failed for order {}: {}", order.getOrderNumber(), e.getMessage());
            }
        });
    }

    @Transactional(readOnly = true)
    public boolean hasPushToken(Long tenantId, Long webCustomerId) {
        return tokenRepository.existsByTenantIdAndWebCustomerId(tenantId, webCustomerId);
    }

    @Transactional(readOnly = true)
    public List<WebDeviceToken> getTokensForCustomers(Long tenantId, List<Long> customerIds) {
        if (customerIds.isEmpty()) return List.of();
        return tokenRepository.findByTenantIdAndWebCustomerIdIn(tenantId, customerIds);
    }
}
