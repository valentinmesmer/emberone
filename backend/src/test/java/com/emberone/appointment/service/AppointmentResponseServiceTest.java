/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AppointmentResponse;
import com.emberone.appointment.domain.AppointmentResponseMode;
import com.emberone.appointment.domain.AppointmentResponseValue;
import com.emberone.appointment.dto.AppointmentParticipantEntryDto;
import com.emberone.appointment.dto.MyAppointmentResponseDto;
import com.emberone.appointment.dto.SubmitAppointmentResponseRequest;
import com.emberone.appointment.event.AppointmentChangeType;
import com.emberone.appointment.event.AppointmentChangedEvent;
import com.emberone.appointment.exception.AppointmentValidationException;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.appointment.repository.AppointmentResponseRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.platform.security.TokenService;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AppointmentResponseServiceTest {

    @Mock AppointmentResponseRepository responseRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock AppointmentAudienceService audienceService;
    @Mock UserRepository userRepository;
    @Mock TokenService tokenService;
    @Mock ApplicationEventPublisher eventPublisher;

    private AppointmentResponseService service;

    @BeforeEach
    void setUp() {
        service =
                new AppointmentResponseService(
                        responseRepository,
                        appointmentRepository,
                        audienceService,
                        userRepository,
                        tokenService,
                        eventPublisher);
    }

    @Test
    void submitCreatesOrUpdatesResponseAndReturnsUserName() {
        Appointment appointment = appointment(AppointmentResponseMode.YES_NO_WITH_COMMENT);
        SubmitAppointmentResponseRequest request = response(AppointmentResponseValue.YES, "Coming");
        when(tokenService.requireUserId()).thenReturn(1L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        when(appointmentRepository.findByIdAndOrganization_Id(5L, 10L))
                .thenReturn(Optional.of(appointment));
        when(responseRepository.findByAppointmentIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
        when(responseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                User.builder()
                                        .id(1L)
                                        .firstName("Ada")
                                        .lastName("Lovelace")
                                        .build()));

        var dto = service.submitResponse(5L, 10L, request);

        assertThat(dto.getValue()).isEqualTo(AppointmentResponseValue.YES);
        assertThat(dto.getComment()).isEqualTo("Coming");
        assertThat(dto.getUserLastName()).isEqualTo("Lovelace");
        verify(audienceService).ensureUserCanAccess(1L, 5L);
        verify(eventPublisher)
                .publishEvent(
                        new AppointmentChangedEvent(7L, 10L, 5L, AppointmentChangeType.UPDATED));
    }

    @Test
    void rejectsResponsesForNoneArchivedPassedDeadlineAndInvalidFields() {
        assertInvalid(
                appointment(AppointmentResponseMode.NONE),
                response(AppointmentResponseValue.YES, null),
                "not allowed");

        Appointment archived = appointment(AppointmentResponseMode.YES_NO);
        archived.setArchived(true);
        assertInvalid(archived, response(AppointmentResponseValue.YES, null), "archived");

        Appointment deadline = appointment(AppointmentResponseMode.YES_NO);
        deadline.setStart(Instant.now().plus(Duration.ofMinutes(5)));
        deadline.setResponseDeadlineMinutesBefore(10);
        assertInvalid(deadline, response(AppointmentResponseValue.YES, null), "deadline");

        assertInvalid(
                appointment(AppointmentResponseMode.YES_NO),
                response(AppointmentResponseValue.UNCERTAIN, null),
                "Uncertain");
        assertInvalid(
                appointment(AppointmentResponseMode.YES_NO),
                response(AppointmentResponseValue.YES, "comment"),
                "Comments");
    }

    @Test
    void submitUpdatesExistingResponseInsteadOfCreatingNewOne() {
        Appointment appointment = appointment(AppointmentResponseMode.YES_NO);
        AppointmentResponse existing =
                AppointmentResponse.builder()
                        .tenantId(7L)
                        .appointment(appointment)
                        .userId(1L)
                        .value(AppointmentResponseValue.NO)
                        .comment("Old")
                        .build();
        SubmitAppointmentResponseRequest request = response(AppointmentResponseValue.YES, null);
        when(tokenService.requireUserId()).thenReturn(1L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        when(appointmentRepository.findByIdAndOrganization_Id(5L, 10L))
                .thenReturn(Optional.of(appointment));
        when(responseRepository.findByAppointmentIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(existing));
        when(responseRepository.save(existing)).thenReturn(existing);
        when(userRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                User.builder()
                                        .id(1L)
                                        .firstName("Ada")
                                        .lastName("Lovelace")
                                        .build()));

        var dto = service.submitResponse(5L, 10L, request);

        assertThat(existing.getValue()).isEqualTo(AppointmentResponseValue.YES);
        assertThat(dto.getValue()).isEqualTo(AppointmentResponseValue.YES);
        verify(responseRepository).save(existing);
    }

    @Test
    void getMyResponsesReturnsSortedAppointmentResponsesForCurrentUser() {
        Appointment appointmentA = Appointment.builder().id(5L).build();
        Appointment appointmentB = Appointment.builder().id(6L).build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(responseRepository.findByUserIdAndAppointmentIdIn(1L, List.of(6L, 5L)))
                .thenReturn(
                        List.of(
                                AppointmentResponse.builder()
                                        .appointment(appointmentB)
                                        .value(AppointmentResponseValue.YES)
                                        .build(),
                                AppointmentResponse.builder()
                                        .appointment(appointmentA)
                                        .value(AppointmentResponseValue.NO)
                                        .build()));

        assertThat(service.getMyResponses(List.of(6L, 5L)))
                .extracting(MyAppointmentResponseDto::getAppointmentId)
                .containsExactly(5L, 6L);
        assertThat(service.getMyResponses(List.of())).isEmpty();
    }

    @Test
    void participationStatsIgnoreResponsesOutsideAudience() {
        Appointment appointment = appointment(AppointmentResponseMode.YES_NO_UNCERTAIN);
        when(tokenService.getUserId()).thenReturn(9L);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(audienceService.resolveAudienceUserIds(appointment)).thenReturn(Set.of(1L, 2L));
        when(responseRepository.findByAppointmentId(5L))
                .thenReturn(
                        List.of(
                                AppointmentResponse.builder()
                                        .userId(1L)
                                        .value(AppointmentResponseValue.YES)
                                        .build(),
                                AppointmentResponse.builder()
                                        .userId(2L)
                                        .value(AppointmentResponseValue.NO)
                                        .build(),
                                AppointmentResponse.builder()
                                        .userId(99L)
                                        .value(AppointmentResponseValue.YES)
                                        .build()));
        when(userRepository.findAllById(any()))
                .thenReturn(
                        List.of(
                                User.builder().id(1L).firstName("A").lastName("A").build(),
                                User.builder().id(2L).firstName("B").lastName("B").build()));

        var stats = service.getParticipationStats(5L, 10L);

        assertThat(stats.getYesCount()).isEqualTo(1);
        assertThat(stats.getNoCount()).isEqualTo(1);
        assertThat(stats.getUncertainCount()).isZero();
    }

    @Test
    void participationStatsSortParticipantsByName() {
        Appointment appointment = appointment(AppointmentResponseMode.YES_NO_UNCERTAIN);
        when(tokenService.getUserId()).thenReturn(9L);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(audienceService.resolveAudienceUserIds(appointment)).thenReturn(Set.of(1L, 2L, 3L));
        when(responseRepository.findByAppointmentId(5L))
                .thenReturn(
                        List.of(
                                AppointmentResponse.builder()
                                        .userId(1L)
                                        .value(AppointmentResponseValue.YES)
                                        .build(),
                                AppointmentResponse.builder()
                                        .userId(2L)
                                        .value(AppointmentResponseValue.NO)
                                        .build(),
                                AppointmentResponse.builder()
                                        .userId(3L)
                                        .value(AppointmentResponseValue.UNCERTAIN)
                                        .build()));
        when(userRepository.findAllById(any()))
                .thenReturn(
                        List.of(
                                User.builder().id(1L).firstName("Zoe").lastName("Beta").build(),
                                User.builder().id(2L).firstName("Amy").lastName("Alpha").build(),
                                User.builder().id(3L).firstName("Bob").lastName("Gamma").build()));

        var stats = service.getParticipationStats(5L, 10L);

        assertThat(stats.getYesParticipants())
                .extracting(AppointmentParticipantEntryDto::getUserLastName)
                .containsExactly("Beta");
        assertThat(stats.getNoParticipants())
                .extracting(AppointmentParticipantEntryDto::getUserLastName)
                .containsExactly("Alpha");
        assertThat(stats.getUncertainParticipants())
                .extracting(AppointmentParticipantEntryDto::getUserLastName)
                .containsExactly("Gamma");
    }

    private void assertInvalid(
            Appointment appointment, SubmitAppointmentResponseRequest request, String message) {
        when(tokenService.requireUserId()).thenReturn(1L);
        when(tokenService.requireTenantId()).thenReturn(7L);
        when(appointmentRepository.findByIdAndOrganization_Id(5L, 10L))
                .thenReturn(Optional.of(appointment));
        assertThatThrownBy(() -> service.submitResponse(5L, 10L, request))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining(message);
    }

    private Appointment appointment(AppointmentResponseMode mode) {
        return Appointment.builder()
                .id(5L)
                .tenantId(7L)
                .responseMode(mode)
                .organization(Organization.builder().id(10L).build())
                .start(Instant.now().plus(Duration.ofDays(2)))
                .responseDeadlineMinutesBefore(0)
                .archived(false)
                .build();
    }

    private SubmitAppointmentResponseRequest response(
            AppointmentResponseValue value, String comment) {
        SubmitAppointmentResponseRequest request = new SubmitAppointmentResponseRequest();
        request.setValue(value);
        request.setComment(comment);
        return request;
    }
}
