package com.hisobnoma.platform.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    /**
     * The placeholder shipped in application.yml. Signing production tokens
     * with a published secret would let anyone forge staff JWTs, so startup
     * fails when the prod profile still runs on it.
     */
    static final String DEFAULT_PLACEHOLDER_SECRET =
            "your-256-bit-secret-key-here-change-in-production-environment";

    /** 64 bytes = 512 bits, the minimum HMAC key size for HS512. */
    static final int MIN_SECRET_BYTES = 64;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${app.jwt.issuer}")
    private String issuer;

    private final Environment environment;

    private SecretKey key;

    public JwtTokenProvider(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        validateSecret();
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** Profiles under which a weak/placeholder JWT secret is tolerated (local development / tests). */
    private static final Set<String> SAFE_PROFILES = Set.of("dev", "test", "local");

    private void validateSecret() {
        boolean isDefault = DEFAULT_PLACEHOLDER_SECRET.equals(jwtSecret);
        boolean tooShort = jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES;
        if (!isDefault && !tooShort) {
            return;
        }
        String problem = isDefault
                ? "JWT secret is the published default placeholder"
                : "JWT secret is shorter than " + MIN_SECRET_BYTES + " bytes (512 bits)";

        // Fail closed for ANY non-development context — not just the literal "prod" profile. A weak
        // secret is only tolerated with no profile at all (local dev) or when every active profile is
        // a recognised dev/test one; a server booting "prod", "staging", "production", or a typo'd
        // profile must refuse to start rather than silently run on a forgeable secret.
        String[] active = environment.getActiveProfiles();
        boolean safeContext = active.length == 0
                || Arrays.stream(active).allMatch(SAFE_PROFILES::contains);
        if (!safeContext) {
            throw new IllegalStateException(problem
                    + " — refusing to start with profile(s) " + Arrays.toString(active)
                    + ". Set the JWT_SECRET environment variable to a random string of at least"
                    + " 64 characters.");
        }
        log.warn("{} — acceptable for dev/test only. Set JWT_SECRET (>= 64 chars) before"
                + " deploying to production.", problem);
    }

    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(userPrincipal);
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        Set<String> permissions = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("userId", userPrincipal.getId())
                .claim("tenantId", userPrincipal.getTenantId())
                .claim("permissions", permissions)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("userId", Long.class);
    }

    public Long getTenantIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("tenantId", Long.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException ex) {
            log.warn("Invalid JWT token ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
