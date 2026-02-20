package com.hex.repository;

import com.hex.config.DynamoDBConfig;
import com.hex.model.LoanAttribute;
import com.hex.model.WorkflowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for WorkflowState persistence in DynamoDB.
 */
@Repository
public class WorkflowRepository {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRepository.class);

    private final DynamoDbTable<WorkflowState> workflowTable;

    public WorkflowRepository(DynamoDbEnhancedClient enhancedClient, DynamoDBConfig dynamoDBConfig) {
        this.workflowTable = enhancedClient.table(
                dynamoDBConfig.getTableName(),
                TableSchema.fromBean(WorkflowState.class));
    }

    /**
     * Persists a workflow state to DynamoDB.
     *
     * @param state the workflow state to save
     */
    public void save(WorkflowState state) {
        state.setUpdatedAt(Instant.now());
        workflowTable.putItem(state);
        log.info("Saved workflow state: requestNumber={}, taskNumber={}",
                state.getRequestNumber(), state.getTaskNumber());
    }

    /**
     * Retrieves a workflow state by its composite key.
     *
     * @param requestNumber the partition key
     * @param taskNumber the sort key
     * @return the workflow state, or empty if not found
     */
    public Optional<WorkflowState> findByRequestAndTask(String requestNumber, String taskNumber) {
        Key key = Key.builder()
                .partitionValue(requestNumber)
                .sortValue(taskNumber)
                .build();
        WorkflowState result = workflowTable.getItem(key);
        return Optional.ofNullable(result);
    }

    /**
     * Updates the workflow status field for an existing record.
     *
     * @param requestNumber the partition key
     * @param taskNumber the sort key
     * @param status the new workflow status
     */
    public void updateStatus(String requestNumber, String taskNumber, String status) {
        findByRequestAndTask(requestNumber, taskNumber).ifPresent(state -> {
            state.setWorkflowStatus(status);
            state.setUpdatedAt(Instant.now());
            workflowTable.putItem(state);
            log.info("Updated workflow status: requestNumber={}, taskNumber={}, status={}",
                    requestNumber, taskNumber, status);
        });
    }

    /**
     * Updates the loan decision and attributes for an existing record.
     *
     * @param requestNumber the partition key
     * @param taskNumber the sort key
     * @param decision the loan decision string
     * @param attributes the updated list of loan attributes
     */
    public void updateDecision(String requestNumber, String taskNumber,
                               String decision, List<LoanAttribute> attributes) {
        findByRequestAndTask(requestNumber, taskNumber).ifPresent(state -> {
            state.setLoanDecision(decision);
            state.setAttributes(attributes);
            state.setUpdatedAt(Instant.now());
            workflowTable.putItem(state);
            log.info("Updated workflow decision: requestNumber={}, taskNumber={}, decision={}",
                    requestNumber, taskNumber, decision);
        });
    }
}
