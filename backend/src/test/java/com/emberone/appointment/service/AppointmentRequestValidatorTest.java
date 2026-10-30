/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AppointmentResponseMode;
import com.emberone.appointment.domain.AppointmentType;
import com.emberone.appointment.domain.AudienceScope;
import com.emberone.appointment.dto.CreateAppointmentRequest;
import com.emberone.appointment.dto.RecurrenceRequest;
import com.emberone.appointment.dto.UpdateAppointmentRequest;
import com.emberone.appointment.exception.AppointmentValidationException;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentRequestValidatorTest {

    @Mock private OrganizationGroupRepository organizationGroupRepository;

    @InjectMocks private AppointmentRequestValidator validator;

    @Test
    void normalizeAndValidateCreate_truncatesTimesTrimsTextAndClearsAudienceForAll() {
        Instant start = futureStart().truncatedTo(ChronoUnit.MINUTES).plusSeconds(30);
        CreateAppointmentRequest request =
                validCreateRequest(
                        start,
                        start.plus(Duration.ofHours(2))
                                .truncatedTo(ChronoUnit.MINUTES)
                                .plusSeconds(45));
        request.setName("  Team meeting  ");
        request.setLocation("  Room A  ");
        request.setDescription("  Notes  ");
        request.setAudienceGroupIds(List.of(99L));

        validator.normalizeAndValidateCreate(request);

        assertThat(request.getStart().atZone(ZoneOffset.UTC).getSecond()).isZero();
        assertThat(request.getEnd().atZone(ZoneOffset.UTC).getSecond()).isZero();
        assertThat(request.getName()).isEqualTo("Team meeting");
        assertThat(request.getLocation()).isEqualTo("Room A");
        assertThat(request.getDescription()).isEqualTo("Notes");
        assertThat(request.getAudienceGroupIds()).isEmpty();
    }

    @Test
    void normalizeAndValidateCreate_rejectsEndNotAfterStart() {
        Instant start = futureStart();
        CreateAppointmentRequest request = validCreateRequest(start, start);

        assertThatThrownBy(() -> validator.normalizeAndValidateCreate(request))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("End must be after start");
    }

    @Test
    void normalizeAndValidateCreate_allowsPastStartWhenEndIsStillFuture() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Instant start = now.minus(Duration.ofHours(1));
        CreateAppointmentRequest request = validCreateRequest(start, now.plus(Duration.ofHours(1)));

        validator.normalizeAndValidateCreate(request);
    }

    @Test
    void normalizeAndValidateCreate_rejectsWhenEndNotInFuture() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Instant start = now.minus(Duration.ofHours(2));
        CreateAppointmentRequest request =
                validCreateRequest(start, now.minus(Duration.ofHours(1)));

        assertThatThrownBy(() -> validator.normalizeAndValidateCreate(request))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("end must be in the future");
    }

    @Test
    void normalizeAndValidateCreate_clearsDeadlineWhenResponseModeIsNone() {
        CreateAppointmentRequest request =
                validCreateRequest(futureStart(), futureStart().plus(Duration.ofHours(1)));
        request.setResponseMode(AppointmentResponseMode.NONE);
        request.setResponseDeadlineMinutesBefore(30);

        validator.normalizeAndValidateCreate(request);

        assertThat(request.getResponseDeadlineMinutesBefore()).isZero();
    }

    @Test
    void normalizeAndValidateCreate_requiresAudienceGroupsForSelectedScope() {
        CreateAppointmentRequest request =
                validCreateRequest(futureStart(), futureStart().plus(Duration.ofHours(1)));
        request.setAudienceScope(AudienceScope.SELECTED_GROUPS);
        request.setAudienceGroupIds(List.of());

        assertThatThrownBy(() -> validator.normalizeAndValidateCreate(request))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("At least one audience group");
    }

    @Test
    void normalizeAndValidateCreate_validatesAudienceGroupsBelongToOrganization() {
        when(organizationGroupRepository.findByOrganizationId(1L))
                .thenReturn(List.of(OrganizationGroup.builder().id(10L).build()));

        CreateAppointmentRequest request =
                validCreateRequest(futureStart(), futureStart().plus(Duration.ofHours(1)));
        request.setAudienceScope(AudienceScope.SELECTED_GROUPS);
        request.setAudienceGroupIds(List.of(10L, 99L));

        assertThatThrownBy(() -> validator.normalizeAndValidateCreate(request))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("do not belong to the organization");
    }

    @Test
    void normalizeAndValidateCreate_validatesRecurrenceTimezoneAndSeriesEnd() {
        Instant start = futureStart();
        CreateAppointmentRequest request =
                validCreateRequest(start, start.plus(Duration.ofHours(1)));
        RecurrenceRequest recurrence = new RecurrenceRequest();
        recurrence.setTimezone("Europe/Berlin");
        recurrence.setSeriesEnd(start.minus(Duration.ofDays(1)));
        request.setRecurrence(recurrence);

        assertThatThrownBy(() -> validator.normalizeAndValidateCreate(request))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("series end must not be before");
    }

    @Test
    void normalizeAndValidateUpdate_allowsPastAppointmentMetadataChanges() {
        Instant pastStart = Instant.now().minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.MINUTES);
        Appointment appointment =
                Appointment.builder()
                        .start(pastStart)
                        .end(pastStart.plus(Duration.ofHours(1)))
                        .responseMode(AppointmentResponseMode.NONE)
                        .responseDeadlineMinutesBefore(0)
                        .audienceScope(AudienceScope.ALL)
                        .build();

        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setName("Updated name");

        validator.normalizeAndValidateUpdate(1L, appointment, request);

        assertThat(request.getName()).isEqualTo("Updated name");
    }

    @Test
    void normalizeAndValidateUpdate_rejectsMovingFutureAppointmentIntoPast() {
        Instant futureStart = futureStart();
        Appointment appointment =
                Appointment.builder()
                        .start(futureStart)
                        .end(futureStart.plus(Duration.ofHours(1)))
                        .responseMode(AppointmentResponseMode.NONE)
                        .responseDeadlineMinutesBefore(0)
                        .audienceScope(AudienceScope.ALL)
                        .build();

        UpdateAppointmentRequest request = new UpdateAppointmentRequest();
        request.setStart(Instant.now().minus(Duration.ofDays(1)));

        assertThatThrownBy(() -> validator.normalizeAndValidateUpdate(1L, appointment, request))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("must be in the future");
    }

    private CreateAppointmentRequest validCreateRequest(Instant start, Instant end) {
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setOrganizationId(1L);
        request.setName("Meeting");
        request.setType(AppointmentType.SERVICE);
        request.setStart(start);
        request.setEnd(end);
        request.setResponseMode(AppointmentResponseMode.NONE);
        request.setAudienceScope(AudienceScope.ALL);
        return request;
    }

    private Instant futureStart() {
        return Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.MINUTES);
    }
}
