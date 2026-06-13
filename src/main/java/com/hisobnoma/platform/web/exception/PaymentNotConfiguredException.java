package com.hisobnoma.platform.web.exception;

import com.hisobnoma.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Raised when a customer tries to pay via a provider that has no merchant
 * credentials configured. Maps to HTTP 503 — the feature exists but is not
 * provisioned in this environment, so the app should fall back (e.g. offer
 * cash on delivery) rather than treat it as a hard failure.
 */
public class PaymentNotConfiguredException extends BusinessException {

    public PaymentNotConfiguredException(String message) {
        super(message, "PAYMENT_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
