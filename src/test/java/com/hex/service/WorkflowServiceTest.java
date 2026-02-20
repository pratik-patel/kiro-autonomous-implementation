package com.hex.service;

import com.hex.config.StepFunctionsConfig;
import com.hex.exception.WorkflowExecutionException;
import com.hex.exception.WorkflowNotFoundException;
import com.hex.model.WorkflowState;
import com.hex.model.dto.AssignTypeRequest;
import com.hex.model.dto.NextStepRequest;
import com.hex.model.dto.StartWorkflowRequest;
import com.hex.model.dto.StartWorkflowResponse;
import com.hex.repository.WorkflowRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowService unit tests")
class WorkflowServiceTest {

    private static final String REQUEST_NUMBER = "REQ-123456";
    private static final String LOAN_NUMBER = "LN-123456";
    private static final String TASK_NUMBER = "TSK-ABCD1234";
    private static final String TASK_TOKEN = "token-abc-123";
    private static final String EXECUTION_ARN = "arn:aws:states:us-east-1:000:execution:test:exec-123";
    private static final String STATE_MACHINE_ARN = "arn:aws:states:us-east-1:000:stateMachine:test";
    private static final String REVIEW_TYPE_LDC = "LDC";
    private static final String MISSING_REQUEST_NUMBER = "REQ-999999";
    private static final String MISSING_TASK_NUMBER = "TSK-NOTFOUND";
    private static final String JSON_PAYLOAD = "{\"json\":\"payload\"}";

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private StepFunctionsClientWrapper stepFunctionsClient;

    @Mock
    private StepFunctionsConfig stepFunctionsConfig;

