package com.hisobnoma.platform.web.exception;

import com.hisobnoma.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Maps to HTTP 429 via the BusinessException handler in GlobalExceptionHandler.
 */
public class TooManyRequestsException extends BusinessException {

    public TooManyRequestsException(String message) {
        super(message, "TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS);
    }
}
