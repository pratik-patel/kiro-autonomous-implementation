# Requirements Document: LDC Loan Review Workflow

## Introduction

The LDC Loan Review Workflow automates the end-to-end review process for loan requests within the PPA (Post-Purchase Analysis) system. The workflow orchestrates loan initialization, review type assignment, loan decision collection, status determination, reclass confirmation, and external system updates using AWS Step Functions, Lambda, and DynamoDB. The MFE (Micro Front-End) is the source of truth for Loan Decisions and Attributes — the workflow trusts API payloads and does not independently query the database to discover this data.

## Glossary

- **Workflow_Engine**: The AWS Step Functions state machine that orchestrates the loan review lifecycle.
- **API_Handler**: The AWS Lambda function that receives HTTP requests and sends task tokens to the Workflow_Engine.
- **Workflow_Repository**: The DynamoDB-backed persistence layer that stores workflow state, loan attributes, and decisions.
- **MFE**: The Micro Front-End application that serves as the source of truth for Loan Decisions and Attributes.
- **Request_Number**: A unique identifier for a loan review request.
- **Loan_Number**: A unique identifier for the loan under review.
- **Task_Number**: A unique identifier for a specific workflow execution task, used to correlate API calls with Step Function executions.
- **Request_Type**: The classification of a loan review request. Valid values: `LDC`, `Sec Policy`, `Conduit`.
- **Review_Type**: The assigned review classification for a loan, provided by the MFE.
- **Loan_Decision**: The decision rendered on a loan (e.g., Approved, Rejected, Repurchase, Reclass).
- **Loan_Attribute**: An individual attribute of a loan under review, each carrying a review status.
- **Attribute_Status**: The review status of a single Loan_Attribute. Values include: `Pending Review`, `Approved`, `Rejected`, `Repurchase`, `Reclass`.
- **Workflow_Status**: The overall status of the loan review. Values: `Initialized`, `Review Type Assigned`, `Decision Pending`, `Approved`, `Rejected`, `Partially Approved`, `Repurchase`, `Reclass Approved`, `Waiting for Confirmation`, `Updating External Systems`, `Completed`, `Failed`.
- **Task_Token**: An AWS Step Functions callback token used to resume a paused execution.
- **External_Systems**: Mock/placeholder integrations representing Vend and PPA downstream systems.
- **Audit_Trail**: A chronological record of all state transitions and actions performed during the workflow.

## Requirements

### Requirement 1: Workflow Initialization

**User Story:** As a loan reviewer, I want to initiate a PPA review workflow for a loan, so that the review process begins with validated inputs and persisted initial state.

#### Acceptance Criteria

1.1. WHEN the `startPPAreview` API is invoked with a valid payload, THE Workflow_Engine SHALL create a new Step Functions execution and persist the initial workflow state to the Workflow_Repository.

1.2. THE API_Handler SHALL validate that the `startPPAreview` payload contains all mandatory fields: Request_Number, Loan_Number, and Request_Type.

1.3. IF the `startPPAreview` payload is missing Request_Number, Loan_Number, or Request_Type, THEN THE API_Handler SHALL reject the request with a 400 Bad Request response containing a descriptive error message identifying the missing fields.

1.4. IF the Request_Type value is not one of `LDC`, `Sec Policy`, or `Conduit`, THEN THE API_Handler SHALL reject the request with a 400 Bad Request response indicating the invalid Request_Type.

1.5. WHEN the `startPPAreview` API payload includes optional Loan_Attributes, THE Workflow_Engine SHALL persist those attributes alongside the initial workflow state in the Workflow_Repository.

1.6. WHEN a new workflow execution is created, THE Workflow_Repository SHALL store the initial Workflow_Status as `Initialized` with a creation timestamp in UTC.

### Requirement 2: Review Type Assignment

**User Story:** As a loan reviewer, I want to assign or update the review type for a loan, so that the workflow proceeds with the correct classification.

#### Acceptance Criteria

2.1. WHEN the `assignToType` API is invoked with a valid payload containing Task_Number, Request_Number, Loan_Number, and Review_Type, THE Workflow_Engine SHALL update the Review_Type in the Workflow_Repository.

