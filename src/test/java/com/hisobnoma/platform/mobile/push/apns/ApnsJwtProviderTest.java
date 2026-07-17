package com.hisobnoma.platform.mobile.push.apns;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the ES256 provider JWT: it carries the team id as issuer and the key id in the header,
 * verifies against the matching public key, and is cached (same token returned within its max age).
 */
class ApnsJwtProviderTest {

    /** Generate a throwaway P-256 keypair and expose the private key as a .p8-style PKCS#8 PEM. */
    private static KeyPair p256() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        return gen.generateKeyPair();
    }

    private static String toPem(KeyPair kp) {
        String b64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + b64 + "\n-----END PRIVATE KEY-----\n";
    }

    private ApnsProperties props(KeyPair kp) {
        ApnsProperties p = new ApnsProperties();
        p.setEnabled(true);
        p.setTeamId("TEAM123456");
        p.setKeyId("KEY7654321");
        p.setBundleId("com.hisobnoma.admin");
        p.setPrivateKey(toPem(kp));
        return p;
    }

    @Test
    void currentToken_signsValidEs256JwtWithTeamAndKeyId() throws Exception {
        KeyPair kp = p256();
        ApnsJwtProvider provider = new ApnsJwtProvider(props(kp));

        String jwt = provider.currentToken();
        assertNotNull(jwt);

        Jws<Claims> parsed = Jwts.parser()
                .verifyWith((ECPublicKey) kp.getPublic())
                .build()
                .parseSignedClaims(jwt);

        assertEquals("TEAM123456", parsed.getPayload().getIssuer());
        assertEquals("KEY7654321", parsed.getHeader().getKeyId());
        assertEquals("ES256", parsed.getHeader().getAlgorithm());
        assertNotNull(parsed.getPayload().getIssuedAt());
    }

    @Test
    void currentToken_isCachedAcrossCalls() throws Exception {
        ApnsJwtProvider provider = new ApnsJwtProvider(props(p256()));
        assertSame(provider.currentToken(), provider.currentToken(),
                "the same JWT string instance is reused within its max age");
    }

    @Test
    void currentToken_returnsNullWhenKeyUnparseable() {
        ApnsProperties p = new ApnsProperties();
        p.setEnabled(true);
        p.setTeamId("T");
        p.setKeyId("K");
        p.setPrivateKey("-----BEGIN PRIVATE KEY-----\nnot-base64!!!\n-----END PRIVATE KEY-----");
        ApnsJwtProvider provider = new ApnsJwtProvider(p);

        assertNull(provider.currentToken());
    }
}
