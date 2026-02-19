# Implementation Plan: LDC Loan Review Workflow

## Overview

This plan implements the LDC Loan Review Workflow as a callback-driven AWS Step Functions state machine with Lambda handlers, DynamoDB persistence, and three API entry points. Tasks are ordered to build foundational layers first (models, persistence) then business logic, handlers, and finally the Step Functions ASL definition.

## Tasks

- [x] 1. Project setup and core domain models
  - [x] 1.1 Create Maven project structure with Spring Boot 3.x, AWS SDK, jqwik, and Lombok dependencies
    - Set up `pom.xml` with Java 17, Spring Boot 3.x starter, AWS Lambda, DynamoDB SDK, Step Functions SDK, jqwik, JUnit 5, Mockito, AssertJ, MapStruct, Lombok
    - Create package structure: `config`, `controller`, `service`, `model`, `repository`, `exception`, `util`
    - _Requirements: Project infrastructure_

  - [x] 1.2 Implement enums: `RequestType`, `AttributeStatus`, `WorkflowStatus`
    - `RequestType`: LDC, SEC_POLICY, CONDUIT (with display value mapping for "Sec Policy")
    - `AttributeStatus`: PENDING_REVIEW, APPROVED, REJECTED, REPURCHASE, RECLASS
    - `WorkflowStatus`: INITIALIZED, REVIEW_TYPE_ASSIGNED, DECISION_PENDING, APPROVED, REJECTED, PARTIALLY_APPROVED, REPURCHASE, RECLASS_APPROVED, WAITING_FOR_CONFIRMATION, UPDATING_EXTERNAL_SYSTEMS, COMPLETED, FAILED
    - _Requirements: 1.4, 4.1–4.5_

  - [x] 1.3 Implement domain models: `WorkflowState`, `LoanAttribute`, `AuditTrailEntry`, `ReclassConfirmation`
    - Use Lombok `@Data`, `@Builder` for all models
    - `WorkflowState` contains all fields from DynamoDB table design
    - `LoanAttribute` with attributeName, attributeStatus, updatedTimestamp
    - `AuditTrailEntry` with previousStatus, newStatus, timestamp, triggeringAction, correlationId
    - _Requirements: 10.1, 10.4, 8.2_

  - [x] 1.4 Implement API DTOs: `StartPPAReviewRequest`, `AssignToTypeRequest`, `GetNextStepRequest`, `LoanAttributeDto`, `WorkflowResponse`
    - Add Jakarta Bean Validation annotations (`@NotBlank`, `@NotNull`) on mandatory fields
    - `WorkflowResponse` includes: requestNumber, loanNumber, status, message, correlationId, taskNumber
    - _Requirements: 11.1, 11.4, 1.2, 2.3, 3.2_

  - [x] 1.5 Implement custom exception hierarchy: `WorkflowException`, `WorkflowValidationException`, `WorkflowNotFoundException`, `WorkflowPersistenceException`, `ExternalSystemException`
    - All extend `RuntimeException` (unchecked)
    - `WorkflowPersistenceException` includes operationType, tableName, errorReason fields
    - _Requirements: 9.3_

- [ ] 2. DynamoDB repository layer
  - [x] 2.1 Implement `WorkflowRepository` interface
    - Define methods: `save(WorkflowState)`, `findByRequestAndLoan(requestNumber, loanNumber)`, `findByTaskNumber(taskNumber)`, `updateStatus(requestNumber, loanNumber, status, auditEntry)`, `updateTaskToken(requestNumber, loanNumber, taskToken)`, `updateAttributes(requestNumber, loanNumber, attributes, decision)`
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 2.2 Implement `DynamoDbWorkflowRepository`
    - Use AWS DynamoDB Enhanced Client
    - Partition key: `requestNumber`, Sort key: `loanNumber`
    - GSI on `taskNumber` for callback lookups
    - Auto-set `lastModifiedTimestamp` on every write operation
    - Wrap SDK exceptions in `WorkflowPersistenceException` with operation context
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 9.3_

  - [ ]* 2.3 Write unit tests for `DynamoDbWorkflowRepository`
    - Test save and retrieve round-trip
    - Test findByTaskNumber via GSI
    - Test lastModifiedTimestamp auto-update
    - Test exception wrapping on DynamoDB failure
    - _Requirements: 10.1, 10.2, 10.3, 9.3_

