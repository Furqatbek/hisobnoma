package com.hisobnoma.platform.web.security;

import com.hisobnoma.platform.web.entity.WebCustomer;
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
 * Issues and validates tokens for online shop customers.
 *
 * Tokens are signed with a key DERIVED from the staff JWT secret
 * (secret + "::web-customer"), so a web-customer token can never pass the
 * staff {@code JwtTokenProvider} signature check and vice versa — strict
 * separation without touching the staff security chain.
 */
@Service
@Slf4j
public class WebCustomerTokenService {

    public static final String TOKEN_TYPE = "web_customer";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.web-customer-expiration:2592000000}") // 30 days
    private long expirationMillis;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(
                (jwtSecret + "::web-customer").getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(WebCustomer customer) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(customer.getId()))
                .claim("type", TOKEN_TYPE)
                .claim("tenantId", customer.getTenantId())
                .claim("phone", customer.getPhone())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    /**
     * @return the authenticated web customer identity, or empty for any
     * invalid/expired/foreign (e.g. staff) token.
     */
    public Optional<WebCustomerPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!TOKEN_TYPE.equals(claims.get("type", String.class))) {
                return Optional.empty();
            }
            return Optional.of(new WebCustomerPrincipal(
                    Long.parseLong(claims.getSubject()),
                    claims.get("tenantId", Long.class),
                    claims.get("phone", String.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public record WebCustomerPrincipal(Long webCustomerId, Long tenantId, String phone) {
    }
}
