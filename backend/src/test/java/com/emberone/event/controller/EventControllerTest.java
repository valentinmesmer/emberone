/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.event.domain.EventType;
import com.emberone.event.dto.CreateServiceEventRequest;
import com.emberone.event.dto.EventAttendancePageDto;
import com.emberone.event.dto.ServiceEventDto;
import com.emberone.event.service.EventAttendanceService;
import com.emberone.event.service.EventService;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock EventService eventService;
    @Mock TokenService tokenService;
    @Mock EventAttendanceService eventAttendanceService;
    @InjectMocks EventController controller;

    @Test
    void createServiceEventFailsWhenUserIdMissing() {
        when(tokenService.requireUserId())
                .thenThrow(new IllegalStateException("No userId found in token"));
        assertThatThrownBy(
                        () -> controller.createServiceEvent(10L, new CreateServiceEventRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No userId found in token");
    }

    @Test
    void createServiceEventReturnsCreatedForAuthenticatedUser() {
        when(tokenService.requireUserId()).thenReturn(42L);
        CreateServiceEventRequest request = new CreateServiceEventRequest();
        ServiceEventDto created = ServiceEventDto.builder().id(5L).build();
        when(eventService.createServiceEvent(42L, 10L, request)).thenReturn(created);

        ResponseEntity<ServiceEventDto> response = controller.createServiceEvent(10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    void deleteServiceEventReturnsNoContent() {
        when(tokenService.requireUserId()).thenReturn(42L);
        ResponseEntity<Void> response = controller.deleteServiceEvent(10L, 5L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(eventService).deleteServiceEvent(42L, 10L, 5L);
    }

    @Test
    void getServiceEventReturnsOk() {
        ServiceEventDto dto = ServiceEventDto.builder().id(5L).build();
        when(eventService.getServiceEvent(10L, 5L)).thenReturn(dto);

        ResponseEntity<ServiceEventDto> response = controller.getServiceEvent(10L, 5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void listAttendancesDelegatesToService() {
        ListQuery query = new ListQuery(0, null, null, null);
        when(eventAttendanceService.listAttendances(10L, EventType.SERVICE, 99L, query))
                .thenReturn(EventAttendancePageDto.builder().content(List.of()).build());
        assertThat(controller.listAttendances(10L, "service", 99L, query).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
