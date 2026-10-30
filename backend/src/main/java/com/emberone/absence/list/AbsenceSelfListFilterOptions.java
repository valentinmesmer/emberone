/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.list;

import com.emberone.absence.domain.AbsenceReason;
import com.emberone.absence.service.UserAbsenceService;
import com.emberone.organization.domain.Organization;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.platform.web.list.options.ListFilterOptionsSource;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Filter options for {@code GET /api/v1/self/absences}. */
@Component
@RequiredArgsConstructor
public class AbsenceSelfListFilterOptions implements ListFilterOptionsSource {

    private final UserAbsenceService userAbsenceService;
    private final TokenService tokenService;
    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    public ListFilterOptionsDto filterOptions() {
        Long userId = tokenService.requireUserId();
        List<ListFilterOptionDto> reasonOptions =
                userAbsenceService.listDistinctReasonsForUser(userId).stream()
                        .map(this::toReasonOption)
                        .toList();
        List<ListFilterOptionDto> organizationOptions =
                userAbsenceService.listDistinctOrganizationsForUser(userId).stream()
                        .map(this::toOrganizationOption)
                        .toList();
        List<ListFilterOptionDto> yearOptions =
                userAbsenceService.listDistinctYearsForUser(userId).stream()
                        .map(
                                year ->
                                        new ListFilterOptionDto(
                                                String.valueOf(year), String.valueOf(year)))
                        .toList();
        return new ListFilterOptionsDto(
                Map.of(
                        "reason",
                        reasonOptions,
                        "organizationId",
                        organizationOptions,
                        "year",
                        yearOptions));
    }

    private ListFilterOptionDto toReasonOption(AbsenceReason reason) {
        String label =
                messageSource.getMessage(
                        "absence.reason." + reason.name(), null, reason.name(), Locale.GERMAN);
        return new ListFilterOptionDto(reason.name(), label);
    }

    private ListFilterOptionDto toOrganizationOption(Organization organization) {
        return new ListFilterOptionDto(
                String.valueOf(organization.getId()), organization.getName());
    }
}
