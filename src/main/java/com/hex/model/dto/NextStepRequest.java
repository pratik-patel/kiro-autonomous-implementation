package com.hex.model.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Request DTO for the getNextStep API endpoint.
 */
@Value
@Builder
public class NextStepRequest {
    String taskNumber;
    String requestNumber;
    String loanNumber;
    String loanDecision;
    List<LoanAttributeDto> attributes;
}
