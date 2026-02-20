package com.hex.service;

import com.hex.exception.WorkflowExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.SendTaskFailureRequest;
import software.amazon.awssdk.services.sfn.model.SendTaskFailureResponse;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessRequest;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessResponse;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StepFunctionsClientWrapper unit tests")
class StepFunctionsClientWrapperTest {

    private static final String STATE_MACHINE_ARN = "arn:aws:states:us-east-1:000:stateMachine:test";
    private static final String EXECUTION_ARN = "arn:aws:states:us-east-1:000:execution:test:exec-1";
    private static final String TASK_TOKEN = "test-task-token-abcdef123456";
    private static final String JSON_OUTPUT = "{\"status\":\"COMPLETED\"}";
    private static final String AWS_ERROR_MESSAGE = "AWS error";

    @Mock
    private SfnClient sfnClient;

    private StepFunctionsClientWrapper wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new StepFunctionsClientWrapper(sfnClient);
    }

    @Test
    @DisplayName("startExecution returns execution ARN on success")
    void startExecutionSuccess() {
        when(sfnClient.startExecution(any(StartExecutionRequest.class)))
                .thenReturn(StartExecutionResponse.builder().executionArn(EXECUTION_ARN).build());

        String result = wrapper.startExecution(STATE_MACHINE_ARN, "{}");

        assertThat(result).isEqualTo(EXECUTION_ARN);
        verify(sfnClient).startExecution(any(StartExecutionRequest.class));
    }

    @Test
    @DisplayName("startExecution throws WorkflowExecutionException on failure")
    void startExecutionFailure() {
        when(sfnClient.startExecution(any(StartExecutionRequest.class)))
                .thenThrow(new RuntimeException(AWS_ERROR_MESSAGE));

        assertThatThrownBy(() -> wrapper.startExecution(STATE_MACHINE_ARN, "{}"))
                .isInstanceOf(WorkflowExecutionException.class)
                .hasMessageContaining("Failed to start workflow execution");
    }

    @Test
    @DisplayName("sendTaskSuccess completes without error")
    void sendTaskSuccessOk() {
        when(sfnClient.sendTaskSuccess(any(SendTaskSuccessRequest.class)))
                .thenReturn(SendTaskSuccessResponse.builder().build());

        wrapper.sendTaskSuccess(TASK_TOKEN, JSON_OUTPUT);

        verify(sfnClient).sendTaskSuccess(any(SendTaskSuccessRequest.class));
    }

    @Test
    @DisplayName("sendTaskSuccess throws WorkflowExecutionException on failure")
    void sendTaskSuccessFailure() {
        when(sfnClient.sendTaskSuccess(any(SendTaskSuccessRequest.class)))
                .thenThrow(new RuntimeException(AWS_ERROR_MESSAGE));

        assertThatThrownBy(() -> wrapper.sendTaskSuccess(TASK_TOKEN, JSON_OUTPUT))
                .isInstanceOf(WorkflowExecutionException.class)
                .hasMessageContaining("Failed to send task success");
    }

    @Test
    @DisplayName("sendTaskFailure completes without error")
    void sendTaskFailureOk() {
        when(sfnClient.sendTaskFailure(any(SendTaskFailureRequest.class)))
                .thenReturn(SendTaskFailureResponse.builder().build());

        wrapper.sendTaskFailure(TASK_TOKEN, "ERROR_CODE", "Something went wrong");

        verify(sfnClient).sendTaskFailure(any(SendTaskFailureRequest.class));
    }

    @Test
    @DisplayName("sendTaskFailure throws WorkflowExecutionException on failure")
    void sendTaskFailureError() {
        when(sfnClient.sendTaskFailure(any(SendTaskFailureRequest.class)))
                .thenThrow(new RuntimeException(AWS_ERROR_MESSAGE));

        assertThatThrownBy(() -> wrapper.sendTaskFailure(TASK_TOKEN, "ERROR_CODE", "cause"))
                .isInstanceOf(WorkflowExecutionException.class)
                .hasMessageContaining("Failed to send task failure");
    }
}
