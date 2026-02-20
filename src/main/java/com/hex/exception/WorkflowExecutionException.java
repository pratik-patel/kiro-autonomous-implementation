package com.hex.exception;

/**
 * Thrown when an unexpected internal error occurs during workflow execution (HTTP 500).
 */
public class WorkflowExecutionException extends RuntimeException {

    public WorkflowExecutionException(String message) {
        super(message);
    }

    public WorkflowExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
