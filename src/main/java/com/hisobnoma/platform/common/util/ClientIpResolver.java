package com.hisobnoma.platform.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single place to resolve the caller's IP for rate limiting and audit
 * logging.
 *
 * X-Forwarded-For is client-controlled: it is only meaningful when the app
 * sits behind a reverse proxy that overwrites it (nginx, ALB). Installs
 * that expose the app directly must set
 * {@code app.security.trust-proxy-headers=false} (TRUST_PROXY_HEADERS env
 * var), otherwise callers can spoof their IP and bypass per-IP rate limits
 * on OTP, checkout and coupon endpoints.
 */
@Component
public class ClientIpResolver {

    private final boolean trustProxyHeaders;

    public ClientIpResolver(
            @Value("${app.security.trust-proxy-headers:true}") boolean trustProxyHeaders) {
        this.trustProxyHeaders = trustProxyHeaders;
    }

    public String resolve(HttpServletRequest request) {
        if (trustProxyHeaders) {
            // nginx sets X-Real-IP to the actual connecting peer ($remote_addr), overwriting any
            // client-supplied value, so it is trustworthy. Prefer it.
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
            // Fall back to the RIGHTMOST X-Forwarded-For entry. nginx appends the real peer at the
            // end ($proxy_add_x_forwarded_for); the leftmost entries are client-supplied and
            // spoofable, so taking the leftmost let a caller forge their IP and bypass per-IP limits.
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] parts = forwarded.split(",");
                for (int i = parts.length - 1; i >= 0; i--) {
                    String candidate = parts[i].trim();
                    if (!candidate.isEmpty()) {
                        return candidate;
                    }
                }
            }
        }
        return request.getRemoteAddr();
    }
}
