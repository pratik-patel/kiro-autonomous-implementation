# Requirements Document

## Introduction

The LDC Loan Review Workflow automates the end-to-end review process for loan decisions within the Delinquency Management System (DMS). The workflow is orchestrated by AWS Step Functions, with Lambda-backed APIs driving state transitions, and DynamoDB persisting workflow state, loan attributes, and decisions. The system supports multiple review types (LDC, Sec Policy, Conduit) and routes loan decisions through status determination, optional reclass confirmation, and external system updates.

## Glossary

- **Workflow_Engine**: The AWS Step Functions state machine that orchestrates the loan review lifecycle.
- **API_Handler**: The AWS Lambda function(s) that receive API requests and send task tokens to the Workflow_Engine.
- **Workflow_State_Store**: The DynamoDB table that persists workflow execution state, loan attributes, and decisions.
- **MFE**: Micro Frontend — the UI application that is the source of truth for Loan Decisions and Attributes. The Workflow_Engine trusts payloads from the MFE without querying the database to discover this data.
- **Review_Type**: The classification of a loan review. Must be one of: `LDC`, `Sec Policy`, `Conduit`.
- **Loan_Attribute**: A named property of a loan under review, each carrying a review status (e.g., Pending Review, Approved, Rejected).
- **Loan_Decision**: The overall decision for a loan, determined by the aggregate status of all Loan_Attributes.
- **External_System_Integrator**: The Lambda function(s) responsible for updating downstream platforms (Vend/PPA) with finalized review outcomes.
- **Task_Token**: The Step Functions callback token used to resume a paused execution when an API call is received.
- **Request_Number**: A unique identifier for a loan review request.
- **Loan_Number**: A unique identifier for the loan being reviewed.
- **Task_Number**: A unique identifier for a workflow task instance, generated during initialization.

## Requirements

### Requirement 1: Workflow Initialization

**User Story:** As a loan reviewer, I want to initiate a PPA review workflow for a loan, so that the review process begins with validated inputs and persisted initial state.

#### Acceptance Criteria

1. WHEN the `startPPAreview` API is invoked with a valid payload, THE API_Handler SHALL start a new Workflow_Engine execution and return the generated Task_Number.
2. THE API_Handler SHALL validate that the payload contains all mandatory fields: Request_Number, Loan_Number, and Request_Type.
3. WHEN any mandatory field is missing from the `startPPAreview` payload, THE API_Handler SHALL reject the request with a descriptive validation error and HTTP 400 status.
4. THE API_Handler SHALL validate that Request_Type is one of the allowed values: `LDC`, `Sec Policy`, `Conduit`.
5. WHEN Request_Type is not one of the allowed values, THE API_Handler SHALL reject the request with a descriptive validation error and HTTP 400 status.
6. WHEN the Workflow_Engine execution starts successfully, THE Workflow_Engine SHALL persist the initial workflow state (Request_Number, Loan_Number, Request_Type, Task_Number, status, and optional Attributes) to the Workflow_State_Store.
7. THE Workflow_Engine SHALL assign the Review_Type from the payload during initialization.
8. WHEN optional Attributes are provided in the `startPPAreview` payload, THE Workflow_Engine SHALL persist the Attributes alongside the initial workflow state.

### Requirement 2: Review Type Assignment

**User Story:** As a loan reviewer, I want to update the review type for an in-progress workflow, so that the review classification can be corrected before loan decisions are made.

#### Acceptance Criteria

1. WHEN the `assignToType` API is invoked with a valid Task_Number, Request_Number, Loan_Number, and Review_Type, THE API_Handler SHALL resume the Workflow_Engine execution with the updated Review_Type.
2. THE API_Handler SHALL validate that all mandatory fields (Task_Number, Request_Number, Loan_Number, Review_Type) are present.
3. WHEN any mandatory field is missing from the `assignToType` payload, THE API_Handler SHALL reject the request with a descriptive validation error and HTTP 400 status.
4. THE Workflow_Engine SHALL persist the updated Review_Type to the Workflow_State_Store.
5. WHEN the Review_Type update is persisted, THE Workflow_Engine SHALL automatically transition to the Loan Decision phase.

### Requirement 3: Loan Decision Submission and Completion Check

**User Story:** As a loan reviewer, I want to submit loan decisions and attributes, so that the system determines the next step based on whether all attributes are reviewed.

#### Acceptance Criteria

1. WHEN the `getNextStep` API is invoked with a valid payload containing Task_Number, Request_Number, Loan_Number, Loan_Decision, and Attributes, THE API_Handler SHALL resume the Workflow_Engine execution with the submitted decision data.
2. THE API_Handler SHALL validate that all mandatory fields (Task_Number, Request_Number, Loan_Number, Loan_Decision, Attributes) are present.
3. WHEN any mandatory field is missing from the `getNextStep` payload, THE API_Handler SHALL reject the request with a descriptive validation error and HTTP 400 status.
4. THE Workflow_Engine SHALL persist the submitted Loan_Decision and Attributes to the Workflow_State_Store.
5. WHEN any Loan_Attribute has a status of "Pending Review", THE Workflow_Engine SHALL suspend execution and wait for the next `getNextStep` API call (loop back).
6. WHEN all Loan_Attributes have a status other than "Pending Review", THE Workflow_Engine SHALL proceed to Status Determination.

