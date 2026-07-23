package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.UnauthorizedException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.distribution.dto.AgentAuthResponse;
import com.hisobnoma.platform.distribution.dto.AgentProfileDto;
import com.hisobnoma.platform.distribution.entity.AgentStatus;
import com.hisobnoma.platform.distribution.entity.DistributionAgent;
import com.hisobnoma.platform.distribution.entity.DistributionAgentOtp;
import com.hisobnoma.platform.distribution.repository.DistributionAgentOtpRepository;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.distribution.security.AgentTokenService;
import com.hisobnoma.platform.distribution.security.AgentTokenService.AgentPrincipal;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.exception.TooManyRequestsException;
import com.hisobnoma.platform.web.service.CheckoutRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

/**
 * Phone + SMS OTP login for distribution agents' mobile app, and the token-scoped
 * identity resolver used by every {@code /api/v1/agent/**} endpoint. Abuse limits
 * mirror the web-customer flow: 60 s resend cooldown, ≤5 codes/day/phone, 5 wrong
 * attempts per code, 5-minute expiry, plus a per-IP request limiter.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentAuthService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_CODES_PER_DAY = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final DistributionAgentOtpRepository otpRepository;
    private final DistributionAgentRepository agentRepository;
    private final AgentTokenService tokenService;
    private final CheckoutRateLimiter rateLimiter;
    private final SmsService smsService;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void requestOtp(String rawPhone, String sourceIp) {
        Long tenantId = resolveTenantId();
        String phone = normalizePhone(rawPhone);
        if (phone.length() < 9 || phone.length() > 15) {
            throw new ValidationException("Invalid phone number");
        }

        // Do not reveal whether the phone belongs to an agent; only send if it does,
        // but always behave the same to the caller (return 200 either way).
        boolean isKnownAgent = !agentRepository
                .findByTenantIdAndPhoneAndStatus(tenantId, phone, AgentStatus.ACTIVE).isEmpty();

        if (!rateLimiter.tryAcquire("agent-otp|" + sourceIp)) {
            throw new TooManyRequestsException("Too many code requests, please try again later");
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

        if (!isKnownAgent) {
            log.info("Agent OTP requested for unknown phone {} (tenant {}) — no SMS sent", mask(phone), tenantId);
            return;
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        String salt = HexFormat.of().formatHex(randomBytes(16));

        otpRepository.save(DistributionAgentOtp.builder()
                .tenantId(tenantId)
                .phone(phone)
                .salt(salt)
                .codeHash(hash(salt, code))
                .expiresAt(now.plus(CODE_TTL))
                .build());

        try {
            smsService.sendSmsAsync("+" + phone, "Агент кабинети: тасдиқлаш коди " + code);
        } catch (Exception e) {
            log.warn("Failed to send agent OTP SMS to {}: {}", mask(phone), e.getMessage());
        }
        log.info("Agent OTP sent to {} (tenant {})", mask(phone), tenantId);
    }

    @Transactional
    public AgentAuthResponse verifyOtp(String rawPhone, String code) {
        Long tenantId = resolveTenantId();
        String phone = normalizePhone(rawPhone);

        DistributionAgentOtp otp = otpRepository
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

        // Resolve to exactly one active agent; ambiguous phones are a data problem, not a login.
        List<DistributionAgent> matches = agentRepository
                .findByTenantIdAndPhoneAndStatus(tenantId, phone, AgentStatus.ACTIVE);
        if (matches.isEmpty()) {
            throw new UnauthorizedException("No active agent for this phone");
        }
        if (matches.size() > 1) {
            log.error("Phone {} maps to {} active agents (tenant {}) — refusing login",
                    mask(phone), matches.size(), tenantId);
            throw new BusinessException("Phone is shared by multiple agents; contact your manager",
                    "AGENT_PHONE_AMBIGUOUS");
        }

        DistributionAgent agent = matches.get(0);
        log.info("Agent {} logged in (tenant {})", agent.getCode(), tenantId);
        return AgentAuthResponse.builder()
                .token(tokenService.generateToken(agent.getId(), tenantId, agent.getCode()))
                .agentId(agent.getId())
                .code(agent.getCode())
                .name(agent.getName())
                .phone(agent.getPhone())
                .build();
    }

    /**
     * Resolves the agent from a bearer token, or 401. The tenant is taken from the
     * token (set at login), never from a request header — the authoritative identity.
     */
    @Transactional(readOnly = true)
    public DistributionAgent requireAgent(String bearerToken) {
        AgentPrincipal principal = tokenService.parse(stripBearer(bearerToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired token"));
        DistributionAgent agent = agentRepository
                .findByIdAndTenantId(principal.agentId(), principal.tenantId())
                .orElseThrow(() -> new UnauthorizedException("Unknown agent"));
        if (agent.getStatus() != AgentStatus.ACTIVE) {
            throw new UnauthorizedException("Agent is not active");
        }
        return agent;
    }

    @Transactional(readOnly = true)
    public AgentProfileDto getProfile(String bearerToken) {
        DistributionAgent agent = requireAgent(bearerToken);
        return AgentProfileDto.builder()
                .agentId(agent.getId())
                .code(agent.getCode())
                .name(agent.getName())
                .phone(agent.getPhone())
                .vehiclePlate(agent.getVehiclePlate())
                .vehicleName(agent.getVehicleName())
                .status(agent.getStatus().name())
                .build();
    }

    // ---- internals ----

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
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
}
