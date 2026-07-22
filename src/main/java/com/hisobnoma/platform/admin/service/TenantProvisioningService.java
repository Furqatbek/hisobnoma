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
import com.hisobnoma.platform.finance.entity.FiscalPeriod;
import com.hisobnoma.platform.finance.entity.FiscalYear;
import com.hisobnoma.platform.finance.entity.PeriodStatus;
import com.hisobnoma.platform.finance.repository.FiscalYearRepository;
import com.hisobnoma.platform.finance.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.Set;

/**
 * One-call tenant onboarding: creates the tenant, seeds its default chart of accounts (a tenant
 * created after the seed migrations otherwise has NO GL accounts and every AR/POS posting fails),
 * creates the tenant's first ADMIN user, and opens a fiscal year with 12 monthly periods (GL
 * posting refuses dates without an open period). All in ONE transaction — a half-provisioned
 * tenant cannot exist. Replaces steps 1–4 of docs/TENANT_ONBOARDING.md.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ProvisionTenantResult provision(ProvisionTenantRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (tenantRepository.existsByCode(code)) {
            throw new BusinessException("Tenant code already exists: " + code, "TENANT_CODE_EXISTS");
        }
        if (userRepository.existsByUsername(request.getAdminUsername().trim())) {
            throw new BusinessException("Username already exists: " + request.getAdminUsername(),
                    "USERNAME_EXISTS");
        }

        // 1. Tenant
        Tenant tenant = Tenant.builder()
                .name(request.getName().trim())
                .code(code)
                .description(request.getDescription())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "Asia/Tashkent")
                .currency(request.getCurrency() != null ? request.getCurrency() : "UZS")
                .build();
        tenant = tenantRepository.save(tenant);
        Long tenantId = tenant.getId();

        // 2. Default chart of accounts (1110 Cash … 2130 VAT Payable … 4100 Sales Revenue …)
        int accounts = accountService.generateDefaultChartOfAccounts(tenantId).size();

        // 3. First ADMIN user
        Role adminRole = roleRepository.findByCode("ADMIN")
                .orElseThrow(() -> new BusinessException("ADMIN role is not seeded"));
        User admin = User.builder()
                .username(request.getAdminUsername().trim())
                .phone(request.getAdminPhone())
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();
        admin.setTenantId(tenantId);
        admin = userRepository.save(admin);

        // 4. Fiscal year with 12 open monthly periods
        int year = request.getFiscalYear() != null ? request.getFiscalYear() : Year.now().getValue();
        FiscalYear fiscalYear = FiscalYear.builder()
                .year(year)
                .name("FY " + year)
                .startDate(LocalDate.of(year, 1, 1))
                .endDate(LocalDate.of(year, 12, 31))
                .status(PeriodStatus.OPEN)
                .build();
        fiscalYear.setTenantId(tenantId);
        for (int m = 1; m <= 12; m++) {
            LocalDate start = LocalDate.of(year, m, 1);
            FiscalPeriod period = FiscalPeriod.builder()
                    .periodNumber(m)
                    .name(start.getMonth() + " " + year)
                    .startDate(start)
                    .endDate(start.withDayOfMonth(start.lengthOfMonth()))
                    .status(PeriodStatus.OPEN)
                    .fiscalYear(fiscalYear)
                    .build();
            period.setTenantId(tenantId);
            fiscalYear.getPeriods().add(period);
        }
        fiscalYearRepository.save(fiscalYear);

        log.info("Provisioned tenant {} ({}): {} accounts, admin '{}', fiscal year {}",
                tenantId, code, accounts, admin.getUsername(), year);
        return new ProvisionTenantResult(tenantId, code, accounts, admin.getId(),
                admin.getUsername(), year);
    }
}
