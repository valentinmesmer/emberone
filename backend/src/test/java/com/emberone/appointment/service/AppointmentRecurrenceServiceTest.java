/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.emberone.appointment.domain.*;
import com.emberone.appointment.dto.CreateAppointmentRequest;
import com.emberone.appointment.dto.RecurrenceRequest;
import com.emberone.appointment.exception.AppointmentValidationException;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.organization.domain.Organization;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentRecurrenceServiceTest {

    @Mock AppointmentRepository appointmentRepository;

    private AppointmentRecurrenceService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentRecurrenceService(appointmentRepository);
    }

    @Test
    void materializationEndUsesEarlierUserEndOrTwelveMonthHorizon() {
        Instant anchor = LocalDateTime.of(2026, 1, 15, 9, 0).toInstant(ZoneOffset.UTC);

        assertThat(
                        service.resolveMaterializationEnd(
                                anchor, anchor.atZone(ZoneOffset.UTC).plusMonths(3).toInstant()))
                .isEqualTo(anchor.atZone(ZoneOffset.UTC).plusMonths(3).toInstant());
        assertThat(
                        service.resolveMaterializationEnd(
                                anchor, anchor.atZone(ZoneOffset.UTC).plusMonths(18).toInstant()))
                .isEqualTo(anchor.atZone(ZoneOffset.UTC).plusMonths(12).toInstant());
        assertThat(service.resolveMaterializationEnd(anchor, null))
                .isEqualTo(anchor.atZone(ZoneOffset.UTC).plusMonths(12).toInstant());
    }

    @Test
    void expandsWeeklyRuleAndTruncatesSeconds() {
        List<Instant> starts =
                service.expandOccurrenceStarts(
                        "FREQ=WEEKLY;COUNT=3",
                        LocalDateTime.of(2026, 1, 5, 18, 30, 45).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 2, 1, 0, 0).toInstant(ZoneOffset.UTC),
                        "Europe/Berlin");

        assertThat(starts)
                .containsExactly(
                        LocalDateTime.of(2026, 1, 5, 18, 30).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 1, 12, 18, 30).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 1, 19, 18, 30).toInstant(ZoneOffset.UTC));
    }

    @Test
    void keepsLocalWallClockTimeAcrossDaylightSavingChange() {
        ZoneId berlin = ZoneId.of("Europe/Berlin");
        List<Instant> starts =
                service.expandOccurrenceStarts(
                        "FREQ=WEEKLY;COUNT=3",
                        LocalDateTime.of(2026, 3, 22, 9, 0).atZone(berlin).toInstant(),
                        LocalDateTime.of(2026, 4, 10, 0, 0).atZone(berlin).toInstant(),
                        "Europe/Berlin");

        assertThat(starts)
                .containsExactly(
                        LocalDateTime.of(2026, 3, 22, 9, 0).atZone(berlin).toInstant(),
                        LocalDateTime.of(2026, 3, 29, 9, 0).atZone(berlin).toInstant(),
                        LocalDateTime.of(2026, 4, 5, 9, 0).atZone(berlin).toInstant());
    }

    @Test
    void capsLargeExpansionAtFiveHundredOccurrences() {
        List<Instant> starts =
                service.expandOccurrenceStarts(
                        "FREQ=DAILY;COUNT=1000",
                        LocalDateTime.of(2026, 1, 1, 8, 0).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2030, 1, 1, 8, 0).toInstant(ZoneOffset.UTC),
                        null);

        assertThat(starts).hasSize(500);
    }

    @Test
    void invalidRuleOrTimezoneBecomesValidationException() {
        assertThatThrownBy(
                        () ->
                                service.expandOccurrenceStarts(
                                        "NOT_A_RULE",
                                        LocalDateTime.of(2026, 1, 1, 8, 0)
                                                .toInstant(ZoneOffset.UTC),
                                        LocalDateTime.of(2026, 2, 1, 8, 0)
                                                .toInstant(ZoneOffset.UTC),
                                        "Invalid/Timezone"))
                .isInstanceOf(AppointmentValidationException.class)
                .hasMessageContaining("Invalid recurrence rule");
    }

    @Test
    void createsNormalizedSeriesWithDefaultTimezone() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        CreateAppointmentRequest request = request();
        RecurrenceRequest recurrence = new RecurrenceRequest();
        recurrence.setRecurrenceRule("FREQ=WEEKLY");
        recurrence.setSeriesEnd(LocalDateTime.of(2026, 6, 1, 12, 0, 30).toInstant(ZoneOffset.UTC));
        recurrence.setTimezone(" ");

        AppointmentSeries series = service.createSeries(organization, request, recurrence);

        assertThat(series.getTenantId()).isEqualTo(7L);
        assertThat(series.getSeriesStart())
                .isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 15).toInstant(ZoneOffset.UTC));
        assertThat(series.getSeriesEnd())
                .isEqualTo(LocalDateTime.of(2026, 6, 1, 12, 0).toInstant(ZoneOffset.UTC));
        assertThat(series.getTimezone()).isEqualTo("Europe/Berlin");
        assertThat(series.getAudienceGroupIds()).containsExactly(20L, 21L);
    }

    @Test
    void materializesDurationAndPersistsAllOccurrences() {
        CreateAppointmentRequest request = request();
        AppointmentSeries series =
                AppointmentSeries.builder()
                        .tenantId(7L)
                        .organization(Organization.builder().id(10L).tenantId(7L).build())
                        .build();
        when(appointmentRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Appointment> result =
                service.materializeOccurrences(
                        series,
                        request,
                        List.of(
                                LocalDateTime.of(2026, 2, 1, 8, 0).toInstant(ZoneOffset.UTC),
                                LocalDateTime.of(2026, 2, 8, 8, 0).toInstant(ZoneOffset.UTC)));

        assertThat(result).hasSize(2);
        assertThat(result)
                .allSatisfy(
                        appointment -> {
                            assertThat(appointment.getSeries()).isSameAs(series);
                            assertThat(appointment.getEnd())
                                    .isEqualTo(appointment.getStart().plus(Duration.ofMinutes(90)));
                            assertThat(appointment.getAudienceGroupIds()).containsExactly(20L, 21L);
                            assertThat(appointment.isArchived()).isFalse();
                        });
    }

    private CreateAppointmentRequest request() {
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setStart(LocalDateTime.of(2026, 1, 1, 10, 15, 45).toInstant(ZoneOffset.UTC));
        request.setEnd(LocalDateTime.of(2026, 1, 1, 11, 45, 45).toInstant(ZoneOffset.UTC));
        request.setName("Training");
        request.setType(AppointmentType.SERVICE);
        request.setLocation("Station");
        request.setDescription("Description");
        request.setResponseMode(AppointmentResponseMode.YES_NO);
        request.setResponseDeadlineMinutesBefore(30);
        request.setAudienceScope(AudienceScope.SELECTED_GROUPS);
        request.setAudienceGroupIds(List.of(20L, 21L));
        return request;
    }

    @Test
    void expandsWeeklyWithUntilEndOfDay() {
        // Frontend seriesEndFromDate + formatUntilUtc for Europe/Berlin CEST (UTC+2):
        // End date 2026-08-31 -> local 23:59:59 -> UNTIL=20260831T215959Z
        List<Instant> starts =
                service.expandOccurrenceStarts(
                        "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO;UNTIL=20260831T215959Z",
                        LocalDateTime.of(2026, 7, 6, 18, 0).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 8, 31, 23, 59).toInstant(ZoneOffset.UTC),
                        "Europe/Berlin");

        assertThat(starts)
                .containsExactly(
                        LocalDateTime.of(2026, 7, 6, 18, 0).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 7, 13, 18, 0).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 7, 20, 18, 0).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 7, 27, 18, 0).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 8, 3, 18, 0).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 8, 10, 18, 0).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 8, 17, 18, 0).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 8, 24, 18, 0).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2026, 8, 31, 18, 0).toInstant(ZoneOffset.UTC));
    }

    @Test
    void expandsWeeklyWithUntilAcrossFormerInfiniteHorizon() {
        ZoneId berlin = ZoneId.of("Europe/Berlin");
        List<Instant> starts =
                service.expandOccurrenceStarts(
                        "FREQ=WEEKLY;INTERVAL=1;BYDAY=SU;UNTIL=20261231T225959Z",
                        LocalDateTime.of(2026, 8, 2, 10, 0).atZone(berlin).toInstant(),
                        LocalDateTime.of(2026, 12, 31, 23, 59).toInstant(ZoneOffset.UTC),
                        "Europe/Berlin");

        assertThat(starts).hasSize(22);
        assertThat(starts.getFirst())
                .isEqualTo(LocalDateTime.of(2026, 8, 2, 10, 0).atZone(berlin).toInstant());
        assertThat(starts.getLast())
                .isEqualTo(LocalDateTime.of(2026, 12, 27, 10, 0).atZone(berlin).toInstant());
    }
}