### Requirement 4: Status Determination

**User Story:** As a loan reviewer, I want the system to automatically determine the overall loan review status based on attribute statuses, so that the workflow routes correctly.

#### Acceptance Criteria

1. WHEN all Loan_Attributes have a status of "Approved", THE Workflow_Engine SHALL set the Loan_Decision status to "Approved".
2. WHEN all Loan_Attributes have a status of "Rejected", THE Workflow_Engine SHALL set the Loan_Decision status to "Rejected".
3. WHEN Loan_Attributes contain a mix of "Approved" and "Rejected" statuses, THE Workflow_Engine SHALL set the Loan_Decision status to "Partially Approved".
4. WHEN any Loan_Attribute has an explicit "Repurchase" status, THE Workflow_Engine SHALL set the Loan_Decision status to "Repurchase".
5. WHEN any Loan_Attribute has an explicit "Reclass" status, THE Workflow_Engine SHALL set the Loan_Decision status to "Reclass Approved".
6. THE Workflow_Engine SHALL persist the determined Loan_Decision status to the Workflow_State_Store.

### Requirement 5: Status Routing and Reclass Confirmation

**User Story:** As a loan reviewer, I want the workflow to route based on the determined status, and require confirmation for reclass decisions, so that reclass outcomes are explicitly acknowledged.

#### Acceptance Criteria

1. WHEN the Loan_Decision status is "Approved", "Rejected", "Partially Approved", or "Repurchase", THE Workflow_Engine SHALL route directly to the External System Update step.
2. WHEN the Loan_Decision status is "Reclass Approved", THE Workflow_Engine SHALL set the workflow status to "Waiting for Confirmation" and suspend execution.
3. WHILE the workflow status is "Waiting for Confirmation", THE Workflow_Engine SHALL wait for the user to invoke the `getNextStep` API to confirm the reclass.
4. WHEN the user invokes `getNextStep` while the workflow status is "Waiting for Confirmation", THE Workflow_Engine SHALL update the confirmation state and resume to the External System Update step.

### Requirement 6: External System Update

**User Story:** As a system administrator, I want the workflow to update downstream platforms with finalized review outcomes, so that external systems reflect the loan review decision.

#### Acceptance Criteria

1. WHEN the Workflow_Engine reaches the External System Update step, THE External_System_Integrator SHALL invoke the downstream platform integration (Vend/PPA) with the finalized loan review data.
2. IF the External_System_Integrator fails to update the downstream platform, THEN THE Workflow_Engine SHALL log the failure with the correlation ID and transition to an error state.
3. WHEN the External_System_Integrator successfully updates the downstream platform, THE Workflow_Engine SHALL transition to the Completion step.

### Requirement 7: Workflow Completion and Audit Trail

**User Story:** As an auditor, I want the workflow to log a complete audit trail upon completion, so that all review actions are traceable.

#### Acceptance Criteria

1. WHEN the Workflow_Engine reaches the Completion step, THE Workflow_Engine SHALL log a complete audit trail entry to the Workflow_State_Store containing: Task_Number, Request_Number, Loan_Number, Review_Type, Loan_Decision, all Attributes, timestamps, and final status.
2. WHEN the audit trail is logged, THE Workflow_Engine SHALL transition to the terminal Success state.
3. THE Workflow_State_Store SHALL retain audit trail records according to the configured data retention policy.

### Requirement 8: Workflow State Persistence

**User Story:** As a system operator, I want all workflow state changes to be persisted to DynamoDB, so that the workflow can be resumed after suspension and state is never lost.

#### Acceptance Criteria

1. THE Workflow_Engine SHALL persist workflow state to the Workflow_State_Store after every state transition.
2. WHEN the Workflow_Engine suspends execution (waiting for API callback), THE Workflow_State_Store SHALL contain sufficient state to resume the workflow from the suspension point.
3. THE Workflow_State_Store SHALL use Request_Number as the partition key and Task_Number as the sort key.
4. THE Workflow_Engine SHALL include a correlation ID in every state persistence operation for traceability.

### Requirement 9: Input Validation and Error Handling

**User Story:** As a developer, I want all API inputs to be validated at the trust boundary, so that invalid data never reaches the workflow engine.

#### Acceptance Criteria

1. THE API_Handler SHALL validate the format of Request_Number, Loan_Number, and Task_Number against expected identifier patterns.
2. WHEN the API_Handler receives a payload that exceeds the maximum allowed size, THE API_Handler SHALL reject the request with HTTP 413 status.
3. IF an unexpected error occurs during workflow execution, THEN THE Workflow_Engine SHALL log the error with the correlation ID and transition to a failed state without exposing internal details in the API response.
4. THE API_Handler SHALL return standardized error responses containing an error code, a user-friendly message, and the correlation ID.
5. IF the Task_Number provided in `assignToType` or `getNextStep` does not match an active workflow execution, THEN THE API_Handler SHALL reject the request with HTTP 404 status and a descriptive error message.
