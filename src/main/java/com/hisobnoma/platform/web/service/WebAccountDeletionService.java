package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebDeviceTokenRepository;
import com.hisobnoma.platform.web.repository.WebLoyaltyTransactionRepository;
import com.hisobnoma.platform.web.repository.WebNotificationRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import com.hisobnoma.platform.web.repository.WebOtpCodeRepository;
import com.hisobnoma.platform.web.repository.WebWishlistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Customer-initiated account deletion for the online shop ({@code DELETE /web/me}).
 * Required by Apple Guideline 5.1.1(v) / Google Play Data Safety.
 *
 * <p>Deletion is immediate (no grace period). It removes everything tied to the
 * person and forfeits the loyalty balance, but keeps completed/cancelled orders
 * for accounting — anonymised and detached so they can't be matched back. An
 * order that is still in flight blocks deletion with {@code ACCOUNT_HAS_ACTIVE_ORDERS}
 * (409), because the courier still needs the contact details.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebAccountDeletionService {

    /** Orders in these states still need the customer's contact info. */
    private static final Set<WebOrderStatus> ACTIVE_STATUSES =
            Set.of(WebOrderStatus.NEW, WebOrderStatus.CONFIRMED, WebOrderStatus.DELIVERING);

    private final WebAuthService authService;
    private final WebCustomerRepository customerRepository;
    private final WebOrderRepository orderRepository;
    private final WebDeviceTokenRepository deviceTokenRepository;
    private final WebWishlistItemRepository wishlistRepository;
    private final WebNotificationRepository notificationRepository;
    private final WebLoyaltyTransactionRepository loyaltyRepository;
    private final WebOtpCodeRepository otpRepository;

    /**
     * Deletes the account identified by the bearer token. The customer id comes
     * from the token only — a caller can never delete someone else's account.
     */
    @Transactional
    public void deleteOwnAccount(String bearerToken) {
        WebCustomer customer = authService.requireCustomer(bearerToken); // 401 if invalid/unknown
        Long tenantId = customer.getTenantId();
        Long customerId = customer.getId();
        String normalizedPhone = WebAuthService.normalizePhone(customer.getPhone());

        long active = orderRepository.countByTenantIdAndPhoneNormalizedAndStatusIn(
                tenantId, normalizedPhone, ACTIVE_STATUSES);
        if (active > 0) {
            throw new BusinessException(
                    "Account has active orders", "ACCOUNT_HAS_ACTIVE_ORDERS", HttpStatus.CONFLICT);
        }

        // Orders are kept for accounting but stripped of personal data and detached
        // from the customer (null phoneNormalized => no longer resurfaces by phone).
        List<WebOrder> orders = orderRepository.findByTenantIdAndPhoneNormalized(tenantId, normalizedPhone);
        for (WebOrder order : orders) {
            order.setCustomerName("Deleted user");
            order.setPhone("deleted");
            order.setPhoneNormalized(null);
            order.setDeliveryAddress(null);
            order.setCustomerNote(null);
            order.setSourceIp(null);
            order.setUserAgent(null);
            order.setCustomerId(null);
        }
        orderRepository.saveAll(orders);

        // Everything below is personal data and is removed outright. Deleting the
        // loyalty ledger forfeits the balance; deleting the customer row makes every
        // existing token stop resolving (subsequent calls get 401).
        deviceTokenRepository.deleteAllByTenantIdAndWebCustomerId(tenantId, customerId);
        wishlistRepository.deleteAllByTenantIdAndWebCustomerId(tenantId, customerId);
        notificationRepository.deleteAllByTenantIdAndWebCustomerId(tenantId, customerId);
        loyaltyRepository.deleteAllByTenantIdAndWebCustomerId(tenantId, customerId);
        otpRepository.deleteAllByTenantIdAndPhone(tenantId, normalizedPhone);
        customerRepository.delete(customer);

        log.info("Web customer account deleted: id={} tenant={} ({} orders anonymised)",
                customerId, tenantId, orders.size());
    }
}
