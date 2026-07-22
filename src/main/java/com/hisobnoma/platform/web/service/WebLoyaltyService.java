package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.web.dto.LoyaltyAdjustRequest;
import com.hisobnoma.platform.web.dto.LoyaltyBalanceDto;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebLoyaltyTransaction;
import com.hisobnoma.platform.web.entity.WebLoyaltyTransactionType;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebLoyaltyTransactionRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebLoyaltyService {

    static final String SETTING_ENABLED = "loyalty.enabled";
    static final String SETTING_EARN_PERCENT = "loyalty.earn_percent";
    static final String SETTING_EXPIRY_DAYS = "loyalty.expiry_days";
    static final String SETTING_MIN_REDEEM = "loyalty.min_redeem";
    static final String SETTING_MAX_REDEEM_PERCENT = "loyalty.max_redeem_percent_of_order";

    private final WebLoyaltyTransactionRepository loyaltyRepository;
    private final WebCustomerRepository webCustomerRepository;
    private final WebOrderRepository orderRepository;
    private final TenantSettingService tenantSettingService;
    private final SecurityContextHelper securityContextHelper;

    // ---- public (customer-facing) ----

    @Transactional(readOnly = true)
    public LoyaltyBalanceDto getBalance(Long tenantId, Long webCustomerId) {
        boolean enabled = isEnabled(tenantId);
        BigDecimal balance = enabled
                ? loyaltyRepository.balanceByCustomer(tenantId, webCustomerId)
                : BigDecimal.ZERO;

        Page<WebLoyaltyTransaction> entries = loyaltyRepository.findByCustomer(
                tenantId, webCustomerId, PageRequest.of(0, 20));

        return LoyaltyBalanceDto.builder()
                .balance(balance)
                .enabled(enabled)
                .minRedeem(getMinRedeem(tenantId))
                .maxRedeemPercent(getMaxRedeemPercent(tenantId))
                .entries(entries.getContent().stream().map(t -> {
                    String orderNumber = null;
                    if (t.getWebOrderId() != null) {
                        orderNumber = orderRepository.findByIdAndTenantId(t.getWebOrderId(), tenantId)
                                .map(WebOrder::getOrderNumber)
                                .orElse(null);
                    }
                    return LoyaltyBalanceDto.LoyaltyEntryDto.builder()
                            .id(t.getId())
                            .type(t.getType().name())
                            .amount(t.getAmount())
                            .orderNumber(orderNumber)
                            .note(t.getNote())
                            .expiresAt(t.getExpiresAt())
                            .createdAt(t.getCreatedAt())
                            .build();
                }).toList())
                .build();
    }

    // ---- checkout: spend points ----

    @Transactional
    public BigDecimal spend(Long tenantId, Long webCustomerId, Long orderId,
                            BigDecimal pointsRequested, BigDecimal orderGoodsTotal) {
        if (!isEnabled(tenantId)) {
            return BigDecimal.ZERO;
        }
        if (pointsRequested == null || pointsRequested.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal balance = loyaltyRepository.balanceByCustomer(tenantId, webCustomerId);
        BigDecimal minRedeem = getMinRedeem(tenantId);
        int maxPercent = getMaxRedeemPercent(tenantId);

        BigDecimal maxByPercent = orderGoodsTotal
                .multiply(BigDecimal.valueOf(maxPercent))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.DOWN);

        BigDecimal clamped = pointsRequested
                .min(balance)
                .min(maxByPercent)
                .min(orderGoodsTotal);

        if (clamped.compareTo(minRedeem) < 0) {
            return BigDecimal.ZERO;
        }

        loyaltyRepository.save(WebLoyaltyTransaction.builder()
                .tenantId(tenantId)
                .webCustomerId(webCustomerId)
                .webOrderId(orderId)
                .type(WebLoyaltyTransactionType.SPEND)
                .amount(clamped.negate())
                .note("Буюртмага сарфланди")
                .build());

        log.info("Loyalty: customer {} spent {} points on order {} (tenant {})",
                webCustomerId, clamped, orderId, tenantId);
        return clamped;
    }

    // ---- order completion: earn points ----

    @Transactional
    public void earn(WebOrder order) {
        Long tenantId = order.getTenantId();
        if (!isEnabled(tenantId)) {
            return;
        }

        WebCustomer customer = webCustomerRepository
                .findByTenantIdAndPhone(tenantId, order.getPhoneNormalized())
                .orElse(null);
        if (customer == null) {
            log.debug("Loyalty: no web customer for phone {} — skipping earn", order.getPhoneNormalized());
            return;
        }

        int earnPercent = getEarnPercent(tenantId);
        if (earnPercent <= 0) {
            return;
        }

        BigDecimal pointsSpent = order.getPointsSpent() != null ? order.getPointsSpent() : BigDecimal.ZERO;
        BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal earnBase = order.getTotalAmount()
                .subtract(deliveryFee)
                .max(BigDecimal.ZERO);

        BigDecimal earned = earnBase
                .multiply(BigDecimal.valueOf(earnPercent))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.DOWN);

        if (earned.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        int expiryDays = getExpiryDays(tenantId);
        Instant expiresAt = expiryDays > 0
                ? Instant.now().plus(expiryDays, ChronoUnit.DAYS)
                : null;

        try {
            loyaltyRepository.save(WebLoyaltyTransaction.builder()
                    .tenantId(tenantId)
                    .webCustomerId(customer.getId())
                    .webOrderId(order.getId())
                    .type(WebLoyaltyTransactionType.EARN)
                    .amount(earned)
                    .expiresAt(expiresAt)
                    .note("Буюртма тугалланди")
                    .build());
            log.info("Loyalty: customer {} earned {} points from order {} (tenant {})",
                    customer.getId(), earned, order.getOrderNumber(), tenantId);
        } catch (DataIntegrityViolationException e) {
            log.info("Loyalty: earn already recorded for order {} — idempotent skip",
                    order.getOrderNumber());
        }
    }

    // ---- cancellation: reverse spend + earn ----

    @Transactional
    public void reverseOrder(WebOrder order) {
        Long tenantId = order.getTenantId();
        List<WebLoyaltyTransaction> transactions =
                loyaltyRepository.findByTenantIdAndWebOrderId(tenantId, order.getId());

        for (WebLoyaltyTransaction tx : transactions) {
            if (tx.getType() == WebLoyaltyTransactionType.ADJUST) {
                continue;
            }
            loyaltyRepository.save(WebLoyaltyTransaction.builder()
                    .tenantId(tenantId)
                    .webCustomerId(tx.getWebCustomerId())
                    .type(WebLoyaltyTransactionType.ADJUST)
                    .amount(tx.getAmount().negate())
                    .note("Буюртма бекор қилинди: " + order.getOrderNumber())
                    .build());
        }
        if (!transactions.isEmpty()) {
            log.info("Loyalty: reversed {} transactions for order {} (tenant {})",
                    transactions.size(), order.getOrderNumber(), tenantId);
        }
    }

    // ---- staff: manual adjust ----

    @Transactional
    public LoyaltyBalanceDto adjust(Long webCustomerId, LoyaltyAdjustRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        WebCustomer customer = webCustomerRepository.findByIdAndTenantId(webCustomerId, tenantId)
                .orElseThrow(() -> new NotFoundException("Web customer not found: " + webCustomerId));

        loyaltyRepository.save(WebLoyaltyTransaction.builder()
                .tenantId(tenantId)
                .webCustomerId(customer.getId())
                .type(WebLoyaltyTransactionType.ADJUST)
                .amount(request.getAmount())
                .note(request.getReason())
                .createdBy(userId)
                .build());

        log.info("Loyalty: staff {} adjusted {} points for customer {} (tenant {})",
                userId, request.getAmount(), webCustomerId, tenantId);

        return getBalance(tenantId, webCustomerId);
    }

    // ---- staff: get customer loyalty ----

    @Transactional(readOnly = true)
    public LoyaltyBalanceDto getCustomerLoyalty(Long webCustomerId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        webCustomerRepository.findByIdAndTenantId(webCustomerId, tenantId)
                .orElseThrow(() -> new NotFoundException("Web customer not found: " + webCustomerId));
        return getBalance(tenantId, webCustomerId);
    }

    // ---- dashboard: total liability ----

    @Transactional(readOnly = true)
    public BigDecimal getTotalLiability(Long tenantId) {
        try {
            return loyaltyRepository.totalLiability(tenantId);
        } catch (Exception e) {
            log.warn("Failed to get loyalty liability: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ---- scheduled: expire old earns ----

    /**
     * Sole expiry mechanism: writes an explicit EXPIRE debit for each overdue EARN, clamped to the
     * customer's current positive balance so already-spent points are never expired a second time
     * and the balance can never go negative. The processed earn keeps its amount (ledger history)
     * but its expiresAt is cleared so it is not picked up again.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void expirePoints() {
        Instant now = Instant.now();
        List<WebLoyaltyTransaction> expired = loyaltyRepository.findExpiredEarns(now);
        int expiredCount = 0;
        for (WebLoyaltyTransaction earn : expired) {
            BigDecimal balance = loyaltyRepository
                    .balanceByCustomer(earn.getTenantId(), earn.getWebCustomerId());
            BigDecimal toExpire = earn.getAmount().min(balance.max(BigDecimal.ZERO));
            if (toExpire.compareTo(BigDecimal.ZERO) > 0) {
                loyaltyRepository.save(WebLoyaltyTransaction.builder()
                        .tenantId(earn.getTenantId())
                        .webCustomerId(earn.getWebCustomerId())
                        .webOrderId(earn.getWebOrderId())
                        .type(WebLoyaltyTransactionType.EXPIRE)
                        .amount(toExpire.negate())
                        .note("Муддати ўтди")
                        .build());
                expiredCount++;
            }
            earn.setExpiresAt(null); // processed — keep the amount for ledger history
            loyaltyRepository.save(earn);
        }
        if (!expired.isEmpty()) {
            log.info("Loyalty: processed {} overdue earns, {} expire debits written",
                    expired.size(), expiredCount);
        }
    }

    // ---- settings helpers ----

    public boolean isEnabled(Long tenantId) {
        return "true".equalsIgnoreCase(
                tenantSettingService.getSettingValue(tenantId, SETTING_ENABLED));
    }

    int getEarnPercent(Long tenantId) {
        String val = tenantSettingService.getSettingValue(tenantId, SETTING_EARN_PERCENT);
        if (val == null) return 0;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 0; }
    }

    int getExpiryDays(Long tenantId) {
        String val = tenantSettingService.getSettingValue(tenantId, SETTING_EXPIRY_DAYS);
        if (val == null) return 180;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 180; }
    }

    BigDecimal getMinRedeem(Long tenantId) {
        String val = tenantSettingService.getSettingValue(tenantId, SETTING_MIN_REDEEM);
        if (val == null) return BigDecimal.valueOf(5000);
        try { return new BigDecimal(val); } catch (NumberFormatException e) { return BigDecimal.valueOf(5000); }
    }

    int getMaxRedeemPercent(Long tenantId) {
        String val = tenantSettingService.getSettingValue(tenantId, SETTING_MAX_REDEEM_PERCENT);
        if (val == null) return 50;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 50; }
    }
}
