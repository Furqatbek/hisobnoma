package com.hisobnoma.platform.mobile.push.repository;

import com.hisobnoma.platform.mobile.push.entity.DevicePushToken;
import com.hisobnoma.platform.mobile.push.entity.PushEnvironment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the APNs token store against H2: tenant-scoped lookups, the unique
 * (tenant_id, token) upsert target, and tenant-scoped delete.
 */
@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class DevicePushTokenRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private DevicePushTokenRepository repository;

    private DevicePushToken token(Long tenantId, Long userId, String value, PushEnvironment env) {
        return DevicePushToken.builder()
                .tenantId(tenantId).userId(userId).token(value)
                .platform("ios").environment(env).build();
    }

    @Test
    void findByTenantIdAndToken_scopesByTenant() {
        em.persistAndFlush(token(1L, 10L, "shared-token", PushEnvironment.PRODUCTION));
        em.persistAndFlush(token(2L, 20L, "shared-token", PushEnvironment.SANDBOX)); // same token, other tenant

        Optional<DevicePushToken> hit = repository.findByTenantIdAndToken(1L, "shared-token");
        assertTrue(hit.isPresent());
        assertEquals(10L, hit.get().getUserId());
        assertEquals(PushEnvironment.PRODUCTION, hit.get().getEnvironment());

        assertEquals(20L, repository.findByTenantIdAndToken(2L, "shared-token").get().getUserId());
        assertTrue(repository.findByTenantIdAndToken(1L, "nope").isEmpty());
    }

    @Test
    void findByTenantId_andByUser_filterCorrectly() {
        em.persistAndFlush(token(1L, 10L, "t-a", PushEnvironment.PRODUCTION));
        em.persistAndFlush(token(1L, 10L, "t-b", PushEnvironment.PRODUCTION));
        em.persistAndFlush(token(1L, 11L, "t-c", PushEnvironment.PRODUCTION));
        em.persistAndFlush(token(2L, 10L, "t-d", PushEnvironment.PRODUCTION)); // other tenant

        assertEquals(3, repository.findByTenantId(1L).size());
        List<DevicePushToken> forUser10 = repository.findByTenantIdAndUserId(1L, 10L);
        assertEquals(2, forUser10.size());
    }

    @Test
    void duplicateTokenForSameTenant_isRejected() {
        em.persistAndFlush(token(1L, 10L, "dup", PushEnvironment.PRODUCTION));
        assertThrows(Exception.class, () -> em.persistAndFlush(token(1L, 11L, "dup", PushEnvironment.SANDBOX)));
    }

    @Test
    void deleteByTenantIdAndToken_removesOnlyThatTenantsRow() {
        em.persistAndFlush(token(1L, 10L, "gone", PushEnvironment.PRODUCTION));
        em.persistAndFlush(token(2L, 20L, "gone", PushEnvironment.PRODUCTION));
        em.flush();
        em.clear();

        repository.deleteByTenantIdAndToken(1L, "gone");
        em.flush();

        assertTrue(repository.findByTenantIdAndToken(1L, "gone").isEmpty());
        assertTrue(repository.findByTenantIdAndToken(2L, "gone").isPresent(), "other tenant's token survives");
    }
}
