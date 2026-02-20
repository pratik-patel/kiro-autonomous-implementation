package com.hex.service;

import com.hex.model.LoanDecisionStatus;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for WorkflowRoutingService.
 * Feature: ldc-loan-review-workflow, Property 10: Non-reclass statuses route to external update
 */
class WorkflowRoutingPropertyTest {

    private final WorkflowRoutingService routingService = new WorkflowRoutingService();

    // ========================================================================
    // Property 10: Non-reclass statuses route to external update
    // Validates: Requirements 5.1, 5.2
    // ========================================================================

    @Property(tries = 100)
    @Label("Property 10a: Non-reclass statuses do not require reclass confirmation")
    void nonReclassStatusesRouteToExternalUpdate(
            @ForAll("nonReclassStatus") LoanDecisionStatus status) {

        assertThat(routingService.requiresReclassConfirmation(status)).isFalse();
    }

    @Property(tries = 100)
    @Label("Property 10b: RECLASS_APPROVED always requires confirmation")
    void reclassApprovedRequiresConfirmation() {
        assertThat(routingService.requiresReclassConfirmation(LoanDecisionStatus.RECLASS_APPROVED))
                .isTrue();
    }

    @Provide
    Arbitrary<LoanDecisionStatus> nonReclassStatus() {
        return Arbitraries.of(
                LoanDecisionStatus.APPROVED,
                LoanDecisionStatus.REJECTED,
                LoanDecisionStatus.PARTIALLY_APPROVED,
                LoanDecisionStatus.REPURCHASE
        );
    }
}
