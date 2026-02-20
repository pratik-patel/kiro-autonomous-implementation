package com.hex.model.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Request DTO for the startPPAreview API endpoint.
 */
@Value
@Builder
public class StartWorkflowRequest {
    String requestNumber;
    String loanNumber;
    String requestType;
    List<LoanAttributeDto> attributes;
}
