package com.hisobnoma.platform.mobile.push.apns;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * Builds and caches the APNs provider-authentication JWT (ES256, signed with the {@code .p8} key).
 * Apple accepts a token for up to 1h and rate-limits new-token creation, so we reuse one and
 * refresh well before expiry rather than signing per push.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApnsJwtProvider {

    /** Refresh comfortably under Apple's 1h ceiling. */
    private static final long MAX_AGE_MILLIS = 50 * 60 * 1000L;

    private final ApnsProperties properties;

    private volatile ECPrivateKey signingKey;
    private volatile String cachedToken;
    private volatile long issuedAtMillis;

    /** @return a valid APNs auth JWT, or null if the key can't be loaded. */
    public synchronized String currentToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && (now - issuedAtMillis) < MAX_AGE_MILLIS) {
            return cachedToken;
        }
        ECPrivateKey key = key();
        if (key == null) {
            return null;
        }
        cachedToken = Jwts.builder()
                .header().keyId(properties.getKeyId()).and()
                .issuer(properties.getTeamId())
                .issuedAt(new Date(now))
                .signWith(key, Jwts.SIG.ES256)
                .compact();
        issuedAtMillis = now;
        return cachedToken;
    }

    private ECPrivateKey key() {
        if (signingKey != null) {
            return signingKey;
        }
        try {
            String pem = properties.getPrivateKey()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(pem);
            signingKey = (ECPrivateKey) KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
            return signingKey;
        } catch (Exception e) {
            log.error("APNs: failed to load .p8 signing key: {}", e.getMessage());
            return null;
        }
    }
}
