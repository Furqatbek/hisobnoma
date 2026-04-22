package com.hisobnoma.platform.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private SecretKey testKey;

    private static final String TEST_SECRET = "test-secret-key-at-least-256-bits-long-for-hmac-sha256-algorithm";
    private static final long ACCESS_EXP_MS = 3600000L; // 1 hour
    private static final long REFRESH_EXP_MS = 604800000L; // 7 days
    private static final String ISSUER = "test-issuer";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpiration", ACCESS_EXP_MS);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpiration", REFRESH_EXP_MS);
        ReflectionTestUtils.setField(tokenProvider, "issuer", ISSUER);
        tokenProvider.init();

        testKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private UserPrincipal buildPrincipal(String username, Long userId, Long tenantId) {
        return new UserPrincipal(
                userId, username, "pwd", tenantId, true, true,
                List.of(new SimpleGrantedAuthority("INVENTORY_READ")));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ---- generateAccessToken ----

    @Test
    void generateToken_returnsNonNullJwtString() {
        // Given
        UserPrincipal principal = buildPrincipal("alice", 1L, 42L);

        // When
        String token = tokenProvider.generateAccessToken(principal);

        // Then
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(3, token.split("\\.").length, "JWT should have 3 dot-separated segments");
    }

    @Test
    void generateToken_containsSubjectClaim() {
        // Given
        UserPrincipal principal = buildPrincipal("alice", 1L, 42L);

        // When
        String token = tokenProvider.generateAccessToken(principal);

        // Then
        assertEquals("alice", parseClaims(token).getSubject());
    }

    @Test
    void generateToken_containsTenantIdClaim() {
        // Given
        UserPrincipal principal = buildPrincipal("bob", 1L, 42L);

        // When
        String token = tokenProvider.generateAccessToken(principal);

        // Then
        Long tenantId = parseClaims(token).get("tenantId", Long.class);
        assertEquals(42L, tenantId);
    }

    @Test
    void generateToken_containsUserIdClaim() {
        // Given
        UserPrincipal principal = buildPrincipal("bob", 99L, 42L);

        // When
        String token = tokenProvider.generateAccessToken(principal);

        // Then
        Long userId = parseClaims(token).get("userId", Long.class);
        assertEquals(99L, userId);
    }

    @Test
    void generateToken_expiryEqualsNowPlusExpirationTime() {
        // Given
        UserPrincipal principal = buildPrincipal("alice", 1L, 42L);
        long beforeMs = System.currentTimeMillis();

        // When
        String token = tokenProvider.generateAccessToken(principal);

        // Then
        long afterMs = System.currentTimeMillis();
        Date expiration = parseClaims(token).getExpiration();
        long expMs = expiration.getTime();
        // expiration should be within [beforeMs + ACCESS_EXP_MS, afterMs + ACCESS_EXP_MS]
        // JWT exp is in seconds, so allow 1s tolerance
        assertTrue(expMs >= beforeMs + ACCESS_EXP_MS - 1000,
                "Expiration too early: " + expMs + " vs expected " + (beforeMs + ACCESS_EXP_MS));
        assertTrue(expMs <= afterMs + ACCESS_EXP_MS + 1000,
                "Expiration too late: " + expMs + " vs expected " + (afterMs + ACCESS_EXP_MS));
    }

    // ---- getUsernameFromToken ----

    @Test
    void extractUsername_validToken_returnsCorrectUsername() {
        // Given
        UserPrincipal principal = buildPrincipal("bob", 1L, 42L);
        String token = tokenProvider.generateAccessToken(principal);

        // When
        String username = tokenProvider.getUsernameFromToken(token);

        // Then
        assertEquals("bob", username);
    }

    @Test
    void extractUsername_malformedToken_throwsJwtException() {
        // When / Then
        assertThrows(JwtException.class, () ->
                tokenProvider.getUsernameFromToken("not.a.jwt"));
    }

    @Test
    void extractUsername_expiredToken_throwsExpiredJwtException() {
        // Given — build a token with past expiration directly using the same key
        String expiredToken = Jwts.builder()
                .subject("alice")
                .issuer(ISSUER)
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(testKey)
                .compact();

        // When / Then
        assertThrows(ExpiredJwtException.class, () ->
                tokenProvider.getUsernameFromToken(expiredToken));
    }

    // ---- getTenantIdFromToken ----

    @Test
    void extractTenantId_validToken_returnsCorrectLong() {
        // Given
        UserPrincipal principal = buildPrincipal("alice", 1L, 99L);
        String token = tokenProvider.generateAccessToken(principal);

        // When
        Long tenantId = tokenProvider.getTenantIdFromToken(token);

        // Then
        assertEquals(99L, tenantId);
    }

    @Test
    void extractTenantId_tokenWithoutClaim_returnsNull() {
        // Given — refresh token has no tenantId claim
        String refreshToken = tokenProvider.generateRefreshToken("alice");

        // When
        Long tenantId = tokenProvider.getTenantIdFromToken(refreshToken);

        // Then
        assertNull(tenantId);
    }

    // ---- validateToken ----

    @Test
    void validateToken_validToken_returnsTrue() {
        // Given
        UserPrincipal principal = buildPrincipal("alice", 1L, 42L);
        String token = tokenProvider.generateAccessToken(principal);

        // When
        boolean result = tokenProvider.validateToken(token);

        // Then
        assertTrue(result);
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        // Given
        String expiredToken = Jwts.builder()
                .subject("alice")
                .issuer(ISSUER)
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(testKey)
                .compact();

        // When
        boolean result = tokenProvider.validateToken(expiredToken);

        // Then
        assertFalse(result);
    }

    @Test
    void validateToken_tamperedSignatureToken_returnsFalse() {
        // Given
        UserPrincipal principal = buildPrincipal("alice", 1L, 42L);
        String token = tokenProvider.generateAccessToken(principal);
        // Replace entire signature segment with garbage
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

        // When
        boolean result = tokenProvider.validateToken(tampered);

        // Then
        assertFalse(result);
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        // When
        boolean result = tokenProvider.validateToken("garbage.garbage");

        // Then
        assertFalse(result);
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        // When / Then — should not throw NPE
        assertDoesNotThrow(() -> tokenProvider.validateToken(null));
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    void validateToken_emptyToken_returnsFalse() {
        // When
        boolean result = tokenProvider.validateToken("");

        // Then
        assertFalse(result);
    }

    // ---- getUserIdFromToken (bonus — method exists) ----

    @Test
    void extractUserId_validToken_returnsCorrectLong() {
        // Given
        UserPrincipal principal = buildPrincipal("alice", 77L, 42L);
        String token = tokenProvider.generateAccessToken(principal);

        // When
        Long userId = tokenProvider.getUserIdFromToken(token);

        // Then
        assertEquals(77L, userId);
    }

    // ---- Expiration via parseClaims (plan's getExpirationDate equivalent) ----

    @Test
    void getExpirationDate_validToken_returnsCorrectDate() {
        // Given — plan references getExpirationDate which doesn't exist;
        // verify expiration via claims parsing instead
        UserPrincipal principal = buildPrincipal("alice", 1L, 42L);
        long beforeMs = System.currentTimeMillis();
        String token = tokenProvider.generateAccessToken(principal);

        // When
        Date expiration = parseClaims(token).getExpiration();

        // Then
        long expMs = expiration.getTime();
        assertTrue(expMs >= beforeMs + ACCESS_EXP_MS - 1000);
        assertTrue(expMs <= System.currentTimeMillis() + ACCESS_EXP_MS + 1000);
    }

    @Test
    void parseClaims_malformedToken_throwsJwtException() {
        // When / Then
        assertThrows(JwtException.class, () ->
                tokenProvider.getUsernameFromToken("bad.token.string"));
    }
}
