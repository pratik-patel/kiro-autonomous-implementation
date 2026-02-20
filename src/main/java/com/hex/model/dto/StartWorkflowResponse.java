package com.hex.model.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Response DTO returned after successfully starting a workflow.
 */
@Value
@Builder
public class StartWorkflowResponse {
    String taskNumber;
    String executionArn;
}
