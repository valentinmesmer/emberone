/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.list;

import com.emberone.appointment.service.AppointmentService;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Filter options for {@code GET /api/v1/auth/organizations/{organizationId}/appointments}. */
@Component
@RequiredArgsConstructor
public class AppointmentOrganizationListFilterOptions {

    private final AppointmentService appointmentService;

    public ListFilterOptionsDto forOrganization(long organizationId) {
        List<ListFilterOptionDto> yearOptions =
                appointmentService.listDistinctYearsForOrganization(organizationId).stream()
                        .map(
                                year ->
                                        new ListFilterOptionDto(
                                                String.valueOf(year), String.valueOf(year)))
                        .toList();
        return new ListFilterOptionsDto(Map.of("year", yearOptions));
    }
}