2.2. WHEN the Review_Type is successfully persisted, THE Workflow_Engine SHALL automatically transition the workflow to the Loan Decision phase.

2.3. THE API_Handler SHALL validate that the `assignToType` payload contains all mandatory fields: Task_Number, Request_Number, Loan_Number, and Review_Type.

2.4. IF the `assignToType` payload is missing any mandatory field, THEN THE API_Handler SHALL reject the request with a 400 Bad Request response identifying the missing fields.

2.5. WHEN the `assignToType` API is invoked, THE Workflow_Repository SHALL update the Workflow_Status to `Review Type Assigned` with an updated timestamp in UTC.

### Requirement 3: Loan Decision Processing

**User Story:** As a loan reviewer, I want to submit loan decisions and attributes via the workflow, so that the system persists decisions and determines the next step.

#### Acceptance Criteria

3.1. WHEN the `getNextStep` API is invoked with a valid payload containing Task_Number, Request_Number, Loan_Number, Loan_Decision, and Attributes, THE Workflow_Engine SHALL persist the Loan_Decision and Loan_Attributes to the Workflow_Repository.

3.2. THE API_Handler SHALL validate that the `getNextStep` payload contains all mandatory fields: Task_Number, Request_Number, Loan_Number, Loan_Decision, and Attributes.

3.3. IF the `getNextStep` payload is missing any mandatory field, THEN THE API_Handler SHALL reject the request with a 400 Bad Request response identifying the missing fields.

3.4. WHEN the Loan_Decision and Attributes are persisted, THE Workflow_Engine SHALL evaluate all Loan_Attributes to determine whether any attribute has an Attribute_Status of `Pending Review`.

3.5. WHILE any Loan_Attribute has an Attribute_Status of `Pending Review`, THE Workflow_Engine SHALL suspend the execution and wait for the next `getNextStep` API invocation via a callback Task_Token.

3.6. WHEN all Loan_Attributes have a non-pending Attribute_Status, THE Workflow_Engine SHALL proceed to Status Determination.

### Requirement 4: Status Determination

**User Story:** As a loan reviewer, I want the workflow to automatically determine the overall loan status based on attribute statuses, so that the correct routing decision is made.

#### Acceptance Criteria

4.1. WHEN all Loan_Attributes have an Attribute_Status of `Approved`, THE Workflow_Engine SHALL set the Workflow_Status to `Approved`.

4.2. WHEN all Loan_Attributes have an Attribute_Status of `Rejected`, THE Workflow_Engine SHALL set the Workflow_Status to `Rejected`.

4.3. WHEN the Loan_Attributes contain a mix of `Approved` and `Rejected` Attribute_Statuses, THE Workflow_Engine SHALL set the Workflow_Status to `Partially Approved`.

4.4. WHEN any Loan_Attribute has an Attribute_Status of `Repurchase`, THE Workflow_Engine SHALL set the Workflow_Status to `Repurchase`.

4.5. WHEN any Loan_Attribute has an Attribute_Status of `Reclass`, THE Workflow_Engine SHALL set the Workflow_Status to `Reclass Approved`.

4.6. THE Workflow_Engine SHALL persist the determined Workflow_Status to the Workflow_Repository with an updated timestamp in UTC.

### Requirement 5: Status Routing

**User Story:** As a loan reviewer, I want the workflow to route to the correct next step based on the determined status, so that the appropriate downstream actions occur.

#### Acceptance Criteria

5.1. WHEN the Workflow_Status is `Approved`, `Rejected`, `Partially Approved`, or `Repurchase`, THE Workflow_Engine SHALL route the workflow directly to the Update External Systems step.

5.2. WHEN the Workflow_Status is `Reclass Approved`, THE Workflow_Engine SHALL route the workflow to the Reclass Confirmation step and set the Workflow_Status to `Waiting for Confirmation`.

5.3. THE Workflow_Engine SHALL persist the routing decision and updated Workflow_Status to the Workflow_Repository.

### Requirement 6: Reclass Confirmation

**User Story:** As a loan reviewer, I want to confirm a reclass decision before the workflow updates external systems, so that reclass actions require explicit user confirmation.

#### Acceptance Criteria

