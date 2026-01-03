package com.hisobnoma.platform.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when validation fails.
 */
@Getter
public class ValidationException extends BusinessException {

    private static final String DEFAULT_CODE = "VALIDATION_FAILED";

    private final List<FieldError> fieldErrors;

    public ValidationException(String message) {
        super(message, DEFAULT_CODE, HttpStatus.BAD_REQUEST);
        this.fieldErrors = new ArrayList<>();
    }

    public ValidationException(String message, List<FieldError> fieldErrors) {
        super(message, DEFAULT_CODE, HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors != null ? fieldErrors : new ArrayList<>();
    }

    public ValidationException(String field, String message) {
        super(message, DEFAULT_CODE, HttpStatus.BAD_REQUEST);
        this.fieldErrors = List.of(new FieldError(field, message, null));
    }

    public ValidationException(String field, String message, Object rejectedValue) {
        super(message, DEFAULT_CODE, HttpStatus.BAD_REQUEST);
        this.fieldErrors = List.of(new FieldError(field, message, rejectedValue));
    }

    public record FieldError(String field, String message, Object rejectedValue) {}
}