- [ ] 3. Validation service
  - [x] 3.1 Implement `PayloadValidationService`
    - Validate `startPPAreview`: mandatory fields (requestNumber, loanNumber, requestType), valid RequestType enum
    - Validate `assignToType`: mandatory fields (taskNumber, requestNumber, loanNumber, reviewType)
    - Validate `getNextStep`: mandatory fields (taskNumber, requestNumber, loanNumber, loanDecision, attributes)
    - Return structured validation errors listing all missing/invalid fields
    - _Requirements: 1.2, 1.3, 1.4, 2.3, 2.4, 3.2, 3.3_

  - [ ]* 3.2 Write property test: startPPAreview payload validation
    - **Property 1: startPPAreview Payload Validation**
    - Generate payloads with random combinations of null/missing mandatory fields
    - Assert: every missing field is identified in the error response
    - **Validates: Requirements 1.2, 1.3**

  - [ ]* 3.3 Write property test: Request type validation
    - **Property 2: Request Type Validation**
    - Generate arbitrary strings excluding "LDC", "Sec Policy", "Conduit"
    - Assert: validation rejects with descriptive error
    - **Validates: Requirements 1.4**

  - [ ]* 3.4 Write property test: assignToType payload validation
    - **Property 3: assignToType Payload Validation**
    - Generate payloads with random combinations of null/missing mandatory fields
    - Assert: every missing field is identified in the error response
    - **Validates: Requirements 2.3, 2.4**

  - [ ]* 3.5 Write property test: getNextStep payload validation
    - **Property 4: getNextStep Payload Validation**
    - Generate payloads with random combinations of null/missing mandatory fields
    - Assert: every missing field is identified in the error response
    - **Validates: Requirements 3.2, 3.3**

- [ ] 4. Core business logic services
  - [ ] 4.1 Implement `PendingAttributeChecker`
    - Accept a list of `LoanAttribute`, return boolean indicating if any attribute has `PENDING_REVIEW` status
    - _Requirements: 3.4, 3.5, 3.6_

  - [ ]* 4.2 Write property test: Pending attribute detection
    - **Property 5: Pending Attribute Detection**
    - Generate random attribute lists with mixed statuses
    - Assert: result is true if and only if at least one attribute is PENDING_REVIEW
    - **Validates: Requirements 3.4**

  - [ ] 4.3 Implement `StatusDeterminationService`
    - Accept a list of `LoanAttribute` (no PENDING_REVIEW), return `WorkflowStatus`
    - Priority order: Reclass → Repurchase → All Approved → All Rejected → Partially Approved
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ]* 4.4 Write property test: Status determination from attributes
    - **Property 6: Status Determination from Attributes**
    - Generate non-pending attribute lists with random statuses
    - Assert: determined status matches the priority rules
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**

  - [ ] 4.5 Implement `StatusRoutingService`
    - Accept `WorkflowStatus`, return routing decision enum: `EXTERNAL_SYSTEMS` or `RECLASS_CONFIRMATION`
    - Approved, Rejected, Partially Approved, Repurchase → EXTERNAL_SYSTEMS
    - Reclass Approved → RECLASS_CONFIRMATION
    - _Requirements: 5.1, 5.2_

  - [ ]* 4.6 Write property test: Status routing correctness
    - **Property 7: Status Routing Correctness**
    - Generate all valid terminal statuses
    - Assert: routing matches expected destination
    - **Validates: Requirements 5.1, 5.2**

  - [ ] 4.7 Implement `AuditTrailService`
    - Record status transitions with previousStatus, newStatus, UTC timestamp, triggeringAction, correlationId
    - Append to the workflow state's audit trail list
    - _Requirements: 8.2_

  - [ ]* 4.8 Write property test: Audit trail completeness
    - **Property 8: Audit Trail Completeness**
    - Generate random sequences of status transitions
    - Assert: audit trail entry count equals transition count, all fields non-null
    - **Validates: Requirements 8.2**

- [ ] 5. Checkpoint — Core logic tests pass
  - Ensure all unit tests and property tests pass for validation, pending detection, status determination, routing, and audit trail. Ask the user if questions arise.