6.1. WHILE the Workflow_Status is `Waiting for Confirmation`, THE Workflow_Engine SHALL suspend the execution and wait for a `getNextStep` API invocation via a callback Task_Token.

6.2. WHEN the `getNextStep` API is invoked during the `Waiting for Confirmation` state, THE Workflow_Engine SHALL update the confirmation status and resume the workflow to the Update External Systems step.

6.3. WHEN the reclass confirmation is received, THE Workflow_Repository SHALL record the confirmation timestamp in UTC and the confirming action.

### Requirement 7: External System Updates

**User Story:** As a system administrator, I want the workflow to update Vend and PPA systems upon loan review completion, so that downstream systems reflect the review outcome.

#### Acceptance Criteria

7.1. WHEN the workflow reaches the Update External Systems step, THE Workflow_Engine SHALL invoke the integration Lambda to update External_Systems with the final Workflow_Status and Loan_Decision.

7.2. IF the External_Systems integration fails, THEN THE Workflow_Engine SHALL log the failure details and set the Workflow_Status to `Failed`.

7.3. WHEN the External_Systems update completes successfully, THE Workflow_Engine SHALL transition the workflow to the Completion step.

### Requirement 8: Workflow Completion and Audit

**User Story:** As an auditor, I want a complete audit trail of all workflow state transitions, so that the review process is traceable and compliant.

#### Acceptance Criteria

8.1. WHEN the workflow reaches the Completion step, THE Workflow_Engine SHALL set the Workflow_Status to `Completed` and record the completion timestamp in UTC.

8.2. THE Workflow_Repository SHALL record an Audit_Trail entry for every Workflow_Status transition, including the previous status, new status, timestamp in UTC, and the triggering action.

8.3. WHEN the workflow completes, THE Workflow_Engine SHALL log a structured completion event to CloudWatch containing Request_Number, Loan_Number, final Workflow_Status, and total execution duration.

### Requirement 9: Error Handling

**User Story:** As a system operator, I want the workflow to handle errors gracefully, so that failures are logged and recoverable without data loss.

#### Acceptance Criteria

9.1. IF a Lambda function invocation fails during any workflow step, THEN THE Workflow_Engine SHALL retry the invocation up to 2 times with exponential backoff.

9.2. IF all retry attempts are exhausted, THEN THE Workflow_Engine SHALL set the Workflow_Status to `Failed` and log the error details to CloudWatch.

9.3. IF a DynamoDB write operation fails, THEN THE Workflow_Repository SHALL throw a descriptive exception containing the operation type, table name, and error reason.

9.4. IF an API request references a Task_Number that does not correspond to an active workflow execution, THEN THE API_Handler SHALL reject the request with a 404 Not Found response.

### Requirement 10: DynamoDB Persistence

**User Story:** As a developer, I want all workflow state persisted in DynamoDB with consistent data integrity, so that the workflow can resume from any point after interruption.

#### Acceptance Criteria

10.1. THE Workflow_Repository SHALL use Request_Number as the partition key and Loan_Number as the sort key for the workflow state table.

10.2. THE Workflow_Repository SHALL store the Task_Token alongside the workflow state to enable callback-based resumption.

10.3. WHEN any workflow state field is updated, THE Workflow_Repository SHALL update the `lastModifiedTimestamp` field with the current UTC timestamp.

10.4. THE Workflow_Repository SHALL store all Loan_Attributes as a nested structure within the workflow state record.

### Requirement 11: API Contract Compliance

**User Story:** As an API consumer, I want consistent and predictable API responses, so that the MFE can reliably interact with the workflow.

#### Acceptance Criteria

11.1. THE API_Handler SHALL return a JSON response body for all API endpoints containing at minimum: `requestNumber`, `loanNumber`, `status`, and `message`.

11.2. WHEN an API request is processed successfully, THE API_Handler SHALL return an HTTP 200 response with the current Workflow_Status.

11.3. IF an unexpected error occurs during API processing, THEN THE API_Handler SHALL return an HTTP 500 response with a correlation ID and a generic error message that does not expose internal details.

11.4. THE API_Handler SHALL include a unique correlation ID in every API response for traceability.
