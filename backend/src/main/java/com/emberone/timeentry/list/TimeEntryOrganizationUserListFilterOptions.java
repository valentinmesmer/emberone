/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.list;

import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.timeentry.service.UserTimeEntryService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Filter options for {@code GET …/organizations/{organizationId}/users/{userId}/time-entries}. */
@Component
@RequiredArgsConstructor
public class TimeEntryOrganizationUserListFilterOptions {

    private final UserTimeEntryService userTimeEntryService;

    public ListFilterOptionsDto forUserInOrganization(long userId, long organizationId) {
        List<ListFilterOptionDto> yearOptions =
                userTimeEntryService
                        .listDistinctYearsForUserInOrganization(userId, organizationId)
                        .stream()
                        .map(
                                year ->
                                        new ListFilterOptionDto(
                                                String.valueOf(year), String.valueOf(year)))
                        .toList();
        return new ListFilterOptionsDto(Map.of("year", yearOptions));
    }
}
