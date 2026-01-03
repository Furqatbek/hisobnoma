package com.hisobnoma.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 */
public class NotFoundException extends BusinessException {

    private static final String DEFAULT_CODE = "NOT_FOUND";

    public NotFoundException(String message) {
        super(message, DEFAULT_CODE, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with id: %d", resourceName, id), DEFAULT_CODE, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String resourceName, String field, Object value) {
        super(String.format("%s not found with %s: %s", resourceName, field, value), DEFAULT_CODE, HttpStatus.NOT_FOUND);
    }
}
