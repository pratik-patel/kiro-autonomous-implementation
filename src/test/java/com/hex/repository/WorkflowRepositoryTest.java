package com.hex.repository;

import com.hex.config.DynamoDBConfig;
import com.hex.model.AttributeStatus;
import com.hex.model.LoanAttribute;
import com.hex.model.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowRepository unit tests")
class WorkflowRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<WorkflowState> workflowTable;

    @Mock
    private DynamoDBConfig dynamoDBConfig;

    private WorkflowRepository repository;

    @BeforeEach
    void setUp() {
        when(dynamoDBConfig.getTableName()).thenReturn("ldc-workflow-state-test");
        when(enhancedClient.table(eq("ldc-workflow-state-test"), any(TableSchema.class)))
                .thenReturn(workflowTable);
        repository = new WorkflowRepository(enhancedClient, dynamoDBConfig);
    }

    @Test
    @DisplayName("save persists workflow state and sets updatedAt")
    void savePersistsState() {
        WorkflowState state = WorkflowState.builder()
                .requestNumber("REQ-123456")
                .taskNumber("TSK-123456")
                .loanNumber("LN-123456")
                .workflowStatus("INITIALIZED")
                .build();

        repository.save(state);

        ArgumentCaptor<WorkflowState> captor = ArgumentCaptor.forClass(WorkflowState.class);
        verify(workflowTable).putItem(captor.capture());
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        assertThat(captor.getValue().getRequestNumber()).isEqualTo("REQ-123456");
    }

    @Test
    @DisplayName("findByRequestAndTask returns state when found")
    void findByRequestAndTaskReturnsState() {
        WorkflowState expected = WorkflowState.builder()
                .requestNumber("REQ-123456")
                .taskNumber("TSK-123456")
                .build();
        when(workflowTable.getItem(any(Key.class))).thenReturn(expected);

        Optional<WorkflowState> result = repository.findByRequestAndTask("REQ-123456", "TSK-123456");

        assertThat(result).isPresent();
        assertThat(result.get().getRequestNumber()).isEqualTo("REQ-123456");
    }

    @Test
    @DisplayName("findByRequestAndTask returns empty when not found")
    void findByRequestAndTaskReturnsEmpty() {
        when(workflowTable.getItem(any(Key.class))).thenReturn(null);

        Optional<WorkflowState> result = repository.findByRequestAndTask("REQ-999999", "TSK-999999");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("updateStatus updates the status field")
    void updateStatusUpdatesField() {
        WorkflowState existing = WorkflowState.builder()
                .requestNumber("REQ-123456")
                .taskNumber("TSK-123456")
                .workflowStatus("INITIALIZED")
                .build();
        when(workflowTable.getItem(any(Key.class))).thenReturn(existing);

        repository.updateStatus("REQ-123456", "TSK-123456", "COMPLETED");

        ArgumentCaptor<WorkflowState> captor = ArgumentCaptor.forClass(WorkflowState.class);
        verify(workflowTable).putItem(captor.capture());
        assertThat(captor.getValue().getWorkflowStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("updateDecision updates decision and attributes")
    void updateDecisionUpdatesFields() {
        WorkflowState existing = WorkflowState.builder()
                .requestNumber("REQ-123456")
                .taskNumber("TSK-123456")
                .build();
        when(workflowTable.getItem(any(Key.class))).thenReturn(existing);

        List<LoanAttribute> attributes = List.of(
                LoanAttribute.builder().attributeName("attr1").attributeStatus(AttributeStatus.APPROVED).build());

        repository.updateDecision("REQ-123456", "TSK-123456", "APPROVED", attributes);

        ArgumentCaptor<WorkflowState> captor = ArgumentCaptor.forClass(WorkflowState.class);
        verify(workflowTable).putItem(captor.capture());
        assertThat(captor.getValue().getLoanDecision()).isEqualTo("APPROVED");
        assertThat(captor.getValue().getAttributes()).hasSize(1);
    }
}
