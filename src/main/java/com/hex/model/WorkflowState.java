package com.hex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.List;

/**
 * Represents the persisted state of a workflow execution in DynamoDB.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDbBean
public class WorkflowState {

    private String requestNumber;
    private String taskNumber;
    private String loanNumber;
    private String reviewType;
    private String workflowStatus;
    private String loanDecision;
    private List<LoanAttribute> attributes;
    private String correlationId;
    private String executionArn;
    private String currentTaskToken;
    private Instant createdAt;
    private Instant updatedAt;
    private Long ttl;

    @DynamoDbPartitionKey
    public String getRequestNumber() {
        return requestNumber;
    }

    @DynamoDbSortKey
    public String getTaskNumber() {
        return taskNumber;
    }
}