    @Mock
    private ObjectMapper objectMapper;

    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        workflowService = new WorkflowService(
                workflowRepository, stepFunctionsClient, stepFunctionsConfig, objectMapper);
    }

    private StartWorkflowRequest createStartRequest() {
        return StartWorkflowRequest.builder()
                .requestNumber(REQUEST_NUMBER)
                .loanNumber(LOAN_NUMBER)
                .requestType(REVIEW_TYPE_LDC)
                .build();
    }

    private WorkflowState createWorkflowState() {
        return WorkflowState.builder()
                .requestNumber(REQUEST_NUMBER)
                .taskNumber(TASK_NUMBER)
                .currentTaskToken(TASK_TOKEN)
                .build();
    }

    @Nested
    @DisplayName("startWorkflow")
    class StartWorkflowTests {

        @Test
        @DisplayName("should start execution, persist state, and return response with task number")
        void startWorkflowSuccess() throws JsonProcessingException {
            when(objectMapper.writeValueAsString(any())).thenReturn(JSON_PAYLOAD);
            when(stepFunctionsConfig.getStateMachineArn()).thenReturn(STATE_MACHINE_ARN);
            when(stepFunctionsClient.startExecution(anyString(), anyString()))
                    .thenReturn(EXECUTION_ARN);

            StartWorkflowResponse response = workflowService.startWorkflow(createStartRequest(), "corr-001");

            assertThat(response.getTaskNumber()).startsWith("TSK-").hasSize(12);
            assertThat(response.getExecutionArn()).isEqualTo(EXECUTION_ARN);

            ArgumentCaptor<WorkflowState> captor = ArgumentCaptor.forClass(WorkflowState.class);
            verify(workflowRepository).save(captor.capture());
            WorkflowState saved = captor.getValue();
            assertThat(saved.getRequestNumber()).isEqualTo(REQUEST_NUMBER);
            assertThat(saved.getLoanNumber()).isEqualTo(LOAN_NUMBER);
            assertThat(saved.getReviewType()).isEqualTo(REVIEW_TYPE_LDC);
            assertThat(saved.getWorkflowStatus()).isEqualTo("INITIALIZED");
            assertThat(saved.getCorrelationId()).isEqualTo("corr-001");
            assertThat(saved.getExecutionArn()).isEqualTo(EXECUTION_ARN);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw WorkflowExecutionException when JSON serialization fails")
        void startWorkflowSerializationFailure() throws JsonProcessingException {
            when(objectMapper.writeValueAsString(any()))
                    .thenThrow(new JsonProcessingException("serialization error") {});

            assertThatThrownBy(() -> workflowService.startWorkflow(createStartRequest(), "corr-002"))
                    .isInstanceOf(WorkflowExecutionException.class)
                    .hasMessageContaining("Failed to serialize object to JSON");

            verify(workflowRepository, never()).save(any());
        }

        @Test
        @DisplayName("should propagate exception when Step Functions start fails")
        void startWorkflowStepFunctionsFailure() throws JsonProcessingException {
            when(objectMapper.writeValueAsString(any())).thenReturn(JSON_PAYLOAD);
            when(stepFunctionsConfig.getStateMachineArn()).thenReturn(STATE_MACHINE_ARN);
            when(stepFunctionsClient.startExecution(anyString(), anyString()))
                    .thenThrow(new WorkflowExecutionException("Failed to start workflow execution"));

            assertThatThrownBy(() -> workflowService.startWorkflow(createStartRequest(), "corr-003"))
                    .isInstanceOf(WorkflowExecutionException.class);

            verify(workflowRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("assignReviewType")
    class AssignReviewTypeTests {

        @Test
        @DisplayName("should find workflow and send task success with serialized request")
        void assignReviewTypeSuccess() throws JsonProcessingException {
            AssignTypeRequest request = AssignTypeRequest.builder()
                    .taskNumber(TASK_NUMBER)
                    .requestNumber(REQUEST_NUMBER)
                    .loanNumber(LOAN_NUMBER)
                    .reviewType(REVIEW_TYPE_LDC)
                    .build();
            when(workflowRepository.findByRequestAndTask(REQUEST_NUMBER, TASK_NUMBER))
                    .thenReturn(Optional.of(createWorkflowState()));
            String serializedOutput = "{\"reviewType\":\"LDC\"}";
            when(objectMapper.writeValueAsString(any())).thenReturn(serializedOutput);

            workflowService.assignReviewType(request, "corr-004");

            verify(stepFunctionsClient).sendTaskSuccess(eq(TASK_TOKEN), eq(serializedOutput));
        }

        @Test
        @DisplayName("should throw WorkflowNotFoundException when workflow does not exist")
        void assignReviewTypeNotFound() {
            AssignTypeRequest request = AssignTypeRequest.builder()
                    .taskNumber(MISSING_TASK_NUMBER)
                    .requestNumber(MISSING_REQUEST_NUMBER)
                    .loanNumber(LOAN_NUMBER)
                    .reviewType(REVIEW_TYPE_LDC)
                    .build();
            when(workflowRepository.findByRequestAndTask(MISSING_REQUEST_NUMBER, MISSING_TASK_NUMBER))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> workflowService.assignReviewType(request, "corr-005"))
                    .isInstanceOf(WorkflowNotFoundException.class)
                    .hasMessageContaining("Workflow not found");

            verify(stepFunctionsClient, never()).sendTaskSuccess(anyString(), anyString());
        }

        @Test
        @DisplayName("should throw WorkflowExecutionException when serialization fails")
        void assignReviewTypeSerializationFailure() throws JsonProcessingException {
            AssignTypeRequest request = AssignTypeRequest.builder()
                    .taskNumber(TASK_NUMBER)
                    .requestNumber(REQUEST_NUMBER)
                    .loanNumber(LOAN_NUMBER)
                    .reviewType(REVIEW_TYPE_LDC)
                    .build();
            when(workflowRepository.findByRequestAndTask(REQUEST_NUMBER, TASK_NUMBER))
                    .thenReturn(Optional.of(createWorkflowState()));
            when(objectMapper.writeValueAsString(any()))
                    .thenThrow(new JsonProcessingException("ser error") {});

            assertThatThrownBy(() -> workflowService.assignReviewType(request, "corr-006"))
                    .isInstanceOf(WorkflowExecutionException.class)
                    .hasMessageContaining("Failed to serialize");
        }
    }

    @Nested
    @DisplayName("submitDecision")
    class SubmitDecisionTests {

        @Test
        @DisplayName("should find workflow and send task success with serialized decision")
        void submitDecisionSuccess() throws JsonProcessingException {
            NextStepRequest request = NextStepRequest.builder()
                    .taskNumber(TASK_NUMBER)
                    .requestNumber(REQUEST_NUMBER)
                    .loanNumber(LOAN_NUMBER)
                    .loanDecision("APPROVED")
                    .attributes(List.of())
                    .build();
            when(workflowRepository.findByRequestAndTask(REQUEST_NUMBER, TASK_NUMBER))
                    .thenReturn(Optional.of(createWorkflowState()));
            String serializedOutput = "{\"decision\":\"APPROVED\"}";
            when(objectMapper.writeValueAsString(any())).thenReturn(serializedOutput);

            workflowService.submitDecision(request, "corr-007");

            verify(stepFunctionsClient).sendTaskSuccess(eq(TASK_TOKEN), eq(serializedOutput));
        }

        @Test
        @DisplayName("should throw WorkflowNotFoundException when workflow does not exist")
        void submitDecisionNotFound() {
            NextStepRequest request = NextStepRequest.builder()
                    .taskNumber(MISSING_TASK_NUMBER)
                    .requestNumber(MISSING_REQUEST_NUMBER)
                    .loanNumber(LOAN_NUMBER)
                    .loanDecision("REJECTED")
                    .attributes(List.of())
                    .build();
            when(workflowRepository.findByRequestAndTask(MISSING_REQUEST_NUMBER, MISSING_TASK_NUMBER))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> workflowService.submitDecision(request, "corr-008"))
                    .isInstanceOf(WorkflowNotFoundException.class)
                    .hasMessageContaining("Workflow not found");

            verify(stepFunctionsClient, never()).sendTaskSuccess(anyString(), anyString());
        }
    }
}
