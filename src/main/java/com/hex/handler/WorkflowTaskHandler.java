package com.hex.handler;

import com.hex.model.LoanAttribute;
import com.hex.model.WorkflowState;
import com.hex.model.WorkflowStatus;
import com.hex.repository.WorkflowRepository;
import com.hex.service.ExternalSystemService;
import com.hex.service.StatusDeterminationService;
import com.hex.service.WorkflowRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Lambda handler invoked by Step Functions for internal workflow tasks.
 * Routes to the appropriate action based on the "action" field in the input map.
 */
@Component
public class WorkflowTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTaskHandler.class);
    private static final String ACTION_KEY = "action";
    private static final String REQUEST_NUMBER_KEY = "requestNumber";
    private static final String TASK_NUMBER_KEY = "taskNumber";
    private static final String STATUS_KEY = "status";
    private static final String WORKFLOW_NOT_FOUND_PREFIX = "Workflow not found: ";

    private final WorkflowRepository workflowRepository;
    private final StatusDeterminationService statusDeterminationService;
    private final ExternalSystemService externalSystemService;
    private final WorkflowRoutingService workflowRoutingService;

    public WorkflowTaskHandler(WorkflowRepository workflowRepository,
                               StatusDeterminationService statusDeterminationService,
                               ExternalSystemService externalSystemService,
                               WorkflowRoutingService workflowRoutingService) {
        this.workflowRepository = workflowRepository;
        this.statusDeterminationService = statusDeterminationService;
        this.externalSystemService = externalSystemService;
        this.workflowRoutingService = workflowRoutingService;
    }

    /**
     * Handles a Step Functions task invocation.
     * Dispatches to the appropriate handler method based on the "action" field.
     *
     * @param input the input map from Step Functions containing action and workflow data
     * @return a result map to pass to the next state
     */
    public Map<String, Object> handleTask(Map<String, Object> input) {
        String action = (String) input.get(ACTION_KEY);
        String requestNumber = (String) input.get(REQUEST_NUMBER_KEY);
        String taskNumber = (String) input.get(TASK_NUMBER_KEY);
        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }
        log.info("Handling task: action={}, requestNumber={}, taskNumber={}", action, requestNumber, taskNumber);

        return switch (action) {
            case "PERSIST_INITIAL_STATE" -> handlePersistInitialState(input);
            case "PERSIST_REVIEW_TYPE" -> handlePersistReviewType(input);
            case "CHECK_PENDING" -> handleCheckPending(requestNumber, taskNumber);
            case "DETERMINE_STATUS" -> handleDetermineStatus(requestNumber, taskNumber);
            case "UPDATE_EXTERNAL" -> handleUpdateExternal(requestNumber, taskNumber);
            case "LOG_AUDIT" -> handleLogAudit(requestNumber, taskNumber);
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        };
    }

    private Map<String, Object> handlePersistInitialState(Map<String, Object> input) {
        String requestNumber = (String) input.get(REQUEST_NUMBER_KEY);
        String taskNumber = (String) input.get(TASK_NUMBER_KEY);
        workflowRepository.updateStatus(requestNumber, taskNumber, WorkflowStatus.INITIALIZED.name());
        log.info("Persisted initial state: requestNumber={}, taskNumber={}", requestNumber, taskNumber);
        return Map.of(STATUS_KEY, WorkflowStatus.INITIALIZED.name());
    }

    private Map<String, Object> handlePersistReviewType(Map<String, Object> input) {
        String requestNumber = (String) input.get(REQUEST_NUMBER_KEY);
        String taskNumber = (String) input.get(TASK_NUMBER_KEY);
        workflowRepository.updateStatus(requestNumber, taskNumber, WorkflowStatus.REVIEW_TYPE_ASSIGNED.name());
        log.info("Persisted review type: requestNumber={}, taskNumber={}", requestNumber, taskNumber);
        return Map.of(STATUS_KEY, WorkflowStatus.REVIEW_TYPE_ASSIGNED.name());
    }

    private Map<String, Object> handleCheckPending(String requestNumber, String taskNumber) {
        WorkflowState state = workflowRepository.findByRequestAndTask(requestNumber, taskNumber)
                .orElseThrow(() -> new IllegalStateException(WORKFLOW_NOT_FOUND_PREFIX + requestNumber));
        List<LoanAttribute> attributes = state.getAttributes();
        boolean hasPending = statusDeterminationService.hasPendingAttributes(attributes);
        log.info("Checked pending: requestNumber={}, hasPending={}", requestNumber, hasPending);
        return Map.of("hasPendingAttributes", hasPending);
    }

    private Map<String, Object> handleDetermineStatus(String requestNumber, String taskNumber) {
        WorkflowState state = workflowRepository.findByRequestAndTask(requestNumber, taskNumber)
                .orElseThrow(() -> new IllegalStateException(WORKFLOW_NOT_FOUND_PREFIX + requestNumber));
        var decision = statusDeterminationService.determineStatus(state.getAttributes());
        boolean requiresConfirmation = workflowRoutingService.requiresReclassConfirmation(decision);
        workflowRepository.updateStatus(requestNumber, taskNumber, WorkflowStatus.DETERMINED.name());
        log.info("Determined status: requestNumber={}, decision={}, requiresConfirmation={}",
                requestNumber, decision, requiresConfirmation);
        return Map.of(
                "decision", decision.name(),
                "requiresReclassConfirmation", requiresConfirmation
        );
    }

    private Map<String, Object> handleUpdateExternal(String requestNumber, String taskNumber) {
        WorkflowState state = workflowRepository.findByRequestAndTask(requestNumber, taskNumber)
                .orElseThrow(() -> new IllegalStateException(WORKFLOW_NOT_FOUND_PREFIX + requestNumber));
        externalSystemService.updateExternalSystems(state);
        workflowRepository.updateStatus(requestNumber, taskNumber, WorkflowStatus.COMPLETED.name());
        log.info("External systems updated: requestNumber={}", requestNumber);
        return Map.of(STATUS_KEY, WorkflowStatus.COMPLETED.name());
    }

    private Map<String, Object> handleLogAudit(String requestNumber, String taskNumber) {
        workflowRepository.updateStatus(requestNumber, taskNumber, WorkflowStatus.COMPLETED.name());
        log.info("Audit trail logged: requestNumber={}, taskNumber={}", requestNumber, taskNumber);
        return Map.of(STATUS_KEY, WorkflowStatus.COMPLETED.name(), "auditLogged", true);
    }
}
