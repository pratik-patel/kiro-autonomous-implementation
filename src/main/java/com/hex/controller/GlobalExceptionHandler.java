package com.hex.controller;

import com.hex.exception.ExternalSystemException;
import com.hex.exception.WorkflowExecutionException;
import com.hex.exception.WorkflowNotFoundException;
import com.hex.exception.WorkflowValidationException;
import com.hex.model.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handling for all REST endpoints.
 * Maps domain exceptions to standardized ApiResponse with appropriate HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors (HTTP 400).
     */
    @ExceptionHandler(WorkflowValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(WorkflowValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles workflow not found errors (HTTP 404).
     */
    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(WorkflowNotFoundException ex) {
        log.warn("Workflow not found: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode("WORKFLOW_NOT_FOUND")
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles external system integration failures (HTTP 502).
     */
    @ExceptionHandler(ExternalSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalSystemException(ExternalSystemException ex) {
        log.error("External system error: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("External system unavailable")
                .errorCode("EXTERNAL_SYSTEM_ERROR")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    /**
     * Handles unexpected internal errors (HTTP 500).
     */
    @ExceptionHandler(WorkflowExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleExecutionException(WorkflowExecutionException ex) {
        log.error("Workflow execution error: {}", ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Internal workflow error")
                .errorCode("INTERNAL_ERROR")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Catch-all handler for unexpected exceptions (HTTP 500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("An unexpected error occurred")
                .errorCode("INTERNAL_ERROR")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
