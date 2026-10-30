/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.exceptionhandler;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.workflow.exception.*;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class WorkflowGlobalExceptionHandlerTest {

    private WorkflowGlobalExceptionHandler handler;

    static Stream<Arguments> errorResponseCases() {
        return Stream.of(
                Arguments.of(
                        "WorkflowNotFound",
                        (Function<
                                        WorkflowGlobalExceptionHandler,
                                        ResponseEntity<Map<String, String>>>)
                                h -> h.handleNotFound(new WorkflowNotFoundException(100L)),
                        HttpStatus.NOT_FOUND,
                        "Workflow not found: 100"),
                Arguments.of(
                        "WorkflowParticipantNotFound",
                        (Function<
                                        WorkflowGlobalExceptionHandler,
                                        ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleNotFound(
                                                new WorkflowParticipantNotFoundException(200L)),
                        HttpStatus.NOT_FOUND,
                        "Workflow participant not found: 200"),
                Arguments.of(
                        "WorkflowState",
                        (Function<
                                        WorkflowGlobalExceptionHandler,
                                        ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleConflict(
                                                new WorkflowStateException("invalid transition")),
                        HttpStatus.CONFLICT,
                        "invalid transition"),
                Arguments.of(
                        "WorkflowConfiguration",
                        (Function<
                                        WorkflowGlobalExceptionHandler,
                                        ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleConflict(
                                                new WorkflowConfigurationException(
                                                        "misconfigured workflow")),
                        HttpStatus.CONFLICT,
                        "misconfigured workflow"),
                Arguments.of(
                        "WorkflowAccessDenied",
                        (Function<
                                        WorkflowGlobalExceptionHandler,
                                        ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleAccessDenied(
                                                new WorkflowAccessDeniedException(
                                                        "workflow denied")),
                        HttpStatus.FORBIDDEN,
                        "workflow denied"));
    }

    static Stream<Arguments> validationResponseCases() {
        return Stream.of(
                Arguments.of(
                        "WorkflowValidationWithoutCode",
                        new WorkflowValidationException("invalid workflow input"),
                        Map.of("error", "invalid workflow input")),
                Arguments.of(
                        "WorkflowValidationWithCode",
                        new WorkflowValidationException(
                                "workflow already open",
                                WorkflowValidationException.CODE_TYPE_ALREADY_OPEN),
                        Map.of(
                                "error",
                                "workflow already open",
                                "code",
                                WorkflowValidationException.CODE_TYPE_ALREADY_OPEN)),
                Arguments.of(
                        "WorkflowValidationWithBlankCode",
                        new WorkflowValidationException("invalid workflow input", "   "),
                        Map.of("error", "invalid workflow input")));
    }

    @BeforeEach
    void setUp() {
        handler = new WorkflowGlobalExceptionHandler();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errorResponseCases")
    void returnsExpectedStatusAndErrorBody(
            String description,
            Function<WorkflowGlobalExceptionHandler, ResponseEntity<Map<String, String>>>
                    handlerCall,
            HttpStatus expectedStatus,
            String expectedError) {
        ResponseEntity<Map<String, String>> response = handlerCall.apply(handler);

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).containsEntry("error", expectedError);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validationResponseCases")
    void validationReturnsExpectedStatusAndBody(
            String description,
            WorkflowValidationException exception,
            Map<String, String> expectedBody) {
        ResponseEntity<Map<String, String>> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsAllEntriesOf(expectedBody);
    }
}
