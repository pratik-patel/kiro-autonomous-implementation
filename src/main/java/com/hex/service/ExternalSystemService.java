package com.hex.service;

import com.hex.exception.ExternalSystemException;
import com.hex.model.WorkflowState;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles downstream integration with Vend/PPA external systems.
 * Protected by Resilience4j circuit breaker for fault tolerance.
 */
@Service
public class ExternalSystemService {

    private static final Logger log = LoggerFactory.getLogger(ExternalSystemService.class);

    /**
     * Updates external systems (Vend/PPA) with the final workflow state.
     * This method is protected by a circuit breaker to prevent cascading failures.
     *
     * @param state the completed workflow state to propagate
     * @throws ExternalSystemException if the external system call fails
     */
    @CircuitBreaker(name = "externalSystem", fallbackMethod = "updateExternalSystemsFallback")
    public void updateExternalSystems(WorkflowState state) {
        log.info("Updating external systems: requestNumber={}, taskNumber={}, decision={}, correlationId={}",
                state.getRequestNumber(), state.getTaskNumber(),
                state.getLoanDecision(), state.getCorrelationId());

        // Placeholder for actual Vend/PPA integration
        // In production, this would invoke external REST APIs or publish to SNS/SQS
        simulateExternalCall(state);

        log.info("External systems updated: requestNumber={}, taskNumber={}",
                state.getRequestNumber(), state.getTaskNumber());
    }

    private void simulateExternalCall(WorkflowState state) {
        // Placeholder — will be replaced with actual HTTP client calls
        log.debug("Simulating external system call for loan: {}", state.getLoanNumber());
    }

    @SuppressWarnings("unused")
    private void updateExternalSystemsFallback(WorkflowState state, Throwable throwable) {
        log.error("Circuit breaker open — external system update failed: requestNumber={}, taskNumber={}, error={}",
                state.getRequestNumber(), state.getTaskNumber(), throwable.getMessage());
        throw new ExternalSystemException(
                "External system unavailable for requestNumber=" + state.getRequestNumber(), throwable);
    }
}
