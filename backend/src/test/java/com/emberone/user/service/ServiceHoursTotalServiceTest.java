/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.event.dto.ServiceHoursTotalDto;
import com.emberone.event.service.EventAttendanceService;
import com.emberone.timeentry.service.UserTimeEntryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceHoursTotalServiceTest {

    @Mock EventAttendanceService eventAttendanceService;
    @Mock UserTimeEntryService userTimeEntryService;
    @InjectMocks ServiceHoursTotalService service;

    @Test
    void getForUserSumsEventAndTimeEntryHours() {
        when(eventAttendanceService.getEventHoursForUser(4L, 2025)).thenReturn(3.5);
        when(userTimeEntryService.sumHoursForUserInYear(4L, 2025)).thenReturn(1.5);

        ServiceHoursTotalDto total = service.getForUser(4L, 2025);

        assertThat(total.getEventHours()).isEqualTo(3.5);
        assertThat(total.getTimeEntryHours()).isEqualTo(1.5);
        assertThat(total.getTotalHours()).isEqualTo(5.0);
    }
}
