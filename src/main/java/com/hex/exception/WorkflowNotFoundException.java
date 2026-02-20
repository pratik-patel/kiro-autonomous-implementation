package com.hex.exception;

/**
 * Thrown when a requested workflow is not found (HTTP 404).
 */
public class WorkflowNotFoundException extends RuntimeException {

    public WorkflowNotFoundException(String message) {
        super(message);
    }
}
