package com.hex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hex.exception.ExternalSystemException;
import com.hex.exception.WorkflowExecutionException;
import com.hex.exception.WorkflowNotFoundException;
import com.hex.exception.WorkflowValidationException;
import com.hex.model.dto.AssignTypeRequest;
import com.hex.model.dto.LoanAttributeDto;
import com.hex.model.dto.NextStepRequest;
import com.hex.model.dto.StartWorkflowRequest;
import com.hex.model.dto.StartWorkflowResponse;
import com.hex.service.InputValidator;
import com.hex.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowController unit tests")
class WorkflowControllerTest {

    private static final String START_URL = "/api/v1/workflow/start";
    private static final String ASSIGN_TYPE_URL = "/api/v1/workflow/assign-type";
    private static final String NEXT_STEP_URL = "/api/v1/workflow/next-step";
    private static final String REQUEST_NUMBER = "REQ-123456";
    private static final String LOAN_NUMBER = "LN-123456";
    private static final String TASK_NUMBER = "TSK-ABCD1234";
    private static final String REVIEW_TYPE_LDC = "LDC";
    private static final String VALIDATION_MISSING_FIELD_CODE = "VALIDATION_MISSING_FIELD";
    private static final String JSON_SUCCESS = "$.success";
    private static final String JSON_MESSAGE = "$.message";
    private static final String JSON_CORRELATION_ID = "$.correlationId";
    private static final String JSON_ERROR_CODE = "$.errorCode";

    @Mock
    private InputValidator inputValidator;

