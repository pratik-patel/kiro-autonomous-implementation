package com.hex.service;

import com.hex.exception.WorkflowExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.SendTaskFailureRequest;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

/**
 * Wraps AWS SDK Step Functions client with error handling and logging.
 */
@Service
public class StepFunctionsClientWrapper {

    private static final Logger log = LoggerFactory.getLogger(StepFunctionsClientWrapper.class);

    private final SfnClient sfnClient;

    public StepFunctionsClientWrapper(SfnClient sfnClient) {
        this.sfnClient = sfnClient;
    }

    /**
     * Starts a new Step Functions execution.
     *
     * @param stateMachineArn the ARN of the state machine
     * @param input the JSON input for the execution
     * @return the execution ARN
     * @throws WorkflowExecutionException if the execution fails to start
     */
    public String startExecution(String stateMachineArn, String input) {
        try {
            StartExecutionResponse response = sfnClient.startExecution(
                    StartExecutionRequest.builder()
                            .stateMachineArn(stateMachineArn)
                            .input(input)
                            .build());
            log.info("Started Step Functions execution: {}", response.executionArn());
            return response.executionArn();
        } catch (Exception e) {
            log.error("Failed to start Step Functions execution", e);
            throw new WorkflowExecutionException("Failed to start workflow execution", e);
        }
    }

    /**
     * Sends a task success to resume a paused execution.
     *
     * @param taskToken the callback task token
     * @param output the JSON output for the task
     * @throws WorkflowExecutionException if sending task success fails
     */
    public void sendTaskSuccess(String taskToken, String output) {
        try {
            sfnClient.sendTaskSuccess(
                    SendTaskSuccessRequest.builder()
                            .taskToken(taskToken)
                            .output(output)
                            .build());
            log.info("Sent task success for token: {}...", taskToken.substring(0, Math.min(20, taskToken.length())));
        } catch (Exception e) {
            log.error("Failed to send task success", e);
            throw new WorkflowExecutionException("Failed to send task success", e);
        }
    }

    /**
     * Sends a task failure to fail a paused execution.
     *
     * @param taskToken the callback task token
     * @param error the error code
     * @param cause the error cause description
     * @throws WorkflowExecutionException if sending task failure fails
     */
    public void sendTaskFailure(String taskToken, String error, String cause) {
        try {
            sfnClient.sendTaskFailure(
                    SendTaskFailureRequest.builder()
                            .taskToken(taskToken)
                            .error(error)
                            .cause(cause)
                            .build());
            log.warn("Sent task failure for token: {}..., error: {}", 
                    taskToken.substring(0, Math.min(20, taskToken.length())), error);
        } catch (Exception e) {
            log.error("Failed to send task failure", e);
            throw new WorkflowExecutionException("Failed to send task failure", e);
        }
    }
}
