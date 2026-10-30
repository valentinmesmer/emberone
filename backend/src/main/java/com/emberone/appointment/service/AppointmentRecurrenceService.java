/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AppointmentSeries;
import com.emberone.appointment.dto.CreateAppointmentRequest;
import com.emberone.appointment.dto.RecurrenceRequest;
import com.emberone.appointment.exception.AppointmentValidationException;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.appointment.util.AppointmentTemporalUtil;
import com.emberone.organization.domain.Organization;
import com.emberone.platform.time.OrganizationWallClockTime;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.Recur;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentRecurrenceService {

    public static final int SERIES_HORIZON_MONTHS = 12;
    private static final int MAX_OCCURRENCES = 500;

    private final AppointmentRepository appointmentRepository;

    public Instant resolveMaterializationEnd(Instant horizonAnchor, Instant userSeriesEnd) {
        Instant horizonEnd =
                horizonAnchor.atZone(ZoneOffset.UTC).plusMonths(SERIES_HORIZON_MONTHS).toInstant();
        if (userSeriesEnd != null && userSeriesEnd.isBefore(horizonEnd)) {
            return userSeriesEnd;
        }
        return horizonEnd;
    }

    public List<Instant> expandOccurrenceStarts(
            String recurrenceRule, Instant seriesStart, Instant seriesEnd, String timezone) {
        try {
            Instant normalizedStart = AppointmentTemporalUtil.truncateToMinutes(seriesStart);
            Instant normalizedEnd =
                    AppointmentTemporalUtil.truncateToMinutes(
                            Objects.requireNonNull(
                                    seriesEnd,
                                    "seriesEnd is required to bound recurrence expansion"));

            ZoneId zoneId = resolveZoneId(timezone);
            Recur<ZonedDateTime> recur = new Recur<>(recurrenceRule);
            ZonedDateTime seed = normalizedStart.atZone(zoneId);
            ZonedDateTime periodStart = seed;
            ZonedDateTime periodEnd = normalizedEnd.atZone(zoneId);

            List<ZonedDateTime> dateList =
                    recur.getDates(seed, periodStart, periodEnd, MAX_OCCURRENCES);
            List<Instant> results = new ArrayList<>();
            for (ZonedDateTime zonedDateTime : dateList) {
                results.add(AppointmentTemporalUtil.truncateToMinutes(zonedDateTime.toInstant()));
                if (results.size() >= MAX_OCCURRENCES) {
                    log.warn("Recurrence expansion capped at {} occurrences", MAX_OCCURRENCES);
                    break;
                }
            }
            if (results.isEmpty()) {
                results.add(normalizedStart);
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to expand recurrence rule: {}", recurrenceRule, e);
            throw new AppointmentValidationException("Invalid recurrence rule: " + e.getMessage());
        }
    }

    public AppointmentSeries createSeries(
            Organization organization,
            CreateAppointmentRequest request,
            RecurrenceRequest recurrence) {
        AppointmentSeries series =
                AppointmentSeries.builder()
                        .tenantId(organization.getTenantId())
                        .organization(organization)
                        .recurrenceRule(recurrence.getRecurrenceRule())
                        .seriesStart(AppointmentTemporalUtil.truncateToMinutes(request.getStart()))
                        .seriesEnd(
                                recurrence.getSeriesEnd() != null
                                        ? AppointmentTemporalUtil.truncateToMinutes(
                                                recurrence.getSeriesEnd())
                                        : null)
                        .timezone(resolveTimezone(recurrence.getTimezone()))
                        .name(request.getName())
                        .type(request.getType())
                        .location(request.getLocation())
                        .description(request.getDescription())
                        .responseMode(request.getResponseMode())
                        .responseDeadlineMinutesBefore(
                                AppointmentResponseDeadlineUtil.normalizeMinutes(
                                        request.getResponseMode(),
                                        request.getResponseDeadlineMinutesBefore()))
                        .audienceScope(request.getAudienceScope())
                        .build();
        series.replaceAudienceGroupIds(request.getAudienceGroupIds());
        return series;
    }

    public List<Appointment> materializeOccurrences(
            AppointmentSeries series,
            CreateAppointmentRequest request,
            List<Instant> occurrenceStarts) {
        Duration duration = Duration.between(request.getStart(), request.getEnd());
        List<Appointment> appointments = new ArrayList<>();

        for (Instant occurrenceStart : occurrenceStarts) {
            Instant occurrenceEnd = occurrenceStart.plus(duration);
            Appointment appointment =
                    Appointment.builder()
                            .tenantId(series.getTenantId())
                            .organization(series.getOrganization())
                            .series(series)
                            .occurrenceStart(occurrenceStart)
                            .name(request.getName())
                            .type(request.getType())
                            .start(occurrenceStart)
                            .end(occurrenceEnd)
                            .location(request.getLocation())
                            .description(request.getDescription())
                            .responseMode(request.getResponseMode())
                            .responseDeadlineMinutesBefore(
                                    AppointmentResponseDeadlineUtil.normalizeMinutes(
                                            request.getResponseMode(),
                                            request.getResponseDeadlineMinutesBefore()))
                            .audienceScope(request.getAudienceScope())
                            .archived(false)
                            .build();
            appointment.replaceAudienceGroupIds(request.getAudienceGroupIds());
            appointments.add(appointment);
        }

        return appointmentRepository.saveAll(appointments);
    }

    private String resolveTimezone(String requestedTimezone) {
        if (requestedTimezone == null || requestedTimezone.isBlank()) {
            return OrganizationWallClockTime.organizationZone().getId();
        }
        return requestedTimezone.trim();
    }

    private ZoneId resolveZoneId(String timezone) {
        return ZoneId.of(resolveTimezone(timezone));
    }
}
