package com.hex.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Standardized API response wrapper for all endpoints.
 *
 * @param <T> the type of the response data payload
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    boolean success;
    String message;
    String correlationId;
    T data;
    String errorCode;
}
