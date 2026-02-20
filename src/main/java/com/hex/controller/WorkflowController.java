package com.hex.controller;

import com.hex.model.dto.ApiResponse;
import com.hex.model.dto.AssignTypeRequest;
import com.hex.model.dto.NextStepRequest;
import com.hex.model.dto.StartWorkflowRequest;
import com.hex.model.dto.StartWorkflowResponse;
import com.hex.service.InputValidator;
import com.hex.service.WorkflowService;
import com.hex.util.CorrelationIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the three LDC workflow endpoints.
 * Delegates validation to InputValidator and business logic to WorkflowService.
 */
@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final InputValidator inputValidator;
    private final WorkflowService workflowService;

    public WorkflowController(InputValidator inputValidator, WorkflowService workflowService) {
        this.inputValidator = inputValidator;
        this.workflowService = workflowService;
    }

    /**
     * Initiates a new LDC loan review workflow.
     *
     * @param request the start workflow request payload
     * @return standardized response with task number and execution ARN
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<StartWorkflowResponse>> startWorkflow(
            @RequestBody StartWorkflowRequest request) {
        String correlationId = CorrelationIdGenerator.generate();
        log.info("POST /api/v1/workflow/start — correlationId={}", correlationId);

        inputValidator.validateStartWorkflowRequest(request);
        StartWorkflowResponse result = workflowService.startWorkflow(request, correlationId);

        ApiResponse<StartWorkflowResponse> response = ApiResponse.<StartWorkflowResponse>builder()
                .success(true)
                .message("Workflow started")
                .correlationId(correlationId)
                .data(result)
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Assigns a review type to an existing workflow, resuming the Step Functions execution.
     *
     * @param request the assign type request payload
     * @return standardized success response
     */
    @PostMapping("/assign-type")
    public ResponseEntity<ApiResponse<Void>> assignType(@RequestBody AssignTypeRequest request) {
        String correlationId = CorrelationIdGenerator.generate();
        log.info("POST /api/v1/workflow/assign-type — correlationId={}", correlationId);

        inputValidator.validateAssignTypeRequest(request);
        workflowService.assignReviewType(request, correlationId);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Review type assigned")
                .correlationId(correlationId)
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Submits a loan decision for an existing workflow, resuming the Step Functions execution.
     *
     * @param request the next step request payload
     * @return standardized success response
     */
    @PostMapping("/next-step")
    public ResponseEntity<ApiResponse<Void>> nextStep(@RequestBody NextStepRequest request) {
        String correlationId = CorrelationIdGenerator.generate();
        log.info("POST /api/v1/workflow/next-step — correlationId={}", correlationId);

        inputValidator.validateNextStepRequest(request);
        workflowService.submitDecision(request, correlationId);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Decision submitted")
                .correlationId(correlationId)
                .build();
        return ResponseEntity.ok(response);
    }
}