- [ ] 6. Workflow orchestration service
  - [ ] 6.1 Implement `WorkflowOrchestrationService`
    - `initializeWorkflow(StartPPAReviewRequest)`: validate, start Step Functions execution, persist initial state with status `Initialized`, return response
    - `assignReviewType(AssignToTypeRequest)`: validate, look up task token by taskNumber, update review type, send task success callback, return response
    - `processNextStep(GetNextStepRequest)`: validate, look up task token, persist decision/attributes, send task success callback, return response
    - Generate and attach correlationId (UUID) to every operation
    - _Requirements: 1.1, 1.5, 1.6, 2.1, 2.2, 2.5, 3.1, 6.2, 6.3_

  - [ ]* 6.2 Write unit tests for `WorkflowOrchestrationService`
    - Test initialization happy path: state persisted, execution started, status Initialized
    - Test assignReviewType: token retrieved, callback sent, status updated
    - Test processNextStep: decision persisted, callback sent
    - Test 404 when taskNumber not found
    - _Requirements: 1.1, 2.1, 3.1, 9.4_

  - [ ]* 6.3 Write property test: Invalid task number rejection
    - **Property 12: Invalid Task Number Rejection**
    - Generate random non-existent task numbers
    - Assert: API returns 404 Not Found
    - **Validates: Requirements 9.4**

- [ ] 7. Lambda handlers (API layer)
  - [ ] 7.1 Implement `StartPPAReviewHandler` Lambda
    - Parse and validate request, delegate to `WorkflowOrchestrationService.initializeWorkflow`
    - Return `WorkflowResponse` with correlationId
    - Handle exceptions: `WorkflowValidationException` → 400, unexpected → 500 with correlation ID
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 11.1, 11.2, 11.3, 11.4_

  - [ ] 7.2 Implement `AssignToTypeHandler` Lambda
    - Parse and validate request, delegate to `WorkflowOrchestrationService.assignReviewType`
    - Return `WorkflowResponse` with correlationId
    - Handle exceptions: `WorkflowValidationException` → 400, `WorkflowNotFoundException` → 404, unexpected → 500
    - _Requirements: 2.1, 2.3, 2.4, 9.4, 11.1, 11.2, 11.3, 11.4_

  - [ ] 7.3 Implement `GetNextStepHandler` Lambda
    - Parse and validate request, delegate to `WorkflowOrchestrationService.processNextStep`
    - Return `WorkflowResponse` with correlationId
    - Handle exceptions: `WorkflowValidationException` → 400, `WorkflowNotFoundException` → 404, unexpected → 500
    - _Requirements: 3.1, 3.2, 3.3, 9.4, 11.1, 11.2, 11.3, 11.4_

  - [ ] 7.4 Implement global exception handler / error response builder
    - Map exception types to HTTP status codes
    - Build `WorkflowResponse` with generic message for 500 errors (no stack traces, no internal details)
    - Always include correlationId
    - _Requirements: 11.3, 11.4_

  - [ ]* 7.5 Write property test: API response structure compliance
    - **Property 10: API Response Structure Compliance**
    - Generate random valid and invalid API requests across all 3 endpoints
    - Assert: every response contains non-null requestNumber, loanNumber, status, message, and non-empty correlationId
    - **Validates: Requirements 11.1, 11.4**

  - [ ]* 7.6 Write property test: Error response opacity
    - **Property 11: Error Response Opacity**
    - Generate error scenarios (DynamoDB failures, null pointers, etc.)
    - Assert: 500 response messages do not contain stack traces, class names, internal exception messages, or DynamoDB table names
    - **Validates: Requirements 11.3**

