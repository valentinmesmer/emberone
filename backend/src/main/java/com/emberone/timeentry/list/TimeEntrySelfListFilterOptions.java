/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.list;

import com.emberone.organization.domain.Organization;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.platform.web.list.options.ListFilterOptionsSource;
import com.emberone.timeentry.service.UserTimeEntryService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Filter options for {@code GET /api/v1/self/time-entries}. */
@Component
@RequiredArgsConstructor
public class TimeEntrySelfListFilterOptions implements ListFilterOptionsSource {

    private final UserTimeEntryService userTimeEntryService;
    private final TokenService tokenService;

    @Override
    @Transactional(readOnly = true)
    public ListFilterOptionsDto filterOptions() {
        Long userId = tokenService.requireUserId();
        List<ListFilterOptionDto> organizationOptions =
                userTimeEntryService.listDistinctOrganizationsForUser(userId).stream()
                        .map(this::toOrganizationOption)
                        .toList();
        List<ListFilterOptionDto> yearOptions =
                userTimeEntryService.listDistinctYearsForUser(userId).stream()
                        .map(
                                year ->
                                        new ListFilterOptionDto(
                                                String.valueOf(year), String.valueOf(year)))
                        .toList();
        return new ListFilterOptionsDto(
                Map.of("organizationId", organizationOptions, "year", yearOptions));
    }

    private ListFilterOptionDto toOrganizationOption(Organization organization) {
        return new ListFilterOptionDto(
                String.valueOf(organization.getId()), organization.getName());
    }
}
