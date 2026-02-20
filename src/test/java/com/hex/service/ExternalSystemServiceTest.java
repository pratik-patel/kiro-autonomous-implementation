package com.hex.service;

import com.hex.model.WorkflowState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("ExternalSystemService unit tests")
class ExternalSystemServiceTest {

    private final ExternalSystemService externalSystemService = new ExternalSystemService();

    private WorkflowState createCompletedState() {
        return WorkflowState.builder()
                .requestNumber("REQ-123456")
                .taskNumber("TSK-ABCD1234")
                .loanNumber("LN-123456")
                .loanDecision("APPROVED")
                .correlationId("corr-001")
                .build();
    }

    @Test
    @DisplayName("should update external systems without error for valid workflow state")
    void updateExternalSystemsSuccess() {
        assertThatNoException()
                .isThrownBy(() -> externalSystemService.updateExternalSystems(createCompletedState()));
    }

    @Test
    @DisplayName("should handle different decision types without error")
    void updateExternalSystemsWithRepurchaseDecision() {
        WorkflowState state = WorkflowState.builder()
                .requestNumber("REQ-654321")
                .taskNumber("TSK-EFGH5678")
                .loanNumber("LN-654321")
                .loanDecision("REPURCHASE")
                .correlationId("corr-002")
                .build();

        assertThatNoException()
                .isThrownBy(() -> externalSystemService.updateExternalSystems(state));
    }
}
