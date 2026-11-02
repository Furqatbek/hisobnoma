package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CustomerBalanceReportDto;
import com.hisobnoma.platform.finance.service.ARReportService;
import com.hisobnoma.platform.mobile.dto.MobileTelegramShareResult;
import com.hisobnoma.platform.telegram.service.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sends the current debtors (customers with an outstanding AR balance) to Telegram,
 * either to the requesting staff member's own linked chat or to every linked chat in
 * the tenant. Reuses the AR customer-balance report and the staff Telegram links.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileDebtorShareService {

    private static final String TITLE = "Қарздорлар рўйхати";

    private final SecurityContextHelper securityContextHelper;
    private final ARReportService arReportService;
    private final UserRepository userRepository;
    private final TelegramNotificationService telegramNotificationService;

    /**
     * @param target "group" to send to every linked Telegram chat in the tenant;
     *               anything else (default "self") sends only to the caller's chat.
     */
    @Transactional(readOnly = true)
    public MobileTelegramShareResult shareDebtors(String target) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        String mode = (target == null || target.isBlank()) ? "self" : target.trim().toLowerCase();

        List<Long> chatIds = new ArrayList<>();
        if ("group".equals(mode)) {
            Set<Long> unique = new LinkedHashSet<>();
            for (User u : userRepository.findByTenantIdAndTelegramChatIdIsNotNull(tenantId)) {
                if (u.getTelegramChatId() != null) {
                    unique.add(u.getTelegramChatId());
                }
            }
            chatIds.addAll(unique);
            if (chatIds.isEmpty()) {
                throw new BusinessException(
                        "No staff member has linked Telegram yet", "TELEGRAM_NO_RECIPIENTS");
            }
        } else {
            mode = "self";
            Long userId = securityContextHelper.getRequiredCurrentUser().getId();
            User me = userRepository.findByIdAndTenantId(userId, tenantId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            if (me.getTelegramChatId() == null) {
                throw new BusinessException(
                        "Telegram is not linked. Link it from Settings → Telegram first.",
                        "TELEGRAM_NOT_LINKED");
            }
            chatIds.add(me.getTelegramChatId());
        }

        String text = buildDebtorsText();
        int sent = 0;
        for (Long chatId : chatIds) {
            try {
                telegramNotificationService.sendDirectMessage(chatId, TITLE, text);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send debtors list to chatId={}: {}", chatId, e.getMessage());
            }
        }
        log.info("Debtors list shared to Telegram: target={} recipients={} sent={} (tenant {})",
                mode, chatIds.size(), sent, tenantId);
        return MobileTelegramShareResult.builder()
                .target(mode).recipients(chatIds.size()).sent(sent).build();
    }

    private String buildDebtorsText() {
        CustomerBalanceReportDto report = arReportService.generateCustomerBalanceReport();
        List<CustomerBalanceReportDto.CustomerBalanceDto> debtors = report.getCustomerBalances().stream()
                .filter(b -> b.getNetBalance() != null && b.getNetBalance().signum() > 0)
                .sorted(Comparator.comparing(CustomerBalanceReportDto.CustomerBalanceDto::getNetBalance).reversed())
                .toList();

        if (debtors.isEmpty()) {
            return "Қарздорлар йўқ.";
        }

        StringBuilder sb = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        int i = 1;
        for (CustomerBalanceReportDto.CustomerBalanceDto d : debtors) {
            sb.append(i++).append(". ")
              .append(d.getCustomerName() != null ? d.getCustomerName() : "—")
              .append(" — ").append(formatAmount(d.getNetBalance())).append('\n');
            total = total.add(d.getNetBalance());
        }
        sb.append("\nЖами: ").append(formatAmount(total))
          .append(" (").append(debtors.size()).append(" та)");
        return sb.toString();
    }

    private static String formatAmount(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0", symbols).format(amount);
    }
}
