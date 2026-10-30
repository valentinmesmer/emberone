/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.event.domain.EventType;
import com.emberone.event.dto.EventAttendanceSelfItemDto;
import com.emberone.event.service.EventAttendanceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class EventSelfControllerTest {

    @Mock EventAttendanceService eventAttendanceService;

    @InjectMocks EventSelfController controller;

    @Test
    void getMyAttendancesConvertsCategoryAndDelegates() {
        List<Long> eventIds = List.of(7L, 8L);
        List<EventAttendanceSelfItemDto> items =
                List.of(EventAttendanceSelfItemDto.builder().eventId(7L).attending(true).build());
        when(eventAttendanceService.getAttendancesForUser(10L, EventType.SERVICE, eventIds))
                .thenReturn(items);

        ResponseEntity<List<EventAttendanceSelfItemDto>> response =
                controller.getMyAttendances(10L, "service", eventIds);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(items);
    }
}
