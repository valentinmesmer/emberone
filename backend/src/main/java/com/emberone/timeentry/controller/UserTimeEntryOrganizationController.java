/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.controller;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.timeentry.dto.UserTimeEntryDto;
import com.emberone.timeentry.dto.UserTimeEntryPageDto;
import com.emberone.timeentry.list.TimeEntryListFilter;
import com.emberone.timeentry.service.UserTimeEntryService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/user-time-entries")
@RequiredArgsConstructor
public class UserTimeEntryOrganizationController {

    private final UserTimeEntryService userTimeEntryService;

    @GetMapping("/{entryId}")
    @PreAuthorize("@securityService.hasTimeEntryReadPermission(#organizationId)")
    public ResponseEntity<UserTimeEntryDto> getTimeEntry(
            @PathVariable Long organizationId, @PathVariable Long entryId) {
        return ResponseEntity.ok(
                userTimeEntryService.getTimeEntryForOrganization(organizationId, entryId));
    }

    @GetMapping
    @PreAuthorize("@securityService.hasTimeEntryReadPermission(#organizationId)")
    public ResponseEntity<UserTimeEntryPageDto> listTimeEntries(
            @PathVariable Long organizationId,
            ListQuery query,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok(
                userTimeEntryService.listTimeEntriesForOrganization(
                        organizationId, query, TimeEntryListFilter.forRange(from, to)));
    }
}
