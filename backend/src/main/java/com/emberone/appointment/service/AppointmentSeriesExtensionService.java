/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AppointmentSeries;
import com.emberone.appointment.dto.CreateAppointmentRequest;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.appointment.repository.AppointmentSeriesRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentSeriesExtensionService {

    private final AppointmentSeriesRepository appointmentSeriesRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentRecurrenceService recurrenceService;

    @Transactional
    public void extendActiveSeries() {
        Instant now = Instant.now();
        List<AppointmentSeries> activeSeries = appointmentSeriesRepository.findActiveSeries(now);
        int extendedCount = 0;

        for (AppointmentSeries series : activeSeries) {
            if (extendSeriesIfNeeded(series)) {
                extendedCount++;
            }
        }

        if (extendedCount > 0) {
            log.info(
                    "Extended {} appointment series to maintain {}-month horizon",
                    extendedCount,
                    AppointmentRecurrenceService.SERIES_HORIZON_MONTHS);
        }
    }

    boolean extendSeriesIfNeeded(AppointmentSeries series) {
        Optional<Instant> latestOccurrenceStart =
                appointmentRepository.findLatestOccurrenceStartBySeriesId(series.getId());
        if (latestOccurrenceStart.isEmpty()) {
            return false;
        }

        ZoneId zoneId = ZoneId.of(series.getTimezone());
        Instant nowInSeriesZone = ZonedDateTime.now(zoneId).toInstant();
        Instant targetHorizon =
                recurrenceService.resolveMaterializationEnd(nowInSeriesZone, series.getSeriesEnd());
        Instant latestAnchor = latestOccurrenceStart.get();

        if (!latestAnchor.isBefore(targetHorizon)) {
            return false;
        }

        List<Instant> occurrenceStarts =
                recurrenceService.expandOccurrenceStarts(
                        series.getRecurrenceRule(),
                        series.getSeriesStart(),
                        targetHorizon,
                        series.getTimezone());

        List<Instant> newStarts =
                occurrenceStarts.stream().filter(anchor -> anchor.isAfter(latestAnchor)).toList();
        if (newStarts.isEmpty()) {
            return false;
        }

        CreateAppointmentRequest template = buildMaterializationTemplate(series);
        recurrenceService.materializeOccurrences(series, template, newStarts);
        log.debug("Added {} occurrences to series {}", newStarts.size(), series.getId());
        return true;
    }

    private CreateAppointmentRequest buildMaterializationTemplate(AppointmentSeries series) {
        List<Appointment> occurrences = appointmentRepository.findBySeries_Id(series.getId());
        Appointment reference =
                occurrences.stream()
                        .filter(occurrence -> !Boolean.TRUE.equals(occurrence.getIsException()))
                        .min(Comparator.comparing(Appointment::getStart))
                        .orElseGet(
                                () ->
                                        occurrences.stream()
                                                .min(Comparator.comparing(Appointment::getStart))
                                                .orElse(null));

        Instant templateStart = reference != null ? reference.getStart() : series.getSeriesStart();
        Instant templateEnd =
                reference != null ? reference.getEnd() : templateStart.plus(Duration.ofHours(1));

        CreateAppointmentRequest template = new CreateAppointmentRequest();
        template.setName(series.getName());
        template.setType(series.getType());
        template.setStart(templateStart);
        template.setEnd(templateEnd);
        template.setLocation(series.getLocation());
        template.setDescription(series.getDescription());
        template.setResponseMode(series.getResponseMode());
        template.setResponseDeadlineMinutesBefore(series.getResponseDeadlineMinutesBefore());
        template.setAudienceScope(series.getAudienceScope());
        template.setAudienceGroupIds(
                series.getAudienceGroupIds() != null
                        ? new ArrayList<>(series.getAudienceGroupIds())
                        : new ArrayList<>());
        return template;
    }
}
