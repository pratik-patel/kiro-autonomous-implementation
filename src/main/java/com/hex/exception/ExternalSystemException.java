package com.hex.exception;

/**
 * Thrown when an external system integration fails (HTTP 502).
 */
public class ExternalSystemException extends RuntimeException {

    public ExternalSystemException(String message) {
        super(message);
    }

    public ExternalSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
