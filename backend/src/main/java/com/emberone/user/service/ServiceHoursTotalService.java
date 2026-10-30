/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import com.emberone.event.dto.ServiceHoursTotalDto;
import com.emberone.event.service.EventAttendanceService;
import com.emberone.timeentry.service.UserTimeEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Composes service-hour totals from event attendance and manual time entries without either module
 * depending on the other.
 */
@Service
@RequiredArgsConstructor
public class ServiceHoursTotalService {

    private final EventAttendanceService eventAttendanceService;
    private final UserTimeEntryService userTimeEntryService;

    @Transactional(readOnly = true)
    public ServiceHoursTotalDto getForUser(Long userId, int year) {
        double eventHours = eventAttendanceService.getEventHoursForUser(userId, year);
        double timeEntryHours = userTimeEntryService.sumHoursForUserInYear(userId, year);
        return ServiceHoursTotalDto.builder()
                .eventHours(eventHours)
                .timeEntryHours(timeEntryHours)
                .totalHours(eventHours + timeEntryHours)
                .build();
    }
}
