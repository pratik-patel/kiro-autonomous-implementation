# Implementation Plan: LDC Loan Review Workflow

## Overview

Implementation follows a bottom-up approach: enums and models first, then validation, business logic, repository, service orchestration, controller, Step Functions definition, and finally integration wiring. Property-based tests are co-located with the components they validate to catch errors early.

## Tasks

- [ ] 1. Set up project structure, enums, and domain models
  - [ ] 1.1 Create Maven project skeleton with pom.xml (Spring Boot 3.x, Java 17, AWS SDK v2, jqwik, Lombok, MapStruct, Resilience4j, DynamoDB Enhanced Client)
    - _Requirements: All_
  - [ ] 1.2 Create enums: `ReviewType`, `WorkflowStatus`, `LoanDecisionStatus`, `AttributeStatus`
    - Define allowed values and `fromString()` parsing methods
    - _Requirements: 1.4, 4.1-4.5_
  - [ ] 1.3 Create domain model `LoanAttribute` (@Value @Builder)
    - Fields: attributeName, attributeStatus (AttributeStatus enum)
    - _Requirements: 3.5, 3.6, 4.1-4.5_
  - [ ] 1.4 Create domain model `WorkflowState` (@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder)
    - Fields: requestNumber (PK), taskNumber (SK), loanNumber, reviewType, workflowStatus, loanDecision, attributes, correlationId, executionArn, currentTaskToken, createdAt, updatedAt, ttl
    - DynamoDB Enhanced Client annotations
    - _Requirements: 8.1-8.4_
  - [ ] 1.5 Create request DTOs: `StartWorkflowRequest`, `AssignTypeRequest`, `NextStepRequest`, `LoanAttributeDto` (@Value @Builder)
    - _Requirements: 1.1, 2.1, 3.1_
  - [ ] 1.6 Create response DTOs: `ApiResponse<T>`, `StartWorkflowResponse` (@Value @Builder)
    - _Requirements: 9.4_

- [ ] 2. Implement input validation
  - [ ] 2.1 Create custom exceptions: `WorkflowValidationException`, `WorkflowNotFoundException`, `ExternalSystemException`, `WorkflowExecutionException`
    - _Requirements: 9.3, 9.4_
  - [ ] 2.2 Implement `InputValidator` service
    - Validate mandatory field presence for each API (startPPAreview, assignToType, getNextStep)
    - Validate Request_Type / Review_Type against allowed enum values
    - Validate identifier format (Request_Number, Loan_Number, Task_Number) via regex
    - Validate payload size against configured maximum
    - _Requirements: 1.2-1.5, 2.2-2.3, 3.2-3.3, 9.1, 9.2_
  - [ ]* 2.3 Write property tests for InputValidator (Properties 1, 2, 3)
    - **Property 1: Mandatory field validation rejects incomplete payloads**
    - **Property 2: Identifier format validation**
    - **Property 3: Request type validation accepts only allowed values**
    - **Validates: Requirements 1.2, 1.3, 1.4, 1.5, 2.2, 2.3, 3.2, 3.3, 9.1**

- [ ] 3. Implement status determination logic
  - [ ] 3.1 Implement `StatusDeterminationService`
    - `determineStatus(List<LoanAttribute>)` with priority: REPURCHASE > RECLASS_APPROVED > PARTIALLY_APPROVED > APPROVED / REJECTED
    - `hasPendingAttributes(List<LoanAttribute>)` returns boolean
    - _Requirements: 3.5, 3.6, 4.1-4.5_
  - [ ]* 3.2 Write property tests for StatusDeterminationService (Properties 4-9)
    - **Property 4: Pending attribute detection controls workflow progression**
    - **Property 5: Homogeneous approved attributes yield APPROVED**
    - **Property 6: Homogeneous rejected attributes yield REJECTED**
    - **Property 7: Mixed approved/rejected yield PARTIALLY_APPROVED**
    - **Property 8: Repurchase takes priority**
    - **Property 9: Reclass takes priority over partial/approved/rejected**
    - **Validates: Requirements 3.5, 3.6, 4.1-4.5**

- [ ] 4. Checkpoint - Ensure all tests pass
  - Run `mvn test` and verify all property tests and unit tests pass
  - Ask the user if questions arise

- [ ] 5. Implement DynamoDB repository layer
  - [ ] 5.1 Create `DynamoDBConfig` configuration class
    - Configure DynamoDB Enhanced Client bean
    - Externalize table name and region via application.yml
    - _Requirements: 8.1_
  - [ ] 5.2 Implement `WorkflowRepository`
    - `save(WorkflowState)`, `findByRequestAndTask(requestNumber, taskNumber)`, `updateStatus(...)`, `updateDecision(...)`
    - Use DynamoDB Enhanced Client with `@DynamoDbBean` annotations
    - _Requirements: 8.1-8.4_
  - [ ]* 5.3 Write unit tests for WorkflowRepository
    - Test save, find, update operations with mocked DynamoDB client
    - _Requirements: 8.1-8.4_

