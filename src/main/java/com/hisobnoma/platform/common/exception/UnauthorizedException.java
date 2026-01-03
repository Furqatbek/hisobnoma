package com.hisobnoma.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication fails or is required.
 */
public class UnauthorizedException extends BusinessException {

    private static final String DEFAULT_CODE = "UNAUTHORIZED";

    public UnauthorizedException() {
        super("Authentication required", DEFAULT_CODE, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(message, DEFAULT_CODE, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message, String code) {
        super(message, code, HttpStatus.UNAUTHORIZED);
    }
}
