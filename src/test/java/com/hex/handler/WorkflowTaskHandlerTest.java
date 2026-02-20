package com.hex.handler;

import com.hex.model.AttributeStatus;
import com.hex.model.LoanAttribute;
import com.hex.model.LoanDecisionStatus;
import com.hex.model.WorkflowState;
import com.hex.repository.WorkflowRepository;
import com.hex.service.ExternalSystemService;
import com.hex.service.StatusDeterminationService;
import com.hex.service.WorkflowRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowTaskHandler unit tests")
class WorkflowTaskHandlerTest {

    private static final String REQUEST_NUMBER = "REQ-123456";
    private static final String TASK_NUMBER = "TSK-ABCD1234";
    private static final String STATUS_KEY = "status";
    private static final String COMPLETED_STATUS = "COMPLETED";

    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private StatusDeterminationService statusDeterminationService;
    @Mock
    private ExternalSystemService externalSystemService;
    @Mock
    private WorkflowRoutingService workflowRoutingService;

    private WorkflowTaskHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WorkflowTaskHandler(
                workflowRepository, statusDeterminationService,
                externalSystemService, workflowRoutingService);
    }

    private Map<String, Object> createInput(String action) {
        Map<String, Object> input = new HashMap<>();
        input.put("action", action);
        input.put("requestNumber", REQUEST_NUMBER);
        input.put("taskNumber", TASK_NUMBER);
        return input;
    }

    @Test
    @DisplayName("PERSIST_INITIAL_STATE updates status to INITIALIZED")
    void persistInitialState() {
        Map<String, Object> result = handler.handleTask(createInput("PERSIST_INITIAL_STATE"));

        verify(workflowRepository).updateStatus(REQUEST_NUMBER, TASK_NUMBER, "INITIALIZED");
        assertThat(result).containsEntry(STATUS_KEY, "INITIALIZED");
    }

    @Test
    @DisplayName("PERSIST_REVIEW_TYPE updates status to REVIEW_TYPE_ASSIGNED")
    void persistReviewType() {
        Map<String, Object> result = handler.handleTask(createInput("PERSIST_REVIEW_TYPE"));

        verify(workflowRepository).updateStatus(REQUEST_NUMBER, TASK_NUMBER, "REVIEW_TYPE_ASSIGNED");
        assertThat(result).containsEntry(STATUS_KEY, "REVIEW_TYPE_ASSIGNED");
    }

    @Test
    @DisplayName("CHECK_PENDING returns hasPendingAttributes flag")
    void checkPending() {
        List<LoanAttribute> attributes = List.of(
                LoanAttribute.builder().attributeName("a1").attributeStatus(AttributeStatus.PENDING_REVIEW).build());
        WorkflowState state = WorkflowState.builder()
                .requestNumber(REQUEST_NUMBER).taskNumber(TASK_NUMBER)
                .attributes(attributes).build();
        when(workflowRepository.findByRequestAndTask(REQUEST_NUMBER, TASK_NUMBER))
                .thenReturn(Optional.of(state));
        when(statusDeterminationService.hasPendingAttributes(attributes)).thenReturn(true);

        Map<String, Object> result = handler.handleTask(createInput("CHECK_PENDING"));

        assertThat(result).containsEntry("hasPendingAttributes", true);
    }

    @Test
    @DisplayName("DETERMINE_STATUS returns decision and routing flag")
    void determineStatus() {
        List<LoanAttribute> attributes = List.of(
                LoanAttribute.builder().attributeName("a1").attributeStatus(AttributeStatus.APPROVED).build());
        WorkflowState state = WorkflowState.builder()
                .requestNumber(REQUEST_NUMBER).taskNumber(TASK_NUMBER)
                .attributes(attributes).build();
        when(workflowRepository.findByRequestAndTask(REQUEST_NUMBER, TASK_NUMBER))
                .thenReturn(Optional.of(state));
        when(statusDeterminationService.determineStatus(attributes)).thenReturn(LoanDecisionStatus.APPROVED);
        when(workflowRoutingService.requiresReclassConfirmation(LoanDecisionStatus.APPROVED)).thenReturn(false);

        Map<String, Object> result = handler.handleTask(createInput("DETERMINE_STATUS"));

        assertThat(result).containsEntry("decision", "APPROVED");
        assertThat(result).containsEntry("requiresReclassConfirmation", false);
        verify(workflowRepository).updateStatus(REQUEST_NUMBER, TASK_NUMBER, "DETERMINED");
    }

    @Test
    @DisplayName("UPDATE_EXTERNAL calls external service and marks COMPLETED")
    void updateExternal() {
        WorkflowState state = WorkflowState.builder()
                .requestNumber(REQUEST_NUMBER).taskNumber(TASK_NUMBER).build();
        when(workflowRepository.findByRequestAndTask(REQUEST_NUMBER, TASK_NUMBER))
                .thenReturn(Optional.of(state));

        Map<String, Object> result = handler.handleTask(createInput("UPDATE_EXTERNAL"));

        verify(externalSystemService).updateExternalSystems(state);
        verify(workflowRepository).updateStatus(REQUEST_NUMBER, TASK_NUMBER, COMPLETED_STATUS);
        assertThat(result).containsEntry(STATUS_KEY, COMPLETED_STATUS);
    }

    @Test
    @DisplayName("LOG_AUDIT marks COMPLETED and returns auditLogged flag")
    void logAudit() {
        Map<String, Object> result = handler.handleTask(createInput("LOG_AUDIT"));

        verify(workflowRepository).updateStatus(REQUEST_NUMBER, TASK_NUMBER, COMPLETED_STATUS);
        assertThat(result).containsEntry("auditLogged", true);
    }

    @Test
    @DisplayName("Unknown action throws IllegalArgumentException")
    void unknownAction() {
        assertThatThrownBy(() -> handler.handleTask(createInput("UNKNOWN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown action");
    }

    @Test
    @DisplayName("Null action throws IllegalArgumentException")
    void nullAction() {
        Map<String, Object> input = new HashMap<>();
        input.put("requestNumber", REQUEST_NUMBER);
        input.put("taskNumber", TASK_NUMBER);
        assertThatThrownBy(() -> handler.handleTask(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action is required");
    }
}
