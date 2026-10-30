/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.domain.AppointmentResponse;
import com.emberone.appointment.domain.AppointmentResponseMode;
import com.emberone.appointment.domain.AppointmentResponseValue;
import com.emberone.appointment.dto.*;
import com.emberone.appointment.event.AppointmentChangeType;
import com.emberone.appointment.event.AppointmentChangedEvent;
import com.emberone.appointment.exception.AppointmentNotFoundException;
import com.emberone.appointment.exception.AppointmentValidationException;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.appointment.repository.AppointmentResponseRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentResponseService {

    private final AppointmentResponseRepository appointmentResponseRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentAudienceService audienceService;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    private static List<AppointmentParticipantEntryDto> toSortedParticipantDtos(
            List<Long> userIds, Map<Long, User> usersById) {
        return userIds.stream()
                .map(
                        userId -> {
                            User u = usersById.get(userId);
                            return AppointmentParticipantEntryDto.builder()
                                    .userId(userId)
                                    .userFirstName(u != null ? u.getFirstName() : null)
                                    .userLastName(u != null ? u.getLastName() : null)
                                    .build();
                        })
                .sorted(
                        Comparator.comparing(
                                        AppointmentParticipantEntryDto::getUserLastName,
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                                .thenComparing(
                                        AppointmentParticipantEntryDto::getUserFirstName,
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    @Transactional
    public AppointmentResponseDto submitResponse(
            Long appointmentId, Long organizationId, SubmitAppointmentResponseRequest request) {
        Long userId = tokenService.requireUserId();
        Long tenantId = tokenService.requireTenantId();
        audienceService.ensureUserCanAccess(userId, appointmentId);

        Appointment appointment =
                appointmentRepository
                        .findByIdAndOrganization_Id(appointmentId, organizationId)
                        .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        ;

        validateResponse(appointment, request);

        AppointmentResponse response =
                appointmentResponseRepository
                        .findByAppointmentIdAndUserId(appointmentId, userId)
                        .orElseGet(
                                () ->
                                        AppointmentResponse.builder()
                                                .tenantId(appointment.getTenantId())
                                                .appointment(appointment)
                                                .userId(userId)
                                                .build());

        response.setValue(request.getValue());
        response.setComment(request.getComment());
        response.setLastRespondedAt(Instant.now());
        AppointmentResponse saved = appointmentResponseRepository.save(response);
        publishChanged(appointment, AppointmentChangeType.UPDATED);
        log.info("Submitted appointment response for appointment {}", appointmentId);

        User user = userRepository.findById(userId).orElseThrow();
        return AppointmentResponseDto.builder()
                .userId(userId)
                .userFirstName(user.getFirstName())
                .userLastName(user.getLastName())
                .value(saved.getValue())
                .comment(saved.getComment())
                .lastRespondedAt(saved.getLastRespondedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<MyAppointmentResponseDto> getMyResponses(List<Long> appointmentIds) {
        Long userId = tokenService.requireUserId();
        if (appointmentIds == null || appointmentIds.isEmpty()) {
            return List.of();
        }
        return appointmentResponseRepository
                .findByUserIdAndAppointmentIdIn(userId, appointmentIds)
                .stream()
                .map(
                        r ->
                                MyAppointmentResponseDto.builder()
                                        .appointmentId(r.getAppointment().getId())
                                        .value(r.getValue())
                                        .comment(r.getComment())
                                        .lastRespondedAt(r.getLastRespondedAt())
                                        .build())
                .sorted(Comparator.comparing(MyAppointmentResponseDto::getAppointmentId))
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentParticipationStatsDto getParticipationStats(
            Long appointmentId, Long organizationId) {
        Long userId = tokenService.getUserId();
        audienceService.ensureUserCanAccess(userId, appointmentId);
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
        Set<Long> audienceUserIds = audienceService.resolveAudienceUserIds(appointment);

        List<AppointmentResponse> responses =
                appointmentResponseRepository.findByAppointmentId(appointmentId);

        int yesCount = 0;
        int noCount = 0;
        int uncertainCount = 0;
        List<Long> yesUserIds = new ArrayList<>();
        List<Long> noUserIds = new ArrayList<>();
        List<Long> uncertainUserIds = new ArrayList<>();

        for (AppointmentResponse response : responses) {
            if (!audienceUserIds.contains(response.getUserId())) {
                continue;
            }
            switch (response.getValue()) {
                case YES -> {
                    yesCount++;
                    yesUserIds.add(response.getUserId());
                }
                case NO -> {
                    noCount++;
                    noUserIds.add(response.getUserId());
                }
                case UNCERTAIN -> {
                    uncertainCount++;
                    uncertainUserIds.add(response.getUserId());
                }
            }
        }

        Set<Long> nameLookupIds = new HashSet<>();
        nameLookupIds.addAll(yesUserIds);
        nameLookupIds.addAll(noUserIds);
        nameLookupIds.addAll(uncertainUserIds);
        Map<Long, User> usersById =
                userRepository.findAllById(nameLookupIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        List<AppointmentParticipantEntryDto> yesParticipants =
                toSortedParticipantDtos(yesUserIds, usersById);
        List<AppointmentParticipantEntryDto> noParticipants =
                toSortedParticipantDtos(noUserIds, usersById);
        List<AppointmentParticipantEntryDto> uncertainParticipants =
                toSortedParticipantDtos(uncertainUserIds, usersById);

        return AppointmentParticipationStatsDto.builder()
                .yesCount(yesCount)
                .noCount(noCount)
                .uncertainCount(uncertainCount)
                .yesParticipants(yesParticipants)
                .noParticipants(noParticipants)
                .uncertainParticipants(uncertainParticipants)
                .build();
    }

    private void validateResponse(
            Appointment appointment, SubmitAppointmentResponseRequest request) {
        if (appointment.getResponseMode() == AppointmentResponseMode.NONE) {
            throw new AppointmentValidationException(
                    "Responses are not allowed for this appointment");
        }
        if (appointment.isArchived()) {
            throw new AppointmentValidationException(
                    "Responses are not allowed for archived appointments");
        }
        if (AppointmentResponseDeadlineUtil.isDeadlinePassed(appointment)) {
            throw new AppointmentValidationException("Response deadline has passed");
        }
        if (appointment.getResponseMode() == AppointmentResponseMode.YES_NO
                && request.getValue() == AppointmentResponseValue.UNCERTAIN) {
            throw new AppointmentValidationException(
                    "Uncertain is not allowed for this appointment");
        }
        if (appointment.getResponseMode() != AppointmentResponseMode.YES_NO_WITH_COMMENT
                && appointment.getResponseMode()
                        != AppointmentResponseMode.YES_NO_UNCERTAIN_WITH_COMMENT
                && request.getComment() != null
                && !request.getComment().isBlank()) {
            throw new AppointmentValidationException(
                    "Comments are not allowed for this appointment");
        }
    }

    private void publishChanged(Appointment appointment, AppointmentChangeType changeType) {
        Long organizationId =
                appointment.getOrganization() != null
                        ? appointment.getOrganization().getId()
                        : null;
        eventPublisher.publishEvent(
                new AppointmentChangedEvent(
                        appointment.getTenantId(),
                        organizationId,
                        appointment.getId(),
                        changeType));
    }
}
