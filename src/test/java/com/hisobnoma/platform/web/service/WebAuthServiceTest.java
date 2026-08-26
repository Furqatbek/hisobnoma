package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.exception.UnauthorizedException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.dto.WebAuthResponse;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebOtpCode;
import com.hisobnoma.platform.web.exception.TooManyRequestsException;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import com.hisobnoma.platform.web.repository.WebOtpCodeRepository;
import com.hisobnoma.platform.web.security.WebCustomerTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebAuthServiceTest {

    @Mock private WebOtpCodeRepository otpRepository;
    @Mock private WebCustomerRepository webCustomerRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private WebOrderRepository orderRepository;
    @Mock private WebCustomerTokenService tokenService;
    @Mock private CheckoutRateLimiter rateLimiter;
    @Mock private SmsService smsService;
    @Mock private WebReferralService referralService;
    @Mock private WebLoyaltyService loyaltyService;

    @InjectMocks
    private WebAuthService service;

    private static final Long TENANT_ID = 1L;
    private static final String PHONE = "998901234567";

    @BeforeEach
    void setTenant() {
        TenantContext.setCurrentTenant(TENANT_ID); // service fails closed without a tenant
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    // ---- requestOtp ----

    @Test
    void requestOtp_savesHashedCodeAndSendsSms() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(otpRepository.findTopByTenantIdAndPhoneOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.empty());
        when(otpRepository.countByTenantIdAndPhoneAndCreatedAtAfter(eq(TENANT_ID), eq(PHONE), any()))
                .thenReturn(0L);
        when(otpRepository.save(any(WebOtpCode.class))).thenAnswer(inv -> inv.getArgument(0));

        service.requestOtp("+998 90 123-45-67", "1.2.3.4");

        ArgumentCaptor<WebOtpCode> otpCaptor = ArgumentCaptor.forClass(WebOtpCode.class);
        verify(otpRepository).save(otpCaptor.capture());
        WebOtpCode saved = otpCaptor.getValue();
        assertEquals(PHONE, saved.getPhone());
        assertEquals(64, saved.getCodeHash().length()); // SHA-256 hex, never plaintext
        assertTrue(saved.getExpiresAt().isAfter(Instant.now()));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).sendSmsAsync(eq("+" + PHONE), messageCaptor.capture());
        String code = messageCaptor.getValue().replaceAll("[^0-9]", "");
        assertEquals(6, code.length());
        assertEquals(saved.getCodeHash(), WebAuthService.hash(saved.getSalt(), code));
    }

    @Test
    void requestOtp_rateLimitedIpGets429() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(false);

        assertThrows(TooManyRequestsException.class,
                () -> service.requestOtp(PHONE, "1.2.3.4"));
        verify(otpRepository, never()).save(any());
    }

    @Test
    void requestOtp_cooldownBlocksImmediateResend() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        WebOtpCode recent = WebOtpCode.builder()
                .phone(PHONE).codeHash("h").salt("s")
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .tenantId(TENANT_ID).build();
        recent.setCreatedAt(Instant.now().minusSeconds(10));
        when(otpRepository.findTopByTenantIdAndPhoneOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(recent));

        assertThrows(TooManyRequestsException.class,
                () -> service.requestOtp(PHONE, "1.2.3.4"));
        verify(smsService, never()).sendSmsAsync(any(), any());
    }

    @Test
    void requestOtp_dailyCapBlocksSixthCode() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(otpRepository.findTopByTenantIdAndPhoneOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.empty());
        when(otpRepository.countByTenantIdAndPhoneAndCreatedAtAfter(eq(TENANT_ID), eq(PHONE), any()))
                .thenReturn(5L);

        assertThrows(TooManyRequestsException.class,
                () -> service.requestOtp(PHONE, "1.2.3.4"));
    }

    @Test
    void requestOtp_smsFailureDoesNotBreakRequest() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(otpRepository.findTopByTenantIdAndPhoneOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.empty());
        when(otpRepository.countByTenantIdAndPhoneAndCreatedAtAfter(eq(TENANT_ID), eq(PHONE), any()))
                .thenReturn(0L);
        when(otpRepository.save(any(WebOtpCode.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("sms down")).when(smsService).sendSmsAsync(any(), any());

        assertDoesNotThrow(() -> service.requestOtp(PHONE, "1.2.3.4"));
    }

    // ---- verifyOtp ----

    private WebOtpCode validOtp(String code) {
        return WebOtpCode.builder()
                .phone(PHONE).salt("abcd")
                .codeHash(WebAuthService.hash("abcd", code))
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .attempts(0).used(false)
                .tenantId(TENANT_ID).build();
    }

    @Test
    void verifyOtp_correctCodeCreatesCustomerAndIssuesToken() {
        WebOtpCode otp = validOtp("123456");
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(otp));
        when(otpRepository.save(any(WebOtpCode.class))).thenAnswer(inv -> inv.getArgument(0));
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, PHONE))
                .thenReturn(Optional.empty());
        when(webCustomerRepository.save(any(WebCustomer.class))).thenAnswer(inv -> {
            WebCustomer c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(42L); // JPA assigns the PK on first persist
            }
            return c;
        });
        when(tokenService.generateToken(any(WebCustomer.class))).thenReturn("the-token");

        WebAuthResponse response = service.verifyOtp("+998901234567", "123456", "Ali", null);

        assertEquals("the-token", response.getToken());
        assertEquals(PHONE, response.getPhone());
        assertEquals("Ali", response.getName());
        assertTrue(otp.isUsed());

        ArgumentCaptor<WebCustomer> captor = ArgumentCaptor.forClass(WebCustomer.class);
        verify(webCustomerRepository, times(2)).save(captor.capture());
        WebCustomer saved = captor.getValue();
        assertNotNull(saved.getVerifiedAt());
        assertNotNull(saved.getLastLoginAt());
        assertEquals("WC-00042", saved.getCustomerCode()); // auto-generated from PK
    }

    @Test
    void verifyOtp_wrongCodeIncrementsAttempts() {
        WebOtpCode otp = validOtp("123456");
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(otp));
        when(otpRepository.save(any(WebOtpCode.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(ValidationException.class,
                () -> service.verifyOtp(PHONE, "999999", null, null));
        assertEquals(1, otp.getAttempts());
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void verifyOtp_lockedAfterMaxAttempts() {
        WebOtpCode otp = validOtp("123456");
        otp.setAttempts(5);
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(otp));

        // Even the CORRECT code is rejected once locked
        assertThrows(TooManyRequestsException.class,
                () -> service.verifyOtp(PHONE, "123456", null, null));
    }

    @Test
    void verifyOtp_expiredCodeRejected() {
        WebOtpCode otp = validOtp("123456");
        otp.setExpiresAt(Instant.now().minusSeconds(1));
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(otp));

        assertThrows(ValidationException.class,
                () -> service.verifyOtp(PHONE, "123456", null, null));
    }

    @Test
    void verifyOtp_existingCustomerKeepsNameWhenNoneGiven() {
        WebOtpCode otp = validOtp("123456");
        WebCustomer existing = WebCustomer.builder()
                .phone(PHONE).name("Ali").verifiedAt(Instant.now().minusSeconds(100))
                .customerCode("WC-00007").tenantId(TENANT_ID).build();
        existing.setId(7L);
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(otp));
        when(otpRepository.save(any(WebOtpCode.class))).thenAnswer(inv -> inv.getArgument(0));
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, PHONE))
                .thenReturn(Optional.of(existing));
        when(webCustomerRepository.save(any(WebCustomer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.generateToken(any(WebCustomer.class))).thenReturn("t");

        WebAuthResponse response = service.verifyOtp(PHONE, "123456", null, null);

        assertEquals("Ali", response.getName());
    }

    // ---- token-scoped access ----

    @Test
    void requireCustomer_invalidTokenThrowsUnauthorized() {
        when(tokenService.parse("bad")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> service.requireCustomer("Bearer bad"));
    }

    @Test
    void getProfile_returnsCustomerCodeAndTenantSlugForWalletQr() {
        WebCustomer customer = WebCustomer.builder()
                .phone(PHONE).name("Ali").customerCode("WC-00042").tenantId(TENANT_ID).build();
        customer.setId(42L);
        when(tokenService.parse("good")).thenReturn(Optional.of(
                new WebCustomerTokenService.WebCustomerPrincipal(42L, TENANT_ID, PHONE)));
        when(webCustomerRepository.findByIdAndTenantId(42L, TENANT_ID))
                .thenReturn(Optional.of(customer));
        Tenant tenant = Tenant.builder().name("Hisobnoma").code("hisobnoma").build();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        var profile = service.getProfile("Bearer good");

        assertEquals(PHONE, profile.get("phone"));
        assertEquals("Ali", profile.get("name"));
        assertEquals("WC-00042", profile.get("customerCode"));
        assertEquals("hisobnoma", profile.get("tenantSlug"));
    }

    @Test
    void requireCustomer_resolvesCustomerFromToken() {
        WebCustomer customer = WebCustomer.builder().phone(PHONE).tenantId(TENANT_ID).build();
        customer.setId(42L);
        when(tokenService.parse("good")).thenReturn(Optional.of(
                new WebCustomerTokenService.WebCustomerPrincipal(42L, TENANT_ID, PHONE)));
        when(webCustomerRepository.findByIdAndTenantId(42L, TENANT_ID))
                .thenReturn(Optional.of(customer));

        assertEquals(42L, service.requireCustomer("Bearer good").getId());
    }

    // ---- signup bonus on first sign-in ----

    @Test
    void verifyOtp_firstSignIn_grantsSignupBonus() {
        WebOtpCode otp = validOtp("123456");
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(otp));
        when(otpRepository.save(any(WebOtpCode.class))).thenAnswer(inv -> inv.getArgument(0));
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, PHONE))
                .thenReturn(Optional.empty());
        when(webCustomerRepository.save(any(WebCustomer.class))).thenAnswer(inv -> {
            WebCustomer c = inv.getArgument(0);
            if (c.getId() == null) c.setId(42L);
            return c;
        });
        when(tokenService.generateToken(any(WebCustomer.class))).thenReturn("t");

        service.verifyOtp(PHONE, "123456", "Ali", null);

        verify(loyaltyService).grantSignupBonus(TENANT_ID, 42L);
    }

    @Test
    void verifyOtp_returningCustomer_noSignupBonus() {
        WebOtpCode otp = validOtp("123456");
        WebCustomer existing = WebCustomer.builder()
                .tenantId(TENANT_ID).phone(PHONE).customerCode("WC-00001").build();
        existing.setId(42L);
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(otp));
        when(otpRepository.save(any(WebOtpCode.class))).thenAnswer(inv -> inv.getArgument(0));
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, PHONE))
                .thenReturn(Optional.of(existing));
        when(webCustomerRepository.save(any(WebCustomer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.generateToken(any(WebCustomer.class))).thenReturn("t");

        service.verifyOtp(PHONE, "123456", null, null);

        verify(loyaltyService, never()).grantSignupBonus(any(), any());
    }

    @Test
    void verifyOtp_signupBonusFailureNeverBreaksLogin() {
        WebOtpCode otp = validOtp("123456");
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(otp));
        when(otpRepository.save(any(WebOtpCode.class))).thenAnswer(inv -> inv.getArgument(0));
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, PHONE))
                .thenReturn(Optional.empty());
        when(webCustomerRepository.save(any(WebCustomer.class))).thenAnswer(inv -> {
            WebCustomer c = inv.getArgument(0);
            if (c.getId() == null) c.setId(42L);
            return c;
        });
        when(tokenService.generateToken(any(WebCustomer.class))).thenReturn("t");
        doThrow(new RuntimeException("loyalty down")).when(loyaltyService).grantSignupBonus(any(), any());

        WebAuthResponse response = service.verifyOtp(PHONE, "123456", "Ali", null);

        assertEquals("t", response.getToken());
    }

    // ---- store-review account (fixed OTP) ----

    private static final String REVIEW_PHONE = "998900000000";

    private void enableReviewAccount() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "reviewAccountPhone", "+998 90 000 00 00");
        org.springframework.test.util.ReflectionTestUtils.setField(service, "reviewAccountCode", "123456");
    }

    @Test
    void requestOtp_reviewAccount_skipsSmsRateLimitAndDbRecord() {
        enableReviewAccount();

        service.requestOtp("+998 90 000 00 00", "1.2.3.4");

        verifyNoInteractions(rateLimiter);
        verify(otpRepository, never()).save(any());
        verifyNoInteractions(smsService);
    }

    @Test
    void requestOtp_anyOfMultipleReviewNumbers_neverGetsSms() {
        // Several comma-separated review numbers: none of them receive a real SMS.
        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "reviewAccountPhone", "+998900000000, 998900000001");
        org.springframework.test.util.ReflectionTestUtils.setField(service, "reviewAccountCode", "123456");

        service.requestOtp("998900000000", "1.2.3.4");
        service.requestOtp("+998 90 000 00 01", "1.2.3.4");

        verifyNoInteractions(smsService);
        verify(otpRepository, never()).save(any());
    }

    @Test
    void verifyOtp_reviewAccount_fixedCode_logsInWithoutOtpRecord() {
        enableReviewAccount();
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, REVIEW_PHONE))
                .thenReturn(Optional.empty());
        when(webCustomerRepository.save(any(WebCustomer.class))).thenAnswer(inv -> {
            WebCustomer c = inv.getArgument(0);
            if (c.getId() == null) c.setId(77L);
            return c;
        });
        when(tokenService.generateToken(any(WebCustomer.class))).thenReturn("review-token");

        WebAuthResponse response = service.verifyOtp("+998 90 000 00 00", "123456", "Reviewer", null);

        assertEquals("review-token", response.getToken());
        // The fixed-code path never consults the OTP table.
        verify(otpRepository, never()).findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void verifyOtp_reviewAccount_wrongCode_throws() {
        enableReviewAccount();

        assertThrows(ValidationException.class,
                () -> service.verifyOtp(REVIEW_PHONE, "000000", "Reviewer", null));
        verify(webCustomerRepository, never()).save(any());
    }

    @Test
    void verifyOtp_reviewAccountNotConfigured_phoneUsesNormalOtpPath() {
        // No review account configured → the review phone is treated as any other phone.
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, REVIEW_PHONE))
                .thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.verifyOtp(REVIEW_PHONE, "123456", "X", null));
    }
}
