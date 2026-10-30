/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permission.exceptionhandler;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.permissiongroup.exception.PermissionGroupNotFoundException;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exceptionhandler.ClientErrorMessageSupport;
import com.emberone.testsupport.ExceptionOriginTestSupport;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class PermissionGlobalExceptionHandlerTest {

    private PermissionGlobalExceptionHandler handler;

    static Stream<Arguments> errorResponseCases() {
        return Stream.of(
                Arguments.of(
                        "PermissionGroupNotFound",
                        (Function<
                                        PermissionGlobalExceptionHandler,
                                        ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handlePermissionGroupNotFound(
                                                new PermissionGroupNotFoundException(5L)),
                        HttpStatus.NOT_FOUND,
                        "Permission group not found with id: 5"),
                Arguments.of(
                        "Conflict",
                        (Function<
                                        PermissionGlobalExceptionHandler,
                                        ResponseEntity<Map<String, String>>>)
                                h -> h.handleConflict(new ConflictException("already in standard")),
                        HttpStatus.CONFLICT,
                        "already in standard"),
                Arguments.of(
                        "IllegalArgument",
                        (Function<
                                        PermissionGlobalExceptionHandler,
                                        ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleIllegalArgumentException(
                                                ExceptionOriginTestSupport
                                                        .applicationIllegalArgument("bad input")),
                        HttpStatus.BAD_REQUEST,
                        "bad input"),
                Arguments.of(
                        "IllegalState",
                        (Function<
                                        PermissionGlobalExceptionHandler,
                                        ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleIllegalStateException(
                                                new IllegalStateException("invalid state")),
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ClientErrorMessageSupport.GENERIC_UNEXPECTED));
    }

    @BeforeEach
    void setUp() {
        handler = new PermissionGlobalExceptionHandler();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errorResponseCases")
    void returnsExpectedStatusAndErrorBody(
            String description,
            Function<PermissionGlobalExceptionHandler, ResponseEntity<Map<String, String>>>
                    handlerCall,
            HttpStatus expectedStatus,
            String expectedError) {
        ResponseEntity<Map<String, String>> response = handlerCall.apply(handler);

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).containsEntry("error", expectedError);
    }

    @Test
    void validationExceptionReturnsFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "group");
        bindingResult.addError(new FieldError("group", "name", "must not be blank"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, String>> response =
                handler.handleValidationExceptions(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("name", "must not be blank");
        assertThat(response.getBody()).doesNotContainKey("error");
    }
}
