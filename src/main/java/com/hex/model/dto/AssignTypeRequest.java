package com.hex.model.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Request DTO for the assignToType API endpoint.
 */
@Value
@Builder
public class AssignTypeRequest {
    String taskNumber;
    String requestNumber;
    String loanNumber;
    String reviewType;
}
