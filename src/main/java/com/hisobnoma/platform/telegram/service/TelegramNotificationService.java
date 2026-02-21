package com.hisobnoma.platform.telegram.service;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.mobile.entity.MobileAlert;
import com.hisobnoma.platform.mobile.repository.AlertPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for sending alert notifications via Telegram.
 * Works alongside PushNotificationService as an additional notification channel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {

    private final TelegramApiClient telegramApi;
    private final UserRepository userRepository;
    private final AlertPreferenceRepository preferenceRepository;

    /**
     * Send alert to a single user via Telegram if they have it enabled.
     */
    @Async
    public void sendAlert(Long userId, Long tenantId, MobileAlert.AlertType alertType,
                          String title, String message) {
        if (!isTelegramEnabledForUser(userId, tenantId, alertType)) {
            return;
        }

        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || userOpt.get().getTelegramChatId() == null) {
            return;
        }

        sendFormattedMessage(userOpt.get().getTelegramChatId(), title, message, alertType);
    }

    /**
     * Send alert to all users in a tenant who have Telegram enabled for this alert type.
     */
    @Async
    public void sendBroadcastAlert(Long tenantId, MobileAlert.AlertType alertType,
                                    String title, String message) {
        List<Long> userIds = preferenceRepository.findUserIdsWithTelegramEnabledForType(tenantId, alertType);

        for (Long userId : userIds) {
            var userOpt = userRepository.findById(userId);
            if (userOpt.isPresent() && userOpt.get().getTelegramChatId() != null) {
                sendFormattedMessage(userOpt.get().getTelegramChatId(), title, message, alertType);
            }
        }
    }

    /**
     * Send directly to a chat ID (for system-level notifications).
     */
    @Async
    public void sendDirectMessage(Long chatId, String title, String message) {
        String text = String.format("<b>%s</b>\n\n%s", title, message);
        telegramApi.sendMessage(chatId, text);
    }

    /**
     * Send low stock alert via Telegram to relevant users.
     */
    @Async
    public void sendLowStockAlert(Long tenantId, String productName, String sku,
                                   int currentStock, int reorderPoint) {
        String title = "Kam qolgan tovar";
        String message = String.format(
                "<b>%s</b> (%s)\nJoriy zaxira: <b>%d</b>\nMinimal: <b>%d</b>",
                productName, sku, currentStock, reorderPoint);

        sendBroadcastAlert(tenantId, MobileAlert.AlertType.LOW_STOCK, title, message);
    }

    /**
     * Send payment received alert via Telegram.
     */
    @Async
    public void sendPaymentAlert(Long userId, Long tenantId, String invoiceNumber, String amount) {
        String title = "To'lov qabul qilindi";
        String message = String.format("Faktura: <b>%s</b>\nMiqdor: <b>%s</b>", invoiceNumber, amount);

        sendAlert(userId, tenantId, MobileAlert.AlertType.PAYMENT_RECEIVED, title, message);
    }

    /**
     * Send credit limit warning via Telegram.
     */
    @Async
    public void sendCreditLimitAlert(Long tenantId, String customerName, String currentBalance,
                                      String creditLimit) {
        String title = "Kredit limiti oshdi";
        String message = String.format(
                "Mijoz: <b>%s</b>\nJoriy qarz: <b>%s</b>\nKredit limiti: <b>%s</b>",
                customerName, currentBalance, creditLimit);

        List<User> users = userRepository.findUsersWithTelegramByTenantId(tenantId);
        for (User user : users) {
            if (user.getTelegramChatId() != null) {
                sendFormattedMessage(user.getTelegramChatId(), title, message,
                        MobileAlert.AlertType.LARGE_TRANSACTION);
            }
        }
    }

    private void sendFormattedMessage(Long chatId, String title, String message,
                                       MobileAlert.AlertType alertType) {
        String emoji = getAlertEmoji(alertType);
        String text = String.format("%s <b>%s</b>\n\n%s", emoji, title, message);
        telegramApi.sendMessage(chatId, text);
    }

    private String getAlertEmoji(MobileAlert.AlertType alertType) {
        return switch (alertType) {
            case LOW_STOCK, OUT_OF_STOCK -> "\u26A0\uFE0F"; // warning
            case PAYMENT_RECEIVED -> "\u2705"; // check
            case PAYMENT_OVERDUE -> "\uD83D\uDD34"; // red circle
            case LARGE_TRANSACTION -> "\uD83D\uDCB0"; // money bag
            case EXPIRING_INVENTORY -> "\u23F0"; // alarm clock
            case ORDER_PLACED -> "\uD83D\uDCE6"; // package
            case ORDER_CANCELLED -> "\u274C"; // cross
            case SYSTEM_ALERT -> "\u2139\uFE0F"; // info
            case APPROVAL_REQUIRED -> "\uD83D\uDD10"; // lock
            default -> "\uD83D\uDD14"; // bell
        };
    }

    private boolean isTelegramEnabledForUser(Long userId, Long tenantId, MobileAlert.AlertType alertType) {
        return preferenceRepository.existsByUserIdAndTenantIdAndAlertTypeAndTelegramEnabledTrue(
                userId, tenantId, alertType);
    }
}
