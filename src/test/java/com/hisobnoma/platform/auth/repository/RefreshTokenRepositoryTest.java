package com.hisobnoma.platform.auth.repository;

import com.hisobnoma.platform.auth.entity.RefreshToken;
import com.hisobnoma.platform.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class RefreshTokenRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .phone("+999000000")
                .passwordHash("$2a$10$hash")
                .enabled(true)
                .roles(new HashSet<>())
                .build();
        testUser.setTenantId(1L);
        em.persistAndFlush(testUser);
    }

    private RefreshToken persistToken(String tokenStr, boolean revoked, Instant expiresAt) {
        RefreshToken t = RefreshToken.builder()
                .token(tokenStr)
                .user(testUser)
                .revoked(revoked)
                .expiresAt(expiresAt)
                .ipAddress("127.0.0.1")
                .deviceInfo("test")
                .build();
        return em.persistAndFlush(t);
    }

    @Test
    void revokeAllByUser_multipleTokens_setsAllRevoked() {
        // Given — 3 active tokens
        persistToken("tok1", false, Instant.now().plus(1, ChronoUnit.HOURS));
        persistToken("tok2", false, Instant.now().plus(1, ChronoUnit.HOURS));
        persistToken("tok3", false, Instant.now().plus(1, ChronoUnit.HOURS));

        // When
        refreshTokenRepository.revokeAllByUser(testUser);
        em.flush();
        em.clear();

        // Then — reload and verify all revoked
        List<RefreshToken> all = refreshTokenRepository.findAll();
        assertEquals(3, all.size());
        all.forEach(t -> assertTrue(t.isRevoked()));
    }

    @Test
    void findAllByUserAndRevokedFalse_mixedTokens_returnsOnlyActive() {
        // Given — plan calls this "findValidTokensByUser"; actual method returns unrevoked tokens
        persistToken("active1", false, Instant.now().plus(1, ChronoUnit.HOURS));
        persistToken("active2", false, Instant.now().plus(2, ChronoUnit.HOURS));
        persistToken("revoked1", true, Instant.now().plus(1, ChronoUnit.HOURS));

        // When
        List<RefreshToken> result = refreshTokenRepository.findAllByUserAndRevokedFalse(testUser);

        // Then
        assertEquals(2, result.size());
        result.forEach(t -> assertFalse(t.isRevoked()));
    }

    @Test
    void deleteAllExpired_pastExpiry_removesExpiredOnly() {
        // Given — plan says "findExpiredTokens"; actual API is deleteAllExpired
        Instant now = Instant.now();
        persistToken("expired1", false, now.minus(1, ChronoUnit.HOURS));
        persistToken("expired2", false, now.minus(30, ChronoUnit.MINUTES));
        persistToken("future1", false, now.plus(1, ChronoUnit.HOURS));

        // When
        refreshTokenRepository.deleteAllExpired(now);
        em.flush();
        em.clear();

        // Then
        List<RefreshToken> remaining = refreshTokenRepository.findAll();
        assertEquals(1, remaining.size());
        assertEquals("future1", remaining.get(0).getToken());
    }
}
