/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.service;

import com.emberone.event.domain.EventAttendance;
import com.emberone.event.domain.EventType;
import com.emberone.event.dto.*;
import com.emberone.event.event.EventAttendanceAddedEvent;
import com.emberone.event.event.EventAttendanceRemovedEvent;
import com.emberone.event.exception.EventAttendanceNotFoundException;
import com.emberone.event.exception.EventValidationException;
import com.emberone.event.list.EventAttendanceListDefinition;
import com.emberone.event.list.EventAttendanceListSpecs;
import com.emberone.event.mapper.EventDtoAssembler;
import com.emberone.event.repository.EventAttendanceRepository;
import com.emberone.event.repository.OperationEventRepository;
import com.emberone.event.repository.ServiceEventRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventAttendanceService {

    private final EventAttendanceRepository eventAttendanceRepository;
    private final ServiceEventRepository serviceEventRepository;
    private final OperationEventRepository operationEventRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TokenService tokenService;
    private final EventService eventService;
    private final EventDtoAssembler eventDtoAssembler;
    private final ApplicationEventPublisher eventPublisher;

    private static double durationHours(Instant start, Instant end) {
        if (start == null) {
            return 0.0;
        }
        Instant effectiveEnd = end != null ? end : start;
        long minutes = Duration.between(start, effectiveEnd).toMinutes();
        if (minutes <= 0) {
            return 0.0;
        }
        return minutes / 60.0;
    }

    private void assertEventInOrganization(EventType eventType, Long eventId, Long organizationId) {
        eventService.assertEventInOrganization(eventType, eventId, organizationId);
    }

    @Transactional(readOnly = true)
    public EventAttendancePageDto listAttendances(
            Long organizationId, EventType eventType, Long eventId, ListQuery query) {
        assertEventInOrganization(eventType, eventId, organizationId);
        Pageable pageable = EventAttendanceListDefinition.INSTANCE.pageable(query);
        Specification<EventAttendance> spec =
                Specification.allOf(
                        EventAttendanceListSpecs.forEvent(eventType, eventId),
                        EventAttendanceListDefinition.INSTANCE.searchSpecification(query));
        Page<EventAttendance> entityPage = eventAttendanceRepository.findAll(spec, pageable);
        List<Long> ids = entityPage.getContent().stream().map(EventAttendance::getId).toList();
        List<EventAttendance> hydrated =
                ids.isEmpty() ? List.of() : eventAttendanceRepository.findWithUserByIdIn(ids);
        List<EventAttendance> ordered =
                PageReorder.reorderByIds(ids, hydrated, EventAttendance::getId);
        Page<EventAttendance> result = PageReorder.pageOf(ordered, entityPage);
        return eventDtoAssembler.toAttendancePageDto(result);
    }

    @Transactional(readOnly = true)
    public List<EventAttendanceSelfItemDto> getAttendancesForUser(
            Long organizationId, EventType eventType, List<Long> eventIds) {
        Long userId = tokenService.requireUserId();
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinctIds = eventIds.stream().distinct().toList();
        Set<Long> validEventIds =
                switch (eventType) {
                    case SERVICE ->
                            new HashSet<>(
                                    serviceEventRepository.findIdsByOrganizationIdAndIdIn(
                                            organizationId, distinctIds));
                    case OPERATION ->
                            new HashSet<>(
                                    operationEventRepository.findIdsByOrganizationIdAndIdIn(
                                            organizationId, distinctIds));
                };
        if (validEventIds.isEmpty()) {
            return List.of();
        }
        Set<Long> attendingIds =
                new HashSet<>(
                        eventAttendanceRepository.findAttendedEventIds(
                                eventType, userId, List.copyOf(validEventIds)));
        return validEventIds.stream()
                .sorted()
                .map(
                        eventId ->
                                EventAttendanceSelfItemDto.builder()
                                        .eventId(eventId)
                                        .attending(attendingIds.contains(eventId))
                                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OperationAlarmAttendanceStatsDto getOperationAlarmAttendanceStatsForUser(Long userId) {
        long totalAlarms = operationEventRepository.countForUserMemberOrganizations(userId);
        long attendedAlarms =
                eventAttendanceRepository.countAttendanceForUserMemberOrganizations(
                        userId, EventType.OPERATION);
        return OperationAlarmAttendanceStatsDto.builder()
                .totalAlarms(totalAlarms)
                .attendedAlarms(attendedAlarms)
                .build();
    }

    @Transactional
    public EventAttendanceDto addAttendance(
            Long organizationId,
            EventType eventType,
            Long eventId,
            CreateEventAttendanceRequest request) {
        assertEventInOrganization(eventType, eventId, organizationId);
        Long targetUserId = request.getUserId();
        userService.assertUserBelongsToOrganization(targetUserId, organizationId);
        if (eventAttendanceRepository.existsByEventTypeAndEventIdAndUser_Id(
                eventType, eventId, targetUserId)) {
            throw new EventValidationException(
                    "This user is already marked as attending this event");
        }
        User user =
                userRepository
                        .findById(targetUserId)
                        .orElseThrow(() -> new UserNotFoundException(targetUserId));
        EventAttendance saved =
                eventAttendanceRepository.save(
                        EventAttendance.builder()
                                .tenantId(user.getTenantId())
                                .eventType(eventType)
                                .eventId(eventId)
                                .user(user)
                                .build());

        eventPublisher.publishEvent(
                new EventAttendanceAddedEvent(
                        user.getTenantId(), eventType, eventId, targetUserId));
        log.info(
                "Added {} event attendance {} for event {} user {}",
                eventType,
                saved.getId(),
                eventId,
                targetUserId);

        return EventAttendanceDto.builder()
                .id(saved.getId())
                .eventType(saved.getEventType())
                .eventId(saved.getEventId())
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    @Transactional
    public void removeAttendance(
            Long organizationId, EventType eventType, Long eventId, Long attendanceId) {
        assertEventInOrganization(eventType, eventId, organizationId);

        EventAttendance row =
                eventAttendanceRepository
                        .findByIdAndEventTypeAndEventId(attendanceId, eventType, eventId)
                        .orElseThrow(
                                () ->
                                        new EventAttendanceNotFoundException(
                                                "Attendance record not found"));

        Long tenantId = row.getTenantId();
        Long userId = row.getUser().getId();
        eventAttendanceRepository.delete(row);

        eventPublisher.publishEvent(
                new EventAttendanceRemovedEvent(tenantId, eventType, eventId, userId));
        log.info(
                "Removed {} event attendance {} for event {} user {}",
                eventType,
                attendanceId,
                eventId,
                userId);
    }

    @Transactional(readOnly = true)
    public double getEventHoursForUser(Long userId, int year) {
        List<EventAttendance> attendances = eventAttendanceRepository.findByUser_Id(userId);
        double eventHours = 0.0;
        for (EventAttendance attendance : attendances) {
            eventHours +=
                    resolveEventDurationHoursForYear(
                            attendance.getEventType(), attendance.getEventId(), year);
        }
        return eventHours;
    }

    private double resolveEventDurationHoursForYear(EventType eventType, Long eventId, int year) {
        ZoneId zone = OrganizationWallClockTime.organizationZone();
        return switch (eventType) {
            case SERVICE ->
                    serviceEventRepository
                            .findById(eventId)
                            .filter(
                                    e ->
                                            e.getStart() != null
                                                    && e.getStart().atZone(zone).getYear() == year)
                            .map(e -> durationHours(e.getStart(), e.getEnd()))
                            .orElse(0.0);
            case OPERATION ->
                    operationEventRepository
                            .findById(eventId)
                            .filter(
                                    e ->
                                            e.getStart() != null
                                                    && e.getStart().atZone(zone).getYear() == year)
                            .map(e -> durationHours(e.getStart(), e.getEnd()))
                            .orElse(0.0);
        };
    }
}
