package com.hisobnoma.platform.auth.repository;

import com.hisobnoma.platform.auth.entity.PasswordResetToken;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class PasswordResetTokenRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("pwreset_user")
                .phone("+999111111111")
                .passwordHash("$2a$10$hash")
                .enabled(true)
                .roles(new HashSet<>())
                .build();
        testUser.setTenantId(1L);
        em.persistAndFlush(testUser);
    }

    @Test
    void findByToken_existingToken_returnsEntity() {
        // Given — future-expiry token
        PasswordResetToken token = PasswordResetToken.builder()
                .token("valid-token-uuid")
                .user(testUser)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(false)
                .build();
        em.persistAndFlush(token);

        // When
        Optional<PasswordResetToken> result = passwordResetTokenRepository.findByToken("valid-token-uuid");

        // Then
        assertTrue(result.isPresent());
        assertEquals("valid-token-uuid", result.get().getToken());
    }

    @Test
    void findValidToken_futureExpiryAndNotUsed_returnsEntity() {
        // Given
        PasswordResetToken token = PasswordResetToken.builder()
                .token("valid")
                .user(testUser)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(false)
                .build();
        em.persistAndFlush(token);

        // When
        Optional<PasswordResetToken> result = passwordResetTokenRepository
                .findValidToken("valid", Instant.now());

        // Then
        assertTrue(result.isPresent());
    }

    @Test
    void findValidToken_expiredToken_returnsEmpty() {
        // Given
        PasswordResetToken expired = PasswordResetToken.builder()
                .token("expired")
                .user(testUser)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .used(false)
                .build();
        em.persistAndFlush(expired);

        // When
        Optional<PasswordResetToken> result = passwordResetTokenRepository
                .findValidToken("expired", Instant.now());

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findValidToken_usedToken_returnsEmpty() {
        // Given
        PasswordResetToken used = PasswordResetToken.builder()
                .token("used")
                .user(testUser)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(true)
                .build();
        em.persistAndFlush(used);

        // When
        Optional<PasswordResetToken> result = passwordResetTokenRepository
                .findValidToken("used", Instant.now());

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void invalidateAllByUser_multipleTokens_marksAllUsed() {
        // Given
        PasswordResetToken t1 = PasswordResetToken.builder()
                .token("t1").user(testUser)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(false).build();
        PasswordResetToken t2 = PasswordResetToken.builder()
                .token("t2").user(testUser)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(false).build();
        em.persistAndFlush(t1);
        em.persistAndFlush(t2);

        // When
        passwordResetTokenRepository.invalidateAllByUser(testUser);
        em.flush();
        em.clear();

        // Then
        passwordResetTokenRepository.findAll().forEach(t -> assertTrue(t.isUsed()));
    }
}
