/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.list;

import com.emberone.event.domain.EventType;
import com.emberone.event.service.EventService;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Filter options for organization service/operation event lists. */
@Component
@RequiredArgsConstructor
public class EventOrganizationListFilterOptions {

    private final EventService eventService;

    public ListFilterOptionsDto forOrganization(long organizationId, EventType eventType) {
        List<ListFilterOptionDto> yearOptions =
                eventService.listDistinctYearsForOrganization(organizationId, eventType).stream()
                        .map(
                                year ->
                                        new ListFilterOptionDto(
                                                String.valueOf(year), String.valueOf(year)))
                        .toList();
        return new ListFilterOptionsDto(Map.of("year", yearOptions));
    }
}
