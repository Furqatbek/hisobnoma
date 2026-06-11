package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.web.dto.WebCustomerDto;
import com.hisobnoma.platform.web.dto.WishlistItemDto;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import com.hisobnoma.platform.web.repository.WebWishlistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff management of online customer accounts, incl. linking them to
 * AR customers so order conversion lands on the existing debtor record.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebCustomerService {

    private final WebCustomerRepository webCustomerRepository;
    private final WebOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final SecurityContextHelper securityContextHelper;
    private final WebPushService pushService;
    private final WebReferralService referralService;
    private final WebWishlistItemRepository wishlistRepository;
    private final WebWishlistService wishlistService;

    @Transactional(readOnly = true)
    public Page<WebCustomerDto> getCustomers(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<WebCustomer> page = (search == null || search.isBlank())
                ? webCustomerRepository.findAllByTenant(tenantId, pageable)
                : webCustomerRepository.searchByTenant(tenantId,
                        "%" + search.toLowerCase().trim() + "%", pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public WebCustomerDto getCustomer(Long id) {
        return toDto(getEntity(id));
    }

    @Transactional
    public WebCustomerDto linkCustomer(Long id, Long customerId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        WebCustomer webCustomer = getEntity(id);
        Customer customer = customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));

        webCustomer.setCustomerId(customer.getId());
        log.info("Linked web customer {} to AR customer {}", id, customer.getCode());
        return toDto(webCustomerRepository.save(webCustomer));
    }

    @Transactional
    public WebCustomerDto unlinkCustomer(Long id) {
        WebCustomer webCustomer = getEntity(id);
        webCustomer.setCustomerId(null);
        return toDto(webCustomerRepository.save(webCustomer));
    }

    @Transactional
    public WebCustomerDto setSmsOptOut(Long id, boolean optOut) {
        WebCustomer webCustomer = getEntity(id);
        webCustomer.setSmsOptOut(optOut);
        log.info("Web customer {} SMS opt-out set to {}", id, optOut);
        return toDto(webCustomerRepository.save(webCustomer));
    }

    @Transactional(readOnly = true)
    public Page<WishlistItemDto> getCustomerWishlist(Long webCustomerId, Pageable pageable) {
        WebCustomer wc = getEntity(webCustomerId);
        return wishlistService.getWishlist(wc.getTenantId(), wc.getId(), pageable);
    }

    private WebCustomer getEntity(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return webCustomerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Web customer not found: " + id));
    }

    private WebCustomerDto toDto(WebCustomer webCustomer) {
        Long tenantId = webCustomer.getTenantId();
        String customerCode = null;
        String customerName = null;
        if (webCustomer.getCustomerId() != null) {
            Customer customer = customerRepository
                    .findByIdAndTenantId(webCustomer.getCustomerId(), tenantId).orElse(null);
            if (customer != null) {
                customerCode = customer.getCode();
                customerName = customer.getName();
            }
        }
        String referredByName = null;
        if (webCustomer.getReferredBy() != null) {
            referredByName = webCustomerRepository
                    .findByIdAndTenantId(webCustomer.getReferredBy(), tenantId)
                    .map(r -> r.getName() != null ? r.getName() : r.getPhone())
                    .orElse(null);
        }

        return WebCustomerDto.builder()
                .id(webCustomer.getId())
                .phone(webCustomer.getPhone())
                .name(webCustomer.getName())
                .verifiedAt(webCustomer.getVerifiedAt())
                .lastLoginAt(webCustomer.getLastLoginAt())
                .customerId(webCustomer.getCustomerId())
                .customerCode(customerCode)
                .customerName(customerName)
                .orderCount(orderRepository.countByTenantIdAndPhoneNormalized(
                        webCustomer.getTenantId(), webCustomer.getPhone()))
                .lastOrderAt(orderRepository.findLastOrderDate(
                        webCustomer.getTenantId(), webCustomer.getPhone()))
                .smsOptOut(webCustomer.isSmsOptOut())
                .referralCode(webCustomer.getReferralCode())
                .referredBy(webCustomer.getReferredBy())
                .referredByName(referredByName)
                .referredCount(referralService.getReferredCount(
                        webCustomer.getTenantId(), webCustomer.getId()))
                .pushReachable(pushService.hasPushToken(
                        webCustomer.getTenantId(), webCustomer.getId()))
                .wishlistCount(wishlistRepository.countByTenantIdAndWebCustomerId(
                        webCustomer.getTenantId(), webCustomer.getId()))
                .build();
    }
}
