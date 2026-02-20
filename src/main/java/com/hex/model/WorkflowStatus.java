package com.hex.model;

/**
 * Represents the current state of a workflow execution.
 */
public enum WorkflowStatus {
    INITIALIZED,
    REVIEW_TYPE_ASSIGNED,
    PENDING_DECISION,
    DETERMINED,
    WAITING_CONFIRMATION,
    UPDATING_EXTERNAL,
    COMPLETED,
    FAILED
}
