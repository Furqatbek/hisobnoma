package com.hisobnoma.platform.distribution.security;

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
 * Issues and validates tokens for distribution agents' mobile app.
 *
 * <p>Signed with a key DERIVED from the staff JWT secret
 * ({@code secret + "::distribution-agent"}), so an agent token can never pass the staff,
 * web-customer, or B2B signature check and vice versa — strict separation without touching
 * the staff security chain.
 */
@Service
@Slf4j
public class AgentTokenService {

    public static final String TOKEN_TYPE = "distribution_agent";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.agent-expiration:2592000000}") // 30 days
    private long expirationMillis;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(
                (jwtSecret + "::distribution-agent").getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long agentId, Long tenantId, String code) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(agentId))
                .claim("type", TOKEN_TYPE)
                .claim("tenantId", tenantId)
                .claim("code", code)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    /** @return the authenticated agent identity, or empty for any invalid/expired/foreign token. */
    public Optional<AgentPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!TOKEN_TYPE.equals(claims.get("type", String.class))) {
                return Optional.empty();
            }
            return Optional.of(new AgentPrincipal(
                    Long.parseLong(claims.getSubject()),
                    claims.get("tenantId", Long.class),
                    claims.get("code", String.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public record AgentPrincipal(Long agentId, Long tenantId, String code) {
    }
}
