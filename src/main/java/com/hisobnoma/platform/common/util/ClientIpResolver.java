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
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
