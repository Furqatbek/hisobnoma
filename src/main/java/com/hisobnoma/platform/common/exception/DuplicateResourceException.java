package com.hisobnoma.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to create a duplicate resource.
 */
public class DuplicateResourceException extends BusinessException {

    private static final String DEFAULT_CODE = "DUPLICATE_RESOURCE";

    public DuplicateResourceException(String message) {
        super(message, DEFAULT_CODE, HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String resourceName, String field, Object value) {
        super(String.format("%s already exists with %s: %s", resourceName, field, value), DEFAULT_CODE, HttpStatus.CONFLICT);
    }
}
