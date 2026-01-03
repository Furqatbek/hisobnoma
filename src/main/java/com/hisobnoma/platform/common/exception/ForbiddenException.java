package com.hisobnoma.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when access to a resource is forbidden.
 */
public class ForbiddenException extends BusinessException {

    private static final String DEFAULT_CODE = "FORBIDDEN";

    public ForbiddenException() {
        super("Access denied", DEFAULT_CODE, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String message) {
        super(message, DEFAULT_CODE, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String message, String code) {
        super(message, code, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String resource, String action) {
        super(String.format("You don't have permission to %s %s", action, resource), DEFAULT_CODE, HttpStatus.FORBIDDEN);
    }
}
