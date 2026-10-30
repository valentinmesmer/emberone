/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.list;

import com.emberone.absence.domain.AbsenceReason;
import com.emberone.absence.service.UserAbsenceService;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/** Filter options for {@code GET …/organizations/{organizationId}/users/{userId}/absences}. */
@Component
@RequiredArgsConstructor
public class AbsenceOrganizationUserListFilterOptions {

    private final UserAbsenceService userAbsenceService;
    private final MessageSource messageSource;

    public ListFilterOptionsDto forUserInOrganization(long userId, long organizationId) {
        List<ListFilterOptionDto> reasonOptions =
                userAbsenceService
                        .listDistinctReasonsForUserInOrganization(userId, organizationId)
                        .stream()
                        .map(this::toReasonOption)
                        .toList();
        List<ListFilterOptionDto> yearOptions =
                userAbsenceService
                        .listDistinctYearsForUserInOrganization(userId, organizationId)
                        .stream()
                        .map(
                                year ->
                                        new ListFilterOptionDto(
                                                String.valueOf(year), String.valueOf(year)))
                        .toList();
        return new ListFilterOptionsDto(Map.of("reason", reasonOptions, "year", yearOptions));
    }

    private ListFilterOptionDto toReasonOption(AbsenceReason reason) {
        String label =
                messageSource.getMessage(
                        "absence.reason." + reason.name(), null, reason.name(), Locale.GERMAN);
        return new ListFilterOptionDto(reason.name(), label);
    }
}
