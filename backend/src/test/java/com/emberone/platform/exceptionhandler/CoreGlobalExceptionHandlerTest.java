/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.exceptionhandler;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.absence.exception.AbsenceValidationException;
import com.emberone.absence.exception.UserAbsenceNotFoundException;
import com.emberone.appointment.exception.AppointmentAccessDeniedException;
import com.emberone.appointment.exception.AppointmentNotFoundException;
import com.emberone.appointment.exception.AppointmentValidationException;
import com.emberone.emergencycontact.exception.EmergencyContactNotFoundException;
import com.emberone.emergencycontact.exception.InvalidEmergencyContactOrderException;
import com.emberone.event.exception.EventAttendanceNotFoundException;
import com.emberone.event.exception.EventNotFoundException;
import com.emberone.event.exception.EventValidationException;
import com.emberone.identity.exception.IdentityException;
import com.emberone.organization.exception.OrganizationCodeAlreadyExistsException;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organizationgroup.exception.OrganizationGroupNotFoundException;
import com.emberone.organizationgroup.exception.SystemGroupModificationException;
import com.emberone.testsupport.ExceptionOriginTestSupport;
import com.emberone.timeentry.exception.TimeEntryValidationException;
import com.emberone.timeentry.exception.UserTimeEntryNotFoundException;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.userorganizationgroupmembership.exception.UserOrganizationGroupMembershipAlreadyExistsException;
import com.emberone.userorganizationgroupmembership.exception.UserOrganizationGroupMembershipNotFoundException;
import com.emberone.userorganizationmembership.exception.UserOrganizationMembershipAlreadyExistsException;
import com.emberone.userorganizationmembership.exception.UserOrganizationMembershipNotFoundException;
import java.io.IOException;
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
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

class CoreGlobalExceptionHandlerTest {

    private CoreGlobalExceptionHandler handler;

