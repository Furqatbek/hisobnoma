package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebLoyaltyTransaction;
import com.hisobnoma.platform.web.entity.WebLoyaltyTransactionType;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebLoyaltyTransactionRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebReferralService {

    static final String SETTING_ENABLED = "referral.enabled";
    static final String SETTING_REWARD_REFERRER = "referral.reward_referrer";
    static final String SETTING_REWARD_REFERRED = "referral.reward_referred";
    static final String SETTING_MONTHLY_CAP = "referral.monthly_cap";

    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final WebCustomerRepository customerRepository;
    private final WebLoyaltyTransactionRepository loyaltyRepository;
    private final WebOrderRepository orderRepository;
    private final TenantSettingService tenantSettingService;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public String getOrCreateCode(Long tenantId, Long webCustomerId) {
        WebCustomer customer = customerRepository.findByIdAndTenantId(webCustomerId, tenantId)
                .orElse(null);
        if (customer == null) return null;

        if (customer.getReferralCode() != null) {
            return customer.getReferralCode();
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            String code = generateCode();
            if (!customerRepository.existsByReferralCode(code)) {
                customer.setReferralCode(code);
                customerRepository.save(customer);
                log.info("Generated referral code {} for customer {} (tenant {})",
                        code, webCustomerId, tenantId);
                return code;
            }
        }
        log.warn("Failed to generate unique referral code after 10 attempts");
        return null;
    }

    @Transactional
    public void applyCode(Long tenantId, Long newCustomerId, String code) {
        if (!isEnabled(tenantId) || code == null || code.isBlank()) return;

        String normalizedCode = code.trim().toUpperCase();
        WebCustomer referrer = customerRepository.findByReferralCode(normalizedCode)
                .orElse(null);
        if (referrer == null) {
            log.debug("Referral code {} not found — ignoring", normalizedCode);
            return;
        }

        if (!referrer.getTenantId().equals(tenantId)) {
            return;
        }

        WebCustomer newCustomer = customerRepository.findByIdAndTenantId(newCustomerId, tenantId)
                .orElse(null);
        if (newCustomer == null) return;

        if (referrer.getId().equals(newCustomerId)) {
            log.debug("Self-referral rejected for customer {}", newCustomerId);
            return;
        }

        if (newCustomer.getReferredBy() != null) {
            log.debug("Customer {} already referred — ignoring code {}", newCustomerId, normalizedCode);
            return;
        }

        newCustomer.setReferredBy(referrer.getId());
        customerRepository.save(newCustomer);
        log.info("Customer {} referred by {} via code {} (tenant {})",
                newCustomerId, referrer.getId(), normalizedCode, tenantId);
    }

    @Transactional
    public void rewardIfFirstCompletion(WebOrder order) {
        Long tenantId = order.getTenantId();
        if (!isEnabled(tenantId)) return;

        String phone = order.getPhoneNormalized();
        if (phone == null) return;

        WebCustomer customer = customerRepository.findByTenantIdAndPhone(tenantId, phone)
                .orElse(null);
        if (customer == null || customer.getReferredBy() == null) return;

        long completedCount = orderRepository.countByTenantIdAndPhoneNormalizedAndStatus(
                tenantId, phone, WebOrderStatus.COMPLETED);
        if (completedCount != 1) {
            return;
        }

        WebCustomer referrer = customerRepository.findByIdAndTenantId(customer.getReferredBy(), tenantId)
                .orElse(null);
        if (referrer == null) return;

        String idempotencyNote = "Реферал: мижоз #" + customer.getId();
        boolean alreadyRewarded = loyaltyRepository.existsByTenantIdAndWebCustomerIdAndTypeAndNote(
                tenantId, referrer.getId(), WebLoyaltyTransactionType.ADJUST, idempotencyNote);
        if (alreadyRewarded) {
            log.debug("Referral reward already given for referrer {} referring customer {}",
                    referrer.getId(), customer.getId());
            return;
        }

        if (isOverMonthlyCap(tenantId, referrer.getId())) {
            log.info("Referrer {} hit monthly cap — skipping reward (tenant {})",
                    referrer.getId(), tenantId);
            return;
        }

        BigDecimal rewardReferrer = getRewardReferrer(tenantId);
        BigDecimal rewardReferred = getRewardReferred(tenantId);

        if (rewardReferrer.compareTo(BigDecimal.ZERO) > 0) {
            loyaltyRepository.save(WebLoyaltyTransaction.builder()
                    .tenantId(tenantId)
                    .webCustomerId(referrer.getId())
                    .type(WebLoyaltyTransactionType.ADJUST)
                    .amount(rewardReferrer)
                    .note(idempotencyNote)
                    .build());
            log.info("Referral reward {} for referrer {} (referred customer {})",
                    rewardReferrer, referrer.getId(), customer.getId());
        }

        if (rewardReferred.compareTo(BigDecimal.ZERO) > 0) {
            String referredNote = "Реферал бонуси";
            loyaltyRepository.save(WebLoyaltyTransaction.builder()
                    .tenantId(tenantId)
                    .webCustomerId(customer.getId())
                    .type(WebLoyaltyTransactionType.ADJUST)
                    .amount(rewardReferred)
                    .note(referredNote)
                    .build());
            log.info("Referral reward {} for referred customer {}", rewardReferred, customer.getId());
        }
    }

    @Transactional(readOnly = true)
    public long getReferredCount(Long tenantId, Long webCustomerId) {
        return customerRepository.countByTenantIdAndReferredBy(tenantId, webCustomerId);
    }

    // ---- settings ----

    boolean isEnabled(Long tenantId) {
        return "true".equalsIgnoreCase(
                tenantSettingService.getSettingValue(tenantId, SETTING_ENABLED));
    }

    BigDecimal getRewardReferrer(Long tenantId) {
        return parseBigDecimal(
                tenantSettingService.getSettingValue(tenantId, SETTING_REWARD_REFERRER), BigDecimal.ZERO);
    }

    BigDecimal getRewardReferred(Long tenantId) {
        return parseBigDecimal(
                tenantSettingService.getSettingValue(tenantId, SETTING_REWARD_REFERRED), BigDecimal.ZERO);
    }

    int getMonthlyCap(Long tenantId) {
        String val = tenantSettingService.getSettingValue(tenantId, SETTING_MONTHLY_CAP);
        if (val == null) return 0;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 0; }
    }

    private boolean isOverMonthlyCap(Long tenantId, Long referrerId) {
        int cap = getMonthlyCap(tenantId);
        if (cap <= 0) return false;

        YearMonth ym = YearMonth.now();
        Instant monthStart = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant monthEnd = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        long count = loyaltyRepository.countByTenantIdAndWebCustomerIdAndTypeAndNoteStartingWithAndCreatedAtBetween(
                tenantId, referrerId, WebLoyaltyTransactionType.ADJUST, "Реферал: мижоз #",
                monthStart, monthEnd);
        return count >= cap;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    private static BigDecimal parseBigDecimal(String val, BigDecimal defaultVal) {
        if (val == null) return defaultVal;
        try { return new BigDecimal(val); } catch (NumberFormatException e) { return defaultVal; }
    }
}