- [ ] 8. Step Functions business logic Lambdas
  - [ ] 8.1 Implement `InitializationLambda`
    - Receive input from Step Functions, persist initial state, return task token for callback wait
    - _Requirements: 1.1, 1.6_

  - [ ] 8.2 Implement `ReviewTypeAssignmentLambda`
    - Receive callback output, update review type in DynamoDB, emit new task token for decision wait
    - _Requirements: 2.1, 2.2, 2.5_

  - [ ] 8.3 Implement `DecisionProcessingLambda`
    - Receive callback output with decision and attributes, persist to DynamoDB
    - Invoke `PendingAttributeChecker`: if pending → return task token for re-wait; if complete → proceed
    - _Requirements: 3.1, 3.4, 3.5, 3.6_

  - [ ] 8.4 Implement `StatusDeterminationLambda`
    - Invoke `StatusDeterminationService` to determine status
    - Invoke `StatusRoutingService` to determine next step
    - Persist status and routing decision, record audit trail entry
    - _Requirements: 4.1–4.6, 5.1, 5.2, 5.3, 8.2_

  - [ ] 8.5 Implement `ExternalSystemsLambda` (mock/placeholder)
    - Simulate Vend/PPA integration call
    - Log integration attempt to CloudWatch
    - Return success/failure result
    - _Requirements: 7.1, 7.2, 7.3_

  - [ ] 8.6 Implement `CompletionLambda`
    - Set Workflow_Status to `Completed`, record completion timestamp
    - Record final audit trail entry
    - Log structured completion event to CloudWatch (requestNumber, loanNumber, finalStatus, duration)
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ]* 8.7 Write unit tests for all Step Functions Lambdas
    - Test each Lambda's happy path and error paths
    - Verify audit trail entries are created on status transitions
    - Verify DynamoDB persistence calls
    - _Requirements: 1.1, 2.1, 3.1, 4.1–4.5, 7.1, 8.1, 8.2_

- [ ] 9. Checkpoint — All Lambda handlers and business logic tests pass
  - Ensure all unit tests and property tests pass. Ask the user if questions arise.

- [ ] 10. Step Functions ASL definition
  - [ ] 10.1 Write the Step Functions state machine definition (ASL JSON)
    - Define states: Initialize, WaitForReviewType, AssignReviewType, WaitForDecision, ProcessDecision, CheckPending, DetermineStatus, RouteStatus, WaitForReclassConfirmation, ConfirmReclass, UpdateExternalSystems, Completion, Failed
    - Use `Task` states with `.waitForTaskToken` for callback waits (Initialize, WaitForReviewType, WaitForDecision, WaitForReclassConfirmation)
    - Use `Choice` state for CheckPending (pending → WaitForDecision, complete → DetermineStatus)
    - Use `Choice` state for RouteStatus (Reclass → WaitForReclassConfirmation, others → UpdateExternalSystems)
    - Configure `Retry` blocks: 2 retries, exponential backoff (IntervalSeconds: 2, MaxAttempts: 2, BackoffRate: 2.0)
    - Configure `Catch` blocks to route to Failed state
    - _Requirements: 9.1, 9.2_

  - [ ]* 10.2 Write unit tests for ASL definition validation
    - Validate JSON structure is valid ASL
    - Verify all states are reachable
    - Verify retry and catch configurations
    - _Requirements: 9.1, 9.2_

- [ ] 11. Timestamp invariant property test
  - [ ]* 11.1 Write property test: Last modified timestamp invariant
    - **Property 9: Last Modified Timestamp Invariant**
    - Generate sequences of update operations on WorkflowRepository
    - Assert: lastModifiedTimestamp is monotonically non-decreasing after each update
    - **Validates: Requirements 10.3**

- [ ] 12. Integration wiring and configuration
  - [ ] 12.1 Implement Spring configuration classes
    - DynamoDB client bean configuration
    - Step Functions client bean configuration
    - Table name and resource ARN externalized to environment variables
    - _Requirements: Project infrastructure_

  - [ ] 12.2 Wire all services with constructor injection
    - Ensure no field-level `@Autowired`
    - All dependencies injected via constructors
    - _Requirements: Code quality standards_

  - [ ]* 12.3 Write integration tests for end-to-end workflow
    - Test full workflow: initialize → assign type → submit decision (pending) → resubmit (complete) → status determination → external systems → completion
    - Test reclass path: initialize → assign → decision (reclass) → reclass confirmation → external systems → completion
    - Verify audit trail contains all transitions
    - _Requirements: 1.1, 2.1, 3.1, 4.1–4.5, 5.1, 5.2, 6.1, 6.2, 7.1, 8.1_

- [ ] 13. Final checkpoint — All tests pass
  - Ensure all unit tests, property tests, and integration tests pass. Verify ≥ 80% code coverage. Ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (12 properties total)
- Unit tests validate specific examples and edge cases
- The Step Functions ASL uses the `.waitForTaskToken` callback pattern for all human-interaction pause points
- External system integration (Vend/PPA) is mock/placeholder — real integration is out of scope
- User assignment is out of scope but the architecture supports adding it later
