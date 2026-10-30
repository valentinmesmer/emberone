/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.controller;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import com.emberone.timeentry.dto.CreateUserTimeEntryRequest;
import com.emberone.timeentry.dto.UpdateUserTimeEntryRequest;
import com.emberone.timeentry.dto.UserTimeEntryDto;
import com.emberone.timeentry.dto.UserTimeEntryPageDto;
import com.emberone.timeentry.list.TimeEntryListFilter;
import com.emberone.timeentry.list.TimeEntryOrganizationUserListFilterOptions;
import com.emberone.timeentry.service.UserTimeEntryService;
import com.emberone.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/organizations/{organizationId}/users/{userId}/time-entries")
@RequiredArgsConstructor
public class UserTimeEntryUserOrganizationController {

    private final UserTimeEntryService userTimeEntryService;
    private final UserService userService;
    private final TimeEntryOrganizationUserListFilterOptions organizationUserListFilterOptions;

    @GetMapping("/list-filter-options")
    @PreAuthorize("@securityService.hasTimeEntryReadPermission(#organizationId)")
    public ResponseEntity<ListFilterOptionsDto> listFilterOptions(
            @PathVariable Long organizationId, @PathVariable Long userId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                organizationUserListFilterOptions.forUserInOrganization(userId, organizationId));
    }

    @GetMapping
    @PreAuthorize("@securityService.hasTimeEntryReadPermission(#organizationId)")
    public ResponseEntity<UserTimeEntryPageDto> listTimeEntries(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            ListQuery query,
            @RequestParam(name = "organizationId", required = false) List<Long> organizationIds,
            @RequestParam(required = false) List<Integer> year) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                userTimeEntryService.listTimeEntriesForUserInOrganization(
                        userId,
                        organizationId,
                        query,
                        TimeEntryListFilter.forSelf(organizationIds, year)));
    }

    @PostMapping
    @PreAuthorize("@securityService.hasTimeEntryWritePermission(#organizationId)")
    public ResponseEntity<UserTimeEntryDto> createTimeEntry(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody CreateUserTimeEntryRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        UserTimeEntryDto created =
                userTimeEntryService.createTimeEntryInOrganization(userId, organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{entryId}")
    @PreAuthorize("@securityService.hasTimeEntryWritePermission(#organizationId)")
    public ResponseEntity<UserTimeEntryDto> updateTimeEntry(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long entryId,
            @Valid @RequestBody UpdateUserTimeEntryRequest request) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        return ResponseEntity.ok(
                userTimeEntryService.updateTimeEntryInOrganization(
                        userId, organizationId, entryId, request));
    }

    @DeleteMapping("/{entryId}")
    @PreAuthorize("@securityService.hasTimeEntryWritePermission(#organizationId)")
    public ResponseEntity<Void> deleteTimeEntry(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long entryId) {
        userService.assertUserBelongsToOrganization(userId, organizationId);
        userTimeEntryService.deleteTimeEntryInOrganization(userId, organizationId, entryId);
        return ResponseEntity.noContent().build();
    }
}
