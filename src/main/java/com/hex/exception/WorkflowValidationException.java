package com.hex.exception;

/**
 * Thrown when API input validation fails (HTTP 400).
 */
public class WorkflowValidationException extends RuntimeException {

    private final String errorCode;

    public WorkflowValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public WorkflowValidationException(String message) {
        this(message, "VALIDATION_ERROR");
    }

    public String getErrorCode() {
        return errorCode;
    }
}
