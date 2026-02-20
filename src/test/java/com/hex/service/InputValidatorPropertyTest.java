package com.hex.service;

import com.hex.exception.WorkflowValidationException;
import com.hex.model.dto.AssignTypeRequest;
import com.hex.model.dto.LoanAttributeDto;
import com.hex.model.dto.NextStepRequest;
import com.hex.model.dto.StartWorkflowRequest;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for InputValidator.
 * Feature: ldc-loan-review-workflow
 */
class InputValidatorPropertyTest {

    private static final String REQ_PATTERN = "^REQ-\\d{6,10}$";
    private static final String LOAN_PATTERN = "^LN-\\d{6,12}$";
    private static final String TASK_PATTERN = "^TSK-\\d{6,10}$";
    private static final int MAX_PAYLOAD = 1048576;

    private final InputValidator validator = new InputValidator(
            REQ_PATTERN, LOAN_PATTERN, TASK_PATTERN, MAX_PAYLOAD);

    // ========================================================================
    // Property 1: Mandatory field validation rejects incomplete payloads
    // Validates: Requirements 1.2, 1.3, 2.2, 2.3, 3.2, 3.3
    // ========================================================================

    @Property(tries = 100)
    @Label("Property 1a: StartWorkflow with any null mandatory field is rejected")
    void startWorkflowRejectsMissingFields(
            @ForAll("nullableRequestNumber") String requestNumber,
            @ForAll("nullableLoanNumber") String loanNumber,
            @ForAll("nullableRequestType") String requestType) {

        // Skip the case where all fields are valid
        if (requestNumber != null && loanNumber != null && requestType != null) {
            return;
        }

        StartWorkflowRequest request = StartWorkflowRequest.builder()
                .requestNumber(requestNumber)
                .loanNumber(loanNumber)
                .requestType(requestType)
                .build();

        assertThatThrownBy(() -> validator.validateStartWorkflowRequest(request))
                .isInstanceOf(WorkflowValidationException.class);
    }

    @Property(tries = 100)
    @Label("Property 1b: AssignType with any null mandatory field is rejected")
    void assignTypeRejectsMissingFields(
            @ForAll("nullableTaskNumber") String taskNumber,
            @ForAll("nullableRequestNumber") String requestNumber,
            @ForAll("nullableLoanNumber") String loanNumber,
            @ForAll("nullableRequestType") String reviewType) {

        if (taskNumber != null && requestNumber != null && loanNumber != null && reviewType != null) {
            return;
        }

        AssignTypeRequest request = AssignTypeRequest.builder()
                .taskNumber(taskNumber)
                .requestNumber(requestNumber)
                .loanNumber(loanNumber)
                .reviewType(reviewType)
                .build();

        assertThatThrownBy(() -> validator.validateAssignTypeRequest(request))
                .isInstanceOf(WorkflowValidationException.class);
    }

    @Property(tries = 100)
    @Label("Property 1c: NextStep with any null mandatory field is rejected")
    void nextStepRejectsMissingFields(
            @ForAll("nullableTaskNumber") String taskNumber,
            @ForAll("nullableRequestNumber") String requestNumber,
            @ForAll("nullableLoanNumber") String loanNumber,
            @ForAll("nullableDecision") String loanDecision) {

        if (taskNumber != null && requestNumber != null && loanNumber != null && loanDecision != null) {
            return;
        }

        NextStepRequest request = NextStepRequest.builder()
                .taskNumber(taskNumber)
                .requestNumber(requestNumber)
                .loanNumber(loanNumber)
                .loanDecision(loanDecision)
                .attributes(List.of(LoanAttributeDto.builder()
                        .attributeName("attr1").attributeStatus("Approved").build()))
                .build();

        assertThatThrownBy(() -> validator.validateNextStepRequest(request))
                .isInstanceOf(WorkflowValidationException.class);
    }

    @Property(tries = 100)
    @Label("Property 1d: StartWorkflow with all valid mandatory fields is accepted")
    void startWorkflowAcceptsValidFields(
            @ForAll("validRequestNumber") String requestNumber,
            @ForAll("validLoanNumber") String loanNumber,
            @ForAll("validRequestType") String requestType) {

        StartWorkflowRequest request = StartWorkflowRequest.builder()
                .requestNumber(requestNumber)
                .loanNumber(loanNumber)
                .requestType(requestType)
                .build();

        assertThatNoException().isThrownBy(() -> validator.validateStartWorkflowRequest(request));
    }

    // ========================================================================
    // Property 2: Identifier format validation
    // Validates: Requirements 9.1
    // ========================================================================

    @Property(tries = 100)
    @Label("Property 2a: Invalid request numbers are rejected")
    void invalidRequestNumbersRejected(
            @ForAll("invalidRequestNumber") String requestNumber) {

        assertThatThrownBy(() -> validator.validateRequestNumber(requestNumber))
                .isInstanceOf(WorkflowValidationException.class)
                .extracting("errorCode")
                .isEqualTo("VALIDATION_INVALID_FORMAT");
    }

    @Property(tries = 100)
    @Label("Property 2b: Valid request numbers are accepted")
    void validRequestNumbersAccepted(
            @ForAll("validRequestNumber") String requestNumber) {

        assertThatNoException().isThrownBy(() -> validator.validateRequestNumber(requestNumber));
    }

