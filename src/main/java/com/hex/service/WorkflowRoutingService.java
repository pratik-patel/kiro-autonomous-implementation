package com.hex.service;

import com.hex.model.LoanDecisionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Determines the next workflow step based on the loan decision status.
 * Non-reclass statuses route to external system update; RECLASS_APPROVED routes to confirmation.
 */
@Service
public class WorkflowRoutingService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRoutingService.class);

    /**
     * Determines whether the workflow should route to external system update
     * or to a reclass confirmation step.
     *
     * @param status the determined loan decision status
     * @return true if the status requires reclass confirmation, false for direct external update
     */
    public boolean requiresReclassConfirmation(LoanDecisionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("LoanDecisionStatus must not be null");
        }
        boolean requiresConfirmation = status == LoanDecisionStatus.RECLASS_APPROVED;
        log.info("Routing decision: status={}, requiresReclassConfirmation={}", status, requiresConfirmation);
        return requiresConfirmation;
    }
}
