/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.emberone.event.domain.EventType;
import com.emberone.event.dto.CreateEventAttendanceRequest;
import com.emberone.event.exception.EventNotFoundException;
import com.emberone.event.mapper.EventDtoAssembler;
import com.emberone.event.repository.EventAttendanceRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EventAttendanceServiceTest {

    @Mock EventAttendanceRepository attendanceRepository;
    @Mock EventService eventService;
    @Mock EventDtoAssembler eventDtoAssembler;
    @Mock UserRepository userRepository;
    @Mock UserService userService;
    @Mock TokenService tokenService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks EventAttendanceService service;

    @Test
    void addAttendanceRejectsWhenEventNotInOrganization() {
        doThrow(
                        new EventNotFoundException(
                                "Event not found or does not belong to the organization"))
                .when(eventService)
                .assertEventInOrganization(EventType.SERVICE, 5L, 10L);

        assertThatThrownBy(
                        () ->
                                service.addAttendance(
                                        10L,
                                        EventType.SERVICE,
                                        5L,
                                        new CreateEventAttendanceRequest()))
                .hasMessageContaining("not found");
    }

    @Test
    void addAttendanceRejectsDuplicate() {
        org.mockito.Mockito.doNothing()
                .when(eventService)
                .assertEventInOrganization(EventType.SERVICE, 5L, 10L);
        CreateEventAttendanceRequest req = new CreateEventAttendanceRequest();
        req.setUserId(1L);
        org.mockito.Mockito.doNothing().when(userService).assertUserBelongsToOrganization(1L, 10L);
        when(attendanceRepository.existsByEventTypeAndEventIdAndUser_Id(EventType.SERVICE, 5L, 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.addAttendance(10L, EventType.SERVICE, 5L, req))
                .hasMessageContaining("already marked");
    }

    @Test
    void addAttendanceRejectsUserNotInOrganization() {
        org.mockito.Mockito.doNothing()
                .when(eventService)
                .assertEventInOrganization(EventType.SERVICE, 5L, 10L);
        CreateEventAttendanceRequest req = new CreateEventAttendanceRequest();
        req.setUserId(99L);
        doThrow(new IllegalArgumentException("User does not belong to the specified organization"))
                .when(userService)
                .assertUserBelongsToOrganization(99L, 10L);

        assertThatThrownBy(() -> service.addAttendance(10L, EventType.SERVICE, 5L, req))
                .hasMessageContaining("does not belong");
    }
}
