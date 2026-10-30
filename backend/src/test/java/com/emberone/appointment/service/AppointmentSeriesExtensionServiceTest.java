/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.emberone.appointment.domain.*;
import com.emberone.appointment.dto.CreateAppointmentRequest;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.appointment.repository.AppointmentSeriesRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentSeriesExtensionServiceTest {

    @Mock AppointmentSeriesRepository seriesRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock AppointmentRecurrenceService recurrenceService;

    @Test
    void seriesWithoutOccurrenceIsNotExtended() {
        AppointmentSeriesExtensionService service = service();
        AppointmentSeries series = series();
        when(appointmentRepository.findLatestOccurrenceStartBySeriesId(3L))
                .thenReturn(Optional.empty());

        assertThat(service.extendSeriesIfNeeded(series)).isFalse();
        verify(recurrenceService, never()).materializeOccurrences(any(), any(), any());
    }

    @Test
    void addsOnlyOccurrencesAfterLatestAnchorUsingExistingDuration() {
        AppointmentSeriesExtensionService service = service();
        AppointmentSeries series = series();
        Instant latest = LocalDateTime.of(2026, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant horizon = LocalDateTime.of(2027, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant next = LocalDateTime.of(2026, 6, 8, 18, 0).toInstant(ZoneOffset.UTC);
        when(appointmentRepository.findLatestOccurrenceStartBySeriesId(3L))
                .thenReturn(Optional.of(latest));
        when(recurrenceService.resolveMaterializationEnd(
                        any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(horizon);
        when(recurrenceService.expandOccurrenceStarts(
                        "FREQ=WEEKLY", series.getSeriesStart(), horizon, "Europe/Berlin"))
                .thenReturn(List.of(latest, next));
        when(appointmentRepository.findBySeries_Id(3L))
                .thenReturn(
                        List.of(
                                Appointment.builder()
                                        .start(latest)
                                        .end(latest.plus(Duration.ofHours(2)))
                                        .isException(false)
                                        .build()));

        assertThat(service.extendSeriesIfNeeded(series)).isTrue();

        var captor = ArgumentCaptor.forClass(CreateAppointmentRequest.class);
        verify(recurrenceService)
                .materializeOccurrences(eq(series), captor.capture(), eq(List.of(next)));
        assertThat(captor.getValue().getEnd())
                .isEqualTo(captor.getValue().getStart().plus(Duration.ofHours(2)));
    }

    @Test
    void extendActiveSeriesExtendsOnlySeriesThatNeedMoreOccurrences() {
        AppointmentSeriesExtensionService service = service();
        AppointmentSeries extendable = series();
        AppointmentSeries upToDate = series().toBuilder().id(4L).build();
        Instant latest = LocalDateTime.of(2026, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant horizon = LocalDateTime.of(2027, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant next = LocalDateTime.of(2026, 6, 8, 18, 0).toInstant(ZoneOffset.UTC);

        when(seriesRepository.findActiveSeries(any())).thenReturn(List.of(extendable, upToDate));
        when(appointmentRepository.findLatestOccurrenceStartBySeriesId(3L))
                .thenReturn(Optional.of(latest));
        when(appointmentRepository.findLatestOccurrenceStartBySeriesId(4L))
                .thenReturn(Optional.of(horizon));
        when(recurrenceService.resolveMaterializationEnd(any(), isNull())).thenReturn(horizon);
        when(recurrenceService.expandOccurrenceStarts(
                        "FREQ=WEEKLY", extendable.getSeriesStart(), horizon, "Europe/Berlin"))
                .thenReturn(List.of(latest, next));
        when(appointmentRepository.findBySeries_Id(3L))
                .thenReturn(
                        List.of(
                                Appointment.builder()
                                        .start(latest)
                                        .end(latest.plus(Duration.ofHours(2)))
                                        .isException(false)
                                        .build()));

        service.extendActiveSeries();

        verify(recurrenceService)
                .materializeOccurrences(
                        eq(extendable), any(CreateAppointmentRequest.class), eq(List.of(next)));
        verify(recurrenceService, never()).materializeOccurrences(eq(upToDate), any(), any());
    }

    @Test
    void extendActiveSeriesDoesNothingWhenNoSeriesQualify() {
        AppointmentSeriesExtensionService service = service();
        AppointmentSeries series = series();
        when(seriesRepository.findActiveSeries(any())).thenReturn(List.of(series));
        when(appointmentRepository.findLatestOccurrenceStartBySeriesId(3L))
                .thenReturn(Optional.empty());

        service.extendActiveSeries();

        verify(recurrenceService, never()).materializeOccurrences(any(), any(), any());
    }

    @Test
    void doesNotExtendWhenLatestAnchorAlreadyReachesHorizon() {
        AppointmentSeriesExtensionService service = service();
        AppointmentSeries series = series();
        Instant latest = LocalDateTime.of(2027, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant horizon = LocalDateTime.of(2027, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        when(appointmentRepository.findLatestOccurrenceStartBySeriesId(3L))
                .thenReturn(Optional.of(latest));
        when(recurrenceService.resolveMaterializationEnd(any(), isNull())).thenReturn(horizon);

        assertThat(service.extendSeriesIfNeeded(series)).isFalse();

        verify(recurrenceService, never()).expandOccurrenceStarts(any(), any(), any(), any());
        verify(recurrenceService, never()).materializeOccurrences(any(), any(), any());
    }

    @Test
    void doesNotExtendWhenRecurrenceProducesNoStartsAfterLatestAnchor() {
        AppointmentSeriesExtensionService service = service();
        AppointmentSeries series = series();
        Instant latest = LocalDateTime.of(2026, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant horizon = LocalDateTime.of(2027, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        when(appointmentRepository.findLatestOccurrenceStartBySeriesId(3L))
                .thenReturn(Optional.of(latest));
        when(recurrenceService.resolveMaterializationEnd(any(), isNull())).thenReturn(horizon);
        when(recurrenceService.expandOccurrenceStarts(
                        "FREQ=WEEKLY", series.getSeriesStart(), horizon, "Europe/Berlin"))
                .thenReturn(List.of(latest));

        assertThat(service.extendSeriesIfNeeded(series)).isFalse();

        verify(recurrenceService, never()).materializeOccurrences(any(), any(), any());
    }

    @Test
    void usesSeriesEndWhenResolvingHorizon() {
        AppointmentSeriesExtensionService service = service();
        Instant seriesEnd = LocalDateTime.of(2026, 12, 31, 18, 0).toInstant(ZoneOffset.UTC);
        AppointmentSeries series = series().toBuilder().seriesEnd(seriesEnd).build();
        Instant latest = LocalDateTime.of(2026, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        when(appointmentRepository.findLatestOccurrenceStartBySeriesId(3L))
                .thenReturn(Optional.of(latest));
        when(recurrenceService.resolveMaterializationEnd(any(), eq(seriesEnd)))
                .thenReturn(seriesEnd);
        when(recurrenceService.expandOccurrenceStarts(any(), any(), eq(seriesEnd), any()))
                .thenReturn(List.of(latest));

        assertThat(service.extendSeriesIfNeeded(series)).isFalse();

        verify(recurrenceService).resolveMaterializationEnd(any(), eq(seriesEnd));
    }

    @Test
    void buildsTemplateFromSeriesStartWhenNoOccurrencesExist() {
        AppointmentSeriesExtensionService service = service();
        AppointmentSeries series = series();
        Instant latest = LocalDateTime.of(2026, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant horizon = LocalDateTime.of(2027, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant next = LocalDateTime.of(2026, 6, 8, 18, 0).toInstant(ZoneOffset.UTC);
        when(appointmentRepository.findLatestOccurrenceStartBySeriesId(3L))
                .thenReturn(Optional.of(latest));
        when(recurrenceService.resolveMaterializationEnd(any(), isNull())).thenReturn(horizon);
        when(recurrenceService.expandOccurrenceStarts(any(), any(), any(), any()))
                .thenReturn(List.of(latest, next));
        when(appointmentRepository.findBySeries_Id(3L)).thenReturn(List.of());

        assertThat(service.extendSeriesIfNeeded(series)).isTrue();

        ArgumentCaptor<CreateAppointmentRequest> captor =
                ArgumentCaptor.forClass(CreateAppointmentRequest.class);
        verify(recurrenceService)
                .materializeOccurrences(eq(series), captor.capture(), eq(List.of(next)));
        assertThat(captor.getValue().getStart()).isEqualTo(series.getSeriesStart());
        assertThat(captor.getValue().getEnd())
                .isEqualTo(series.getSeriesStart().plus(Duration.ofHours(1)));
        assertThat(captor.getValue().getName()).isEqualTo("Training");
    }

    @Test
    void buildsTemplateFromExceptionOccurrenceWhenNoRegularOccurrencesExist() {
        AppointmentSeriesExtensionService service = service();
        AppointmentSeries series = series();
        Instant latest = LocalDateTime.of(2026, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant horizon = LocalDateTime.of(2027, 6, 1, 18, 0).toInstant(ZoneOffset.UTC);
        Instant next = LocalDateTime.of(2026, 6, 8, 18, 0).toInstant(ZoneOffset.UTC);
        Instant exceptionStart = LocalDateTime.of(2026, 5, 25, 10, 0).toInstant(ZoneOffset.UTC);
        when(appointmentRepository.findLatestOccurrenceStartBySeriesId(3L))
                .thenReturn(Optional.of(latest));
        when(recurrenceService.resolveMaterializationEnd(any(), isNull())).thenReturn(horizon);
        when(recurrenceService.expandOccurrenceStarts(any(), any(), any(), any()))
                .thenReturn(List.of(latest, next));
        when(appointmentRepository.findBySeries_Id(3L))
                .thenReturn(
                        List.of(
                                Appointment.builder()
                                        .start(exceptionStart)
                                        .end(exceptionStart.plus(Duration.ofMinutes(90)))
                                        .isException(true)
                                        .build()));

        assertThat(service.extendSeriesIfNeeded(series)).isTrue();

        ArgumentCaptor<CreateAppointmentRequest> captor =
                ArgumentCaptor.forClass(CreateAppointmentRequest.class);
        verify(recurrenceService)
                .materializeOccurrences(eq(series), captor.capture(), eq(List.of(next)));
        assertThat(captor.getValue().getStart()).isEqualTo(exceptionStart);
        assertThat(captor.getValue().getEnd())
                .isEqualTo(exceptionStart.plus(Duration.ofMinutes(90)));
    }

    private AppointmentSeriesExtensionService service() {
        return new AppointmentSeriesExtensionService(
                seriesRepository, appointmentRepository, recurrenceService);
    }

    private AppointmentSeries series() {
        return AppointmentSeries.builder()
                .id(3L)
                .seriesStart(LocalDateTime.of(2026, 1, 1, 18, 0).toInstant(ZoneOffset.UTC))
                .recurrenceRule("FREQ=WEEKLY")
                .timezone("Europe/Berlin")
                .name("Training")
                .type(AppointmentType.SERVICE)
                .responseMode(AppointmentResponseMode.NONE)
                .audienceScope(AudienceScope.ALL)
                .build();
    }
}