    @Mock
    private WorkflowService workflowService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        WorkflowController controller = new WorkflowController(inputValidator, workflowService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/workflow/start")
    class StartWorkflowEndpointTests {

        @Test
        @DisplayName("should return 200 with task number on valid request")
        void startWorkflowSuccess() throws Exception {
            String executionArn = "arn:aws:states:us-east-1:000:execution:test:exec-1";
            StartWorkflowRequest request = StartWorkflowRequest.builder()
                    .requestNumber(REQUEST_NUMBER)
                    .loanNumber(LOAN_NUMBER)
                    .requestType(REVIEW_TYPE_LDC)
                    .build();
            StartWorkflowResponse serviceResponse = StartWorkflowResponse.builder()
                    .taskNumber(TASK_NUMBER)
                    .executionArn(executionArn)
                    .build();
            when(workflowService.startWorkflow(any(), anyString())).thenReturn(serviceResponse);

            mockMvc.perform(post(START_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_SUCCESS).value(true))
                    .andExpect(jsonPath(JSON_MESSAGE).value("Workflow started"))
                    .andExpect(jsonPath(JSON_CORRELATION_ID).isNotEmpty())
                    .andExpect(jsonPath("$.data.taskNumber").value(TASK_NUMBER))
                    .andExpect(jsonPath("$.data.executionArn").value(executionArn));

            verify(inputValidator).validateStartWorkflowRequest(any());
            verify(workflowService).startWorkflow(any(), anyString());
        }

        @Test
        @DisplayName("should return 400 when validation fails")
        void startWorkflowValidationFailure() throws Exception {
            doThrow(new WorkflowValidationException("Missing mandatory fields: requestNumber", VALIDATION_MISSING_FIELD_CODE))
                    .when(inputValidator).validateStartWorkflowRequest(any());

            StartWorkflowRequest request = StartWorkflowRequest.builder().build();

            mockMvc.perform(post(START_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_SUCCESS).value(false))
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(VALIDATION_MISSING_FIELD_CODE));

            verify(workflowService, never()).startWorkflow(any(), anyString());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/workflow/assign-type")
    class AssignTypeEndpointTests {

        @Test
        @DisplayName("should return 200 on valid assign type request")
        void assignTypeSuccess() throws Exception {
            AssignTypeRequest request = AssignTypeRequest.builder()
                    .taskNumber(TASK_NUMBER)
                    .requestNumber(REQUEST_NUMBER)
                    .loanNumber(LOAN_NUMBER)
                    .reviewType(REVIEW_TYPE_LDC)
                    .build();

            mockMvc.perform(post(ASSIGN_TYPE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_SUCCESS).value(true))
                    .andExpect(jsonPath(JSON_MESSAGE).value("Review type assigned"))
                    .andExpect(jsonPath(JSON_CORRELATION_ID).isNotEmpty());

            verify(inputValidator).validateAssignTypeRequest(any());
            verify(workflowService).assignReviewType(any(), anyString());
        }

        @Test
        @DisplayName("should return 404 when workflow not found")
        void assignTypeNotFound() throws Exception {
            doThrow(new WorkflowNotFoundException("Workflow not found"))
                    .when(workflowService).assignReviewType(any(), anyString());

            AssignTypeRequest request = AssignTypeRequest.builder()
                    .taskNumber(TASK_NUMBER)
                    .requestNumber(REQUEST_NUMBER)
                    .loanNumber(LOAN_NUMBER)
                    .reviewType(REVIEW_TYPE_LDC)
                    .build();

            mockMvc.perform(post(ASSIGN_TYPE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_SUCCESS).value(false))
                    .andExpect(jsonPath(JSON_ERROR_CODE).value("WORKFLOW_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/workflow/next-step")
    class NextStepEndpointTests {

        @Test
        @DisplayName("should return 200 on valid next step request")
        void nextStepSuccess() throws Exception {
            NextStepRequest request = NextStepRequest.builder()
                    .taskNumber(TASK_NUMBER)
                    .requestNumber(REQUEST_NUMBER)
                    .loanNumber(LOAN_NUMBER)
                    .loanDecision("APPROVED")
                    .attributes(List.of(LoanAttributeDto.builder()
                            .attributeName("attr1").attributeStatus("Approved").build()))
                    .build();

            mockMvc.perform(post(NEXT_STEP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_SUCCESS).value(true))
                    .andExpect(jsonPath(JSON_MESSAGE).value("Decision submitted"))
                    .andExpect(jsonPath(JSON_CORRELATION_ID).isNotEmpty());

            verify(inputValidator).validateNextStepRequest(any());
            verify(workflowService).submitDecision(any(), anyString());
        }

        @Test
        @DisplayName("should return 400 when validation fails for next step")
        void nextStepValidationFailure() throws Exception {
            doThrow(new WorkflowValidationException("Missing mandatory fields: loanDecision", VALIDATION_MISSING_FIELD_CODE))
                    .when(inputValidator).validateNextStepRequest(any());

            NextStepRequest request = NextStepRequest.builder().build();

            mockMvc.perform(post(NEXT_STEP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_SUCCESS).value(false))
                    .andExpect(jsonPath(JSON_ERROR_CODE).value(VALIDATION_MISSING_FIELD_CODE));

            verify(workflowService, never()).submitDecision(any(), anyString());
        }
    }

    @Nested
    @DisplayName("GlobalExceptionHandler coverage")
    class ExceptionHandlerTests {

        @Test
        @DisplayName("should return 502 when ExternalSystemException is thrown")
        void externalSystemError() throws Exception {
            doThrow(new ExternalSystemException("Vend unavailable"))
                    .when(workflowService).submitDecision(any(), anyString());

            NextStepRequest request = NextStepRequest.builder()
                    .taskNumber(TASK_NUMBER).requestNumber(REQUEST_NUMBER)
                    .loanNumber(LOAN_NUMBER).loanDecision("APPROVED")
                    .attributes(List.of(LoanAttributeDto.builder()
                            .attributeName("a1").attributeStatus("Approved").build()))
                    .build();

            mockMvc.perform(post(NEXT_STEP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath(JSON_SUCCESS).value(false))
                    .andExpect(jsonPath(JSON_ERROR_CODE).value("EXTERNAL_SYSTEM_ERROR"));
        }

        @Test
        @DisplayName("should return 500 when WorkflowExecutionException is thrown")
        void workflowExecutionError() throws Exception {
            doThrow(new WorkflowExecutionException("Step Functions failed", new RuntimeException("boom")))
                    .when(workflowService).startWorkflow(any(), anyString());

            StartWorkflowRequest request = StartWorkflowRequest.builder()
                    .requestNumber(REQUEST_NUMBER).loanNumber(LOAN_NUMBER)
                    .requestType(REVIEW_TYPE_LDC).build();

            mockMvc.perform(post(START_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath(JSON_SUCCESS).value(false))
                    .andExpect(jsonPath(JSON_ERROR_CODE).value("INTERNAL_ERROR"));
        }

        @Test
        @DisplayName("should return 500 for unexpected generic exceptions")
        void genericError() throws Exception {
            doThrow(new RuntimeException("unexpected"))
                    .when(workflowService).startWorkflow(any(), anyString());

            StartWorkflowRequest request = StartWorkflowRequest.builder()
                    .requestNumber(REQUEST_NUMBER).loanNumber(LOAN_NUMBER)
                    .requestType(REVIEW_TYPE_LDC).build();

            mockMvc.perform(post(START_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath(JSON_SUCCESS).value(false))
                    .andExpect(jsonPath(JSON_ERROR_CODE).value("INTERNAL_ERROR"));
        }
    }
}
