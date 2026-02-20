package com.hex.service;

import com.hex.exception.WorkflowValidationException;
import com.hex.model.ReviewType;
import com.hex.model.dto.AssignTypeRequest;
import com.hex.model.dto.NextStepRequest;
import com.hex.model.dto.StartWorkflowRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates all API inputs at the trust boundary.
 * Checks mandatory fields, identifier formats, request types, and payload sizes.
 */
@Service
public class InputValidator {

    private static final Logger log = LoggerFactory.getLogger(InputValidator.class);

    private static final String FIELD_REQUEST_NUMBER = "requestNumber";
    private static final String FIELD_LOAN_NUMBER = "loanNumber";

    private final Pattern requestNumberPattern;
    private final Pattern loanNumberPattern;
    private final Pattern taskNumberPattern;
    private final int maxPayloadSize;

    public InputValidator(
            @Value("${workflow.validation.request-number-pattern}") String requestNumberRegex,
            @Value("${workflow.validation.loan-number-pattern}") String loanNumberRegex,
            @Value("${workflow.validation.task-number-pattern}") String taskNumberRegex,
            @Value("${workflow.validation.max-payload-size}") int maxPayloadSize) {
        this.requestNumberPattern = Pattern.compile(requestNumberRegex);
        this.loanNumberPattern = Pattern.compile(loanNumberRegex);
        this.taskNumberPattern = Pattern.compile(taskNumberRegex);
        this.maxPayloadSize = maxPayloadSize;
    }

    /**
     * Validates a startPPAreview request payload.
     *
     * @param request the start workflow request
     * @throws WorkflowValidationException if validation fails
     */
    public void validateStartWorkflowRequest(StartWorkflowRequest request) {
        List<String> missingFields = new ArrayList<>();
        if (isBlank(request.getRequestNumber())) {
            missingFields.add(FIELD_REQUEST_NUMBER);
        }
        if (isBlank(request.getLoanNumber())) {
            missingFields.add(FIELD_LOAN_NUMBER);
        }
        if (isBlank(request.getRequestType())) {
            missingFields.add("requestType");
        }
        if (!missingFields.isEmpty()) {
            throw new WorkflowValidationException(
                    "Missing mandatory fields: " + String.join(", ", missingFields),
                    "VALIDATION_MISSING_FIELD");
        }
        validateRequestNumber(request.getRequestNumber());
        validateLoanNumber(request.getLoanNumber());
        validateRequestType(request.getRequestType());
    }

    /**
     * Validates an assignToType request payload.
     *
     * @param request the assign type request
     * @throws WorkflowValidationException if validation fails
     */
    public void validateAssignTypeRequest(AssignTypeRequest request) {
        List<String> missingFields = new ArrayList<>();
        if (isBlank(request.getTaskNumber())) {
            missingFields.add("taskNumber");
        }
        if (isBlank(request.getRequestNumber())) {
            missingFields.add(FIELD_REQUEST_NUMBER);
        }
        if (isBlank(request.getLoanNumber())) {
            missingFields.add(FIELD_LOAN_NUMBER);
        }
        if (isBlank(request.getReviewType())) {
            missingFields.add("reviewType");
        }
        if (!missingFields.isEmpty()) {
            throw new WorkflowValidationException(
                    "Missing mandatory fields: " + String.join(", ", missingFields),
                    "VALIDATION_MISSING_FIELD");
        }
        validateTaskNumber(request.getTaskNumber());
        validateRequestNumber(request.getRequestNumber());
        validateLoanNumber(request.getLoanNumber());
        validateRequestType(request.getReviewType());
    }

    /**
     * Validates a getNextStep request payload.
     *
     * @param request the next step request
     * @throws WorkflowValidationException if validation fails
     */
    public void validateNextStepRequest(NextStepRequest request) {
        List<String> missingFields = new ArrayList<>();
        if (isBlank(request.getTaskNumber())) {
            missingFields.add("taskNumber");
        }
        if (isBlank(request.getRequestNumber())) {
            missingFields.add(FIELD_REQUEST_NUMBER);
        }
        if (isBlank(request.getLoanNumber())) {
            missingFields.add(FIELD_LOAN_NUMBER);
        }
        if (isBlank(request.getLoanDecision())) {
            missingFields.add("loanDecision");
        }
        if (request.getAttributes() == null || request.getAttributes().isEmpty()) {
            missingFields.add("attributes");
        }
        if (!missingFields.isEmpty()) {
            throw new WorkflowValidationException(
                    "Missing mandatory fields: " + String.join(", ", missingFields),
                    "VALIDATION_MISSING_FIELD");
        }
        validateTaskNumber(request.getTaskNumber());
        validateRequestNumber(request.getRequestNumber());
        validateLoanNumber(request.getLoanNumber());
    }

    /**
     * Validates payload size does not exceed the configured maximum.
     *
     * @param payloadSize the size of the payload in bytes
     * @throws WorkflowValidationException if payload exceeds maximum
     */
    public void validatePayloadSize(int payloadSize) {
        if (payloadSize > maxPayloadSize) {
            throw new WorkflowValidationException(
                    "Payload size " + payloadSize + " exceeds maximum " + maxPayloadSize,
                    "PAYLOAD_TOO_LARGE");
        }
    }

    /**
     * Validates a request number matches the expected format.
     *
     * @param requestNumber the request number to validate
     * @throws WorkflowValidationException if format is invalid
     */
    public void validateRequestNumber(String requestNumber) {
        if (!requestNumberPattern.matcher(requestNumber).matches()) {
            log.warn("Invalid request number format: {}", requestNumber);
            throw new WorkflowValidationException(
                    "Invalid request number format: " + requestNumber,
                    "VALIDATION_INVALID_FORMAT");
        }
    }

    /**
     * Validates a loan number matches the expected format.
     *
     * @param loanNumber the loan number to validate
     * @throws WorkflowValidationException if format is invalid
     */
    public void validateLoanNumber(String loanNumber) {
        if (!loanNumberPattern.matcher(loanNumber).matches()) {
            log.warn("Invalid loan number format: {}", loanNumber);
            throw new WorkflowValidationException(
                    "Invalid loan number format: " + loanNumber,
                    "VALIDATION_INVALID_FORMAT");
        }
    }

    /**
     * Validates a task number matches the expected format.
     *
     * @param taskNumber the task number to validate
     * @throws WorkflowValidationException if format is invalid
     */
    public void validateTaskNumber(String taskNumber) {
        if (!taskNumberPattern.matcher(taskNumber).matches()) {
            log.warn("Invalid task number format: {}", taskNumber);
            throw new WorkflowValidationException(
                    "Invalid task number format: " + taskNumber,
                    "VALIDATION_INVALID_FORMAT");
        }
    }

    /**
     * Validates a request/review type is one of the allowed values.
     *
     * @param requestType the request type string to validate
     * @throws WorkflowValidationException if the type is not allowed
     */
    public void validateRequestType(String requestType) {
        try {
            ReviewType.fromString(requestType);
        } catch (IllegalArgumentException e) {
            throw new WorkflowValidationException(
                    e.getMessage(),
                    "VALIDATION_INVALID_REQUEST_TYPE");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
