package com.hisobnoma.platform.distribution.b2b.service;

import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.UnauthorizedException;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.distribution.b2b.dto.B2bAuthResponse;
import com.hisobnoma.platform.distribution.b2b.dto.B2bLoginRequest;
import com.hisobnoma.platform.distribution.b2b.dto.B2bProfileDto;
import com.hisobnoma.platform.distribution.b2b.security.B2bCustomerTokenService;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Buyer authentication for the public B2B marketplace. A wholesale customer logs in with
 * their finance-customer {@code code} + registered {@code phone} (both required, tenant-scoped)
 * and receives a dedicated B2B JWT, validated inside the services (the endpoints themselves are
 * anonymous at the Spring Security layer).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class B2bAuthService {

    private final CustomerRepository customerRepository;
    private final B2bCustomerTokenService tokenService;

    /**
     * Tenant for a pre-auth login. Fails closed — a B2B buyer must declare their tenant via the
     * {@code X-Tenant-ID} header; we never silently default to tenant 1 (which would let a
     * headerless or spoofed request operate on the wrong tenant's data). After login every call
     * is tenant-bound by the token itself (see {@link #requireCustomer}), not the header.
     */
    Long resolveTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException("X-Tenant-ID header is required", "TENANT_REQUIRED");
        }
        return tenantId;
    }

    @Transactional(readOnly = true)
    public B2bAuthResponse login(B2bLoginRequest request) {
        Long tenantId = resolveTenantId();
        Customer customer = customerRepository.findByCodeAndTenantId(request.getCode().trim(), tenantId)
                .orElseThrow(() -> new UnauthorizedException("Invalid code or phone"));
        if (!phoneMatches(customer.getPhone(), request.getPhone())) {
            throw new UnauthorizedException("Invalid code or phone");
        }
        String token = tokenService.generateToken(customer.getId(), tenantId, customer.getCode());
        log.info("B2B login: customer {} (tenant {})", customer.getCode(), tenantId);
        return B2bAuthResponse.builder()
                .token(token)
                .customerId(customer.getId())
                .code(customer.getCode())
                .name(customer.getName())
                .currency(customer.getDefaultCurrency())
                .availableCredit(customer.getAvailableCredit())
                .build();
    }

    /** Resolves + tenant-scopes the buyer from a bearer token, or throws Unauthorized. */
    @Transactional(readOnly = true)
    public Customer requireCustomer(String bearerToken) {
        B2bCustomerTokenService.B2bCustomerPrincipal principal = tokenService.parse(stripBearer(bearerToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired token"));
        return customerRepository.findByIdAndTenantId(principal.customerId(), principal.tenantId())
                .orElseThrow(() -> new UnauthorizedException("Unknown customer"));
    }

    @Transactional(readOnly = true)
    public B2bProfileDto getProfile(String bearerToken) {
        Customer c = requireCustomer(bearerToken);
        return B2bProfileDto.builder()
                .customerId(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .phone(c.getPhone())
                .currency(c.getDefaultCurrency())
                .creditLimit(c.getCreditLimit())
                .currentBalance(c.getCurrentBalance())
                .availableCredit(c.getAvailableCredit())
                .paymentTermsDays(c.getPaymentTermsDays())
                .creditHold(c.isCreditHold())
                .build();
    }

    private static boolean phoneMatches(String stored, String provided) {
        if (stored == null || provided == null) {
            return false;
        }
        String a = stored.replaceAll("\\D", "");
        String b = provided.replaceAll("\\D", "");
        return !a.isEmpty() && a.equals(b);
    }

    private static String stripBearer(String header) {
        if (header == null) {
            return "";
        }
        return header.regionMatches(true, 0, "Bearer ", 0, 7) ? header.substring(7).trim() : header.trim();
    }
}
