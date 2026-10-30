/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AppointmentResponseMode;
import com.emberone.appointment.domain.AudienceScope;
import com.emberone.appointment.dto.CreateAppointmentRequest;
import com.emberone.appointment.dto.RecurrenceRequest;
import com.emberone.appointment.dto.UpdateAppointmentRequest;
import com.emberone.appointment.exception.AppointmentValidationException;
import com.emberone.appointment.util.AppointmentTemporalUtil;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentRequestValidator {

    private final OrganizationGroupRepository organizationGroupRepository;

    public void normalizeAndValidateCreate(CreateAppointmentRequest request) {
        normalizeCreateRequest(request);
        validateTimeRange(request.getStart(), request.getEnd());
        validateEndInFutureForCreate(request.getEnd());
        validateResponseSettings(
                request.getResponseMode(), request.getResponseDeadlineMinutesBefore());
        validateAudience(
                request.getOrganizationId(),
                request.getAudienceScope(),
                request.getAudienceGroupIds());
        validateRecurrence(request.getRecurrence(), request.getStart());
    }

    public void normalizeAndValidateUpdate(
            Long organizationId, Appointment appointment, UpdateAppointmentRequest request) {
        normalizeUpdateRequest(request);

        Instant effectiveStart =
                request.getStart() != null ? request.getStart() : appointment.getStart();
        Instant effectiveEnd = request.getEnd() != null ? request.getEnd() : appointment.getEnd();
        validateTimeRange(effectiveStart, effectiveEnd);
        validateStartNotMovedFromFutureToNonFuture(appointment, effectiveStart);

        AppointmentResponseMode effectiveResponseMode =
                request.getResponseMode() != null
                        ? request.getResponseMode()
                        : appointment.getResponseMode();
        Integer effectiveDeadlineMinutesBefore =
                request.getResponseDeadlineMinutesBefore() != null
                        ? request.getResponseDeadlineMinutesBefore()
                        : appointment.getResponseDeadlineMinutesBefore();
        validateResponseSettings(effectiveResponseMode, effectiveDeadlineMinutesBefore);
        if (request.getRecurrence() != null) {
            validateRecurrence(request.getRecurrence(), effectiveStart);
        }

        AudienceScope effectiveAudienceScope =
                request.getAudienceScope() != null
                        ? request.getAudienceScope()
                        : appointment.getAudienceScope();
        List<Long> effectiveAudienceGroupIds =
                resolveEffectiveAudienceGroupIds(request, appointment);
        validateAudience(organizationId, effectiveAudienceScope, effectiveAudienceGroupIds);
    }

    private void normalizeCreateRequest(CreateAppointmentRequest request) {
        request.setStart(AppointmentTemporalUtil.truncateToMinutes(request.getStart()));
        request.setEnd(AppointmentTemporalUtil.truncateToMinutes(request.getEnd()));
        if (request.getRecurrence() != null && request.getRecurrence().getSeriesEnd() != null) {
            request.getRecurrence()
                    .setSeriesEnd(
                            AppointmentTemporalUtil.truncateToMinutes(
                                    request.getRecurrence().getSeriesEnd()));
        }
        request.setName(normalizeRequiredText(request.getName(), "Name is required"));
        request.setLocation(normalizeOptionalText(request.getLocation()));
        request.setDescription(normalizeOptionalText(request.getDescription()));
        request.setResponseDeadlineMinutesBefore(
                normalizeDeadlineMinutes(
                        request.getResponseMode(), request.getResponseDeadlineMinutesBefore()));

        request.setAudienceGroupIds(normalizeGroupIds(request.getAudienceGroupIds()));
        if (request.getAudienceScope() == AudienceScope.ALL) {
            request.setAudienceGroupIds(new ArrayList<>());
        }
    }

    private void normalizeUpdateRequest(UpdateAppointmentRequest request) {
        if (request.getStart() != null) {
            request.setStart(AppointmentTemporalUtil.truncateToMinutes(request.getStart()));
        }
        if (request.getEnd() != null) {
            request.setEnd(AppointmentTemporalUtil.truncateToMinutes(request.getEnd()));
        }
        if (request.getRecurrence() != null && request.getRecurrence().getSeriesEnd() != null) {
            request.getRecurrence()
                    .setSeriesEnd(
                            AppointmentTemporalUtil.truncateToMinutes(
                                    request.getRecurrence().getSeriesEnd()));
        }
        if (request.getName() != null) {
            request.setName(normalizeRequiredText(request.getName(), "Name must not be blank"));
        }
        request.setLocation(normalizeOptionalText(request.getLocation()));
        request.setDescription(normalizeOptionalText(request.getDescription()));

        if (request.getResponseMode() != null
                || request.getResponseDeadlineMinutesBefore() != null) {
            AppointmentResponseMode mode = request.getResponseMode();
            request.setResponseDeadlineMinutesBefore(
                    normalizeDeadlineMinutes(mode, request.getResponseDeadlineMinutesBefore()));
        }

        request.setAudienceGroupIds(normalizeGroupIds(request.getAudienceGroupIds()));
        if (request.getAudienceScope() == AudienceScope.ALL) {
            request.setAudienceGroupIds(new ArrayList<>());
        }
    }

    private Integer normalizeDeadlineMinutes(AppointmentResponseMode mode, Integer minutesBefore) {
        if (mode == AppointmentResponseMode.NONE) {
            return 0;
        }
        return minutesBefore != null ? minutesBefore : 0;
    }

    private List<Long> resolveEffectiveAudienceGroupIds(
            UpdateAppointmentRequest request, Appointment appointment) {
        if (request.getAudienceScope() == AudienceScope.ALL) {
            return List.of();
        }
        if (request.getAudienceGroupIds() != null) {
            return request.getAudienceGroupIds();
        }
        if (request.getAudienceScope() == AudienceScope.SELECTED_GROUPS
                && appointment.getAudienceScope() != AudienceScope.SELECTED_GROUPS) {
            return List.of();
        }
        return appointment.getAudienceGroupIds() != null
                ? appointment.getAudienceGroupIds()
                : List.of();
    }

    private void validateTimeRange(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new AppointmentValidationException("Start and end are required");
        }
        if (!end.isAfter(start)) {
            throw new AppointmentValidationException("End must be after start");
        }
    }

    /**
     * New appointments must not already be over (minute precision, server clock). Start may lie in
     * the past when the slot is still open (now before end).
     */
    private void validateEndInFutureForCreate(Instant end) {
        Instant nowMinute = AppointmentTemporalUtil.truncateToMinutes(Instant.now());
        if (!end.isAfter(nowMinute)) {
            throw new AppointmentValidationException("Appointment end must be in the future");
        }
    }

    /**
     * Prevents moving a still-upcoming occurrence into the past or the current minute; edits to
     * appointments that have already begun keep allowed (metadata, past reschedules within the
     * past).
     */
    private void validateStartNotMovedFromFutureToNonFuture(
            Appointment appointment, Instant effectiveStart) {
        Instant nowMinute = AppointmentTemporalUtil.truncateToMinutes(Instant.now());
        Instant previousStart = AppointmentTemporalUtil.truncateToMinutes(appointment.getStart());
        boolean previousWasFuture = previousStart.isAfter(nowMinute);
        if (previousWasFuture && !effectiveStart.isAfter(nowMinute)) {
            throw new AppointmentValidationException("Appointment start must be in the future");
        }
    }

    private void validateResponseSettings(AppointmentResponseMode mode, Integer minutesBefore) {
        int minutes = minutesBefore != null ? minutesBefore : 0;
        if (mode == AppointmentResponseMode.NONE) {
            if (minutes > 0) {
                throw new AppointmentValidationException(
                        "Response deadline is not allowed when response mode is NONE");
            }
            return;
        }

        if (minutes < 0) {
            throw new AppointmentValidationException(
                    "Response deadline offset must not be negative");
        }
    }

    private void validateAudience(
            Long organizationId, AudienceScope scope, List<Long> audienceGroupIds) {
        if (scope == AudienceScope.ALL) {
            return;
        }

        if (scope != AudienceScope.SELECTED_GROUPS) {
            throw new AppointmentValidationException("Unsupported audience scope");
        }

        if (audienceGroupIds == null || audienceGroupIds.isEmpty()) {
            throw new AppointmentValidationException("At least one audience group is required");
        }

        Set<Long> validGroupIds =
                organizationGroupRepository.findByOrganizationId(organizationId).stream()
                        .map(group -> group.getId())
                        .collect(java.util.stream.Collectors.toSet());

        List<Long> invalidGroupIds =
                audienceGroupIds.stream()
                        .filter(groupId -> groupId == null || !validGroupIds.contains(groupId))
                        .toList();

        if (!invalidGroupIds.isEmpty()) {
            throw new AppointmentValidationException(
                    "Audience groups do not belong to the organization: " + invalidGroupIds);
        }
    }

    private void validateRecurrence(RecurrenceRequest recurrence, Instant start) {
        if (recurrence == null) {
            return;
        }
        if (recurrence.getTimezone() != null) {
            String normalizedTimezone = normalizeOptionalText(recurrence.getTimezone());
            recurrence.setTimezone(normalizedTimezone);
            if (normalizedTimezone != null) {
                try {
                    ZoneId.of(normalizedTimezone);
                } catch (Exception ex) {
                    throw new AppointmentValidationException("Invalid recurrence timezone");
                }
            }
        }
        if (recurrence.getSeriesEnd() != null && recurrence.getSeriesEnd().isBefore(start)) {
            throw new AppointmentValidationException(
                    "Recurrence series end must not be before appointment start");
        }
    }

    private List<Long> normalizeGroupIds(List<Long> groupIds) {
        if (groupIds == null) {
            return null;
        }
        LinkedHashSet<Long> deduplicated = new LinkedHashSet<>(groupIds);
        deduplicated.remove(null);
        return new ArrayList<>(deduplicated);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequiredText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new AppointmentValidationException(message);
        }
        return normalized;
    }
}
