package com.hisobnoma.platform.distribution.b2b.service;

import com.hisobnoma.platform.common.exception.UnauthorizedException;
import com.hisobnoma.platform.distribution.b2b.dto.B2bAuthResponse;
import com.hisobnoma.platform.distribution.b2b.dto.B2bLoginRequest;
import com.hisobnoma.platform.distribution.b2b.security.B2bCustomerTokenService;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class B2bAuthServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private B2bCustomerTokenService tokenService;

    @InjectMocks private B2bAuthService service;

    // No TenantContext set in unit tests -> default tenant 1.
    private static final Long TENANT_ID = 1L;

    private Customer customer() {
        Customer c = Customer.builder().code("C-1").name("Osiyo Savdo").phone("+998 90 111-22-33")
                .defaultCurrency("UZS").creditLimit(new BigDecimal("1000000")).currentBalance(BigDecimal.ZERO)
                .build();
        c.setId(100L);
        c.setTenantId(TENANT_ID);
        return c;
    }

    @Test
    void login_validCredentials_returnsToken() {
        when(customerRepository.findByCodeAndTenantId("C-1", TENANT_ID)).thenReturn(Optional.of(customer()));
        when(tokenService.generateToken(100L, TENANT_ID, "C-1")).thenReturn("tok-123");

        // Phone given in a different format — digits must still match.
        B2bAuthResponse resp = service.login(B2bLoginRequest.builder().code("C-1").phone("998901112233").build());

        assertEquals("tok-123", resp.getToken());
        assertEquals(100L, resp.getCustomerId());
        assertEquals("C-1", resp.getCode());
    }

    @Test
    void login_wrongPhone_throws() {
        when(customerRepository.findByCodeAndTenantId("C-1", TENANT_ID)).thenReturn(Optional.of(customer()));

        assertThrows(UnauthorizedException.class,
                () -> service.login(B2bLoginRequest.builder().code("C-1").phone("998900000000").build()));
    }

    @Test
    void login_unknownCode_throws() {
        when(customerRepository.findByCodeAndTenantId("NOPE", TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> service.login(B2bLoginRequest.builder().code("NOPE").phone("998901112233").build()));
    }

    @Test
    void requireCustomer_validToken_returnsCustomer() {
        when(tokenService.parse("tok-123"))
                .thenReturn(Optional.of(new B2bCustomerTokenService.B2bCustomerPrincipal(100L, TENANT_ID, "C-1")));
        when(customerRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(customer()));

        Customer c = service.requireCustomer("Bearer tok-123");

        assertEquals(100L, c.getId());
    }

    @Test
    void requireCustomer_invalidToken_throws() {
        when(tokenService.parse("bad")).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> service.requireCustomer("Bearer bad"));
    }

    @Test
    void getProfile_returnsCreditInfo() {
        when(tokenService.parse("tok-123"))
                .thenReturn(Optional.of(new B2bCustomerTokenService.B2bCustomerPrincipal(100L, TENANT_ID, "C-1")));
        when(customerRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(customer()));

        var profile = service.getProfile("Bearer tok-123");

        assertEquals("C-1", profile.getCode());
        assertEquals(0, new BigDecimal("1000000").compareTo(profile.getAvailableCredit()));
    }
}
