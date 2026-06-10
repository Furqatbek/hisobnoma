package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebOtpCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class WebCustomerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WebCustomerRepository customerRepository;

    @Autowired
    private WebOtpCodeRepository otpRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private WebCustomer persistCustomer(String phone, String name, Long tenantId) {
        return entityManager.persistAndFlush(WebCustomer.builder()
                .phone(phone).name(name).verifiedAt(Instant.now())
                .lastLoginAt(Instant.now()).tenantId(tenantId)
                .build());
    }

    @Test
    void phoneIsUniquePerTenantButAllowedAcrossTenants() {
        persistCustomer("998901234567", "Ali", TENANT_ID);
        persistCustomer("998901234567", "Vali", OTHER_TENANT_ID);

        assertThrows(jakarta.persistence.PersistenceException.class,
                () -> persistCustomer("998901234567", "Dup", TENANT_ID));
    }

    @Test
    void findByTenantIdAndPhone_scopesByTenant() {
        WebCustomer ali = persistCustomer("998901234567", "Ali", TENANT_ID);
        persistCustomer("998901234567", "Vali", OTHER_TENANT_ID);

        assertEquals(ali.getId(), customerRepository
                .findByTenantIdAndPhone(TENANT_ID, "998901234567").orElseThrow().getId());
        assertTrue(customerRepository.findByTenantIdAndPhone(TENANT_ID, "998900000000").isEmpty());
    }

    @Test
    void searchByTenant_matchesPhoneAndName() {
        persistCustomer("998901234567", "Ali Valiyev", TENANT_ID);
        persistCustomer("998907654321", "Botir", TENANT_ID);

        Page<WebCustomer> byPhone = customerRepository.searchByTenant(
                TENANT_ID, "%1234%", PageRequest.of(0, 10));
        Page<WebCustomer> byName = customerRepository.searchByTenant(
                TENANT_ID, "%ali%", PageRequest.of(0, 10));

        assertEquals(1, byPhone.getTotalElements());
        assertEquals(1, byName.getTotalElements());
        assertEquals("Ali Valiyev", byName.getContent().get(0).getName());
    }

    @Test
    void countByTenantId_excludesOtherTenants() {
        persistCustomer("998901111111", null, TENANT_ID);
        persistCustomer("998902222222", null, TENANT_ID);
        persistCustomer("998903333333", null, OTHER_TENANT_ID);

        assertEquals(2, customerRepository.countByTenantId(TENANT_ID));
    }

    // ---- OTP codes ----

    private WebOtpCode persistOtp(String phone, Instant expiresAt, boolean used) {
        return entityManager.persistAndFlush(WebOtpCode.builder()
                .phone(phone).codeHash("hash").salt("salt")
                .expiresAt(expiresAt).used(used).tenantId(TENANT_ID)
                .build());
    }

    @Test
    void findTopUnused_returnsLatestUnusedCode() throws InterruptedException {
        persistOtp("998901234567", Instant.now().plus(5, ChronoUnit.MINUTES), true);
        Thread.sleep(5);
        WebOtpCode latest = persistOtp("998901234567", Instant.now().plus(5, ChronoUnit.MINUTES), false);

        assertEquals(latest.getId(), otpRepository
                .findTopByTenantIdAndPhoneAndUsedFalseOrderByCreatedAtDesc(TENANT_ID, "998901234567")
                .orElseThrow().getId());
    }

    @Test
    void countByCreatedAtAfter_countsTodaysCodes() {
        persistOtp("998901234567", Instant.now().plus(5, ChronoUnit.MINUTES), false);
        persistOtp("998901234567", Instant.now().plus(5, ChronoUnit.MINUTES), false);
        persistOtp("998907777777", Instant.now().plus(5, ChronoUnit.MINUTES), false);

        long count = otpRepository.countByTenantIdAndPhoneAndCreatedAtAfter(
                TENANT_ID, "998901234567", Instant.now().minus(1, ChronoUnit.HOURS));

        assertEquals(2, count);
    }
}