    @Property(tries = 100)
    @Label("Property 2c: Invalid loan numbers are rejected")
    void invalidLoanNumbersRejected(
            @ForAll("invalidLoanNumber") String loanNumber) {

        assertThatThrownBy(() -> validator.validateLoanNumber(loanNumber))
                .isInstanceOf(WorkflowValidationException.class)
                .extracting("errorCode")
                .isEqualTo("VALIDATION_INVALID_FORMAT");
    }

    @Property(tries = 100)
    @Label("Property 2d: Valid loan numbers are accepted")
    void validLoanNumbersAccepted(
            @ForAll("validLoanNumber") String loanNumber) {

        assertThatNoException().isThrownBy(() -> validator.validateLoanNumber(loanNumber));
    }

    @Property(tries = 100)
    @Label("Property 2e: Invalid task numbers are rejected")
    void invalidTaskNumbersRejected(
            @ForAll("invalidTaskNumber") String taskNumber) {

        assertThatThrownBy(() -> validator.validateTaskNumber(taskNumber))
                .isInstanceOf(WorkflowValidationException.class)
                .extracting("errorCode")
                .isEqualTo("VALIDATION_INVALID_FORMAT");
    }

    @Property(tries = 100)
    @Label("Property 2f: Valid task numbers are accepted")
    void validTaskNumbersAccepted(
            @ForAll("validTaskNumber") String taskNumber) {

        assertThatNoException().isThrownBy(() -> validator.validateTaskNumber(taskNumber));
    }

    // ========================================================================
    // Property 3: Request type validation accepts only allowed values
    // Validates: Requirements 1.4, 1.5
    // ========================================================================

    @Property(tries = 100)
    @Label("Property 3a: Allowed request types are accepted")
    void allowedRequestTypesAccepted(
            @ForAll("validRequestType") String requestType) {

        assertThatNoException().isThrownBy(() -> validator.validateRequestType(requestType));
    }

    @Property(tries = 100)
    @Label("Property 3b: Disallowed request types are rejected")
    void disallowedRequestTypesRejected(
            @ForAll("invalidRequestType") String requestType) {

        assertThatThrownBy(() -> validator.validateRequestType(requestType))
                .isInstanceOf(WorkflowValidationException.class)
                .extracting("errorCode")
                .isEqualTo("VALIDATION_INVALID_REQUEST_TYPE");
    }

    // ========================================================================
    // Arbitraries (generators)
    // ========================================================================

    private static final Set<String> VALID_REQUEST_TYPES = Set.of("LDC", "Sec Policy", "Conduit");

    @Provide
    Arbitrary<String> validRequestNumber() {
        return Arbitraries.integers().between(100000, 9999999999L > Integer.MAX_VALUE ? Integer.MAX_VALUE : 999999999)
                .map(n -> "REQ-" + String.format("%06d", Math.abs(n) % 10000000));
    }

    @Provide
    Arbitrary<String> validLoanNumber() {
        return Arbitraries.integers().between(100000, 999999999)
                .map(n -> "LN-" + String.format("%06d", Math.abs(n) % 10000000));
    }

    @Provide
    Arbitrary<String> validTaskNumber() {
        return Arbitraries.integers().between(100000, 999999999)
                .map(n -> "TSK-" + String.format("%06d", Math.abs(n) % 10000000));
    }

    @Provide
    Arbitrary<String> invalidRequestNumber() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just("REQ-123"),
                Arbitraries.just("REQ-"),
                Arbitraries.just("INVALID-123456"),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                        .filter(s -> !s.matches("^REQ-\\d{6,10}$"))
        );
    }

    @Provide
    Arbitrary<String> invalidLoanNumber() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just("LN-123"),
                Arbitraries.just("LN-"),
                Arbitraries.just("LOAN-123456"),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                        .filter(s -> !s.matches("^LN-\\d{6,12}$"))
        );
    }

    @Provide
    Arbitrary<String> invalidTaskNumber() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just("TSK-123"),
                Arbitraries.just("TSK-"),
                Arbitraries.just("TASK-123456"),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                        .filter(s -> !s.matches("^TSK-\\d{6,10}$"))
        );
    }

    @Provide
    Arbitrary<String> validRequestType() {
        return Arbitraries.of("LDC", "Sec Policy", "Conduit");
    }

    @Provide
    Arbitrary<String> invalidRequestType() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30)
                .filter(s -> !VALID_REQUEST_TYPES.contains(s)
                        && !s.equalsIgnoreCase("LDC")
                        && !s.equalsIgnoreCase("SEC_POLICY")
                        && !s.equalsIgnoreCase("CONDUIT")
                        && !s.equalsIgnoreCase("Sec Policy"));
    }

    @Provide
    Arbitrary<String> nullableRequestNumber() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                validRequestNumber()
        );
    }

    @Provide
    Arbitrary<String> nullableLoanNumber() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                validLoanNumber()
        );
    }

    @Provide
    Arbitrary<String> nullableTaskNumber() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                validTaskNumber()
        );
    }

    @Provide
    Arbitrary<String> nullableRequestType() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                validRequestType()
        );
    }

    @Provide
    Arbitrary<String> nullableDecision() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.of("APPROVED", "REJECTED")
        );
    }
}
