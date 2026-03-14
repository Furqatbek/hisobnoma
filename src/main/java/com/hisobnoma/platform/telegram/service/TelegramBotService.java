package com.hisobnoma.platform.telegram.service;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.telegram.config.TelegramProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telegram bot service that handles incoming messages via long-polling.
 * <p>
 * User linking flow:
 * 1. User generates a 6-digit link code via the web UI (POST /api/v1/telegram/link-code)
 * 2. User sends the code to the bot in Telegram
 * 3. Bot validates the code and links the Telegram chat to the user account
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TelegramApiClient telegramApi;
    private final UserRepository userRepository;
    private final TelegramProperties properties;

    /**
     * Pending link codes: code -> userId.
     * Codes expire after 10 minutes (checked in verification).
     */
    private final ConcurrentHashMap<String, LinkRequest> pendingLinks = new ConcurrentHashMap<>();

    private Long lastUpdateId = null;

    record LinkRequest(Long userId, Instant createdAt) {
        boolean isExpired() {
            return Instant.now().isAfter(createdAt.plusSeconds(600)); // 10 min
        }
    }

    /**
     * Poll for Telegram updates every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void pollUpdates() {
        if (!telegramApi.isConfigured()) return;
        try {
            Map<String, Object> response = telegramApi.getUpdates(lastUpdateId);
            if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> updates = (List<Map<String, Object>>) response.get("result");
            if (updates == null || updates.isEmpty()) return;

            for (Map<String, Object> update : updates) {
                lastUpdateId = ((Number) update.get("update_id")).longValue() + 1;
                processUpdate(update);
            }
        } catch (Exception e) {
            log.error("Error polling Telegram updates: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void processUpdate(Map<String, Object> update) {
        try {
            Map<String, Object> message = (Map<String, Object>) update.get("message");
            if (message == null) return;

            Map<String, Object> chat = (Map<String, Object>) message.get("chat");
            Long chatId = ((Number) chat.get("id")).longValue();
            String text = (String) message.get("text");

            if (text == null) return;

            String command = text.trim();

            if (command.equals("/start")) {
                handleStart(chatId);
            } else if (command.equals("/status")) {
                handleStatus(chatId);
            } else if (command.equals("/unlink")) {
                handleUnlink(chatId);
            } else if (command.equals("/help")) {
                handleHelp(chatId);
            } else if (command.matches("\\d{6}")) {
                handleLinkCode(chatId, command);
            } else {
                telegramApi.sendMessage(chatId,
                        "Noma'lum buyruq. /help ni bosing.");
            }
        } catch (Exception e) {
            log.error("Error processing Telegram update: {}", e.getMessage());
        }
    }

    private void handleStart(Long chatId) {
        String msg = """
                <b>Hisobnoma Bot</b>ga xush kelibsiz!

                Bu bot orqali siz bildirishnomalar olasiz:
                - Kam qolgan tovar ogohlantirishlari
                - To'lov qabul qilindi xabarlari
                - Katta tranzaksiya ogohlantirishlari
                - Kredit limiti ogohlantirishlari

                <b>Akkauntingizni ulash uchun:</b>
                1. Hisobnoma web panelida Sozlamalar > Telegram bo'limiga o'ting
                2. "Kod olish" tugmasini bosing
                3. Olingan 6 raqamli kodni shu yerga yuboring

                Bir nechta akkauntni bitta chatga ulash mumkin!

                Buyruqlar: /help /status /unlink""";
        telegramApi.sendMessage(chatId, msg);
    }

    private void handleHelp(Long chatId) {
        String msg = """
                <b>Mavjud buyruqlar:</b>

                /start - Botni boshlash
                /status - Ulash holati
                /unlink - Akkauntni uzish
                /help - Yordam

                <b>Akkauntni ulash:</b> Web paneldan 6 raqamli kod oling va shu yerga yuboring.""";
        telegramApi.sendMessage(chatId, msg);
    }

    @Transactional(readOnly = true)
    public void handleStatus(Long chatId) {
        var users = userRepository.findAllByTelegramChatId(chatId);
        if (!users.isEmpty()) {
            StringBuilder msg = new StringBuilder("<b>Ulangan akkauntlar:</b>\n");
            for (int i = 0; i < users.size(); i++) {
                var user = users.get(i);
                msg.append(String.format("\n%d. <b>%s</b> (%s) — %s",
                        i + 1,
                        user.getFullName(),
                        user.getUsername(),
                        user.getTelegramLinkedAt() != null ? user.getTelegramLinkedAt().toString().substring(0, 10) : "-"));
            }
            telegramApi.sendMessage(chatId, msg.toString());
        } else {
            telegramApi.sendMessage(chatId, "Bu chat hech qanday akkauntga ulanmagan. /start ni bosing.");
        }
    }

    @Transactional
    public void handleUnlink(Long chatId) {
        var users = userRepository.findAllByTelegramChatId(chatId);
        if (!users.isEmpty()) {
            for (User user : users) {
                user.setTelegramChatId(null);
                user.setTelegramLinkedAt(null);
                userRepository.save(user);
            }
            telegramApi.sendMessage(chatId,
                    String.format("%d ta akkaunt muvaffaqiyatli uzildi.", users.size()));
        } else {
            telegramApi.sendMessage(chatId, "Bu chat hech qanday akkauntga ulanmagan.");
        }
    }

    @Transactional
    public void handleLinkCode(Long chatId, String code) {
        LinkRequest request = pendingLinks.get(code);
        if (request == null || request.isExpired()) {
            pendingLinks.remove(code);
            telegramApi.sendMessage(chatId, "Kod noto'g'ri yoki muddati o'tgan. Yangi kod oling.");
            return;
        }

        var userOpt = userRepository.findById(request.userId());
        if (userOpt.isEmpty()) {
            telegramApi.sendMessage(chatId, "Foydalanuvchi topilmadi.");
            pendingLinks.remove(code);
            return;
        }

        User user = userOpt.get();

        // Check if this user is already linked to this chat
        if (chatId.equals(user.getTelegramChatId())) {
            telegramApi.sendMessage(chatId,
                    String.format("Bu akkaunt allaqachon ulangan: <b>%s</b>", user.getFullName()));
            pendingLinks.remove(code);
            return;
        }

        user.setTelegramChatId(chatId);
        user.setTelegramLinkedAt(Instant.now());
        userRepository.save(user);
        pendingLinks.remove(code);

        int linkedCount = userRepository.findAllByTelegramChatId(chatId).size();
        String msg = String.format("Muvaffaqiyatli ulandi! Akkaunt: <b>%s</b>", user.getFullName());
        if (linkedCount > 1) {
            msg += String.format("\n\nBu chatga jami <b>%d</b> ta akkaunt ulangan.", linkedCount);
        }
        telegramApi.sendMessage(chatId, msg);
        log.info("Telegram linked: userId={}, chatId={}, totalLinked={}", user.getId(), chatId, linkedCount);
    }

    /**
     * Generate a 6-digit link code for the given user.
     */
    public String generateLinkCode(Long userId) {
        // Remove any existing codes for this user
        pendingLinks.entrySet().removeIf(e -> e.getValue().userId().equals(userId));

        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        pendingLinks.put(code, new LinkRequest(userId, Instant.now()));
        return code;
    }

    /**
     * Cleanup expired link codes periodically.
     */
    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void cleanupExpiredCodes() {
        pendingLinks.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
