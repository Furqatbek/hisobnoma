package com.hisobnoma.platform.distribution.b2b.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and validates tokens for B2B wholesale buyers (finance customers).
 *
 * <p>Signed with a key DERIVED from the staff JWT secret ({@code secret + "::b2b-customer"}),
 * so a B2B token can never pass the staff or the web-customer signature check and vice versa —
 * strict separation without touching the staff security chain.
 */
@Service
@Slf4j
public class B2bCustomerTokenService {

    public static final String TOKEN_TYPE = "b2b_customer";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.b2b-customer-expiration:2592000000}") // 30 days
    private long expirationMillis;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor((jwtSecret + "::b2b-customer").getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long customerId, Long tenantId, String code) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(customerId))
                .claim("type", TOKEN_TYPE)
                .claim("tenantId", tenantId)
                .claim("code", code)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    /** @return the authenticated buyer identity, or empty for any invalid/expired/foreign token. */
    public Optional<B2bCustomerPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!TOKEN_TYPE.equals(claims.get("type", String.class))) {
                return Optional.empty();
            }
            return Optional.of(new B2bCustomerPrincipal(
                    Long.parseLong(claims.getSubject()),
                    claims.get("tenantId", Long.class),
                    claims.get("code", String.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public record B2bCustomerPrincipal(Long customerId, Long tenantId, String code) {
    }
}
