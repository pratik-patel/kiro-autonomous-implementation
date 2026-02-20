package com.hex.service;

import com.hex.config.StepFunctionsConfig;
import com.hex.exception.WorkflowNotFoundException;
import com.hex.model.WorkflowState;
import com.hex.model.WorkflowStatus;
import com.hex.model.dto.AssignTypeRequest;
import com.hex.model.dto.NextStepRequest;
import com.hex.model.dto.StartWorkflowRequest;
import com.hex.model.dto.StartWorkflowResponse;
import com.hex.repository.WorkflowRepository;
import com.hex.util.CorrelationIdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Core business logic for workflow orchestration.
 * Coordinates between the API layer, Step Functions, and DynamoDB.
 */
@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowRepository workflowRepository;
    private final StepFunctionsClientWrapper stepFunctionsClient;
    private final StepFunctionsConfig stepFunctionsConfig;
    private final ObjectMapper objectMapper;

    public WorkflowService(WorkflowRepository workflowRepository,
                           StepFunctionsClientWrapper stepFunctionsClient,
                           StepFunctionsConfig stepFunctionsConfig,
                           ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.stepFunctionsClient = stepFunctionsClient;
        this.stepFunctionsConfig = stepFunctionsConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Starts a new workflow execution.
     * Generates a Task_Number, starts a Step Functions execution, and returns the response.
     *
     * @param request the start workflow request
     * @param correlationId the correlation ID for tracing
     * @return the start workflow response with task number and execution ARN
     */
    public StartWorkflowResponse startWorkflow(StartWorkflowRequest request, String correlationId) {
        String taskNumber = generateTaskNumber();
        log.info("Starting workflow: requestNumber={}, taskNumber={}, correlationId={}",
                request.getRequestNumber(), taskNumber, correlationId);

        WorkflowState initialState = WorkflowState.builder()
                .requestNumber(request.getRequestNumber())
                .taskNumber(taskNumber)
                .loanNumber(request.getLoanNumber())
                .reviewType(request.getRequestType())
                .workflowStatus(WorkflowStatus.INITIALIZED.name())
                .correlationId(correlationId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        String input = serializeToJson(initialState);
        String executionArn = stepFunctionsClient.startExecution(
                stepFunctionsConfig.getStateMachineArn(), input);

        initialState.setExecutionArn(executionArn);
        workflowRepository.save(initialState);

        log.info("Workflow started: taskNumber={}, executionArn={}", taskNumber, executionArn);
        return StartWorkflowResponse.builder()
                .taskNumber(taskNumber)
                .executionArn(executionArn)
                .build();
    }

    /**
     * Assigns a review type to an existing workflow by sending task success to Step Functions.
     *
     * @param request the assign type request
     * @param correlationId the correlation ID for tracing
     */
    public void assignReviewType(AssignTypeRequest request, String correlationId) {
        log.info("Assigning review type: taskNumber={}, reviewType={}, correlationId={}",
                request.getTaskNumber(), request.getReviewType(), correlationId);

        WorkflowState state = findWorkflowOrThrow(request.getRequestNumber(), request.getTaskNumber());
        String output = serializeToJson(request);
        stepFunctionsClient.sendTaskSuccess(state.getCurrentTaskToken(), output);

        log.info("Review type assigned: taskNumber={}", request.getTaskNumber());
    }

    /**
     * Submits a decision for an existing workflow by sending task success to Step Functions.
     *
     * @param request the next step request
     * @param correlationId the correlation ID for tracing
     */
    public void submitDecision(NextStepRequest request, String correlationId) {
        log.info("Submitting decision: taskNumber={}, decision={}, correlationId={}",
                request.getTaskNumber(), request.getLoanDecision(), correlationId);

        WorkflowState state = findWorkflowOrThrow(request.getRequestNumber(), request.getTaskNumber());
        String output = serializeToJson(request);
        stepFunctionsClient.sendTaskSuccess(state.getCurrentTaskToken(), output);

        log.info("Decision submitted: taskNumber={}", request.getTaskNumber());
    }

    private WorkflowState findWorkflowOrThrow(String requestNumber, String taskNumber) {
        return workflowRepository.findByRequestAndTask(requestNumber, taskNumber)
                .orElseThrow(() -> new WorkflowNotFoundException(
                        "Workflow not found: requestNumber=" + requestNumber + ", taskNumber=" + taskNumber));
    }

    private String generateTaskNumber() {
        return "TSK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new com.hex.exception.WorkflowExecutionException("Failed to serialize object to JSON", e);
        }
    }
}