- [ ] 6. Implement AWS Step Functions client wrapper
  - [ ] 6.1 Create `StepFunctionsClientWrapper` service
    - `startExecution(stateMachineArn, input)`, `sendTaskSuccess(taskToken, output)`, `sendTaskFailure(taskToken, error, cause)`
    - Wrap AWS SDK SfnClient with error handling and logging
    - _Requirements: 1.1, 2.1, 3.1_
  - [ ] 6.2 Create `StepFunctionsConfig` configuration class
    - Configure SfnClient bean, externalize state machine ARN
    - _Requirements: 1.1_

- [ ] 7. Implement workflow service layer
  - [ ] 7.1 Implement `WorkflowService`
    - `startWorkflow(StartWorkflowRequest)`: Generate Task_Number, start execution, return response
    - `assignReviewType(AssignTypeRequest)`: Validate workflow exists, send task success
    - `submitDecision(NextStepRequest)`: Validate workflow exists, send task success
    - _Requirements: 1.1, 1.6-1.8, 2.1, 2.4-2.5, 3.1, 3.4_
  - [ ]* 7.2 Write unit tests for WorkflowService
    - Test each method with mocked repository and Step Functions client
    - _Requirements: 1.1, 2.1, 3.1, 9.5_

- [ ] 8. Implement external system integration
  - [ ] 8.1 Implement `ExternalSystemService`
    - `updateExternalSystems(WorkflowState)`: Mock/placeholder for Vend/PPA integration
    - Configure Resilience4j circuit breaker
    - _Requirements: 6.1-6.3_
  - [ ]* 8.2 Write unit tests for ExternalSystemService
    - Test success path, failure path, circuit breaker behavior
    - _Requirements: 6.1-6.3_

- [ ] 9. Implement REST controller
  - [ ] 9.1 Implement `WorkflowController`
    - POST `/api/v1/workflow/start` → startPPAreview
    - POST `/api/v1/workflow/assign-type` → assignToType
    - POST `/api/v1/workflow/next-step` → getNextStep
    - Generate correlation ID, delegate to InputValidator and WorkflowService
    - _Requirements: 1.1, 2.1, 3.1_
  - [ ] 9.2 Implement `GlobalExceptionHandler` (@ControllerAdvice)
    - Map custom exceptions to standardized ApiResponse with error codes
    - _Requirements: 9.3, 9.4_
  - [ ]* 9.3 Write unit tests for WorkflowController (MockMvc)
    - Test valid requests, validation errors (400), not found (404), server errors (500)
    - _Requirements: 1.1-1.5, 2.1-2.3, 3.1-3.3, 9.1-9.5_

- [ ] 10. Implement workflow routing logic
  - [ ] 10.1 Implement `WorkflowRoutingService`
    - `determineNextStep(LoanDecisionStatus)`: Routes to external update or reclass confirmation
    - _Requirements: 5.1-5.4_
  - [ ]* 10.2 Write property test for routing logic (Property 10)
    - **Property 10: Non-reclass statuses route to external update**
    - **Validates: Requirements 5.1, 5.2**

- [ ] 11. Implement Step Functions task handler and audit
  - [ ] 11.1 Implement `WorkflowTaskHandler` Lambda handler
    - Handle Initialize, PersistReviewType, ProcessDecision, DetermineStatus, UpdateExternalSystems, LogAuditTrail tasks
    - Delegate to appropriate services
    - _Requirements: 1.6-1.8, 2.4-2.5, 3.4-3.6, 4.1-4.6, 5.1-5.4, 6.1-6.3, 7.1-7.3_
  - [ ]* 11.2 Write unit tests for WorkflowTaskHandler
    - Test each task type with mocked services
    - _Requirements: 7.1-7.3_

- [ ] 12. Create Step Functions state machine definition
  - [ ] 12.1 Write ASL (Amazon States Language) JSON definition
    - Define all states: Initialize, WaitForAssignToType, AssignReviewType, WaitForDecision, ProcessDecision, CheckPendingAttributes, DetermineStatus, RouteByStatus, WaitForReclassConfirmation, ConfirmReclass, UpdateExternalSystems, LogAuditTrail, Success, ErrorState
    - Use Task states with `.waitForTaskToken` for callback pattern
    - _Requirements: All workflow steps (1-7)_

- [ ] 13. Create DynamoDB table Terraform definition
  - [ ] 13.1 Write Terraform configuration for `ldc-workflow-state` table
    - PK: requestNumber, SK: taskNumber
    - GSI: loanNumber-index (PK: loanNumber, SK: createdAt)
    - On-demand capacity, SSE encryption, PITR enabled, TTL on `ttl` field
    - _Requirements: 8.3, 7.3_

- [ ] 14. Final checkpoint - Ensure all tests pass
  - Run `mvn test` and verify all tests pass
  - Verify coverage ≥ 80% on all new classes
  - Ask the user if questions arise

 - [ ] 15. Create Terraform scripts and validate the deployment

 - [ ] 16. Create end to end integration tests for validating all happy and negative scenarios

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints at tasks 4 and 14 ensure incremental validation
- Property tests validate universal correctness properties (Properties 1-10)
- Unit tests validate specific examples and edge cases
- Status determination priority: REPURCHASE > RECLASS_APPROVED > PARTIALLY_APPROVED > APPROVED / REJECTED
