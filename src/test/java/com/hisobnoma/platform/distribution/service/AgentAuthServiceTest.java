package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.UnauthorizedException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.distribution.dto.AgentAuthResponse;
import com.hisobnoma.platform.distribution.entity.AgentStatus;
import com.hisobnoma.platform.distribution.entity.DistributionAgent;
import com.hisobnoma.platform.distribution.entity.DistributionAgentOtp;
import com.hisobnoma.platform.distribution.repository.DistributionAgentOtpRepository;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.distribution.security.AgentTokenService;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.exception.TooManyRequestsException;
import com.hisobnoma.platform.web.service.CheckoutRateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentAuthServiceTest {

    private static final Long TENANT_ID = 5L;
    private static final String PHONE = "998901234567";

    @Mock private DistributionAgentOtpRepository otpRepository;
    @Mock private DistributionAgentRepository agentRepository;
    @Mock private AgentTokenService tokenService;
    @Mock private CheckoutRateLimiter rateLimiter;
    @Mock private SmsService smsService;

    @InjectMocks private AgentAuthService service;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private DistributionAgent agent() {
        DistributionAgent a = DistributionAgent.builder()
                .code("AG-1").name("Alisher").phone(PHONE).status(AgentStatus.ACTIVE).build();
        a.setId(42L);
        a.setTenantId(TENANT_ID);
        return a;
    }

    @Test
    void requestOtp_knownActiveAgent_sendsSmsAndStoresHash() {
        when(agentRepository.findByTenantIdAndPhoneAndStatus(TENANT_ID, PHONE, AgentStatus.ACTIVE))
                .thenReturn(List.of(agent()));
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(otpRepository.findTopByTenantIdAndPhoneOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.empty());
        when(otpRepository.countByTenantIdAndPhoneAndCreatedAtAfter(eq(TENANT_ID), eq(PHONE), any()))
                .thenReturn(0L);

        service.requestOtp(PHONE, "1.2.3.4");

        verify(otpRepository).save(any(DistributionAgentOtp.class));
        verify(smsService).sendSmsAsync(eq("+" + PHONE), contains("код"));
    }

    @Test
    void requestOtp_unknownPhone_noSmsNoPersist() {
        when(agentRepository.findByTenantIdAndPhoneAndStatus(TENANT_ID, PHONE, AgentStatus.ACTIVE))
                .thenReturn(List.of());
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(otpRepository.findTopByTenantIdAndPhoneOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.empty());
        when(otpRepository.countByTenantIdAndPhoneAndCreatedAtAfter(eq(TENANT_ID), eq(PHONE), any()))
                .thenReturn(0L);

        service.requestOtp(PHONE, "1.2.3.4");

        verify(otpRepository, never()).save(any());
        verifyNoInteractions(smsService);
    }

    @Test
    void requestOtp_rateLimited_throws() {
        when(agentRepository.findByTenantIdAndPhoneAndStatus(TENANT_ID, PHONE, AgentStatus.ACTIVE))
                .thenReturn(List.of(agent()));
        when(rateLimiter.tryAcquire(anyString())).thenReturn(false);

        assertThrows(TooManyRequestsException.class, () -> service.requestOtp(PHONE, "1.2.3.4"));
    }

    @Test
    void requestOtp_noTenant_failsClosed() {
        TenantContext.clear();
        assertThrows(BusinessException.class, () -> service.requestOtp(PHONE, "1.2.3.4"));
    }

    private DistributionAgentOtp validOtp(String code) {
        String salt = "abcdef0123456789";
        return DistributionAgentOtp.builder()
                .phone(PHONE).salt(salt).codeHash(AgentAuthService.hash(salt, code))
                .expiresAt(Instant.now().plusSeconds(120)).build();
    }

    @Test
    void verifyOtp_correctCode_singleActiveAgent_issuesToken() {
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(validOtp("123456")));
        when(agentRepository.findByTenantIdAndPhoneAndStatus(TENANT_ID, PHONE, AgentStatus.ACTIVE))
                .thenReturn(List.of(agent()));
        when(tokenService.generateToken(42L, TENANT_ID, "AG-1")).thenReturn("tok-xyz");

        AgentAuthResponse resp = service.verifyOtp(PHONE, "123456");

        assertEquals("tok-xyz", resp.getToken());
        assertEquals(42L, resp.getAgentId());
        assertEquals("AG-1", resp.getCode());
        ArgumentCaptor<DistributionAgentOtp> captor = ArgumentCaptor.forClass(DistributionAgentOtp.class);
        verify(otpRepository).save(captor.capture());
        assertTrue(captor.getValue().isUsed());
    }

    @Test
    void verifyOtp_wrongCode_incrementsAttemptsAndThrows() {
        DistributionAgentOtp otp = validOtp("123456");
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(otp));

        assertThrows(ValidationException.class, () -> service.verifyOtp(PHONE, "000000"));
        assertEquals(1, otp.getAttempts());
        verify(tokenService, never()).generateToken(any(), any(), any());
    }

    @Test
    void verifyOtp_ambiguousPhone_refusesLogin() {
        when(otpRepository.findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, PHONE))
                .thenReturn(Optional.of(validOtp("123456")));
        DistributionAgent a2 = agent();
        a2.setId(43L);
        when(agentRepository.findByTenantIdAndPhoneAndStatus(TENANT_ID, PHONE, AgentStatus.ACTIVE))
                .thenReturn(List.of(agent(), a2));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verifyOtp(PHONE, "123456"));
        assertEquals("AGENT_PHONE_AMBIGUOUS", ex.getCode());
        verify(tokenService, never()).generateToken(any(), any(), any());
    }

    @Test
    void requireAgent_suspendedAgent_unauthorized() {
        DistributionAgent suspended = agent();
        suspended.setStatus(AgentStatus.SUSPENDED);
        when(tokenService.parse("tok"))
                .thenReturn(Optional.of(new AgentTokenService.AgentPrincipal(42L, TENANT_ID, "AG-1")));
        when(agentRepository.findByIdAndTenantId(42L, TENANT_ID)).thenReturn(Optional.of(suspended));

        assertThrows(UnauthorizedException.class, () -> service.requireAgent("Bearer tok"));
    }

    @Test
    void requireAgent_invalidToken_unauthorized() {
        when(tokenService.parse("bad")).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> service.requireAgent("Bearer bad"));
    }
}
