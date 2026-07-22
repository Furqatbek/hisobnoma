package com.hisobnoma.platform.admin.service;

import com.hisobnoma.platform.admin.dto.ProvisionTenantRequest;
import com.hisobnoma.platform.admin.dto.ProvisionTenantResult;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.RoleRepository;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.FiscalYear;
import com.hisobnoma.platform.finance.repository.FiscalYearRepository;
import com.hisobnoma.platform.finance.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantProvisioningServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private AccountService accountService;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private FiscalYearRepository fiscalYearRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private TenantProvisioningService service;

    private ProvisionTenantRequest request() {
        return ProvisionTenantRequest.builder()
                .name("Acme").code("acme").adminUsername("acme-admin")
                .adminPassword("secret-pass").fiscalYear(2026).build();
    }

    @Test
    void provision_createsTenantAccountsAdminAndFiscalYearAtomically() {
        when(tenantRepository.existsByCode("ACME")).thenReturn(false);
        when(userRepository.existsByUsername("acme-admin")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0); t.setId(42L); return t;
        });
        when(accountService.generateDefaultChartOfAccounts(42L)).thenReturn(List.of());
        Role admin = new Role(); admin.setCode("ADMIN");
        when(roleRepository.findSystemRoleByCode("ADMIN")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("secret-pass")).thenReturn("{hash}");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0); u.setId(7L); return u;
        });

        ProvisionTenantResult result = service.provision(request());

        assertEquals(42L, result.tenantId());
        assertEquals("ACME", result.code(), "code uppercased");
        assertEquals(7L, result.adminUserId());
        assertEquals(2026, result.fiscalYear());
        verify(accountService).generateDefaultChartOfAccounts(42L);

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(user.capture());
        assertEquals("{hash}", user.getValue().getPasswordHash());
        assertEquals(42L, user.getValue().getTenantId());
        assertTrue(user.getValue().getRoles().contains(admin));

        ArgumentCaptor<FiscalYear> fy = ArgumentCaptor.forClass(FiscalYear.class);
        verify(fiscalYearRepository).save(fy.capture());
        assertEquals(42L, fy.getValue().getTenantId());
        assertEquals(12, fy.getValue().getPeriods().size(), "12 monthly periods");
        assertEquals(42L, fy.getValue().getPeriods().get(0).getTenantId());
    }

    @Test
    void provision_rejectsDuplicateCodeAndUsername() {
        when(tenantRepository.existsByCode("ACME")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.provision(request()));

        when(tenantRepository.existsByCode("ACME")).thenReturn(false);
        when(userRepository.existsByUsername("acme-admin")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.provision(request()));
        verify(tenantRepository, never()).save(any());
    }
}
