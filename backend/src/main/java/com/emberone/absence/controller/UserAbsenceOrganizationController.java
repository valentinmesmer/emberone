/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.controller;

import com.emberone.absence.dto.UserAbsenceDto;
import com.emberone.absence.dto.UserAbsencePageDto;
import com.emberone.absence.list.AbsenceListFilter;
import com.emberone.absence.list.AbsenceOrganizationListFilterOptions;
import com.emberone.absence.service.UserAbsenceService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/user-absences")
@RequiredArgsConstructor
public class UserAbsenceOrganizationController {

    private final UserAbsenceService userAbsenceService;
    private final AbsenceOrganizationListFilterOptions organizationListFilterOptions;

    @GetMapping("/active/list-filter-options")
    @PreAuthorize("@securityService.hasAbsenceReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(organizationListFilterOptions.forOrganization(organizationId));
    }

    @GetMapping("/{absenceId:\\d+}")
    @PreAuthorize("@securityService.hasAbsenceReadPermission(#organizationId)")
    public ResponseEntity<UserAbsenceDto> getAbsence(
            @PathVariable Long organizationId, @PathVariable Long absenceId) {
        return ResponseEntity.ok(
                userAbsenceService.getAbsenceForOrganization(organizationId, absenceId));
    }

    /** Lists absences explicitly linked to this organization (period / reason / year filters). */
    @GetMapping("/active")
    @PreAuthorize("@securityService.hasAbsenceReadPermission(#organizationId)")
    public ResponseEntity<UserAbsencePageDto> getActiveAbsencesForOrganization(
            @PathVariable Long organizationId,
            ListQuery query,
            @RequestParam(required = false) List<String> period,
            @RequestParam(required = false) List<String> reason,
            @RequestParam(required = false) List<Integer> year) {
        return ResponseEntity.ok(
                userAbsenceService.listActiveAbsencesForOrganization(
                        organizationId,
                        query,
                        AbsenceListFilter.parseOrganizationActiveList(period, reason, year)));
    }
}