    static Stream<Arguments> errorResponseCases() {
        return Stream.of(
                Arguments.of(
                        "OrganizationNotFound",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleResourceNotFound(
                                                new OrganizationNotFoundException(42L)),
                        HttpStatus.NOT_FOUND,
                        "Organization not found with id: 42"),
                Arguments.of(
                        "OrganizationCodeAlreadyExists",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleConflict(
                                                new OrganizationCodeAlreadyExistsException(
                                                        "FD-01")),
                        HttpStatus.CONFLICT,
                        "Organization code already exists: FD-01"),
                Arguments.of(
                        "OrganizationGroupNotFound",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleResourceNotFound(
                                                new OrganizationGroupNotFoundException(3L)),
                        HttpStatus.NOT_FOUND,
                        "Organization group not found with id: 3"),
                Arguments.of(
                        "SystemGroupModification",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleAccessDenied(
                                                new SystemGroupModificationException(
                                                        "system group locked")),
                        HttpStatus.FORBIDDEN,
                        "system group locked"),
                Arguments.of(
                        "UserNotFound",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h -> h.handleResourceNotFound(new UserNotFoundException(7L)),
                        HttpStatus.NOT_FOUND,
                        "User not found with id: 7"),
                Arguments.of(
                        "EmergencyContactNotFound",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleResourceNotFound(
                                                new EmergencyContactNotFoundException(11L)),
                        HttpStatus.NOT_FOUND,
                        "Emergency contact not found: 11"),
                Arguments.of(
                        "InvalidEmergencyContactOrder",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleValidation(
                                                new InvalidEmergencyContactOrderException(
                                                        "invalid order")),
                        HttpStatus.BAD_REQUEST,
                        "invalid order"),
                Arguments.of(
                        "UserAbsenceNotFound",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h -> h.handleResourceNotFound(new UserAbsenceNotFoundException(5L)),
                        HttpStatus.NOT_FOUND,
                        "Absence not found: 5"),
                Arguments.of(
                        "AbsenceValidation",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleValidation(
                                                new AbsenceValidationException("invalid absence")),
                        HttpStatus.BAD_REQUEST,
                        "invalid absence"),
                Arguments.of(
                        "UserTimeEntryNotFound",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleResourceNotFound(
                                                new UserTimeEntryNotFoundException(12L)),
                        HttpStatus.NOT_FOUND,
                        "Time entry not found: 12"),
                Arguments.of(
                        "TimeEntryValidation",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleValidation(
                                                new TimeEntryValidationException("invalid entry")),
                        HttpStatus.BAD_REQUEST,
                        "invalid entry"),
                Arguments.of(
                        "UserOrganizationMembershipNotFound",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleResourceNotFound(
                                                new UserOrganizationMembershipNotFoundException(
                                                        20L)),
                        HttpStatus.NOT_FOUND,
                        "User organization membership not found with id: 20"),
                Arguments.of(
                        "UserOrganizationMembershipAlreadyExists",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleConflict(
                                                new UserOrganizationMembershipAlreadyExistsException(
                                                        1L, 2L)),
                        HttpStatus.CONFLICT,
                        "User 1 is already a member of organization 2"),
                Arguments.of(
                        "UserOrganizationGroupMembershipNotFound",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleResourceNotFound(
                                                new UserOrganizationGroupMembershipNotFoundException(
                                                        30L)),
                        HttpStatus.NOT_FOUND,
                        "User organization group membership not found with id: 30"),
                Arguments.of(
                        "UserOrganizationGroupMembershipAlreadyExists",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleConflict(
                                                new UserOrganizationGroupMembershipAlreadyExistsException(
                                                        8L, 9L)),
                        HttpStatus.CONFLICT,
                        "User 8 is already a member of organization group 9"),
                Arguments.of(
                        "AppointmentNotFound",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h -> h.handleResourceNotFound(new AppointmentNotFoundException(1L)),
                        HttpStatus.NOT_FOUND,
                        "Appointment not found: 1"),
                Arguments.of(
                        "AppointmentAccessDenied",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleAccessDenied(
                                                new AppointmentAccessDeniedException("denied")),
                        HttpStatus.FORBIDDEN,
                        "denied"),
                Arguments.of(
                        "AppointmentValidation",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleValidation(
                                                new AppointmentValidationException(
                                                        "invalid appointment")),
                        HttpStatus.BAD_REQUEST,
                        "invalid appointment"),
                Arguments.of(
                        "EventNotFound",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleResourceNotFound(
                                                new EventNotFoundException("event missing")),
                        HttpStatus.NOT_FOUND,
                        "event missing"),
                Arguments.of(
                        "EventAttendanceNotFound",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleResourceNotFound(
                                                new EventAttendanceNotFoundException(
                                                        "attendance missing")),
                        HttpStatus.NOT_FOUND,
                        "attendance missing"),
                Arguments.of(
                        "EventValidation",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleValidation(
                                                new EventValidationException("invalid event")),
                        HttpStatus.BAD_REQUEST,
                        "invalid event"),
                Arguments.of(
                        "IdentityException",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleIdentityException(
                                                new IdentityException("Authentik unavailable")),
                        HttpStatus.BAD_GATEWAY,
                        ClientErrorMessageSupport.GENERIC_IDENTITY_UNAVAILABLE),
                Arguments.of(
                        "IllegalArgument",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleIllegalArgumentException(
                                                ExceptionOriginTestSupport
                                                        .applicationIllegalArgument("bad input")),
                        HttpStatus.BAD_REQUEST,
                        "bad input"),
                Arguments.of(
                        "IllegalState",
                        (Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>>)
                                h ->
                                        h.handleIllegalStateException(
                                                new IllegalStateException("invalid state")),
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ClientErrorMessageSupport.GENERIC_UNEXPECTED));
    }

    @BeforeEach
    void setUp() {
        handler = new CoreGlobalExceptionHandler();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errorResponseCases")
    void returnsExpectedStatusAndErrorBody(
            String description,
            Function<CoreGlobalExceptionHandler, ResponseEntity<Map<String, String>>> handlerCall,
            HttpStatus expectedStatus,
            String expectedError) {
        ResponseEntity<Map<String, String>> response = handlerCall.apply(handler);

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).containsEntry("error", expectedError);
    }

    @Test
    void validationExceptionReturnsFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, String>> response =
                handler.handleValidationExceptions(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("email", "must not be blank");
        assertThat(response.getBody()).doesNotContainKey("error");
    }

    @Test
    void genericExceptionReturnsSanitizedMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleGenericException(new RuntimeException("database password leaked"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "An unexpected error occurred");
        assertThat(response.getBody().get("error")).doesNotContain("password");
    }

    @Test
    void clientDisconnectDoesNotReturnErrorBody() {
        AsyncRequestNotUsableException exception =
                new AsyncRequestNotUsableException(
                        "ServletOutputStream failed to write: java.io.IOException: Broken pipe",
                        new IOException("Broken pipe"));

        assertThat(handler.handleGenericException(exception)).isNull();
    }

    @Test
    void asyncRequestNotUsableFromClientDisconnectIsHandledWithoutResponse() {
        AsyncRequestNotUsableException exception =
                new AsyncRequestNotUsableException(
                        "ServletOutputStream failed to write: java.io.IOException: Broken pipe",
                        new IOException("Broken pipe"));

        handler.handleAsyncRequestNotUsable(exception);
    }
}
