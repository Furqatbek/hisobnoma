package com.hisobnoma.platform.telegram.service;

import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.APInvoiceStatus;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import com.hisobnoma.platform.finance.repository.APInvoiceRepository;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Sends daily business reports via Telegram to all connected users.
 * Runs every minute to check if it's time to send for each tenant (configurable time).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramDailyReportService {

    private final TelegramApiClient telegramApi;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantSettingService tenantSettingService;
    private final POSTransactionRepository transactionRepository;
    private final StockRepository stockRepository;
    private final ARInvoiceRepository arInvoiceRepository;
    private final APInvoiceRepository apInvoiceRepository;
    private final BankAccountRepository bankAccountRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final List<ARInvoiceStatus> AR_EXCLUDED = List.of(ARInvoiceStatus.CANCELLED, ARInvoiceStatus.PAID);
    private static final List<APInvoiceStatus> AP_EXCLUDED = List.of(APInvoiceStatus.CANCELLED, APInvoiceStatus.PAID);

    private static final String SETTING_REPORT_ENABLED = "telegram.daily_report.enabled";
    private static final String SETTING_REPORT_TIME = "telegram.daily_report.time";
    private static final String SETTING_REPORT_SALES = "telegram.daily_report.sales";
    private static final String SETTING_REPORT_INVENTORY = "telegram.daily_report.inventory";
    private static final String SETTING_REPORT_FINANCE = "telegram.daily_report.finance";

    /**
     * Track which tenants have already received their report today to avoid duplicates.
     */
    private final Map<Long, LocalDate> lastSentDate = new HashMap<>();

    @Scheduled(cron = "0 * * * * *") // every minute
    @Transactional(readOnly = true)
    public void sendDailyReports() {
        if (!telegramApi.isConfigured()) return;

        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        tenantRepository.findAll().forEach(tenant -> {
            try {
                Long tenantId = tenant.getId();

                if (!getReportBoolSetting(tenantId, SETTING_REPORT_ENABLED, true)) return;

                LocalTime reportTime = parseReportTime(getReportStrSetting(tenantId, SETTING_REPORT_TIME, "20:00"));
                if (!now.equals(reportTime)) return;

                // Skip if already sent today
                LocalDate today = LocalDate.now();
                if (today.equals(lastSentDate.get(tenantId))) return;

                sendReportForTenant(tenantId);
                lastSentDate.put(tenantId, today);
            } catch (Exception e) {
                log.error("Failed to send daily report for tenantId={}: {}", tenant.getId(), e.getMessage());
            }
        });
    }

    private void sendReportForTenant(Long tenantId) {
        List<User> users = userRepository.findUsersWithTelegramByTenantId(tenantId);
        if (users.isEmpty()) return;

        boolean showSales = getReportBoolSetting(tenantId, SETTING_REPORT_SALES, true);
        boolean showInventory = getReportBoolSetting(tenantId, SETTING_REPORT_INVENTORY, true);
        boolean showFinance = getReportBoolSetting(tenantId, SETTING_REPORT_FINANCE, true);

        String report = buildDailyReport(tenantId, showSales, showInventory, showFinance);

        // Group users by chatId to avoid sending duplicate reports to the same chat
        Map<Long, List<User>> chatUsers = new LinkedHashMap<>();
        for (User user : users) {
            if (user.getTelegramChatId() != null) {
                chatUsers.computeIfAbsent(user.getTelegramChatId(), k -> new ArrayList<>()).add(user);
            }
        }

        for (Long chatId : chatUsers.keySet()) {
            try {
                telegramApi.sendMessage(chatId, report);
            } catch (Exception e) {
                log.error("Failed to send daily report to chatId={}: {}", chatId, e.getMessage());
            }
        }

        log.info("Daily report sent to {} chats for tenantId={}", chatUsers.size(), tenantId);
    }

    private String buildDailyReport(Long tenantId, boolean showSales, boolean showInventory, boolean showFinance) {
        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\uD83D\uDCCA <b>Kunlik hisobot — %s</b>", today.format(DATE_FMT)));

        if (showSales) {
            appendSalesSection(sb, tenantId, today, zone);
        }

        if (showInventory) {
            appendInventorySection(sb, tenantId);
        }

        if (showFinance) {
            appendFinanceSection(sb, tenantId);
        }

        return sb.toString();
    }

    private void appendSalesSection(StringBuilder sb, Long tenantId, LocalDate today, ZoneId zone) {
        LocalDate yesterday = today.minusDays(1);
        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant yesterdayStart = yesterday.atStartOfDay(zone).toInstant();

        BigDecimal todayRevenue = safe(transactionRepository.sumCompletedSalesByDateRange(tenantId, todayStart, todayEnd));
        BigDecimal yesterdayRevenue = safe(transactionRepository.sumCompletedSalesByDateRange(tenantId, yesterdayStart, todayStart));
        Integer todayCount = transactionRepository.countCompletedSalesByDateRange(tenantId, todayStart, todayEnd);
        Integer yesterdayCount = transactionRepository.countCompletedSalesByDateRange(tenantId, yesterdayStart, todayStart);
        int txCount = todayCount != null ? todayCount : 0;
        int yTxCount = yesterdayCount != null ? yesterdayCount : 0;

        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        BigDecimal monthRevenue = safe(transactionRepository.sumCompletedSalesByDateRange(tenantId, monthStart, todayEnd));
        Integer monthCount = transactionRepository.countCompletedSalesByDateRange(tenantId, monthStart, todayEnd);

        sb.append("\n\n\uD83D\uDCB0 <b>Savdo</b>\n");
        sb.append(String.format("Bugungi tushum: <b>%s</b>", formatMoney(todayRevenue)));
        String changeStr = changeIndicator(todayRevenue, yesterdayRevenue);
        if (!changeStr.isEmpty()) {
            sb.append("  ").append(changeStr);
        }
        sb.append(String.format("\nTranzaksiyalar: <b>%d</b>", txCount));
        if (txCount > 0) {
            BigDecimal avg = todayRevenue.divide(new BigDecimal(txCount), 0, RoundingMode.HALF_UP);
            sb.append(String.format(" (o'rtacha: %s)", formatMoney(avg)));
        }
        sb.append(String.format("\nKechagi tushum: %s (%d ta)", formatMoney(yesterdayRevenue), yTxCount));
        sb.append(String.format("\nOy boshidan: <b>%s</b> (%d ta)", formatMoney(monthRevenue), monthCount != null ? monthCount : 0));
    }

    private void appendInventorySection(StringBuilder sb, Long tenantId) {
        Integer lowStock = stockRepository.countLowStockProducts(tenantId);
        Integer outOfStock = stockRepository.countOutOfStockProducts(tenantId);
        BigDecimal inventoryValue = safe(stockRepository.calculateTotalInventoryValue(tenantId));

        sb.append("\n\n\uD83D\uDCE6 <b>Ombor</b>\n");
        sb.append(String.format("Ombor qiymati: %s\n", formatMoney(inventoryValue)));
        if (lowStock != null && lowStock > 0) {
            sb.append(String.format("\u26A0\uFE0F Kam qolgan: <b>%d</b> ta mahsulot\n", lowStock));
        }
        if (outOfStock != null && outOfStock > 0) {
            sb.append(String.format("\uD83D\uDD34 Tugagan: <b>%d</b> ta mahsulot\n", outOfStock));
        }
        if ((lowStock == null || lowStock == 0) && (outOfStock == null || outOfStock == 0)) {
            sb.append("\u2705 Barcha mahsulotlar yetarli\n");
        }
    }

    private void appendFinanceSection(StringBuilder sb, Long tenantId) {
        BigDecimal arBalance = safe(arInvoiceRepository.sumOutstandingBalance(tenantId, AR_EXCLUDED));
        BigDecimal apBalance = safe(apInvoiceRepository.sumOutstandingBalance(tenantId, AP_EXCLUDED));
        BigDecimal bankBalance = safe(bankAccountRepository.sumTotalBalance(tenantId));

        sb.append("\n\uD83C\uDFE6 <b>Moliya</b>\n");
        sb.append(String.format("Bank balansi: <b>%s</b>\n", formatMoney(bankBalance)));
        sb.append(String.format("Debitorlik qarzi: %s\n", formatMoney(arBalance)));
        sb.append(String.format("Kreditorlik qarzi: %s", formatMoney(apBalance)));
    }

    private LocalTime parseReportTime(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (Exception e) {
            return LocalTime.of(20, 0);
        }
    }

    private boolean getReportBoolSetting(Long tenantId, String key, boolean defaultValue) {
        try {
            String val = tenantSettingService.getSettingValue(tenantId, key);
            return val != null ? "true".equalsIgnoreCase(val) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getReportStrSetting(Long tenantId, String key, String defaultValue) {
        try {
            String val = tenantSettingService.getSettingValue(tenantId, key);
            return val != null && !val.isBlank() ? val : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String changeIndicator(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return "";
        BigDecimal change = current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            return "\u2B06\uFE0F +" + change + "%";
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            return "\u2B07\uFE0F " + change + "%";
        }
        return "";
    }

    private String formatMoney(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("uz", "UZ"));
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(0);
        return nf.format(amount) + " so'm";
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
