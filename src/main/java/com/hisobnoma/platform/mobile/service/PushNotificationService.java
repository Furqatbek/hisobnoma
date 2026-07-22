package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.mobile.entity.DeviceToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for sending push notifications via Firebase Cloud Messaging (FCM).
 * This is a placeholder implementation - actual FCM integration requires Firebase Admin SDK.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final DeviceTokenService deviceTokenService;

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    /**
     * Send push notification to a specific user.
     */
    @Async
    public void sendPushNotification(Long userId, String title, String body, String type, Long entityId) {
        List<DeviceToken> devices = deviceTokenService.getActiveDeviceTokens(userId);

        if (devices.isEmpty()) {
            log.debug("No active devices found for user {}", userId);
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", type);
        if (entityId != null) {
            data.put("entityId", entityId.toString());
        }

        for (DeviceToken device : devices) {
            sendToDevice(device.getFcmToken(), title, body, data);
        }
    }

    /**
     * Send push notification to multiple users.
     */
    @Async
    public void sendPushNotificationToUsers(List<Long> userIds, String title, String body,
                                             String type, Long entityId) {
        List<DeviceToken> devices = deviceTokenService.getActiveDeviceTokensForUsers(userIds);

        if (devices.isEmpty()) {
            log.debug("No active devices found for users {}", userIds);
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", type);
        if (entityId != null) {
            data.put("entityId", entityId.toString());
        }

        for (DeviceToken device : devices) {
            sendToDevice(device.getFcmToken(), title, body, data);
        }
    }

    /**
     * Whether FCM delivery is actually configured. While false, {@link #sendToDevice} is a logged
     * no-op — callers that fall back to another channel (e.g. wishlist SMS) MUST check this instead
     * of treating a non-throwing send as delivered.
     */
    public boolean isDeliveryEnabled() {
        return firebaseEnabled;
    }

    /**
     * Send push notification directly to a device token.
     */
    public void sendToDevice(String fcmToken, String title, String body, Map<String, String> data) {
        if (!firebaseEnabled) {
            log.info("Firebase disabled - would send push notification: title='{}', body='{}' to token='{}'",
                    title, body, fcmToken.substring(0, Math.min(20, fcmToken.length())) + "...");
            return;
        }

        try {
            // TODO: Implement actual FCM integration
            // Example using Firebase Admin SDK:
            // Message message = Message.builder()
            //     .setToken(fcmToken)
            //     .setNotification(Notification.builder()
            //         .setTitle(title)
            //         .setBody(body)
            //         .build())
            //     .putAllData(data)
            //     .build();
            // FirebaseMessaging.getInstance().send(message);

            log.info("Push notification sent to device: title='{}'", title);
        } catch (Exception e) {
            log.error("Failed to send push notification to device: {}", e.getMessage());
        }
    }

    /**
     * Send low stock alert to relevant users.
     */
    @Async
    public void sendLowStockAlert(Long tenantId, String productName, String sku, int currentStock, int reorderPoint) {
        String title = "Low Stock Alert";
        String body = String.format("%s (%s) is low on stock. Current: %d, Reorder Point: %d",
                productName, sku, currentStock, reorderPoint);

        log.info("Low stock alert: {}", body);
        // In a real implementation, you would query for users who should receive this alert
        // and send them push notifications
    }

    /**
     * Send large transaction alert.
     */
    @Async
    public void sendLargeTransactionAlert(Long tenantId, Long userId, String transactionNumber,
                                           String amount, String customerName) {
        String title = "Large Transaction Alert";
        String body = String.format("Transaction %s for %s by %s", transactionNumber, amount, customerName);

        sendPushNotification(userId, title, body, "LARGE_TRANSACTION", null);
    }

    /**
     * Send payment received alert.
     */
    @Async
    public void sendPaymentReceivedAlert(Long tenantId, Long userId, String invoiceNumber, String amount) {
        String title = "Payment Received";
        String body = String.format("Payment of %s received for invoice %s", amount, invoiceNumber);

        sendPushNotification(userId, title, body, "PAYMENT_RECEIVED", null);
    }

    /**
     * Send daily summary push notification.
     */
    @Async
    public void sendDailySummary(Long userId, String todayRevenue, int transactionCount, int lowStockItems) {
        String title = "Daily Summary";
        String body = String.format("Revenue: %s | Transactions: %d | Low Stock: %d items",
                todayRevenue, transactionCount, lowStockItems);

        sendPushNotification(userId, title, body, "DAILY_SUMMARY", null);
    }
}
