package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.exception.UnauthorizedException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.dto.PublicOrderDto;
import com.hisobnoma.platform.web.dto.WebAuthResponse;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOtpCode;
import com.hisobnoma.platform.web.exception.TooManyRequestsException;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import com.hisobnoma.platform.web.repository.WebOtpCodeRepository;
import com.hisobnoma.platform.web.security.WebCustomerTokenService;
import com.hisobnoma.platform.web.security.WebCustomerTokenService.WebCustomerPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phone + SMS OTP login for online shop customers, and the token-scoped
 * "my orders" view. All abuse limits live here:
 * 60 s cooldown between codes, max 5 codes/day/phone, 5 wrong attempts
 * per code, 5-minute expiry, plus a per-IP request limiter.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebAuthService {
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_CODES_PER_DAY = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final WebOtpCodeRepository otpRepository;
    private final WebCustomerRepository webCustomerRepository;
    private final TenantRepository tenantRepository;
    private final WebOrderRepository orderRepository;
    private final WebCustomerTokenService tokenService;
    private final CheckoutRateLimiter rateLimiter;
    private final SmsService smsService;
    private final WebReferralService referralService;
    private final WebLoyaltyService loyaltyService;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void requestOtp(String rawPhone, String sourceIp) {
        Long tenantId = resolveTenantId();
        String phone = normalizePhone(rawPhone);
        if (phone.length() < 9 || phone.length() > 15) {
            throw new ValidationException("Invalid phone number");
        }

        if (!rateLimiter.tryAcquire("otp|" + sourceIp)) {
            throw new TooManyRequestsException("Too many OTP requests, please try again later");
        }

        Instant now = Instant.now();
        otpRepository.findTopByTenantIdAndPhoneOrderByCreatedAtDesc(tenantId, phone)
                .filter(last -> last.getCreatedAt() != null
                        && last.getCreatedAt().isAfter(now.minus(RESEND_COOLDOWN)))
                .ifPresent(last -> {
                    throw new TooManyRequestsException("Please wait before requesting a new code");
                });

        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        if (otpRepository.countByTenantIdAndPhoneAndCreatedAtAfter(tenantId, phone, startOfDay)
                >= MAX_CODES_PER_DAY) {
            throw new TooManyRequestsException("Daily code limit reached, please try again tomorrow");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        String salt = HexFormat.of().formatHex(randomBytes(16));

        otpRepository.save(WebOtpCode.builder()
                .tenantId(tenantId)
                .phone(phone)
                .salt(salt)
                .codeHash(hash(salt, code))
                .expiresAt(now.plus(CODE_TTL))
                .build());

        try {
            smsService.sendSmsAsync("+" + phone, "Дўкон: тасдиқлаш коди " + code);
        } catch (Exception e) {
            // The code stays valid; the user can retry after the cooldown.
            log.warn("Failed to send OTP SMS to {}: {}", mask(phone), e.getMessage());
        }
        log.info("OTP requested for phone {} (tenant {})", mask(phone), tenantId);
    }

    @Transactional
    public WebAuthResponse verifyOtp(String rawPhone, String code, String name, String referralCode) {
        Long tenantId = resolveTenantId();
        String phone = normalizePhone(rawPhone);

        WebOtpCode otp = otpRepository
                .findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(tenantId, phone)
                .filter(c -> c.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ValidationException("Code expired or not requested"));

        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Too many wrong attempts, request a new code");
        }

        if (!otp.getCodeHash().equals(hash(otp.getSalt(), code))) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            throw new ValidationException("Invalid code");
        }

        otp.setUsed(true);
        otpRepository.save(otp);

        Instant now = Instant.now();
        WebCustomer customer = webCustomerRepository.findByTenantIdAndPhone(tenantId, phone)
                .orElseGet(() -> WebCustomer.builder().tenantId(tenantId).phone(phone).build());
        if (customer.getVerifiedAt() == null) {
            customer.setVerifiedAt(now);
        }
        if (name != null && !name.isBlank()) {
            customer.setName(name.trim());
        }
        customer.setLastLoginAt(now);
        boolean isNew = customer.getId() == null;
        customer = webCustomerRepository.save(customer);

        // Assign the stable public identity code once the PK is known.
        if (customer.getCustomerCode() == null) {
            customer.setCustomerCode(String.format("WC-%05d", customer.getId()));
            customer = webCustomerRepository.save(customer);
        }

        if (isNew && referralCode != null && !referralCode.isBlank()) {
            try {
                referralService.applyCode(tenantId, customer.getId(), referralCode);
            } catch (Exception e) {
                log.warn("Failed to apply referral code '{}' for customer {}: {}",
                        referralCode, customer.getId(), e.getMessage());
            }
        }

        if (isNew) {
            // One-time welcome bonus (loyalty.signup_bonus). Must never break login.
            try {
                loyaltyService.grantSignupBonus(tenantId, customer.getId());
            } catch (Exception e) {
                log.warn("Failed to grant signup bonus for customer {}: {}",
                        customer.getId(), e.getMessage());
            }
        }

        log.info("Web customer {} logged in (tenant {})", mask(phone), tenantId);
        return WebAuthResponse.builder()
                .token(tokenService.generateToken(customer))
                .phone(customer.getPhone())
                .name(customer.getName())
                .build();
    }

    /**
     * Resolves the web customer from a bearer token, or 401.
     */
    @Transactional(readOnly = true)
    public WebCustomer requireCustomer(String bearerToken) {
        WebCustomerPrincipal principal = tokenService.parse(stripBearer(bearerToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired token"));
        return webCustomerRepository.findByIdAndTenantId(
                        principal.webCustomerId(), principal.tenantId())
                .orElseThrow(() -> new UnauthorizedException("Unknown customer"));
    }

    /**
     * The customer's own profile for the mobile app's {@code /me} screen.
     * Includes {@code customerCode} + {@code tenantSlug} so the app can render
     * a real, scannable wallet QR ({base}/{tenantSlug}/{customerCode}).
     */
    @Transactional(readOnly = true)
    public Map<String, String> getProfile(String bearerToken) {
        WebCustomer customer = requireCustomer(bearerToken);
        String tenantSlug = tenantRepository.findById(customer.getTenantId())
                .map(Tenant::getCode)
                .orElse("");
        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("phone", customer.getPhone());
        profile.put("name", customer.getName() != null ? customer.getName() : "");
        profile.put("customerCode",
                customer.getCustomerCode() != null ? customer.getCustomerCode() : "");
        profile.put("tenantSlug", tenantSlug);
        return profile;
    }

    @Transactional(readOnly = true)
    public Page<PublicOrderDto> getMyOrders(String bearerToken, Pageable pageable) {
        WebCustomer customer = requireCustomer(bearerToken);
        Page<WebOrder> orders = orderRepository.findByTenantAndNormalizedPhone(
                customer.getTenantId(), customer.getPhone(), pageable);
        return orders.map(this::toPublicDto);
    }

    // ---- internals ----

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            // Fail closed: never silently authenticate against tenant 1 / spend tenant 1's SMS
            // budget. The caller must supply X-Tenant-ID (or a customer token carrying the tenant).
            throw new BusinessException("X-Tenant-ID header is required", "TENANT_REQUIRED");
        }
        return tenantId;
    }

    static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private static String stripBearer(String header) {
        if (header == null) {
            return "";
        }
        return header.startsWith("Bearer ") ? header.substring(7) : header;
    }

    static String hash(String salt, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((salt + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    private static String mask(String phone) {
        return phone.length() > 4 ? "***" + phone.substring(phone.length() - 4) : "***";
    }

    private PublicOrderDto toPublicDto(WebOrder order) {
        return PublicOrderDto.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .address(order.getDeliveryAddress())
                .deliveryFee(order.getDeliveryFee())
                .discountTotal(order.getDiscountTotal())
                .couponCode(order.getCouponCode())
                .couponDiscount(order.getCouponDiscount())
                .pointsSpent(order.getPointsSpent())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .createdAt(order.getCreatedAt())
                .lines(order.getLines().stream()
                        .map(l -> PublicOrderDto.Line.builder()
                                .productName(l.getProductName())
                                .quantity(l.getQuantity())
                                .unitPrice(l.getUnitPrice())
                                .lineTotal(l.getLineTotal())
                                .build())
                        .toList())
                .build();
    }
}
